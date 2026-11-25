package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CleanovaBlue,
    onPrimary = Color.White,
    primaryContainer = CleanovaSkyContainer,
    onPrimaryContainer = Color(0xFF00326B),
    secondary = CleanovaMint,
    onSecondary = Color.White,
    secondaryContainer = CleanovaMintContainer,
    onSecondaryContainer = Color(0xFF0A4717),
    tertiary = CleanovaAmber,
    onTertiary = Color.White,
    tertiaryContainer = CleanovaAmberContainer,
    onTertiaryContainer = Color(0xFF5E3800),
    error = CleanovaCoral,
    onError = Color.White,
    errorContainer = CleanovaCoralContainer,
    onErrorContainer = Color(0xFF6B0C06),
    background = CleanovaLightBg,
    onBackground = CleanovaLightText,
    surface = CleanovaLightSurface,
    onSurface = CleanovaLightText,
    surfaceVariant = CleanovaLightSurfaceVariant,
    onSurfaceVariant = CleanovaLightTextSecondary,
    outline = CleanovaLightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = CleanovaBlueBright,
    onPrimary = Color.White,
    primaryContainer = CleanovaSkyContainerDark,
    onPrimaryContainer = Color(0xFFB3D7FF),
    secondary = CleanovaMint,
    onSecondary = Color(0xFF002107),
    secondaryContainer = CleanovaMintContainerDark,
    onSecondaryContainer = Color(0xFFA8F2B5),
    tertiary = CleanovaAmber,
    onTertiary = Color(0xFF2E1B00),
    tertiaryContainer = CleanovaAmberContainerDark,
    onTertiaryContainer = Color(0xFFFFD89E),
    error = CleanovaCoral,
    onError = Color.White,
    errorContainer = CleanovaCoralContainerDark,
    onErrorContainer = Color(0xFFFFB4AE),
    background = CleanovaDarkBg,
    onBackground = CleanovaDarkText,
    surface = CleanovaDarkSurface,
    onSurface = CleanovaDarkText,
    surfaceVariant = CleanovaDarkSurfaceVariant,
    onSurfaceVariant = CleanovaDarkTextSecondary,
    outline = CleanovaDarkOutline
)

@Composable
fun CleanovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CleanovaTypography,
        shapes = CleanovaShapes,
        content = content
    )
}
