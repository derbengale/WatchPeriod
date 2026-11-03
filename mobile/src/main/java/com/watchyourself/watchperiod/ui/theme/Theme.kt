package com.watchyourself.watchperiod.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Rose,
    onPrimary = Color.White,
    primaryContainer = RoseSoft,
    onPrimaryContainer = RoseDeep,
    secondary = Lavender,
    onSecondary = Color.White,
    secondaryContainer = LavenderSoft,
    onSecondaryContainer = Color(0xFF26124D),
    tertiary = Mint,
    onTertiary = Color.White,
    tertiaryContainer = MintSoft,
    onTertiaryContainer = Color(0xFF0C3A29),
    background = Cream,
    onBackground = Plum,
    surface = Color.White,
    onSurface = Plum,
    surfaceVariant = Color(0xFFF7EAEE),
    onSurfaceVariant = Color(0xFF705661),
    outline = Color(0xFFB59AA4),
    outlineVariant = Color(0xFFE8D7DD),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C3),
    onPrimary = Color(0xFF54102A),
    primaryContainer = Color(0xFF70243C),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFC9B8FF),
    onSecondary = Color(0xFF2D1A5C),
    secondaryContainer = Color(0xFF43307A),
    onSecondaryContainer = Color(0xFFE7E0FF),
    tertiary = Color(0xFF82DDB5),
    onTertiary = Color(0xFF003826),
    tertiaryContainer = Color(0xFF1A5D43),
    onTertiaryContainer = Color(0xFFD8F5E7),
    background = Color(0xFF161013),
    onBackground = Color(0xFFF3DFE5),
    surface = Color(0xFF211A1E),
    onSurface = Color(0xFFF3DFE5),
    surfaceVariant = Color(0xFF3A2C33),
    onSurfaceVariant = Color(0xFFD9C2CA),
    outline = Color(0xFFA98C96),
    outlineVariant = Color(0xFF4D3C44),
    error = Color(0xFFFFB4AB),
)

@Composable
fun WatchPeriodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}