package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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

// 8 cores — origem: claude-design/tokens.jsx (Avatar).
private val avatarColors = listOf(
    Color(0xFF5C7349), Color(0xFFB85838), Color(0xFF3E5230), Color(0xFF7A4F2B),
    Color(0xFF1F2A1A), Color(0xFF92A57F), Color(0xFF8E3F25), Color(0xFF5B5B53),
)

/**
 * Avatares ilustrados — esquema "preset:<id>" no Book/User.avatarUrl.
 *
 * Cada preset usa o mesmo bounding box (DEFAULT_WIDTH_FACTOR x DEFAULT_HEIGHT_FACTOR)
 * pra garantir peso visual uniforme no grid. Ilustrações de proporções diferentes
 * (busto vs figura inteira) são acomodadas via ContentScale.Fit dentro do box.
 */
private data class PresetAvatar(
    val drawableRes: Int,
    val bgColor: Color,
    val displayName: String
)

// Fatores padronizados pra todos os avatares terem peso visual equivalente no grid.
// Ilustrações com figura inteira (Don Quixote, Joana d'Arc, Leitor) ficam mais
// compactas mas mostradas integralmente via ContentScale.Fit; ilustrações de busto
// (Pequeno Príncipe, Pétalas, Indígena, Detetive) ocupam o mesmo bounding box.
private const val DEFAULT_WIDTH_FACTOR = 1.20f
private const val DEFAULT_HEIGHT_FACTOR = 1.50f

private val presetAvatars: Map<String, PresetAvatar> = mapOf(
    "preset:pequeno_principe" to PresetAvatar(
        drawableRes = R.drawable.avatar_pequeno_principe,
        bgColor = Color(0xFFE5EBDA), // OlivaSoft
        displayName = "Pequeno Príncipe"
    ),
    "preset:don_quixote" to PresetAvatar(
        drawableRes = R.drawable.avatar_don_quixote,
        bgColor = Color(0xFFFBE5DA), // TerracotaSoft — combina com armadura/cavaleiro
        displayName = "Don Quixote"
    ),
    "preset:petalas" to PresetAvatar(
        drawableRes = R.drawable.avatar_petalas,
        bgColor = Color(0xFFF1E3BE), // Mustard soft — combina com pétalas/jardim
        displayName = "Pétalas"
    ),
    "preset:indigena" to PresetAvatar(
        drawableRes = R.drawable.avatar_indigena,
        bgColor = Color(0xFFD7DCE2), // Ink soft — céu/horizonte, contrasta com a paleta
        displayName = "Indígena"
    ),
    "preset:detetive" to PresetAvatar(
        drawableRes = R.drawable.avatar_detetive,
        bgColor = Color(0xFFD9D9CF), // Tertiary soft — tom noir/clássico
        displayName = "Detetive"
    ),
    "preset:joana_darc" to PresetAvatar(
        drawableRes = R.drawable.avatar_joana_darc,
        bgColor = Color(0xFFEBDCE4), // Plum soft — combina com a bandeira azul-real
        displayName = "Joana d'Arc"
    ),
    "preset:leitor" to PresetAvatar(
        drawableRes = R.drawable.avatar_leitor,
        bgColor = Color(0xFFE5EBDA).copy(alpha = 0.85f), // verde-folha levemente diluído
        displayName = "Leitor"
    ),
    "preset:leitora" to PresetAvatar(
        drawableRes = R.drawable.avatar_leitora,
        bgColor = Color(0xFFF5E8E0), // rosado-cremoso — neutro feminino, combina com tom de pele
        displayName = "Leitora"
    ),
    "preset:mago" to PresetAvatar(
        drawableRes = R.drawable.avatar_mago,
        bgColor = Color(0xFFE0D7E8), // lavanda — cosmos/magia, combina com chapéu roxo
        displayName = "Mago"
    ),
    "preset:emilia" to PresetAvatar(
        drawableRes = R.drawable.avatar_emilia,
        bgColor = Color(0xFFF1EFE6), // DividerSoft — neutro creme pra não competir com a boneca colorida
        displayName = "Emília"
    ),
    "preset:fantasma" to PresetAvatar(
        drawableRes = R.drawable.avatar_fantasma,
        bgColor = Color(0xFF2A3340), // azul-noite — contraste pro fantasma claro
        displayName = "Fantasma"
    ),
    "preset:alice" to PresetAvatar(
        drawableRes = R.drawable.avatar_alice,
        bgColor = Color(0xFFD8E7D9), // verde-menta suave — País das Maravilhas
        displayName = "Alice"
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
    // Bounding box uniforme pra todos os avatares ilustrados — garante
    // proporcionalidade visual no grid. Ilustração se ajusta dentro via Fit.
    val containerWidth = size * DEFAULT_WIDTH_FACTOR
    val containerHeight = size * DEFAULT_HEIGHT_FACTOR
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
                .width(containerWidth)
                .height(containerHeight),
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
