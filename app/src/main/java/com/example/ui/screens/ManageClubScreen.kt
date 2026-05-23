package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClubMember
import com.example.ui.admin.*
import com.example.ui.components.Avatar
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.TramabookCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageClubScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChapters: () -> Unit,
    onNavigateToModerationLog: () -> Unit
) {
    val club by viewModel.activeClub.collectAsState()
    val currentBook by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val members by viewModel.clubMembers.collectAsState()
    val membersRaw by viewModel.activeClubMembersRaw.collectAsState()
    val pattern by viewModel.activeMeetingPattern.collectAsState()
    val meeting by viewModel.latestMeeting.collectAsState()
    val removed by viewModel.removedCommentsInActiveClub.collectAsState()
    val isSuper by viewModel.isCurrentUserSuperAdmin.collectAsState()
    val suggestedBooks by viewModel.suggestedBooks.collectAsState()
    val nextBooks by viewModel.nextBooks.collectAsState()

    var showEditInfo by remember { mutableStateOf(false) }
    var showRegenCode by remember { mutableStateOf(false) }
    var memberSheetFor by remember { mutableStateOf<ClubMember?>(null) }
    var removeMemberFor by remember { mutableStateOf<ClubMember?>(null) }
    var showEditPattern by remember { mutableStateOf(false) }
    var editingMeetingId by remember { mutableStateOf<String?>(null) }
    var creatingNewMeeting by remember { mutableStateOf(false) }
    var cancelMeetingId by remember { mutableStateOf<String?>(null) }
    var showChangeBook by remember { mutableStateOf(false) }
    var showFinishBook by remember { mutableStateOf(false) }
    var showArchiveClub by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar clube") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Info
            item {
                SectionCard(title = "Informações do clube") {
                    Text(
                        text = club?.nome ?: "—",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = club?.descricao ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Muted)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val corIdx = club?.cor?.toIntOrNull() ?: 0
                    val corClub = ClubColors.getOrNull(corIdx) ?: ClubColors.first()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(corClub.bg))
                        Text(
                            text = if (club?.privacidade == "convidados") "Só convidados" else "Aberto a quem tem link",
                            style = MaterialTheme.typography.bodySmall.copy(color = Muted)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TbButton(
                        text = "Editar",
                        onClick = { showEditInfo = true },
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Código
            item {
                SectionCard(title = "Código de convite") {
                    Text(
                        text = club?.codigo ?: "—",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Terracota,
                            letterSpacing = 4.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TbButton(
                            text = "Copiar",
                            onClick = {
                                clipboard.setText(AnnotatedString(club?.codigo ?: ""))
                                scope.launch { snackbar.showSnackbar("Código copiado") }
                            },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.weight(1f)
                        )
                        TbButton(
                            text = "Gerar novo",
                            onClick = { showRegenCode = true },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Membros
            item {
                SectionCard(title = "Membros (${members.size})") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        membersRaw.forEach { rawMember ->
                            val user = members.find { it.id == rawMember.userId }
                            val papelLabel = when (rawMember.papel) {
                                "super_admin" -> "Super admin"
                                "admin" -> "Admin"
                                else -> "Membro"
                            }
                            val papelColor = when (rawMember.papel) {
                                "super_admin" -> Terracota
                                "admin" -> Oliva
                                else -> Muted
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Avatar(name = user?.nome ?: "—", avatarUrl = user?.avatarUrl ?: "", size = 32.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user?.nome ?: "—", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        papelLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(color = papelColor)
                                    )
                                }
                                if (rawMember.papel != "super_admin") {
                                    IconButton(onClick = { memberSheetFor = rawMember }) {
                                        Icon(Icons.Outlined.MoreVert, contentDescription = "Ações")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Encontros
            item {
                SectionCard(title = "Encontros") {
                    // ── Padrão recorrente ────────────────────────────────
                    if (pattern != null) {
                        val p = pattern!!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(OlivaSoft.copy(alpha = 0.4f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Oliva),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = Cream,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recurrenceShortLabel(p.tipoRecorrencia).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OlivaDark,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = recurrenceFullLabel(p.tipoRecorrencia, p.diaSemana, p.valorRecorrencia, p.hora),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink
                                    )
                                )
                                Text(
                                    text = "📍 ${p.local}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Muted)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DividerSoft.copy(alpha = 0.3f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = Muted,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Sem padrão recorrente definido.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Muted)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TbButton(
                        text = if (pattern != null) "Editar padrão de recorrência" else "Definir padrão",
                        onClick = { showEditPattern = true },
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Próximo encontro ─────────────────────────────────
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            tint = Terracota,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PRÓXIMO ENCONTRO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Terracota,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (meeting != null) {
                        val m = meeting!!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Cream)
                                .border(0.5.dp, Divider, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Terracota.copy(alpha = 0.12f)),
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
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Terracota,
                                        fontFamily = LiterataFontFamily
                                    )
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = m.data,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(text = m.hora, style = MaterialTheme.typography.bodySmall.copy(color = Muted))
                                Text(
                                    text = "📍 ${m.local}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Muted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TbButton(
                                text = "Editar",
                                onClick = { editingMeetingId = m.id },
                                variant = TbButtonVariant.Outline,
                                size = TbButtonSize.Sm,
                                modifier = Modifier.weight(1f)
                            )
                            TbButton(
                                text = "Cancelar",
                                onClick = { cancelMeetingId = m.id },
                                variant = TbButtonVariant.Outline,
                                size = TbButtonSize.Sm,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DividerSoft.copy(alpha = 0.3f))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = null,
                                tint = Muted.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Nenhum encontro agendado",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Muted)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = Terracota,
                            modifier = Modifier.size(18.dp).align(Alignment.CenterVertically)
                        )
                        TbButton(
                            text = "Encontro avulso",
                            onClick = { creatingNewMeeting = true },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Livro atual
            item {
                SectionCard(title = "Livro atual") {
                    if (currentBook != null) {
                        val b = currentBook!!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(OlivaSoft.copy(alpha = 0.4f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            com.example.ui.components.Cover(
                                title = b.title,
                                author = b.author,
                                coverUrl = b.coverUrl,
                                width = 56.dp,
                                height = 84.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LENDO AGORA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OlivaDark,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = b.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = b.author,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Muted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.List,
                                        contentDescription = null,
                                        tint = OlivaMid,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${chapters.size} ${if (chapters.size == 1) "capítulo" else "capítulos"}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = OlivaDark)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DividerSoft.copy(alpha = 0.3f))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.List,
                                contentDescription = null,
                                tint = Muted.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Nenhum livro em leitura no momento",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Muted)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TbButton(
                            text = "Trocar livro",
                            onClick = { showChangeBook = true },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.weight(1f)
                        )
                        TbButton(
                            text = "Capítulos",
                            onClick = onNavigateToChapters,
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.weight(1f),
                            enabled = currentBook != null
                        )
                    }
                    if (currentBook != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TbButton(
                            text = "Marcar como finalizado",
                            onClick = { showFinishBook = true },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 6. Moderação
            item {
                SectionCard(title = "Moderação") {
                    Text(
                        "${removed.size} ${if (removed.size == 1) "comentário removido" else "comentários removidos"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TbButton(
                        text = "Ver log",
                        onClick = onNavigateToModerationLog,
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 7. Zona de risco (super)
            if (isSuper) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Terracota.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Terracota.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Zona de risco",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Terracota
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Arquivar este clube remove ele da lista ativa. Histórico é preservado.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Muted)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TbButton(
                                text = "Arquivar clube",
                                onClick = { showArchiveClub = true },
                                variant = TbButtonVariant.Outline,
                                size = TbButtonSize.Sm,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogos
    if (showEditInfo && club != null) {
        EditClubInfoDialog(
            initialNome = club!!.nome,
            initialDescricao = club!!.descricao,
            initialCorIndex = club!!.cor,
            initialPrivacidade = club!!.privacidade,
            onDismiss = { showEditInfo = false },
            onSave = { n, d, c, p ->
                viewModel.editClubInfo(n, d, c, p)
                showEditInfo = false
            }
        )
    }

    if (showRegenCode) {
        RegenerateCodeDialog(
            onDismiss = { showRegenCode = false },
            onConfirm = {
                viewModel.regenerateInviteCode { newCode ->
                    scope.launch { snackbar.showSnackbar("Novo código: $newCode") }
                }
                showRegenCode = false
            }
        )
    }

    memberSheetFor?.let { rawMember ->
        val user = members.find { it.id == rawMember.userId }
        MemberActionSheet(
            memberName = user?.nome ?: "Membro",
            memberPapel = rawMember.papel,
            currentUserIsSuper = isSuper,
            onDismiss = { memberSheetFor = null },
            onPromote = {
                viewModel.promoteMemberToAdmin(rawMember.userId)
                memberSheetFor = null
            },
            onDemote = {
                viewModel.demoteAdminToMember(rawMember.userId)
                memberSheetFor = null
            },
            onTransferSuper = {
                viewModel.transferSuperAdmin(rawMember.userId)
                memberSheetFor = null
            },
            onRemove = {
                removeMemberFor = rawMember
                memberSheetFor = null
            }
        )
    }

    removeMemberFor?.let { rawMember ->
        val user = members.find { it.id == rawMember.userId }
        RemoveMemberDialog(
            memberName = user?.nome ?: "Membro",
            onDismiss = { removeMemberFor = null },
            onConfirm = { motivo ->
                viewModel.removeMember(rawMember.userId, motivo)
                removeMemberFor = null
            }
        )
    }

    if (showEditPattern) {
        EditMeetingPatternDialog(
            initialDiaSemana = pattern?.diaSemana ?: java.util.Calendar.SUNDAY,
            initialHora = pattern?.hora ?: "19:00",
            initialLocal = pattern?.local ?: "",
            initialAgenda = pattern?.agendaTemplate ?: "Discussão do livro atual",
            initialTipoRecorrencia = pattern?.tipoRecorrencia ?: "semanal",
            initialValorRecorrencia = pattern?.valorRecorrencia ?: 0,
            onDismiss = { showEditPattern = false },
            onSave = { dia, h, l, a, tipo, valor ->
                viewModel.upsertMeetingPattern(dia, h, l, a, tipo, valor)
                showEditPattern = false
            }
        )
    }

    if (editingMeetingId != null && meeting != null) {
        EditSingleMeetingDialog(
            initialData = meeting!!.data,
            initialHora = meeting!!.hora,
            initialLocal = meeting!!.local,
            initialAgenda = meeting!!.agenda,
            onDismiss = { editingMeetingId = null },
            onSave = { d, h, l, a ->
                viewModel.upsertMeeting(meeting!!.id, d, h, l, a)
                editingMeetingId = null
            }
        )
    }

    if (creatingNewMeeting) {
        EditSingleMeetingDialog(
            initialData = "",
            initialHora = "",
            initialLocal = pattern?.local ?: "",
            initialAgenda = pattern?.agendaTemplate ?: "",
            onDismiss = { creatingNewMeeting = false },
            onSave = { d, h, l, a ->
                viewModel.upsertMeeting(null, d, h, l, a)
                creatingNewMeeting = false
            }
        )
    }

    if (cancelMeetingId != null) {
        CancelMeetingDialog(
            onDismiss = { cancelMeetingId = null },
            onConfirm = {
                viewModel.cancelMeeting(cancelMeetingId!!)
                cancelMeetingId = null
            }
        )
    }

    if (showFinishBook) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showFinishBook = false },
            title = { Text("Marcar como finalizado?") },
            text = { Text("O livro atual vai pra estante com data do encontro = hoje.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markCurrentBookFinished()
                    showFinishBook = false
                }) { Text("Finalizar", color = Terracota, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showFinishBook = false }) { Text("Cancelar", color = Muted) }
            }
        )
    }

    if (showChangeBook) {
        ChangeCurrentBookDialog(
            suggested = suggestedBooks,
            next = nextBooks,
            onDismiss = { showChangeBook = false },
            onPick = { bookId ->
                viewModel.changeCurrentBookManually(bookId)
                showChangeBook = false
            }
        )
    }

    if (showArchiveClub) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showArchiveClub = false },
            title = { Text("Arquivar '${club?.nome}'?") },
            text = {
                Text("Você e os membros não verão mais este clube na lista. Você pode reativar depois em Arquivados. Histórico é preservado.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.archiveClub()
                    showArchiveClub = false
                    onNavigateBack()
                }) { Text("Arquivar", color = Terracota, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveClub = false }) { Text("Cancelar", color = Muted) }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    TramabookCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberActionSheet(
    memberName: String,
    memberPapel: String,
    currentUserIsSuper: Boolean,
    onDismiss: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onTransferSuper: () -> Unit,
    onRemove: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                memberName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (memberPapel == "member" && currentUserIsSuper) {
                TbButton(text = "Promover a admin", onClick = onPromote, variant = TbButtonVariant.Outline, modifier = Modifier.fillMaxWidth())
            }
            if (memberPapel == "admin" && currentUserIsSuper) {
                TbButton(text = "Rebaixar a membro", onClick = onDemote, variant = TbButtonVariant.Outline, modifier = Modifier.fillMaxWidth())
                TbButton(text = "Transferir super_admin pra este admin", onClick = onTransferSuper, variant = TbButtonVariant.Outline, modifier = Modifier.fillMaxWidth())
            }
            TbButton(text = "Remover do clube", onClick = onRemove, variant = TbButtonVariant.Outline, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChangeCurrentBookDialog(
    suggested: List<com.example.data.model.Book>,
    next: List<com.example.data.model.Book>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val all = (next + suggested).distinctBy { it.id }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("Trocar livro atual") },
        text = {
            if (all.isEmpty()) {
                Text("Nenhuma sugestão disponível. Sugira um livro primeiro na aba Votação.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    all.forEach { b ->
                        TbButton(
                            text = "${b.title} — ${b.author}",
                            onClick = { onPick(b.id) },
                            variant = TbButtonVariant.Outline,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar", color = Muted) }
        }
    )
}

private fun recurrenceShortLabel(tipo: String): String = when (tipo) {
    "semanal" -> "Toda semana"
    "quinzenal" -> "Quinzenal"
    "mensal_dia_semana" -> "Mensal"
    "mensal_dia_mes" -> "Mensal"
    "personalizado_dias" -> "Personalizado"
    else -> "Recorrente"
}

private fun recurrenceFullLabel(tipo: String, diaSemana: Int, valor: Int, hora: String): String {
    val dia = when (diaSemana) {
        java.util.Calendar.SUNDAY -> "domingos"
        java.util.Calendar.MONDAY -> "segundas"
        java.util.Calendar.TUESDAY -> "terças"
        java.util.Calendar.WEDNESDAY -> "quartas"
        java.util.Calendar.THURSDAY -> "quintas"
        java.util.Calendar.FRIDAY -> "sextas"
        java.util.Calendar.SATURDAY -> "sábados"
        else -> "—"
    }
    val ordinal = when (valor) {
        1 -> "1ª"
        2 -> "2ª"
        3 -> "3ª"
        4 -> "4ª"
        5 -> "última"
        else -> "1ª"
    }
    return when (tipo) {
        "semanal" -> "Toda ${dia.dropLast(1)}, ${hora}"
        "quinzenal" -> "A cada 15 dias (${dia}), ${hora}"
        "mensal_dia_semana" -> "Toda $ordinal ${dia.dropLast(1)} do mês, ${hora}"
        "mensal_dia_mes" -> "Todo dia $valor do mês, ${hora}"
        "personalizado_dias" -> "A cada $valor dias, ${hora}"
        else -> "$dia, ${hora}"
    }
}
