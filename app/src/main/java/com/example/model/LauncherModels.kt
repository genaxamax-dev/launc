package com.example.model

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

enum class LauncherDestination {
    HOME,
    APPS,
    SETTINGS
}

enum class AppCategory(val title: String) {
    ALL("Все"),
    MEDIA("Мультимедиа"),
    CAR("Авто Haval"),
    SYSTEM("Система")
}

@Immutable
data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?,
    val category: AppCategory,
    val isPinned: Boolean = false
)

enum class CarActionKind {
    CLIMATE,
    RADIO,
    AVM_360,
    DVR,
    BLUETOOTH,
    CAR_SETTINGS,
    USB_MEDIA,
    PHONE
}

@Immutable
data class HavalCarAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: CarActionKind,
    val primaryPackage: String,
    val primaryActivity: String,
    val fallbackComponents: List<Pair<String, String>> = emptyList(),
    val fallbackIntentAction: String? = null
)

enum class MediaConnectionMode {
    MEDIA_BROWSER_CONNECTED,
    MEDIA_KEYS_FALLBACK
}

@Immutable
data class MediaBrowseItem(
    val mediaId: String,
    val title: String,
    val subtitle: String,
    val isPlayable: Boolean,
    val isBrowsable: Boolean
)

@Immutable
data class MediaPlayerState(
    val isYandexMusicInstalled: Boolean = false,
    val yandexMusicVersion: String? = null,
    val isPlaying: Boolean = false,
    val trackTitle: String = "Яндекс.Музыка",
    val artistName: String = "Нажмите Play для запуска воспроизведения",
    val albumName: String = "",
    val albumArt: Bitmap? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val connectionMode: MediaConnectionMode = MediaConnectionMode.MEDIA_KEYS_FALLBACK,
    val activeSourceLabel: String = "МУЛЬТИМЕДИА",
    val volumeCurrent: Int = 12,
    val volumeMax: Int = 30,
    val isMuted: Boolean = false,
    val isVolumeFixed: Boolean = false,
    val browseItems: List<MediaBrowseItem> = emptyList(),
    val canBrowseYandex: Boolean = false,
    val isNotificationListenerConnected: Boolean = false,
    val diagnosticStatus: String = "Режим универсальных медиа-клавиш"
)

enum class WifiConnectionStatus {
    CONNECTED,
    ENABLED_NO_CONNECTION,
    DISABLED
}

@Immutable
data class WifiState(
    val status: WifiConnectionStatus = WifiConnectionStatus.DISABLED,
    val signalLevel: Int = 0, // 0..4
    val linkSpeedMbps: Int = 0
)

@Immutable
data class LauncherSettings(
    val wallpaperDim: Float = 0.44f,
    val enforceHavalLeftInset: Boolean = true,
    val enforceHavalTopInset: Boolean = true,
    val showSecondsInClock: Boolean = false,
    val lightweightBackground: Boolean = false,
    val reduceAnimations: Boolean = true,
    val githubRepo: String = "",
    val autoCheckUpdates: Boolean = true,
    val lastUpdateCheckMs: Long = 0L,
    val pinnedPackages: List<String> = listOf(
        "ru.yandex.music",
        "com.adayo.ora.radio",
        "com.adayo.media",
        "com.adayo.ac",
        "com.android.car.dialer"
    )
)

@Immutable
sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object RepoNotConfigured : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data class UpToDate(val currentVersion: String) : UpdateCheckState()
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val releaseUrl: String,
        val apkUrl: String?
    ) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}
