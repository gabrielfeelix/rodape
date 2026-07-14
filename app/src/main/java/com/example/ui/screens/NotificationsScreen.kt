package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.RodapeIcons
import com.example.ui.theme.RodapeTheme
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
                            imageVector = RodapeIcons.Back,
                            contentDescription = "Voltar",
                            tint = RodapeTheme.colors.terracota
                        )
                    }
                },
                actions = {
                    if (list.any { !it.lida }) {
                        TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                            Text(
                                "Ler todas",
                                color = RodapeTheme.colors.terracota,
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
                    // Empty com personalidade (3.11): sino em disco pastel +
                    // copy que celebra o vazio em vez de constatar ausência.
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(RodapeTheme.colors.olivaSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = RodapeIcons.Bell,
                            contentDescription = null,
                            tint = RodapeTheme.colors.olivaDark,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = "Tudo em dia por aqui",
                        style = MaterialTheme.typography.titleMedium,
                        color = RodapeTheme.colors.ink
                    )
                    Text(
                        text = "Quando o clube se mexer — votação, encontro, comentário — você fica sabendo aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RodapeTheme.colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
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
                // 10dp (3.11): 4dp colava unread/read e as linhas viravam bloco.
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                grouped.forEach { (group, groupItems) ->
                    item {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = RodapeTheme.colors.muted,
                            modifier = Modifier.padding(
                                start = 4.dp,
                                top = 16.dp,
                                bottom = 8.dp
                            )
                        )
                    }
                    itemsIndexed(groupItems, key = { _, n -> n.id }) { i, notif ->
                        // Swipe = marcar como LIDA (3.11) — não há delete de
                        // notificação no backend, então o gesto usa a ação real
                        // que existe; o item volta pro lugar e o estilo unread some.
                        val swipeState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { v ->
                                if (v != SwipeToDismissBoxValue.Settled && !notif.lida) {
                                    viewModel.markNotificationAsRead(notif.id)
                                }
                                false
                            }
                        )
                        SwipeToDismissBox(
                            state = swipeState,
                            enableDismissFromStartToEnd = !notif.lida,
                            enableDismissFromEndToStart = !notif.lida,
                            backgroundContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(RodapeTheme.colors.olivaSoft)
                                        .padding(horizontal = 16.dp),
                                ) {
                                    Icon(
                                        imageVector = RodapeIcons.Check,
                                        contentDescription = null,
                                        tint = RodapeTheme.colors.olivaDark,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "Lida",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = RodapeTheme.colors.olivaDark,
                                    )
                                }
                            },
                        ) {
                        NotificationItem(
                            modifier = Modifier.staggeredEntrance(index = i),
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
                        } // fecha o content do SwipeToDismissBox
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        "comment_on_chapter" -> Pair(RodapeIcons.Heart, Pair(RodapeTheme.colors.olivaSoft, RodapeTheme.colors.olivaDark))
        "voting_open", "next_book_decided", "voting_closed" -> Pair(RodapeIcons.Vote, Pair(RodapeTheme.colors.terracotaSoft, RodapeTheme.colors.terracota))
        "meeting_reminder" -> Pair(RodapeIcons.Calendar, Pair(RodapeTheme.colors.olivaSoft, RodapeTheme.colors.olivaDark))
        // 3.11: celebração em DOURADO (o círculo ink parecia bug entre pastéis).
        "member_finished" -> Pair(RodapeIcons.CheckCircle, Pair(RodapeTheme.colors.warningSoft, RodapeTheme.colors.warning))
        "book_finished" -> Pair(RodapeIcons.CheckCircle, Pair(RodapeTheme.colors.olivaSoft, RodapeTheme.colors.olivaDark))
        // 3.11: evento negativo em tom NEUTRO — terracota é de promoção/celebração,
        // não de "você saiu do clube".
        "member_removed" -> Pair(RodapeIcons.Info, Pair(RodapeTheme.colors.dividerSoft, RodapeTheme.colors.muted))
        "promoted_to_admin", "super_admin_transferred" -> Pair(RodapeIcons.StarFill, Pair(RodapeTheme.colors.terracotaSoft, RodapeTheme.colors.terracota))
        else -> Pair(RodapeIcons.Bell, Pair(RodapeTheme.colors.olivaSoft, RodapeTheme.colors.olivaDark))
    }

    // 3.11: unread = TRILHO terracota 3dp na borda + título mais pesado — o
    // fill cream inteiro era fraco sobre o app já cream. Dot vira só reforço.
    val railColor = RodapeTheme.colors.terracota
    // Fundo OPACO obrigatório: o item fica sobre o backgroundContent do
    // SwipeToDismissBox (o "✓ Lida" olivaSoft). Sem preencher, em repouso esse
    // fundo verde vaza através do item. O trilho é desenhado DEPOIS (por cima).
    val itemBg = MaterialTheme.colorScheme.background
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(itemBg)
            .drawBehind {
                if (!lida) {
                    drawRect(
                        color = railColor,
                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height),
                    )
                }
            }
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
                    // Peso diferencia unread de read (3.11) — antes tudo SemiBold.
                    fontWeight = if (!lida) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = RodapeTheme.colors.ink
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = RodapeTheme.colors.muted
            )
        }

        // Unread dot
        if (!lida) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RodapeTheme.colors.terracota)
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
