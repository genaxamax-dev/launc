package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.model.HeadlightMode
import com.example.model.LauncherSettings
import com.example.model.MediaPlayerState
import com.example.model.UpdateCheckState
import com.example.model.WeatherSourceMode
import com.example.model.WeatherState
import com.example.model.WifiConnectionStatus
import com.example.model.WifiState
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.CockpitHairline
import com.example.ui.theme.CockpitObsidian
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceBright
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.HavalAmber
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YandexGold

@Composable
fun LauncherSettingsScreen(
    settings: LauncherSettings,
    mediaState: MediaPlayerState,
    wifiState: WifiState,
    weatherState: WeatherState,
    headlightsOn: Boolean,
    updateState: UpdateCheckState,
    onWallpaperDimChange: (Float) -> Unit,
    onToggleLeftInset: (Boolean) -> Unit,
    onToggleTopInset: (Boolean) -> Unit,
    onToggleShowSeconds: (Boolean) -> Unit,
    onToggleLightweightBackground: (Boolean) -> Unit,
    onToggleReduceAnimations: (Boolean) -> Unit,
    onHeadlightModeChange: (HeadlightMode) -> Unit,
    onWeatherModeChange: (WeatherSourceMode) -> Unit,
    onSaveWeatherConfig: (String, String) -> Unit,
    onRefreshWeatherNow: () -> Unit,
    onOpenYandexWeather: () -> Unit,
    onSaveGithubRepo: (String) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckUpdatesNow: () -> Unit,
    onOpenUpdateLink: (String) -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onReconnectMediaBrowser: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showAdbDialog by remember { mutableStateOf(false) }
    var repoDraft by remember { mutableStateOf(settings.githubRepo) }
    var cityDraft by remember { mutableStateOf(settings.weatherCity) }
    var yandexKeyDraft by remember { mutableStateOf(settings.yandexWeatherApiKey) }

    LaunchedEffect(settings.githubRepo) {
        if (settings.githubRepo.isNotEmpty()) {
            repoDraft = settings.githubRepo
        }
    }
    LaunchedEffect(settings.weatherCity) {
        if (cityDraft.isEmpty() && settings.weatherCity.isNotEmpty()) {
            cityDraft = settings.weatherCity
        }
    }
    LaunchedEffect(settings.yandexWeatherApiKey) {
        if (yandexKeyDraft.isEmpty() && settings.yandexWeatherApiKey.isNotEmpty()) {
            yandexKeyDraft = settings.yandexWeatherApiKey
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        shape = RoundedCornerShape(24.dp),
        color = CockpitSurface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, CockpitHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("settings_back_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = CockpitSurfaceElevated,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад"
                    )
                }

                Column {
                    Text(
                        text = "Настройки M6 Launcher",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Версия v${BuildConfig.VERSION_NAME} • Обновления GitHub, оптимизация и диагностика",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Section 1: GitHub Auto Update
            SettingsCard(title = "АВТООБНОВЛЕНИЕ С GITHUB") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = repoDraft,
                            onValueChange = { repoDraft = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_github_repo_input"),
                            label = { Text("Репозиторий (owner/repo или URL)") },
                            placeholder = { Text("например: genaxamax/haval-m6-launcher") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HavalAmber,
                                unfocusedBorderColor = CockpitHairline,
                                focusedContainerColor = CockpitSurface,
                                unfocusedContainerColor = CockpitSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = HavalAmber,
                                unfocusedLabelColor = TextSecondary
                            )
                        )

                        Button(
                            onClick = { onSaveGithubRepo(repoDraft) },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("settings_save_repo_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HavalAmber,
                                contentColor = CockpitObsidian
                            )
                        ) {
                            Text("Сохранить")
                        }
                    }

                    SettingsSwitchRow(
                        title = "Автопроверка обновлений по Wi-Fi",
                        subtitle = "Фоновая проверка новых релизов на GitHub при подключении к сети (раз в 6 часов)",
                        checked = settings.autoCheckUpdates,
                        testTag = "settings_auto_update_switch",
                        onCheckedChange = onToggleAutoCheckUpdates
                    )

                    val (statusText, statusColor) = when (updateState) {
                        UpdateCheckState.Idle -> {
                            if (settings.githubRepo.isBlank()) {
                                "Укажите репозиторий GitHub" to TextMuted
                            } else {
                                "Готово к проверке (${settings.githubRepo})" to TextSecondary
                            }
                        }
                        UpdateCheckState.RepoNotConfigured -> "Репозиторий GitHub не задан" to HavalAmber
                        UpdateCheckState.Checking -> "Проверка релизов на GitHub…" to TelemetryCyan
                        is UpdateCheckState.UpToDate -> "Актуальная версия v${updateState.currentVersion}" to ActiveEmerald
                        is UpdateCheckState.UpdateAvailable -> "Доступна версия ${updateState.latestVersion}" to HavalAmber
                        is UpdateCheckState.Error -> updateState.message to HavalAmber
                    }

                    DiagnosticRow(
                        label = "Текущая версия: v${BuildConfig.VERSION_NAME}",
                        value = statusText,
                        accent = statusColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCheckUpdatesNow,
                            enabled = updateState !is UpdateCheckState.Checking,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("settings_check_update_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelemetryCyan,
                                contentColor = CockpitObsidian
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Проверить сейчас")
                        }

                        if (updateState is UpdateCheckState.UpdateAvailable) {
                            Button(
                                onClick = {
                                    onOpenUpdateLink(updateState.apkUrl ?: updateState.releaseUrl)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("settings_download_update_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ActiveEmerald,
                                    contentColor = CockpitObsidian
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Скачать ${updateState.latestVersion}")
                            }
                        }
                    }
                }
            }

            // Section 2: Headlights & Weather
            SettingsCard(title = "ФАРЫ АВТО И ПОГОДА") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DiagnosticRow(
                        label = "Фары Haval M6 сейчас",
                        value = if (headlightsOn) "Включены (вечер/ночь)" else "Выключены (день)",
                        accent = if (headlightsOn) YandexGold else TelemetryCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeadlightMode.entries.forEach { mode ->
                            val selected = settings.headlightMode == mode
                            OutlinedButton(
                                onClick = { onHeadlightModeChange(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("settings_headlight_${mode.name.lowercase()}"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) HavalAmber else CockpitHairline
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) HavalAmber.copy(alpha = 0.2f) else CockpitSurface
                                )
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (selected) HavalAmber else TextPrimary
                                )
                            }
                        }
                    }

                    SettingsSwitchRow(
                        title = "Только погода по времени года (офлайн)",
                        subtitle = "Если выключено — берётся онлайн-прогноз (или Яндекс.Погода при наличии API-ключа) с резервом по сезону",
                        checked = settings.weatherMode == WeatherSourceMode.SEASONAL_ONLY,
                        testTag = "settings_seasonal_weather_switch",
                        onCheckedChange = { seasonalOnly ->
                            onWeatherModeChange(
                                if (seasonalOnly) WeatherSourceMode.SEASONAL_ONLY
                                else WeatherSourceMode.AUTO_NETWORK_OR_SEASON
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = cityDraft,
                            onValueChange = { cityDraft = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_weather_city_input"),
                            label = { Text("Город (пусто = авто по IP)") },
                            placeholder = { Text("например: Москва") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TelemetryCyan,
                                unfocusedBorderColor = CockpitHairline,
                                focusedContainerColor = CockpitSurface,
                                unfocusedContainerColor = CockpitSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = TelemetryCyan,
                                unfocusedLabelColor = TextSecondary
                            )
                        )

                        OutlinedTextField(
                            value = yandexKeyDraft,
                            onValueChange = { yandexKeyDraft = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_yandex_weather_key_input"),
                            label = { Text("Ключ API Яндекс.Погоды (опц.)") },
                            placeholder = { Text("X-Yandex-Weather-Key") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YandexGold,
                                unfocusedBorderColor = CockpitHairline,
                                focusedContainerColor = CockpitSurface,
                                unfocusedContainerColor = CockpitSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = YandexGold,
                                unfocusedLabelColor = TextSecondary
                            )
                        )

                        Button(
                            onClick = { onSaveWeatherConfig(cityDraft, yandexKeyDraft) },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("settings_save_weather_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelemetryCyan,
                                contentColor = CockpitObsidian
                            )
                        ) {
                            Text("Сохранить")
                        }
                    }

                    DiagnosticRow(
                        label = "Текущая погода (${weatherState.sourceLabel})",
                        value = "${weatherState.cityName}: ${weatherState.temperatureC}°C • ${weatherState.conditionLabel}",
                        accent = ActiveEmerald
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRefreshWeatherNow,
                            enabled = !weatherState.isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("settings_refresh_weather_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CockpitSurfaceBright,
                                contentColor = TelemetryCyan
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Обновить погоду")
                        }

                        Button(
                            onClick = onOpenYandexWeather,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("settings_open_yandex_weather_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YandexGold.copy(alpha = 0.2f),
                                contentColor = YandexGold
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WbSunny,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Яндекс.Погода")
                        }
                    }
                }
            }

            // Section 3: Performance Optimization for Weak Hardware
            SettingsCard(title = "ОПТИМИЗАЦИЯ ДЛЯ СЛАБОГО ЖЕЛЕЗА (i.MX8Q)") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsSwitchRow(
                        title = "Облегчённый фон без фото",
                        subtitle = "Отключает фоновую картинку и заменяет её чистым карбоновым градиентом (экономия ОЗУ и GPU)",
                        checked = settings.lightweightBackground,
                        testTag = "settings_lightweight_bg_switch",
                        onCheckedChange = onToggleLightweightBackground
                    )

                    SettingsSwitchRow(
                        title = "Минимальные анимации",
                        subtitle = "Убирает фоновые анимации эквалайзера и плавные переходы между экранами",
                        checked = settings.reduceAnimations,
                        testTag = "settings_reduce_anim_switch",
                        onCheckedChange = onToggleReduceAnimations
                    )
                }
            }

            // Section 3: Display & Haval M6 Safe Area
            SettingsCard(title = "ЭКРАН И ОТСТУПЫ СИСТЕМНЫХ ПАНЕЛЕЙ") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!settings.lightweightBackground) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Затемнение фонового фото Haval M6",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${(settings.wallpaperDim * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = HavalAmber
                                )
                            }
                            Slider(
                                value = settings.wallpaperDim,
                                onValueChange = onWallpaperDimChange,
                                valueRange = 0.15f..0.85f,
                                modifier = Modifier.testTag("settings_dim_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = HavalAmber,
                                    activeTrackColor = HavalAmber,
                                    inactiveTrackColor = CockpitSurfaceBright
                                )
                            )
                        }
                    }

                    SettingsSwitchRow(
                        title = "Резервировать 120 px слева (NavigationBar ГУ)",
                        subtitle = "На широком экране 1920×720 предотвращает перекрытие кнопок Назад / Домой / Недавние",
                        checked = settings.enforceHavalLeftInset,
                        testTag = "settings_left_inset_switch",
                        onCheckedChange = onToggleLeftInset
                    )

                    SettingsSwitchRow(
                        title = "Резервировать 80 px сверху (StatusBar ГУ)",
                        subtitle = "Исключает наложение верхнего системного статус-бара Adayo на часы лаунчера",
                        checked = settings.enforceHavalTopInset,
                        testTag = "settings_top_inset_switch",
                        onCheckedChange = onToggleTopInset
                    )

                    SettingsSwitchRow(
                        title = "Показывать секунды в часах",
                        subtitle = "Выключение секунд снижает нагрузку на процессор (обновление раз в минуту)",
                        checked = settings.showSecondsInClock,
                        testTag = "settings_seconds_switch",
                        onCheckedChange = onToggleShowSeconds
                    )
                }
            }

            // Section 4: Wi-Fi & Yandex Music Diagnostics
            SettingsCard(title = "СЕТЬ WI-FI И ИНТЕГРАЦИЯ ЯНДЕКС.МУЗЫКИ") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val (wifiText, wifiColor) = when (wifiState.status) {
                        WifiConnectionStatus.CONNECTED -> {
                            val speedPart = if (wifiState.linkSpeedMbps > 0) " (${wifiState.linkSpeedMbps} Мбит/с)" else ""
                            "Подключён$speedPart" to ActiveEmerald
                        }
                        WifiConnectionStatus.ENABLED_NO_CONNECTION -> "Включён, нет подключения" to HavalAmber
                        WifiConnectionStatus.DISABLED -> "Отключён" to TextSecondary
                    }
                    DiagnosticRow(
                        label = "Состояние Wi-Fi",
                        value = wifiText,
                        accent = wifiColor
                    )
                    DiagnosticRow(
                        label = "Пакет ru.yandex.music",
                        value = if (mediaState.isYandexMusicInstalled) {
                            "Установлен (v${mediaState.yandexMusicVersion ?: "?"})"
                        } else {
                            "Не найден"
                        },
                        accent = if (mediaState.isYandexMusicInstalled) ActiveEmerald else HavalAmber
                    )
                    DiagnosticRow(
                        label = "Доступ к уведомлениям",
                        value = if (mediaState.isNotificationListenerConnected) {
                            "Включён (MediaSession)"
                        } else {
                            "Не выдан (нажмите кнопку ниже)"
                        },
                        accent = if (mediaState.isNotificationListenerConnected) ActiveEmerald else HavalAmber
                    )
                    DiagnosticRow(
                        label = "Режим управления",
                        value = mediaState.diagnosticStatus,
                        accent = TelemetryCyan
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenNotificationAccess,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("settings_notification_access_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (mediaState.isNotificationListenerConnected) ActiveEmerald else HavalAmber
                            )
                        ) {
                            Text("Доступ к уведомлениям", color = TextPrimary)
                        }

                        OutlinedButton(
                            onClick = onOpenWifiSettings,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("settings_wifi_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, TelemetryCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = TelemetryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Сети Wi-Fi", color = TextPrimary)
                        }

                        Button(
                            onClick = onReconnectMediaBrowser,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("reconnect_media_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YandexGold,
                                contentColor = CockpitObsidian
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Обновить сессию")
                        }

                        OutlinedButton(
                            onClick = onOpenHomeSettings,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("home_settings_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, HavalAmber)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = null,
                                tint = HavalAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Домашний экран", color = TextPrimary)
                        }
                    }
                }
            }

            // Section 5: Haval M6 ADB Installation & Rollback Reference
            SettingsCard(title = "УСТАНОВКА НА ГОЛОВНОЕ УСТРОЙСТВО HAVAL M6") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Шпаргалка ADB для MEK-MX8Q",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Обход фильтра APK (service call package 195), назначение HOME и откат на штатный com.adayo.launcher",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { showAdbDialog = true },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_adb_guide_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CockpitSurfaceBright,
                            contentColor = TelemetryCyan
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Команды ADB")
                    }
                }
            }
        }
    }

    if (showAdbDialog) {
        AlertDialog(
            onDismissRequest = { showAdbDialog = false },
            containerColor = CockpitSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text("Команды установки на Haval M6")
            },
            text = {
                Text(
                    text = """
                        1. Установка APK (обход фильтра прошивки):
                        adb shell service call package 195 i32 1
                        adb install -r -g app-debug.apk

                        2. Выдать доступ к уведомлениям (для Яндекс.Музыки):
                        adb shell cmd notification allow_listener com.aistudio.havalm6launcher.kqtxzn/com.example.media.YandexNotificationListener

                        3. Отключение штатного лаунчера и выбор домашним:
                        adb shell pm disable-user --user 0 com.adayo.launcher
                        adb shell cmd package set-home-activity com.aistudio.havalm6launcher.kqtxzn/com.example.MainActivity

                        4. Откат на штатный экран Adayo:
                        adb shell pm enable com.adayo.launcher
                        adb shell cmd package set-home-activity com.adayo.launcher/.ui.Launcher_MainActivity
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showAdbDialog = false }) {
                    Text("Понятно", color = HavalAmber)
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CockpitSurfaceElevated.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, CockpitHairline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = HavalAmber
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = CockpitObsidian,
                checkedTrackColor = HavalAmber,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CockpitSurfaceBright
            )
        )
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
    }
}
