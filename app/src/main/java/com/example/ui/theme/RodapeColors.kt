package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Conjunto semântico de cores do Rodapé, resolvido POR TEMA (claro/escuro).
 *
 * O app tem design system próprio (mais tokens do que o ColorScheme do Material 3
 * comporta). Antes esses tokens eram constantes globais fixas (Ink/Paper/…),
 * o que travava o dark mode. Agora cada tela lê `RodapeTheme.colors.<token>` e
 * o valor certo vem do tema ativo via [LocalRodapeColors].
 *
 * Os nomes das propriedades espelham 1:1 as antigas constantes (só camelCase):
 * `Ink` → `ink`, `OlivaSoft` → `olivaSoft`, etc. — a migração é mecânica.
 */
@Immutable
data class RodapeColors(
    // Acento (terracota)
    val terracota: Color,
    val terracotaDark: Color,
    val terracotaSoft: Color,
    // Herói (oliva)
    val oliva: Color,
    val olivaDark: Color,
    val olivaDeep: Color,
    val olivaSoft: Color,
    val olivaMid: Color,
    // Destaque
    val dourado: Color,
    // Neutro / texto
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val tertiary: Color,
    val tertiarySoft: Color,
    // Superfícies
    val paper: Color,
    val paperDeep: Color,
    val cardSurface: Color,
    val cardSoft: Color,
    val cream: Color,
    val divider: Color,
    val dividerSoft: Color,
    val disabledSurface: Color,
    // Aviso / conflito (base dourada)
    val warning: Color,
    val warningSoft: Color,
    // Meta: útil pra ramos raros que precisam saber o tema (ex.: elevação/sombra).
    val isDark: Boolean,
)

/** Paleta CLARA — valores originais do design system. */
val LightRodapeColors = RodapeColors(
    terracota = Terracota,
    terracotaDark = TerracotaDark,
    terracotaSoft = TerracotaSoft,
    oliva = Oliva,
    olivaDark = OlivaDark,
    olivaDeep = OlivaDeep,
    olivaSoft = OlivaSoft,
    olivaMid = OlivaMid,
    dourado = Dourado,
    ink = Ink,
    inkSoft = InkSoft,
    muted = Muted,
    tertiary = Tertiary,
    tertiarySoft = TertiarySoft,
    paper = Paper,
    paperDeep = PaperDeep,
    cardSurface = CardSurface,
    cardSoft = CardSoft,
    cream = Cream,
    divider = Divider,
    dividerSoft = DividerSoft,
    disabledSurface = DisabledSurface,
    warning = Warning,
    warningSoft = WarningSoft,
    isDark = false,
)

/** Paleta ESCURA — carvão-oliva morno (ver Color.kt pra a lógica de inversão). */
val DarkRodapeColors = RodapeColors(
    terracota = TerracotaD,
    terracotaDark = TerracotaDarkD,
    terracotaSoft = TerracotaSoftD,
    oliva = OlivaD,
    olivaDark = OlivaDarkD,
    olivaDeep = OlivaDeepD,
    olivaSoft = OlivaSoftD,
    olivaMid = OlivaMidD,
    dourado = DouradoD,
    ink = InkD,
    inkSoft = InkSoftD,
    muted = MutedD,
    tertiary = TertiaryD,
    tertiarySoft = TertiarySoftD,
    paper = PaperD,
    paperDeep = PaperDeepD,
    cardSurface = CardSurfaceD,
    cardSoft = CardSoftD,
    cream = CreamD,
    divider = DividerD,
    dividerSoft = DividerSoftD,
    disabledSurface = DisabledSurfaceD,
    warning = WarningD,
    warningSoft = WarningSoftD,
    isDark = true,
)

/**
 * CompositionLocal com as cores do tema ativo. `static` porque a paleta troca
 * raramente (só no toggle de tema) — evita invalidar leitores a cada recomposição.
 * Default claro pra previews/testes sem provider.
 */
val LocalRodapeColors = staticCompositionLocalOf { LightRodapeColors }

/** Acesso ergonômico: `RodapeTheme.colors.ink`. */
object RodapeTheme {
    val colors: RodapeColors
        @Composable @ReadOnlyComposable
        get() = LocalRodapeColors.current
}
