package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.reduceMotion
import kotlinx.coroutines.delay

/**
 * O app é local-first e o Room emite lista vazia imediatamente no cold start —
 * antes do primeiro sync chegar. Sem um sinal explícito de loading, telas
 * mostravam "não encontrado"/empty state por alguns segundos e depois "pulavam"
 * pro conteúdo real.
 *
 * Este gate segura um estado de loading enquanto (a) não há dado E (b) ainda
 * estamos dentro da janela de graça do primeiro sync. Se o dado chegar, o
 * conteúdo aparece na hora; se realmente não existir nada, o empty state real
 * aparece após a janela.
 */
@Composable
fun rememberShowLoading(hasData: Boolean, graceMs: Long = 2_500): Boolean {
    var graceOver by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(graceMs)
        graceOver = true
    }
    return !hasData && !graceOver
}

/**
 * Loading de tela cheia com a MARCA (3.14): lombadas de livro "respirando" em
 * onda sobre a linha da prateleira — era o spinner Material genérico, e este é
 * o único loading full-screen do app. Geometria pura (rounded rects), UM relógio
 * de animação; sob reduced-motion as lombadas ficam paradas na altura cheia.
 */
@Composable
fun CenteredLoading(modifier: Modifier = Modifier) {
    val reduce = reduceMotion()
    val t = if (reduce) 0f else {
        val transition = rememberInfiniteTransition(label = "brandLoader")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "brandLoaderT",
        ).value
    }
    // Onda triangular com fase por lombada: 1 relógio, 4 fases.
    fun wave(phase: Float): Float {
        if (reduce) return 1f
        val x = (t + phase) % 1f
        return 0.62f + 0.38f * (1f - kotlin.math.abs(x * 2f - 1f))
    }
    val spines = listOf(
        Triple(6.dp, 22.dp, RodapeTheme.colors.oliva),
        Triple(7.dp, 28.dp, RodapeTheme.colors.terracota),
        Triple(6.dp, 24.dp, RodapeTheme.colors.dourado),
        Triple(7.dp, 19.dp, RodapeTheme.colors.olivaMid),
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Carregando" },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                spines.forEachIndexed { i, (w, h, color) ->
                    Box(
                        Modifier
                            .width(w)
                            .height(h)
                            .graphicsLayer {
                                scaleY = wave(i * 0.22f)
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                            }
                            .clip(RoundedCornerShape(RodapeRadii.xs))
                            .background(color)
                    )
                }
            }
            Box(
                Modifier
                    .width(44.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(RodapeRadii.full))
                    .background(RodapeTheme.colors.divider)
            )
        }
    }
}
