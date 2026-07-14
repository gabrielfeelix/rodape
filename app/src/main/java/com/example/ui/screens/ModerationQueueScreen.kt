package com.example.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ContentReport
import com.example.ui.components.Avatar
import com.example.ui.components.RodapeDialog
import com.example.ui.components.SkeletonRowList
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.rememberShowLoading
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.timeAgo

/**
 * Fila de denúncias pendentes (admin) — cumpre o requisito de "agir sobre denúncias
 * em até 24h". Cada card mostra o autor denunciado, motivo e detalhe; o admin pode
 * REMOVER o conteúdo (resolve a denúncia) ou DESCARTAR (improcedente).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationQueueScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
) {
    val reports by viewModel.pendingReports.collectAsStateWithLifecycle()
    val loading by viewModel.pendingReportsLoading.collectAsStateWithLifecycle()
    val members by viewModel.clubMembers.collectAsStateWithLifecycle()
    val showLoading = rememberShowLoading(hasData = reports.isNotEmpty()) || loading

    LaunchedEffect(Unit) { viewModel.refreshPendingReports() }

    var confirmRemove by remember { mutableStateOf<ContentReport?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Denúncias") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(RodapeIcons.Back, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshPendingReports() }) {
                        Icon(RodapeIcons.Info, contentDescription = "Atualizar")
                    }
                },
            )
        }
    ) { padding ->
        if (showLoading && reports.isEmpty()) {
            SkeletonRowList(
                count = 3,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        } else if (reports.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = RodapeIcons.Shield,
                        contentDescription = null,
                        tint = RodapeTheme.colors.muted,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "Nenhuma denúncia pendente. Tudo tranquilo por aqui.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = RodapeTheme.colors.muted,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(reports, key = { it.id }) { r ->
                    val autor = members.find { it.id == r.targetUserId }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RodapeTheme.colors.cardSurface, RoundedCornerShape(RodapeRadii.sm))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Avatar(name = autor?.nome ?: "Membro", avatarUrl = autor?.avatarUrl ?: "", size = 32.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    autor?.nome ?: "Membro",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold, color = RodapeTheme.colors.ink,
                                    ),
                                )
                                Text(
                                    "${alvoLabel(r)} · ${timeAgo(r.criadoEm)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
                                )
                            }
                        }
                        Text(
                            "Motivo: ${r.motivo.label}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.terracota),
                        )
                        if (!r.detalhe.isNullOrBlank()) {
                            Text(
                                "“${r.detalhe}”",
                                style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.ink),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TbButton(
                                text = "Remover conteúdo",
                                onClick = { confirmRemove = r },
                                variant = TbButtonVariant.Terra,
                                size = TbButtonSize.Sm,
                            )
                            TbButton(
                                text = "Descartar",
                                onClick = { viewModel.dismissReport(r.id) },
                                variant = TbButtonVariant.Outline,
                                size = TbButtonSize.Sm,
                            )
                        }
                    }
                }
            }
        }
    }

    confirmRemove?.let { r ->
        RodapeDialog(
            onDismissRequest = { confirmRemove = null },
            title = "Remover conteúdo?",
            text = {
                Text(
                    "O conteúdo some para todos os membros e a denúncia é marcada como resolvida.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resolveReportByRemoving(r)
                    confirmRemove = null
                }) { Text("Remover", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) {
                    Text("Cancelar", color = RodapeTheme.colors.muted)
                }
            },
        )
    }
}

private fun alvoLabel(r: ContentReport): String = when (r.targetType.wire) {
    "comment" -> "Comentário"
    "saved_quote" -> "Citação"
    "book_rating" -> "Resenha"
    "book_suggestion" -> "Sugestão"
    "profile" -> "Perfil"
    "reaction" -> "Reação"
    else -> "Conteúdo"
}
