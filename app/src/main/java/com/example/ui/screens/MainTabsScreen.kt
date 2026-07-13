package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.ThemeMode
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeTheme
import com.example.util.displayName
import com.example.util.displayFirstName
import com.example.ui.components.*
import com.example.ui.theme.Terracota
import com.example.ui.theme.TerracotaSoft
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.OlivaDeep
import com.example.ui.theme.Oliva
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.OlivaMid
import com.example.ui.theme.Cream
import com.example.ui.theme.CardSoft
import com.example.ui.theme.CardSurface
import com.example.ui.theme.Divider
import com.example.ui.theme.TertiarySoft
import com.example.ui.theme.Ink
import com.example.ui.theme.Muted
import com.example.ui.theme.DividerSoft
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.clubColorFor
import com.example.ui.theme.cardShadow
import com.example.ui.theme.navShadow
import com.example.ui.theme.ticketShadow
import com.example.ui.theme.floatShadow
import com.example.ui.theme.heroCoverShadow
import com.example.ui.theme.pillShadow
import com.example.ui.theme.Paper
import com.example.ui.theme.Dourado
import com.example.ui.theme.RodapeIcons
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.components.Cover
import com.example.ui.components.Pill
import com.example.ui.components.PillVariant
import com.example.ui.components.ProgressBar
import com.example.ui.components.RodapeCard
import com.example.ui.components.TbSectionHeader

