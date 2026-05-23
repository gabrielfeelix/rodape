package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.Avatar
import com.example.ui.theme.Cream
import com.example.ui.theme.Ink
import com.example.ui.theme.Muted
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.Terracota
import com.example.ui.theme.TerracotaSoft
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
                title = {
                    Text(
                        "Avisos",
                        style = MaterialTheme.typography.headlineLarge
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
                    if (list.any { !it.lida }) {
                        TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                            Text(
                                "Ler todas",
                                color = Terracota,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                        tint = Muted,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Você não tem nenhuma notificação.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Muted
                    )
                }
            }
        } else {
            // Group notifications by day
            val grouped = list.groupBy { notif ->
                when {
                    notif.payloadJson.contains("meetingId") -> "ESTA SEMANA"
                    else -> "HOJE"
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                grouped.forEach { (group, groupItems) ->
                    item {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Muted,
                            modifier = Modifier.padding(
                                start = 4.dp,
                                top = 16.dp,
                                bottom = 8.dp
                            )
                        )
                    }
                    items(groupItems) { notif ->
                        NotificationItem(
                            tipo = notif.tipo,
                            payloadJson = notif.payloadJson,
                            lida = notif.lida,
                            onClick = { viewModel.markNotificationAsRead(notif.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    tipo: String,
    payloadJson: String,
    lida: Boolean,
    onClick: () -> Unit
) {
    // Extract user name from payload to decide icon vs avatar
    val userName = if (payloadJson.contains("\"userName\"")) {
        payloadJson.substringAfter("\"userName\":\"").substringBefore("\"")
    } else ""

    // Notification title
    val title = when (tipo) {
        "comment_on_chapter" -> "Comentário na leitura"
        "next_book_decided" -> "Próxima leitura definida!"
        "voting_open" -> "Nova votação de livro!"
        "voting_closed" -> "Votação encerrada"
        "meeting_reminder" -> "Encontro marcado!"
        "member_finished" -> "Comemoração! 🥳"
        "member_removed" -> "Você foi removido do clube"
        "promoted_to_admin" -> "Você virou admin!"
        "super_admin_transferred" -> "Você é o novo super admin"
        else -> "Novidades e avisos"
    }

    // Notification description
    val desc = when {
        tipo == "comment_on_chapter" && payloadJson.contains("chapterTitle") -> {
            val chapterTitle = payloadJson.substringAfter("\"chapterTitle\":\"").substringBefore("\"")
            val quote = if (userName == "Luciana") ": \"Essa parte me pegou de surpresa...\"" else ""
            "$userName comentou em '$chapterTitle'$quote. Vem participar da rodada!"
        }
        tipo == "next_book_decided" && payloadJson.contains("bookTitle") -> {
            val bookTitle = payloadJson.substringAfter("\"bookTitle\":\"").substringBefore("\"")
            "Nosso próximo companheiro de viagem será '$bookTitle'. Prepare seu coração!"
        }
        tipo == "voting_open" -> {
            "Quem será nosso próximo parceiro de leituras? A votação iniciou, dê seu palpite!"
        }
        tipo == "voting_closed" && payloadJson.contains("titulos") -> {
            val titulos = payloadJson.substringAfter("\"titulos\":[").substringBefore("]")
                .replace("\"", "").trim().ifEmpty { "—" }
            val n = payloadJson.substringAfter("\"n\":").substringBefore("}").trim().toIntOrNull() ?: 1
            if (n == 1) "O clube escolheu: $titulos." else "O clube escolheu $n livros: $titulos."
        }
        tipo == "member_removed" -> {
            val clubName = payloadJson.substringAfter("\"clubName\":\"").substringBefore("\"")
            val motivo = if (payloadJson.contains("\"motivo\":\"")) {
                payloadJson.substringAfter("\"motivo\":\"").substringBefore("\"")
            } else ""
            if (motivo.isBlank()) "Você foi removido de '$clubName'."
            else "Você foi removido de '$clubName'. Motivo: $motivo"
        }
        tipo == "promoted_to_admin" -> {
            val clubName = payloadJson.substringAfter("\"clubName\":\"").substringBefore("\"")
            val promotedBy = payloadJson.substringAfter("\"promotedBy\":\"").substringBefore("\"")
            "$promotedBy te promoveu a admin em '$clubName'. Boas-vindas ao time de organização."
        }
        tipo == "super_admin_transferred" -> {
            val clubName = payloadJson.substringAfter("\"clubName\":\"").substringBefore("\"")
            val fromUser = payloadJson.substringAfter("\"fromUser\":\"").substringBefore("\"")
            "$fromUser te passou o título de super admin de '$clubName'. Cuida bem dele."
        }
        payloadJson.contains("meetingId") -> {
            val datePart = payloadJson.substringAfter("\"date\":\"").substringBefore("\"")
            "Nosso próximo encontro será: $datePart. Confirme sua presença para nos vermos!"
        }
        payloadJson.contains("bookTitle") -> {
            val bookTitle = payloadJson.substringAfter("\"bookTitle\":\"").substringBefore("\"")
            "Viva! $userName terminou todas as metas de '$bookTitle'! Que jornada incrível."
        }
        else -> "Navegue para conferir os detalhes desse momento do clube."
    }

    // Icon type
    val iconInfo: Pair<ImageVector, Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>> = when (tipo) {
        "comment_on_chapter" -> Pair(Icons.Outlined.FavoriteBorder, Pair(OlivaSoft, OlivaDark))
        "voting_open", "next_book_decided", "voting_closed" -> Pair(Icons.Outlined.ThumbUp, Pair(TerracotaSoft, Terracota))
        "meeting_reminder" -> Pair(Icons.Outlined.DateRange, Pair(OlivaSoft, OlivaDark))
        "member_finished" -> Pair(Icons.Outlined.CheckCircle, Pair(Ink, Cream))
        "member_removed" -> Pair(Icons.Outlined.Info, Pair(TerracotaSoft, Terracota))
        "promoted_to_admin", "super_admin_transferred" -> Pair(Icons.Outlined.Star, Pair(TerracotaSoft, Terracota))
        else -> Pair(Icons.Outlined.Notifications, Pair(OlivaSoft, OlivaDark))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (!lida) Cream else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar (if user notification) or icon circle
        if (userName.isNotBlank() && tipo == "comment_on_chapter") {
            Avatar(
                name = userName,
                size = 40.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconInfo.second.first),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconInfo.first,
                    contentDescription = null,
                    tint = iconInfo.second.second,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Text content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Ink
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
        }

        // Unread dot
        if (!lida) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Terracota)
            )
        }
    }
}
