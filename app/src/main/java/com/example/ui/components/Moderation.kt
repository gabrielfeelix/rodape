package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.model.ReportReason
import com.example.data.model.ReportTargetType
import com.example.ui.theme.RodapeTheme

/**
 * Diálogo de DENÚNCIA de conteúdo (moderação UGC — requisito de loja).
 * Escolhe um [ReportReason] e opcionalmente um detalhe. Reutilizável em qualquer
 * superfície com conteúdo de outro usuário (chat, citação, resenha, sugestão, perfil).
 */
@Composable
fun ReportDialog(
    targetType: ReportTargetType,
    onDismiss: () -> Unit,
    onSubmit: (ReportReason, String?) -> Unit,
) {
    var motivo by remember { mutableStateOf(ReportReason.SPAM) }
    var detalhe by remember { mutableStateOf("") }

    val alvo = when (targetType) {
        ReportTargetType.COMMENT -> "este comentário"
        ReportTargetType.SAVED_QUOTE -> "esta citação"
        ReportTargetType.BOOK_RATING -> "esta resenha"
        ReportTargetType.BOOK_SUGGESTION -> "esta sugestão"
        ReportTargetType.PROFILE -> "este perfil"
        ReportTargetType.REACTION -> "esta reação"
    }

    RodapeDialog(
        onDismissRequest = onDismiss,
        title = "Denunciar $alvo",
        text = {
            Column {
                Text(
                    "Por que você está denunciando?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RodapeTheme.colors.tertiary,
                )
                Spacer(Modifier.height(8.dp))
                ReportReason.entries.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { motivo = r }
                            .padding(vertical = 2.dp),
                    ) {
                        ThemedRadio(selected = motivo == r, onClick = { motivo = r })
                        Text(
                            r.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = RodapeTheme.colors.ink,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = detalhe,
                    onValueChange = { detalhe = it.take(1000) },
                    label = { Text("Detalhe (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(motivo, detalhe.trim().ifBlank { null }) }) {
                Text("Enviar denúncia", color = RodapeTheme.colors.terracotaDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = RodapeTheme.colors.muted)
            }
        },
    )
}

/**
 * Confirmação de BLOQUEIO de usuário. Explica o efeito (esconde conteúdo dos dois
 * lados). Requisito de loja pra apps com UGC.
 */
@Composable
fun BlockConfirmDialog(
    nome: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    RodapeDialog(
        onDismissRequest = onDismiss,
        title = "Bloquear $nome?",
        text = {
            Text(
                "Você deixa de ver o conteúdo de $nome, e essa pessoa deixa de ver o seu. " +
                    "Você pode desfazer depois em Ajustes.",
                style = MaterialTheme.typography.bodyMedium,
                color = RodapeTheme.colors.ink,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Bloquear", color = RodapeTheme.colors.terracotaDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = RodapeTheme.colors.muted)
            }
        },
    )
}
