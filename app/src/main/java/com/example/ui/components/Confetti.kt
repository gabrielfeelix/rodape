package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.RodapeTheme
import kotlin.random.Random

private data class Confetto(
    val x: Float,       // posição inicial 0..1 da largura
    val delay: Float,   // 0..0.25 do progresso (escalona a queda)
    val drift: Float,   // deriva horizontal (px)
    val spins: Float,   // voltas de rotação
    val w: Float,       // largura (px)
    val colorIdx: Int,
)

/**
 * Explosão de confete de marco de leitura (25/50/75/100%). Overlay one-shot,
 * transparente a toques, disparado quando [trigger] muda (incremente um Int).
 * Respeita reduced-motion (não desenha nada). Retângulos caindo com deriva +
 * rotação, fade no fim. Paleta da marca (terracota/oliva/dourado).
 */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val palette = listOf(
        RodapeTheme.colors.terracota,
        RodapeTheme.colors.oliva,
        RodapeTheme.colors.dourado,
        RodapeTheme.colors.olivaMid,
    )
    val progress = remember { Animatable(0f) }
    var pieces by remember { mutableStateOf<List<Confetto>>(emptyList()) }
    var playing by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger == 0 || reduced) return@LaunchedEffect
        pieces = List(90) {
            Confetto(
                x = Random.nextFloat(),
                delay = Random.nextFloat() * 0.25f,
                drift = (Random.nextFloat() - 0.5f) * 260f,
                spins = (Random.nextFloat() - 0.5f) * 3f,
                w = 6f + Random.nextFloat() * 8f,
                colorIdx = Random.nextInt(4),
            )
        }
        playing = true
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1900))
        playing = false
    }

    if (!playing) return
    val p = progress.value
    Canvas(modifier = modifier.fillMaxSize()) {
        val h = size.height
        val wpx = size.width
        pieces.forEach { c ->
            val span = (1f - c.delay).coerceAtLeast(0.01f)
            val local = ((p - c.delay) / span).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = -40f + local * (h + 80f)
            val x = c.x * wpx + c.drift * local
            val alpha = if (local > 0.8f) (1f - (local - 0.8f) / 0.2f).coerceIn(0f, 1f) else 1f
            val cx = x + c.w / 2f
            val cy = y + c.w * 0.3f
            rotate(degrees = c.spins * 360f * local, pivot = Offset(cx, cy)) {
                drawRect(
                    color = palette[c.colorIdx].copy(alpha = alpha),
                    topLeft = Offset(x, y),
                    size = Size(c.w, c.w * 0.6f),
                )
            }
        }
    }
}
