package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RodapeMotion
import com.example.ui.theme.reduceMotion

/**
 * Entrada encenada (fade + rise 8dp) — primitivo ÚNICO pra "conteúdo pousando"
 * em Home, estante, listas, heros. Usar isto em vez de reinventar stagger
 * inline em cada tela.
 *
 * - `index` escalona o delay (40ms/degrau) e é CAPADO em [maxSteps]: numa lista
 *   longa o item 40 não fica 1,6s invisível — depois do teto todo mundo entra
 *   no mesmo compasso do último degrau.
 * - Dispara UMA vez, na primeira composição do elemento (em LazyList: quando o
 *   item entra na composição — efeito "prateleira enchendo" ao rolar).
 * - Draw-only ([graphicsLayer]): alpha/translationY não relayoutam nada.
 * - Reduced-motion: curto-circuito — devolve o Modifier intocado (estado final).
 */
@Composable
fun Modifier.staggeredEntrance(
    index: Int = 0,
    baseDelayMs: Int = 40,
    maxSteps: Int = 6,
): Modifier {
    if (reduceMotion()) return this
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val delay = index.coerceAtMost(maxSteps) * baseDelayMs
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(
            durationMillis = RodapeMotion.Dur.standard,
            delayMillis = delay,
            easing = RodapeMotion.Ease.standard,
        ),
        label = "entranceAlpha",
    )
    val rise by animateFloatAsState(
        targetValue = if (entered) 0f else 1f,
        animationSpec = tween(
            durationMillis = RodapeMotion.Dur.emphasized,
            delayMillis = delay,
            easing = RodapeMotion.Ease.emphasizedDecelerate,
        ),
        label = "entranceRise",
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = rise * 8.dp.toPx()
    }
}
