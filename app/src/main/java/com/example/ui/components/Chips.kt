package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSoft
import com.example.ui.theme.Cream
import com.example.ui.theme.Divider
import com.example.ui.theme.DividerSoft
import com.example.ui.theme.Ink
import com.example.ui.theme.Oliva
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.Tertiary
import com.example.ui.theme.TerracotaDark
import com.example.ui.theme.TerracotaSoft

enum class PillVariant { Default, Olive, OliveDeep, Terra, Mustard, Ink, Outline }

private data class PillStyle(val bg: Color, val fg: Color, val border: Color?)

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    variant: PillVariant = PillVariant.Default,
) {
    val style = when (variant) {
        PillVariant.Default -> PillStyle(CardSoft, Tertiary, Divider)
        PillVariant.Olive -> PillStyle(OlivaSoft, OlivaDark, null)
        PillVariant.OliveDeep -> PillStyle(Oliva, Cream, null)
        PillVariant.Terra -> PillStyle(TerracotaSoft, TerracotaDark, null)
        PillVariant.Mustard -> PillStyle(Color(0xFFF1E3BE), Color(0xFF6E5316), null)
        PillVariant.Ink -> PillStyle(Ink, Cream, null)
        PillVariant.Outline -> PillStyle(Color.Transparent, Tertiary, Divider)
    }
    val base = modifier.clip(RoundedCornerShape(999.dp)).background(style.bg)
    val bordered = if (style.border != null) {
        base.border(1.dp, style.border, RoundedCornerShape(999.dp))
    } else base

    Box(bordered.padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(
            text = text.uppercase(),
            color = style.fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
fun ProgressBar(
    value: Float,
    modifier: Modifier = Modifier,
    color: Color = Oliva,
    track: Color = DividerSoft,
    height: Dp = 6.dp,
) {
    val clamped = value.coerceIn(0f, 1f)
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(clamped)
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}
