package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.ui.components.Avatar
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.RodapeCard
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moderação") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (removed.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nenhum comentário removido por aqui.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted
                )
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
                                        color = Ink
                                    )
                                )
                                Text(
                                    timeAgo(c.criadoEm),
                                    style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = c.texto,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Muted,
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Removido por ${remover?.nome ?: "admin"}" +
                                (if (c.motivoRemocao?.isNotBlank() == true) " · Motivo: ${c.motivoRemocao}" else ""),
                            style = MaterialTheme.typography.labelSmall.copy(color = Terracota)
                        )
                        if (isSuper) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TbButton(
                                text = "Restaurar",
                                onClick = { viewModel.restoreRemovedComment(c.id) },
                                variant = TbButtonVariant.Outline,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
