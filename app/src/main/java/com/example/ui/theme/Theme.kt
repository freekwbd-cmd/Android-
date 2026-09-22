package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VibeDarkColorScheme = darkColorScheme(
    primary = NeonCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF380816),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = NeonViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF280B40),
    onSecondaryContainer = Color(0xFFF3E5FF),
    tertiary = NeonCyan,
    onTertiary = VoidBlack,
    tertiaryContainer = Color(0xFF00363D),
    onTertiaryContainer = Color(0xFF97F0FF),
    background = DarkCanvas,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    outlineVariant = Color(0xFF1E2238)
)

@Composable
fun VibeAITheme(
    darkTheme: Boolean = true, // Cyberpunk aesthetic is immersive dark by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VibeDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    VibeAITheme(content = content)
}
