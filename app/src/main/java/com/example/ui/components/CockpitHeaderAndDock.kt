package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.LauncherDestination
import com.example.model.UpdateCheckState
import com.example.model.WifiConnectionStatus
import com.example.model.WifiState
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.CockpitHairline
import com.example.ui.theme.CockpitObsidian
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.HavalAmber
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YandexGold
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CockpitTopBar(
    activeSourceLabel: String,
    wifiState: WifiState,
    updateState: UpdateCheckState,
    showSeconds: Boolean,
    onWifiClick: () -> Unit,
    onUpdateClick: (UpdateCheckState.UpdateAvailable) -> Unit,
    onCycleDim: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    if (showSeconds) {
        LaunchedEffect(Unit) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1000L)
            }
        }
    } else {
        DisposableEffect(context) {
            nowMillis = System.currentTimeMillis()
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    nowMillis = System.currentTimeMillis()
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }

    val timePattern = if (showSeconds) "HH:mm:ss" else "HH:mm"
    val minuteKey = if (showSeconds) nowMillis / 1000L else nowMillis / 60_000L
    val timeText = remember(minuteKey, timePattern) {
        SimpleDateFormat(timePattern, Locale("ru", "RU")).format(Date(nowMillis))
    }
    val dateText = remember(nowMillis / 3_600_000L) {
        SimpleDateFormat("EEE, d MMMM", Locale("ru", "RU")).format(Date(nowMillis))
            .replaceFirstChar { it.uppercaseChar() }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Digital Clock + Date + Haval Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary
            )

            Column {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "HAVAL M6",
                        style = MaterialTheme.typography.labelLarge,
                        color = HavalAmber
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = activeSourceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = TelemetryCyan
                    )
                }
            }
        }

        // Right: Update badge + Wi-Fi status pill + Cockpit Utility Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (updateState is UpdateCheckState.UpdateAvailable) {
                Surface(
                    onClick = { onUpdateClick(updateState) },
                    modifier = Modifier
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("top_update_pill"),
                    shape = RoundedCornerShape(14.dp),
                    color = HavalAmber.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, HavalAmber)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = "Доступно обновление",
                            tint = HavalAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Обновление ${updateState.latestVersion}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            WifiStatusPill(
                wifiState = wifiState,
                onClick = onWifiClick
            )

            IconButton(
                onClick = onCycleDim,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("top_dim_button"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = CockpitSurface.copy(alpha = 0.88f),
                    contentColor = HavalAmber
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Brightness6,
                    contentDescription = "Яркость фона"
                )
            }

            IconButton(
                onClick = onOpenApps,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("top_apps_button"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = CockpitSurface.copy(alpha = 0.88f),
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = "Все приложения"
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("top_settings_button"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = CockpitSurface.copy(alpha = 0.88f),
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Настройки лаунчера"
                )
            }
        }
    }
}

@Composable
private fun WifiStatusPill(
    wifiState: WifiState,
    onClick: () -> Unit
) {
    val (icon, tint, label) = when (wifiState.status) {
        WifiConnectionStatus.CONNECTED -> Triple(
            Icons.Rounded.Wifi,
            ActiveEmerald,
            if (wifiState.linkSpeedMbps > 0) "Wi-Fi • ${wifiState.linkSpeedMbps} Мбит/с" else "Wi-Fi подключён"
        )
        WifiConnectionStatus.ENABLED_NO_CONNECTION -> Triple(
            Icons.Rounded.Wifi,
            HavalAmber,
            "Wi-Fi • нет сети"
        )
        WifiConnectionStatus.DISABLED -> Triple(
            Icons.Rounded.WifiOff,
            TextMuted,
            "Wi-Fi выкл"
        )
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .testTag("top_wifi_pill"),
        shape = RoundedCornerShape(14.dp),
        color = CockpitSurface.copy(alpha = 0.88f),
        border = BorderStroke(
            width = 1.dp,
            color = if (wifiState.status == WifiConnectionStatus.CONNECTED) {
                ActiveEmerald.copy(alpha = 0.45f)
            } else {
                CockpitHairline
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (wifiState.status == WifiConnectionStatus.DISABLED) TextSecondary else TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CockpitBottomDock(
    currentDestination: LauncherDestination,
    onHomeClick: () -> Unit,
    onYandexMusicClick: () -> Unit,
    onAndroidAutoClick: () -> Unit,
    onClimateClick: () -> Unit,
    onRadioClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .testTag("cockpit_bottom_dock"),
        shape = RoundedCornerShape(18.dp),
        color = CockpitSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, CockpitHairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockActionButton(
                label = "Главная",
                icon = Icons.Rounded.Home,
                isActive = currentDestination == LauncherDestination.HOME,
                accentColor = HavalAmber,
                testTag = "dock_home_button",
                onClick = onHomeClick,
                modifier = Modifier.weight(1f)
            )
            DockActionButton(
                label = "Я.Музыка",
                icon = Icons.Rounded.LibraryMusic,
                isActive = false,
                accentColor = YandexGold,
                testTag = "dock_yandex_music_button",
                onClick = onYandexMusicClick,
                modifier = Modifier.weight(1f)
            )
            DockActionButton(
                label = "Android Auto",
                icon = Icons.Rounded.DirectionsCar,
                isActive = false,
                accentColor = TelemetryCyan,
                testTag = "dock_android_auto_button",
                onClick = onAndroidAutoClick,
                modifier = Modifier.weight(1f)
            )
            DockActionButton(
                label = "Климат",
                icon = Icons.Rounded.AcUnit,
                isActive = false,
                accentColor = TelemetryCyan,
                testTag = "dock_climate_button",
                onClick = onClimateClick,
                modifier = Modifier.weight(1f)
            )
            DockActionButton(
                label = "Радио",
                icon = Icons.Rounded.Radio,
                isActive = false,
                accentColor = HavalAmber,
                testTag = "dock_radio_button",
                onClick = onRadioClick,
                modifier = Modifier.weight(1f)
            )
            DockActionButton(
                label = "Телефон",
                icon = Icons.Rounded.Phone,
                isActive = false,
                accentColor = ActiveEmerald,
                testTag = "dock_phone_button",
                onClick = onPhoneClick,
                modifier = Modifier.weight(1f)
            )
            DockActionButton(
                label = "Приложения",
                icon = Icons.Rounded.Apps,
                isActive = currentDestination == LauncherDestination.APPS,
                accentColor = TextPrimary,
                testTag = "dock_apps_button",
                onClick = onAppsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DockActionButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) accentColor else CockpitSurfaceElevated.copy(alpha = 0.88f),
        border = if (isActive) null else BorderStroke(1.dp, CockpitHairline.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) CockpitObsidian else accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) CockpitObsidian else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
