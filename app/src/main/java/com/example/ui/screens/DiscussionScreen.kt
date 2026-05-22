package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.VerdeMusgo
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.data.model.Comment
import com.example.ui.components.MemberAvatar
import com.example.ui.components.PillButton
import com.example.ui.components.StandardCard
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
                    Text(
                        text = "Capítulo $chapterNum · $chapterTitle",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                actions = {
                    IconButton(onClick = { /* More options template */ }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Opções",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                StandardCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Atenção: possível spoiler",
                            style = MaterialTheme.typography.headlineLarge.copy(color = Terracota, textAlign = TextAlign.Center)
                        )

                        Text(
                            text = "Este capítulo está à frente do seu progresso atual de leitura (você parou no capítulo $currentProgNum).",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PillButton(
                            text = "Revelar debate mesmo assim",
                            onClick = { forceRevealDebate = true }
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
                // Redesigned Spoiler Pill (A2)
                val isLido = chapterNum <= currentProgNum
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLido) {
                        Row(
                            modifier = Modifier
                                .background(VerdeMusgo.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                                .padding(vertical = 12.dp, horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = VerdeMusgo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tu já passou daqui. Tá liberado.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = VerdeMusgo
                                )
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .background(Terracota.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                                .padding(vertical = 12.dp, horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Terracota,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "não lido – cuidado com o spoiler!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Terracota
                                )
                            )
                        }
                    }
                }

                // Editorial Title (B4)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CAPÍTULO $chapterNum",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            letterSpacing = 1.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = chapterTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FrauncesFontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(comments) { comment ->
                            val userObj = members.find { it.id == comment.userId }
                            val userNameVal = if (comment.userId == "user_voce") "Você" else userObj?.nome ?: "Iniciante"
                            val commentReactions = reactions.filter { it.commentId == comment.id }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MemberAvatar(name = userNameVal, avatarUrl = userObj?.avatarUrl ?: "")

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    StandardCard(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedCommentToReact = comment
                                        }
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = userNameVal,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Terracota)
                                            )
                                            Text(
                                                text = comment.texto,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }

                                    // Render refined and responsive comment reactions (B9)
                                    if (commentReactions.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val emojiGroups = commentReactions.groupBy { it.emoji }
                                            emojiGroups.forEach { (emoji, list) ->
                                                val count = list.size
                                                val hasUserReacted = list.any { it.userId == "user_voce" }

                                                val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                                                val reactedBg = Terracota.copy(alpha = 0.08f)
                                                val normalBg = if (darkTheme) Color(0xFF2E2A24) else Color(0xFFFBF7EE)

                                                val borderStroke = if (hasUserReacted) {
                                                    BorderStroke(1.dp, Terracota)
                                                } else {
                                                    BorderStroke(1.dp, Color.Transparent)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .background(if (hasUserReacted) reactedBg else normalBg, RoundedCornerShape(14.dp))
                                                        .border(borderStroke, RoundedCornerShape(14.dp))
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable { viewModel.toggleReaction(comment.id, emoji) }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = emoji,
                                                            fontSize = 16.sp
                                                        )
                                                        Text(
                                                            text = count.toString(),
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontFamily = InterFontFamily,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = if (hasUserReacted) Terracota else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Comment input box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Comenta esse capítulo...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        IconButton(
                            onClick = {
                                if (commentText.trim().isNotEmpty()) {
                                    viewModel.sendComment(chapterId, commentText)
                                    commentText = ""
                                }
                            },
                            enabled = commentText.trim().isNotEmpty(),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Terracota)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Send,
                                contentDescription = "Enviar",
                                tint = if (commentText.trim().isNotEmpty()) Terracota else MaterialTheme.colorScheme.onSurfaceVariant
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
