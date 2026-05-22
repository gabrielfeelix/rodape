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
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LiterataFontFamily

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
    val shape = RoundedCornerShape(4.dp)
    val box = modifier
        .width(width)
        .height(height)
        .shadow(elevation = 8.dp, shape = shape, clip = false)
        .clip(shape)

    if (coverUrl.isNotBlank()) {
        AsyncImage(
            model = coverUrl,
            contentDescription = "Capa de $title",
            modifier = box,
            contentScale = ContentScale.Crop,
        )
        return
    }

    val palette = coverPalettes[hashOf(title).mod(coverPalettes.size)]
    val titleFontSize = (92f / maxOf(8, title.length) * 1.4f).coerceIn(9f, 15f)
    val titleLineHeight = (92f / maxOf(8, title.length) * 1.5f).coerceIn(11f, 17f)
    Box(
        modifier = box
            .background(palette.bg)
            .semantics { contentDescription = "Capa de $title" },
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
