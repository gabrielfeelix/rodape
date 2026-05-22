package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Terracota,
    secondary = VerdeMusgo,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextoDark,
    onSurface = TextoDark,
    onSurfaceVariant = TextoSecundarioDark,
    outline = Color(0x33A89B82)
)

private val LightColorScheme = lightColorScheme(
    primary = Terracota,
    secondary = VerdeMusgo,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextoLight,
    onSurface = TextoLight,
    onSurfaceVariant = TextoSecundarioLight,
    outline = Color(0x1B6B6A65)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
