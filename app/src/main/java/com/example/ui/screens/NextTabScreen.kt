package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
                    RodapeTheme.colors.olivaSoft.copy(alpha = 0.5f),
                    RoundedCornerShape(RodapeRadii.full)
                )
                .border(
                    0.5.dp,
                    RodapeTheme.colors.divider,
                    RoundedCornerShape(RodapeRadii.full)
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
                            if (isSelected) RodapeTheme.colors.oliva else Color.Transparent,
                            RoundedCornerShape(RodapeRadii.md)
                        )
                        .clickable { subTab = tab },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (isSelected) RodapeTheme.colors.cream else RodapeTheme.colors.tertiary,
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
                            if (subTab == tab) RodapeTheme.colors.terracota else Color.Transparent,
                            RoundedCornerShape(RodapeRadii.full)
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
            // Fade-through na troca de sub-tab (Encontro ↔ Votação) — antes era seco.
            // Specs hoisteados: transitionSpec não é contexto @Composable.
            val subEnterSpec = rodapeTween<Float>(RodapeMotion.Dur.standard)
            val subExitSpec = rodapeTween<Float>(RodapeMotion.Dur.fast)
            AnimatedContent(
                targetState = subTab,
                transitionSpec = {
                    fadeIn(subEnterSpec) togetherWith fadeOut(subExitSpec)
                },
                label = "nextSubTab",
            ) { tab ->
                when (tab) {
                    "encontro" -> EncontroTab(viewModel = viewModel, onNavigateToMeetingDetail = onNavigateToMeetingDetail)
                    "votacao" -> VotacaoTab(viewModel = viewModel, onNavigateToSuggestBook = onNavigateToSuggestBook)
                }
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
    val currentBook by viewModel.currentBook.collectAsState()
    val currentBookTitle = currentBook?.title
    val currentUserId = viewModel.currentUserId.collectAsState().value
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val meetingPattern by viewModel.activeMeetingPattern.collectAsState()
    // Agendamento DIRETO na aba Encontro (antes só existia enterrado em Gerenciar
    // clube → Encontros — ninguém achava).
    var creatingMeeting by remember { mutableStateOf(false) }
    var suggestedRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }

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
                    imageVector = RodapeIcons.Calendar,
                    contentDescription = null,
                    tint = RodapeTheme.colors.muted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Nenhum próximo encontro agendado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = RodapeTheme.colors.muted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Sem callback de agendamento no escopo desta tela: mostra só a
                // microcopy por papel (admin vs membro). Ver relatório.
                Text(
                    text = if (isAdmin)
                        "Marque o próximo encontro do clube — data, hora, local e capítulos."
                    else
                        "Assim que o organizador marcar um encontro, ele aparece aqui.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RodapeTheme.colors.muted,
                    textAlign = TextAlign.Center
                )
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TbButton(
                        text = "Agendar encontro",
                        onClick = { creatingMeeting = true },
                        variant = TbButtonVariant.Terra,
                        size = TbButtonSize.Lg,
                    )
                }
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
                        Box(modifier = Modifier.size(6.dp).background(RodapeTheme.colors.terracota, androidx.compose.foundation.shape.CircleShape))
                        Overline(
                            text = "CRONOGRAMA · ${currentBookTitle ?: "—"}",
                            color = RodapeTheme.colors.terracota,
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
                            .clip(RoundedCornerShape(RodapeRadii.sm))
                            .background(if (concluded) RodapeTheme.colors.dividerSoft.copy(alpha = 0.3f) else RodapeTheme.colors.cream)
                            .border(0.5.dp, RodapeTheme.colors.divider, RoundedCornerShape(RodapeRadii.sm))
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
                                .clip(RoundedCornerShape(RodapeRadii.sm))
                                .background(
                                    if (concluded) RodapeTheme.colors.muted.copy(alpha = 0.2f)
                                    else RodapeTheme.colors.terracota.copy(alpha = 0.12f)
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
                                    color = if (concluded) RodapeTheme.colors.muted else RodapeTheme.colors.terracota,
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
                                        color = if (concluded) RodapeTheme.colors.muted else RodapeTheme.colors.ink
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
                                    text = "Caps ${m.chapterStart}–${m.chapterEnd}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (concluded) RodapeTheme.colors.muted else RodapeTheme.colors.terracota,
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
                // Ticket FÍSICO do próximo encontro (3.1): canhoto oliva com o
                // day-stamp tipografado (weekday + dia serif grande + mês) separado
                // do corpo cream (agenda/local/caps) por linha PICOTADA, com
                // notches laterais ancorados na altura real do picote via
                // onGloballyPositioned (sobrevive a dynamic type) e ticketShadow.
                // O glifo de calendário gigante sangrando sob o número morreu.
                val dateParts = meeting!!.data.split(",")
                val tWeekday = dateParts.firstOrNull()?.trim()?.uppercase()?.take(3) ?: ""
                val tRest = dateParts.getOrNull(1)?.trim() ?: meeting!!.data.trim()
                val tDay = tRest.takeWhile { it.isDigit() }.ifEmpty { "–" }
                val tMonth = tRest.dropWhile { it.isDigit() }.trim().lowercase()
                    .removePrefix("de ").trim().take(3)
                val perforationColor = RodapeTheme.colors.cream
                var perforationY by remember { mutableFloatStateOf(0f) }
                val notchSize = 16.dp
                val notchY = with(LocalDensity.current) { perforationY.toDp() } - (notchSize / 2)

                Box(modifier = Modifier.fillMaxWidth().ticketShadow(cornerRadius = RodapeRadii.md)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RodapeRadii.md))
                            .background(RodapeTheme.colors.cardSurface)
                            .clickable { onNavigateToMeetingDetail(meeting!!.id) }
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "Abrir detalhes do próximo encontro"
                            }
                    ) {
                        // ── Canhoto oliva: day-stamp + hora ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RodapeTheme.colors.olivaDeep)
                                // Âncora dos notches: bottom REAL do canhoto.
                                .onGloballyPositioned { perforationY = it.boundsInParent().bottom }
                                .drawBehind {
                                    drawLine(
                                        color = perforationColor.copy(alpha = 0.35f),
                                        start = Offset(0f, size.height - 0.5.dp.toPx()),
                                        end = Offset(size.width, size.height - 0.5.dp.toPx()),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                                        ),
                                    )
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Day-stamp tipografado (a11y lê a data inteira, não fragmentos)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = "${meeting!!.data}, ${meeting!!.hora}"
                                },
                            ) {
                                Text(
                                    text = tWeekday,
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = RodapeTheme.colors.cream.copy(alpha = 0.65f),
                                    maxLines = 1,
                                )
                                Text(
                                    text = tDay,
                                    fontFamily = LiterataFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 40.sp,
                                    lineHeight = 40.sp,
                                    letterSpacing = (-1).sp,
                                    color = RodapeTheme.colors.cream,
                                )
                                Text(
                                    text = tMonth,
                                    fontFamily = LiterataFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 13.sp,
                                    color = RodapeTheme.colors.cream.copy(alpha = 0.85f),
                                    maxLines = 1,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PRÓXIMO ENCONTRO",
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.6.sp,
                                    color = RodapeTheme.colors.cream.copy(alpha = 0.7f),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = RodapeIcons.Clock,
                                        contentDescription = null,
                                        tint = RodapeTheme.colors.cream.copy(alpha = 0.85f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = meeting!!.hora,
                                        fontFamily = InterFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.cream,
                                    )
                                }
                                if (meeting!!.chapterStart != null && meeting!!.chapterEnd != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Caps ${meeting!!.chapterStart}–${meeting!!.chapterEnd}",
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.olivaSoft,
                                    )
                                }
                            }
                        }

                        // ── Corpo cream: agenda + local ──
                        Column(modifier = Modifier.padding(20.dp)) {
                            if (meeting!!.agenda.isNotBlank()) {
                                Text(
                                    text = meeting!!.agenda,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = RodapeTheme.colors.ink
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = RodapeIcons.Pin,
                                    contentDescription = "Local",
                                    tint = RodapeTheme.colors.olivaMid,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = meeting!!.local.ifBlank { "Local a definir" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = RodapeTheme.colors.tertiary
                                )
                            }
                        }
                    }

                    // Notches "recortados" na altura REAL do picote (cor da
                    // superfície-mãe = fundo da tela, theme-aware). Só desenham
                    // depois da 1ª medição.
                    if (perforationY > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-notchSize / 2), y = notchY)
                                .size(notchSize)
                                .background(RodapeTheme.colors.paper, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (notchSize / 2), y = notchY)
                                .size(notchSize)
                                .background(RodapeTheme.colors.paper, CircleShape)
                        )
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
                            color = RodapeTheme.colors.ink
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val haptics = LocalHapticFeedback.current
                    val rsvpReduceMotion = reduceMotion()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Vou", "Talvez", "Não vou").forEach { statusOption ->
                            val isSelected = userStatus == statusOption

                            // "Carimbo" de RSVP: cores animadas + punch de escala
                            // (afunda 0.92 e volta de mola) + check com scale-in +
                            // háptico. O punch só dispara em MUDANÇA real de seleção
                            // (guard da 1ª composição) — não ao entrar na tela.
                            val pillBg by animateColorAsState(
                                targetValue = if (isSelected) RodapeTheme.colors.ink else RodapeTheme.colors.ink.copy(alpha = 0f),
                                animationSpec = rodapeTween(RodapeMotion.Dur.standard),
                                label = "rsvpBg",
                            )
                            val pillFg by animateColorAsState(
                                targetValue = if (isSelected) RodapeTheme.colors.cream else RodapeTheme.colors.tertiary,
                                animationSpec = rodapeTween(RodapeMotion.Dur.standard),
                                label = "rsvpFg",
                            )
                            val stampScale = remember { Animatable(1f) }
                            var stampArmed by remember { mutableStateOf(false) }
                            LaunchedEffect(isSelected) {
                                if (!stampArmed) { stampArmed = true; return@LaunchedEffect }
                                if (isSelected && !rsvpReduceMotion) {
                                    stampScale.snapTo(0.92f)
                                    stampScale.animateTo(
                                        1f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .graphicsLayer {
                                        scaleX = stampScale.value
                                        scaleY = stampScale.value
                                    }
                                    .clip(RoundedCornerShape(RodapeRadii.full))
                                    .background(pillBg)
                                    // A11y: anuncia "selecionado" e papel de opção única.
                                    .selectable(
                                        selected = isSelected,
                                        role = Role.RadioButton,
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.rsvpMeeting(meeting!!.id, statusOption)
                                        },
                                    )
                                    // C1: a opção escolhida anuncia a mudança (RSVP salvo).
                                    .semantics { if (isSelected) liveRegion = LiveRegionMode.Polite }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else RodapeTheme.colors.divider,
                                        shape = RoundedCornerShape(RodapeRadii.full)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = scaleIn(
                                            rodapeSpring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                        ) + fadeIn(rodapeTween(RodapeMotion.Dur.fast)),
                                    ) {
                                        Icon(
                                            imageVector = RodapeIcons.Check,
                                            contentDescription = null,
                                            tint = pillFg,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Text(
                                        text = statusOption,
                                        color = pillFg,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
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
                                    color = RodapeTheme.colors.oliva,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text("Vou", style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                talvez.size.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    color = RodapeTheme.colors.olivaMid, // substituído de Color(0xFFD4A373)
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text("Talvez", style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                naoVou.size.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    color = RodapeTheme.colors.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text("Não vou", style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = RodapeTheme.colors.divider)
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
                            imageVector = if (isConfirmadosExpanded) RodapeIcons.ChevD else RodapeIcons.ChevR,
                            contentDescription = null,
                            tint = RodapeTheme.colors.olivaMid,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vou (${confirmados.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RodapeTheme.colors.ink
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
                                    color = RodapeTheme.colors.muted
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
                                        Text(text = nameVal, style = MaterialTheme.typography.bodyLarge.copy(color = RodapeTheme.colors.ink))
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
                            imageVector = if (isTalvezExpanded) RodapeIcons.ChevD else RodapeIcons.ChevR,
                            contentDescription = null,
                            tint = RodapeTheme.colors.olivaMid,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Talvez (${talvez.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RodapeTheme.colors.ink
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
                                    color = RodapeTheme.colors.muted
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
                                        Text(text = nameVal, style = MaterialTheme.typography.bodyLarge.copy(color = RodapeTheme.colors.ink))
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
                            imageVector = if (isNaoVouExpanded) RodapeIcons.ChevD else RodapeIcons.ChevR,
                            contentDescription = null,
                            tint = RodapeTheme.colors.olivaMid,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Não vou (${naoVou.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RodapeTheme.colors.ink
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
                                    color = RodapeTheme.colors.muted
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
                                        Text(text = nameVal, style = MaterialTheme.typography.bodyLarge.copy(color = RodapeTheme.colors.ink))
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
                        title = "Pauta",
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
                                        color = RodapeTheme.colors.terracota,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyLarge.copy(color = RodapeTheme.colors.ink)
                                )
                            }
                        }
                    }
                }
            }

            if (isAdmin) {
                item {
                    TbButton(
                        text = "+ Agendar outro encontro",
                        onClick = { creatingMeeting = true },
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Md,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Editor de encontro reaproveitado do Gerenciar clube — agora acessível daqui.
    if (creatingMeeting) {
        LaunchedEffect(Unit) { suggestedRange = viewModel.suggestNextChapterRange() }
        com.example.ui.admin.EditSingleMeetingDialog(
            initialData = "",
            initialHora = meetingPattern?.hora ?: "",
            initialLocal = meetingPattern?.local ?: "",
            initialAgenda = meetingPattern?.agendaTemplate ?: "",
            initialBookId = currentBook?.id,
            initialChapterStart = suggestedRange?.first ?: 1,
            initialChapterEnd = suggestedRange?.second ?: chapters.size.coerceAtLeast(1),
            currentBookId = currentBook?.id,
            currentBookTitle = currentBook?.title,
            totalChapters = chapters.size,
            onDismiss = { creatingMeeting = false },
            onSave = { d, h, l, a, bId, cs, ce ->
                viewModel.upsertMeeting(null, d, h, l, a, bId, cs, ce)
                creatingMeeting = false
            }
        )
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
    // Room-backed reativo, já escopado à rodada ativa: o voto otimista reflete na hora.
    val roundVotes by viewModel.votesForActiveRound.collectAsState()
    val members by viewModel.clubMembers.collectAsState()
    val activeRound by viewModel.activeVotingRound.collectAsState()
    val suggestionsByBookId by viewModel.bookSuggestionsByBookId.collectAsState()
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()

    val currentUserId = viewModel.currentUserId.collectAsState().value ?: ""

    val totalVotes = roundVotes.size

    // Perf: agrupa por livro uma vez por emissão. Voto ÚNICO por usuário por rodada
    // (o servidor tem PK (round,user)) — guardamos o livro em que o usuário votou.
    val votesByBook = remember(roundVotes) { roundVotes.groupBy { it.clubBookId } }
    val userVotedBookId = remember(roundVotes, currentUserId) {
        roundVotes.firstOrNull { it.userId == currentUserId }?.clubBookId
    }

    // Apuração AO VIVO (3.3): lista ordenada por votos; o líder (sem empate)
    // ganha aro dourado + selo. Computado AQUI (contexto composable) — dentro
    // da LazyColumn DSL `remember` não compila.
    val sortedBooks = remember(suggestedBooks, votesByBook) {
        suggestedBooks.sortedByDescending { votesByBook[it.id]?.size ?: 0 }
    }
    val leaderId = remember(sortedBooks, votesByBook, totalVotes) {
        if (totalVotes == 0) null
        else {
            val top = sortedBooks.firstOrNull()?.let { votesByBook[it.id]?.size ?: 0 } ?: 0
            val tied = sortedBooks.count { (votesByBook[it.id]?.size ?: 0) == top }
            if (top > 0 && tied == 1) sortedBooks.first().id else null
        }
    }

    // Gate de loading: hasData = já há livros sugeridos. Enquanto carrega, mostra
    // skeleton; passada a janela sem dado, cai no empty state real.
    val showBooksLoading = com.example.ui.components.rememberShowLoading(hasData = suggestedBooks.isNotEmpty())

    // Quando o empty state já mostra o CTA primário "Sugerir o primeiro livro",
    // não repita o CTA de sugerir no rodapé.
    val showFirstBookCta = activeRound != null && !showBooksLoading && suggestedBooks.isEmpty()

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
                    style = MaterialTheme.typography.headlineLarge.copy(color = RodapeTheme.colors.olivaDark),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (activeRound != null) {
                    val r = activeRound!!
                    val dataLabel = com.example.util.formatShortDate(r.fechaEm)
                    val nLabel = if (r.nLivros == 1) "Escolham o próximo livro" else "Escolham os próximos ${r.nLivros} livros"
                    // Chip de contagem regressiva com dot terracota PULSANDO —
                    // votação aberta é coisa VIVA (reduced-motion → dot parado).
                    val votingReduce = reduceMotion()
                    val dotAlpha = if (votingReduce) 1f else {
                        val pulse = rememberInfiniteTransition(label = "votingDot")
                        pulse.animateFloat(
                            initialValue = 0.35f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(700),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "votingDotAlpha",
                        ).value
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(RodapeRadii.full))
                            .background(RodapeTheme.colors.terracotaSoft.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .graphicsLayer { alpha = dotAlpha }
                                .background(RodapeTheme.colors.terracota, CircleShape)
                        )
                        Text(
                            text = "Aberta até $dataLabel",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = RodapeTheme.colors.terracota,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Manchete da apuração: quantos já votaram do total de membros.
                    Text(
                        text = "$totalVotes ${if (totalVotes == 1) "voto" else "votos"} · ${members.size} ${if (members.size == 1) "membro" else "membros"} · $nLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = RodapeTheme.colors.tertiary
                    )
                } else {
                    Text(
                        text = "Não há votação aberta no momento.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = RodapeTheme.colors.muted
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
                            color = RodapeTheme.colors.muted,
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
                // Cards REORDENAM com animação (animateItem) conforme a apuração
                // muda; sortedBooks/leaderId computados acima (fora da DSL).
                items(sortedBooks, key = { it.id }) { book ->
                    val bookVotes = votesByBook[book.id] ?: emptyList()
                    val hasUserVoted = userVotedBookId == book.id
                    val pct = if (totalVotes > 0) bookVotes.size.toFloat() / totalVotes.toFloat() else 0f
                    val hasJustification = suggestionsByBookId[book.id]?.justificativa?.isNotBlank() == true
                    val isLeader = book.id == leaderId

                    // Card não é mais clicável pra votar (tocar pra "ver melhor"
                    // registrava/desfazia voto sem querer, ignorando o limite).
                    // O voto acontece só no botão "Votar nesse", que respeita o limite.
                    RodapeCard(
                        modifier = Modifier
                            .animateItem()
                            .then(
                                if (isLeader) Modifier.border(
                                    1.5.dp,
                                    RodapeTheme.colors.dourado,
                                    RoundedCornerShape(RodapeRadii.md)
                                ) else Modifier
                            )
                    ) {
                        if (isLeader) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(RodapeRadii.full))
                                    .background(RodapeTheme.colors.dourado.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            ) {
                                Icon(
                                    imageVector = RodapeIcons.Trophy,
                                    contentDescription = null,
                                    tint = RodapeTheme.colors.dourado,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = "Na frente",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = RodapeTheme.colors.dourado,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
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
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 16.sp, color = RodapeTheme.colors.ink),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(text = book.author, style = MaterialTheme.typography.bodyLarge.copy(color = RodapeTheme.colors.muted))
                                        // Affordance unificada com a fila: texto-link
                                        // (o Info icon 16dp tinha target minúsculo e
                                        // linguagem diferente da lista irmã).
                                        if (hasJustification) {
                                            Text(
                                                text = "Ver justificativa",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = RodapeTheme.colors.olivaMid, fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .minimumInteractiveComponentSize()
                                                    .clickable(role = Role.Button) { justificationSheetFor = book.id }
                                            )
                                        }
                                    }
                                    if (hasUserVoted) {
                                        // C1: liveRegion faz o leitor de tela anunciar
                                        // "Seu voto" quando o voto é registrado/trocado.
                                        Pill(
                                            text = "Seu voto",
                                            variant = PillVariant.OliveDeep,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .semantics { liveRegion = LiveRegionMode.Polite },
                                        )
                                    }
                                    if (isAdmin) {
                                        var showMenu by remember(book.id) { mutableStateOf(false) }
                                        var showConfirm by remember(book.id) { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { showMenu = true },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(RodapeIcons.MoreV, contentDescription = "Ações do livro na votação", tint = RodapeTheme.colors.muted, modifier = Modifier.size(18.dp))
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
                                                    }) { Text("Remover", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showConfirm = false }) {
                                                        Text("Cancelar", color = RodapeTheme.colors.muted)
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
                                    color = if (hasUserVoted) RodapeTheme.colors.oliva else RodapeTheme.colors.terracota,
                                    track = RodapeTheme.colors.dividerSoft,
                                    height = 8.dp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${bookVotes.size} ${if (bookVotes.size == 1) "voto" else "votos"}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RodapeTheme.colors.ink)
                                    )
                                    Text(
                                        text = "${(pct * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                TbButton(
                                    // Voto único por rodada: no seu voto o botão vira ação
                                    // de desfazer ("Remover voto"); votar em outro troca. Sem
                                    // "limite atingido" (era o bug da 2ª votação travando tudo).
                                    text = when {
                                        hasUserVoted -> "Remover voto"
                                        userVotedBookId != null -> "Trocar para este"
                                        else -> "Votar neste"
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
        } else if (!showBooksLoading && suggestedBooks.isNotEmpty()) {
            // Sem rodada aberta: as sugestões NÃO somem mais — ficam visíveis como
            // "na fila", pra o membro ver que a sugestão dele pegou (antes a lista
            // só renderizava com rodada aberta e o membro sugeria no vazio → bug).
            item {
                // Desambiguação (3.3): "Sugestões..." = aguardando votação;
                // "PRÓXIMAS LEITURAS" (abaixo) = livros JÁ escolhidos, na ordem.
                Text(
                    text = "Sugestões pra próxima votação (${suggestedBooks.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold, color = RodapeTheme.colors.ink
                    )
                )
            }
            items(suggestedBooks, key = { it.id }) { book ->
                val hasJustification = suggestionsByBookId[book.id]?.justificativa?.isNotBlank() == true
                RodapeCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Cover(
                            title = book.title, author = book.author, coverUrl = book.coverUrl,
                            width = 56.dp, height = 84.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 16.sp, color = RodapeTheme.colors.ink),
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (hasJustification) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ver justificativa",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = RodapeTheme.colors.olivaMid, fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.clickable { justificationSheetFor = book.id }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = if (isAdmin) "Abra a votação pra o clube escolher entre elas."
                           else "Quando um admin abrir a votação, vocês votam entre elas.",
                    style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted),
                    modifier = Modifier.padding(top = 4.dp)
                )
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
                            .background(RodapeTheme.colors.terracota, androidx.compose.foundation.shape.CircleShape)
                    )
                    Overline(text = "PRÓXIMAS LEITURAS", color = RodapeTheme.colors.terracota)
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
                                style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.ink),
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
            if (!showFirstBookCta) {
                TbButton(
                    text = "Sugerir livro",
                    onClick = onNavigateToSuggestBook,
                    modifier = Modifier.fillMaxWidth(),
                    variant = TbButtonVariant.Outline
                )
            }
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
                        color = RodapeTheme.colors.inkSoft,
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
                    Text("Encerrar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) {
                    Text("Cancelar", color = RodapeTheme.colors.muted)
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
                // Stepper com ÍCONES (Minus/Plus) — "−"/"+" texto tinha cara de
                // label, não de controle. Target 48dp via minimumInteractiveComponentSize.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepperIconButton(
                        icon = RodapeIcons.Minus,
                        contentDescription = "Diminuir quantidade",
                        enabled = n > 1,
                        onClick = { if (n > 1) n-- },
                    )
                    Text(
                        "$n",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    StepperIconButton(
                        icon = RodapeIcons.Plus,
                        contentDescription = "Aumentar quantidade",
                        enabled = n < 12,
                        onClick = { if (n < 12) n++ },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Por quanto tempo a votação fica aberta?", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                // PillToggle de verdade: target 48dp + Role/selected pro TalkBack
                // (antes era clickable em Box, target pequeno, sem semântica).
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 7, 14, 30).forEach { d ->
                        PillToggle(
                            text = "$d dias",
                            selected = dias == d,
                            onClick = { dias = d },
                        )
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
                        PillToggle(
                            text = label,
                            selected = cadencia == key,
                            onClick = { cadencia = key },
                        )
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

/** Botão circular de stepper (−/+): ícone, borda divider, target 48dp, estado disabled. */
@Composable
private fun StepperIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .border(
                1.dp,
                if (enabled) RodapeTheme.colors.divider else RodapeTheme.colors.dividerSoft,
                CircleShape
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) RodapeTheme.colors.ink else RodapeTheme.colors.muted.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
    }
}

