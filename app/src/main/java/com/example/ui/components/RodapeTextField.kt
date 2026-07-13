package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.RodapeRadii

/**
 * Campo de texto ÚNICO do Rodapé (Onda 1 do PLANO-UI).
 *
 * Antes: CreateClub/JoinClub tinham o tratamento rico (fill cream, foco terracota,
 * raio 14dp) e TODA tela de auth/senha usava `OutlinedTextField` cru do Material —
 * os primeiros campos que o usuário toca eram os menos cuidados. Este wrapper
 * unifica: fill cream, borda de foco no acento, raio [RodapeRadii.sm], label/erro
 * padronizados. Use em todo formulário.
 *
 * [accent] resolve pra `terracota` quando não informado (padrão dos formulários);
 * passe `RodapeTheme.colors.oliva` onde o oliva for o acento local (ex.: comentário).
 */
@Composable
fun RodapeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 6,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    accent: Color = Color.Unspecified,
) {
    val accentColor = if (accent == Color.Unspecified) RodapeTheme.colors.terracota else accent

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = RodapeTheme.colors.muted,
                    letterSpacing = 0.4.sp,
                ),
            )
            Spacer(Modifier.height(6.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            shape = RoundedCornerShape(RodapeRadii.sm),
            placeholder = if (placeholder != null) {
                {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = RodapeTheme.colors.muted,
                        ),
                    )
                }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = RodapeTheme.colors.divider,
                errorBorderColor = RodapeTheme.colors.terracota,
                cursorColor = accentColor,
                focusedContainerColor = RodapeTheme.colors.cream,
                unfocusedContainerColor = RodapeTheme.colors.cream,
                errorContainerColor = RodapeTheme.colors.cream,
                focusedTextColor = RodapeTheme.colors.ink,
                unfocusedTextColor = RodapeTheme.colors.ink,
            ),
        )
        if (supportingText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isError) RodapeTheme.colors.terracota else RodapeTheme.colors.muted,
                ),
            )
        }
    }
}
