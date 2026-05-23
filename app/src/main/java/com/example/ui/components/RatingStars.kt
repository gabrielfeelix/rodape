package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        modifier = modifier,
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
                contentDescription = "Estrela $i",
                tint = if (i <= selected) Gold else DividerSoft,
                modifier = Modifier
                    .size(size)
                    .clickable { onChange(i) }
            )
        }
    }
}
