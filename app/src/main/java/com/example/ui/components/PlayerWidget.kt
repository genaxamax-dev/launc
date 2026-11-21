package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MediaBrowseItem
import com.example.model.MediaConnectionMode
import com.example.model.MediaPlayerState
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.CockpitHairline
import com.example.ui.theme.CockpitObsidian
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceBright
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.HavalAmber
import com.example.ui.theme.HavalAmberBright
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YandexGold
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun YandexPlayerWidget(
    mediaState: MediaPlayerState,
    reduceAnimations: Boolean,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onStepVolume: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onOpenYandexMusic: () -> Unit,
    onOpenMyWave: () -> Unit,
    onOpenCollection: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenUsbMedia: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onPlayBrowseItem: (MediaBrowseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("yandex_player_widget"),
        shape = RoundedCornerShape(24.dp),
        color = CockpitSurface.copy(alpha = 0.90f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    YandexGold.copy(alpha = 0.45f),
                    CockpitHairline,
                    HavalAmber.copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top source & Yandex Music status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (mediaState.isPlaying) ActiveEmerald else YandexGold)
                    )
                    Text(
                        text = mediaState.activeSourceLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = YandexGold
                    )
                    Surface(
                        onClick = {
                            if (!mediaState.isNotificationListenerConnected) {
                                onOpenNotificationAccess()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = CockpitSurfaceBright.copy(alpha = 0.8f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (!mediaState.isNotificationListenerConnected) HavalAmber.copy(alpha = 0.6f) else CockpitHairline
                        ),
                        modifier = Modifier.testTag("media_connection_badge")
                    ) {
                        Text(
                            text = when {
                                mediaState.connectionMode == MediaConnectionMode.MEDIA_BROWSER_CONNECTED -> "MEDIA SESSION"
                                !mediaState.isNotificationListenerConnected -> "НУЖЕН ДОСТУП"
                                else -> "КЛАВИШИ"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                mediaState.connectionMode == MediaConnectionMode.MEDIA_BROWSER_CONNECTED -> ActiveEmerald
                                !mediaState.isNotificationListenerConnected -> HavalAmber
                                else -> TelemetryCyan
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onOpenYandexMusic,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_yandex_music_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = YandexGold.copy(alpha = 0.16f),
                        contentColor = YandexGold
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Открыть Яндекс.Музыку",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (mediaState.isYandexMusicInstalled) "Я.Музыка" else "Плеер",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Album Art + Track Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AlbumArtBox(
                    mediaState = mediaState,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenYandexMusic)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = mediaState.trackTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaState.artistName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqualizerMiniIndicator(
                            isPlaying = mediaState.isPlaying,
                            animate = mediaState.isPlaying && !reduceAnimations
                        )
                        Text(
                            text = if (mediaState.isPlaying) "Сейчас играет" else "Пауза / Ожидание",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (mediaState.isPlaying) ActiveEmerald else TextMuted
                        )
                    }
                    Text(
                        text = mediaState.diagnosticStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mediaState.isNotificationListenerConnected) TextMuted else HavalAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Progress / Seek bar
            Column(modifier = Modifier.fillMaxWidth()) {
                if (mediaState.durationMs > 0L) {
                    val progress = (mediaState.positionMs.toFloat() / mediaState.durationMs.toFloat())
                        .coerceIn(0f, 1f)
                    Slider(
                        value = progress,
                        onValueChange = { frac ->
                            onSeek((frac * mediaState.durationMs).toLong())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("player_seek_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = YandexGold,
                            activeTrackColor = YandexGold,
                            inactiveTrackColor = CockpitSurfaceBright
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(mediaState.positionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = formatDuration(mediaState.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    LiveActivityWaveBar(
                        isPlaying = mediaState.isPlaying,
                        animate = mediaState.isPlaying && !reduceAnimations,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )
                }
            }

            // Primary Driver-Friendly Transport Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TransportIconButton(
                    icon = if (mediaState.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (mediaState.isMuted) "Включить звук" else "Выключить звук",
                    containerColor = CockpitSurfaceElevated,
                    contentColor = if (mediaState.isMuted) HavalAmber else TextSecondary,
                    size = 52.dp,
                    testTag = "player_mute_button",
                    onClick = onToggleMute
                )

                TransportIconButton(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = "Предыдущий трек",
                    containerColor = CockpitSurfaceBright,
                    contentColor = TextPrimary,
                    size = 60.dp,
                    testTag = "player_prev_button",
                    onClick = onPrevious
                )

                // Hero Play/Pause
                Surface(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("player_play_pause_button"),
                    shape = CircleShape,
                    color = if (mediaState.isPlaying) HavalAmber else YandexGold
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (mediaState.isPlaying) "Пауза" else "Воспроизвести",
                            tint = CockpitObsidian,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                TransportIconButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Следующий трек",
                    containerColor = CockpitSurfaceBright,
                    contentColor = TextPrimary,
                    size = 60.dp,
                    testTag = "player_next_button",
                    onClick = onNext
                )

                TransportIconButton(
                    icon = Icons.Rounded.Usb,
                    contentDescription = "Медиа USB и Bluetooth",
                    containerColor = CockpitSurfaceElevated,
                    contentColor = TelemetryCyan,
                    size = 52.dp,
                    testTag = "player_usb_media_button",
                    onClick = onOpenUsbMedia
                )
            }

            // Yandex Music Quick Modes OR Browse Items
            if (mediaState.canBrowseYandex && mediaState.browseItems.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(mediaState.browseItems, key = { it.mediaId }) { item ->
                        Surface(
                            onClick = { onPlayBrowseItem(item) },
                            shape = RoundedCornerShape(12.dp),
                            color = CockpitSurfaceElevated,
                            border = BorderStroke(1.dp, CockpitHairline),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = item.title,
                                    tint = YandexGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YandexQuickChip(
                        label = "Моя волна",
                        icon = Icons.Rounded.AutoAwesome,
                        accent = YandexGold,
                        testTag = "ym_my_wave_chip",
                        onClick = onOpenMyWave,
                        modifier = Modifier.weight(1f)
                    )
                    YandexQuickChip(
                        label = "Коллекция",
                        icon = Icons.Rounded.Favorite,
                        accent = HavalAmberBright,
                        testTag = "ym_collection_chip",
                        onClick = onOpenCollection,
                        modifier = Modifier.weight(1f)
                    )
                    YandexQuickChip(
                        label = "Поиск",
                        icon = Icons.Rounded.Search,
                        accent = TelemetryCyan,
                        testTag = "ym_search_chip",
                        onClick = onOpenSearch,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Volume Control Strip
            var dragVolume by remember { mutableStateOf<Float?>(null) }
            var lastSentVolume by remember { mutableIntStateOf(-1) }
            val maxVolFloat = mediaState.volumeMax.toFloat().coerceAtLeast(1f)
            val displayedVolumeFloat = (dragVolume ?: mediaState.volumeCurrent.toFloat())
                .coerceIn(0f, maxVolFloat)
            val displayedVolumeInt = displayedVolumeFloat.roundToInt()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CockpitSurfaceElevated.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onStepVolume(-1) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("volume_down_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                        contentDescription = "Тише",
                        tint = TextSecondary
                    )
                }

                Slider(
                    value = displayedVolumeFloat,
                    onValueChange = { raw ->
                        dragVolume = raw
                        val target = raw.roundToInt()
                        if (target != lastSentVolume) {
                            lastSentVolume = target
                            onVolumeChange(target)
                        }
                    },
                    onValueChangeFinished = {
                        dragVolume?.let { raw ->
                            onVolumeChange(raw.roundToInt())
                        }
                        dragVolume = null
                        lastSentVolume = -1
                    },
                    valueRange = 0f..maxVolFloat,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = HavalAmber,
                        activeTrackColor = HavalAmber,
                        inactiveTrackColor = CockpitSurfaceBright
                    )
                )

                IconButton(
                    onClick = { onStepVolume(1) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("volume_up_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Громче",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = if (mediaState.isVolumeFixed) {
                        "ФИКС"
                    } else {
                        "$displayedVolumeInt/${mediaState.volumeMax}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (mediaState.isVolumeFixed) TextMuted else HavalAmberBright,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AlbumArtBox(
    mediaState: MediaPlayerState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(Color(0xFF282012), CockpitSurfaceBright)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        val art = mediaState.albumArt
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = "Обложка трека",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            YandexGold.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f,
                    center = center
                )
                drawCircle(
                    color = CockpitHairline,
                    radius = size.minDimension * 0.36f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
            Icon(
                imageVector = if (mediaState.isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                contentDescription = "Аудио",
                tint = YandexGold,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun TransportIconButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    size: androidx.compose.ui.unit.Dp,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
private fun YandexQuickChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = CockpitSurfaceElevated,
        border = BorderStroke(1.dp, CockpitHairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EqualizerMiniIndicator(
    isPlaying: Boolean,
    animate: Boolean
) {
    if (!animate) {
        Canvas(modifier = Modifier.size(width = 16.dp, height = 14.dp)) {
            val bars = if (isPlaying) listOf(0.55f, 0.95f, 0.7f) else listOf(0.25f, 0.25f, 0.25f)
            val color = if (isPlaying) ActiveEmerald else TextMuted
            val barWidth = size.width / 5f
            bars.forEachIndexed { index, frac ->
                val h = size.height * frac
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x = index * (barWidth * 1.8f), y = size.height - h),
                    size = Size(width = barWidth, height = h),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "eq")
    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )

    Canvas(modifier = Modifier.size(width = 16.dp, height = 14.dp)) {
        val bars = listOf(bar1, bar2, bar3)
        val barWidth = size.width / 5f
        bars.forEachIndexed { index, frac ->
            val h = size.height * frac
            drawRoundRect(
                color = ActiveEmerald,
                topLeft = Offset(x = index * (barWidth * 1.8f), y = size.height - h),
                size = Size(width = barWidth, height = h),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

@Composable
private fun LiveActivityWaveBar(
    isPlaying: Boolean,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    if (!animate) {
        Canvas(modifier = modifier) {
            drawRoundRect(
                color = CockpitSurfaceBright,
                size = size,
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            )
            if (isPlaying) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(HavalAmber, YandexGold)
                    ),
                    size = size,
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        drawRoundRect(
            color = CockpitSurfaceBright,
            size = size,
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        )
        val segWidth = size.width * 0.28f
        val travel = size.width - segWidth
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(HavalAmber, YandexGold)
            ),
            topLeft = Offset(x = travel * phase, y = 0f),
            size = Size(width = segWidth, height = size.height),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}
