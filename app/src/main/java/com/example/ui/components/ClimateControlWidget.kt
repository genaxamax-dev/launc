package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.car.ClimateController
import com.example.model.ClimateState
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.CockpitHairline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceBright
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.HavalAmber
import com.example.ui.theme.HavalAmberBright
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ClimateControlWidget(
    climateState: ClimateState,
    reduceAnimations: Boolean,
    onIncreaseTemp: () -> Unit,
    onDecreaseTemp: () -> Unit,
    onToggleAc: () -> Unit,
    onOpenClimateApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetAccent = when {
        climateState.targetTempC <= 18.5f -> TelemetryCyan
        climateState.targetTempC >= 25.0f -> HavalAmberBright
        else -> TextPrimary
    }

    val tempColor by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = if (reduceAnimations) 0 else 180),
        label = "climate_temp_color"
    )

    val tempFraction = ((climateState.targetTempC - ClimateController.MIN_TEMP_C) /
        (ClimateController.MAX_TEMP_C - ClimateController.MIN_TEMP_C))
        .coerceIn(0.05f, 1f)

    val canDecrease = climateState.targetTempC > ClimateController.MIN_TEMP_C
    val canIncrease = climateState.targetTempC < ClimateController.MAX_TEMP_C

    Surface(
        modifier = modifier
            .height(56.dp)
            .testTag("climate_control_widget"),
        shape = RoundedCornerShape(16.dp),
        color = CockpitSurface.copy(alpha = 0.88f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    TelemetryCyan.copy(alpha = 0.45f),
                    CockpitHairline,
                    HavalAmber.copy(alpha = 0.45f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onDecreaseTemp,
                enabled = canDecrease,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("climate_temp_down_button"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (canDecrease) TelemetryCyan.copy(alpha = 0.16f) else CockpitSurfaceElevated,
                    contentColor = if (canDecrease) TelemetryCyan else TextMuted,
                    disabledContainerColor = CockpitSurfaceElevated.copy(alpha = 0.4f),
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Уменьшить температуру",
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenClimateApp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = formatTemperature(climateState.targetTempC),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 23.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = tempColor,
                    modifier = Modifier.testTag("climate_current_temp_text")
                )

                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CockpitSurfaceBright)
                ) {
                    Box(
                        modifier = Modifier
                            .width(68.dp * tempFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(TelemetryCyan, ActiveEmerald, HavalAmber)
                                )
                            )
                    )
                }
            }

            IconButton(
                onClick = onIncreaseTemp,
                enabled = canIncrease,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("climate_temp_up_button"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (canIncrease) HavalAmber.copy(alpha = 0.18f) else CockpitSurfaceElevated,
                    contentColor = if (canIncrease) HavalAmberBright else TextMuted,
                    disabledContainerColor = CockpitSurfaceElevated.copy(alpha = 0.4f),
                    disabledContentColor = TextMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Увеличить температуру",
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onToggleAc,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("climate_ac_toggle_button"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (climateState.isAcOn) TelemetryCyan.copy(alpha = 0.18f) else CockpitSurfaceElevated,
                    contentColor = if (climateState.isAcOn) TelemetryCyan else TextMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.AcUnit,
                    contentDescription = if (climateState.isAcOn) "Выключить A/C" else "Включить A/C",
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenClimateApp,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("climate_open_full_button"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = CockpitSurfaceElevated.copy(alpha = 0.85f),
                    contentColor = TextSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Открыть полный экран климата",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatTemperature(tempC: Float): String {
    return when {
        tempC <= ClimateController.MIN_TEMP_C -> "16.0°"
        tempC >= ClimateController.MAX_TEMP_C -> "32.0°"
        else -> String.format(Locale.US, "%.1f°", tempC)
    }
}