import androidx.compose.foundation.lazy.LazyRow
import com.example.ui.theme.InterFontFamily
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(
    viewModel: MainViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDiscussion: (String, String) -> Unit,
    onNavigateToSuggestBook: () -> Unit,
    onNavigateToJoinClub: () -> Unit,
    onNavigateToCreateClub: () -> Unit,
    onLogoutCompleted: () -> Unit,
    onNavigateToBookDetail: (String) -> Unit = {},
    onNavigateToFrases: () -> Unit = {},
    onNavigateToManageClub: () -> Unit = {},
    onNavigateToManageChapters: () -> Unit = {},
    onNavigateToMeetingDetail: (String) -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
) {
    // rememberSaveable: sobrevive a rotacao / mudanca de fonte do sistema
    // (com remember, o processo recompunha e caia pra "home", perdendo a tab atual).
    var selectedTab by rememberSaveable { mutableStateOf("home") }
    // Sub-tab inicial pra abrir a Next tab (encontro ou votacao). null = padrao.
    // Usado por CTAs da Home pra mandar direto pra votacao em vez do default.
    var pendingNextSubTab by rememberSaveable { mutableStateOf<String?>(null) }
    // Back numa tab != home volta pra home em vez de sair do app (as tabs sao
    // estado interno; main_tabs e a raiz da backstack de navegacao).
    BackHandler(enabled = selectedTab != "home") { selectedTab = "home" }
    // Observa pedidos externos de troca de tab (ex: notificações navegando)
    val requestedTab by viewModel.requestedTab.collectAsState()
    LaunchedEffect(requestedTab) {
        requestedTab?.let { tab ->
            selectedTab = tab
            viewModel.consumeRequestedTab()
        }
    }
    var showBottomSheet by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val shouldShowRatePrompt by viewModel.shouldShowRatePrompt.collectAsState()
    // Mostra prompt só 1x por sessão pra não ser invasivo (e o markAppRated já garante 1x permanente)
    var ratePromptShown by remember { mutableStateOf(false) }
    val showRateDialog = shouldShowRatePrompt && !ratePromptShown

    val activeClub by viewModel.activeClub.collectAsState()
    val allClubs by viewModel.allClubs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val supaName by viewModel.supabaseDisplayName.collectAsState()
    val unreadFlow = remember(viewModel.notifications) {
        viewModel.notifications.map { notifs -> notifs.count { !it.lida } }
    }
    val unreadNotificationsCount by unreadFlow.collectAsState(initial = 0)
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()
    // A2: badge na aba "Clube" quando há votação aberta (algo pra você votar).
    val activeVotingRoundForBadge by viewModel.activeVotingRound.collectAsState()

    // Estado vazio: usuario logado mas nao tem clubes. Mostra CTAs em vez das tabs
    // (as tabs todas dependem de um clube ativo — sem clube, todas mostrariam vazio).
    // Gate de loading: no cold start os clubes ainda estao baixando e allClubs
    // emite vazio — sem o gate, quem TEM clube via o empty state piscar.
    if (allClubs.isEmpty()) {
        if (rememberShowLoading(hasData = false)) {
            CenteredLoading()
            return
        }
        val firstName = (supaName ?: currentUser?.nome)?.substringBefore(" ")
        NoClubsEmptyState(
            userFirstName = firstName,
            onCreateClub = onNavigateToCreateClub,
            onJoinClub = onNavigateToJoinClub,
            onSignOut = {
                viewModel.signOutSupabase {
                    onLogoutCompleted()
                }
            },
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = remember(snackbarHostState) {
        { msg ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            }
        }
    }
    val pendingCount by viewModel.pendingMutationsCount.collectAsState()
    // G1: depois de drenar a fila, mostra um "Tudo salvo ✓" transitório em vez de
    // a pill só sumir — pro leigo, a rodinha que some sozinha parecia "travado".
    var showSavedPill by remember { mutableStateOf(false) }
    var prevPendingCount by remember { mutableStateOf(0) }
    LaunchedEffect(pendingCount) {
        if (pendingCount == 0 && prevPendingCount > 0) {
            showSavedPill = true
            kotlinx.coroutines.delay(2200)
            showSavedPill = false
        }
        prevPendingCount = pendingCount
    }
    // Preserva o estado de cada tab (scroll da LazyColumn, etc.) mesmo quando o
    // when(selectedTab) troca o composable ativo — cada tab guarda seu proprio estado.
    val saveableStateHolder = rememberSaveableStateHolder()

    Scaffold(
        topBar = {
            // Header do design (shell.jsx GlobalHeader): avatar · pill de clube · sino.
            GlobalHeader(
                userName = displayName(currentUser?.nome),
                avatarUrl = currentUser?.avatarUrl ?: "",
                clubName = activeClub?.nome ?: "Rodapé",
                clubColor = clubColorFor(activeClub?.cor ?: "0").bg,
                unreadCount = unreadNotificationsCount,
                isAdmin = isAdmin,
                onAvatar = { selectedTab = "profile" },
                onClubTap = { showBottomSheet = true },
                onBell = onNavigateToNotifications,
                onManageClub = onNavigateToManageClub,
            )
        },
        bottomBar = {
            CustomBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                hasOpenVoting = activeVotingRoundForBadge != null,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            saveableStateHolder.SaveableStateProvider(selectedTab) {
            when (selectedTab) {
                "home" -> HomeScreenTab(
                    viewModel = viewModel,
                    onNavigateToDiscussion = onNavigateToDiscussion,
                    onNavigateToTab = { tab, sub ->
                        pendingNextSubTab = sub
                        selectedTab = tab
                    },
                    onShowMessage = showMessage,
                )
                "book" -> BookDetailScreenTab(
                    viewModel = viewModel,
                    onNavigateToDiscussion = onNavigateToDiscussion,
                    onShowMessage = showMessage,
                    onNavigateToSuggestBook = onNavigateToSuggestBook,
                    // "Abrir/Ver votação" tem que cair no sub-tab Votação — antes só
                    // trocava a aba principal e abria no default (Encontro): o botão
                    // prometia votação e entregava agenda.
                    onNavigateToVoting = { pendingNextSubTab = "votacao"; selectedTab = "next" },
                    onNavigateToManageClub = onNavigateToManageClub,
                    onNavigateToManageChapters = onNavigateToManageChapters,
                )
                "next" -> NextTabScreen(
                    viewModel = viewModel,
                    onNavigateToSuggestBook = onNavigateToSuggestBook,
                    onNavigateToMeetingDetail = onNavigateToMeetingDetail,
                    initialSubTab = pendingNextSubTab,
                    onSubTabConsumed = { pendingNextSubTab = null },
                )
                "shelf" -> ShelfTabScreen(
                    viewModel = viewModel,
                    onNavigateToBookDetail = onNavigateToBookDetail,
                    onNavigateToSuggest = onNavigateToSuggestBook
                )
                "profile" -> ProfileScreenTab(
                    viewModel = viewModel,
                    onLogoutCompleted = onLogoutCompleted,
                    onNavigateToTab = { selectedTab = it },
                    onNavigateToJoinClub = onNavigateToJoinClub,
                    onNavigateToCreateClub = onNavigateToCreateClub,
                    onNavigateToFrases = onNavigateToFrases,
                    onNavigateToBookDetail = onNavigateToBookDetail,
                    onNavigateToAbout = onNavigateToAbout
                )
            }
            }

            // Indicador de sync offline: a fila de mutações sempre existiu,
            // mas era invisível — o usuário não sabia se a ação tinha "pegado".
            // C1: liveRegion faz o leitor de tela anunciar "aguardando"/"salvo".
            if (pendingCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(RodapeRadii.full))
                        .background(RodapeTheme.colors.ink.copy(alpha = 0.88f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CircularProgressIndicator(
                        color = RodapeTheme.colors.cream,
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = if (pendingCount == 1) "1 alteração aguardando conexão"
                               else "$pendingCount alterações aguardando conexão",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = RodapeTheme.colors.cream,
                    )
                }
            } else if (showSavedPill) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(RodapeRadii.full))
                        .background(RodapeTheme.colors.oliva)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Tudo salvo ✓",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RodapeTheme.colors.cream,
                    )
                }
            }
        }
    }

    // Prompt automático de avaliar Play Store após engajamento
    if (showRateDialog) {
        AlertDialog(
            onDismissRequest = {
                ratePromptShown = true
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Curtindo o Rodapé? ⭐",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                Text(
                    "Se o app tá ajudando vocês a ler juntos, uma avaliação na Play Store nos ajuda demais a chegar em outros clubes. Leva 30 segundos 💚",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        color = RodapeTheme.colors.ink,
                        lineHeight = 20.sp
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    com.example.util.openPlayStorePage(context)
                    viewModel.markAppRated()
                    ratePromptShown = true
                }) {
                    Text("Avaliar agora", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissRatePromptForever()
                    ratePromptShown = true
                }) {
                    Text("Não, obrigado", color = RodapeTheme.colors.muted)
                }
            }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(RodapeTheme.colors.tertiarySoft)
                        .clip(CircleShape)
                )

                Text(
                    // Com 1 clube só não há "troca" possível — o conteúdo real é
                    // criar/entrar em outro clube.
                    text = if (allClubs.size <= 1) "Seus clubes" else "Trocar de clube",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LiterataFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentBooks by viewModel.currentBooksMap.collectAsState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    allClubs.forEach { club ->
                        val isActive = club.id == activeClub?.id
                        val lastActivity = currentBooks[club.id] ?: "Sem livro atual"
                        val resolvedClubColor = clubColorFor(club.cor)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RodapeRadii.md))
                                .background(if (isActive) RodapeTheme.colors.olivaSoft.copy(alpha = 0.35f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) RodapeTheme.colors.terracota else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(RodapeRadii.md)
                                )
                                .clickable {
                                    viewModel.selectActiveClub(club.id)
                                    showBottomSheet = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(resolvedClubColor.bg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = club.nome.take(1).uppercase(),
                                    color = resolvedClubColor.ink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = club.nome,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = if (lastActivity == "Sem livro atual") "Sem livro atual" else "Lendo: $lastActivity",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            if (isActive) {
                                Surface(
                                    shape = RoundedCornerShape(RodapeRadii.sm),
                                    color = RodapeTheme.colors.olivaSoft,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "atual",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = RodapeTheme.colors.oliva,
                                            fontFamily = InterFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TbButton(
                    text = "+ Criar outro clube",
                    onClick = { showBottomSheet = false; onNavigateToCreateClub() },
                    variant = TbButtonVariant.Terra,
                    size = TbButtonSize.Md,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                TbButton(
                    text = "+ Entrar em outro clube",
                    onClick = { showBottomSheet = false; onNavigateToJoinClub() },
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Md,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                )
            }
        }
    }
}

/**
 * Header assinatura do design (shell.jsx GlobalHeader):
 * avatar 40 · pill branco de clube (círculo colorido + overline CLUBE + nome serif
 * + chevron) · sino circular 40 com badge numérico terracota. Engrenagem de admin
 * ganha o mesmo tratamento circular do sino (não existe no protótipo, é do produto).
 */
@Composable
private fun GlobalHeader(
    userName: String,
    avatarUrl: String,
    clubName: String,
    clubColor: Color,
    unreadCount: Int,
    isAdmin: Boolean,
    onAvatar: () -> Unit,
    onClubTap: () -> Unit,
    onBell: () -> Unit,
    onManageClub: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Avatar(
            name = userName,
            avatarUrl = avatarUrl,
            size = 40.dp,
            modifier = Modifier
                // Alvo de toque de 48dp + rotulo/role pra leitor de tela (era um
                // alvo de 40dp sem descricao).
                .minimumInteractiveComponentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onAvatar,
                )
                .semantics { contentDescription = "Abrir perfil" },
        )

        // Pill de clube
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(RodapeRadii.full))
                .background(RodapeTheme.colors.cardSurface)
                .border(1.dp, RodapeTheme.colors.divider, RoundedCornerShape(RodapeRadii.full))
                .clickable(onClick = onClubTap)
                .padding(start = 8.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(clubColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = clubName.take(1).uppercase(),
                    color = RodapeTheme.colors.cream,
                    fontFamily = LiterataFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CLUBE",
                    fontFamily = InterFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RodapeTheme.colors.muted,
                    letterSpacing = 0.6.sp,
                    lineHeight = 10.sp,
                )
                Text(
                    text = clubName,
                    fontFamily = LiterataFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RodapeTheme.colors.ink,
                    letterSpacing = (-0.2).sp,
                    lineHeight = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = RodapeIcons.ChevD,
                contentDescription = "Trocar de clube",
                tint = RodapeTheme.colors.tertiary,
                modifier = Modifier.size(14.dp),
            )
        }

        if (isAdmin) {
            HeaderCircleButton(onClick = onManageClub, contentDescription = "Gerenciar clube") {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = RodapeTheme.colors.ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Box {
            HeaderCircleButton(
                onClick = onBell,
                contentDescription = if (unreadCount > 0)
                    "Notificações, $unreadCount não lidas" else "Notificações",
            ) {
                Icon(
                    imageVector = RodapeIcons.Bell,
                    contentDescription = null,
                    tint = RodapeTheme.colors.ink,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .border(2.dp, RodapeTheme.colors.paper, CircleShape)
                        .padding(2.dp)
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .background(RodapeTheme.colors.terracota, CircleShape)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                        color = RodapeTheme.colors.cream,
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            // Alvo de toque de 48dp (o círculo visual continua 40dp)
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .background(RodapeTheme.colors.cardSurface)
            .border(1.dp, RodapeTheme.colors.divider, CircleShape)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun CustomBottomBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    hasOpenVoting: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Fade do design: conteúdo some suavemente sob a barra (shell.jsx:108).
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0f to RodapeTheme.colors.paper.copy(alpha = 0f),
                    0.5f to RodapeTheme.colors.paper,
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navShadow(cornerRadius = 32.dp),
            shape = RoundedCornerShape(RodapeRadii.full),
            color = RodapeTheme.colors.olivaDeep,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarItem(
                    label = "Início",
                    icon = RodapeIcons.Home,
                    selected = selectedTab == "home",
                    onClick = { onTabSelected("home") }
                )
                BottomBarItem(
                    label = "Livro",
                    icon = RodapeIcons.Book,
                    selected = selectedTab == "book",
                    onClick = { onTabSelected("book") }
                )
                BottomBarItem(
                    // "Encontros" escondia a votação (que vive aqui dentro). "Clube"
                    // é o hub de coordenação: votar no próximo livro + encontros.
                    label = "Clube",
                    icon = RodapeIcons.Calendar,
                    selected = selectedTab == "next",
                    onClick = { onTabSelected("next") },
                    badge = hasOpenVoting,
                )
                BottomBarItem(
                    label = "Estante",
                    icon = RodapeIcons.Shelf,
                    selected = selectedTab == "shelf",
                    onClick = { onTabSelected("shelf") }
                )
                BottomBarItem(
                    label = "Perfil",
                    icon = RodapeIcons.User,
                    selected = selectedTab == "profile",
                    onClick = { onTabSelected("profile") }
                )
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    badge: Boolean,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            // contentDescription = null: o Text(label) irmão já rotula (o
            // clickable mescla os filhos). Sem isso, o TalkBack lia o rótulo 2x.
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        if (badge) {
            // Anel olivaDeep (cor da barra) separa o dot do fundo — sem ele, na
            // aba "Clube" selecionada (pílula terracota) o dot terracota sumia.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(RodapeTheme.colors.olivaDeep)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(RodapeTheme.colors.terracota),
            )
        }
    }
}

@Composable
fun BottomBarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Boolean = false,
) {
    if (selected) {
        Row(
            modifier = Modifier
                // Alvo de toque de 48dp + anuncia como aba selecionada (Role.Tab)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(RodapeRadii.full))
                .background(RodapeTheme.colors.terracota)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick
                )
                .semantics { this.selected = selected; role = Role.Tab }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TabIconWithBadge(icon = icon, tint = RodapeTheme.colors.cream, badge = badge)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = RodapeTheme.colors.cream,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    } else {
        Column(
            modifier = Modifier
                // Alvo de toque de 48dp + anuncia como aba (nao selecionada)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(RodapeRadii.full))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick
                )
                .semantics { this.selected = selected; role = Role.Tab }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TabIconWithBadge(icon = icon, tint = RodapeTheme.colors.cream.copy(alpha = 0.7f), badge = badge)
            // Rótulo sempre visível: sem ele, 4 das 5 abas viram só ícone e o
            // usuário precisa decorar o que cada uma faz.
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = RodapeTheme.colors.cream.copy(alpha = 0.7f),
                    fontFamily = InterFontFamily,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}

// ── Checklist guiado de clube novo (onboarding do ciclo) ─────────────────────
// Stepper: círculos com conector, ✓ oliva nos passos feitos, passo atual em
// terracota (clicável, com dica + chevron), futuros esmaecidos. Some quando o
// clube já está "rodando" (livro + capítulos + encontro).
@Composable
private fun ClubSetupChecklist(
    isAdmin: Boolean,
    hasBook: Boolean,
    hasChapters: Boolean,
    hasMeeting: Boolean,
    hasOpenRound: Boolean,
    hasSuggestions: Boolean,
    hasUserVoted: Boolean,
    onStepVote: () -> Unit,
    onStepChapters: () -> Unit,
    onStepMeeting: () -> Unit,
    onStepRead: () -> Unit,
) {
    data class SetupStep(val title: String, val hint: String, val done: Boolean, val onClick: () -> Unit)
    // Passos DIFERENTES por papel: o admin conduz o ciclo (abrir → encerrar →
    // capítulos → encontro); o membro só faz o que está ao alcance dele (sugerir
    // → votar → ler). Antes os dois viam os mesmos 3 passos, e o membro ficava
    // com tarefas de admin que não conseguia executar.
    val steps = if (isAdmin) listOf(
        SetupStep(
            "Abrir a votação",
            "O clube sugere e vota no próximo livro",
            hasOpenRound || hasBook, onStepVote
        ),
        SetupStep(
            "Encerrar a votação",
            "Quando todos votarem, encerre pra definir a leitura atual",
            hasBook, onStepVote
        ),
        SetupStep(
            "Cadastrar os capítulos",
            "Busque online ou gere o índice",
            hasChapters, onStepChapters
        ),
        SetupStep(
            "Agendar o primeiro encontro",
            "Marque a data e a faixa de capítulos",
            hasMeeting, onStepMeeting
        ),
    ) else listOf(
        SetupStep(
            "Sugerir um livro",
            "Sugira um título pro clube votar",
            hasSuggestions || hasBook, onStepVote
        ),
        SetupStep(
            "Votar no próximo",
            "Escolha entre as sugestões do clube",
            hasUserVoted || hasBook, onStepVote
        ),
        SetupStep(
            "Ler e marcar progresso",
            "Abra o livro e acompanhe sua leitura",
            false, onStepRead
        ),
    )
    val doneCount = steps.count { it.done }
    val nextIndex = steps.indexOfFirst { !it.done }

    RodapeCard(contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Primeiros passos",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = RodapeTheme.colors.ink
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Pra primeira leitura do clube rolar",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            color = RodapeTheme.colors.muted
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(RodapeTheme.colors.olivaSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$doneCount/${steps.size}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = RodapeTheme.colors.olivaDark
                        )
                    )
                }
            }

            Column {
                steps.forEachIndexed { i, step ->
                    val isNext = i == nextIndex
                    val isLast = i == steps.lastIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RodapeRadii.sm))
                            .then(if (isNext) Modifier.clickable { step.onClick() } else Modifier)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = when {
                                            step.done -> RodapeTheme.colors.oliva
                                            isNext -> RodapeTheme.colors.terracota
                                            else -> RodapeTheme.colors.cream
                                        },
                                        shape = CircleShape
                                    )
                                    .then(
                                        if (!step.done && !isNext) Modifier.border(1.5.dp, RodapeTheme.colors.divider, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (step.done) {
                                    Text(
                                        text = "✓",
                                        color = RodapeTheme.colors.cream,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else {
                                    Text(
                                        text = "${i + 1}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNext) RodapeTheme.colors.cream else RodapeTheme.colors.muted
                                        )
                                    )
                                }
                            }
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(26.dp)
                                        .background(if (step.done) RodapeTheme.colors.oliva else RodapeTheme.colors.divider)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 5.dp, bottom = if (isLast) 0.dp else 12.dp)
                        ) {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (step.done) RodapeTheme.colors.muted else RodapeTheme.colors.ink
                                )
                            )
                            if (isNext) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = step.hint,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = InterFontFamily,
                                        color = RodapeTheme.colors.muted
                                    )
                                )
                            }
                        }

                        if (isNext) {
                            Icon(
                                imageVector = RodapeIcons.ChevR,
                                contentDescription = null,
                                tint = RodapeTheme.colors.terracota,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- HOME SCREEN TAB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenTab(
    viewModel: MainViewModel,
    onNavigateToDiscussion: (String, String) -> Unit,
    // (tab, subTab?) — subTab so faz sentido pra "next" hoje. null pra demais.
    onNavigateToTab: (String, String?) -> Unit,
    onShowMessage: (String) -> Unit = {},
) {
    val activeClub by viewModel.activeClub.collectAsState()
    val currentBook by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val progress by viewModel.userProgress.collectAsState()
    val allProgress by viewModel.allProgressForClub.collectAsState()
    val members by viewModel.clubMembers.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentChapIndex = progress?.currentChapter ?: 0

    val meeting by viewModel.latestMeeting.collectAsState()
    val rsvps by viewModel.latestMeetingRsvps.collectAsState()
    val homeIsAdmin by viewModel.isCurrentUserAdmin.collectAsState()
    // Estado do ciclo pro checklist guiado (A1): votação aberta, sugestões na
    // fila e se este membro já votou na rodada ativa.
    val activeRound by viewModel.activeVotingRound.collectAsState()
    val suggestedForChecklist by viewModel.suggestedBooks.collectAsState()
    val roundVotesForChecklist by viewModel.votesForActiveRound.collectAsState()
    val homeUserId by viewModel.currentUserId.collectAsState()
    val hasUserVotedChecklist = remember(roundVotesForChecklist, homeUserId) {
        homeUserId != null && roundVotesForChecklist.any { it.userId == homeUserId }
    }

    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val salutation = when (hour) {
        in 5..12 -> "Bom dia"
        in 13..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    val nameToGreet = currentUserFirst(viewModel)
    val fullGreeting = "$salutation, $nameToGreet."

    var isRefreshing by remember { mutableStateOf(false) }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.forceRefresh { isRefreshing = false }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome and Headline Block (Image 1 Left Top)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = fullGreeting,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Headline contextual: clube vazio (so eu) -> CTA de convidar.
            // Com membros + sem livro -> "esperando" (status quo).
            // Com membros + livro current -> tom mais ativo "lendo juntos".
            val (lead, accent) = when {
                members.size <= 1 -> "Ainda só você. " to "Bora chamar a galera."
                currentBook == null -> "A galera tá " to "esperando."
                else -> "Lendo " to "juntos."
            }
            // Cores hoistadas: buildAnnotatedString é lambda não-composable.
            val inkColor = RodapeTheme.colors.ink
            val olivaColor = RodapeTheme.colors.oliva
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = inkColor)) { append(lead) }
                    withStyle(SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontFamily = LiterataFontFamily,
                        color = olivaColor
                    )) { append(accent) }
                },
                // Design: 28px serif SemiBold (screens-main.jsx headline)
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                )
            )
        }

        // Checklist guiado de clube novo — some quando o clube já está "rodando"
        // (livro atual + capítulos + encontro agendado).
        val clubRolling = currentBook != null && chapters.isNotEmpty() && meeting != null
        if (activeClub != null && !clubRolling) {
            item {
                ClubSetupChecklist(
                    isAdmin = homeIsAdmin,
                    hasBook = currentBook != null,
                    hasChapters = chapters.isNotEmpty(),
                    hasMeeting = meeting != null,
                    hasOpenRound = activeRound != null,
                    hasSuggestions = suggestedForChecklist.isNotEmpty(),
                    hasUserVoted = hasUserVotedChecklist,
                    onStepVote = { onNavigateToTab("next", "votacao") },
                    onStepChapters = { onNavigateToTab("book", null) },
                    onStepMeeting = { onNavigateToTab("next", "encontro") },
                    onStepRead = { onNavigateToTab("book", null) },
                )
            }
        }

        // Section: Próximo encontro card. Tres estados:
        //  - latestMeeting carregando (clube tem encontros mas ainda nao baixou): skeleton
        //  - latestMeeting null pos-carregar: "Nenhum proximo encontro agendado"
        //  - latestMeeting != null: card real
        // Como nao temos sinal de loading explicito, heuristica: se members ja tem
        // dado mas meeting ainda nao, assumimos ainda carregando — caso comum em cold-start
        // depois que o select de members chegou primeiro.
        item {
            val stillLoadingMeeting = meeting == null && members.isEmpty()
            if (stillLoadingMeeting) {
                com.example.ui.components.SkeletonMeetingCard()
            } else if (meeting == null) {
                val isAdminHome by viewModel.isCurrentUserAdmin.collectAsState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(RodapeRadii.md))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Nenhum próximo encontro agendado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdminHome) {
                        TbButton(
                            text = "Agendar encontro",
                            onClick = { onNavigateToTab("next", "encontro") },
                            variant = TbButtonVariant.Terra,
                            size = TbButtonSize.Md,
                        )
                    }
                }
            } else {
                val confirmedUsers = rsvps.filter { it.status == "Vou" }.mapNotNull { r ->
                    members.find { it.id == r.userId }
                }
                val isParticipating = rsvps.any { it.userId == viewModel.currentUserId.value && it.status == "Vou" }
                MeetingTicket(
                    meeting = meeting!!,
                    confirmedUsers = confirmedUsers,
                    isParticipating = isParticipating,
                    onRsvp = {
                        val nextStatus = if (isParticipating) "Não vou" else "Vou"
                        meeting?.let { m -> viewModel.rsvpMeeting(m.id, nextStatus) }
                        onShowMessage(
                            if (isParticipating) "Presença desmarcada"
                            else "Presença confirmada 🎉"
                        )
                    },
                )
            }
        }

        // Section: Tua Leitura Row Card. So aparece quando ha livro "current".
        // Sem livro, mostra 2 CTAs claros — sugerir o primeiro livro (vai pra
        // Proximo > Votacao, onde a sugestao acontece) e convidar amigos.
        if (currentBook == null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: sugerir primeiro livro com CARA DE BOTAO (Terracota,
                    // ilustracao a esquerda + chevron a direita)
                    RodapeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTab("next", "votacao") },
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(RodapeRadii.sm))
                                    .background(RodapeTheme.colors.terracota.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = RodapeIcons.Book,
                                    contentDescription = null,
                                    tint = RodapeTheme.colors.terracota,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sugerir o primeiro livro",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.ink
                                    )
                                )
                                Text(
                                    // Papel-neutro: "comece a votação" prometia ao membro
                                    // comum um poder de admin (abrir rodada) que ele não tem.
                                    // Sugerir, sim, todos podem — e é onde o card leva.
                                    text = "O clube ainda não tem leitura — sugira um título",
                                    style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
                                )
                            }
                            Icon(
                                imageVector = RodapeIcons.ChevR,
                                contentDescription = null,
                                tint = RodapeTheme.colors.terracota,
                            )
                        }
                    }

                    // Card 2: convidar membros — mostra o codigo do clube + botao
                    // de share. So aparece se ainda nao convidou ninguem (clube
                    // pequeno). Tambem com cara de botao.
                    val codigo = activeClub?.codigo.orEmpty()
                    if (codigo.isNotBlank()) {
                        RodapeCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    com.example.util.shareClubInvite(
                                        context,
                                        activeClub?.nome ?: "Rodapé",
                                        codigo,
                                    )
                                },
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(RodapeRadii.sm))
                                        .background(RodapeTheme.colors.oliva.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = RodapeIcons.Groups,
                                        contentDescription = null,
                                        tint = RodapeTheme.colors.olivaDeep,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Convidar alguém pro clube",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = LiterataFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            color = RodapeTheme.colors.ink
                                        )
                                    )
                                    Text(
                                        text = "Código: $codigo · toque pra compartilhar",
                                        style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
                                    )
                                }
                                Icon(
                                    imageVector = RodapeIcons.Share,
                                    contentDescription = null,
                                    tint = RodapeTheme.colors.olivaDeep,
                                )
                            }
                        }
                    }
                }
            }
        } else item {
            val book = currentBook!!  // smart cast nao funciona em property delegate
            RodapeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab("book", null) },
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Cover(
                        title = book.title,
                        author = book.author,
                        coverUrl = book.coverUrl,
                        width = 48.dp,
                        height = 72.dp
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        val bookTitle = book.title
                        val totalChaps = chapters.size
                        val curChap = currentChapIndex
                        val readingLabel = if (totalChaps > 0) "SUA LEITURA · CAP. $curChap/$totalChaps"
                            else "SUA LEITURA · sem capítulos definidos"

                        Text(
                            text = readingLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = RodapeTheme.colors.terracota,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val visualPct = if (totalChaps > 0) (curChap.toFloat() / totalChaps.toFloat()).coerceIn(0f, 1f) else 0f
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProgressBar(
                                value = visualPct,
                                color = RodapeTheme.colors.terracota,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(visualPct * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = RodapeTheme.colors.muted
                                )
                            )
                        }
                    }

                    Icon(
                        imageVector = RodapeIcons.ChevR,
                        contentDescription = "Ver livro",
                        tint = RodapeTheme.colors.muted.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Section: Onde a galera tá. So faz sentido com livro escolhido
        // (a posicao da galera e medida em capitulos do livro current).
        if (currentBook != null) item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Onde a galera tá",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                )

                val readersCount = members.size
                Text(
                    text = "$readersCount ${if (readersCount == 1) "leitor" else "leitores"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }

        if (currentBook != null) item {
            val currentBookId = currentBook!!.id  // smart cast nao funciona em property delegate
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                val currentUserId = viewModel.currentUserId.value
                val sortedMembers = members.sortedWith(compareBy { it.id != currentUserId })

                // Calcula o mediano dos progressos pra detectar quem está significativamente atrás
                val allChapNums = members.map { m ->
                    allProgress.find { it.userId == m.id && it.bookId == currentBookId }
                        ?.currentChapter
                        ?: 0
                }.sorted()
                val medianChap = if (allChapNums.isNotEmpty()) {
                    allChapNums[allChapNums.size / 2]
                } else 0

                items(sortedMembers) { member ->
                    val memberProg = allProgress.find { it.userId == member.id && it.bookId == currentBookId }
                    val memChap = memberProg?.currentChapter ?: 0
                    val totalChaps = chapters.size

                    val isCurrentUser = member.id == currentUserId
                    val displayName = if (isCurrentUser) {
                        "Você"
                    } else {
                        // Primeiro nome + inicial do sobrenome (ex: "Marina S.")
                        // Cabe no card de 84dp sem espremer o vizinho.
                        val parts = member.nome.trim().split(" ").filter { it.isNotBlank() }
                        when {
                            parts.size >= 2 -> "${parts[0]} ${parts[1].first()}."
                            else -> parts.firstOrNull() ?: member.nome
                        }
                    }

                    val finished = totalChaps > 0 && memChap >= totalChaps
                    // "No seu ritmo" = ≥3 caps ATRÁS do mediano da galera (tom acolhedor);
                    // "Na frente" = ≥3 caps adiante. Anéis semânticos do design (ReaderChip):
                    // oliva sólido = terminou, tracejado = no seu ritmo, ink = na frente.
                    val noSeuRitmo = !finished && totalChaps > 0 && medianChap - memChap >= 3
                    val ahead = !finished && totalChaps > 0 && memChap - medianChap >= 3
                    // Mesmos termos do pill visível (TalkBack lia "adiantado" enquanto
                    // a tela mostrava "Na frente" — divergência confusa).
                    val statusText = when {
                        finished -> "terminou o livro"
                        noSeuRitmo -> "no seu ritmo"
                        ahead -> "na frente"
                        else -> "no capítulo $memChap"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // A11y: TalkBack lê o chip inteiro ("Marina, no capítulo 5")
                        // em vez de avatar + pill + nome como 3 fragmentos soltos.
                        modifier = Modifier
                            .width(84.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "$displayName, $statusText"
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            val ringColor = when {
                                finished -> RodapeTheme.colors.oliva
                                noSeuRitmo -> RodapeTheme.colors.muted
                                ahead -> RodapeTheme.colors.ink
                                isCurrentUser -> RodapeTheme.colors.terracota
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .drawBehind {
                                        if (ringColor != Color.Transparent) {
                                            drawCircle(
                                                color = ringColor,
                                                radius = size.minDimension / 2 - 0.75.dp.toPx(),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = 1.5.dp.toPx(),
                                                    pathEffect = if (noSeuRitmo) {
                                                        PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                                    } else null,
                                                ),
                                            )
                                        }
                                    }
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Avatar(
                                    name = member.nome,
                                    avatarUrl = member.avatarUrl ?: "",
                                    size = 66.dp,
                                )
                            }

                            // Pill de status sobrepondo a base do avatar, com sombra suave
                            Box(modifier = Modifier.offset(y = 10.dp).pillShadow()) {
                                Pill(
                                    text = when {
                                        finished -> "Terminou"
                                        noSeuRitmo -> "No seu ritmo"
                                        ahead -> "Na frente"
                                        else -> "Cap. $memChap"
                                    },
                                    variant = when {
                                        finished -> PillVariant.OliveDeep
                                        else -> PillVariant.Default
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = RodapeTheme.colors.inkSoft
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // About the club — so aparece se ha descricao real preenchida.
        // Card vazio com lorem-ipsum fake confunde mais do que ajuda.
        val descricao = activeClub?.descricao?.trim().orEmpty()
        if (descricao.isNotBlank()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = RodapeIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SOBRE O CLUBE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            item {
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = descricao,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RodapeTheme.colors.muted
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }
}

// Private helper to gracefully get the welcome first name or nickname
@Composable
private fun currentUserFirst(viewModel: MainViewModel): String {
    // Prefere o nome do Room (User.nome), que ATUALIZA depois de editar o perfil;
    // supaName (JWT auth.user_metadata.full_name) e imutavel, entao vira so
    // fallback enquanto o Room ainda nao carregou. "Leitor(a)" e ultimo recurso.
    val supaName by viewModel.supabaseDisplayName.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    return displayFirstName(currentUser?.nome, supaName)
}

// --- BOOK DETAIL SCREEN TAB ---
@Composable
fun BookDetailScreenTab(
    viewModel: MainViewModel,
    onNavigateToDiscussion: (String, String) -> Unit,
    onShowMessage: (String) -> Unit = {},
    onNavigateToSuggestBook: () -> Unit = {},
    onNavigateToVoting: () -> Unit = {},
    onNavigateToManageClub: () -> Unit = {},
    onNavigateToManageChapters: () -> Unit = {},
) {
    val currentBook by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val progress by viewModel.userProgress.collectAsState()
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()

    val currentChapIndex = progress?.currentChapter ?: 0

    if (currentBook == null) {
        // Distingue loading (cold start, ainda sincronizando) de vazio real.
        val showLoading = rememberShowLoading(hasData = currentBook != null)
        if (showLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SkeletonReadingCard()
                SkeletonRowList(count = 3)
            }
        } else {
            // Aba "Livro" sem livro: antes era um texto morto sem ação. Agora
            // orienta e dá CTA por papel (admin escolhe/abre votação; membro
            // sugere/vê votação).
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.weight(1f))
                Icon(
                    RodapeIcons.Book,
                    contentDescription = null,
                    tint = RodapeTheme.colors.muted,
                    modifier = Modifier.size(52.dp),
                )
                Text(
                    text = "Nenhuma leitura em andamento",
                    style = MaterialTheme.typography.titleLarge,
                    color = RodapeTheme.colors.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (isAdmin) {
                        "O clube ainda não escolheu um livro. Abra uma votação ou defina a leitura atual."
                    } else {
                        "O clube ainda não escolheu um livro. Sugira um título ou acompanhe a votação."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = RodapeTheme.colors.muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                if (isAdmin) {
                    TbButton(
                        text = "Gerenciar clube",
                        onClick = onNavigateToManageClub,
                        variant = TbButtonVariant.Primary,
                    )
                    TbButton(
                        text = "Abrir votação",
                        onClick = onNavigateToVoting,
                        variant = TbButtonVariant.OlivaSoft,
                    )
                } else {
                    TbButton(
                        text = "Sugerir um livro",
                        onClick = onNavigateToSuggestBook,
                        variant = TbButtonVariant.Primary,
                    )
                    TbButton(
                        text = "Ver votação",
                        onClick = onNavigateToVoting,
                        variant = TbButtonVariant.OlivaSoft,
                    )
                }
                Spacer(Modifier.weight(1.4f))
            }
        }
    } else {
        val totalChapters = chapters.size
        val pct = if (totalChapters > 0) {
            (currentChapIndex.toFloat() / totalChapters.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val pctInt = (pct * 100).toInt()

        // D1: pular pro capítulo lido (em vez de tocar "Marcar progresso" N vezes
        // pra quem leu em bloco ou voltou atrasado).
        var showJumpDialog by remember { mutableStateOf(false) }
        if (showJumpDialog && chapters.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showJumpDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        "Li até o capítulo…",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(chapters, key = { it.id }) { ch ->
                            val isCurrent = ch.numero == currentChapIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(RodapeRadii.sm))
                                    .clickable {
                                        viewModel.updateBookProgress(currentBook!!.id, ch.numero)
                                        onShowMessage(
                                            if (ch.numero == chapters.size) "Livro terminado! 🎉"
                                            else "Progresso salvo — Cap. ${ch.numero}"
                                        )
                                        showJumpDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "${ch.numero}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) RodapeTheme.colors.oliva else RodapeTheme.colors.terracota,
                                    ),
                                    modifier = Modifier.width(28.dp),
                                )
                                Text(
                                    text = ch.titulo.ifBlank { "Capítulo ${ch.numero}" },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.ink),
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "atual",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = RodapeTheme.colors.oliva,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showJumpDialog = false }) {
                        Text("Fechar", color = RodapeTheme.colors.muted)
                    }
                },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── OLIVE HERO ──────────────────────────────────────────────────
            item {
                val heroCircleColor = RodapeTheme.colors.cream
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                        .background(RodapeTheme.colors.olivaDeep)
                        // Círculos decorativos do design (screens-main.jsx:362-367)
                        .drawBehind {
                            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            drawCircle(
                                color = heroCircleColor.copy(alpha = 0.08f),
                                radius = 130.dp.toPx(),
                                center = Offset(size.width * 0.92f, -30.dp.toPx()),
                                style = stroke,
                            )
                            drawCircle(
                                color = heroCircleColor.copy(alpha = 0.08f),
                                radius = 90.dp.toPx(),
                                center = Offset(-10.dp.toPx(), size.height * 0.85f),
                                style = stroke,
                            )
                        }
                        .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 80.dp)
                ) {
                    // Label row
                    Text(
                        text = "Livro atual",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = RodapeTheme.colors.cream.copy(alpha = 0.70f)
                        ),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Cover + info row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.heroCoverShadow(cornerRadius = 4.dp)) {
                            Cover(
                                title = currentBook!!.title,
                                author = currentBook!!.author,
                                coverUrl = currentBook?.coverUrl ?: "",
                                width = 108.dp,
                                height = 162.dp
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentBook!!.title,
                                // Design: serif 26px no hero
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RodapeTheme.colors.cream,
                                    lineHeight = 31.sp
                                ),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentBook!!.author,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = RodapeTheme.colors.cream.copy(alpha = 0.70f)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = RodapeIcons.StarFill,
                                    contentDescription = null,
                                    tint = RodapeTheme.colors.dourado,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (totalChapters > 0) "$totalChapters capítulos" else "Capítulos a definir",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = RodapeTheme.colors.cream.copy(alpha = 0.80f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ── PROGRESS CARD (overlaps hero curve) ─────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-54).dp)
                        .padding(horizontal = 22.dp)
                ) {
                    RodapeCard(
                        modifier = Modifier.fillMaxWidth().floatShadow(cornerRadius = 20.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        // Reading info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Sua leitura",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                        color = RodapeTheme.colors.muted
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Cores hoistadas: buildAnnotatedString é lambda não-composable.
                                val chapInkColor = RodapeTheme.colors.ink
                                val chapMutedColor = RodapeTheme.colors.muted
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = chapInkColor)) {
                                            append("Cap. $currentChapIndex")
                                        }
                                        withStyle(SpanStyle(color = chapMutedColor, fontWeight = FontWeight.Normal)) {
                                            append(" de $totalChapters")
                                        }
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            // Circular ring progress indicator
                            val ringTrackColor = RodapeTheme.colors.dividerSoft
                            val ringProgressColor = RodapeTheme.colors.terracota
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .drawBehind {
                                        val stroke = 5.dp.toPx()
                                        val radius = (size.minDimension - stroke) / 2f
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        // track arc
                                        drawArc(
                                            color = ringTrackColor,
                                            startAngle = -90f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = stroke
                                            )
                                        )
                                        // progress arc
                                        drawArc(
                                            color = ringProgressColor,
                                            startAngle = -90f,
                                            sweepAngle = 360f * pct,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = stroke
                                            )
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$pctInt%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.ink
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TbButton(
                            text = "Marcar progresso",
                            onClick = {
                                val nextChap = currentChapIndex + 1
                                when {
                                    // Antes o botão ficava MUDO quando o livro não tinha
                                    // capítulos (nextChap=1 > chapters.size=0) — parecia
                                    // "não faz nada". Agora avisa o que fazer.
                                    chapters.isEmpty() ->
                                        onShowMessage("Cadastre os capítulos do livro pra acompanhar a leitura.")
                                    nextChap <= chapters.size -> {
                                        viewModel.updateBookProgress(currentBook!!.id, nextChap)
                                        onShowMessage(
                                            if (nextChap == chapters.size) "Livro terminado! 🎉"
                                            else "Progresso salvo — Cap. $nextChap"
                                        )
                                    }
                                    else -> onShowMessage("Você já terminou o livro! 🎉")
                                }
                            },
                            variant = TbButtonVariant.Terra,
                            size = TbButtonSize.Lg,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // D1: pular pro capítulo certo (quem leu vários de uma vez).
                        if (chapters.size > 1) {
                            Text(
                                text = "Li vários — escolher o capítulo",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = RodapeTheme.colors.terracota,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(RodapeRadii.sm))
                                    .clickable { showJumpDialog = true }
                                    .padding(vertical = 4.dp)
                            )
                        }

                        // Recuperação de toque errado: sem isso não há como voltar atrás
                        if (currentChapIndex > 0) {
                            Text(
                                text = "Marquei sem querer — voltar um capítulo",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = RodapeTheme.colors.muted,
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(RodapeRadii.sm))
                                    .clickable {
                                        val prev = currentChapIndex - 1
                                        viewModel.updateBookProgress(currentBook!!.id, prev)
                                        onShowMessage("Voltamos pro Cap. $prev")
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── CHAPTER LIST ────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-38).dp)
                        .padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Capítulos",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = RodapeTheme.colors.ink
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val readCount = chapters.count { it.numero < currentChapIndex }
                            Text(
                                text = "$readCount lidos",
                                style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
                            )
                            // Admin edita o índice direto daqui (sem ir em Gerenciar clube).
                            if (isAdmin && chapters.isNotEmpty()) {
                                Text(
                                    text = "Editar",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.terracota
                                    ),
                                    modifier = Modifier.clickable { onNavigateToManageChapters() }
                                )
                            }
                        }
                    }

                    if (chapters.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RodapeRadii.md),
                            color = RodapeTheme.colors.dividerSoft.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "📚",
                                    fontSize = 32.sp
                                )
                                Text(
                                    text = "Capítulos ainda não cadastrados",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.ink
                                    )
                                )
                                Text(
                                    text = if (isAdmin) {
                                        "Cadastre os capítulos pra liberar a discussão por capítulo."
                                    } else {
                                        "Peça pro admin abrir Gerenciar clube → Capítulos."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted),
                                    textAlign = TextAlign.Center
                                )
                                if (isAdmin) {
                                    Spacer(Modifier.height(4.dp))
                                    // Atalho DIRETO pro índice de capítulos (antes ia
                                    // pro "Gerenciar clube" e o admin tinha que caçar
                                    // "Capítulos" lá dentro — 2 passos e escondido).
                                    TbButton(
                                        text = "Gerenciar capítulos",
                                        onClick = onNavigateToManageChapters,
                                        variant = TbButtonVariant.Primary,
                                        size = TbButtonSize.Sm,
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chapters.forEach { chapter ->
                            val chapNumber = chapter.numero
                            // No progresso 0, o cap. 1 e o alvo atual (destravado): um
                            // membro recem-entrado precisa abrir a discussao do cap. 1.
                            val effectiveChap = maxOf(currentChapIndex, 1)
                            val isCompleted = chapNumber < effectiveChap
                            val isCurrent = chapNumber == effectiveChap
                            val isLocked = chapNumber > effectiveChap

                            val commentsFlow = remember(chapter.id) {
                                viewModel.getCommentsForChapter(chapter.id)
                            }
                            val chapterComments by commentsFlow.collectAsState(initial = emptyList())
                            val commentsCount = chapterComments.size

                            // Chapter row card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Capítulo à frente NÃO é mais bloqueado: abrir cai na
                                    // barreira de spoiler (revelável), em vez de exigir
                                    // "Marcar progresso" +1 N vezes pra destravar o cap N.
                                    .clickable {
                                        onNavigateToDiscussion(chapter.id, chapter.titulo)
                                    },
                                shape = RoundedCornerShape(RodapeRadii.md),
                                color = if (isCurrent) RodapeTheme.colors.cream else RodapeTheme.colors.cardSurface,
                                border = BorderStroke(
                                    width = if (isCurrent) 1.5.dp else 0.5.dp,
                                    color = if (isCurrent) RodapeTheme.colors.terracota else RodapeTheme.colors.divider
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Status indicator circle
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = when {
                                                    isLocked -> RodapeTheme.colors.dividerSoft
                                                    isCurrent -> RodapeTheme.colors.terracota
                                                    else -> RodapeTheme.colors.olivaSoft
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isLocked -> Icon(
                                                imageVector = RodapeIcons.Lock,
                                                contentDescription = "Bloqueado",
                                                tint = RodapeTheme.colors.muted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            isCurrent -> Icon(
                                                imageVector = RodapeIcons.Book,
                                                contentDescription = "Capítulo atual",
                                                tint = RodapeTheme.colors.cream,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            else -> Icon(
                                                imageVector = RodapeIcons.Check,
                                                contentDescription = "Concluído",
                                                tint = RodapeTheme.colors.olivaDeep,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Chapter info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Cap. $chapNumber".uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    letterSpacing = 0.4.sp,
                                                    color = RodapeTheme.colors.muted
                                                )
                                            )
                                            if (isCurrent) {
                                                Pill(
                                                    text = "Atual",
                                                    variant = PillVariant.Terra
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isLocked) "Chega aqui pra liberar" else chapter.titulo,
                                            // Design: título do capítulo em serif 15px
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = LiterataFontFamily,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isLocked) RodapeTheme.colors.muted else RodapeTheme.colors.ink
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Right side info
                                    if (!isLocked) {
                                        Text(
                                            text = if (commentsCount == 1) "1 comentário"
                                                   else if (commentsCount > 1) "$commentsCount comentários"
                                                   else "—",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = RodapeTheme.colors.muted,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card "Estatísticas" removido: era 100% mock ("1h 45m", "25 pág/h")
            // e não existe no design. Volta quando houver tracking real de leitura.
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// --- PROFILE TAB ---
@Composable
fun ProfileScreenTab(
    viewModel: MainViewModel,
    onLogoutCompleted: () -> Unit,
    onNavigateToTab: (String) -> Unit,
    onNavigateToJoinClub: () -> Unit,
    onNavigateToCreateClub: () -> Unit,
    onNavigateToFrases: () -> Unit,
    onNavigateToBookDetail: (String) -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    val supaName by viewModel.supabaseDisplayName.collectAsState()
    val supaEmail by viewModel.supabaseEmail.collectAsState()
    val nameLegacy by viewModel.userName.collectAsState()
    val emailLegacy by viewModel.userEmail.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    // Nome exibido vem do Room (User.nome), que ATUALIZA depois de editar o perfil;
    // supaName (JWT imutavel) e nameLegacy sao apenas fallback.
    val name = currentUser?.nome ?: supaName ?: nameLegacy
    val email = supaEmail ?: emailLegacy
    val allClubs by viewModel.allClubs.collectAsState()
    val activeClub by viewModel.activeClub.collectAsState()
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val finishedBooks by viewModel.finishedBooks.collectAsState()
    val archivedClubs by viewModel.archivedClubsForUser.collectAsState()
    val ratedApp by viewModel.ratedApp.collectAsState()
    val profileCurrentBooks by viewModel.currentBooksMap.collectAsState()
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    // rememberSaveable: a edicao de perfil sobrevive a rotacao / mudanca de fonte.
    var isEditingProfile by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showLeaveClubDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var leaveClubError by remember { mutableStateOf<String?>(null) }

    // Editando o perfil, Back cancela a edicao em vez de sair do app (e perder
    // o que foi digitado).
    BackHandler(enabled = isEditingProfile) { isEditingProfile = false }

    if (isEditingProfile) {
        EditProfileView(
            initialName = name ?: "",
            initialEmail = email ?: "",
            initialAvatarUrl = currentUser?.avatarUrl ?: "",
            initialPronome = currentUser?.pronome ?: "",
            onSave = { newName, newEmail, newAvatar, newPronome ->
                viewModel.updateUserProfile(newName, newEmail, newAvatar, newPronome)
                isEditingProfile = false
            },
            onCancel = {
                isEditingProfile = false
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Profile header ──────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Avatar(
                            name = displayName(name),
                            avatarUrl = currentUser?.avatarUrl ?: "",
                            size = 72.dp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = name ?: "Usuário",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RodapeTheme.colors.ink
                                )
                            )
                            if (!email.isNullOrBlank()) {
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyLarge.copy(color = RodapeTheme.colors.muted)
                                )
                            }
                        }
                    }
                    IconButton(onClick = { isEditingProfile = true }) {
                        Icon(
                            imageVector = RodapeIcons.Edit,
                            contentDescription = "Editar Perfil",
                            tint = RodapeTheme.colors.terracota
                        )
                    }
                }
            }

            // ── Stat Cards (3 side-by-side) ─────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: Livros lidos (do clube ativo). Quando futuramente o
                    // app tiver agregacao cross-clube, somar de todos os clubes.
                    RodapeCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = "${finishedBooks.size}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = LiterataFontFamily,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RodapeTheme.colors.ink
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "lidos\nno clube",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = InterFontFamily,
                                color = RodapeTheme.colors.muted
                            )
                        )
                    }

                    // Card 2: Clubes ativos
                    RodapeCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = "${allClubs.size}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = LiterataFontFamily,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RodapeTheme.colors.ink
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "clubes\nativos",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = InterFontFamily,
                                color = RodapeTheme.colors.muted
                            )
                        )
                    }

                    // Card 3: Frases guardadas (Oliva background, Cream text).
                    // Sombra tingida (cardShadow) + elevação Material 0 — antes usava
                    // cardElevation(2dp), o cinza Material que o design proíbe.
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .cardShadow(cornerRadius = 20.dp)
                            .clickable { onNavigateToFrases() },
                        shape = RoundedCornerShape(RodapeRadii.md),
                        colors = CardDefaults.cardColors(containerColor = RodapeTheme.colors.oliva, contentColor = RodapeTheme.colors.cream),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = savedQuotes.size.toString(),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.cream
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "frases\nguardadas",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = InterFontFamily,
                                        color = RodapeTheme.colors.cream.copy(alpha = 0.85f)
                                    )
                                )
                            }
                            // Chevron: sinaliza que este é o único stat card navegável
                            // (os outros dois são só números, não clicáveis).
                            Icon(
                                imageVector = RodapeIcons.ChevR,
                                contentDescription = null,
                                tint = RodapeTheme.colors.cream.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Meus livros favoritos ───────────────────────────────────
            item {
                TbSectionHeader(title = "Meus livros favoritos")
                Spacer(modifier = Modifier.height(12.dp))
                if (favoriteBooks.isEmpty()) {
                    Text(
                        text = "Toque no ♥ na página de um livro pra guardá-lo aqui.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            color = RodapeTheme.colors.muted
                        )
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(favoriteBooks, key = { it.id }) { fav ->
                            Column(
                                modifier = Modifier
                                    .width(92.dp)
                                    .clickable { viewModel.openFavoriteBook(fav.id, onNavigateToBookDetail) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Cover(
                                    title = fav.title,
                                    author = fav.author,
                                    coverUrl = fav.coverUrl,
                                    width = 92.dp,
                                    height = 138.dp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = fav.title,
                                    style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.ink, fontSize = 11.sp),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // ── Seus Clubes ─────────────────────────────────────────────
            item {
                TbSectionHeader(title = "Seus clubes")
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allClubs.forEach { club ->
                        val isActive = club.id == activeClub?.id
                        val clubColor = clubColorFor(club.cor)
                        // Livro atual REAL do clube (antes era hardcoded por ID mágico
                        // "club_rodape"/"club_filosofia" — clubes reais mostravam sempre
                        // "Sem livro atual"). Usa a mesma fonte do seletor de clube.
                        val livro = profileCurrentBooks[club.id]
                        val clubReadingText = if (livro.isNullOrBlank()) "Sem livro atual" else "Lendo: $livro"

                        Card(
                            onClick = {
                                viewModel.selectActiveClub(club.id)
                                onNavigateToTab("home")
                            },
                            shape = RoundedCornerShape(RodapeRadii.md),
                            colors = CardDefaults.cardColors(containerColor = RodapeTheme.colors.cream, contentColor = RodapeTheme.colors.ink),
                            border = BorderStroke(
                                1.dp,
                                if (isActive) RodapeTheme.colors.oliva else RodapeTheme.colors.divider
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(clubColor.bg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = club.nome.take(1).uppercase(),
                                            color = clubColor.ink,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = LiterataFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 18.sp
                                            )
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = club.nome,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily = LiterataFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 15.sp,
                                                    color = RodapeTheme.colors.ink
                                                )
                                            )
                                            if (isActive) {
                                                Pill(text = "Atual", variant = PillVariant.Terra)
                                            }
                                        }
                                        Text(
                                            text = clubReadingText,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = InterFontFamily,
                                                color = RodapeTheme.colors.muted
                                            )
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = RodapeIcons.ChevR,
                                    contentDescription = "Selecionar",
                                    tint = RodapeTheme.colors.muted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // "+ Criar outro clube" — destaque terracota (acao primaria)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RodapeRadii.md))
                            .background(RodapeTheme.colors.terracota)
                            .clickable { onNavigateToCreateClub() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = RodapeIcons.Plus,
                                contentDescription = null,
                                tint = RodapeTheme.colors.cream,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Criar outro clube",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RodapeTheme.colors.cream
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // "+ Entrar em outro clube" — outline secundario
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RodapeRadii.md))
                            .border(
                                BorderStroke(1.5.dp, RodapeTheme.colors.divider),
                                shape = RoundedCornerShape(RodapeRadii.md)
                            )
                            .clickable { onNavigateToJoinClub() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = RodapeIcons.Plus,
                                contentDescription = null,
                                tint = RodapeTheme.colors.muted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Entrar em outro clube",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RodapeTheme.colors.muted
                                )
                            )
                        }
                    }
                }
            }

            // ── Clubes arquivados (só aparece se houver) ─────────────────
            if (archivedClubs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Overline(
                        text = "ARQUIVADOS",
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        archivedClubs.forEach { club ->
                            val resolvedClubColor = clubColorFor(club.cor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(RodapeRadii.md))
                                    .border(0.5.dp, RodapeTheme.colors.divider, RoundedCornerShape(RodapeRadii.md))
                                    .clickable { viewModel.unarchiveClub(club.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(resolvedClubColor.bg.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = club.nome.take(1).uppercase(),
                                        color = resolvedClubColor.ink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = club.nome,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = LiterataFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            color = RodapeTheme.colors.ink
                                        )
                                    )
                                    Text(
                                        text = "Arquivado · toque para reativar",
                                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                                    )
                                }
                                Icon(
                                    imageVector = RodapeIcons.ChevR,
                                    contentDescription = "Reativar",
                                    tint = RodapeTheme.colors.muted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Aparência (tamanho de fonte) ─────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Overline(
                    text = "APARÊNCIA",
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                val fontScale by viewModel.fontScale.collectAsState()
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tamanho da letra",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = RodapeTheme.colors.ink
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ajuste pra ler melhor. Vale pro app todo.",
                        style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            0.9f to "A−",
                            1.0f to "A",
                            1.15f to "A+",
                            1.30f to "A++"
                        )
                        options.forEach { (scale, label) ->
                            val selected = kotlin.math.abs(fontScale - scale) < 0.05f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(RodapeRadii.sm))
                                    .background(if (selected) RodapeTheme.colors.terracota else RodapeTheme.colors.cream)
                                    .border(
                                        1.dp,
                                        if (selected) RodapeTheme.colors.terracota else RodapeTheme.colors.divider,
                                        RoundedCornerShape(RodapeRadii.sm)
                                    )
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.setFontScale(scale) },
                                    )
                                    .semantics { contentDescription = "Tamanho de fonte $label" }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) RodapeTheme.colors.cream else RodapeTheme.colors.ink
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ── Aparência (tema claro/escuro) ────────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))
                val themeMode by viewModel.themeMode.collectAsState()
                RodapeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tema",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = RodapeTheme.colors.ink
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Claro, escuro ou seguindo o sistema.",
                        style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            ThemeMode.SYSTEM to "Sistema",
                            ThemeMode.LIGHT to "Claro",
                            ThemeMode.DARK to "Escuro"
                        )
                        options.forEach { (mode, label) ->
                            val selected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(RodapeRadii.sm))
                                    .background(if (selected) RodapeTheme.colors.terracota else RodapeTheme.colors.cream)
                                    .border(
                                        1.dp,
                                        if (selected) RodapeTheme.colors.terracota else RodapeTheme.colors.divider,
                                        RoundedCornerShape(RodapeRadii.sm)
                                    )
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.setThemeMode(mode) },
                                    )
                                    .semantics { contentDescription = "Tema $label" }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) RodapeTheme.colors.cream else RodapeTheme.colors.ink
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ── Item "Sobre o Rodapé" ───────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RodapeRadii.sm))
                        .border(0.5.dp, RodapeTheme.colors.divider, RoundedCornerShape(RodapeRadii.sm))
                        .clickable { onNavigateToAbout() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = RodapeIcons.Info,
                        contentDescription = null,
                        tint = RodapeTheme.colors.oliva,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sobre o Rodapé",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = RodapeTheme.colors.ink
                            )
                        )
                        Text(
                            text = "Versão, direitos autorais, privacidade",
                            style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                        )
                    }
                    Icon(
                        imageVector = RodapeIcons.ChevR,
                        contentDescription = null,
                        tint = RodapeTheme.colors.muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Ajude o app a crescer ────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Overline(
                    text = "AJUDE O RODAPÉ A CRESCER",
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RodapeRadii.md),
                    colors = CardDefaults.cardColors(containerColor = RodapeTheme.colors.olivaSoft.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, RodapeTheme.colors.oliva.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Somos novos por aqui 💚",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = RodapeTheme.colors.olivaDark
                            )
                        )
                        Text(
                            text = "A gente lê todo feedback nas primeiras horas. Conta o que você acha, o que falta, o que poderia ser melhor — é assim que o Rodapé vai virar o que vocês precisam.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = RodapeTheme.colors.ink,
                                lineHeight = 20.sp
                            )
                        )
                        TbButton(
                            text = "Mandar feedback",
                            onClick = { showFeedbackDialog = true },
                            variant = TbButtonVariant.Primary,
                            size = TbButtonSize.Md,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!ratedApp) {
                            TbButton(
                                text = "⭐ Avaliar na Play Store",
                                onClick = {
                                    com.example.util.openPlayStorePage(context)
                                    viewModel.markAppRated()
                                },
                                variant = TbButtonVariant.Outline,
                                size = TbButtonSize.Md,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "⭐ Obrigado por avaliar!",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = RodapeTheme.colors.olivaDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ── Sair do clube (membro comum) — link discreto: sair do clube e
            //    sair da conta têm consequências bem diferentes, então não podem
            //    ter o mesmo peso visual (dois botões iguais colados convidavam
            //    ao toque errado).
            if (activeClub != null) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Sair do clube \"${activeClub!!.nome}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            color = RodapeTheme.colors.muted
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            // C5: alvo de 48dp + anuncia como botão (era só texto clicável).
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(RodapeRadii.sm))
                            .clickable { leaveClubError = null; showLeaveClubDialog = true }
                            .semantics { role = Role.Button }
                            .padding(vertical = 8.dp)
                    )
                }
            }

            // ── Sair da conta ────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))
                TbButton(
                    text = "Sair da conta",
                    onClick = { showLogoutDialog = true },
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Lg,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Excluir conta (requisito Play Store) ─────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Excluir minha conta",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Alvo de toque de 48dp + anuncia como botao (era um Text
                        // minusculo com clickable sem role).
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(RodapeRadii.sm))
                        .clickable(role = Role.Button) { showDeleteAccountDialog = true }
                        .padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Dialog: sair do clube
    if (showLeaveClubDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveClubDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sair do clube?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Você deixa de ver a estante, discussões e encontros deste clube. Dá pra voltar depois com o código de convite.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
                    )
                    leaveClubError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.leaveActiveClub(
                        onBlocked = { msg -> leaveClubError = msg },
                        onDone = { showLeaveClubDialog = false },
                    )
                }) { Text("Sair do clube", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveClubDialog = false }) { Text("Voltar", color = RodapeTheme.colors.muted) }
            },
        )
    }

    // Dialog: excluir conta
    if (showDeleteAccountDialog) {
        var deleting by remember { mutableStateOf(false) }
        var deleteError by remember { mutableStateOf<String?>(null) }
        // G2: confirmação por digitação numa ação IRREVERSÍVEL — atrito de propósito.
        var confirmText by remember { mutableStateOf("") }
        val confirmedToDelete = confirmText.trim().equals("EXCLUIR", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteAccountDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Excluir sua conta?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Essa ação é permanente. Sua conta, perfil e dados pessoais são removidos. Conteúdo já compartilhado nos clubes pode permanecer anônimo.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
                    )
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        singleLine = true,
                        enabled = !deleting,
                        label = { Text("Digite EXCLUIR para confirmar") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            focusedLabelColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                    deleteError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting && confirmedToDelete,
                    onClick = {
                        deleting = true
                        deleteError = null
                        viewModel.deleteAccount(
                            onDeleted = {
                                showDeleteAccountDialog = false
                                onLogoutCompleted()
                            },
                            onError = { msg ->
                                deleting = false
                                // Fallback honesto: se o RPC ainda não existe, abre
                                // email pra solicitar a exclusão manualmente.
                                deleteError = "$msg\nSe persistir, toque em \"Pedir por email\"."
                            },
                        )
                    }
                ) { Text(if (deleting) "Excluindo…" else "Excluir conta", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        com.example.util.openEmailFeedback(
                            context = context,
                            body = "Quero excluir minha conta do Rodapé (email: ${email ?: "-"})."
                        )
                    }) { Text("Pedir por email", color = RodapeTheme.colors.muted) }
                    TextButton(enabled = !deleting, onClick = { showDeleteAccountDialog = false }) { Text("Cancelar", color = RodapeTheme.colors.muted) }
                }
            },
        )
    }

    // Dialog de feedback
    if (showFeedbackDialog) {
        var feedbackText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Envie esse feedback 💚",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Pode escrever sem cerimônia: o que rolou bem, o que travou, o que poderia melhorar. A gente lê tudo.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
                    )
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Ex: amei a estante! mas senti falta de…") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Vai abrir seu app de email pra você revisar antes de enviar.",
                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    com.example.util.openEmailFeedback(
                        context = context,
                        body = feedbackText.ifBlank { "" }
                    )
                    showFeedbackDialog = false
                }) {
                    Text("Abrir email", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Cancelar", color = RodapeTheme.colors.muted)
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = RodapeTheme.colors.cream,
            title = {
                Text(
                    "Deseja sair?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LiterataFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RodapeTheme.colors.ink
                    )
                )
            },
            text = {
                Text(
                    "Tem certeza que deseja desconectar da sua conta?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
                )
            },
            confirmButton = {
                TbButton(
                    text = "Sair",
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout { onLogoutCompleted() }
                    },
                    variant = TbButtonVariant.Terra
                )
            },
            dismissButton = {
                TbButton(
                    text = "Cancelar",
                    onClick = { showLogoutDialog = false },
                    variant = TbButtonVariant.Outline
                )
            }
        )
    }
}

