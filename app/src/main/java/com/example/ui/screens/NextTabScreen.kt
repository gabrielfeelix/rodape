package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun NextTabScreen(
    viewModel: MainViewModel,
    onNavigateToSuggestBook: () -> Unit,
    onNavigateToBookDetail: (String) -> Unit = {}
) {
    var subTab by remember { mutableStateOf("encontro") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Inner tabs switcher at the top of Próximo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .height(48.dp)
                .background(
                    OlivaSoft.copy(alpha = 0.5f),
                    RoundedCornerShape(24.dp)
                )
                .border(
                    0.5.dp,
                    Divider,
                    RoundedCornerShape(24.dp)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val nextTabs = listOf("encontro" to "Encontro", "votacao" to "Votação", "estante" to "Estante")
            nextTabs.forEach { (tab, label) ->
                val isSelected = subTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) Oliva else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { subTab = tab },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (isSelected) Cream else Tertiary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        // Terracota underline accent below selected tab — rendered as a thin bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            val nextTabs = listOf("encontro", "votacao", "estante")
            nextTabs.forEachIndexed { i, tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            if (subTab == tab) Terracota else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (subTab) {
                "encontro" -> EncontroTab(viewModel = viewModel)
                "votacao" -> VotacaoTab(viewModel = viewModel, onNavigateToSuggestBook = onNavigateToSuggestBook)
                "estante" -> EstanteTab(viewModel = viewModel, onNavigateToBookDetail = onNavigateToBookDetail)
            }
        }
    }
}

// --- SUB-TAB 1: ENCONTRO ---
@Composable
fun EncontroTab(viewModel: MainViewModel) {
    val meeting by viewModel.latestMeeting.collectAsState()
    val rsvps by viewModel.latestMeetingRsvps.collectAsState()
    val members by viewModel.clubMembers.collectAsState()

    var isConfirmadosExpanded by remember { mutableStateOf(true) }
    var isTalvezExpanded by remember { mutableStateOf(false) }
    var isNaoVouExpanded by remember { mutableStateOf(false) }

    if (meeting == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Nenhum próximo encontro agendado.",
                style = MaterialTheme.typography.bodyLarge,
                color = Muted
            )
        }
    } else {
        val userRsvp by viewModel.getRsvpOfUser(meeting!!.id).collectAsState(initial = null)
        val userStatus = userRsvp?.status ?: "Sem resposta"

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Meeting header card with olive gradient
                TramabookCard(contentPadding = PaddingValues(0.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Oliva, OlivaDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Cream.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = meeting!!.data.substringBefore(" "),
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontSize = 20.sp,
                                            color = Cream,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "out",
                                        style = MaterialTheme.typography.labelMedium.copy(color = Cream.copy(alpha = 0.8f))
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Próximo encontro",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = OlivaSoft,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = meeting!!.data,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Cream
                                    )
                                )
                                Text(
                                    text = meeting!!.hora,
                                    style = MaterialTheme.typography.bodyLarge.copy(color = OlivaSoft.copy(alpha = 0.9f))
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = "Local",
                                tint = OlivaMid,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = meeting!!.local,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Tertiary
                            )
                        }
                    }
                }
            }

            item {
                TbSectionHeader(
                    title = "Sua participação",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TramabookCard {
                    Text(
                        "Você vai participar desse encontro?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Ink
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Vou", "Talvez", "Não vou").forEach { statusOption ->
                            val isSelected = userStatus == statusOption

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(if (isSelected) Ink else Color.Transparent)
                                    .clickable { viewModel.rsvpMeeting(meeting!!.id, statusOption) }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else Divider,
                                        shape = RoundedCornerShape(22.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statusOption,
                                    color = if (isSelected) Cream else Tertiary,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }

            // RSVP breakdowns
            item {
                TbSectionHeader(
                    title = "Quem vai?",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TramabookCard {
                    val confirmados = rsvps.filter { it.status == "Vou" }
                    val talvez = rsvps.filter { it.status == "Talvez" }
                    val naoVou = rsvps.filter { it.status == "Não vou" }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                confirmados.size.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    color = Oliva,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text("Confirmados", style = MaterialTheme.typography.labelSmall.copy(color = Muted))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                talvez.size.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    color = OlivaMid, // substituído de Color(0xFFD4A373)
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text("Talvez", style = MaterialTheme.typography.labelSmall.copy(color = Muted))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                naoVou.size.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    color = Tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text("Não vão", style = MaterialTheme.typography.labelSmall.copy(color = Muted))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = Divider)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Group 1: Confirmados
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isConfirmadosExpanded = !isConfirmadosExpanded }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isConfirmadosExpanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = OlivaMid,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirmados (${confirmados.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isConfirmadosExpanded) {
                        Column(
                            modifier = Modifier.padding(start = 28.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (confirmados.isEmpty()) {
                                Text(
                                    "Nenhum membro confirmado ainda.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Muted
                                )
                            } else {
                                confirmados.forEach { resp ->
                                    val userObj = members.find { it.id == resp.userId }
                                    val nameVal = if (resp.userId == "user_voce") "Você" else userObj?.nome ?: "Membro"
                                    val urlVal = if (resp.userId == "user_voce") (viewModel.currentUser.value?.avatarUrl ?: "") else userObj?.avatarUrl ?: ""
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Avatar(name = nameVal, avatarUrl = urlVal, size = 32.dp)
                                        Text(text = nameVal, style = MaterialTheme.typography.bodyLarge.copy(color = Ink))
                                    }
                                }
                            }
                        }
                    }

                    // Group 2: Talvez
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTalvezExpanded = !isTalvezExpanded }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTalvezExpanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = OlivaMid,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Talvez (${talvez.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isTalvezExpanded) {
                        Column(
                            modifier = Modifier.padding(start = 28.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (talvez.isEmpty()) {
                                Text(
                                    "Ninguém em dúvida por enquanto.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Muted
                                )
                            } else {
                                talvez.forEach { resp ->
                                    val userObj = members.find { it.id == resp.userId }
                                    val nameVal = if (resp.userId == "user_voce") "Você" else userObj?.nome ?: "Membro"
                                    val urlVal = if (resp.userId == "user_voce") (viewModel.currentUser.value?.avatarUrl ?: "") else userObj?.avatarUrl ?: ""
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Avatar(name = nameVal, avatarUrl = urlVal, size = 32.dp)
                                        Text(text = nameVal, style = MaterialTheme.typography.bodyLarge.copy(color = Ink))
                                    }
                                }
                            }
                        }
                    }

                    // Group 3: Não vão
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isNaoVouExpanded = !isNaoVouExpanded }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isNaoVouExpanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = OlivaMid,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Não vão (${naoVou.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isNaoVouExpanded) {
                        Column(
                            modifier = Modifier.padding(start = 28.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (naoVou.isEmpty()) {
                                Text(
                                    "Ninguém recusou até agora.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Muted
                                )
                            } else {
                                naoVou.forEach { resp ->
                                    val userObj = members.find { it.id == resp.userId }
                                    val nameVal = if (resp.userId == "user_voce") "Você" else userObj?.nome ?: "Membro"
                                    val urlVal = if (resp.userId == "user_voce") (viewModel.currentUser.value?.avatarUrl ?: "") else userObj?.avatarUrl ?: ""
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Avatar(name = nameVal, avatarUrl = urlVal, size = 32.dp)
                                        Text(text = nameVal, style = MaterialTheme.typography.bodyLarge.copy(color = Ink))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                TbSectionHeader(
                    title = "Programação / pauta",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TramabookCard {
                    meeting!!.agenda.split("\n").forEachIndexed { index, line ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Terracota,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyLarge.copy(color = Ink)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- SUB-TAB 2: VOTAÇÃO ---
@Composable
fun VotacaoTab(
    viewModel: MainViewModel,
    onNavigateToSuggestBook: () -> Unit
) {
    val suggestedBooks by viewModel.suggestedBooks.collectAsState()
    val votes by viewModel.suggestionsAndVotes.collectAsState()
    val members by viewModel.clubMembers.collectAsState()

    val currentUserId = viewModel.currentUserId.collectAsState().value ?: "user_voce"

    val totalVotes = votes.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TramabookCard {
                Text(
                    text = "Votação do próximo livro",
                    style = MaterialTheme.typography.headlineLarge.copy(color = OlivaDark),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Votem no livro que vocês gostariam de ler a partir da próxima semana. Cada membro tem direito a um voto, mudando quando quiser.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Tertiary
                )
            }
        }

        if (suggestedBooks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhum livro sugerido ainda.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Muted
                    )
                }
            }
        } else {
            items(suggestedBooks) { book ->
                val bookVotes = votes.filter { it.clubBookId == book.id }
                val hasUserVoted = bookVotes.any { it.userId == currentUserId }

                val pct = if (totalVotes > 0) {
                    bookVotes.size.toFloat() / totalVotes.toFloat()
                } else {
                    0f
                }

                TramabookCard(
                    modifier = Modifier.clickable { viewModel.voteForBook(book.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Cover(
                            title = book.title,
                            author = book.author,
                            coverUrl = book.coverUrl,
                            width = 64.dp,
                            height = 96.dp
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontSize = 16.sp,
                                            color = Ink
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = book.author,
                                        style = MaterialTheme.typography.bodyLarge.copy(color = Muted)
                                    )
                                }

                                if (hasUserVoted) {
                                    Pill(
                                        text = "Teu voto",
                                        variant = PillVariant.OliveDeep,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Vote progress bar — Phase-1 ProgressBar component
                            ProgressBar(
                                value = pct,
                                modifier = Modifier.fillMaxWidth(),
                                color = if (hasUserVoted) Oliva else Terracota,
                                track = DividerSoft,
                                height = 8.dp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${bookVotes.size} ${if (bookVotes.size == 1) "voto" else "votos"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Ink
                                    )
                                )
                                Text(
                                    text = "${(pct * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TbButton(
                                text = if (hasUserVoted) "Teu voto" else "Votar nesse",
                                onClick = { viewModel.voteForBook(book.id) },
                                modifier = Modifier.fillMaxWidth(),
                                variant = if (hasUserVoted) TbButtonVariant.OlivaSoft else TbButtonVariant.Primary,
                                size = TbButtonSize.Sm
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            TbButton(
                text = "Sugerir livro",
                onClick = onNavigateToSuggestBook,
                modifier = Modifier.fillMaxWidth(),
                variant = TbButtonVariant.Outline
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- SUB-TAB 3: ESTANTE (Lidos) ---
@Composable
fun EstanteTab(
    viewModel: MainViewModel,
    onNavigateToBookDetail: (String) -> Unit = {}
) {
    val finishedBooks by viewModel.finishedBooks.collectAsState()
    var filterBy by remember { mutableStateOf("Todos") }

    // Dynamic rating: odd index ID get 5 stars (Favoritos), even gets 4
    val getBookRating = { book: Book ->
        if (book.id == "fin_1" || book.id == "fin_3") 5 else 4
    }

    val displayedBooks = if (filterBy == "Favoritos") {
        finishedBooks.filter { getBookRating(it) == 5 }
    } else {
        finishedBooks
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Shelf Filter Pills using Phase-1 Pill component
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Todos", "Favoritos").forEach { option ->
                val isSelected = filterBy == option
                Box(
                    modifier = Modifier.clickable { filterBy = option }
                ) {
                    Pill(
                        text = option,
                        variant = if (isSelected) PillVariant.OliveDeep else PillVariant.Default
                    )
                }
            }
        }

        if (displayedBooks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nenhum livro nesta categoria.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(displayedBooks) { book ->
                    val rating = getBookRating(book)
                    TramabookCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBookDetail(book.id) },
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Cover(
                                title = book.title,
                                author = book.author,
                                coverUrl = book.coverUrl,
                                width = 100.dp,
                                height = 150.dp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 14.sp,
                                    color = Ink
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Muted,
                                    fontSize = 12.sp
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Star ratings — gold literal from prototype, no dedicated token
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                repeat(5) { rateIndex ->
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = "Rating Star",
                                        tint = if (rateIndex < rating) Color(0xFFE6BF6B) else DividerSoft, // estrela — cor do protótipo
                                        modifier = Modifier.size(16.dp)
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
