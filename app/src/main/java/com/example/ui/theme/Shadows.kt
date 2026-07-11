package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Sombras do design — origem: claude-design (shell.jsx, tokens.jsx, screens-*.jsx) ──
// O protótipo usa sombras largas, suaves e TINGIDAS (marrom-papel e verde-oliva),
// nunca o cinza de elevação Material. Em API 28+ desenhamos a receita exata via
// setShadowLayer; em 24-27 (hardware canvas não suporta) cai pra elevação comum.

/** Marrom-papel rgba(45,30,15,…) — sombra padrão de cards. */
val ShadowBrown = Color(0xFF2D1E0F)

/** Verde-oliva rgba(41,56,32,…) — sombra de nav, ticket e capas sobre oliva. */
val ShadowOlive = Color(0xFF293820)

/** Tinta rgba(20,18,15,…) — sheets. */
val ShadowInk = Color(0xFF14120F)

/**
 * Uma camada de sombra suave atrás do conteúdo (equivale a
 * `box-shadow: [offsetX] [offsetY] [blur] [color@alpha]` com o canto [cornerRadius]).
 * Empilhe chamadas para receitas multicamada.
 */
fun Modifier.softShadow(
    color: Color,
    alpha: Float,
    blur: Dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp,
    cornerRadius: Dp = 20.dp,
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        // Fallback: elevação genérica proporcional ao blur (uma vez só por cadeia
        // ficaria melhor, mas duplicar elevação só engrossa levemente).
        return shadow(elevation = blur / 3, shape = RoundedCornerShape(cornerRadius))
    }
    return drawBehind {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            paint.color = android.graphics.Color.TRANSPARENT
            paint.setShadowLayer(
                blur.toPx(),
                offsetX.toPx(),
                offsetY.toPx(),
                color.copy(alpha = alpha).toArgb(),
            )
            val r = cornerRadius.toPx()
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        }
    }
}

// ── Receitas prontas (1:1 com o protótipo) ──

/** Card branco padrão: `0 1px 2px + 0 4px 14px rgba(45,30,15,0.04)` + borda 0.5. */
fun Modifier.cardShadow(cornerRadius: Dp = 20.dp): Modifier = this
    .softShadow(ShadowBrown, 0.04f, blur = 2.dp, offsetY = 1.dp, cornerRadius = cornerRadius)
    .softShadow(ShadowBrown, 0.04f, blur = 14.dp, offsetY = 4.dp, cornerRadius = cornerRadius)

/** Bottom nav oliva: `0 8px 24px rgba(41,56,32,0.25) + 0 2px 6px rgba(41,56,32,0.15)`. */
fun Modifier.navShadow(cornerRadius: Dp = 999.dp): Modifier = this
    .softShadow(ShadowOlive, 0.25f, blur = 24.dp, offsetY = 8.dp, cornerRadius = cornerRadius)
    .softShadow(ShadowOlive, 0.15f, blur = 6.dp, offsetY = 2.dp, cornerRadius = cornerRadius)

/** Ticket do encontro: `0 8px 28px rgba(41,56,32,0.20) + 0 2px 6px rgba(0,0,0,0.08)`. */
fun Modifier.ticketShadow(cornerRadius: Dp = 24.dp): Modifier = this
    .softShadow(ShadowOlive, 0.20f, blur = 28.dp, offsetY = 8.dp, cornerRadius = cornerRadius)
    .softShadow(Color.Black, 0.08f, blur = 6.dp, offsetY = 2.dp, cornerRadius = cornerRadius)

/** Capa no hero oliva: `0 14px 32px rgba(0,0,0,0.4)`. */
fun Modifier.heroCoverShadow(cornerRadius: Dp = 4.dp): Modifier = this
    .softShadow(Color.Black, 0.4f, blur = 32.dp, offsetY = 14.dp, cornerRadius = cornerRadius)

/** Capa destacada no detalhe: `0 18px 38px rgba(41,56,32,0.22) + 0 4px 10px rgba(0,0,0,0.10)`. */
fun Modifier.detailCoverShadow(cornerRadius: Dp = 4.dp): Modifier = this
    .softShadow(ShadowOlive, 0.22f, blur = 38.dp, offsetY = 18.dp, cornerRadius = cornerRadius)
    .softShadow(Color.Black, 0.10f, blur = 10.dp, offsetY = 4.dp, cornerRadius = cornerRadius)

/** Card flutuante (progresso sobreposto ao hero): `0 8px 28px rgba(0,0,0,0.08)`. */
fun Modifier.floatShadow(cornerRadius: Dp = 20.dp): Modifier = this
    .softShadow(Color.Black, 0.08f, blur = 28.dp, offsetY = 8.dp, cornerRadius = cornerRadius)

/** Pill de status pequena: `0 1px 4px rgba(0,0,0,0.10)`. */
fun Modifier.pillShadow(cornerRadius: Dp = 999.dp): Modifier = this
    .softShadow(Color.Black, 0.10f, blur = 4.dp, offsetY = 1.dp, cornerRadius = cornerRadius)

/** Sheet: `0 -8px 32px rgba(20,18,15,0.18)`. */
fun Modifier.sheetShadow(cornerRadius: Dp = 24.dp): Modifier = this
    .softShadow(ShadowInk, 0.18f, blur = 32.dp, offsetY = (-8).dp, cornerRadius = cornerRadius)
