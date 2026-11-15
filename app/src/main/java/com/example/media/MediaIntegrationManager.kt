package com.example.media

import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.model.MediaBrowseItem
import com.example.model.MediaConnectionMode
import com.example.model.MediaPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MediaIntegrationManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val YANDEX_MUSIC_PKG = "ru.yandex.music"

        private val notificationUpdates = MutableSharedFlow<Pair<String?, String?>>(extraBufferCapacity = 8)

        fun onYandexNotification(title: String?, artist: String?) {
            if (!title.isNullOrBlank() || !artist.isNullOrBlank()) {
                notificationUpdates.tryEmit(title to artist)
            }
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val pm = context.packageManager

    private val _state = MutableStateFlow(MediaPlayerState())
    val state: StateFlow<MediaPlayerState> = _state.asStateFlow()

    private var mediaController: MediaController? = null
    private var sessionListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var pollJob: Job? = null
    private var listenerWatchJob: Job? = null
    private var notificationWatchJob: Job? = null
    private var receiverRegistered = false
    private var lastMutedVolume: Int = 10

    private val mediaBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when {
                action == "android.media.VOLUME_CHANGED_ACTION" -> {
                    refreshAudioStatus()
                }
                action.contains("source", ignoreCase = true) -> {
                    val source = intent.getStringExtra("source")
                        ?: intent.getStringExtra("sourceId")
                        ?: intent.getStringExtra("curSource")
                    if (!source.isNullOrBlank()) {
                        _state.update {
                            it.copy(activeSourceLabel = formatAdayoSource(source))
                        }
                    }
                }
                action.contains("mediainsert", ignoreCase = true) ||
                    action.contains("usbinsert", ignoreCase = true) -> {
                    _state.update { it.copy(activeSourceLabel = "USB МЕДИА") }
                }
                else -> {
                    val track = intent.getStringExtra("track")
                    val artist = intent.getStringExtra("artist")
                    val album = intent.getStringExtra("album")
                    val playing = if (intent.hasExtra("playing")) {
                        intent.getBooleanExtra("playing", _state.value.isPlaying)
                    } else {
                        _state.value.isPlaying
                    }
                    if (!track.isNullOrBlank() || !artist.isNullOrBlank()) {
                        _state.update { current ->
                            current.copy(
                                trackTitle = track?.takeIf { it.isNotBlank() } ?: current.trackTitle,
                                artistName = artist?.takeIf { it.isNotBlank() } ?: current.artistName,
                                albumName = album ?: current.albumName,
                                isPlaying = playing,
                                activeSourceLabel = if (action.contains("yandex")) "ЯНДЕКС.МУЗЫКА" else current.activeSourceLabel
                            )
                        }
                    }
                }
            }
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromPlaybackState(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromMetadata(metadata)
        }

        override fun onSessionDestroyed() {
            detachMediaController(resetMode = true)
        }
    }

    fun start() {
        refreshYandexPackageStatus()
        refreshAudioStatus()
        registerMediaBroadcasts()
        observeNotificationListener()
        observeNotificationMetadata()
        connectYandexSession()
        startAudioPolling()
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        listenerWatchJob?.cancel()
        listenerWatchJob = null
        notificationWatchJob?.cancel()
        notificationWatchJob = null
        unregisterMediaBroadcasts()
        disconnectSession()
    }

    fun refreshYandexPackageStatus() {
        val pkgInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(YANDEX_MUSIC_PKG, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(YANDEX_MUSIC_PKG, 0)
            }
        }.getOrNull()

        val installed = pkgInfo != null
        val version = pkgInfo?.versionName
        _state.update { current ->
            current.copy(
                isYandexMusicInstalled = installed,
                yandexMusicVersion = version,
                trackTitle = if (current.connectionMode == MediaConnectionMode.MEDIA_KEYS_FALLBACK &&
                    current.trackTitle == "Яндекс.Музыка"
                ) {
                    if (installed) "Яндекс.Музыка" else "Универсальный медиаплеер"
                } else {
                    current.trackTitle
                },
                artistName = if (current.connectionMode == MediaConnectionMode.MEDIA_KEYS_FALLBACK &&
                    current.artistName.startsWith("Нажмите Play")
                ) {
                    if (installed) "Управление по медиа-клавишам • Готово"
                    else "Яндекс.Музыка не найдена • Медиа-клавиши активны"
                } else {
                    current.artistName
                }
            )
        }
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller != null) {
            val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
            if (isPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            return
        }
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        scope.launch {
            delay(250)
            refreshAudioStatus()
        }
    }

    fun skipToNext() {
        val controller = mediaController
        if (controller != null) {
            controller.transportControls.skipToNext()
            return
        }
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        scope.launch {
            delay(250)
            refreshAudioStatus()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController
        if (controller != null) {
            controller.transportControls.skipToPrevious()
            return
        }
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        scope.launch {
            delay(250)
            refreshAudioStatus()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.transportControls?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun playBrowseItem(item: MediaBrowseItem) {
        val controller = mediaController ?: return
        if (item.isPlayable) {
            controller.transportControls.playFromMediaId(item.mediaId, null)
        }
    }

    fun setVolume(volume: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = volume.coerceIn(0, max)
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
        if (target > 0) {
            lastMutedVolume = target
        }
        refreshAudioStatus()
    }

    fun stepVolume(delta: Int) {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        setVolume(current + delta)
    }

    fun toggleMute() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (current > 0) {
            lastMutedVolume = current
            setVolume(0)
        } else {
            setVolume(lastMutedVolume.coerceAtLeast(6))
        }
    }

    fun openYandexMusic(): Result<Unit> = runCatching {
        val launch = pm.getLaunchIntentForPackage(YANDEX_MUSIC_PKG)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launch != null) {
            context.startActivity(launch)
        } else {
            val musicIntent = Intent.makeMainSelectorActivity(
                Intent.ACTION_MAIN,
                Intent.CATEGORY_APP_MUSIC
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (musicIntent.resolveActivity(pm) != null) {
                context.startActivity(musicIntent)
            } else {
                error("Яндекс.Музыка не установлена на устройстве")
            }
        }
    }

    fun openYandexDeepLink(uriString: String, fallbackSearchQuery: String? = null): Result<Unit> {
        return runCatching {
            val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                setPackage(YANDEX_MUSIC_PKG)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (deepLinkIntent.resolveActivity(pm) != null) {
                context.startActivity(deepLinkIntent)
                return@runCatching
            }

            if (!fallbackSearchQuery.isNullOrBlank()) {
                val searchIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    setPackage(YANDEX_MUSIC_PKG)
                    putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                    putExtra(SearchManager.QUERY, fallbackSearchQuery)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (searchIntent.resolveActivity(pm) != null) {
                    context.startActivity(searchIntent)
                    return@runCatching
                }
            }

            val launch = pm.getLaunchIntentForPackage(YANDEX_MUSIC_PKG)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: error("Яндекс.Музыка не установлена на устройстве")
            context.startActivity(launch)
        }
    }

    fun reconnectMediaBrowser() {
        disconnectSession()
        refreshYandexPackageStatus()
        connectYandexSession()
    }

    private fun observeNotificationListener() {
        listenerWatchJob?.cancel()
        listenerWatchJob = scope.launch {
            YandexNotificationListener.connected.collect { isConnected ->
                _state.update { it.copy(isNotificationListenerConnected = isConnected) }
                if (isConnected) {
                    connectYandexSession()
                }
            }
        }
    }

    private fun observeNotificationMetadata() {
        notificationWatchJob?.cancel()
        notificationWatchJob = scope.launch {
            notificationUpdates.collect { (title, artist) ->
                _state.update { current ->
                    current.copy(
                        trackTitle = title?.takeIf { it.isNotBlank() } ?: current.trackTitle,
                        artistName = artist?.takeIf { it.isNotBlank() } ?: current.artistName,
                        activeSourceLabel = "ЯНДЕКС.МУЗЫКА"
                    )
                }
            }
        }
    }

    private fun isListenerAccessGranted(): Boolean {
        val enabledBySystem = runCatching {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }.getOrDefault(false)
        return YandexNotificationListener.instance != null || enabledBySystem
    }

    private fun connectYandexSession() {
        val listener = YandexNotificationListener.component(context)
        val hasAccess = isListenerAccessGranted()

        if (!hasAccess) {
            _state.update {
                it.copy(
                    connectionMode = MediaConnectionMode.MEDIA_KEYS_FALLBACK,
                    isNotificationListenerConnected = false,
                    diagnosticStatus = "Нет доступа к уведомлениям • включите Notification access"
                )
            }
            return
        }

        _state.update { it.copy(isNotificationListenerConnected = true) }

        if (sessionListener == null) {
            val cb = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
                bindYandexSession(sessions)
            }
            val registered = runCatching {
                sessionManager.addOnActiveSessionsChangedListener(cb, listener)
            }.isSuccess
            if (registered) {
                sessionListener = cb
            }
        }

        val activeSessions = runCatching {
            sessionManager.getActiveSessions(listener)
        }.getOrNull()

        if (activeSessions != null) {
            bindYandexSession(activeSessions)
        } else {
            _state.update {
                it.copy(
                    connectionMode = MediaConnectionMode.MEDIA_KEYS_FALLBACK,
                    diagnosticStatus = "Доступ к уведомлениям ожидает активации системой"
                )
            }
        }
    }

    private fun bindYandexSession(sessions: List<MediaController>?) {
        val controller = sessions?.firstOrNull { it.packageName == YANDEX_MUSIC_PKG }
            ?: sessions?.firstOrNull()

        if (controller == null) {
            detachMediaController(resetMode = true)
            _state.update {
                it.copy(
                    connectionMode = MediaConnectionMode.MEDIA_KEYS_FALLBACK,
                    diagnosticStatus = if (it.isYandexMusicInstalled) {
                        "MediaSession готова • Запустите трек в Яндекс.Музыке"
                    } else {
                        "MediaSession готова • Ожидание активного плеера"
                    }
                )
            }
            return
        }

        if (controller.sessionToken == mediaController?.sessionToken) {
            updateFromMetadata(controller.metadata)
            updateFromPlaybackState(controller.playbackState)
            return
        }

        detachMediaController(resetMode = false)
        mediaController = controller
        controller.registerCallback(controllerCallback)
        updateFromMetadata(controller.metadata)
        updateFromPlaybackState(controller.playbackState)

        val isYandex = controller.packageName == YANDEX_MUSIC_PKG
        _state.update {
            it.copy(
                connectionMode = MediaConnectionMode.MEDIA_BROWSER_CONNECTED,
                activeSourceLabel = if (isYandex) "ЯНДЕКС.МУЗЫКА" else controller.packageName.substringAfterLast('.').uppercase(),
                diagnosticStatus = "MediaSession подключена (${controller.packageName})"
            )
        }
    }

    private fun detachMediaController(resetMode: Boolean = true) {
        mediaController?.let { runCatching { it.unregisterCallback(controllerCallback) } }
        mediaController = null
        if (resetMode) {
            _state.update {
                it.copy(
                    connectionMode = MediaConnectionMode.MEDIA_KEYS_FALLBACK,
                    canBrowseYandex = false,
                    browseItems = emptyList()
                )
            }
        }
    }

    private fun disconnectSession() {
        sessionListener?.let { listener ->
            runCatching { sessionManager.removeOnActiveSessionsChangedListener(listener) }
        }
        sessionListener = null
        detachMediaController(resetMode = true)
    }

    private fun updateFromMetadata(metadata: MediaMetadata?) {
        if (metadata == null) return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)

        var artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)

        if (artist.isNullOrBlank()) {
            val description = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
            if (!description.isNullOrBlank()) {
                val parsed = description.split(", ").drop(1).joinToString(", ").trim()
                artist = parsed.ifEmpty { description }
            }
        }

        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val art: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        _state.update { current ->
            current.copy(
                trackTitle = title?.takeIf { it.isNotBlank() } ?: current.trackTitle,
                artistName = artist?.takeIf { it.isNotBlank() } ?: current.artistName,
                albumName = album ?: current.albumName,
                durationMs = if (duration > 0) duration else current.durationMs,
                albumArt = art ?: current.albumArt
            )
        }
    }

    private fun updateFromPlaybackState(playbackState: PlaybackState?) {
        if (playbackState == null) return
        val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING ||
            playbackState.state == PlaybackState.STATE_BUFFERING
        _state.update { current ->
            current.copy(
                isPlaying = isPlaying,
                positionMs = playbackState.position.coerceAtLeast(0L)
            )
        }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        runCatching {
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }

    private fun startAudioPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                refreshAudioStatus()
                val interval = if (_state.value.isPlaying) 1500L else 2800L
                delay(interval)
            }
        }
    }

    private fun refreshAudioStatus() {
        val activeAudio = audioManager.isMusicActive
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val controller = mediaController

        _state.update { current ->
            val playing = if (controller?.playbackState != null) {
                controller.playbackState?.state == PlaybackState.STATE_PLAYING
            } else {
                activeAudio
            }
            val pos = if (controller?.playbackState != null && playing) {
                val pb = controller.playbackState!!
                val elapsed = (SystemClock.elapsedRealtime() - pb.lastPositionUpdateTime)
                    .coerceAtLeast(0L)
                (pb.position + (elapsed * pb.playbackSpeed).toLong())
                    .coerceIn(0L, current.durationMs.coerceAtLeast(Long.MAX_VALUE))
            } else {
                current.positionMs
            }

            current.copy(
                isPlaying = playing,
                positionMs = pos,
                volumeCurrent = vol,
                volumeMax = maxVol,
                isMuted = vol == 0
            )
        }
    }

    private fun registerMediaBroadcasts() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction("com.android.music.metachanged")
            addAction("com.android.music.playstatechanged")
            addAction("ru.yandex.music.metachanged")
            addAction("ru.yandex.music.playstatechanged")
            addAction("com.spotify.music.metachanged")
            addAction("com.spotify.music.playbackstatechanged")
            addAction("com.adayo.action.uisourcechange")
            addAction("com.adayo.action.sourcerealchange")
            addAction("com.adayo.action.frontsourcechange")
            addAction("com.adayo.action.mediainsert")
            addAction("com.adayo.action.usbinsert")
        }
        ContextCompat.registerReceiver(
            context,
            mediaBroadcastReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    private fun unregisterMediaBroadcasts() {
        if (!receiverRegistered) return
        runCatching { context.unregisterReceiver(mediaBroadcastReceiver) }
        receiverRegistered = false
    }

    private fun formatAdayoSource(raw: String): String = when {
        raw.contains("RADIO", ignoreCase = true) -> "РАДИО FM/AM"
        raw.contains("BT_AUDIO", ignoreCase = true) -> "BLUETOOTH АУДИО"
        raw.contains("USB", ignoreCase = true) -> "USB МЕДИА"
        raw.contains("NET_MUSIC", ignoreCase = true) -> "ОНЛАЙН МУЗЫКА"
        raw.contains("AUX", ignoreCase = true) -> "AUX"
        raw.contains("NAVI", ignoreCase = true) -> "НАВИГАЦИЯ"
        else -> "МУЛЬТИМЕДИА"
    }
}
