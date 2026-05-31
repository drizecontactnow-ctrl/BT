package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldSecondary,
    secondary = MonzSecondary,
    tertiary = MonzTertiary,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = MonzPrimary,
    secondary = MonzSecondary,
    tertiary = MonzTertiary,
    background = Color.White,
    surface = MonzMintLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F1A19),
    onSurface = Color(0xFF0F1A19),
    surfaceVariant = Color(0xFFE2F0EE),
    onSurfaceVariant = Color(0xFF3B5250)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
