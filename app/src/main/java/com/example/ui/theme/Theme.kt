package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema único claro. Oliva é herói (secondary), terracota é acento (primary).
private val TramabookColorScheme = lightColorScheme(
    primary = Terracota,
    onPrimary = Color.White,
    primaryContainer = TerracotaSoft,
    onPrimaryContainer = TerracotaDark,
    secondary = Oliva,
    onSecondary = Color.White,
    secondaryContainer = OlivaSoft,
    onSecondaryContainer = OlivaDark,
    background = Paper,
    onBackground = Ink,
    surface = CardSurface,
    onSurface = Ink,
    surfaceVariant = CardSoft,
    onSurfaceVariant = Muted,
    outline = Divider,
    outlineVariant = DividerSoft,
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TramabookColorScheme,
        typography = Typography,
        content = content,
    )
}
