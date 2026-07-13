package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ui.components.TbSectionHeader
import com.example.ui.components.RodapeCard
import com.example.ui.components.SkeletonText
import com.example.ui.components.rememberShowLoading
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

    var showMinutesEdit by rememberSaveable { mutableStateOf(false) }
    var minutesDraft by rememberSaveable { mutableStateOf("") }
    // Descartar por engano apaga o rascunho da ata — confirma quando há texto.
    var showMinutesDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    // Seed-once (R1): o Room emite null no cold start e re-emite a cada sync.
    // remember(myNote) resemeava o rascunho a cada emissão — apagando (a) o texto
    // digitado antes da 1ª emissão e (b) o que o usuário edita quando o flow re-emite.
    // Semeia só na primeira emissão não-nula; rememberSaveable sobrevive à rotação
    // e noteSeeded (também salvo) evita re-semear depois da rotação.
    var noteDraft by rememberSaveable { mutableStateOf(myNote?.texto ?: "") }
    var noteSeeded by rememberSaveable { mutableStateOf(myNote != null) }
    LaunchedEffect(myNote) {
        if (!noteSeeded && myNote != null) {
            noteDraft = myNote?.texto ?: ""
            noteSeeded = true
        }
    }
    var noteEditing by rememberSaveable { mutableStateOf(false) }

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
                // Gate: o flow emite null antes do primeiro sync — loading em
                // vez de "não encontrado" piscando.
                if (com.example.ui.components.rememberShowLoading(hasData = meeting != null)) {
                    com.example.ui.components.CenteredLoading()
                } else {
                    Text("Encontro não encontrado.", style = MaterialTheme.typography.bodyLarge, color = RodapeTheme.colors.muted)
                }
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
                        .background(if (concluded) RodapeTheme.colors.dividerSoft.copy(alpha = 0.3f) else RodapeTheme.colors.cream)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (concluded) RodapeTheme.colors.muted.copy(alpha = 0.15f)
                                else RodapeTheme.colors.terracota.copy(alpha = 0.12f)
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
                                color = if (concluded) RodapeTheme.colors.muted else RodapeTheme.colors.terracota,
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
                                    color = if (concluded) RodapeTheme.colors.muted else RodapeTheme.colors.ink
                                ),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (concluded) {
                                Pill(text = "Concluído", variant = PillVariant.Olive)
                            }
                        }
                        Text(m.hora, style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Place, contentDescription = null, tint = RodapeTheme.colors.muted, modifier = Modifier.size(14.dp))
                            Text(m.local, style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted))
                        }
                        if (m.chapterStart != null && m.chapterEnd != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Caps ${m.chapterStart}–${m.chapterEnd}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = RodapeTheme.colors.terracota,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // Pauta
            if (m.agenda.isNotBlank()) {
                item {
                    TbSectionHeader(
                        title = "Pauta",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    RodapeCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = m.agenda,
                            style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.inkSoft, lineHeight = 20.sp)
                        )
                    }
                }
            }

            // RSVP
            if (!concluded) {
                item {
                    TbSectionHeader(
                        title = "Sua participação",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    RodapeCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Vou", "Talvez", "Não vou").forEach { opt ->
                                val sel = userRsvp?.status == opt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (sel) RodapeTheme.colors.ink else androidx.compose.ui.graphics.Color.Transparent)
                                        .selectable(
                                            selected = sel,
                                            role = Role.RadioButton,
                                            onClick = { viewModel.rsvpMeeting(m.id, opt) },
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (sel) RodapeTheme.colors.cream else RodapeTheme.colors.tertiary,
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
                TbSectionHeader(
                    // I1: "ata" é jargão pra muita gente — a palavra simples vem primeiro.
                    title = "Resumo do encontro (ata)",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Local-first: minutes vem null antes do 1º sync. Skeleton na janela
                // de graça evita "ninguém escreveu a ata" piscando antes do dado chegar.
                val minutesLoading = rememberShowLoading(hasData = minutes != null)
                if (minutesLoading) {
                    RodapeCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonText(width = 240.dp)
                            SkeletonText(width = 200.dp, height = 12.dp)
                            SkeletonText(width = 140.dp, height = 12.dp)
                        }
                    }
                } else if (minutes == null) {
                    RodapeCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Ninguém escreveu a ata deste encontro ainda.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
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
                    RodapeCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = minutes!!.texto,
                            style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.inkSoft, lineHeight = 20.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Editado por ${editor?.nome ?: "—"} · ${timeAgo(minutes!!.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
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
                TbSectionHeader(
                    title = "Suas anotações (privadas)",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Local-first: myNote vem null antes do 1º sync — skeleton na janela de graça.
                val noteLoading = rememberShowLoading(hasData = myNote != null)
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
                } else if (noteLoading) {
                    RodapeCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonText(width = 220.dp)
                            SkeletonText(width = 160.dp, height = 12.dp)
                        }
                    }
                } else {
                    RodapeCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = myNote?.texto?.takeIf { it.isNotBlank() }
                                ?: "Você ainda não anotou nada para este encontro.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (myNote?.texto.isNullOrBlank()) RodapeTheme.colors.muted else RodapeTheme.colors.inkSoft,
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

            // Botão admin: Concluir encontro (com confirmação — a ação tem
            // efeito colateral grande: pode mandar o livro pra Estante)
            if (isAdmin && !concluded) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    var showConcludeConfirm by remember { mutableStateOf(false) }
                    TbButton(
                        text = "Marcar encontro como concluído",
                        onClick = { showConcludeConfirm = true },
                        variant = TbButtonVariant.Terra,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Se este for o último do livro atual, o livro vai pra Estante automaticamente.",
                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (showConcludeConfirm) {
                        AlertDialog(
                            onDismissRequest = { showConcludeConfirm = false },
                            title = { Text("Concluir encontro?") },
                            text = { Text("Se este for o último encontro do livro atual, o livro vai pra Estante e a leitura é encerrada pra todo mundo.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showConcludeConfirm = false
                                    viewModel.concludeMeeting(m.id)
                                }) { Text("Concluir", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConcludeConfirm = false }) {
                                    Text("Voltar", color = RodapeTheme.colors.muted)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // Dialog editar ata
    if (showMinutesEdit) {
        AlertDialog(
            // Tocar fora com texto no rascunho pede confirmação antes de descartar.
            onDismissRequest = {
                if (minutesDraft.isNotBlank()) showMinutesDiscardConfirm = true
                else showMinutesEdit = false
            },
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
                }) { Text("Salvar", color = RodapeTheme.colors.oliva, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showMinutesEdit = false }) {
                    Text("Cancelar", color = RodapeTheme.colors.muted)
                }
            }
        )
    }

    // Confirmação de descarte da ata: evita perder o rascunho ao tocar fora do dialog.
    if (showMinutesDiscardConfirm) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showMinutesDiscardConfirm = false },
            title = { Text("Descartar a ata?") },
            text = { Text("O texto que você digitou vai ser perdido.") },
            confirmButton = {
                TextButton(onClick = {
                    showMinutesDiscardConfirm = false
                    showMinutesEdit = false
                }) { Text("Descartar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showMinutesDiscardConfirm = false }) {
                    Text("Continuar editando", color = RodapeTheme.colors.muted)
                }
            }
        )
    }
}
