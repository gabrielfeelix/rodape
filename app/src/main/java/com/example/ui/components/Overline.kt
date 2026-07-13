package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.RodapeTheme

/**
 * Overline — rótulo-sobrescrito (eyebrow) uppercase, pequeno e discreto.
 *
 * Consolida o padrão que estava redeclarado ~13× inline como
 * `labelSmall.copy(fontWeight = Bold, letterSpacing = 1.sp, color = …)`. O
 * `labelSmall` da escala (Type.kt) JÁ é Inter Bold 10sp com `letterSpacing = 1.sp`,
 * então a única variação real entre os call-sites era a `color` — o render aqui é
 * idêntico ao que cada site produzia. Passe o texto já em CAIXA ALTA (a decisão de
 * uppercase é do call-site, que às vezes usa `.uppercase()` dinâmico).
 */
@Composable
fun Overline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = RodapeTheme.colors.muted,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(color = color),
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
    )
}
