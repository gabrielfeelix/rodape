package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
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
        containerColor = MaterialTheme.colorScheme.surface,
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
        containerColor = MaterialTheme.colorScheme.surface,
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
        containerColor = MaterialTheme.colorScheme.surface,
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
    initialTipoRecorrencia: String,
    initialValorRecorrencia: Int,
    onDismiss: () -> Unit,
    onSave: (diaSemana: Int, hora: String, local: String, agenda: String, tipoRecorrencia: String, valorRecorrencia: Int) -> Unit
) {
    var diaSemana by remember { mutableStateOf(initialDiaSemana) }
    var hora by remember { mutableStateOf(initialHora) }
    var local by remember { mutableStateOf(initialLocal) }
    var agenda by remember { mutableStateOf(initialAgenda) }
    var tipo by remember { mutableStateOf(initialTipoRecorrencia.ifBlank { "semanal" }) }
    var valor by remember { mutableStateOf(initialValorRecorrencia) }

    val diasLabel = listOf(
        java.util.Calendar.SUNDAY to "Dom",
        java.util.Calendar.MONDAY to "Seg",
        java.util.Calendar.TUESDAY to "Ter",
        java.util.Calendar.WEDNESDAY to "Qua",
        java.util.Calendar.THURSDAY to "Qui",
        java.util.Calendar.FRIDAY to "Sex",
        java.util.Calendar.SATURDAY to "Sáb"
    )

    val tiposLabel = listOf(
        "semanal" to "Toda semana",
        "quinzenal" to "A cada 15 dias",
        "mensal_dia_semana" to "1x por mês (mesmo dia da semana)",
        "mensal_dia_mes" to "1x por mês (dia fixo do mês)",
        "personalizado_dias" to "A cada N dias"
    )

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("Padrão de encontros") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        "Como repete?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Column {
                        tiposLabel.forEach { (key, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tipo = key
                                        valor = when (key) {
                                            "mensal_dia_semana" -> 1
                                            "mensal_dia_mes" -> 1
                                            "personalizado_dias" -> 21
                                            else -> 0
                                        }
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = tipo == key,
                                    onClick = {
                                        tipo = key
                                        valor = when (key) {
                                            "mensal_dia_semana" -> 1
                                            "mensal_dia_mes" -> 1
                                            "personalizado_dias" -> 21
                                            else -> 0
                                        }
                                    }
                                )
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (tipo in setOf("semanal", "quinzenal", "mensal_dia_semana")) {
                    item {
                        Text(
                            "Dia da semana",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            diasLabel.forEach { (key, label) ->
                                val selected = diaSemana == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { diaSemana = key }
                                        .background(
                                            if (selected) Oliva else Color.Transparent,
                                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) Oliva else Muted.copy(alpha = 0.3f),
                                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (selected) MaterialTheme.colorScheme.surface else Muted,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (tipo == "mensal_dia_semana") {
                    item {
                        Text(
                            "Qual semana do mês?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1 to "1ª", 2 to "2ª", 3 to "3ª", 4 to "4ª", 5 to "Última").forEach { (n, label) ->
                                val selected = valor == n
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { valor = n }
                                        .background(
                                            if (selected) Oliva else Color.Transparent,
                                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) Oliva else Muted.copy(alpha = 0.3f),
                                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (selected) MaterialTheme.colorScheme.surface else Muted,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (tipo == "mensal_dia_mes") {
                    item {
                        Text(
                            "Dia do mês (1–31)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedTextField(
                            value = valor.toString(),
                            onValueChange = { txt ->
                                if (txt.isBlank()) valor = 1
                                else txt.toIntOrNull()?.let { if (it in 1..31) valor = it }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (tipo == "personalizado_dias") {
                    item {
                        Text(
                            "Repete a cada quantos dias?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedTextField(
                            value = valor.toString(),
                            onValueChange = { txt ->
                                if (txt.isBlank()) valor = 1
                                else txt.toIntOrNull()?.let { if (it in 1..365) valor = it }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = hora,
                        onValueChange = { hora = it.take(5) },
                        label = { Text("Hora (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = local,
                        onValueChange = { local = it },
                        label = { Text("Local") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = agenda,
                        onValueChange = { if (it.length <= 140) agenda = it },
                        label = { Text("Agenda padrão") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(diaSemana, hora, local, agenda, tipo, valor) }) {
                Text("Salvar", color = Oliva, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSingleMeetingDialog(
    initialData: String,
    initialHora: String,
    initialLocal: String,
    initialAgenda: String,
    initialBookId: String?,
    initialChapterStart: Int?,
    initialChapterEnd: Int?,
    currentBookId: String?,
    currentBookTitle: String?,
    totalChapters: Int,
    onDismiss: () -> Unit,
    onSave: (
        data: String,
        hora: String,
        local: String,
        agenda: String,
        bookId: String?,
        chapterStart: Int?,
        chapterEnd: Int?
    ) -> Unit
) {
    // Data vira DatePicker (antes era texto livre que NUNCA casava com o parser,
    // e o encontro virava "agora" pra todos). Prefill: tenta ler a data existente.
    var dateMillis by remember { mutableStateOf(parseMeetingLabelToMillis(initialData)) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Hora fica como HH:MM (24h). Sanitiza o valor inicial pra só o começo.
    var hora by remember { mutableStateOf(sanitizeHora(initialHora)) }
    var local by remember { mutableStateOf(initialLocal) }
    var agenda by remember { mutableStateOf(initialAgenda) }
    // "vincular ao livro atual?" — true se já está vinculado OU se tem livro atual e usuário não desmarcou
    var linkedToCurrentBook by remember { mutableStateOf(initialBookId != null || (currentBookId != null && initialBookId == null && initialChapterStart == null)) }
    var chapterStart by remember { mutableStateOf(initialChapterStart ?: 1) }
    var chapterEnd by remember { mutableStateOf(initialChapterEnd ?: totalChapters.coerceAtLeast(1)) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("Encontro") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = dateMillis?.let { formatMillisBr(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Data do encontro") },
                        placeholder = { Text("Toque para escolher") },
                        trailingIcon = {
                            Icon(Icons.Outlined.DateRange, contentDescription = null, tint = Muted)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Ink,
                            disabledBorderColor = Divider,
                            disabledLabelColor = Muted,
                            disabledTrailingIconColor = Muted,
                            disabledPlaceholderColor = Muted,
                        ),
                    )
                }
                item {
                    OutlinedTextField(
                        value = hora,
                        onValueChange = { txt ->
                            // aceita só dígitos e ":", formato HH:MM
                            hora = txt.filter { it.isDigit() || it == ':' }.take(5)
                        },
                        label = { Text("Hora (24h, ex: 19:00)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = local,
                        onValueChange = { local = it },
                        label = { Text("Local") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = agenda,
                        onValueChange = { if (it.length <= 280) agenda = it },
                        label = { Text("Agenda") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Vincular ao livro atual? ──
                if (currentBookId != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { linkedToCurrentBook = !linkedToCurrentBook }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = linkedToCurrentBook,
                                onCheckedChange = { linkedToCurrentBook = it }
                            )
                            Column {
                                Text(
                                    "Discutir o livro atual",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    "📖 ${currentBookTitle ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Muted)
                                )
                            }
                        }
                    }

                    if (linkedToCurrentBook && totalChapters > 0) {
                        item {
                            Text(
                                "Capítulos deste encontro",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = chapterStart.toString(),
                                    onValueChange = { txt ->
                                        if (txt.isBlank()) chapterStart = 1
                                        else txt.toIntOrNull()?.let { if (it in 1..totalChapters) chapterStart = it }
                                    },
                                    label = { Text("Do cap.") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("até", style = MaterialTheme.typography.bodyMedium)
                                OutlinedTextField(
                                    value = chapterEnd.toString(),
                                    onValueChange = { txt ->
                                        if (txt.isBlank()) chapterEnd = chapterStart
                                        else txt.toIntOrNull()?.let { if (it in 1..totalChapters) chapterEnd = it }
                                    },
                                    label = { Text("o cap.") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                "Total de capítulos no livro: $totalChapters",
                                style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val effectiveStart = if (linkedToCurrentBook && currentBookId != null) chapterStart else null
                    val effectiveEnd = if (linkedToCurrentBook && currentBookId != null) {
                        chapterEnd.coerceAtLeast(chapterStart)
                    } else null
                    val effectiveBookId = if (linkedToCurrentBook) currentBookId else null
                    // Envia data em dd/MM/yyyy (formato que o parser aceita) e hora HH:MM.
                    val dataOut = dateMillis?.let { formatMillisDdMmYyyy(it) } ?: ""
                    onSave(dataOut, sanitizeHora(hora), local, agenda, effectiveBookId, effectiveStart, effectiveEnd)
                },
                enabled = dateMillis != null && isValidHora(hora)
            ) { Text("Salvar", color = Oliva, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK", color = Oliva, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = Muted) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ── Helpers de data/hora do encontro ──
// O banco guarda um timestamp; o parser do repositório aceita "dd/MM/yyyy" + "HH:MM".
private val brDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
private val brDateFriendly = java.text.SimpleDateFormat("EEE, dd 'de' MMM 'de' yyyy", java.util.Locale("pt", "BR"))

private fun formatMillisDdMmYyyy(millis: Long): String = brDate.format(java.util.Date(millis))
private fun formatMillisBr(millis: Long): String =
    brDateFriendly.format(java.util.Date(millis)).replaceFirstChar { it.uppercase() }

/** Extrai a 1ª ocorrência de HH:MM; default 19:00. */
private fun sanitizeHora(raw: String): String {
    val m = Regex("""(\d{1,2}):(\d{2})""").find(raw) ?: return "19:00"
    val h = m.groupValues[1].toInt().coerceIn(0, 23)
    val min = m.groupValues[2].toInt().coerceIn(0, 59)
    return "%02d:%02d".format(h, min)
}

private fun isValidHora(raw: String): Boolean = Regex("""^\d{1,2}:\d{2}$""").matches(raw.trim())

/** Lê a data existente (dd/MM/yyyy OU "SEG, 24 DE OUTUBRO DE 2026") pra prefill; null se não der. */
private fun parseMeetingLabelToMillis(label: String): Long? {
    if (label.isBlank()) return null
    // Formato canônico dd/MM/yyyy
    runCatching { return brDate.parse(label)?.time }.getOrNull()
    // Formato de exibição: "...DD DE <MES> DE YYYY"
    val meses = listOf("janeiro","fevereiro","março","abril","maio","junho","julho","agosto","setembro","outubro","novembro","dezembro")
    val low = label.lowercase()
    val dia = Regex("""\b(\d{1,2})\b""").find(low)?.groupValues?.get(1)?.toIntOrNull() ?: return null
    val mesIdx = meses.indexOfFirst { low.contains(it) }
    if (mesIdx < 0) return null
    val ano = Regex("""\b(20\d{2})\b""").find(low)?.groupValues?.get(1)?.toIntOrNull()
        ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    return runCatching {
        java.util.Calendar.getInstance().apply {
            clear(); set(ano, mesIdx, dia, 12, 0, 0)
        }.timeInMillis
    }.getOrNull()
}

@Composable
fun CancelMeetingDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
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
