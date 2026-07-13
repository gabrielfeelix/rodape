package com.example.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeTheme

/**
 * RodapeDialog — shell de diálogo do design system.
 *
 * Wrapper FINO sobre o `AlertDialog` do M3 (de propósito): preserva todo o
 * comportamento de layout/altura/scroll do Material — o que mais dá errado num
 * diálogo custom — e só corrige o que era "Material vazando":
 *  - raio 28dp (default M3) → `RodapeRadii.md`
 *  - container cinza/`surface` → `cardSurface` (cream, theme-aware)
 *  - título sempre Literata (a `titleLarge` da escala já é Literata SemiBold)
 *
 * As ações ficam nos slots `confirmButton`/`dismissButton` — os call-sites já
 * usam `TextButton` com cor tingida (oliva/terracota/muted), então continuam
 * on-brand. Radio/Checkbox roxos do Material → use [ThemedRadio]/[ThemedCheckbox].
 *
 * Deixado pro checkpoint visual (decisão de design, não risco de compile): ações
 * como `TbButton` filled e sombra tingida custom (exigiria trocar o Surface do M3).
 */
@Composable
fun RodapeDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = RodapeTheme.colors.cardSurface,
        titleContentColor = RodapeTheme.colors.ink,
        textContentColor = RodapeTheme.colors.ink,
        shape = RoundedCornerShape(RodapeRadii.md),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

/** RadioButton tingido oliva (o default do M3 é roxo). */
@Composable
fun ThemedRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        colors = RadioButtonDefaults.colors(
            selectedColor = RodapeTheme.colors.oliva,
            unselectedColor = RodapeTheme.colors.muted,
        ),
    )
}

/** Checkbox tingido oliva (o default do M3 é roxo). */
@Composable
fun ThemedCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = CheckboxDefaults.colors(
            checkedColor = RodapeTheme.colors.oliva,
            uncheckedColor = RodapeTheme.colors.muted,
            checkmarkColor = RodapeTheme.colors.cream,
        ),
    )
}
