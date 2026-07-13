package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Cream
import com.example.ui.theme.DisabledSurface
import com.example.ui.theme.Divider
import com.example.ui.theme.Ink
import com.example.ui.theme.Oliva
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.Terracota
import com.example.ui.theme.TerracotaDark
import com.example.ui.theme.TerracotaSoft

enum class TbButtonVariant { Primary, Terra, TerraSoft, Outline, Dark, OlivaSoft }
enum class TbButtonSize { Sm, Md, Lg }

private data class ButtonStyle(val bg: Color, val fg: Color, val border: BorderStroke?)

@Composable
fun TbButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TbButtonVariant = TbButtonVariant.Primary,
    size: TbButtonSize = TbButtonSize.Md,
    enabled: Boolean = true,
    // Estado ocupado: troca o rótulo por um spinner (contentColor) e bloqueia o
    // clique. Antes todo fluxo async ou travava o label ou trocava a tela inteira.
    loading: Boolean = false,
    // Ícone líder opcional (tingido no contentColor). Substitui o emoji cravado em
    // labels de botão (📷/📚/📲…) — passe RodapeIcons.* aqui.
    leadingIcon: ImageVector? = null,
) {
    val style = when (variant) {
        TbButtonVariant.Primary -> ButtonStyle(RodapeTheme.colors.oliva, RodapeTheme.colors.cream, null)
        TbButtonVariant.Terra -> ButtonStyle(RodapeTheme.colors.terracota, RodapeTheme.colors.cream, null)
        TbButtonVariant.TerraSoft -> ButtonStyle(RodapeTheme.colors.terracotaSoft, RodapeTheme.colors.terracotaDark, null)
        TbButtonVariant.Outline -> ButtonStyle(RodapeTheme.colors.cardSurface, RodapeTheme.colors.ink, BorderStroke(1.dp, RodapeTheme.colors.divider))
        TbButtonVariant.Dark -> ButtonStyle(RodapeTheme.colors.ink, RodapeTheme.colors.cream, null)
        TbButtonVariant.OlivaSoft -> ButtonStyle(RodapeTheme.colors.olivaSoft, RodapeTheme.colors.olivaDark, null)
    }
    val height = when (size) {
        TbButtonSize.Sm -> 32.dp
        TbButtonSize.Md -> 46.dp
        TbButtonSize.Lg -> 54.dp
    }
    val fontSize = when (size) {
        TbButtonSize.Sm -> 13
        TbButtonSize.Md -> 15
        TbButtonSize.Lg -> 16
    }
    val horizontalPadding = when (size) {
        TbButtonSize.Sm -> 12.dp
        TbButtonSize.Md -> 18.dp
        TbButtonSize.Lg -> 22.dp
    }

    // Press-scale 0.98 do design (PillButton) no lugar do ripple puro
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "pressScale",
    )

    Button(
        onClick = onClick,
        // Enquanto carrega, o botão não dispara (mantém o fill de acento, não o
        // cinza de disabled — parece "ocupado", não "bloqueado").
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        border = style.border,
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        colors = ButtonDefaults.buttonColors(
            containerColor = style.bg,
            contentColor = style.fg,
            // Loading não é disabled: preserva as cores de acento também no estado
            // desabilitado-por-loading pra não piscar cinza.
            disabledContainerColor = if (loading) style.bg else RodapeTheme.colors.disabledSurface,
            disabledContentColor = if (loading) style.fg else RodapeTheme.colors.ink.copy(alpha = 0.38f),
        ),
    ) {
        Crossfade(targetState = loading, label = "tbButtonLoading") { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(
                    color = style.fg,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size((fontSize + 3).dp),
                )
            } else if (leadingIcon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = style.fg,
                        modifier = Modifier.size((fontSize + 3).dp),
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = fontSize.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = fontSize.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
