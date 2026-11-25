package com.cleanova.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF006CE5), onPrimary = Color.White,
    background = Color(0xFFF2F2F7), onBackground = Color(0xFF17171A),
    surface = Color.White, onSurface = Color(0xFF17171A),
    surfaceVariant = Color(0xFFE6E6ED), onSurfaceVariant = Color(0xFF575760)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF76B6FF), onPrimary = Color(0xFF002852),
    background = Color(0xFF000000), onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF1C1C1E), onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF303036), onSurfaceVariant = Color(0xFFB6B6BF)
)
@Composable
fun CleanovaTheme(appearance: String, content: @Composable () -> Unit) {
    val dark = appearance == "Dark" || (appearance == "System" && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
