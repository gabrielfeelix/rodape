package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Cream
import com.example.ui.theme.Divider
import com.example.ui.theme.Ink
import com.example.ui.theme.Oliva
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.Terracota
import com.example.ui.theme.TerracotaDark
import com.example.ui.theme.TerracotaSoft

enum class TbButtonVariant { Primary, Terra, Outline, Soft, Dark, OliveSoft }
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
) {
    val style = when (variant) {
        TbButtonVariant.Primary -> ButtonStyle(Oliva, Cream, null)
        TbButtonVariant.Terra -> ButtonStyle(Terracota, Cream, null)
        TbButtonVariant.Outline -> ButtonStyle(Color.White, Ink, BorderStroke(1.dp, Divider))
        TbButtonVariant.Soft -> ButtonStyle(TerracotaSoft, TerracotaDark, null)
        TbButtonVariant.Dark -> ButtonStyle(Ink, Cream, null)
        TbButtonVariant.OliveSoft -> ButtonStyle(OlivaSoft, OlivaDark, null)
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

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(999.dp),
        border = style.border,
        contentPadding = PaddingValues(horizontal = 22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = style.bg,
            contentColor = style.fg,
            disabledContainerColor = Color(0xFFD8C9B8),
            disabledContentColor = Cream,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize.sp,
            ),
        )
    }
}
