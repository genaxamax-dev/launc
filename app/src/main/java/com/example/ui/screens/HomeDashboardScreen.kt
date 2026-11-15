package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(0.46f))
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
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                )
            }
        } else {
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
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
