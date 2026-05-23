package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun EditClubInfoDialog(
    initialNome: String,
    initialDescricao: String,
    initialCorIndex: String,
    initialPrivacidade: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var nome by remember { mutableStateOf(initialNome) }
    var descricao by remember { mutableStateOf(initialDescricao) }
    var corIndex by remember { mutableStateOf(initialCorIndex.toIntOrNull() ?: 0) }
    var privacidade by remember { mutableStateOf(initialPrivacidade) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Editar clube",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { if (it.length <= 40) nome = it },
                    label = { Text("Nome do clube") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { if (it.length <= 140) descricao = it },
                    label = { Text("Descrição") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Cor", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClubColors.forEachIndexed { idx, c ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .border(
                                    width = if (corIndex == idx) 2.dp else 0.dp,
                                    color = if (corIndex == idx) Ink else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(c.bg)
                                .clickable { corIndex = idx }
                        )
                    }
                }
                Text("Privacidade", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("convidados" to "Só convidados", "publico" to "Aberto a quem tem link").forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { privacidade = key }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = privacidade == key, onClick = { privacidade = key })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(nome, descricao, corIndex.toString(), privacidade) },
                enabled = nome.trim().length >= 3
            ) { Text("Salvar", color = Oliva, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) }
        }
    )
}

@Composable
fun RemoveMemberDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remover $memberName?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "A pessoa recebe uma notificação. Comentários e frases dela ficam no histórico.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Muted)
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { if (it.length <= 200) motivo = it },
                    label = { Text("Motivo (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(motivo) }) {
                Text("Remover", color = Terracota, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) }
        }
    )
}

@Composable
fun RegenerateCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gerar novo código?") },
        text = {
            Text(
                "O código atual deixa de funcionar. Quem ainda não entrou precisa receber o novo código.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Gerar novo", color = Terracota, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) }
        }
    )
}

@Composable
fun EditMeetingPatternDialog(
    initialDiaSemana: Int,
    initialHora: String,
    initialLocal: String,
    initialAgenda: String,
    onDismiss: () -> Unit,
    onSave: (Int, String, String, String) -> Unit
) {
    var diaSemana by remember { mutableStateOf(initialDiaSemana) }
    var hora by remember { mutableStateOf(initialHora) }
    var local by remember { mutableStateOf(initialLocal) }
    var agenda by remember { mutableStateOf(initialAgenda) }

    val diasLabel = listOf(
        java.util.Calendar.SUNDAY to "Domingo",
        java.util.Calendar.MONDAY to "Segunda",
        java.util.Calendar.TUESDAY to "Terça",
        java.util.Calendar.WEDNESDAY to "Quarta",
        java.util.Calendar.THURSDAY to "Quinta",
        java.util.Calendar.FRIDAY to "Sexta",
        java.util.Calendar.SATURDAY to "Sábado"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Padrão de encontros") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dia da semana", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Column {
                    diasLabel.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { diaSemana = key }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = diaSemana == key, onClick = { diaSemana = key })
                            Text(label)
                        }
                    }
                }
                OutlinedTextField(
                    value = hora,
                    onValueChange = { hora = it.take(5) },
                    label = { Text("Hora (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = local,
                    onValueChange = { local = it },
                    label = { Text("Local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = agenda,
                    onValueChange = { if (it.length <= 140) agenda = it },
                    label = { Text("Agenda padrão") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(diaSemana, hora, local, agenda) }) {
                Text("Salvar", color = Oliva, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) }
        }
    )
}

@Composable
fun EditSingleMeetingDialog(
    initialData: String,
    initialHora: String,
    initialLocal: String,
    initialAgenda: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var data by remember { mutableStateOf(initialData) }
    var hora by remember { mutableStateOf(initialHora) }
    var local by remember { mutableStateOf(initialLocal) }
    var agenda by remember { mutableStateOf(initialAgenda) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encontro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = data,
                    onValueChange = { data = it },
                    label = { Text("Data (ex: DOMINGO, 24 DE OUTUBRO)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hora,
                    onValueChange = { hora = it.take(12) },
                    label = { Text("Hora (ex: 19:00 — 21:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = local,
                    onValueChange = { local = it },
                    label = { Text("Local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = agenda,
                    onValueChange = { if (it.length <= 280) agenda = it },
                    label = { Text("Agenda") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(data, hora, local, agenda) },
                enabled = data.trim().isNotEmpty() && hora.trim().isNotEmpty()
            ) { Text("Salvar", color = Oliva, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) }
        }
    )
}

@Composable
fun CancelMeetingDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancelar encontro?") },
        text = { Text("Os RSVPs serão descartados. Sem volta.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Cancelar encontro", color = Terracota, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Voltar", color = Muted) }
        }
    )
}
