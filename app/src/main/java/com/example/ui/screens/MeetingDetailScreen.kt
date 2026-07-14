package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.components.RodapeDialog
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
                        Icon(RodapeIcons.Back, contentDescription = "Voltar")
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
            // Header = CANHOTO EXPANDIDO do ticket (3.2): quem toca no ticket da
            // NextTab reencontra o mesmo objeto aqui — olivaDeep, day-stamp serif
            // e linha picotada embaixo. Concluído rebaixa pra neutro (memória).
            item {
                val headerBg = if (concluded) RodapeTheme.colors.dividerSoft.copy(alpha = 0.4f)
                    else RodapeTheme.colors.olivaDeep
                val headerFg = if (concluded) RodapeTheme.colors.muted else RodapeTheme.colors.cream
                val dateParts = m.data.split(",")
                val hWeekday = dateParts.firstOrNull()?.trim()?.uppercase()?.take(3) ?: ""
                val hRest = dateParts.getOrNull(1)?.trim() ?: m.data.trim()
                val hDay = hRest.takeWhile { it.isDigit() }.ifEmpty { "–" }
                val hMonth = hRest.dropWhile { it.isDigit() }.trim().lowercase()
                    .removePrefix("de ").trim().take(3)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (concluded) Modifier else Modifier.ticketShadow(cornerRadius = RodapeRadii.md))
                        .clip(RoundedCornerShape(RodapeRadii.md))
                        .background(headerBg)
                        .drawBehind {
                            drawLine(
                                color = headerFg.copy(alpha = 0.35f),
                                start = Offset(0f, size.height - 0.5.dp.toPx()),
                                end = Offset(size.width, size.height - 0.5.dp.toPx()),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                                ),
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Day-stamp tipografado — mesmo motivo do ticket.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // Destino do shared-element vindo do ticket da NextTab (mesma key).
                        modifier = Modifier
                            .rodapeSharedElement("meeting-stamp-$meetingId")
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${m.data}, ${m.hora}"
                            },
                    ) {
                        Text(
                            text = hWeekday,
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = headerFg.copy(alpha = 0.65f),
                            maxLines = 1,
                        )
                        Text(
                            text = hDay,
                            fontFamily = LiterataFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            fontSize = 40.sp,
                            lineHeight = 40.sp,
                            letterSpacing = (-1).sp,
                            color = headerFg,
                        )
                        Text(
                            text = hMonth,
                            fontFamily = LiterataFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            color = headerFg.copy(alpha = 0.85f),
                            maxLines = 1,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (concluded) {
                            Pill(text = "Concluído", variant = PillVariant.Olive)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = m.data,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = headerFg
                            ),
                        )
                        Text(m.hora, style = MaterialTheme.typography.bodyMedium.copy(color = headerFg.copy(alpha = 0.85f)))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(RodapeIcons.Pin, contentDescription = null, tint = headerFg.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                            Text(m.local, style = MaterialTheme.typography.bodySmall.copy(color = headerFg.copy(alpha = 0.85f)))
                        }
                        if (m.chapterStart != null && m.chapterEnd != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Caps ${m.chapterStart}–${m.chapterEnd}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (concluded) RodapeTheme.colors.terracota else RodapeTheme.colors.olivaSoft,
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
                                // 3.4: mesma identidade semântica do picker da NextTab
                                // (Vou=oliva, Talvez=dourado legível, Não vou=neutro),
                                // raio unificado em pílula (full) e liveRegion que
                                // faltava aqui (a NextTab já anunciava).
                                val (selBg, selFg) = when (opt) {
                                    "Vou" -> RodapeTheme.colors.olivaSoft to RodapeTheme.colors.olivaDark
                                    "Talvez" -> RodapeTheme.colors.warningSoft to RodapeTheme.colors.warning
                                    else -> RodapeTheme.colors.dividerSoft to RodapeTheme.colors.tertiary
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(RodapeRadii.full))
                                        .background(if (sel) selBg else androidx.compose.ui.graphics.Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (sel) selFg.copy(alpha = 0.35f) else RodapeTheme.colors.divider,
                                            RoundedCornerShape(RodapeRadii.full)
                                        )
                                        .selectable(
                                            selected = sel,
                                            role = Role.RadioButton,
                                            onClick = { viewModel.rsvpMeeting(m.id, opt) },
                                        )
                                        .semantics { if (sel) liveRegion = LiveRegionMode.Polite },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (sel) selFg else RodapeTheme.colors.tertiary,
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
                        RodapeDialog(
                            onDismissRequest = { showConcludeConfirm = false },
                            title = "Concluir encontro?",
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
        RodapeDialog(
            // Tocar fora com texto no rascunho pede confirmação antes de descartar.
            onDismissRequest = {
                if (minutesDraft.isNotBlank()) showMinutesDiscardConfirm = true
                else showMinutesEdit = false
            },
            title = "Ata do encontro",
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
        RodapeDialog(
            onDismissRequest = { showMinutesDiscardConfirm = false },
            title = "Descartar a ata?",
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
