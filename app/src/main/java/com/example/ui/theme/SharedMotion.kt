package com.example.ui.theme

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Escopos de shared-element expostos via CompositionLocal (Onda 4). Providos UMA
 * vez no NavHost (`SharedTransitionLayout` + cada `composable{}` provê seu
 * `AnimatedContentScope`). Assim o day-stamp lá no fundo (NavHost → MainTabs →
 * NextTab → ticket) aplica o shared-element SEM threading dos escopos por 4
 * assinaturas de composable.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedScope = compositionLocalOf<AnimatedContentScope?> { null }

/**
 * Aplica um shared-element pela [key], lendo os escopos dos Locals. No-op se
 * algum faltar (preview/telas fora do NavHost). A mesma [key] nos dois lados
 * (ticket da NextTab e header do MeetingDetail) faz o day-stamp "voar" entre eles.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.rodapeSharedElement(key: String): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return this
    val avs = LocalNavAnimatedScope.current ?: return this
    return with(sts) {
        this@rodapeSharedElement.sharedElement(
            rememberSharedContentState(key = key),
            animatedVisibilityScope = avs,
        )
    }
}
