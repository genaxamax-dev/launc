package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.LauncherDestination
import com.example.ui.LauncherViewModel
import com.example.ui.components.CockpitBottomDock
import com.example.ui.components.CockpitTopBar
import com.example.ui.screens.AppDrawerScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LauncherSettingsScreen
import com.example.ui.theme.CockpitObsidian
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.HavalAmber
import com.example.ui.theme.M6LauncherTheme
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            M6LauncherTheme {
                M6LauncherRoot(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onStop()
    }
}

@Composable
private fun M6LauncherRoot(viewModel: LauncherViewModel) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val wifiState by viewModel.wifiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pinnedApps by viewModel.pinnedApps.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Decode wallpaper once in RGB_565 (50% less RAM) on IO thread only if enabled
    val heroBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = settings.lightweightBackground
    ) {
        value = if (settings.lightweightBackground) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeResource(context.resources, R.drawable.haval_m6_hero, bounds)
                    var sampleSize = 1
                    while (bounds.outWidth / sampleSize > 1920) {
                        sampleSize *= 2
                    }
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeResource(context.resources, R.drawable.haval_m6_hero, opts)
                        ?.asImageBitmap()
                }.getOrNull()
            }
        }
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Prevent hardware/system Back button from exiting the Home Launcher
    BackHandler(enabled = destination == LauncherDestination.HOME) {
        // Stay on Home screen
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitObsidian)
    ) {
        val wallpaper = heroBitmap
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CockpitObsidian.copy(alpha = settings.wallpaperDim))
            )
        } else {
            // Ultra-lightweight carbon gradient background (0 texture RAM)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CockpitObsidian,
                                CockpitSurface,
                                CockpitObsidian
                            )
                        )
                    )
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isHavalHeadUnitWide = maxWidth >= 1000.dp && maxWidth > maxHeight
            val safePadding = WindowInsets.safeDrawing.asPaddingValues()
            val layoutDirection = LocalLayoutDirection.current

            val leftSafePadding = if (isHavalHeadUnitWide && settings.enforceHavalLeftInset) {
                max(safePadding.calculateLeftPadding(layoutDirection), 120.dp)
            } else {
                safePadding.calculateLeftPadding(layoutDirection)
            }
            val topSafePadding = if (isHavalHeadUnitWide && settings.enforceHavalTopInset) {
                max(safePadding.calculateTopPadding(), 80.dp)
            } else {
                safePadding.calculateTopPadding()
            }
            val rightSafePadding = safePadding.calculateRightPadding(layoutDirection)
            val bottomSafePadding = safePadding.calculateBottomPadding()

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = leftSafePadding,
                        top = topSafePadding,
                        end = rightSafePadding,
                        bottom = bottomSafePadding
                    ),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = CockpitSurfaceElevated,
                            contentColor = TextPrimary,
                            actionColor = HavalAmber,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CockpitTopBar(
                        activeSourceLabel = mediaState.activeSourceLabel,
                        wifiState = wifiState,
                        showSeconds = settings.showSecondsInClock,
                        onWifiClick = { viewModel.openWifiSettings() },
                        onCycleDim = { viewModel.cycleWallpaperDimPreset() },
                        onOpenSettings = {
                            viewModel.navigateTo(
                                if (destination == LauncherDestination.SETTINGS) {
                                    LauncherDestination.HOME
                                } else {
                                    LauncherDestination.SETTINGS
                                }
                            )
                        },
                        onOpenApps = {
                            viewModel.navigateTo(
                                if (destination == LauncherDestination.APPS) {
                                    LauncherDestination.HOME
                                } else {
                                    LauncherDestination.APPS
                                }
                            )
                        }
                    )

                    val screenContent: @Composable (LauncherDestination) -> Unit = { screen ->
                        when (screen) {
                            LauncherDestination.HOME -> {
                                HomeDashboardScreen(
                                    mediaState = mediaState,
                                    reduceAnimations = settings.reduceAnimations,
                                    havalActions = viewModel.havalActions,
                                    pinnedApps = pinnedApps,
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onPrevious = { viewModel.skipPrevious() },
                                    onNext = { viewModel.skipNext() },
                                    onSeek = { viewModel.seekTo(it) },
                                    onVolumeChange = { viewModel.setVolume(it) },
                                    onStepVolume = { viewModel.stepVolume(it) },
                                    onToggleMute = { viewModel.toggleMute() },
                                    onOpenYandexMusic = { viewModel.openYandexMusic() },
                                    onOpenMyWave = { viewModel.openYandexMyWave() },
                                    onOpenCollection = { viewModel.openYandexCollection() },
                                    onOpenSearch = { viewModel.openYandexSearch() },
                                    onOpenUsbMedia = { viewModel.launchUsbMedia() },
                                    onPlayBrowseItem = { viewModel.playBrowseItem(it) },
                                    onCarActionClick = { viewModel.launchCarAction(it) },
                                    onPinnedAppClick = { viewModel.launchApp(it) },
                                    onOpenAppsDrawer = { viewModel.navigateTo(LauncherDestination.APPS) }
                                )
                            }

                            LauncherDestination.APPS -> {
                                AppDrawerScreen(
                                    apps = filteredApps,
                                    searchQuery = searchQuery,
                                    selectedCategory = selectedCategory,
                                    onSearchChange = { viewModel.updateSearchQuery(it) },
                                    onSelectCategory = { viewModel.selectCategory(it) },
                                    onAppClick = { viewModel.launchApp(it) },
                                    onTogglePin = { viewModel.togglePinApp(it) },
                                    onOpenAppInfo = { viewModel.openAppInfo(it) },
                                    onBack = { viewModel.navigateTo(LauncherDestination.HOME) }
                                )
                            }

                            LauncherDestination.SETTINGS -> {
                                LauncherSettingsScreen(
                                    settings = settings,
                                    mediaState = mediaState,
                                    wifiState = wifiState,
                                    onWallpaperDimChange = { viewModel.setWallpaperDim(it) },
                                    onToggleLeftInset = { viewModel.setEnforceHavalLeftInset(it) },
                                    onToggleTopInset = { viewModel.setEnforceHavalTopInset(it) },
                                    onToggleShowSeconds = { viewModel.setShowSecondsInClock(it) },
                                    onToggleLightweightBackground = { viewModel.setLightweightBackground(it) },
                                    onToggleReduceAnimations = { viewModel.setReduceAnimations(it) },
                                    onOpenWifiSettings = { viewModel.openWifiSettings() },
                                    onReconnectMediaBrowser = { viewModel.reconnectMediaBrowser() },
                                    onOpenHomeSettings = { viewModel.openHomeLauncherSettings() },
                                    onBack = { viewModel.navigateTo(LauncherDestination.HOME) }
                                )
                            }
                        }
                    }

                    if (settings.reduceAnimations) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            screenContent(destination)
                        }
                    } else {
                        AnimatedContent(
                            targetState = destination,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(160)) togetherWith
                                    fadeOut(animationSpec = tween(120))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            label = "launcher_screen"
                        ) { screen ->
                            screenContent(screen)
                        }
                    }

                    CockpitBottomDock(
                        currentDestination = destination,
                        onHomeClick = { viewModel.navigateTo(LauncherDestination.HOME) },
                        onYandexMusicClick = { viewModel.openYandexMusic() },
                        onRadioClick = { viewModel.launchRadio() },
                        onPhoneClick = { viewModel.launchPhone() },
                        onAppsClick = {
                            viewModel.navigateTo(
                                if (destination == LauncherDestination.APPS) {
                                    LauncherDestination.HOME
                                } else {
                                    LauncherDestination.APPS
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
