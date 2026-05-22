package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StandardCard
import com.example.ui.theme.Terracota
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val list by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificações", style = MaterialTheme.typography.headlineLarge) },
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
                    if (list.any { !it.lida }) {
                        TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                            Text("Ler todas", color = Terracota, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Sino",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Você não tem nenhuma notificação.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
            ) {
                items(list) { notif ->
                    StandardCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.markNotificationAsRead(notif.id) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (notif.lida) Color.Transparent else Terracota, CircleShape)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Beautiful and cozy payload formatting (A8)
                                val text = when (notif.tipo) {
                                    "comment_on_chapter" -> "Comentário na leitura"
                                    "next_book_decided" -> "Próxima leitura definida!"
                                    "voting_open" -> "Nova votação de livro!"
                                    "meeting_reminder" -> "Encontro marcado!"
                                    "member_finished" -> "Comemoração! 🥳"
                                    else -> "Novidades e avisos"
                                }

                                val desc = when {
                                    notif.tipo == "comment_on_chapter" && notif.payloadJson.contains("chapterTitle") -> {
                                        val userName = notif.payloadJson.substringAfter("\"userName\":\"").substringBefore("\"")
                                        val chapterTitle = notif.payloadJson.substringAfter("\"chapterTitle\":\"").substringBefore("\"")
                                        val quote = if (userName == "Luciana") ": \"Essa parte me pegou de surpresa...\"" else ""
                                        "$userName comentou em '$chapterTitle'$quote. Vem participar da rodada!"
                                    }
                                    notif.tipo == "next_book_decided" && notif.payloadJson.contains("bookTitle") -> {
                                        val bookTitle = notif.payloadJson.substringAfter("\"bookTitle\":\"").substringBefore("\"")
                                        "Nosso próximo companheiro de viagem será '$bookTitle'. Prepare seu coração!"
                                    }
                                    notif.tipo == "voting_open" -> {
                                        "Quem será nosso próximo parceiro de leituras? A votação iniciou, dê seu palpite!"
                                    }
                                    notif.payloadJson.contains("meetingId") -> {
                                        val datePart = notif.payloadJson.substringAfter("\"date\":\"").substringBefore("\"")
                                        "Nosso próximo encontro será: $datePart. Confirme sua presença para nos vermos!"
                                    }
                                    notif.payloadJson.contains("bookTitle") -> {
                                        val userName = notif.payloadJson.substringAfter("\"userName\":\"").substringBefore("\"")
                                        val bookTitle = notif.payloadJson.substringAfter("\"bookTitle\":\"").substringBefore("\"")
                                        "Viva! $userName terminou todas as metas de '$bookTitle'! Que jornada incrível."
                                    }
                                    else -> "Navegue para conferir os detalhes desse momento do clube."
                                }

                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
