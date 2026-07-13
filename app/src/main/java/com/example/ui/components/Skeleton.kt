package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Divider
import com.example.ui.theme.RodapeTheme

/**
 * Skeleton/shimmer placeholder pra mostrar enquanto dados HTTP carregam.
 * Usa animacao infinita de opacidade (subtil — nao pisca).
 *
 * Padroes de uso:
 *  if (data == null) SkeletonBox(...) else RealContent(data)
 *  ou
 *  if (data.isEmpty() && stillLoading) SkeletonList(items=3) else ContentList(data)
 */
@Composable
private fun shimmerAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    return alpha
}

/** Caixa retangular com cor pulsando — bloco basico. */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    val alpha = shimmerAlpha()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(RodapeTheme.colors.divider.copy(alpha = alpha))
    )
}

/** Linha de texto skeleton — height fixa, width parametrico. */
@Composable
fun SkeletonText(
    width: Dp,
    height: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    SkeletonBox(modifier = modifier.width(width).height(height), cornerRadius = 4.dp)
}

/** Avatar circular skeleton. */
@Composable
fun SkeletonAvatar(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val alpha = shimmerAlpha()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(RodapeTheme.colors.divider.copy(alpha = alpha))
    )
}

/** Card horizontal skeleton: avatar + 2 linhas. Pra membros, notificacoes, etc. */
@Composable
fun SkeletonRow(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 40.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonAvatar(size = avatarSize)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SkeletonText(width = 160.dp)
            SkeletonText(width = 100.dp, height = 12.dp)
        }
    }
}

/** Lista vertical de N skeletons, com espacamento padrao. */
@Composable
fun SkeletonRowList(
    count: Int = 3,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(count) { SkeletonRow() }
    }
}

/** Card de proximo encontro skeleton — usa a mesma area visual do real. */
@Composable
fun SkeletonMeetingCard(modifier: Modifier = Modifier) {
    val alpha = shimmerAlpha()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(RodapeTheme.colors.divider.copy(alpha = alpha))
    )
}

/** Card "Tua leitura" skeleton — capa + 3 linhas de texto + barra de progresso. */
@Composable
fun SkeletonReadingCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SkeletonBox(modifier = Modifier.width(48.dp).height(72.dp), cornerRadius = 4.dp)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SkeletonText(width = 120.dp, height = 11.dp)
            SkeletonText(width = 180.dp, height = 16.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(6.dp), cornerRadius = 999.dp)
        }
    }
}
