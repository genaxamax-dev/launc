package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.MediaBrowseItem
import com.example.model.MediaPlayerState
import com.example.ui.components.YandexPlayerWidget

@Composable
fun HomeDashboardScreen(
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
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideCockpit = maxWidth >= 760.dp

        if (isWideCockpit) {
            val playerWidth = (maxWidth * 0.40f).coerceIn(380.dp, 500.dp)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                YandexPlayerWidget(
                    mediaState = mediaState,
                    reduceAnimations = reduceAnimations,
                    onTogglePlayPause = onTogglePlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeek = onSeek,
                    onVolumeChange = onVolumeChange,
                    onStepVolume = onStepVolume,
                    onToggleMute = onToggleMute,
                    onOpenYandexMusic = onOpenYandexMusic,
                    onOpenMyWave = onOpenMyWave,
                    onOpenCollection = onOpenCollection,
                    onOpenSearch = onOpenSearch,
                    onOpenUsbMedia = onOpenUsbMedia,
                    onOpenNotificationAccess = onOpenNotificationAccess,
                    onPlayBrowseItem = onPlayBrowseItem,
                    modifier = Modifier.width(playerWidth)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                YandexPlayerWidget(
                    mediaState = mediaState,
                    reduceAnimations = reduceAnimations,
                    onTogglePlayPause = onTogglePlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeek = onSeek,
                    onVolumeChange = onVolumeChange,
                    onStepVolume = onStepVolume,
                    onToggleMute = onToggleMute,
                    onOpenYandexMusic = onOpenYandexMusic,
                    onOpenMyWave = onOpenMyWave,
                    onOpenCollection = onOpenCollection,
                    onOpenSearch = onOpenSearch,
                    onOpenUsbMedia = onOpenUsbMedia,
                    onOpenNotificationAccess = onOpenNotificationAccess,
                    onPlayBrowseItem = onPlayBrowseItem,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
