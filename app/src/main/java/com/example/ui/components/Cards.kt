package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.cardShadow
import com.example.ui.theme.floatShadow

@Composable
fun RodapeCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.cardShadow(cornerRadius = RodapeRadii.md),
        shape = RoundedCornerShape(RodapeRadii.md),
        colors = CardDefaults.cardColors(
            containerColor = RodapeTheme.colors.cardSurface,
            contentColor = RodapeTheme.colors.ink,
        ),
        border = BorderStroke(0.5.dp, RodapeTheme.colors.divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Tier ELEVADO do card (Onda 1 do PLANO-UI). Mesma linguagem do [RodapeCard], mas
 * com sombra flutuante tingida ([floatShadow]) pra heros/tickets/cards coloridos —
 * profundidade vira token, não `CardDefaults.cardElevation` (que injeta o cinza
 * Material que o design proíbe). [containerColor] permite cards de acento (ex.: o
 * stat oliva de frases) sem perder a sombra do sistema.
 */
@Composable
fun RodapeCardElevated(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val bg = if (containerColor == Color.Unspecified) RodapeTheme.colors.cardSurface else containerColor
    val fg = if (contentColor == Color.Unspecified) RodapeTheme.colors.ink else contentColor
    Card(
        modifier = modifier.floatShadow(cornerRadius = RodapeRadii.md),
        shape = RoundedCornerShape(RodapeRadii.md),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun TbSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // A11y: título de seção vira heading pra navegação por títulos do TalkBack
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        action?.invoke()
    }
}
