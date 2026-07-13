package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala ÚNICA de raio de canto (Onda 1 do PLANO-UI). Antes o app usava
 * 3/4/6/10/12/14/16/20/22/24/28 sem sistema. Regra:
 *  - [xs] capas de livro · [sm] chips, banners, campos · [md] cards · [full] pílulas.
 */
object RodapeRadii {
    val xs: Dp = 3.dp
    val sm: Dp = 12.dp
    val md: Dp = 20.dp
    val full: Dp = 999.dp
}

/**
 * Tokens que não cabem no ColorScheme do Material 3.
 * Acesse via `RodapeTokens` direto (são estáticos, tema único claro).
 */
object RodapeTokens {
    val dourado = Dourado
    val terracotaDark = TerracotaDark
    val terracotaSoft = TerracotaSoft
    val olivaDark = OlivaDark
    val olivaDeep = OlivaDeep
    val olivaSoft = OlivaSoft
    val olivaMid = OlivaMid
    val tertiary = Tertiary
    val tertiarySoft = TertiarySoft
    val inkSoft = InkSoft
    val paperDeep = PaperDeep
    val cardSoft = CardSoft
    val cream = Cream
    val dividerSoft = DividerSoft
}

/** Cor de clube — presets do protótipo (CreateClubScreen). */
data class ClubColor(val id: String, val bg: Color, val soft: Color, val ink: Color)

/** Os 5 presets na ordem do protótipo. Índice "0".."4" é o que fica em Club.cor. */
val ClubColors = listOf(
    ClubColor("olive", Color(0xFF4F653F), Color(0xFFE1E7D7), Color.White),
    ClubColor("terracotta", Color(0xFF934528), Color(0xFFF3DCD0), Color.White),
    ClubColor("plum", Color(0xFF6E3A52), Color(0xFFEBDCE4), Color.White),
    ClubColor("mustard", Color(0xFFA6802B), Color(0xFFF1E3BE), Color.White),
    ClubColor("ink", Color(0xFF2E3A47), Color(0xFFD7DCE2), Color.White),
)

/** Resolve a cor do clube a partir do valor salvo em Club.cor (índice ou hex). */
fun clubColorFor(cor: String): ClubColor {
    cor.toIntOrNull()?.let { idx ->
        if (idx in ClubColors.indices) return ClubColors[idx]
    }
    return runCatching { Color(android.graphics.Color.parseColor(cor)) }
        .map { ClubColor("custom", it, OlivaSoft, Color.White) }
        .getOrDefault(ClubColors[0])
}
