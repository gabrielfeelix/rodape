package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.reduceMotion

/**
 * Skeleton/shimmer placeholder pra mostrar enquanto dados HTTP carregam.
 *
 * Shimmer REAL: varredura de gradiente diagonal (~20°), tinta
 * `divider → cardSoft → divider` — não mais o pulso de opacidade antigo.
 *
 * Performance (importante): o progresso é lido **só na draw phase**
 * ([drawBehind] + lambda `() -> Float`) — a animação re-DESENHA a caixa a cada
 * frame sem recompor nem re-layoutar nada. E cada *grupo* de skeleton
 * ([SkeletonRow], [SkeletonRowList], [SkeletonReadingCard]…) hoisteia UMA
 * `rememberInfiniteTransition` e passa pros filhos — N caixas, 1 relógio,
 * varredura sincronizada.
 *
 * Reduced-motion: a varredura para (fill estático em divider) — quem pediu
 * "remover animações" no sistema não ganha gradiente passeando.
 *
 * Padrões de uso:
 *  if (data == null) SkeletonBox(...) else RealContent(data)
 *  ou
 *  if (data.isEmpty() && stillLoading) SkeletonList(items=3) else ContentList(data)
 */
@Composable
fun rememberShimmerProgress(): State<Float> {
    // Sob reduced-motion devolve progresso parado — os primitivos também trocam
    // o highlight pela base, então o resultado é um fill chapado, sem banda.
    if (reduceMotion()) {
        return remember { mutableFloatStateOf(0f) }
    }
    val transition = rememberInfiniteTransition(label = "shimmer")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
}

/**
 * Pinta a varredura. `progress` é lambda de propósito: a leitura acontece dentro
 * do draw scope, então o frame só invalida desenho (não composição/layout).
 * Banda ~70% da largura, inclinação ~20° (tan ≈ 0,36). Fora da banda o
 * TileMode.Clamp segura a cor base.
 */
private fun Modifier.shimmerFill(
    base: Color,
    highlight: Color,
    progress: () -> Float,
): Modifier = drawBehind {
    val band = size.width.coerceAtLeast(1f) * 0.7f
    val travel = size.width + band * 2f
    val x = -band + travel * progress()
    drawRect(
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(x, 0f),
            end = Offset(x + band, band * 0.36f),
        )
    )
}

/** Cores do shimmer no tema ativo; sob reduced-motion o highlight vira a base. */
@Composable
private fun shimmerColors(): Pair<Color, Color> {
    val base = RodapeTheme.colors.divider.copy(alpha = 0.5f)
    val highlight = if (reduceMotion()) base else RodapeTheme.colors.cardSoft
    return base to highlight
}

/** Caixa retangular com varredura — bloco básico. */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = RodapeRadii.sm,
    progress: (() -> Float)? = null,
) {
    val p = progress ?: rememberShimmerProgress().let { s -> { s.value } }
    val (base, highlight) = shimmerColors()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .shimmerFill(base, highlight, p)
    )
}

/** Linha de texto skeleton — height fixa, width paramétrico. */
@Composable
fun SkeletonText(
    width: Dp,
    height: Dp = 14.dp,
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
) {
    SkeletonBox(
        modifier = modifier.width(width).height(height),
        cornerRadius = RodapeRadii.xs,
        progress = progress,
    )
}

/** Avatar circular skeleton. */
@Composable
fun SkeletonAvatar(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
) {
    val p = progress ?: rememberShimmerProgress().let { s -> { s.value } }
    val (base, highlight) = shimmerColors()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .shimmerFill(base, highlight, p)
    )
}

/** Card horizontal skeleton: avatar + 2 linhas. Pra membros, notificações, etc. */
@Composable
fun SkeletonRow(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp,
    progress: (() -> Float)? = null,
) {
    // Hoisteia um relógio pro grupo inteiro (avatar + 2 linhas).
    val p = progress ?: rememberShimmerProgress().let { s -> { s.value } }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonAvatar(size = avatarSize, progress = p)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SkeletonText(width = 160.dp, progress = p)
            SkeletonText(width = 100.dp, height = 12.dp, progress = p)
        }
    }
}

/** Lista vertical de N skeletons, com espaçamento padrão. */
@Composable
fun SkeletonRowList(
    count: Int = 3,
    modifier: Modifier = Modifier,
) {
    // UM relógio pra lista inteira — varredura desce sincronizada.
    val progress = rememberShimmerProgress()
    val p = remember(progress) { { progress.value } }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(count) { SkeletonRow(progress = p) }
    }
}

/** Card de próximo encontro skeleton — usa a mesma área visual do real. */
@Composable
fun SkeletonMeetingCard(modifier: Modifier = Modifier) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        cornerRadius = RodapeRadii.md,
    )
}

/** Card "Tua leitura" skeleton — capa + 3 linhas de texto + barra de progresso. */
@Composable
fun SkeletonReadingCard(modifier: Modifier = Modifier) {
    val progress = rememberShimmerProgress()
    val p = remember(progress) { { progress.value } }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Capa: raio xs=3, igual à capa real (era 4 — drift).
        SkeletonBox(
            modifier = Modifier.width(48.dp).height(72.dp),
            cornerRadius = RodapeRadii.xs,
            progress = p,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SkeletonText(width = 120.dp, height = 11.dp, progress = p)
            SkeletonText(width = 180.dp, height = 16.dp, progress = p)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(
                modifier = Modifier.fillMaxWidth().height(6.dp),
                cornerRadius = RodapeRadii.full,
                progress = p,
            )
        }
    }
}
