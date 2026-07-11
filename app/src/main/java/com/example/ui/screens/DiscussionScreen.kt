package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val comments by viewModel.getCommentsForChapter(chapterId).collectAsState(initial = emptyList())
    val chapters by viewModel.currentChapters.collectAsState()
    val progress by viewModel.userProgress.collectAsState()
    val members by viewModel.clubMembers.collectAsState()
    val reactions by viewModel.getReactionsForChapter(chapterId).collectAsState(initial = emptyList())

    val chapterObj = chapters.find { it.id == chapterId }
    val chapterNum = chapterObj?.numero ?: 1
    val currentProgNum = progress?.currentChapter ?: 0

    val isAheadOfProgress = chapterNum > currentProgNum
    var forceRevealDebate by remember { mutableStateOf(false) }

    var commentText by remember { mutableStateOf("") }
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
        if (isAheadOfProgress && !forceRevealDebate) {
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

                if (comments.isEmpty()) {
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
                    val currentUid by viewModel.currentUserId.collectAsState()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(comments) { comment ->
                            val userObj = members.find { it.id == comment.userId }
                            val userNameVal = if (comment.userId == currentUid) "Você" else userObj?.nome ?: "Iniciante"
                            val commentReactions = reactions.filter { it.commentId == comment.id }
                            val isOwn = comment.userId == currentUid
                            val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()
                            var showModMenu by remember(comment.id) { mutableStateOf(false) }
                            var showRemoveDialog by remember(comment.id) { mutableStateOf(false) }
                            var modMotivo by remember(comment.id) { mutableStateOf("") }

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
                                            modifier = Modifier.size(32.dp)
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
                                onValueChange = { commentText = it },
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
                                    if (commentText.trim().isNotEmpty()) {
                                        viewModel.sendComment(chapterId, commentText)
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