@Composable
fun EditProfileView(
    initialName: String,
    initialEmail: String,
    initialAvatarUrl: String,
    initialPronome: String = "",
    onSave: (String, String, String, String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var avatarUrl by remember { mutableStateOf(initialAvatarUrl) }
    var pronome by remember { mutableStateOf(initialPronome) }

    // Avatares disponíveis — fonte única em Avatar.kt (só domínio público).
    val presetNames = com.example.ui.components.presetAvatarKeys

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RodapeTheme.colors.cardSoft)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Header com seta de voltar visível: antes só dava pra sair pelo
            // "Cancelar" lá embaixo (exigia scroll) ou pelo back do sistema.
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = RodapeIcons.Back,
                        contentDescription = "Voltar",
                        tint = RodapeTheme.colors.ink
                    )
                }
                Text(
                    text = "Editar perfil",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = RodapeTheme.colors.ink
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )
            }
        }

        // ── Large avatar preview ──
        item {
            Avatar(
                name = name.ifEmpty { "Você" },
                avatarUrl = avatarUrl,
                size = 100.dp
            )
        }

        // ── Preset avatar grid ──
        item {
            Text(
                text = "ESCOLHER UM AVATAR",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = RodapeTheme.colors.muted
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            // Grid 4 colunas x 3 linhas (12 slots; sobram 2 vazios pra simetria)
            // Como todos os avatares ilustrados agora têm mesmo bounding box (1.20w x 1.50h),
            // o peso visual é uniforme — independente da proporção da arte original.
            val rows = presetNames.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowPresets ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = avatarUrl == preset
                            val isIllustrated = preset.startsWith("preset:")
                            val displayLabel = com.example.ui.components.presetDisplayName(preset)
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(76.dp)
                                    // Anuncia o avatar como opcao de um grupo e informa
                                    // se esta selecionado (antes era um clickable mudo).
                                    .selectable(
                                        selected = isSelected,
                                        role = Role.RadioButton,
                                        onClick = { avatarUrl = preset },
                                    )
                                    .semantics {
                                        stateDescription = if (isSelected) "Selecionado" else "Não selecionado"
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Avatar(
                                    name = displayLabel,
                                    avatarUrl = if (isIllustrated) preset else "",
                                    size = 44.dp,
                                    ring = if (isSelected) RodapeTheme.colors.ink else null
                                )
                            }
                        }
                        // Preenche espaços vazios na última linha pra manter alinhamento
                        repeat(4 - rowPresets.size) {
                            Box(modifier = Modifier.width(56.dp).height(76.dp))
                        }
                    }
                }
            }
        }

        // ── Nome field (exige nome + sobrenome) ──
        item {
            val nameTouched = name.isNotEmpty()
            // Nome único é válido (contas Google como "Ana"). Antes exigia 2 palavras,
            // o que travava salvar QUALQUER coisa — até só a foto — pra quem tem 1 nome.
            val nameValid = name.trim().length >= 2
            val nameError = nameTouched && !nameValid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SEU NOME",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = RodapeTheme.colors.muted
                    )
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Como você quer ser chamado", color = RodapeTheme.colors.muted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RodapeRadii.sm),
                    singleLine = true,
                    isError = nameError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RodapeTheme.colors.terracota,
                        unfocusedBorderColor = RodapeTheme.colors.divider,
                        focusedContainerColor = RodapeTheme.colors.cream,
                        unfocusedContainerColor = RodapeTheme.colors.cream,
                        focusedTextColor = RodapeTheme.colors.ink,
                        unfocusedTextColor = RodapeTheme.colors.ink
                    )
                )
                if (nameError) {
                    Text(
                        text = "Digite pelo menos 2 letras.",
                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.terracota)
                    )
                }
            }
        }

        // ── Pronome (opcional) — C6: escolha, nunca imposição ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PRONOME (OPCIONAL)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = RodapeTheme.colors.muted
                    )
                )
                // Sugestões rápidas — tocar preenche; tocar de novo limpa. Também
                // dá pra digitar livre no campo abaixo (elu, ile, ela/dele, etc.).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ela/dela", "ele/dele", "elu/delu").forEach { opt ->
                        val selected = pronome.trim().equals(opt, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(RodapeRadii.sm))
                                .background(if (selected) RodapeTheme.colors.terracota else RodapeTheme.colors.cream)
                                .border(
                                    1.dp,
                                    if (selected) RodapeTheme.colors.terracota else RodapeTheme.colors.divider,
                                    RoundedCornerShape(RodapeRadii.sm)
                                )
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { pronome = if (selected) "" else opt },
                                )
                                .semantics { contentDescription = "Pronome $opt" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) RodapeTheme.colors.cream else RodapeTheme.colors.ink
                                )
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = pronome,
                    onValueChange = { if (it.length <= 40) pronome = it },
                    placeholder = { Text("Como prefere ser tratado(a)", color = RodapeTheme.colors.muted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RodapeRadii.sm),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RodapeTheme.colors.terracota,
                        unfocusedBorderColor = RodapeTheme.colors.divider,
                        focusedContainerColor = RodapeTheme.colors.cream,
                        unfocusedContainerColor = RodapeTheme.colors.cream,
                        focusedTextColor = RodapeTheme.colors.ink,
                        unfocusedTextColor = RodapeTheme.colors.ink
                    )
                )
            }
        }

        // ── E-mail field ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "EMAIL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = RodapeTheme.colors.muted
                    )
                )
                // Email é gerenciado pelo Supabase Auth (auth.users) e NÃO é
                // alterável por aqui — o campo era editável mas a mudança era
                // descartada em silêncio. Agora é somente leitura.
                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    placeholder = { Text("exemplo@email.com", color = RodapeTheme.colors.muted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RodapeRadii.sm),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = RodapeTheme.colors.ink,
                        disabledBorderColor = RodapeTheme.colors.divider,
                        disabledContainerColor = RodapeTheme.colors.cream,
                        disabledPlaceholderColor = RodapeTheme.colors.muted,
                    )
                )
                Text(
                    text = "O email é usado pra login e não pode ser alterado por aqui.",
                    style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
                )
            }
        }

        // ── Cancelar / Salvar buttons ──
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TbButton(
                    text = "Cancelar",
                    onClick = onCancel,
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Lg,
                    modifier = Modifier.weight(1f)
                )
                // Email não é editável (gerido pelo Auth), então só nome/avatar
                // contam pra habilitar Salvar. Nome único é aceito (não trava a foto).
                val nameValid = name.trim().length >= 2
                val nameChanged = name != initialName
                val avatarChanged = avatarUrl != initialAvatarUrl
                val pronomeChanged = pronome.trim() != initialPronome.trim()
                // A4: trocar só o avatar (ou só o pronome) não depende do nome — só
                // exige nome válido quando o nome está sendo de fato alterado.
                val canSave = (nameChanged || avatarChanged || pronomeChanged) && (!nameChanged || nameValid)
                TbButton(
                    text = "Salvar",
                    onClick = {
                        if (canSave) {
                            onSave(name.trim(), email.trim(), avatarUrl, pronome.trim().ifBlank { null })
                        }
                    },
                    variant = TbButtonVariant.Terra,
                    size = TbButtonSize.Lg,
                    enabled = canSave,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Ticket perfurado do design (screens-main.jsx MeetingTicket): cabeçalho com
 * borda tracejada + countdown com dot dourado, carimbo de data em serif itálico
 * 64sp, perfuração vertical pontilhada, stub inferior escurecido com avatares e
 * botão de RSVP, e notches recortados nas laterais.
 */
@Composable
private fun MeetingTicket(
    meeting: com.example.data.model.Meeting,
    confirmedUsers: List<com.example.data.model.User>,
    isParticipating: Boolean,
    onRsvp: () -> Unit,
) {
    // Parse do label persistido ("DOMINGO, 24 DE OUTUBRO").
    val dateParts = meeting.data.split(",")
    val weekday = dateParts.firstOrNull()?.trim()?.uppercase() ?: ""
    val rest = dateParts.getOrNull(1)?.trim() ?: ""
    val dayNumber = rest.takeWhile { it.isDigit() }.ifEmpty { "–" }
    val monthName = rest.dropWhile { it.isDigit() }.trim().lowercase().removePrefix("de ").trim()
    val daysUntil = remember(meeting.data) { com.example.util.daysUntilMeetingLabel(meeting.data) }
    val ticketLineColor = RodapeTheme.colors.cream

    Box(modifier = Modifier.fillMaxWidth().ticketShadow(cornerRadius = RodapeRadii.md)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RodapeRadii.md))
                .background(RodapeTheme.colors.olivaDeep)
        ) {
            // Cabeçalho — overline + countdown, separado por linha tracejada
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = ticketLineColor.copy(alpha = 0.25f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                            ),
                        )
                    }
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PRÓXIMO ENCONTRO",
                    fontFamily = InterFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = RodapeTheme.colors.cream.copy(alpha = 0.7f),
                )
                if (daysUntil != null && daysUntil >= 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RodapeRadii.full))
                            .background(RodapeTheme.colors.cream.copy(alpha = 0.12f))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(6.dp).background(RodapeTheme.colors.dourado, CircleShape))
                        Text(
                            text = when (daysUntil) {
                                0 -> "é hoje!"
                                1 -> "amanhã"
                                else -> "em $daysUntil dias"
                            },
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp,
                            color = RodapeTheme.colors.cream,
                        )
                    }
                }
            }

            // Corpo — carimbo de data · perfuração · detalhes
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 24.dp)
                        // A11y: lê "quinta, 24 de outubro" em vez de 3 fragmentos
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$weekday, $dayNumber de $monthName"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = weekday,
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = RodapeTheme.colors.cream.copy(alpha = 0.65f),
                        maxLines = 1,
                    )
                    Text(
                        text = dayNumber,
                        fontFamily = LiterataFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 64.sp,
                        lineHeight = 64.sp,
                        letterSpacing = (-2).sp,
                        color = RodapeTheme.colors.cream,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                    Text(
                        text = monthName,
                        fontFamily = LiterataFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = RodapeTheme.colors.cream.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Perfuração vertical pontilhada
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .drawBehind {
                            drawLine(
                                color = ticketLineColor.copy(alpha = 0.3f),
                                start = Offset(0.5f, 0f),
                                end = Offset(0.5f, size.height),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                                ),
                            )
                        }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 22.dp),
                ) {
                    Text(
                        text = meeting.agenda.ifEmpty { "Próximo encontro do clube" },
                        fontFamily = LiterataFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        letterSpacing = (-0.3).sp,
                        color = RodapeTheme.colors.cream,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = RodapeIcons.Clock,
                            contentDescription = null,
                            tint = RodapeTheme.colors.cream.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = meeting.hora,
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = RodapeTheme.colors.cream.copy(alpha = 0.85f),
                        )
                    }
                    if (meeting.local.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = RodapeIcons.Pin,
                                contentDescription = null,
                                tint = RodapeTheme.colors.cream.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp).padding(top = 1.dp),
                            )
                            Text(
                                text = meeting.local,
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = RodapeTheme.colors.cream.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Stub inferior — canhoto do ingresso
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.18f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        confirmedUsers.take(3).forEach { u ->
                            Avatar(
                                name = u.nome,
                                avatarUrl = u.avatarUrl ?: "",
                                size = 26.dp,
                                ring = RodapeTheme.colors.olivaDeep,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when {
                            confirmedUsers.isEmpty() -> "Ninguém confirmou ainda"
                            confirmedUsers.size > 3 -> "+${confirmedUsers.size - 3} vão"
                            else -> "${confirmedUsers.size} ${if (confirmedUsers.size == 1) "vai" else "vão"}"
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = RodapeTheme.colors.cream.copy(alpha = 0.8f),
                    )
                }

                Row(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(RodapeRadii.full))
                        .background(if (isParticipating) RodapeTheme.colors.oliva else RodapeTheme.colors.cream)
                        .clickable(onClick = onRsvp, role = Role.Button)
                        .semantics {
                            selected = isParticipating
                            stateDescription = if (isParticipating) "presença confirmada" else "não confirmada"
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = if (isParticipating) "Confirmado" else "Eu vou",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.1).sp,
                        color = if (isParticipating) RodapeTheme.colors.cream else RodapeTheme.colors.olivaDeep,
                    )
                    Icon(
                        imageVector = if (isParticipating) RodapeIcons.Check else RodapeIcons.ChevR,
                        contentDescription = null,
                        tint = if (isParticipating) RodapeTheme.colors.cream else RodapeTheme.colors.olivaDeep,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        // Notches recortados nas laterais (na altura da perfuração)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-8).dp, y = 70.dp)
                .size(16.dp)
                .background(RodapeTheme.colors.paper, CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = 70.dp)
                .size(16.dp)
                .background(RodapeTheme.colors.paper, CircleShape)
        )
    }
}
