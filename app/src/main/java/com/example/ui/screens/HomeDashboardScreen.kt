package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.HavalCarAction
import com.example.model.InstalledApp
import com.example.model.MediaBrowseItem
import com.example.model.MediaPlayerState
import com.example.ui.components.HavalControlsAndPinnedPanel
import com.example.ui.components.YandexPlayerWidget

@Composable
fun HomeDashboardScreen(
    mediaState: MediaPlayerState,
    reduceAnimations: Boolean,
    havalActions: List<HavalCarAction>,
    pinnedApps: List<InstalledApp>,
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
    onPlayBrowseItem: (MediaBrowseItem) -> Unit,
    onCarActionClick: (HavalCarAction) -> Unit,
    onPinnedAppClick: (InstalledApp) -> Unit,
    onOpenAppsDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideCockpit = maxWidth >= 760.dp

        if (isWideCockpit) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                    onPlayBrowseItem = onPlayBrowseItem,
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight()
                )

                HavalControlsAndPinnedPanel(
                    havalActions = havalActions,
                    pinnedApps = pinnedApps,
                    onCarActionClick = onCarActionClick,
                    onPinnedAppClick = onPinnedAppClick,
                    onOpenAppsDrawer = onOpenAppsDrawer,
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    onPlayBrowseItem = onPlayBrowseItem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                )

                HavalControlsAndPinnedPanel(
                    havalActions = havalActions,
                    pinnedApps = pinnedApps,
                    onCarActionClick = onCarActionClick,
                    onPinnedAppClick = onPinnedAppClick,
                    onOpenAppsDrawer = onOpenAppsDrawer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
        }
    }
}
