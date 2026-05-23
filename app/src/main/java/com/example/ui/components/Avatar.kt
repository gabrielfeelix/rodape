package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.ui.theme.OlivaSoft

// 8 cores — origem: claude-design/tokens.jsx (Avatar).
private val avatarColors = listOf(
    Color(0xFF5C7349), Color(0xFFB85838), Color(0xFF3E5230), Color(0xFF7A4F2B),
    Color(0xFF1F2A1A), Color(0xFF92A57F), Color(0xFF8E3F25), Color(0xFF5B5B53),
)

/**
 * Avatares ilustrados — esquema "preset:<id>" no Book/User.avatarUrl.
 *
 * Cada preset declara seus próprios fatores visuais porque ilustrações têm
 * proporções diferentes (busto compacto ~1:1 vs figura inteira ~1:2). O
 * círculo de fundo sempre tem [size]; a ilustração escala/posiciona em cima.
 *
 * - [widthFactor]: largura da imagem relativa ao [size] do círculo (1.0 = mesma)
 * - [heightFactor]: altura da imagem relativa ao [size] (pode ser >1 pra estourar)
 * - [verticalOffsetFactor]: deslocamento vertical (negativo sobe, fração de [size])
 */
private data class PresetAvatar(
    val drawableRes: Int,
    val bgColor: Color,
    val displayName: String,
    val widthFactor: Float = 1.15f,
    val heightFactor: Float = 1.30f,
    val verticalOffsetFactor: Float = 0f
)

private val presetAvatars: Map<String, PresetAvatar> = mapOf(
    "preset:pequeno_principe" to PresetAvatar(
        drawableRes = R.drawable.avatar_pequeno_principe,
        bgColor = Color(0xFFE5EBDA), // OlivaSoft
        displayName = "Pequeno Príncipe",
        widthFactor = 1.15f,
        heightFactor = 1.30f
    ),
    "preset:don_quixote" to PresetAvatar(
        drawableRes = R.drawable.avatar_don_quixote,
        bgColor = Color(0xFFFBE5DA), // TerracotaSoft — combina com armadura/cavaleiro
        displayName = "Don Quixote",
        // Figura inteira ~1:1.7 — ocupa mais altura pra ver da cabeça até o meio do corpo
        widthFactor = 1.30f,
        heightFactor = 2.10f
    )
)

/** True se o avatarUrl é um preset ilustrado conhecido. */
fun isPresetAvatar(avatarUrl: String): Boolean = presetAvatars.containsKey(avatarUrl)

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
    // Preset ilustrado: círculo de fundo + ilustração estourando o topo
    val preset = presetAvatars[avatarUrl]
    if (preset != null) {
        PresetAvatarView(
            preset = preset,
            modifier = modifier,
            size = size,
            ring = ring,
            name = name
        )
        return
    }

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
private fun PresetAvatarView(
    preset: PresetAvatar,
    modifier: Modifier,
    size: Dp,
    ring: Color?,
    name: String
) {
    // Container externo precisa caber o estouro da ilustração. Largura igual ao
    // widthFactor, altura igual ao heightFactor — ambos relativos ao size do círculo.
    val containerWidth = size * preset.widthFactor.coerceAtLeast(1f)
    val containerHeight = size * preset.heightFactor.coerceAtLeast(1f)
    Box(
        modifier = modifier
            .width(containerWidth)
            .height(containerHeight)
            .semantics { contentDescription = "Avatar de $name" },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Círculo de fundo (preset.bgColor), com ring opcional
        val circleMod = Modifier
            .size(size)
            .align(Alignment.BottomCenter)
            .let { base ->
                if (ring != null) base.border(2.dp, ring, CircleShape) else base
            }
            .clip(CircleShape)
            .background(preset.bgColor)
        Box(modifier = circleMod)

        // Ilustração — sem clip, ancorada na base, ultrapassa o círculo no topo
        Image(
            painter = painterResource(id = preset.drawableRes),
            contentDescription = null,
            modifier = Modifier
                .width(size * preset.widthFactor)
                .height(containerHeight)
                .offset(y = size * preset.verticalOffsetFactor),
            contentScale = ContentScale.Fit
        )
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
