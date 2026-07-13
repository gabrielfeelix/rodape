package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Tema CLARO. Oliva é herói (secondary), terracota é acento (primary).
private val LightColorScheme = lightColorScheme(
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
    onSurfaceVariant = Tertiary,
    outline = Divider,
    outlineVariant = DividerSoft,
)

// Tema ESCURO. Acentos claros → texto ESCURO por cima (onPrimary/onSecondary =
// superfície escura), casando com o padrão do código que usa `cream` como texto
// sobre fills de acento (cream inverte pra escuro no dark).
private val DarkColorScheme = darkColorScheme(
    primary = TerracotaD,
    onPrimary = PaperD,
    primaryContainer = TerracotaSoftD,
    onPrimaryContainer = TerracotaDarkD,
    secondary = OlivaD,
    onSecondary = PaperD,
    secondaryContainer = OlivaSoftD,
    onSecondaryContainer = OlivaDarkD,
    background = PaperD,
    onBackground = InkD,
    surface = CardSurfaceD,
    onSurface = InkD,
    surfaceVariant = CardSoftD,
    onSurfaceVariant = TertiaryD,
    outline = DividerD,
    outlineVariant = DividerSoftD,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val rodapeColors = if (darkTheme) DarkRodapeColors else LightRodapeColors
    CompositionLocalProvider(LocalRodapeColors provides rodapeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
