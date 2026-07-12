package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Comment
import com.example.ui.components.Avatar
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.RodapeCard
import com.example.ui.components.SkeletonRowList
import com.example.ui.components.rememberShowLoading
import com.example.ui.theme.CardSoft
import com.example.ui.theme.Cream
import com.example.ui.theme.Divider
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.Muted
import com.example.ui.theme.Oliva
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.Paper
import com.example.ui.theme.Terracota
import com.example.ui.theme.TerracotaDark
import com.example.ui.theme.TerracotaSoft
import com.example.ui.theme.Tertiary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionScreen(
    viewModel: MainViewModel,
    chapterId: String,
    chapterTitle: String,
    onNavigateBack: () -> Unit
) {
    // Flows memoizados por chapterId: sem isso, cada recomposição recriava o
    // Flow e disparava um refetch em loop (maior gargalo de performance da tela).
    val commentsFlow = remember(chapterId) { viewModel.getCommentsForChapter(chapterId) }
    val comments by commentsFlow.collectAsState(initial = emptyList())
    val chapters by viewModel.currentChapters.collectAsState()
    val progress by viewModel.userProgress.collectAsState()
    val members by viewModel.clubMembers.collectAsState()
    val reactionsFlow = remember(chapterId) { viewModel.getReactionsForChapter(chapterId) }
    val reactions by reactionsFlow.collectAsState(initial = emptyList())

    val chapterObj = chapters.find { it.id == chapterId }
    val chapterNum = chapterObj?.numero ?: 1
    val currentProgNum = progress?.currentChapter ?: 0

    val isAheadOfProgress = chapterNum > currentProgNum
    var forceRevealDebate by remember { mutableStateOf(false) }

    var commentText by rememberSaveable { mutableStateOf("") }
    var selectedCommentToReact by remember { mutableStateOf<Comment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CAPÍTULO $chapterNum",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Tertiary,
                                letterSpacing = 0.6.sp
                            )
                        )
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Terracota
                        )
                    }
                },
                // (botão "⋮" removido: era um placeholder sem ação — controle
                // morto confunde mais do que ajuda)
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
            )
        },
        containerColor = Paper
    ) { padding ->
        if (progress == null || chapters.isEmpty() || chapterObj == null) {
            // Progresso OU capítulos ainda carregando: mostra loader em vez de
            // assumir cap. 0 / cair no fallback chapterNum = 1. Numa corrida de
            // load, liberar a discussão com chapterNum = 1 vazaria spoiler de
            // capítulos à frente — então esperamos os dois chegarem.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                com.example.ui.components.CenteredLoading()
            }
        } else if (isAheadOfProgress && !forceRevealDebate) {
            // Visual Spoiler Barrier
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                RodapeCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Atenção: possível spoiler",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Terracota,
                                textAlign = TextAlign.Center
                            )
                        )

                        Text(
                            text = "Este capítulo está à frente do seu progresso atual de leitura (você parou no capítulo $currentProgNum).",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TbButton(
                            text = "Revelar debate mesmo assim",
                            onClick = { forceRevealDebate = true },
                            variant = TbButtonVariant.TerraSoft
                        )

                        TextButton(onClick = onNavigateBack) {
                            Text("Voltar para minha meta", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            // Discussion Box
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Spoiler clearance / warning banner
                val isLido = chapterNum <= currentProgNum
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isLido) {
                        // "Tu já passou daqui. Tá liberado." — olive card per design
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OlivaSoft, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Oliva, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Cream,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Você já passou daqui.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OlivaDark
                                    )
                                )
                                Text(
                                    text = "Está liberado.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = InterFontFamily,
                                        color = OlivaDark
                                    )
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TerracotaSoft, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Terracota,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "não lido – cuidado com o spoiler!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = TerracotaDark
                                )
                            )
                        }
                    }
                }

                // Coletores e derivações memoizados UMA vez, fora do items{}:
                // evita 1 coletor de state + 1 filter/find por comentário a cada frame.
                val currentUid by viewModel.currentUserId.collectAsState()
                val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()
                val reactionsByComment = remember(reactions) { reactions.groupBy { it.commentId } }
                val membersById = remember(members) { members.associateBy { it.id } }
                val listState = rememberLazyListState()

                // Skeleton no cold start (Room emite lista vazia antes do 1º sync):
                // evita piscar o empty state e depois "pular" pro conteúdo real.
                val showLoading = rememberShowLoading(hasData = comments.isNotEmpty())

                if (showLoading) {
                    SkeletonRowList(
                        count = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    )
                } else if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum comentário neste capítulo. Seja o primeiro a opinar!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    // Comentários vêm em ordem ASC: rola pro último quando um novo entra.
                    LaunchedEffect(comments.size) {
                        if (comments.isNotEmpty()) listState.animateScrollToItem(comments.size - 1)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            val userObj = membersById[comment.userId]
                            val userNameVal = if (comment.userId == currentUid) "Você" else userObj?.nome ?: "Iniciante"
                            val commentReactions = reactionsByComment[comment.id].orEmpty()
                            val isOwn = comment.userId == currentUid
                            var showModMenu by remember(comment.id) { mutableStateOf(false) }
                            var showRemoveDialog by remember(comment.id) { mutableStateOf(false) }
                            var modMotivo by remember(comment.id) { mutableStateOf("") }
                            var showOwnerMenu by remember(comment.id) { mutableStateOf(false) }
                            var showEditDialog by remember(comment.id) { mutableStateOf(false) }
                            var editText by remember(comment.id) { mutableStateOf(comment.texto) }
                            var showDeleteDialog by remember(comment.id) { mutableStateOf(false) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Avatar(
                                    name = userNameVal,
                                    avatarUrl = userObj?.avatarUrl ?: "",
                                    size = 36.dp
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    // Comment bubble
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = if (isOwn) TerracotaSoft else Cream,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .then(
                                                if (!isOwn) Modifier.border(
                                                    width = 0.5.dp,
                                                    color = Divider,
                                                    shape = RoundedCornerShape(14.dp)
                                                ) else Modifier
                                            )
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { selectedCommentToReact = comment }
                                            .semantics {
                                                role = Role.Button
                                                contentDescription = "Reagir a este comentário"
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (isOwn) "Tu" else userNameVal,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isOwn) TerracotaDark else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            if (comment.removido) {
                                                Text(
                                                    text = "[mensagem removida pela moderação]",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = LiterataFontFamily,
                                                        fontSize = 14.5.sp,
                                                        lineHeight = 21.sp,
                                                        color = Muted,
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                    )
                                                )
                                            } else {
                                                Text(
                                                    text = comment.texto,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = LiterataFontFamily,
                                                        fontSize = 14.5.sp,
                                                        lineHeight = 21.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Reaction chips
                                    if (commentReactions.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val emojiGroups = commentReactions.groupBy { it.emoji }
                                            emojiGroups.forEach { (emoji, list) ->
                                                val count = list.size
                                                val hasUserReacted = list.any { it.userId == currentUid }

                                                val chipBg = if (hasUserReacted) TerracotaSoft else CardSoft
                                                val chipBorder = if (hasUserReacted) {
                                                    BorderStroke(1.dp, Terracota)
                                                } else {
                                                    BorderStroke(1.dp, Divider)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .background(chipBg, RoundedCornerShape(999.dp))
                                                        .border(chipBorder, RoundedCornerShape(999.dp))
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .clickable { viewModel.toggleReaction(comment.id, emoji) }
                                                        .semantics {
                                                            role = Role.Button
                                                            contentDescription = "Reação $emoji, $count"
                                                            stateDescription =
                                                                if (hasUserReacted) "Você reagiu" else "Sem sua reação"
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = emoji,
                                                            fontSize = 12.sp
                                                        )
                                                        Text(
                                                            text = count.toString(),
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontFamily = InterFontFamily,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (hasUserReacted) TerracotaDark else Tertiary
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isAdmin && !isOwn && !comment.removido) {
                                    Box {
                                        IconButton(
                                            onClick = { showModMenu = true },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.MoreVert,
                                                contentDescription = "Moderação",
                                                tint = Muted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showModMenu,
                                            onDismissRequest = { showModMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Remover (moderação)") },
                                                onClick = {
                                                    showModMenu = false
                                                    modMotivo = ""
                                                    showRemoveDialog = true
                                                }
                                            )
                                        }
                                    }
                                }

                                if (isOwn && !comment.removido) {
                                    Box {
                                        IconButton(
                                            onClick = { showOwnerMenu = true },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.MoreVert,
                                                contentDescription = "Opções do comentário",
                                                tint = Muted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showOwnerMenu,
                                            onDismissRequest = { showOwnerMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Editar") },
                                                onClick = {
                                                    showOwnerMenu = false
                                                    editText = comment.texto
                                                    showEditDialog = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Apagar") },
                                                onClick = {
                                                    showOwnerMenu = false
                                                    showDeleteDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (showEditDialog) {
                                AlertDialog(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    onDismissRequest = { showEditDialog = false },
                                    title = { Text("Editar comentário") },
                                    text = {
                                        OutlinedTextField(
                                            value = editText,
                                            onValueChange = { editText = it.take(4000) },
                                            minLines = 2,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                viewModel.editComment(comment.id, editText.trim())
                                                showEditDialog = false
                                            },
                                            enabled = editText.trim().isNotEmpty()
                                        ) { Text("Salvar", color = Terracota, fontWeight = FontWeight.SemiBold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showEditDialog = false }) {
                                            Text("Cancelar", color = Muted)
                                        }
                                    }
                                )
                            }

                            if (showDeleteDialog) {
                                AlertDialog(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("Apagar comentário?") },
                                    text = { Text("Seu comentário será removido de vez. Não dá pra desfazer.") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.deleteOwnComment(comment.id)
                                            showDeleteDialog = false
                                        }) { Text("Apagar", color = Terracota, fontWeight = FontWeight.SemiBold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteDialog = false }) {
                                            Text("Cancelar", color = Muted)
                                        }
                                    }
                                )
                            }

                            if (showRemoveDialog) {
                                AlertDialog(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    onDismissRequest = { showRemoveDialog = false },
                                    title = { Text("Remover comentário?") },
                                    text = {
                                        Column {
                                            Text("Vira placeholder pra todos. Registra no log.")
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = modMotivo,
                                                onValueChange = { modMotivo = it.take(200) },
                                                label = { Text("Motivo (opcional)") },
                                                minLines = 2,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.removeComment(comment.id, modMotivo)
                                            showRemoveDialog = false
                                        }) { Text("Remover", color = Terracota, fontWeight = FontWeight.SemiBold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRemoveDialog = false }) {
                                            Text("Cancelar", color = Muted)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Comment input footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Cream,
                    border = BorderStroke(0.5.dp, Divider)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Rounded text field with Cream background and Divider border
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Paper, RoundedCornerShape(999.dp))
                                .border(1.dp, Divider, RoundedCornerShape(999.dp))
                                .clip(RoundedCornerShape(999.dp))
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it.take(4000) },
                                placeholder = {
                                    Text(
                                        text = "Comenta esse capítulo...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = InterFontFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(999.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Oliva,
                                    unfocusedBorderColor = Divider,
                                    focusedContainerColor = Paper,
                                    unfocusedContainerColor = Paper
                                )
                            )
                        }

                        // Circular send button — Terracota when active
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (commentText.trim().isNotEmpty()) Terracota else CardSoft,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .clickable(enabled = commentText.trim().isNotEmpty()) {
                                    val toSend = commentText.trim()
                                    if (toSend.isNotEmpty()) {
                                        viewModel.sendComment(chapterId, toSend)
                                        commentText = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Send,
                                contentDescription = "Enviar",
                                tint = if (commentText.trim().isNotEmpty()) Cream else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Reaction selector sheet block
    if (selectedCommentToReact != null) {
        val comment = selectedCommentToReact!!
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { selectedCommentToReact = null },
            title = { Text("Adicionar reação", style = MaterialTheme.typography.headlineLarge) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val emojis = listOf("❤", "🤯", "💀", "🍷", "👍")
                    emojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 32.sp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.toggleReaction(comment.id, emoji)
                                    selectedCommentToReact = null
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Reagir com $emoji"
                                }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedCommentToReact = null }) {
                    Text("Fechar")
                }
            }
        )
    }
}
