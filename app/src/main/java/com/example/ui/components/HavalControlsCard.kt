package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CarActionKind
import com.example.model.HavalCarAction
import com.example.model.InstalledApp
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.CockpitHairline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.HavalAmber
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YandexGold
import com.example.ui.theme.YandexRed

@Composable
fun HavalControlsAndPinnedPanel(
    havalActions: List<HavalCarAction>,
    pinnedApps: List<InstalledApp>,
    onCarActionClick: (HavalCarAction) -> Unit,
    onPinnedAppClick: (InstalledApp) -> Unit,
    onOpenAppsDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Haval M6 Vehicle Systems Panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.25f),
            shape = RoundedCornerShape(24.dp),
            color = CockpitSurface.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, CockpitHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "СИСТЕМЫ HAVAL M6",
                        style = MaterialTheme.typography.labelLarge,
                        color = HavalAmber
                    )
                    Text(
                        text = "Adayo MEK-MX8Q",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                // 2 rows x 3 columns of big automotive tiles
                val rows = havalActions.chunked(3)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { action ->
                                HavalSystemTile(
                                    action = action,
                                    onClick = { onCarActionClick(action) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pinned Apps Quick Strip
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.75f),
            shape = RoundedCornerShape(24.dp),
            color = CockpitSurface.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, CockpitHairline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ИЗБРАННЫЕ ПРИЛОЖЕНИЯ",
                        style = MaterialTheme.typography.labelLarge,
                        color = TelemetryCyan
                    )
                    Surface(
                        onClick = onOpenAppsDrawer,
                        shape = RoundedCornerShape(8.dp),
                        color = CockpitSurfaceElevated,
                        modifier = Modifier.testTag("customize_pinned_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Настроить избранное",
                                tint = TelemetryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Настроить",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }

                if (pinnedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Откройте «Приложения» и закрепите нужные программы на главный экран",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(pinnedApps, key = { it.packageName }) { app ->
                            PinnedAppTile(
                                app = app,
                                onClick = { onPinnedAppClick(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HavalSystemTile(
    action: HavalCarAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, accent) = iconAndAccentForKind(action.kind)
    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("car_action_${action.id}"),
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurfaceElevated.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = action.title,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = action.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PinnedAppTile(
    app: InstalledApp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(104.dp)
            .minimumInteractiveComponentSize()
            .testTag("pinned_app_${app.packageName}"),
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurfaceElevated.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, CockpitHairline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DrawableAppIcon(
                drawable = app.icon,
                contentDescription = app.label,
                size = 44.dp,
                cornerRadius = 12.dp
            )
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun iconAndAccentForKind(kind: CarActionKind): Pair<ImageVector, Color> = when (kind) {
    CarActionKind.CLIMATE -> Icons.Rounded.AcUnit to TelemetryCyan
    CarActionKind.RADIO -> Icons.Rounded.Radio to HavalAmber
    CarActionKind.AVM_360 -> Icons.Rounded.SurroundSound to ActiveEmerald
    CarActionKind.DVR -> Icons.Rounded.Videocam to YandexRed
    CarActionKind.BLUETOOTH -> Icons.Rounded.BluetoothAudio to TelemetryCyan
    CarActionKind.CAR_SETTINGS -> Icons.Rounded.DirectionsCar to YandexGold
    CarActionKind.USB_MEDIA -> Icons.Rounded.Usb to HavalAmber
    CarActionKind.PHONE -> Icons.Rounded.Phone to ActiveEmerald
}
