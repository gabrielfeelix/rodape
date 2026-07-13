package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.ui.components.Avatar
import com.example.ui.components.SkeletonRowList
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.RodapeCard
import com.example.ui.components.rememberShowLoading
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.timeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationLogScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val removed by viewModel.removedCommentsInActiveClub.collectAsState()
    val members by viewModel.clubMembers.collectAsState()
    val isSuper by viewModel.isCurrentUserSuperAdmin.collectAsState()
    val showLoading = rememberShowLoading(hasData = removed.isNotEmpty())

    // Restaurar reverte uma decisão de moderação — exige confirmação (evita
    // toque acidental). Guarda o id do comentário a restaurar.
    var restoreCommentId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moderação") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(RodapeIcons.Back, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (showLoading) {
            SkeletonRowList(
                count = 3,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        } else if (removed.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = RodapeTheme.colors.muted,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "Nenhum comentário removido por aqui.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = RodapeTheme.colors.muted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(removed, key = { it.id }) { c ->
                    val author = members.find { it.id == c.userId }
                    val remover = members.find { it.id == c.removidoPor }
                    RodapeCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Avatar(name = author?.nome ?: "Membro", avatarUrl = author?.avatarUrl ?: "", size = 28.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    author?.nome ?: "Membro",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.ink
                                    )
                                )
                                Text(
                                    timeAgo(c.criadoEm),
                                    style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = c.texto,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = RodapeTheme.colors.muted,
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Removido por ${remover?.nome ?: "admin"}" +
                                (if (c.motivoRemocao?.isNotBlank() == true) " · Motivo: ${c.motivoRemocao}" else ""),
                            style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.terracota)
                        )
                        if (isSuper) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TbButton(
                                text = "Restaurar",
                                onClick = { restoreCommentId = c.id },
                                variant = TbButtonVariant.Outline,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    if (restoreCommentId != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { restoreCommentId = null },
            title = { Text("Restaurar comentário?") },
            text = {
                Text(
                    "O comentário volta a aparecer na conversa para todos os membros.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restoreRemovedComment(restoreCommentId!!)
                    restoreCommentId = null
                }) { Text("Restaurar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { restoreCommentId = null }) { Text("Cancelar", color = RodapeTheme.colors.muted) }
            }
        )
    }
}
