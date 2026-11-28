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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.example.model.Season
import com.example.model.UpdateCheckState
import com.example.model.WeatherCondition
import com.example.model.WeatherState
import com.example.ui.LauncherViewModel
import com.example.ui.components.CockpitBottomDock
import com.example.ui.components.CockpitTopBar
import com.example.ui.screens.AppDrawerScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LauncherSettingsScreen
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.CockpitObsidian
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.HavalAmber
import com.example.ui.theme.M6LauncherTheme
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YandexGold
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
    val climateState by viewModel.climateState.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val headlightsOn by viewModel.headlightsOn.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var activeUpdateDialog by remember { mutableStateOf<UpdateCheckState.UpdateAvailable?>(null) }
    var showWeatherDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Decode day or night (headlights ON) wallpaper in RGB_565 (50% less RAM) on IO thread
    val heroBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = settings.lightweightBackground,
        key2 = headlightsOn
    ) {
        value = if (settings.lightweightBackground) {
            null
        } else {
            val resId = if (headlightsOn) {
                R.drawable.img_haval_m6_hero_night
            } else {
                R.drawable.haval_m6_hero
            }
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeResource(context.resources, resId, bounds)
                    var sampleSize = 1
                    while (bounds.outWidth / sampleSize > 1920) {
                        sampleSize *= 2
                    }
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeResource(context.resources, resId, opts)
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
            WeatherAtmosphereOverlay(
                weatherState = weatherState,
                reduceAnimations = settings.reduceAnimations
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
                    if (destination == LauncherDestination.HOME) {
                        CockpitTopBar(
                            climateState = climateState,
                            weatherState = weatherState,
                            reduceAnimations = settings.reduceAnimations,
                            wifiState = wifiState,
                            updateState = updateState,
                            onIncreaseClimateTemp = { viewModel.increaseClimateTemp() },
                            onDecreaseClimateTemp = { viewModel.decreaseClimateTemp() },
                            onToggleClimateAc = { viewModel.toggleClimateAc() },
                            onOpenClimateApp = { viewModel.launchClimate() },
                            onWeatherClick = { showWeatherDialog = true },
                            onWifiClick = { viewModel.openWifiSettings() },
                            onUpdateClick = { activeUpdateDialog = it },
                            onCycleDim = { viewModel.cycleWallpaperDimPreset() },
                            onOpenSettings = { viewModel.navigateTo(LauncherDestination.SETTINGS) },
                            onOpenApps = { viewModel.navigateTo(LauncherDestination.APPS) },
                            onAndroidAutoClick = { viewModel.launchAndroidAuto() }
                        )
                    }

                    val screenContent: @Composable (LauncherDestination) -> Unit = { screen ->
                        when (screen) {
                            LauncherDestination.HOME -> {
                                HomeDashboardScreen(
                                    mediaState = mediaState,
                                    reduceAnimations = settings.reduceAnimations,
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
                                    onOpenNotificationAccess = { viewModel.openNotificationListenerSettings() },
                                    onPlayBrowseItem = { viewModel.playBrowseItem(it) }
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
                                    onOpenAppInfo = { viewModel.openAppInfo(it) },
                                    onBack = { viewModel.navigateTo(LauncherDestination.HOME) }
                                )
                            }

                            LauncherDestination.SETTINGS -> {
                                LauncherSettingsScreen(
                                    settings = settings,
                                    mediaState = mediaState,
                                    wifiState = wifiState,
                                    weatherState = weatherState,
                                    headlightsOn = headlightsOn,
                                    updateState = updateState,
                                    onWallpaperDimChange = { viewModel.setWallpaperDim(it) },
                                    onToggleLeftInset = { viewModel.setEnforceHavalLeftInset(it) },
                                    onToggleTopInset = { viewModel.setEnforceHavalTopInset(it) },
                                    onToggleShowSeconds = { viewModel.setShowSecondsInClock(it) },
                                    onToggleLightweightBackground = { viewModel.setLightweightBackground(it) },
                                    onToggleReduceAnimations = { viewModel.setReduceAnimations(it) },
                                    onHeadlightModeChange = { viewModel.setHeadlightMode(it) },
                                    onWeatherModeChange = { viewModel.setWeatherMode(it) },
                                    onSaveWeatherConfig = { city, key -> viewModel.saveWeatherConfig(city, key) },
                                    onRefreshWeatherNow = { viewModel.refreshWeather(manual = true) },
                                    onOpenYandexWeather = { viewModel.openYandexWeather() },
                                    onSaveGithubRepo = { viewModel.saveGithubRepo(it) },
                                    onToggleAutoCheckUpdates = { viewModel.setAutoCheckUpdates(it) },
                                    onCheckUpdatesNow = { viewModel.checkForUpdates(manual = true) },
                                    onOpenUpdateLink = { viewModel.openUpdateLink(it) },
                                    onOpenWifiSettings = { viewModel.openWifiSettings() },
                                    onOpenNotificationAccess = { viewModel.openNotificationListenerSettings() },
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
                                .weight(1f),
                            label = "launcher_screen"
                        ) { screen ->
                            screenContent(screen)
                        }
                    }

                    if (destination == LauncherDestination.HOME) {
                        CockpitBottomDock(
                            currentDestination = destination,
                            onHomeClick = { viewModel.navigateTo(LauncherDestination.HOME) },
                            onYandexMusicClick = { viewModel.openYandexMusic() },
                            onAndroidAutoClick = { viewModel.launchAndroidAuto() },
                            onClimateClick = { viewModel.launchClimate() },
                            onRadioClick = { viewModel.launchRadio() },
                            onPhoneClick = { viewModel.launchPhone() },
                            onAppsClick = { viewModel.navigateTo(LauncherDestination.APPS) }
                        )
                    }
                }
            }
        }

        val dialogUpdate = activeUpdateDialog
        if (dialogUpdate != null) {
            AlertDialog(
                onDismissRequest = { activeUpdateDialog = null },
                containerColor = CockpitSurfaceElevated,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = {
                    Text("Доступно обновление ${dialogUpdate.latestVersion}")
                },
                text = {
                    val notes = dialogUpdate.releaseNotes.take(400).ifBlank {
                        "Новая сборка готова на GitHub (текущая: v${dialogUpdate.currentVersion})."
                    }
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.openUpdateLink(dialogUpdate.apkUrl ?: dialogUpdate.releaseUrl)
                            activeUpdateDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ActiveEmerald,
                            contentColor = CockpitObsidian
                        )
                    ) {
                        Text(if (dialogUpdate.apkUrl != null) "Скачать APK" else "Открыть релиз")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeUpdateDialog = null }) {
                        Text("Позже", color = HavalAmber)
                    }
                }
            )
        }

        if (showWeatherDialog) {
            AlertDialog(
                onDismissRequest = { showWeatherDialog = false },
                containerColor = CockpitSurfaceElevated,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = {
                    val tempStr = if (weatherState.temperatureC > 0) "+${weatherState.temperatureC}°C" else "${weatherState.temperatureC}°C"
                    Text("${weatherState.cityName} • $tempStr")
                },
                text = {
                    val details = buildString {
                        appendLine("Условия: ${weatherState.conditionLabel}")
                        weatherState.feelsLikeC?.let {
                            val feel = if (it > 0) "+$it°C" else "$it°C"
                            appendLine("Ощущается как: $feel")
                        }
                        weatherState.windSpeedMs?.let {
                            appendLine("Ветер: $it м/с")
                        }
                        appendLine("Сезон: ${weatherState.season.label}")
                        appendLine("Источник: ${weatherState.sourceLabel}")
                        append("Фары авто: ${if (headlightsOn) "включены" else "выключены"}")
                    }
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.openYandexWeather()
                            showWeatherDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YandexGold,
                            contentColor = CockpitObsidian
                        )
                    ) {
                        Text("Яндекс.Погода")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.refreshWeather(manual = true)
                        }
                    ) {
                        Text("Обновить", color = TelemetryCyan)
                    }
                }
            )
        }
    }
}

