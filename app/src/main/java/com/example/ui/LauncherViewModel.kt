package com.example.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.car.ClimateController
import com.example.data.AppRepository
import com.example.data.LauncherPreferencesRepository
import com.example.media.MediaIntegrationManager
import com.example.model.AppCategory
import com.example.model.ClimateState
import com.example.model.HavalCarAction
import com.example.model.HeadlightMode
import com.example.model.InstalledApp
import com.example.model.LauncherDestination
import com.example.model.LauncherSettings
import com.example.model.MediaBrowseItem
import com.example.model.MediaPlayerState
import com.example.model.UpdateCheckState
import com.example.model.WeatherSourceMode
import com.example.model.WeatherState
import com.example.model.WifiConnectionStatus
import com.example.model.WifiState
import com.example.network.GitHubUpdateChecker
import com.example.network.WeatherRepository
import com.example.network.WifiMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherViewModel(
    application: Application,
    private val appRepository: AppRepository,
    private val preferencesRepository: LauncherPreferencesRepository
) : AndroidViewModel(application) {

    companion object {
        private const val AUTO_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L // 6 hours
        private const val WEATHER_REFRESH_INTERVAL_MS = 30L * 60L * 1000L // 30 minutes
    }

    private val mediaManager = MediaIntegrationManager(application, viewModelScope)
    private val wifiMonitor = WifiMonitor(application)
    private val climateController = ClimateController(application, viewModelScope, preferencesRepository)

    private val _destination = MutableStateFlow(LauncherDestination.HOME)
    val destination: StateFlow<LauncherDestination> = _destination.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AppCategory.ALL)
    val selectedCategory: StateFlow<AppCategory> = _selectedCategory.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    private val _weatherState = MutableStateFlow(WeatherRepository.seasonalWeather())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _timeTick = MutableStateFlow(System.currentTimeMillis())
    private var timeReceiverRegistered = false
    private var lastWeatherFetchMs = 0L
    private var lastWeatherConfigKey = ""

    private val timeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _timeTick.value = System.currentTimeMillis()
        }
    }

    private val _rawApps = MutableStateFlow<List<InstalledApp>>(emptyList())

    private var autoCheckedThisSession = false

    val settings: StateFlow<LauncherSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LauncherSettings()
        )

    val mediaState: StateFlow<MediaPlayerState> = mediaManager.state
    val wifiState: StateFlow<WifiState> = wifiMonitor.wifiState
    val climateState: StateFlow<ClimateState> = climateController.state

    val headlightsOn: StateFlow<Boolean> = combine(
        settings,
        _timeTick
    ) { currentSettings, _ ->
        WeatherRepository.computeHeadlightsOn(currentSettings.headlightMode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeatherRepository.computeHeadlightsOn(HeadlightMode.AUTO)
    )

    val filteredApps: StateFlow<List<InstalledApp>> = combine(
        _rawApps,
        _searchQuery,
        _selectedCategory
    ) { apps, query, category ->
        val q = query.trim().lowercase()
        apps.filter { app ->
            val categoryMatch = category == AppCategory.ALL || app.category == category
            val queryMatch = q.isEmpty() ||
                app.label.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
            categoryMatch && queryMatch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            _rawApps.value = appRepository.loadInstalledApps(emptySet())
        }
        viewModelScope.launch {
            appRepository.observePackageChanges().collect {
                _rawApps.value = appRepository.loadInstalledApps(emptySet())
                mediaManager.refreshYandexPackageStatus()
            }
        }
        viewModelScope.launch {
            combine(wifiState, settings) { wifi, currentSettings ->
                wifi to currentSettings
            }.collect { (wifi, currentSettings) ->
                if (!autoCheckedThisSession &&
                    wifi.status == WifiConnectionStatus.CONNECTED &&
                    currentSettings.autoCheckUpdates &&
                    currentSettings.githubRepo.isNotBlank()
                ) {
                    val elapsed = System.currentTimeMillis() - currentSettings.lastUpdateCheckMs
                    if (elapsed >= AUTO_CHECK_INTERVAL_MS || currentSettings.lastUpdateCheckMs == 0L) {
                        autoCheckedThisSession = true
                        checkForUpdates(manual = false)
                    } else {
                        autoCheckedThisSession = true
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(wifiState, settings, _timeTick) { wifi, currentSettings, now ->
                Triple(wifi, currentSettings, now)
            }.collect { (wifi, currentSettings, now) ->
                val configKey = "${currentSettings.weatherMode}|${currentSettings.weatherCity}|${currentSettings.yandexWeatherApiKey}|${wifi.status == WifiConnectionStatus.CONNECTED}"
                val elapsed = now - lastWeatherFetchMs
                if (configKey != lastWeatherConfigKey || elapsed >= WEATHER_REFRESH_INTERVAL_MS) {
                    lastWeatherConfigKey = configKey
                    lastWeatherFetchMs = now
                    fetchWeatherInternal(
                        city = currentSettings.weatherCity,
                        yandexKey = currentSettings.yandexWeatherApiKey,
                        mode = currentSettings.weatherMode,
                        wifiConnected = wifi.status == WifiConnectionStatus.CONNECTED
                    )
                }
            }
        }
    }

    fun onStart() {
        _timeTick.value = System.currentTimeMillis()
        registerTimeReceiver()
        mediaManager.start()
        wifiMonitor.start()
        climateController.start()
    }

    fun onStop() {
        unregisterTimeReceiver()
        mediaManager.stop()
        wifiMonitor.stop()
        climateController.stop()
    }

    private fun registerTimeReceiver() {
        if (timeReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(getApplication(), timeChangeReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        timeReceiverRegistered = true
    }

    private fun unregisterTimeReceiver() {
        if (!timeReceiverRegistered) return
        runCatching { getApplication<Application>().unregisterReceiver(timeChangeReceiver) }
        timeReceiverRegistered = false
    }

    fun navigateTo(dest: LauncherDestination) {
        _destination.value = dest
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: AppCategory) {
        _selectedCategory.value = category
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun launchApp(app: InstalledApp) {
        appRepository.launchApp(app.packageName, app.activityName)
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Не удалось открыть ${app.label}"
            }
    }

    private fun launchCarAction(action: HavalCarAction) {
        appRepository.launchCarAction(action)
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Не удалось открыть ${action.title}"
            }
    }

    fun launchClimate() {
        launchCarAction(appRepository.getClimateAction())
    }

    fun increaseClimateTemp() = climateController.increaseTemperature()
    fun decreaseClimateTemp() = climateController.decreaseTemperature()
    fun toggleClimateAc() = climateController.toggleAc()

    fun launchUsbMedia() {
        launchCarAction(appRepository.getUsbMediaAction())
    }

    fun launchRadio() {
        launchCarAction(appRepository.getRadioAction())
    }

    fun launchPhone() {
        launchCarAction(appRepository.getPhoneAction())
    }

    fun launchAndroidAuto() {
        appRepository.launchAndroidAuto()
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Не удалось открыть Android Auto"
            }
    }

    fun openWifiSettings() {
        appRepository.openWifiSettings()
            .onFailure {
                _statusMessage.value = "Откройте настройки Wi-Fi вручную"
            }
    }

    fun openNotificationListenerSettings() {
        appRepository.openNotificationListenerSettings()
            .onFailure {
                _statusMessage.value = "Откройте «Доступ к уведомлениям» в настройках"
            }
    }

    fun openAppInfo(app: InstalledApp) {
        appRepository.openAppInfo(app.packageName)
    }

    fun openHomeLauncherSettings() {
        appRepository.openHomeLauncherSettings()
            .onFailure {
                _statusMessage.value = "Откройте настройки системы для выбора лаунчера"
            }
    }

    // Media Controls
    fun togglePlayPause() = mediaManager.togglePlayPause()
    fun skipNext() = mediaManager.skipToNext()
    fun skipPrevious() = mediaManager.skipToPrevious()
    fun seekTo(posMs: Long) = mediaManager.seekTo(posMs)
    fun playBrowseItem(item: MediaBrowseItem) = mediaManager.playBrowseItem(item)
    fun setVolume(vol: Int) = mediaManager.setVolume(vol)
    fun stepVolume(delta: Int) = mediaManager.stepVolume(delta)
    fun toggleMute() = mediaManager.toggleMute()
    fun reconnectMediaBrowser() = mediaManager.reconnectMediaBrowser()

    fun openYandexMusic() {
        mediaManager.openYandexMusic()
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Яндекс.Музыка не найдена"
            }
    }

    fun openYandexMyWave() {
        mediaManager.openYandexDeepLink("yandexmusic://radio/station/user:onyourwave", "Моя волна")
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Яндекс.Музыка не найдена"
            }
    }

    fun openYandexCollection() {
        mediaManager.openYandexDeepLink("yandexmusic://mymusic/")
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Яндекс.Музыка не найдена"
            }
    }

    fun openYandexSearch() {
        mediaManager.openYandexDeepLink("yandexmusic://search/", "")
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Яндекс.Музыка не найдена"
            }
    }

    // GitHub Updates
    fun saveGithubRepo(rawInput: String) {
        val normalized = GitHubUpdateChecker.normalizeRepo(rawInput)
        viewModelScope.launch {
            if (rawInput.isBlank()) {
                preferencesRepository.setGithubRepo("")
                _updateState.value = UpdateCheckState.RepoNotConfigured
                _statusMessage.value = "Репозиторий GitHub очищен"
            } else if (normalized.isEmpty()) {
                _statusMessage.value = "Укажите репозиторий в формате owner/repo или ссылку GitHub"
            } else {
                preferencesRepository.setGithubRepo(normalized)
                _statusMessage.value = "Репозиторий сохранён: $normalized"
                checkForUpdates(manual = true)
            }
        }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoCheckUpdates(enabled) }
    }

    fun checkForUpdates(manual: Boolean = true) {
        val repo = settings.value.githubRepo
        if (repo.isBlank()) {
            _updateState.value = UpdateCheckState.RepoNotConfigured
            if (manual) {
                _statusMessage.value = "Укажите свой репозиторий GitHub в настройках"
            }
            return
        }
        if (_updateState.value is UpdateCheckState.Checking) return

        viewModelScope.launch {
            _updateState.value = UpdateCheckState.Checking
            val result = GitHubUpdateChecker.checkLatestRelease(repo)
            _updateState.value = result
            if (result is UpdateCheckState.UpToDate || result is UpdateCheckState.UpdateAvailable) {
                preferencesRepository.setLastUpdateCheckMs(System.currentTimeMillis())
            }
            if (manual) {
                when (result) {
                    is UpdateCheckState.UpdateAvailable ->
                        _statusMessage.value = "Доступно обновление ${result.latestVersion}"
                    is UpdateCheckState.UpToDate ->
                        _statusMessage.value = "Установлена актуальная версия (${result.currentVersion})"
                    is UpdateCheckState.Error ->
                        _statusMessage.value = result.message
                    UpdateCheckState.RepoNotConfigured ->
                        _statusMessage.value = "Укажите репозиторий GitHub"
                    else -> Unit
                }
            }
        }
    }

    fun openUpdateLink(url: String) {
        appRepository.openUrl(url)
            .onFailure {
                _statusMessage.value = "Не удалось открыть ссылку в браузере"
            }
    }

    // Settings
    fun setWallpaperDim(dim: Float) {
        viewModelScope.launch { preferencesRepository.setWallpaperDim(dim) }
    }

    fun cycleWallpaperDimPreset() {
        val current = settings.value.wallpaperDim
        val next = when {
            current < 0.32f -> 0.45f
            current < 0.58f -> 0.70f
            else -> 0.22f
        }
        setWallpaperDim(next)
    }

    fun setEnforceHavalLeftInset(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setEnforceHavalLeftInset(enabled) }
    }

    fun setEnforceHavalTopInset(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setEnforceHavalTopInset(enabled) }
    }

    fun setShowSecondsInClock(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowSecondsInClock(enabled) }
    }

    fun setLightweightBackground(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setLightweightBackground(enabled) }
    }

    fun setReduceAnimations(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setReduceAnimations(enabled) }
    }

    fun setHeadlightMode(mode: HeadlightMode) {
        viewModelScope.launch { preferencesRepository.setHeadlightMode(mode) }
    }

    fun setWeatherMode(mode: WeatherSourceMode) {
        viewModelScope.launch { preferencesRepository.setWeatherMode(mode) }
    }

    fun saveWeatherConfig(city: String, yandexApiKey: String) {
        viewModelScope.launch {
            preferencesRepository.setWeatherCity(city)
            preferencesRepository.setYandexWeatherApiKey(yandexApiKey)
            _statusMessage.value = "Настройки погоды сохранены"
            refreshWeather(manual = true)
        }
    }

    fun refreshWeather(manual: Boolean = true) {
        val currentSettings = settings.value
        val wifiConnected = wifiState.value.status == WifiConnectionStatus.CONNECTED
        lastWeatherFetchMs = System.currentTimeMillis()
        viewModelScope.launch {
            fetchWeatherInternal(
                city = currentSettings.weatherCity,
                yandexKey = currentSettings.yandexWeatherApiKey,
                mode = currentSettings.weatherMode,
                wifiConnected = wifiConnected
            )
            if (manual) {
                val w = _weatherState.value
                _statusMessage.value = "${w.cityName}: ${w.temperatureC}°C (${w.sourceLabel})"
            }
        }
    }

    fun openYandexWeather() {
        val w = _weatherState.value
        appRepository.openYandexWeather(w.latitude, w.longitude)
            .onFailure {
                _statusMessage.value = "Не удалось открыть Яндекс.Погоду"
            }
    }

    private suspend fun fetchWeatherInternal(
        city: String,
        yandexKey: String,
        mode: WeatherSourceMode,
        wifiConnected: Boolean
    ) {
        _weatherState.update { it.copy(isLoading = true) }
        val result = WeatherRepository.fetchWeather(
            cityQuery = city,
            yandexApiKey = yandexKey,
            mode = mode,
            wifiConnected = wifiConnected
        )
        _weatherState.value = result
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appRepo = AppRepository(application)
            val prefsRepo = LauncherPreferencesRepository(application)
            return LauncherViewModel(application, appRepo, prefsRepo) as T
        }
    }
}
