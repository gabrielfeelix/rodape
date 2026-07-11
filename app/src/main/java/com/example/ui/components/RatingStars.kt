package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DividerSoft

private val Gold = Color(0xFFE6BF6B)

@Composable
fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    spacing: Dp = 2.dp
) {
    val rounded = rating.toInt().coerceIn(0, 5)
    Row(
        // A11y: anuncia o valor numérico da nota; as estrelas coloridas sozinhas
        // (dourado ~1.75:1) não são legíveis nem por baixa visão nem por cego.
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "Avaliação: ${"%.1f".format(rating)} de 5 estrelas"
        },
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(5) { idx ->
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = if (idx < rounded) Gold else DividerSoft,
                modifier = Modifier.size(size)
            )
        }
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = "Dar nota $i de 5",
                tint = if (i <= selected) Gold else DividerSoft,
                modifier = Modifier
                    // Alvo de toque de 48dp (a estrela visual continua com `size`).
                    .minimumInteractiveComponentSize()
                    .size(size)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 24.dp),
                        role = Role.Button,
                        onClick = { onChange(i) },
                    )
            )
        }
    }
}
