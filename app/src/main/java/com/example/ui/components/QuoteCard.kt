package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.cardShadow

/**
 * QuoteCard — a frase como KEEPSAKE (3.8): objeto de guardar/compartilhar,
 * não uma linha de lista.
 *
 * - Gradiente papel→cream + sombra TINGIDA em camadas (cardShadow; morreu o
 *   CardDefaults.cardElevation cinza — regra 6).
 * - Aspas de abertura (top-left) E fechamento (bottom-right) EMOLDURANDO o
 *   texto (antes só a de cima, sobrepondo).
 * - Atribuição como linha única elegante DENTRO do card (traço no acento +
 *   "livro · capítulo") — antes o título flutuava órfão fora.
 * - [accent] varia por card (oliva/terracota/dourado no call-site) pra lista
 *   não parecer template repetido.
 * - SEM lixeira dentro do objeto: ações via [onLongPress] (menu no call-site).
 *   O card é renderizável para PNG (share-as-image) — por isso é autocontido.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuoteCard(
    texto: String,
    ref: String,
    modifier: Modifier = Modifier,
    bookTitle: String? = null,
    accent: Color = RodapeTheme.colors.oliva,
    onLongPress: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(RodapeRadii.md)
    var base = modifier
        .fillMaxWidth()
        .cardShadow(cornerRadius = RodapeRadii.md)
        .clip(shape)
        .background(
            Brush.verticalGradient(
                0f to RodapeTheme.colors.cardSurface,
                1f to RodapeTheme.colors.cream,
            )
        )
        .border(0.5.dp, RodapeTheme.colors.divider, shape)
    if (onLongPress != null) {
        base = base.combinedClickable(
            onClick = onLongPress,
            onLongClick = onLongPress,
            onLongClickLabel = "Opções da frase",
        )
    }

    Box(modifier = base) {
        // Aspa de ABERTURA — emoldura o texto por cima, no acento.
        Text(
            text = "“",
            fontFamily = LiterataFontFamily,
            fontSize = 56.sp,
            lineHeight = 56.sp,
            color = accent.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 6.dp),
        )
        // Aspa de FECHAMENTO — canto oposto, fechando a moldura.
        Text(
            text = "”",
            fontFamily = LiterataFontFamily,
            fontSize = 56.sp,
            lineHeight = 56.sp,
            color = accent.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = texto,
                fontFamily = LiterataFontFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 17.sp,
                lineHeight = 26.sp,
                color = RodapeTheme.colors.inkSoft,
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 34.dp, bottom = 14.dp),
            )

            // Atribuição: traço no acento + linha única, dentro do objeto.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 28.dp, end = 56.dp, bottom = 20.dp),
            ) {
                Box(
                    Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(RodapeRadii.full))
                        .background(accent)
                )
                Text(
                    text = listOfNotNull(
                        bookTitle?.takeIf { it.isNotBlank() },
                        ref.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = RodapeTheme.colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
