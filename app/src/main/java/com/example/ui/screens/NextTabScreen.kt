package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    onNavigateToMeetingDetail: (String) -> Unit = {},
    initialSubTab: String? = null,
    onSubTabConsumed: () -> Unit = {},
) {
    var subTab by rememberSaveable { mutableStateOf(initialSubTab ?: "encontro") }
    // Se o caller setar initialSubTab depois da composicao inicial (ex: usuario
    // ja estava em Next > Encontro e clicou num CTA da Home pra Votacao), troca.
    LaunchedEffect(initialSubTab) {
        if (initialSubTab != null && initialSubTab != subTab) {
            subTab = initialSubTab
            onSubTabConsumed()
        }
    }

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
            val nextTabs = listOf("encontro" to "Encontro", "votacao" to "Votação")
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
            val nextTabs = listOf("encontro", "votacao")
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
                "encontro" -> EncontroTab(viewModel = viewModel, onNavigateToMeetingDetail = onNavigateToMeetingDetail)
                "votacao" -> VotacaoTab(viewModel = viewModel, onNavigateToSuggestBook = onNavigateToSuggestBook)
            }
        }
    }
}

// --- SUB-TAB 1: ENCONTRO ---
@Composable
fun EncontroTab(
    viewModel: MainViewModel,
    onNavigateToMeetingDetail: (String) -> Unit = {}
) {
    val meeting by viewModel.latestMeeting.collectAsState()
    val rsvps by viewModel.latestMeetingRsvps.collectAsState()
    val members by viewModel.clubMembers.collectAsState()
    val meetingsForBook by viewModel.meetingsForCurrentBook.collectAsState()
    val currentBookTitle = viewModel.currentBook.collectAsState().value?.title
    val currentUserId = viewModel.currentUserId.collectAsState().value
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()

    var isConfirmadosExpanded by remember { mutableStateOf(true) }
    var isTalvezExpanded by remember { mutableStateOf(false) }
    var isNaoVouExpanded by remember { mutableStateOf(false) }

    // Perf: agrupa/indexa uma vez por emissão em vez de refiltrar/rebuscar por card.
    val rsvpsByStatus = remember(rsvps) { rsvps.groupBy { it.status } }
    val membersById = remember(members) { members.associateBy { it.id } }

    // Gate de loading: hasData = já temos encontro. Enquanto carrega, mostra
    // skeleton; passada a janela sem dado, cai no empty state real.
    val showLoading = com.example.ui.components.rememberShowLoading(hasData = meeting != null)

    if (meeting == null) {
        if (showLoading) {
            // Gate: o flow começa null antes do primeiro sync — mostra skeleton em
            // vez de "nenhum encontro" piscando enquanto os dados carregam.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                com.example.ui.components.SkeletonMeetingCard()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.EventNote,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Nenhum próximo encontro agendado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Sem callback de agendamento no escopo desta tela: mostra só a
                // microcopy por papel (admin vs membro). Ver relatório.
                Text(
                    text = if (isAdmin)
                        "Quando você agendar o próximo encontro, ele aparece aqui."
                    else
                        "Assim que o organizador marcar um encontro, ele aparece aqui.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // A11y + perf: memoiza o flow por id do encontro (sem remember o flow era
        // recriado a cada recomposição).
        val userRsvpFlow = remember(meeting!!.id) { viewModel.getRsvpOfUser(meeting!!.id) }
        val userRsvp by userRsvpFlow.collectAsState(initial = null)
        val userStatus = userRsvp?.status ?: "Sem resposta"

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cronograma do livro atual (se houver múltiplos encontros)
            if (meetingsForBook.size > 1) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Terracota, androidx.compose.foundation.shape.CircleShape))
                        Text(
                            text = "CRONOGRAMA · ${currentBookTitle ?: "—"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Terracota,
                                letterSpacing = 1.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                items(meetingsForBook, key = { it.id }) { m ->
                    val concluded = m.status == "concluido"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (concluded) DividerSoft.copy(alpha = 0.3f) else Cream)
                            .border(0.5.dp, Divider, RoundedCornerShape(12.dp))
                            .clickable { onNavigateToMeetingDetail(m.id) }
                            // A11y: card navega — anuncia como botão com rótulo.
                            .semantics {
                                role = Role.Button
                                contentDescription = "Abrir encontro de ${m.data}"
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (concluded) Muted.copy(alpha = 0.2f)
                                    else Terracota.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val dayNum = m.data
                                .substringAfter(",", "")
                                .trim()
                                .takeWhile { it.isDigit() }
                                .ifEmpty { m.data.trim().takeWhile { it.isDigit() }.ifEmpty { "—" } }
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (concluded) Muted else Terracota,
                                    fontFamily = LiterataFontFamily
                                )
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = m.data,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        color = if (concluded) Muted else Ink
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (concluded) {
                                    Pill(text = "Concluído", variant = PillVariant.Olive)
                                }
                            }
                            if (m.chapterStart != null && m.chapterEnd != null) {
                                Text(
                                    text = "📖 Caps ${m.chapterStart}–${m.chapterEnd}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (concluded) Muted else Terracota,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            item {
                // Meeting header card with olive gradient
                RodapeCard(contentPadding = PaddingValues(0.dp)) {
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
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.DateRange,
                                        contentDescription = null,
                                        tint = Cream.copy(alpha = 0.35f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    val dayNumber = meeting!!.data
                                        .substringAfter(",", "")
                                        .trim()
                                        .takeWhile { it.isDigit() }
                                        .ifEmpty {
                                            meeting!!.data.trim().takeWhile { it.isDigit() }.ifEmpty { "—" }
                                        }
                                    Text(
                                        text = dayNumber,
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontSize = 20.sp,
                                            color = Cream,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(top = 8.dp)
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
                RodapeCard {
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
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(if (isSelected) Ink else Color.Transparent)
                                    // A11y: anuncia "selecionado" e papel de opção única.
                                    .selectable(
                                        selected = isSelected,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.rsvpMeeting(meeting!!.id, statusOption) },
                                    )
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
                RodapeCard {
                    val confirmados = rsvpsByStatus["Vou"] ?: emptyList()
                    val talvez = rsvpsByStatus["Talvez"] ?: emptyList()
                    val naoVou = rsvpsByStatus["Não vou"] ?: emptyList()

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
                                    val userObj = membersById[resp.userId]
                                    val nameVal = if (resp.userId == currentUserId) "Você" else userObj?.nome ?: "Membro"
                                    val urlVal = if (resp.userId == currentUserId) (viewModel.currentUser.value?.avatarUrl ?: "") else userObj?.avatarUrl ?: ""
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
                                    val userObj = membersById[resp.userId]
                                    val nameVal = if (resp.userId == currentUserId) "Você" else userObj?.nome ?: "Membro"
                                    val urlVal = if (resp.userId == currentUserId) (viewModel.currentUser.value?.avatarUrl ?: "") else userObj?.avatarUrl ?: ""
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
                                    val userObj = membersById[resp.userId]
                                    val nameVal = if (resp.userId == currentUserId) "Você" else userObj?.nome ?: "Membro"
                                    val urlVal = if (resp.userId == currentUserId) (viewModel.currentUser.value?.avatarUrl ?: "") else userObj?.avatarUrl ?: ""
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

            // Só renderiza a pauta se houver conteúdo — agenda vazia gerava um
            // bullet "•" solitário ("".split("\n") devolve [""]).
            if (meeting!!.agenda.isNotBlank()) {
                item {
                    TbSectionHeader(
                        title = "Programação / pauta",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    RodapeCard {
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
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- SUB-TAB 2: VOTAÇÃO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotacaoTab(
    viewModel: MainViewModel,
    onNavigateToSuggestBook: () -> Unit
) {
    val suggestedBooks by viewModel.suggestedBooks.collectAsState()
    val nextBooks by viewModel.nextBooks.collectAsState()
    val votes by viewModel.suggestionsAndVotes.collectAsState()
    val members by viewModel.clubMembers.collectAsState()
    val activeRound by viewModel.activeVotingRound.collectAsState()
    val suggestionsByBookId by viewModel.bookSuggestionsByBookId.collectAsState()
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()

    val currentUserId = viewModel.currentUserId.collectAsState().value ?: ""

    // Escopo por RODADA ATIVA: antes usava todos os votos do clube (todas as
    // rodadas), então numa 2ª votação as contagens/percentuais vinham diluídos e
    // o "limite" travava com votos de rodadas antigas (bug B1).
    val roundVotes = remember(votes, activeRound) {
        activeRound?.let { r -> votes.filter { it.votingRoundId == r.id } } ?: emptyList()
    }
    val totalVotes = roundVotes.size

    // Perf: agrupa por livro uma vez por emissão. Voto ÚNICO por usuário por rodada
    // (o servidor tem PK (round,user)) — guardamos o livro em que o usuário votou.
    val votesByBook = remember(roundVotes) { roundVotes.groupBy { it.clubBookId } }
    val userVotedBookId = remember(roundVotes, currentUserId) {
        roundVotes.firstOrNull { it.userId == currentUserId }?.clubBookId
    }

    // Gate de loading: hasData = já há livros sugeridos. Enquanto carrega, mostra
    // skeleton; passada a janela sem dado, cai no empty state real.
    val showBooksLoading = com.example.ui.components.rememberShowLoading(hasData = suggestedBooks.isNotEmpty())

    var showOpenSheet by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var justificationSheetFor by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RodapeCard {
                Text(
                    text = "Votação do próximo livro",
                    style = MaterialTheme.typography.headlineLarge.copy(color = OlivaDark),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (activeRound != null) {
                    val r = activeRound!!
                    val dataLabel = com.example.util.formatShortDate(r.fechaEm)
                    val nLabel = if (r.nLivros == 1) "Escolham o próximo livro" else "Escolham os próximos ${r.nLivros} livros"
                    Text(
                        text = "Aberta até $dataLabel · $nLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Tertiary
                    )
                } else {
                    Text(
                        text = "Não há votação aberta no momento.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Muted
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TbButton(
                            text = "Abrir nova votação",
                            onClick = { showOpenSheet = true },
                            variant = TbButtonVariant.Terra,
                            size = TbButtonSize.Md,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (activeRound != null) {
            if (showBooksLoading) {
                item {
                    com.example.ui.components.SkeletonRowList(count = 3)
                }
            } else if (suggestedBooks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Nenhum livro sugerido ainda.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Muted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TbButton(
                            text = "Sugerir o primeiro livro",
                            onClick = onNavigateToSuggestBook,
                            variant = TbButtonVariant.Primary,
                            size = TbButtonSize.Md
                        )
                    }
                }
            } else {
                items(suggestedBooks, key = { it.id }) { book ->
                    val bookVotes = votesByBook[book.id] ?: emptyList()
                    val hasUserVoted = userVotedBookId == book.id
                    val pct = if (totalVotes > 0) bookVotes.size.toFloat() / totalVotes.toFloat() else 0f
                    val hasJustification = suggestionsByBookId[book.id]?.justificativa?.isNotBlank() == true

                    // Card não é mais clicável pra votar (tocar pra "ver melhor"
                    // registrava/desfazia voto sem querer, ignorando o limite).
                    // O voto acontece só no botão "Votar nesse", que respeita o limite.
                    RodapeCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Cover(
                                title = book.title, author = book.author, coverUrl = book.coverUrl,
                                width = 64.dp, height = 96.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = book.title,
                                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 16.sp, color = Ink),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (hasJustification) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = "Ver justificativa",
                                                    tint = OlivaMid,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable { justificationSheetFor = book.id }
                                                )
                                            }
                                        }
                                        Text(text = book.author, style = MaterialTheme.typography.bodyLarge.copy(color = Muted))
                                    }
                                    if (hasUserVoted) {
                                        Pill(text = "Teu voto", variant = PillVariant.OliveDeep, modifier = Modifier.padding(start = 8.dp))
                                    }
                                    if (isAdmin) {
                                        var showMenu by remember(book.id) { mutableStateOf(false) }
                                        var showConfirm by remember(book.id) { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { showMenu = true },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(Icons.Outlined.MoreVert, contentDescription = "Ações do livro na votação", tint = Muted, modifier = Modifier.size(18.dp))
                                            }
                                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                                DropdownMenuItem(
                                                    text = { Text("Remover sugestão") },
                                                    onClick = {
                                                        showMenu = false
                                                        showConfirm = true
                                                    }
                                                )
                                            }
                                        }
                                        if (showConfirm) {
                                            AlertDialog(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                onDismissRequest = { showConfirm = false },
                                                title = { Text("Remover sugestão?") },
                                                text = { Text("Sugestão e votos serão apagados.") },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        viewModel.removeSuggestion(book.id)
                                                        showConfirm = false
                                                    }) { Text("Remover", color = Terracota, fontWeight = FontWeight.SemiBold) }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showConfirm = false }) {
                                                        Text("Cancelar", color = Muted)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
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
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Ink)
                                    )
                                    Text(
                                        text = "${(pct * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                TbButton(
                                    // Voto único por rodada: tocar no próprio voto desfaz;
                                    // votar em outro troca. Sem "limite atingido" (era o
                                    // bug da 2ª votação travando todos os botões).
                                    text = when {
                                        hasUserVoted -> "Teu voto"
                                        userVotedBookId != null -> "Trocar pra esse"
                                        else -> "Votar nesse"
                                    },
                                    onClick = { viewModel.voteForBook(book.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = if (hasUserVoted) TbButtonVariant.OlivaSoft else TbButtonVariant.Primary,
                                    size = TbButtonSize.Sm,
                                    enabled = true,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fila do clube (status = "next")
        if (nextBooks.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Terracota, androidx.compose.foundation.shape.CircleShape)
                    )
                    Text(
                        text = "FILA DO CLUBE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Terracota
                        )
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(nextBooks, key = { it.id }) { b ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            Cover(title = b.title, author = b.author, coverUrl = b.coverUrl, width = 64.dp, height = 96.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = b.title,
                                style = MaterialTheme.typography.labelSmall.copy(color = Ink),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
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
            if (isAdmin && activeRound != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TbButton(
                    text = "Encerrar votação agora",
                    onClick = { showCloseDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = TbButtonVariant.Outline
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Sheet de justificativa
    if (justificationSheetFor != null) {
        val bookId = justificationSheetFor!!
        val sug = suggestionsByBookId[bookId]
        val author = members.find { it.id == sug?.suggestedByUserId }
        ModalBottomSheet(
            onDismissRequest = { justificationSheetFor = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Avatar(name = author?.nome ?: "Membro", avatarUrl = author?.avatarUrl ?: "", size = 32.dp)
                    Text(
                        text = "${author?.nome ?: "Membro"} sugeriu",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Text(
                    text = "\"${sug?.justificativa ?: ""}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = InkSoft,
                        lineHeight = 20.sp
                    )
                )
                TbButton(
                    text = "Fechar",
                    onClick = { justificationSheetFor = null },
                    variant = TbButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Dialog: encerrar votação
    if (showCloseDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Encerrar votação?") },
            text = {
                Text(
                    "Encerrar agora vai escolher os ${activeRound?.nLivros ?: 1} livro(s) mais votado(s). O atual passa para a estante. Sem volta.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.closeActiveVotingRound()
                    showCloseDialog = false
                }) {
                    Text("Encerrar", color = Terracota, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) {
                    Text("Cancelar", color = Muted)
                }
            }
        )
    }

    // Sheet: abrir votação
    if (showOpenSheet) {
        OpenVotingSheet(
            onDismiss = { showOpenSheet = false },
            onConfirm = { n, dias, cadencia ->
                viewModel.openVotingRound(n, dias, cadencia)
                showOpenSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun OpenVotingSheet(
    onDismiss: () -> Unit,
    onConfirm: (nLivros: Int, durationDays: Int, cadencia: String) -> Unit
) {
    var n by remember { mutableStateOf(1) }
    var dias by remember { mutableStateOf(7) }
    var cadencia by remember { mutableStateOf("unica") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Abrir votação do clube",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 20.sp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quantos livros vamos escolher?", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TbButton(text = "−", onClick = { if (n > 1) n-- }, variant = TbButtonVariant.Outline, size = TbButtonSize.Sm)
                    Text("$n", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp))
                    TbButton(text = "+", onClick = { if (n < 12) n++ }, variant = TbButtonVariant.Outline, size = TbButtonSize.Sm)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Por quanto tempo a votação fica aberta?", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 7, 14, 30).forEach { d ->
                        val selected = dias == d
                        Box(modifier = Modifier.clickable { dias = d }) {
                            Pill(
                                text = "$d dias",
                                variant = if (selected) PillVariant.OliveDeep else PillVariant.Default
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Qual a cadência?", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                // FlowRow pra acomodar 7 opcoes em 2 linhas em phone, 1 linha em tablet.
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val opcoes = listOf(
                        "unica" to "Única",
                        "semanal" to "Semanal",
                        "quinzenal" to "Quinzenal",
                        "mensal" to "Mensal",
                        "trimestral" to "Trimestral",
                        "semestral" to "Semestral",
                        "anual" to "Anual",
                    )
                    opcoes.forEach { (key, label) ->
                        val selected = cadencia == key
                        Box(modifier = Modifier.clickable { cadencia = key }) {
                            Pill(
                                text = label,
                                variant = if (selected) PillVariant.OliveDeep else PillVariant.Default
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TbButton(text = "Cancelar", onClick = onDismiss, variant = TbButtonVariant.Outline, modifier = Modifier.weight(1f))
                TbButton(text = "Abrir votação", onClick = { onConfirm(n, dias, cadencia) }, variant = TbButtonVariant.Terra, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

