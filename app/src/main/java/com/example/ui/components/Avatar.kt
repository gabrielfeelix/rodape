package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

// 8 cores — origem: claude-design/tokens.jsx (Avatar).
private val avatarColors = listOf(
    Color(0xFF5C7349), Color(0xFFB85838), Color(0xFF3E5230), Color(0xFF7A4F2B),
    Color(0xFF1F2A1A), Color(0xFF92A57F), Color(0xFF8E3F25), Color(0xFF5B5B53),
)

private fun initialsOf(name: String): String =
    name.trim().split(" ").filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    avatarUrl: String = "",
    size: Dp = 40.dp,
    ring: Color? = null,
) {
    val sized = modifier.size(size)
    val shaped = if (ring != null) {
        sized.border(2.dp, ring, CircleShape).clip(CircleShape)
    } else {
        sized.clip(CircleShape)
    }

    if (avatarUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar de $name",
            modifier = shaped,
            contentScale = ContentScale.Crop,
            loading = { InitialsAvatar(name, size) },
            error = { InitialsAvatar(name, size) },
        )
    } else {
        Box(modifier = shaped.semantics { contentDescription = "Avatar de $name" }) {
            InitialsAvatar(name, size)
        }
    }
}

@Composable
private fun InitialsAvatar(name: String, size: Dp) {
    val bg = avatarColors[name.sumOf { it.code }.mod(avatarColors.size)]
    Box(
        modifier = Modifier.fillMaxSize().background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = Color(0xFFFBF6EC),
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
