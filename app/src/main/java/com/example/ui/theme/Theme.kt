package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SovereignDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF002629),
    primaryContainer = Color(0xFF003D42),
    onPrimaryContainer = ElectricCyan,
    secondary = AmberPulse,
    onSecondary = Color(0xFF2B1D00),
    secondaryContainer = Color(0xFF422D00),
    onSecondaryContainer = AmberPulse,
    tertiary = CrimsonNeon,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4D0019),
    onTertiaryContainer = Color(0xFFFFB2C5),
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    error = CrimsonNeon
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SovereignDarkColorScheme,
        typography = Typography,
        content = content
    )
}
