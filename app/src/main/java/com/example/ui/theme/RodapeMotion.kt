package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * RodapeMotion — sistema de movimento do design system.
 *
 * Durations e easings NOMEADOS (chega de número mágico de animação espalhado) e,
 * o mais importante, respeito a **reduced-motion** ("remover animações" do sistema)
 * via [LocalReducedMotion], provido uma vez no root ([MyApplicationTheme]).
 *
 * Prefira os helpers `@Composable` [rodapeTween]/[rodapeSpring]: eles já degradam
 * pra instantâneo quando reduced-motion está ligado — acessibilidade deixa de ser
 * um checklist manual em cada call-site. Para entradas encenadas (stagger), o
 * call-site checa [reduceMotion] e simplesmente renderiza no estado final.
 */
object RodapeMotion {
    /** Durations em ms. */
    object Dur {
        const val fast = 120
        const val standard = 240
        const val emphasized = 400
    }

    /** Easings. `emphasized*` = curvas "emphasized" do Material 3. */
    object Ease {
        val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }
}

/**
 * Verdadeiro quando o usuário pediu "remover animações" no sistema
 * (ANIMATOR_DURATION_SCALE = 0). Provido no root; default `false` pra previews/testes.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Açúcar: `if (reduceMotion()) …`. */
@Composable
@ReadOnlyComposable
fun reduceMotion(): Boolean = LocalReducedMotion.current

/**
 * Tween do design system que vira instantâneo (duração 0) sob reduced-motion.
 * Retorna [FiniteAnimationSpec] pra encaixar em `animate*AsState`, `AnimatedContent`,
 * `animateContentSize`, etc.
 */
@Composable
fun <T> rodapeTween(
    durationMillis: Int = RodapeMotion.Dur.standard,
    delayMillis: Int = 0,
    easing: Easing = RodapeMotion.Ease.standard,
): FiniteAnimationSpec<T> = if (LocalReducedMotion.current) {
    snap()
} else {
    tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = easing)
}

/**
 * Spring do design system que vira instantâneo sob reduced-motion. Bom pra
 * scale-punch/mola de ícone e badge.
 */
@Composable
fun <T> rodapeSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
): FiniteAnimationSpec<T> = if (LocalReducedMotion.current) {
    snap()
} else {
    spring(dampingRatio = dampingRatio, stiffness = stiffness)
}
