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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.LauncherSettings
import com.example.model.MediaPlayerState
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YandexGold

@Composable
fun LauncherSettingsScreen(
    settings: LauncherSettings,
    mediaState: MediaPlayerState,
    wifiState: WifiState,
    onWallpaperDimChange: (Float) -> Unit,
    onToggleLeftInset: (Boolean) -> Unit,
    onToggleTopInset: (Boolean) -> Unit,
    onToggleShowSeconds: (Boolean) -> Unit,
    onToggleLightweightBackground: (Boolean) -> Unit,
    onToggleReduceAnimations: (Boolean) -> Unit,
    onOpenWifiSettings: () -> Unit,
    onReconnectMediaBrowser: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showAdbDialog by remember { mutableStateOf(false) }

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
                        text = "Производительность, сеть Wi-Fi, отступы 1920×720 и Яндекс.Музыка",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Section 1: Performance Optimization for Weak Hardware
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

            // Section 2: Display & Haval M6 Safe Area
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

            // Section 3: Wi-Fi & Yandex Music Diagnostics
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
                            Text("Переподключить плеер")
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

            // Section 4: Haval M6 ADB Installation & Rollback Reference
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
                        adb install -r -g app-release.apk

                        2. Отключение штатного лаунчера и выбор домашним:
                        adb shell pm disable-user --user 0 com.adayo.launcher
                        adb shell cmd package set-home-activity com.aistudio.havalm6launcher.kqtxzn/com.example.MainActivity

                        3. Откат на штатный экран Adayo:
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
