package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CockpitColorScheme = darkColorScheme(
    primary = HavalAmber,
    onPrimary = CockpitObsidian,
    primaryContainer = CockpitSurfaceBright,
    onPrimaryContainer = HavalAmberBright,
    secondary = TelemetryCyan,
    onSecondary = CockpitObsidian,
    secondaryContainer = CockpitSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = YandexGold,
    onTertiary = CockpitObsidian,
    background = CockpitObsidian,
    onBackground = TextPrimary,
    surface = CockpitSurface,
    onSurface = TextPrimary,
    surfaceVariant = CockpitSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CockpitHairline,
    outlineVariant = CockpitSurfaceBright,
    error = YandexRed,
    onError = TextPrimary
)

val CockpitShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun M6LauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CockpitColorScheme,
        typography = Typography,
        shapes = CockpitShapes,
        content = content
    )
}
