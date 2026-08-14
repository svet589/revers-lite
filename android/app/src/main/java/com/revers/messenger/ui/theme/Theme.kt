package com.revers.messenger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE63946),
    secondary = Color(0xFF1A1A23),
    tertiary = Color(0xFF2A2A3A),
    background = Color(0xFF0F0F12),
    surface = Color(0xFF1A1A23),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE63946),
    secondary = Color(0xFFEBEBEF),
    tertiary = Color(0xFFDDDDE5),
    background = Color(0xFFE8E9F0),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF1A1A1E),
    onBackground = Color(0xFF1A1A1E),
    onSurface = Color(0xFF1A1A1E)
)

@Composable
fun ReversTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
