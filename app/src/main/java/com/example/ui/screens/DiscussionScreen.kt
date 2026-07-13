package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddReaction
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
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeIcons
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.Terracota
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

    // Alinha com a aba Livro (effectiveChap = maxOf(prog, 1)): um leitor sem
    // progresso está "no capítulo 1", então o cap 1 abre DIRETO. A barreira de
    // spoiler vale só pra capítulos realmente à frente da posição atual — antes
    // usava `> currentProgNum` cru e o cap 1 caía na barreira pra quem tinha
    // progresso 0 (todo leitor novo), contradizendo a aba Livro que o libera.
    val effectiveProg = maxOf(currentProgNum, 1)
    val isAheadOfProgress = chapterNum > effectiveProg
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
                                color = RodapeTheme.colors.tertiary,
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
                            imageVector = RodapeIcons.Back,
                            contentDescription = "Voltar",
                            tint = RodapeTheme.colors.terracota
                        )
                    }
                },
                // (botão "⋮" removido: era um placeholder sem ação — controle
                // morto confunde mais do que ajuda)
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RodapeTheme.colors.paper)
            )
        },
        containerColor = RodapeTheme.colors.paper
    ) { padding ->
        // Loading = o CAPÍTULO ainda não resolveu (grace de 2.5s no cold start,
        // como o resto do app). `progress == null` NÃO é loading: é "ainda não
        // marquei leitura" (= cap 0) — estado PERMANENTE em todo livro recém-criado.
        // Antes o gate incluía `progress == null` e travava no spinner PARA SEMPRE
        // nesses livros (bug do "carregando infinito"). O fallback seguro já existe:
        // currentProgNum = progress?.currentChapter ?: 0, e cap à frente cai na
        // barreira de spoiler (esconde conteúdo), então tratar null como 0 é seguro.
        val chapterResolving = rememberShowLoading(hasData = chapterObj != null)
        if (chapterResolving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                com.example.ui.components.CenteredLoading()
            }
        } else if (chapterObj == null) {
            // Capítulos resolveram mas este id não existe (ex.: capítulo de um livro
            // que não é o atual do clube). Estado honesto em vez de spinner eterno.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Capítulo não encontrado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = RodapeTheme.colors.tertiary
                )
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
                            // I1: dá a dica do que "spoiler" quer dizer, pra quem não conhece.
                            text = "Atenção: pode contar o que vem (spoiler)",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = RodapeTheme.colors.terracota,
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

                        // Ênfase invertida (P9): a ação segura (voltar) é a primária;
                        // revelar o spoiler fica como opção secundária, discreta.
                        TbButton(
                            text = "Voltar para minha meta",
                            onClick = onNavigateBack,
                            variant = TbButtonVariant.Primary
                        )

                        TextButton(onClick = { forceRevealDebate = true }) {
                            Text(
                                "Revelar debate mesmo assim",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        // "Você já passou daqui. Está liberado." — olive card per design
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RodapeTheme.colors.olivaSoft, RoundedCornerShape(RodapeRadii.sm))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(RodapeTheme.colors.oliva, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = RodapeIcons.CheckCircle,
                                    contentDescription = null,
                                    tint = RodapeTheme.colors.cream,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Você já passou daqui.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.olivaDark
                                    )
                                )
                                Text(
                                    text = "Está liberado.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = InterFontFamily,
                                        color = RodapeTheme.colors.olivaDark
                                    )
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RodapeTheme.colors.terracotaSoft, RoundedCornerShape(RodapeRadii.sm))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = RodapeIcons.Warning,
                                    contentDescription = null,
                                    tint = RodapeTheme.colors.terracota,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "não lido – cuidado com o spoiler!",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = RodapeTheme.colors.terracotaDark
                                    )
                                )
                            }
                            // Marcar leitura DIRETO daqui: pula o progresso pro cap N (sem
                            // tocar "Marcar progresso" +1 N vezes na aba Livro). Ao marcar,
                            // este banner vira "Você já passou daqui" na hora.
                            Text(
                                text = "Marcar que li este capítulo",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RodapeTheme.colors.terracotaDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        chapterObj?.let { ch ->
                                            viewModel.updateBookProgress(ch.bookId, chapterNum)
                                        }
                                    }
                                    .padding(top = 2.dp)
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
                            val userNameVal = if (comment.userId == currentUid) "Você" else userObj?.nome ?: "Membro"
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
                                                color = if (isOwn) RodapeTheme.colors.terracotaSoft else RodapeTheme.colors.cream,
                                                shape = RoundedCornerShape(RodapeRadii.sm)
                                            )
                                            .then(
                                                if (!isOwn) Modifier.border(
                                                    width = 0.5.dp,
                                                    color = RodapeTheme.colors.divider,
                                                    shape = RoundedCornerShape(RodapeRadii.sm)
                                                ) else Modifier
                                            )
                                            .clip(RoundedCornerShape(RodapeRadii.sm))
                                            // C2: reagir virou long-press (ou o ícone ao
                                            // lado do nome) — tocar a bolha pra reler não
                                            // abre mais o seletor de emoji sem querer.
                                            .pointerInput(comment.id) {
                                                detectTapGestures(onLongPress = { selectedCommentToReact = comment })
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isOwn) "Você" else userNameVal,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = InterFontFamily,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isOwn) RodapeTheme.colors.terracotaDark else MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                // C2/C5: ícone de reagir agora é um alvo de
                                                // toque real (48dp, rotulado) — o caminho
                                                // acessível e discoverable pra reagir.
                                                Box(
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .clip(CircleShape)
                                                        .clickable { selectedCommentToReact = comment }
                                                        .semantics {
                                                            role = Role.Button
                                                            contentDescription = "Reagir a este comentário"
                                                        },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = RodapeIcons.Smile,
                                                        contentDescription = null,
                                                        tint = RodapeTheme.colors.muted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            if (comment.removido) {
                                                Text(
                                                    text = "[mensagem removida pela moderação]",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = LiterataFontFamily,
                                                        fontSize = 14.5.sp,
                                                        lineHeight = 21.sp,
                                                        color = RodapeTheme.colors.muted,
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

                                                val chipBg = if (hasUserReacted) RodapeTheme.colors.terracotaSoft else RodapeTheme.colors.cardSoft
                                                val chipBorder = if (hasUserReacted) {
                                                    BorderStroke(1.dp, RodapeTheme.colors.terracota)
                                                } else {
                                                    BorderStroke(1.dp, RodapeTheme.colors.divider)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .background(chipBg, RoundedCornerShape(RodapeRadii.full))
                                                        .border(chipBorder, RoundedCornerShape(RodapeRadii.full))
                                                        .clip(RoundedCornerShape(RodapeRadii.full))
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
                                                                color = if (hasUserReacted) RodapeTheme.colors.terracotaDark else RodapeTheme.colors.tertiary
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
                                                imageVector = RodapeIcons.MoreV,
                                                contentDescription = "Moderação",
                                                tint = RodapeTheme.colors.muted,
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
                                                imageVector = RodapeIcons.MoreV,
                                                contentDescription = "Opções do comentário",
                                                tint = RodapeTheme.colors.muted,
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
                                        ) { Text("Salvar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showEditDialog = false }) {
                                            Text("Cancelar", color = RodapeTheme.colors.muted)
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
                                        }) { Text("Apagar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteDialog = false }) {
                                            Text("Cancelar", color = RodapeTheme.colors.muted)
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
                                            Text("Vira um placeholder para todos e fica registrado no log.")
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
                                        }) { Text("Remover", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRemoveDialog = false }) {
                                            Text("Cancelar", color = RodapeTheme.colors.muted)
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
                    color = RodapeTheme.colors.cream,
                    border = BorderStroke(0.5.dp, RodapeTheme.colors.divider)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pílula única: o próprio OutlinedTextField é dono da borda
                        // (foco → oliva) e do fill. O Box só carrega o weight —
                        // antes ele desenhava uma 2ª borda concêntrica (muddy no foco).
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it.take(4000) },
                                placeholder = {
                                    Text(
                                        text = "Comente este capítulo...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = InterFontFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(RodapeRadii.full),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RodapeTheme.colors.oliva,
                                    unfocusedBorderColor = RodapeTheme.colors.divider,
                                    focusedContainerColor = RodapeTheme.colors.paper,
                                    unfocusedContainerColor = RodapeTheme.colors.paper
                                )
                            )
                        }

                        // Circular send button — Terracota when active
                        // C1: sem live region, o envio era mudo pro leitor de tela —
                        // anuncia "Comentário enviado" via a View de acessibilidade.
                        val sendView = androidx.compose.ui.platform.LocalView.current
                        Box(
                            modifier = Modifier
                                // C5: alvo de toque de 48dp (mantém o círculo visual de 44).
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .background(
                                    color = if (commentText.trim().isNotEmpty()) RodapeTheme.colors.terracota else RodapeTheme.colors.cardSoft,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .clickable(enabled = commentText.trim().isNotEmpty()) {
                                    val toSend = commentText.trim()
                                    if (toSend.isNotEmpty()) {
                                        viewModel.sendComment(chapterId, toSend)
                                        commentText = ""
                                        sendView.announceForAccessibility("Comentário enviado")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = RodapeIcons.Send,
                                contentDescription = "Enviar",
                                tint = if (commentText.trim().isNotEmpty()) RodapeTheme.colors.cream else MaterialTheme.colorScheme.onSurfaceVariant,
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
            title = { Text("Adicionar reação", style = MaterialTheme.typography.titleLarge) },
            text = {
                // E2: paleta um pouco maior (era só 5). Duas linhas de 5 pra caber
                // no diálogo sem espremer/estourar.
                val emojis = listOf("❤", "😂", "😮", "😢", "🤯", "💀", "🍷", "👏", "🔥", "👍")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    emojis.chunked(5).forEach { rowEmojis ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            rowEmojis.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 30.sp,
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
