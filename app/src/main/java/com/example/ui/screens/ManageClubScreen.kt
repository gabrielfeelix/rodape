package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.components.Cover
import com.example.ui.components.Overline
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.RodapeCard
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
    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserPapel by viewModel.currentUserPapel.collectAsState()
    val suggestedBooks by viewModel.suggestedBooks.collectAsState()
    val nextBooks by viewModel.nextBooks.collectAsState()
    val bookSearchResults by viewModel.searchResultsUnified.collectAsState()
    val bookSearchLoading by viewModel.searchLoading.collectAsState()

    var showEditInfo by remember { mutableStateOf(false) }
    var showRegenCode by remember { mutableStateOf(false) }
    var memberSheetFor by remember { mutableStateOf<ClubMember?>(null) }
    var removeMemberFor by remember { mutableStateOf<ClubMember?>(null) }
    var showEditPattern by remember { mutableStateOf(false) }
    var editingMeetingId by remember { mutableStateOf<String?>(null) }
    var creatingNewMeeting by remember { mutableStateOf(false) }
    var cancelMeetingId by remember { mutableStateOf<String?>(null) }
    var concludeMeetingId by remember { mutableStateOf<String?>(null) }
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
                        Icon(RodapeIcons.Back, contentDescription = "Voltar")
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
            // 0. HERO dashboard (3.13): o retrato do clube ANTES dos controles —
            // nome grande em Literata, pulso (membros · leitura · próximo
            // encontro) de relance, capa atual à direita.
            item {
                val heroShape = RoundedCornerShape(RodapeRadii.md)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(heroShape)
                        .background(RodapeTheme.colors.olivaDeep)
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = club?.nome ?: "—",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = RodapeTheme.colors.cream
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = buildString {
                                append("${members.size} ${if (members.size == 1) "membro" else "membros"}")
                                currentBook?.let { append(" · lendo ${it.title}") }
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = RodapeTheme.colors.cream.copy(alpha = 0.8f)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        meeting?.let { m ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = RodapeIcons.Calendar,
                                    contentDescription = null,
                                    tint = RodapeTheme.colors.cream.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = "Próximo: ${m.data}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = RodapeTheme.colors.cream.copy(alpha = 0.8f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    currentBook?.let { b ->
                        Cover(
                            title = b.title,
                            author = b.author,
                            coverUrl = b.coverUrl,
                            width = 56.dp,
                            height = 84.dp,
                        )
                    }
                }
            }

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
                        style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
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
                            style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
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
                            color = RodapeTheme.colors.terracota,
                            letterSpacing = 4.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    TbButton(
                        text = "Compartilhar convite",
                        leadingIcon = RodapeIcons.Share,
                        onClick = {
                            val code = club?.codigo ?: return@TbButton
                            val name = club?.nome ?: "Clube"
                            com.example.util.shareClubInvite(ctx, name, code)
                        },
                        variant = TbButtonVariant.Terra,
                        size = TbButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                                "super_admin" -> RodapeTheme.colors.terracota
                                "admin" -> RodapeTheme.colors.oliva
                                else -> RodapeTheme.colors.muted
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
                                        Icon(
                                            RodapeIcons.MoreV,
                                            contentDescription = "Ações para ${user?.nome ?: "membro"}"
                                        )
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
                                .clip(RoundedCornerShape(RodapeRadii.sm))
                                .background(RodapeTheme.colors.olivaSoft.copy(alpha = 0.4f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(RodapeTheme.colors.oliva),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = RodapeTheme.colors.cream,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Overline(
                                    text = recurrenceShortLabel(p.tipoRecorrencia).uppercase(),
                                    color = RodapeTheme.colors.olivaDark
                                )
                                Text(
                                    text = recurrenceFullLabel(p.tipoRecorrencia, p.diaSemana, p.valorRecorrencia, p.hora),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.ink
                                    )
                                )
                                Text(
                                    text = "${p.local}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RodapeRadii.sm))
                                .background(RodapeTheme.colors.dividerSoft.copy(alpha = 0.3f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = RodapeTheme.colors.muted,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Sem padrão recorrente definido.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
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

                    // ── Encontros do livro atual ─────────────────────────
                    val meetingsBook by viewModel.meetingsForCurrentBook.collectAsState()
                    val scheduledMeetings by viewModel.scheduledMeetingsInActiveClub.collectAsState()

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = RodapeIcons.Calendar,
                            contentDescription = null,
                            tint = RodapeTheme.colors.terracota,
                            modifier = Modifier.size(16.dp)
                        )
                        Overline(
                            text = if (meetingsBook.isNotEmpty()) "ENCONTROS DESTE LIVRO" else "PRÓXIMOS ENCONTROS",
                            color = RodapeTheme.colors.terracota
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Mostra encontros do livro atual (se houver) OU encontros avulsos agendados (fallback)
                    val displayList = if (meetingsBook.isNotEmpty()) meetingsBook
                        else scheduledMeetings.filter { it.bookId == null }
                    if (displayList.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RodapeRadii.sm))
                                .background(RodapeTheme.colors.dividerSoft.copy(alpha = 0.3f))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = RodapeIcons.Calendar,
                                contentDescription = null,
                                tint = RodapeTheme.colors.muted.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Nenhum encontro agendado",
                                style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayList.forEach { m ->
                                MeetingRow(
                                    meeting = m,
                                    onEdit = { editingMeetingId = m.id },
                                    onCancel = { cancelMeetingId = m.id },
                                    onConclude = { concludeMeetingId = m.id }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TbButton(
                        text = "+ Novo encontro",
                        onClick = { creatingNewMeeting = true },
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                .clip(RoundedCornerShape(RodapeRadii.sm))
                                .background(RodapeTheme.colors.olivaSoft.copy(alpha = 0.4f))
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
                                Overline(
                                    text = "LENDO AGORA",
                                    color = RodapeTheme.colors.olivaDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = b.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.ink
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = b.author,
                                    style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = RodapeIcons.Log,
                                        contentDescription = null,
                                        tint = RodapeTheme.colors.olivaMid,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${chapters.size} ${if (chapters.size == 1) "capítulo" else "capítulos"}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.olivaDark)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RodapeRadii.sm))
                                .background(RodapeTheme.colors.dividerSoft.copy(alpha = 0.3f))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = RodapeIcons.Log,
                                contentDescription = null,
                                tint = RodapeTheme.colors.muted.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Nenhum livro em leitura no momento",
                                style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
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
                        colors = CardDefaults.cardColors(containerColor = RodapeTheme.colors.terracota.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RodapeTheme.colors.terracota.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(RodapeRadii.md)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Zona de risco",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RodapeTheme.colors.terracota
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Arquivar este clube remove ele da lista ativa. Histórico é preservado.",
                                style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
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
                viewModel.regenerateInviteCode(
                    onResult = { newCode ->
                        if (newCode.isNotBlank()) {
                            scope.launch { snackbar.showSnackbar("Novo código: $newCode") }
                        }
                    },
                    onError = { msg ->
                        scope.launch { snackbar.showSnackbar(msg) }
                    }
                )
                showRegenCode = false
            }
        )
    }

    memberSheetFor?.let { rawMember ->
        val user = members.find { it.id == rawMember.userId }
        // Só mostra "Remover" quando o servidor de fato aceitaria a remoção:
        // - ninguém remove um super_admin nem a si mesmo;
        // - super_admin remove admins e membros;
        // - admin (não super) só remove membros comuns.
        val isSelf = rawMember.userId == currentUserId
        val canRemove = !isSelf && rawMember.papel != "super_admin" && (
            isSuper || (currentUserPapel == "admin" && rawMember.papel == "member")
        )
        MemberActionSheet(
            memberName = user?.nome ?: "Membro",
            memberPapel = rawMember.papel,
            currentUserIsSuper = isSuper,
            canRemove = canRemove,
            onDismiss = { memberSheetFor = null },
            onPromote = {
                viewModel.promoteMemberToAdmin(rawMember.userId) { msg ->
                    scope.launch { snackbar.showSnackbar(msg) }
                }
                memberSheetFor = null
            },
            onDemote = {
                viewModel.demoteAdminToMember(rawMember.userId) { msg ->
                    scope.launch { snackbar.showSnackbar(msg) }
                }
                memberSheetFor = null
            },
            onTransferSuper = {
                viewModel.transferSuperAdmin(rawMember.userId) { msg ->
                    scope.launch { snackbar.showSnackbar(msg) }
                }
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
                viewModel.removeMember(
                    rawMember.userId,
                    motivo,
                    onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
                )
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

    if (editingMeetingId != null) {
        val meetingsBook by viewModel.meetingsForCurrentBook.collectAsState()
        val scheduled by viewModel.scheduledMeetingsInActiveClub.collectAsState()
        val editing = meetingsBook.find { it.id == editingMeetingId } ?: scheduled.find { it.id == editingMeetingId }
        if (editing != null) {
            EditSingleMeetingDialog(
                initialData = editing.data,
                initialHora = editing.hora,
                initialLocal = editing.local,
                initialAgenda = editing.agenda,
                initialBookId = editing.bookId,
                initialChapterStart = editing.chapterStart,
                initialChapterEnd = editing.chapterEnd,
                currentBookId = currentBook?.id,
                currentBookTitle = currentBook?.title,
                totalChapters = chapters.size,
                onDismiss = { editingMeetingId = null },
                onSave = { d, h, l, a, bId, cs, ce ->
                    viewModel.upsertMeeting(editing.id, d, h, l, a, bId, cs, ce)
                    editingMeetingId = null
                }
            )
        }
    }

    if (creatingNewMeeting) {
        // Sugestão automática de caps pro próximo encontro
        var suggested by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        LaunchedEffect(Unit) {
            suggested = viewModel.suggestNextChapterRange()
        }
        EditSingleMeetingDialog(
            initialData = "",
            initialHora = pattern?.hora ?: "",
            initialLocal = pattern?.local ?: "",
            initialAgenda = pattern?.agendaTemplate ?: "",
            initialBookId = currentBook?.id,
            initialChapterStart = suggested?.first ?: 1,
            initialChapterEnd = suggested?.second ?: chapters.size.coerceAtLeast(1),
            currentBookId = currentBook?.id,
            currentBookTitle = currentBook?.title,
            totalChapters = chapters.size,
            onDismiss = { creatingNewMeeting = false },
            onSave = { d, h, l, a, bId, cs, ce ->
                viewModel.upsertMeeting(null, d, h, l, a, bId, cs, ce)
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

    // Concluir tem efeito colateral grande (pode mandar o livro pra Estante) —
    // merece a mesma confirmação que cancelar.
    if (concludeMeetingId != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { concludeMeetingId = null },
            title = { Text("Concluir encontro?") },
            text = { Text("Se este for o último encontro do livro atual, o livro vai pra Estante e a leitura é encerrada pra todo mundo.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.concludeMeeting(concludeMeetingId!!)
                    concludeMeetingId = null
                }) { Text("Concluir") }
            },
            dismissButton = {
                TextButton(onClick = { concludeMeetingId = null }) { Text("Voltar") }
            },
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
                }) { Text("Finalizar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showFinishBook = false }) { Text("Cancelar", color = RodapeTheme.colors.muted) }
            }
        )
    }

    if (showChangeBook) {
        ChangeCurrentBookDialog(
            suggested = suggestedBooks,
            next = nextBooks,
            searchResults = bookSearchResults,
            searchLoading = bookSearchLoading,
            onSearch = { viewModel.searchOpenLibrary(it) },
            onDismiss = {
                showChangeBook = false
                viewModel.searchOpenLibrary("")
            },
            onPick = { bookId ->
                viewModel.changeCurrentBookManually(bookId)
                showChangeBook = false
                viewModel.searchOpenLibrary("")
            },
            onPickSearchResult = { r ->
                viewModel.setSearchedBookAsCurrent(r)
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
                }) { Text("Arquivar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveClub = false }) { Text("Cancelar", color = RodapeTheme.colors.muted) }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    RodapeCard(modifier = Modifier.fillMaxWidth()) {
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
    canRemove: Boolean,
    onDismiss: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onTransferSuper: () -> Unit,
    onRemove: () -> Unit
) {
    // Confirmação pra transferir super admin — ação irreversível pelo próprio.
    var showTransferConfirm by remember { mutableStateOf(false) }
    if (showTransferConfirm) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showTransferConfirm = false },
            title = { Text("Transferir super admin?") },
            text = {
                Text("$memberName vira o super admin do clube e você deixa de ter esse poder. Não dá pra desfazer sozinho — só o novo super admin pode devolver.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showTransferConfirm = false
                    onTransferSuper()
                }) { Text("Transferir", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showTransferConfirm = false }) { Text("Voltar", color = RodapeTheme.colors.muted) }
            },
        )
    }

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
                TbButton(text = "Transferir super_admin pra este admin", onClick = { showTransferConfirm = true }, variant = TbButtonVariant.Outline, modifier = Modifier.fillMaxWidth())
            }
            if (canRemove) {
                TbButton(text = "Remover do clube", onClick = onRemove, variant = TbButtonVariant.Outline, modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChangeCurrentBookDialog(
    suggested: List<com.example.data.model.Book>,
    next: List<com.example.data.model.Book>,
    searchResults: List<com.example.data.search.UnifiedBookResult>,
    searchLoading: Boolean,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onPickSearchResult: (com.example.data.search.UnifiedBookResult) -> Unit,
) {
    val all = (next + suggested).distinctBy { it.id }
    var query by remember { mutableStateOf("") }
    // Debounce da busca (400ms), só a partir de 3 letras — mesma regra da tela Sugerir.
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length >= 3) { kotlinx.coroutines.delay(400); onSearch(q) } else onSearch("")
    }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("Trocar livro atual") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar um livro pra ler…") },
                    singleLine = true,
                    shape = RoundedCornerShape(RodapeRadii.sm),
                    modifier = Modifier.fillMaxWidth()
                )
                val q = query.trim()
                if (q.length >= 3) {
                    when {
                        searchLoading -> Text(
                            "Buscando…",
                            style = MaterialTheme.typography.bodySmall,
                            color = RodapeTheme.colors.muted
                        )
                        searchResults.isEmpty() -> Text(
                            "Nada encontrado pra “$q”.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RodapeTheme.colors.muted
                        )
                        else -> searchResults.take(8).forEach { r ->
                            TbButton(
                                text = "${r.title} — ${r.author}",
                                onClick = { onPickSearchResult(r) },
                                variant = TbButtonVariant.Outline,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else if (all.isEmpty()) {
                    Text(
                        "Digite acima pra buscar um livro e definir a leitura do clube.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RodapeTheme.colors.muted
                    )
                } else {
                    Text(
                        "Ou escolha um já sugerido:",
                        style = MaterialTheme.typography.labelSmall,
                        color = RodapeTheme.colors.muted
                    )
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
            TextButton(onClick = onDismiss) { Text("Fechar", color = RodapeTheme.colors.muted) }
        }
    )
}

@Composable
private fun MeetingRow(
    meeting: com.example.data.model.Meeting,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onConclude: () -> Unit
) {
    val concluded = meeting.status == "concluido"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RodapeRadii.sm))
            .background(if (concluded) RodapeTheme.colors.dividerSoft.copy(alpha = 0.3f) else RodapeTheme.colors.cream)
            .border(0.5.dp, RodapeTheme.colors.divider, RoundedCornerShape(RodapeRadii.sm))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(RodapeRadii.sm))
                    .background(
                        if (concluded) RodapeTheme.colors.muted.copy(alpha = 0.15f)
                        else RodapeTheme.colors.terracota.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val dayNumber = meeting.data
                    .substringAfter(",", "")
                    .trim()
                    .takeWhile { it.isDigit() }
                    .ifEmpty { meeting.data.trim().takeWhile { it.isDigit() }.ifEmpty { "—" } }
                Text(
                    text = dayNumber,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (concluded) RodapeTheme.colors.muted else RodapeTheme.colors.terracota,
                        fontFamily = LiterataFontFamily
                    )
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = meeting.data,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = if (concluded) RodapeTheme.colors.muted else RodapeTheme.colors.ink
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (concluded) {
                        com.example.ui.components.Pill(text = "Concluído", variant = com.example.ui.components.PillVariant.Olive)
                    }
                }
                Text(text = meeting.hora, style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted))
                if (meeting.chapterStart != null && meeting.chapterEnd != null) {
                    Text(
                        text = "Caps ${meeting.chapterStart}–${meeting.chapterEnd}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = RodapeTheme.colors.terracota,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Text(
                    text = "${meeting.local}",
                    style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!concluded) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TbButton(
                    text = "Editar",
                    onClick = onEdit,
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Sm,
                    modifier = Modifier.weight(1f)
                )
                TbButton(
                    text = "Cancelar",
                    onClick = onCancel,
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Sm,
                    modifier = Modifier.weight(1f)
                )
                TbButton(
                    text = "Concluir",
                    onClick = onConclude,
                    variant = TbButtonVariant.Terra,
                    size = TbButtonSize.Sm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
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