@Composable
private fun WeatherAtmosphereOverlay(
    weatherState: WeatherState,
    reduceAnimations: Boolean
) {
    val seasonTint = when (weatherState.season) {
        Season.WINTER -> Color(0xFF0E253D).copy(alpha = 0.14f)
        Season.AUTUMN -> Color(0xFF2B1B0E).copy(alpha = 0.08f)
        Season.SPRING -> Color(0xFF0F261D).copy(alpha = 0.05f)
        Season.SUMMER -> Color.Transparent
    }
    if (seasonTint != Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(seasonTint)
        )
    }

    val showSnow = weatherState.condition == WeatherCondition.SNOW
    val showRain = weatherState.condition == WeatherCondition.RAIN ||
        weatherState.condition == WeatherCondition.THUNDERSTORM
    if (!showSnow && !showRain) return

    val phase = if (reduceAnimations) {
        0.35f
    } else {
        val transition = rememberInfiniteTransition(label = "weather_fx")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (showSnow) 6500 else 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "weather_phase"
        )
        animated
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val count = if (reduceAnimations) 16 else 24
        for (i in 0 until count) {
            val seedX = ((i * 73 + 19) % 100) / 100f
            val baseY = ((i * 41 + 11) % 100) / 100f
            val yFrac = (baseY + phase * (if (showSnow) 1f else 1.8f)) % 1f
            val x = size.width * seedX
            val y = size.height * yFrac

            if (showSnow) {
                val radius = 2.5f + (i % 3) * 1.4f
                drawCircle(
                    color = Color.White.copy(alpha = 0.42f),
                    radius = radius,
                    center = Offset(x, y)
                )
            } else {
                drawLine(
                    color = TelemetryCyan.copy(alpha = 0.25f),
                    start = Offset(x, y),
                    end = Offset(x - 6f, y + 18f),
                    strokeWidth = 1.6f
                )
            }
        }
    }
}
