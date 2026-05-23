package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Avatar
import com.example.ui.components.Pill
import com.example.ui.components.PillVariant
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.TramabookCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.timeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    viewModel: MainViewModel,
    meetingId: String,
    onNavigateBack: () -> Unit
) {
    val meetingFlow = remember(meetingId) { viewModel.getMeetingByIdFlow(meetingId) }
    val meeting by meetingFlow.collectAsState(initial = null)
    val rsvpFlow = remember(meetingId) { viewModel.getRsvpOfUser(meetingId) }
    val userRsvp by rsvpFlow.collectAsState(initial = null)
    val members by viewModel.clubMembers.collectAsState()
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()
    val minutesFlow = remember(meetingId) { viewModel.getMeetingMinutesFlow(meetingId) }
    val minutes by minutesFlow.collectAsState(initial = null)
    val noteFlow = remember(meetingId) { viewModel.getMyMeetingNoteFlow(meetingId) }
    val myNote by noteFlow.collectAsState(initial = null)

    var showMinutesEdit by remember { mutableStateOf(false) }
    var minutesDraft by remember { mutableStateOf("") }
    var noteDraft by remember(myNote) { mutableStateOf(myNote?.texto ?: "") }
    var noteEditing by remember { mutableStateOf(false) }

    val m = meeting
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encontro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (m == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Encontro não encontrado.", style = MaterialTheme.typography.bodyLarge, color = Muted)
            }
            return@Scaffold
        }

        val concluded = m.status == "concluido"

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (concluded) DividerSoft.copy(alpha = 0.3f) else Cream)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (concluded) Muted.copy(alpha = 0.15f)
                                else Terracota.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val dayNumber = m.data
                            .substringAfter(",", "")
                            .trim()
                            .takeWhile { it.isDigit() }
                            .ifEmpty { m.data.trim().takeWhile { it.isDigit() }.ifEmpty { "—" } }
                        Text(
                            text = dayNumber,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (concluded) Muted else Terracota,
                                fontFamily = LiterataFontFamily
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = m.data,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (concluded) Muted else Ink
                                ),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (concluded) {
                                Pill(text = "Concluído", variant = PillVariant.Olive)
                            }
                        }
                        Text(m.hora, style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Place, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                            Text(m.local, style = MaterialTheme.typography.bodySmall.copy(color = Muted))
                        }
                        if (m.chapterStart != null && m.chapterEnd != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📖 Caps ${m.chapterStart}–${m.chapterEnd}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Terracota,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // Agenda
            if (m.agenda.isNotBlank()) {
                item {
                    Text(
                        text = "AGENDA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Muted,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TramabookCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = m.agenda,
                            style = MaterialTheme.typography.bodyMedium.copy(color = InkSoft, lineHeight = 20.sp)
                        )
                    }
                }
            }

            // RSVP
            if (!concluded) {
                item {
                    Text(
                        text = "SUA PARTICIPAÇÃO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Muted,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TramabookCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Vou", "Talvez", "Não vou").forEach { opt ->
                                val sel = userRsvp?.status == opt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (sel) Ink else androidx.compose.ui.graphics.Color.Transparent)
                                        .clickable { viewModel.rsvpMeeting(m.id, opt) }
                                        .then(
                                            if (!sel) Modifier.background(androidx.compose.ui.graphics.Color.Transparent)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (sel) Cream else Tertiary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ata (Minutes)
            item {
                Text(
                    text = "ATA DO ENCONTRO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Muted,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (minutes == null) {
                    TramabookCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Ninguém escreveu a ata deste encontro ainda.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Muted)
                        )
                    }
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TbButton(
                            text = "Escrever ata",
                            onClick = {
                                minutesDraft = ""
                                showMinutesEdit = true
                            },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    val editor = members.find { it.id == minutes!!.lastEditorId }
                    TramabookCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = minutes!!.texto,
                            style = MaterialTheme.typography.bodyMedium.copy(color = InkSoft, lineHeight = 20.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Editado por ${editor?.nome ?: "—"} · ${timeAgo(minutes!!.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TbButton(
                            text = "Editar ata",
                            onClick = {
                                minutesDraft = minutes!!.texto
                                showMinutesEdit = true
                            },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Suas anotações (privadas)
            item {
                Text(
                    text = "SUAS ANOTAÇÕES (PRIVADAS)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Muted,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (noteEditing) {
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        placeholder = { Text("Anote o que quiser sobre este encontro. Só você vê.") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TbButton(
                            text = "Cancelar",
                            onClick = {
                                noteDraft = myNote?.texto ?: ""
                                noteEditing = false
                            },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.weight(1f)
                        )
                        TbButton(
                            text = "Salvar",
                            onClick = {
                                viewModel.saveMyMeetingNote(m.id, noteDraft)
                                noteEditing = false
                            },
                            variant = TbButtonVariant.Terra,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    TramabookCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = myNote?.texto?.takeIf { it.isNotBlank() }
                                ?: "Você ainda não anotou nada para este encontro.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (myNote?.texto.isNullOrBlank()) Muted else InkSoft,
                                lineHeight = 20.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TbButton(
                        text = if (myNote == null || myNote!!.texto.isBlank()) "Anotar" else "Editar anotações",
                        onClick = { noteEditing = true },
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Botão admin: Concluir encontro
            if (isAdmin && !concluded) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    TbButton(
                        text = "Marcar encontro como concluído",
                        onClick = { viewModel.concludeMeeting(m.id) },
                        variant = TbButtonVariant.Terra,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Se este for o último do livro atual, o livro vai pra Estante automaticamente.",
                        style = MaterialTheme.typography.labelSmall.copy(color = Muted),
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    // Dialog editar ata
    if (showMinutesEdit) {
        AlertDialog(
            onDismissRequest = { showMinutesEdit = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Ata do encontro",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = minutesDraft,
                    onValueChange = { minutesDraft = it },
                    placeholder = { Text("O que rolou no encontro? Decisões, momentos, citações…") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (minutesDraft.isNotBlank() && m != null) {
                        viewModel.saveMeetingMinutes(m.id, minutesDraft)
                    }
                    showMinutesEdit = false
                }) { Text("Salvar", color = Oliva, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showMinutesEdit = false }) {
                    Text("Cancelar", color = Muted)
                }
            }
        )
    }
}
