package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.LauncherPreferencesRepository
import com.example.media.MediaIntegrationManager
import com.example.model.AppCategory
import com.example.model.HavalCarAction
import com.example.model.InstalledApp
import com.example.model.LauncherDestination
import com.example.model.LauncherSettings
import com.example.model.MediaBrowseItem
import com.example.model.MediaPlayerState
import com.example.model.WifiState
import com.example.network.WifiMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(
    application: Application,
    private val appRepository: AppRepository,
    private val preferencesRepository: LauncherPreferencesRepository
) : AndroidViewModel(application) {

    private val mediaManager = MediaIntegrationManager(application, viewModelScope)
    private val wifiMonitor = WifiMonitor(application)

    private val _destination = MutableStateFlow(LauncherDestination.HOME)
    val destination: StateFlow<LauncherDestination> = _destination.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AppCategory.ALL)
    val selectedCategory: StateFlow<AppCategory> = _selectedCategory.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _rawApps = MutableStateFlow<List<InstalledApp>>(emptyList())

    val settings: StateFlow<LauncherSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LauncherSettings()
        )

    val mediaState: StateFlow<MediaPlayerState> = mediaManager.state
    val wifiState: StateFlow<WifiState> = wifiMonitor.wifiState

    val havalActions: List<HavalCarAction> = appRepository.getHavalCarActions()

    private val installedApps: StateFlow<List<InstalledApp>> = combine(
        _rawApps,
        settings.map { it.pinnedPackages }.distinctUntilChanged()
    ) { apps, pinnedList ->
        val pinnedSet = pinnedList.toSet()
        apps.map { app ->
            val shouldPin = pinnedSet.contains(app.packageName)
            if (app.isPinned == shouldPin) app else app.copy(isPinned = shouldPin)
        }.sortedWith(
            compareByDescending<InstalledApp> { it.packageName == "ru.yandex.music" }
                .thenByDescending { it.isPinned }
                .thenBy { it.label.lowercase() }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pinnedApps: StateFlow<List<InstalledApp>> = combine(
        installedApps,
        settings.map { it.pinnedPackages }.distinctUntilChanged()
    ) { apps, pinnedList ->
        val byPkg = apps.associateBy { it.packageName }
        pinnedList.mapNotNull { pkg -> byPkg[pkg] }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredApps: StateFlow<List<InstalledApp>> = combine(
        installedApps,
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
            _rawApps.value = appRepository.loadInstalledApps(settings.value.pinnedPackages.toSet())
        }
        viewModelScope.launch {
            appRepository.observePackageChanges().collect {
                _rawApps.value = appRepository.loadInstalledApps(settings.value.pinnedPackages.toSet())
                mediaManager.refreshYandexPackageStatus()
            }
        }
    }

    fun onStart() {
        mediaManager.start()
        wifiMonitor.start()
    }

    fun onStop() {
        mediaManager.stop()
        wifiMonitor.stop()
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

    fun togglePinApp(app: InstalledApp) {
        viewModelScope.launch {
            preferencesRepository.togglePinnedPackage(app.packageName)
            val msg = if (app.isPinned) {
                "Убрано с главного экрана: ${app.label}"
            } else {
                "Закреплено на главном экране: ${app.label}"
            }
            _statusMessage.value = msg
        }
    }

    fun launchApp(app: InstalledApp) {
        appRepository.launchApp(app.packageName, app.activityName)
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Не удалось открыть ${app.label}"
            }
    }

    fun launchCarAction(action: HavalCarAction) {
        appRepository.launchCarAction(action)
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Не удалось открыть ${action.title}"
            }
    }

    fun launchUsbMedia() {
        launchCarAction(appRepository.getUsbMediaAction())
    }

    fun launchRadio() {
        val radioAction = havalActions.firstOrNull { it.id == "radio" }
        if (radioAction != null) {
            launchCarAction(radioAction)
        }
    }

    fun launchPhone() {
        launchCarAction(appRepository.getPhoneAction())
    }

    fun openWifiSettings() {
        appRepository.openWifiSettings()
            .onFailure {
                _statusMessage.value = "Откройте настройки Wi-Fi вручную"
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
        mediaManager.openYandexDeepLink("yandexmusic://radio", "Моя волна")
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Яндекс.Музыка не найдена"
            }
    }

    fun openYandexCollection() {
        mediaManager.openYandexDeepLink("yandexmusic://collection")
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Яндекс.Музыка не найдена"
            }
    }

    fun openYandexSearch() {
        mediaManager.openYandexDeepLink("yandexmusic://search", "")
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Яндекс.Музыка не найдена"
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

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appRepo = AppRepository(application)
            val prefsRepo = LauncherPreferencesRepository(application)
            return LauncherViewModel(application, appRepo, prefsRepo) as T
        }
    }
}
