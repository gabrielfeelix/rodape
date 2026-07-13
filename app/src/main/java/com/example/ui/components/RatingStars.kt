package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RodapeIcons
import com.example.ui.theme.RodapeTheme

@Composable
fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    spacing: Dp = 2.dp
) {
    // Estrela cheia = StarFill (sólida, dourado); vazia = Star (contorno, dividerSoft).
    // Antes as duas eram a mesma Star de contorno só trocando o tint → a "cheia"
    // ficava um oco dourado, parecendo inacabada. Meia-estrela distingue 4.5 de 4.8.
    val gold = RodapeTheme.colors.dourado
    val empty = RodapeTheme.colors.dividerSoft
    val full = rating.toInt().coerceIn(0, 5)
    val frac = (rating - full).coerceIn(0f, 1f)
    val showHalf = frac >= 0.25f && frac < 0.75f
    val fullShown = (if (frac >= 0.75f) full + 1 else full).coerceIn(0, 5)

    Row(
        // A11y: anuncia o valor numérico da nota; as estrelas coloridas sozinhas
        // (dourado ~1.75:1) não são legíveis nem por baixa visão nem por cego.
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "Avaliação: ${"%.1f".format(rating)} de 5 estrelas"
        },
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(5) { idx ->
            when {
                idx < fullShown -> Icon(
                    imageVector = RodapeIcons.StarFill,
                    contentDescription = null,
                    tint = gold,
                    modifier = Modifier.size(size),
                )
                idx == fullShown && showHalf -> HalfStar(size, gold, empty)
                else -> Icon(
                    imageVector = RodapeIcons.Star,
                    contentDescription = null,
                    tint = empty,
                    modifier = Modifier.size(size),
                )
            }
        }
    }
}

/** Estrela cheia à esquerda + contorno à direita, revelando 50% de preenchimento. */
@Composable
private fun HalfStar(size: Dp, fill: Color, empty: Color) {
    Box(Modifier.size(size)) {
        Icon(
            imageVector = RodapeIcons.Star,
            contentDescription = null,
            tint = empty,
            modifier = Modifier.size(size),
        )
        Icon(
            imageVector = RodapeIcons.StarFill,
            contentDescription = null,
            tint = fill,
            modifier = Modifier
                .size(size)
                .drawWithContent {
                    clipRect(right = this.size.width / 2f) { this@drawWithContent.drawContent() }
                },
        )
    }
}

@Composable
fun RatingStarsInput(
    selected: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    spacing: Dp = 8.dp
) {
    val gold = RodapeTheme.colors.dourado
    val empty = RodapeTheme.colors.dividerSoft
    Row(
        // selectableGroup: TalkBack anuncia as 5 estrelas como um grupo de opcoes
        // (ex: "opcao 3 de 5") em vez de 5 botoes soltos sem relacao.
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (i in 1..5) {
            val isThisSelected = i == selected
            val filled = i <= selected
            Icon(
                imageVector = if (filled) RodapeIcons.StarFill else RodapeIcons.Star,
                contentDescription = "Dar nota $i de 5",
                tint = if (filled) gold else empty,
                modifier = Modifier
                    // Alvo de toque de 48dp (a estrela visual continua com `size`).
                    .minimumInteractiveComponentSize()
                    .size(size)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 24.dp),
                        role = Role.RadioButton,
                        onClick = { onChange(i) },
                    )
                    // Marca a estrela escolhida como selecionada e informa a nota
                    // atual, pra TalkBack transmitir a avaliacao ja dada.
                    .semantics {
                        this.selected = isThisSelected
                        if (isThisSelected) stateDescription = "Nota atual: $selected de 5"
                    }
            )
        }
    }
}
