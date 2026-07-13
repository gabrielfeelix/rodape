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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.Avatar
import com.example.ui.components.SkeletonRowList
import com.example.ui.components.rememberShowLoading
import com.example.ui.theme.Cream
import com.example.ui.theme.Ink
import com.example.ui.theme.Muted
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.Terracota
import com.example.ui.theme.TerracotaSoft
import com.example.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDiscussion: (chapterId: String, title: String) -> Unit = { _, _ -> },
    onNavigateToTab: (String) -> Unit = {}
) {
    val list by viewModel.notifications.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val showLoading = rememberShowLoading(hasData = list.isNotEmpty())

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
        if (showLoading) {
            SkeletonRowList(
                count = 5,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else if (list.isEmpty()) {
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
            // Agrupa por data REAL (criadoEm), não pelo conteúdo do payload.
            val now = System.currentTimeMillis()
            val umDia = 86_400_000L
            val grouped = list
                .sortedByDescending { it.criadoEm }
                .groupBy { notif ->
                    val idade = now - notif.criadoEm
                    when {
                        idade < umDia -> "HOJE"
                        idade < 2 * umDia -> "ONTEM"
                        idade < 7 * umDia -> "ESTA SEMANA"
                        idade < 30 * umDia -> "ESTE MÊS"
                        else -> "ANTES"
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
                            onClick = {
                                viewModel.markNotificationAsRead(notif.id)
                                handleNotificationNavigation(
                                    tipo = notif.tipo,
                                    payloadJson = notif.payloadJson,
                                    chapters = chapters,
                                    onNavigateToDiscussion = onNavigateToDiscussion,
                                    onNavigateToTab = onNavigateToTab,
                                    onNavigateBack = onNavigateBack
                                )
                            }
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
    // Decodifica o payload JSON de forma segura. Qualquer falha vira null,
    // então nunca renderizamos JSON cru na tela.
    val payload: JsonObject? = remember(payloadJson) {
        runCatching { Json.parseToJsonElement(payloadJson).jsonObject }.getOrNull()
    }
    fun str(key: String): String? =
        (payload?.get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    fun strList(key: String): List<String> =
        (payload?.get(key) as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() } }
            ?: emptyList()

    val actorName = str("actorName")
    val clubName = str("clubName")
    val bookTitle = str("bookTitle")
    val chapterTitle = str("chapterTitle")
    val motivo = str("motivo")
    val data = str("data")
    val titulos = strList("titulos")

    // Fallbacks amigáveis para quando alguma chave vier ausente.
    val actor = actorName ?: "Alguém"
    val club = clubName ?: "seu clube"
    val book = bookTitle ?: "o livro"
    val chapter = chapterTitle ?: "a leitura"

    // Notification title
    val title = when (tipo) {
        "comment_on_chapter" -> "Novo comentário em \"$chapter\""
        "next_book_decided" -> "Próximo livro: \"$book\""
        "book_finished" -> "Livro finalizado: \"$book\""
        "voting_open" -> "Votação aberta em \"$club\""
        "voting_closed" -> "Votação encerrada em \"$club\""
        "meeting_reminder" -> "Encontro chegando em \"$club\""
        "member_finished" -> "$actor terminou \"$book\" 🎉"
        "member_removed" -> "Você saiu de \"$club\""
        "promoted_to_admin" -> "$actor te promoveu a admin em \"$club\""
        "super_admin_transferred" -> "$actor te passou o comando de \"$club\""
        else -> "Novidades e avisos"
    }

    // Notification description
    val desc = when (tipo) {
        "comment_on_chapter" ->
            "$actor comentou em ${bookTitle?.let { "\"$it\"" } ?: "uma leitura"}. Venha participar da rodada!"
        "next_book_decided" ->
            "Nossa próxima leitura já foi escolhida. Prepare seu coração!"
        "book_finished" ->
            "O clube terminou essa leitura. Já pensou no próximo livro?"
        "voting_open" ->
            "$actor abriu a votação. Dê seu palpite para o próximo livro!"
        "voting_closed" ->
            if (titulos.isNotEmpty()) "O clube escolheu: ${titulos.joinToString(", ")}."
            else "A votação foi encerrada. Confira o resultado!"
        "meeting_reminder" ->
            if (data != null) "Nosso próximo encontro: $data. Confirme sua presença!"
            else "Nosso próximo encontro está chegando. Confirme sua presença!"
        "member_finished" ->
            "Que jornada incrível! Bora comemorar essa conquista."
        "member_removed" ->
            "$actor removeu você" + (if (motivo != null) " · $motivo" else "")
        "promoted_to_admin" ->
            "Boas-vindas ao time de organização do clube."
        "super_admin_transferred" ->
            "Agora o comando é seu. Cuida bem do clube. 💛"
        else -> "Navegue para conferir os detalhes desse momento do clube."
    }

    // Icon type
    val iconInfo: Pair<ImageVector, Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>> = when (tipo) {
        "comment_on_chapter" -> Pair(Icons.Outlined.FavoriteBorder, Pair(OlivaSoft, OlivaDark))
        "voting_open", "next_book_decided", "voting_closed" -> Pair(Icons.Outlined.ThumbUp, Pair(TerracotaSoft, Terracota))
        "meeting_reminder" -> Pair(Icons.Outlined.DateRange, Pair(OlivaSoft, OlivaDark))
        "member_finished" -> Pair(Icons.Outlined.CheckCircle, Pair(Ink, Cream))
        "book_finished" -> Pair(Icons.Outlined.CheckCircle, Pair(OlivaSoft, OlivaDark))
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
            .semantics { stateDescription = if (lida) "lida" else "não lida" }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar (if user notification) or icon circle
        if (!actorName.isNullOrBlank() && tipo == "comment_on_chapter") {
            Avatar(
                name = actorName,
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

private fun handleNotificationNavigation(
    tipo: String,
    payloadJson: String,
    chapters: List<com.example.data.model.Chapter>,
    onNavigateToDiscussion: (String, String) -> Unit,
    onNavigateToTab: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // F2: lê o payload pelo JSON parseado (o slicing manual quebrava se o servidor
    // reordenasse/espaçasse os campos).
    val payload: JsonObject? = runCatching { Json.parseToJsonElement(payloadJson).jsonObject }.getOrNull()
    fun field(name: String): String? = (payload?.get(name) as? JsonPrimitive)?.contentOrNull
    when (tipo) {
        "comment_on_chapter" -> {
            val chapterId = field("chapterId").orEmpty()
            if (chapterId.isNotBlank()) {
                val chapter = chapters.find { it.id == chapterId }
                val title = chapter?.titulo ?: field("chapterTitle") ?: "Capítulo"
                onNavigateToDiscussion(chapterId, title)
            }
        }
        "voting_open", "voting_closed", "next_book_decided" -> {
            onNavigateBack()
            onNavigateToTab("next")
        }
        "meeting_reminder" -> {
            onNavigateBack()
            onNavigateToTab("next")
        }
        "member_finished" -> {
            onNavigateBack()
            onNavigateToTab("book")
        }
        "promoted_to_admin", "super_admin_transferred" -> {
            onNavigateBack()
            onNavigateToTab("home")
        }
        // member_removed e demais: só marca como lida, sem navegação
        else -> {}
    }
}
