package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.softShadow

private data class CoverPalette(val bg: Color, val fg: Color)

// 8 paletas — origem: claude-design/tokens.jsx (Cover).
private val coverPalettes = listOf(
    CoverPalette(Color(0xFF3E5230), Color(0xFFF0EEE5)),
    CoverPalette(Color(0xFFB85838), Color(0xFFFBFAF4)),
    CoverPalette(Color(0xFF1F2A1A), Color(0xFFE5EBDA)),
    CoverPalette(Color(0xFF7A4F2B), Color(0xFFFBFAF4)),
    CoverPalette(Color(0xFFD8C9B0), Color(0xFF3E5230)),
    CoverPalette(Color(0xFF5C7349), Color(0xFFFBFAF4)),
    CoverPalette(Color(0xFFE5EBDA), Color(0xFF3E5230)),
    CoverPalette(Color(0xFFFBE5DA), Color(0xFF8E3F25)),
)

private fun hashOf(text: String): Int = text.sumOf { it.code }

@Composable
fun Cover(
    title: String,
    author: String,
    coverUrl: String = "",
    modifier: Modifier = Modifier,
    width: Dp = 92.dp,
    height: Dp = 138.dp,
) {
    val shape = RoundedCornerShape(RodapeRadii.xs)
    val box = modifier
        .width(width)
        .height(height)
        // Sombra multicamada do design (tokens.jsx Cover): curta + difusa
        .softShadow(Color.Black, alpha = 0.18f, blur = 2.dp, offsetY = 1.dp, cornerRadius = 3.dp)
        .softShadow(Color.Black, alpha = 0.10f, blur = 14.dp, offsetY = 6.dp, cornerRadius = 3.dp)
        .clip(shape)
        // Lombada: gradiente escuro na borda esquerda + risco de luz (efeito livro físico)
        .drawWithContent {
            drawContent()
            val spineW = 6.dp.toPx()
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.22f),
                    1f to Color.Transparent,
                    endX = spineW,
                ),
                size = androidx.compose.ui.geometry.Size(spineW, size.height),
            )
            drawRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = androidx.compose.ui.geometry.Offset(spineW, 0f),
                size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height),
            )
        }

    if (coverUrl.isNotBlank()) {
        // AsyncImage (sem subcomposicao — mais leve em grids/rows que
        // SubcomposeAsyncImage). A capa gerada fica ATRAS: aparece enquanto
        // carrega e se a imagem falhar; a foto real cobre por cima quando chega.
        Box(modifier = box) {
            GeneratedCover(title, author, width)
            // 4.3: crossfade ao resolver — a capa real esmaece por cima da
            // gerada em vez de "pipocar" (pop) quando a rede responde.
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Capa de $title",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    } else {
        Box(modifier = box.semantics { contentDescription = "Capa de $title" }) {
            GeneratedCover(title, author, width)
        }
    }
}

@Composable
private fun GeneratedCover(title: String, author: String, width: Dp) {
    val palette = coverPalettes[hashOf(title).mod(coverPalettes.size)]
    val titleFontSize = (width.value / maxOf(8, title.length) * 1.4f).coerceIn(9f, 15f)
    val titleLineHeight = (width.value / maxOf(8, title.length) * 1.5f).coerceIn(11f, 17f)
    val ornament = hashOf(title + author).mod(4)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            // Ornamentos por hash — origem: claude-design/tokens.jsx (Cover):
            // 0=círculo, 1=linha, 2=hachura diagonal, 3=réguas duplas
            .drawBehind {
                val fg = palette.fg.copy(alpha = 0.35f)
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                when (ornament) {
                    0 -> drawCircle(
                        color = fg,
                        radius = size.width * 0.22f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height * 0.62f),
                        style = stroke,
                    )
                    1 -> drawLine(
                        color = fg,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.14f, size.height * 0.68f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height * 0.68f),
                        strokeWidth = 1.dp.toPx(),
                    )
                    2 -> repeat(4) { i ->
                        val off = size.width * (0.42f + 0.12f * i)
                        drawLine(
                            color = fg,
                            start = androidx.compose.ui.geometry.Offset(off, size.height * 0.78f),
                            end = androidx.compose.ui.geometry.Offset(off - size.width * 0.16f, size.height * 0.62f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    else -> repeat(2) { i ->
                        val y = size.height * (0.64f + 0.05f * i)
                        drawLine(
                            color = fg,
                            start = androidx.compose.ui.geometry.Offset(size.width * 0.14f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width * 0.86f, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(width * 0.09f)) {
            Text(
                text = title,
                color = palette.fg,
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = titleFontSize.sp,
                lineHeight = titleLineHeight.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = author.uppercase(),
                color = palette.fg.copy(alpha = 0.75f),
                fontFamily = InterFontFamily,
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
