package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.components.*
import com.example.ui.theme.Terracota
import com.example.ui.theme.TerracotaSoft
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
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.components.Cover
import com.example.ui.components.Pill
import com.example.ui.components.PillVariant
import com.example.ui.components.ProgressBar
import com.example.ui.components.TramabookCard
import com.example.ui.components.TbSectionHeader

import androidx.compose.foundation.lazy.LazyRow
import com.example.ui.theme.InterFontFamily
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(
    viewModel: MainViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDiscussion: (String, String) -> Unit,
    onNavigateToSuggestBook: () -> Unit,
    onNavigateToJoinClub: () -> Unit,
    onLogoutCompleted: () -> Unit,
    onNavigateToBookDetail: (String) -> Unit = {},
    onNavigateToFrases: () -> Unit = {},
    onNavigateToManageClub: () -> Unit = {},
    onNavigateToMeetingDetail: (String) -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf("home") }
    // Observa pedidos externos de troca de tab (ex: notificações navegando)
    val requestedTab by viewModel.requestedTab.collectAsState()
    LaunchedEffect(requestedTab) {
        requestedTab?.let { tab ->
            selectedTab = tab
            viewModel.consumeRequestedTab()
        }
    }
    var showBottomSheet by remember { mutableStateOf(false) }

    val activeClub by viewModel.activeClub.collectAsState()
    val allClubs by viewModel.allClubs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val unreadNotificationsCount by viewModel.notifications.map { notifs ->
        notifs.count { !it.lida }
    }.collectAsState(initial = 0)
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 16.dp)) {
                        Avatar(
                            name = currentUser?.nome ?: "Você",
                            avatarUrl = currentUser?.avatarUrl ?: "",
                            size = 40.dp,
                            modifier = Modifier.clickable {
                                selectedTab = "profile"
                            }
                        )
                    }
                },
                title = {
                    val rawName = activeClub?.nome ?: "Tramabook"
                    val clubName = if (rawName.length > 20) rawName.take(20) + "..." else rawName
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showBottomSheet = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = clubName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Terracota,
                                fontFamily = LiterataFontFamily,
                                fontSize = 20.sp
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Terracota.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Trocar de clube",
                                tint = Terracota,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onNavigateToManageClub) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Gerenciar clube",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notificações",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (unreadNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Terracota, CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            CustomBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                "home" -> HomeScreenTab(
                    viewModel = viewModel,
                    onNavigateToDiscussion = onNavigateToDiscussion,
                    onNavigateToTab = { selectedTab = it }
                )
                "book" -> BookDetailScreenTab(
                    viewModel = viewModel,
                    onNavigateToDiscussion = onNavigateToDiscussion
                )
                "next" -> NextTabScreen(
                    viewModel = viewModel,
                    onNavigateToSuggestBook = onNavigateToSuggestBook,
                    onNavigateToMeetingDetail = onNavigateToMeetingDetail
                )
                "shelf" -> ShelfTabScreen(
                    viewModel = viewModel,
                    onNavigateToBookDetail = onNavigateToBookDetail
                )
                "profile" -> ProfileScreenTab(
                    viewModel = viewModel,
                    onLogoutCompleted = onLogoutCompleted,
                    onNavigateToTab = { selectedTab = it },
                    onNavigateToJoinClub = onNavigateToJoinClub,
                    onNavigateToFrases = onNavigateToFrases
                )
            }
        }
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
                        .background(TertiarySoft)
                        .clip(CircleShape)
                )

                Text(
                    text = "Trocar de clube",
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isActive) OlivaSoft.copy(alpha = 0.35f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) Terracota else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = OlivaSoft,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "atual",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Oliva,
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
                    text = "+ Entrar em outro clube",
                    onClick = { showBottomSheet = false; onNavigateToJoinClub() },
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Md,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
fun CustomBottomBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            color = OlivaDeep,
            shadowElevation = 8.dp
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
                    icon = if (selectedTab == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    selected = selectedTab == "home",
                    onClick = { onTabSelected("home") }
                )
                BottomBarItem(
                    label = "Livro atual",
                    icon = if (selectedTab == "book") Icons.Filled.Edit else Icons.Outlined.Edit,
                    selected = selectedTab == "book",
                    onClick = { onTabSelected("book") }
                )
                BottomBarItem(
                    label = "Próximo",
                    icon = if (selectedTab == "next") Icons.Filled.Star else Icons.Outlined.Star,
                    selected = selectedTab == "next",
                    onClick = { onTabSelected("next") }
                )
                BottomBarItem(
                    label = "Estante",
                    icon = if (selectedTab == "shelf") Icons.Filled.List else Icons.Outlined.List,
                    selected = selectedTab == "shelf",
                    onClick = { onTabSelected("shelf") }
                )
                BottomBarItem(
                    label = "Perfil",
                    icon = if (selectedTab == "profile") Icons.Filled.Person else Icons.Outlined.Person,
                    selected = selectedTab == "profile",
                    onClick = { onTabSelected("profile") }
                )
            }
        }
    }
}

@Composable
fun BottomBarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Terracota)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Cream,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Cream,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    } else {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Cream.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- HOME SCREEN TAB ---
@Composable
fun HomeScreenTab(
    viewModel: MainViewModel,
    onNavigateToDiscussion: (String, String) -> Unit,
    onNavigateToTab: (String) -> Unit
) {
    val activeClub by viewModel.activeClub.collectAsState()
    val currentBook by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val progress by viewModel.userProgress.collectAsState()
    val allProgress by viewModel.allProgressForClub.collectAsState()
    val members by viewModel.clubMembers.collectAsState()

    val currentChapIndex = progress?.currentChapter ?: 0

    val meeting by viewModel.latestMeeting.collectAsState()
    val rsvps by viewModel.latestMeetingRsvps.collectAsState()

    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val salutation = when (hour) {
        in 5..12 -> "Bom dia"
        in 13..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    val nameToGreet = currentUserFirst(viewModel)
    val fullGreeting = "$salutation, $nameToGreet."

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
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Ink)) {
                        append("A galera tá ")
                    }
                    withStyle(SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontFamily = LiterataFontFamily,
                        color = Oliva
                    )) {
                        append("esperando.")
                    }
                },
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 38.sp
                )
            )
        }

        // Section header for PRÓXIMO ENCONTRO
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Terracota, CircleShape)
                )
                Text(
                    text = "PRÓXIMO ENCONTRO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Terracota
                    )
                )
            }
        }

        // Section: Próximo encontro card (Image 1 Left Card 1)
        item {
            if (meeting == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum próximo encontro agendado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val confirmedUsers = rsvps.filter { it.status == "Vou" }.mapNotNull { r ->
                    members.find { it.id == r.userId }
                }

                val rawData = meeting?.data ?: "DOMINGO, 24 DE OUTUBRO"
                val dateParts = rawData.split(",")
                val dayNameStr = dateParts.firstOrNull()?.trim()?.take(3)?.uppercase() ?: "DOM"
                val fullDatePart = dateParts.getOrNull(1)?.trim() ?: "24 DE OUTUBRO"
                val dayNumber = fullDatePart.trim().takeWhile { it.isDigit() }.ifEmpty { "24" }
                val monthName = fullDatePart.replace(dayNumber, "").replace("de", "").trim().lowercase()
                val finalMonthLabel = if (monthName.length > 3) "${monthName.take(3)}. de 25" else "$monthName de 25"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(OlivaDeep)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date Column
                            Column(
                                modifier = Modifier.width(80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayNameStr,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Cream.copy(alpha = 0.5f),
                                        letterSpacing = 0.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dayNumber,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Cream,
                                        fontFamily = LiterataFontFamily,
                                        fontStyle = FontStyle.Italic
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = finalMonthLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Cream.copy(alpha = 0.4f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Vertical Divider Line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(84.dp)
                                    .background(Cream.copy(alpha = 0.15f))
                            )

                            // Detail Column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // "em 3 dias" countdown pill
                                    Pill(text = "em 3 dias", variant = PillVariant.Default)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = meeting?.agenda?.ifEmpty { "Discussão: A Hora da Estrela" } ?: "Discussão: A Hora da Estrela",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Cream
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DateRange,
                                        contentDescription = null,
                                        tint = Cream.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = meeting?.hora ?: "19:00 — 21:00",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Cream.copy(alpha = 0.6f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Place,
                                        contentDescription = null,
                                        tint = Cream.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = meeting?.local ?: "Café Lispector, Vila Madalena",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Cream.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Horizontal thin outline divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Cream.copy(alpha = 0.12f))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // RSVP Attendance row with overlapping avatars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((-8).dp)
                            ) {
                                confirmedUsers.take(3).forEach { u ->
                                    Avatar(
                                        name = u.nome,
                                        avatarUrl = u.avatarUrl ?: "",
                                        size = 28.dp,
                                        ring = OlivaDeep,
                                        modifier = Modifier
                                    )
                                }
                                if (confirmedUsers.size > 3) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(OlivaMid.copy(alpha = 0.4f), CircleShape)
                                            .border(1.5.dp, OlivaDeep, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${confirmedUsers.size - 3}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Cream
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (confirmedUsers.isEmpty()) "Ninguém confirmado" else "${confirmedUsers.size} confirmaram",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Cream.copy(alpha = 0.6f)
                                    )
                                )
                            }

                            // RSVP Toggle — pill-style button
                            val isParticipating = rsvps.any { it.userId == (viewModel.currentUserId.value ?: "user_voce") && it.status == "Vou" }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isParticipating) Oliva else Cream)
                                    .clickable {
                                        val nextStatus = if (isParticipating) "Não vou" else "Vou"
                                        meeting?.let { m -> viewModel.rsvpMeeting(m.id, nextStatus) }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isParticipating) "Confirmado ✓" else "Eu vou >",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isParticipating) Cream else OlivaDeep,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Tua Leitura Row Card (Image 1 Left Card 2)
        item {
            TramabookCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab("book") },
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Cover(
                        title = currentBook?.title ?: "A Hora da Estrela",
                        author = currentBook?.author ?: "",
                        coverUrl = currentBook?.coverUrl ?: "",
                        width = 48.dp,
                        height = 72.dp
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        val bookTitle = currentBook?.title ?: "A Hora da Estrela"
                        val totalChaps = if (chapters.isNotEmpty()) chapters.size else 13
                        val curChap = if (currentChapIndex > 0) currentChapIndex else 8

                        Text(
                            text = "TUA LEITURA · CAP. $curChap/$totalChaps",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Terracota,
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

                        val visualPct = curChap.toFloat() / totalChaps.toFloat()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProgressBar(
                                value = visualPct,
                                color = Terracota,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(visualPct * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Muted
                                )
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Ver livro",
                        tint = Muted.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Section: Onde a galera tá list (Image 1 Left bottom)
        item {
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

        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                val currentUserId = viewModel.currentUserId.value ?: "user_voce"
                val sortedMembers = members.sortedWith(compareBy { it.id != currentUserId })
                
                items(sortedMembers) { member ->
                    val memberProg = allProgress.find { it.userId == member.id && it.bookId == (currentBook?.id ?: "book_metamorfose") }
                    val memChap = memberProg?.currentChapter ?: if (member.id == "user_marina") 9 else if (member.id == "user_sofia") 13 else 8
                    val totalChaps = if (chapters.isNotEmpty()) chapters.size else 13

                    val isCurrentUser = member.id == currentUserId
                    val displayName = if (isCurrentUser) "Você" else member.nome.substringBefore(" ")

                    val finished = memChap >= totalChaps

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(84.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Avatar(
                                name = member.nome,
                                avatarUrl = member.avatarUrl ?: "",
                                size = 56.dp,
                                ring = if (isCurrentUser) Terracota else null
                            )

                            // Small progress badge below avatar
                            Box(modifier = Modifier.offset(y = 10.dp)) {
                                Pill(
                                    text = if (finished) "Terminou" else "Cap. $memChap",
                                    variant = if (finished) PillVariant.OliveDeep else PillVariant.Default
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Ink
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // About the club details bottom drawer representation
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
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
            TramabookCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = activeClub?.descricao ?: "Um clubinho clássico de leitura íntima para tomar vinho e conversar livremente sobre livros excelentes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Private helper to gracefully get the welcome first name or nickname
@Composable
private fun currentUserFirst(viewModel: MainViewModel): String {
    val currentUser by viewModel.currentUser.collectAsState()
    val name = currentUser?.nome ?: "Bia"
    return name.substringBefore(" ")
}

// --- BOOK DETAIL SCREEN TAB ---
@Composable
fun BookDetailScreenTab(
    viewModel: MainViewModel,
    onNavigateToDiscussion: (String, String) -> Unit
) {
    val currentBook by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()
    val progress by viewModel.userProgress.collectAsState()

    val currentChapIndex = progress?.currentChapter ?: 0

    if (currentBook == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhum livro atualmente em leitura.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val totalChapters = chapters.size
        val pct = if (totalChapters > 0) {
            (currentChapIndex.toFloat() / totalChapters.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val pctInt = (pct * 100).toInt()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── OLIVE HERO ──────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = OlivaDeep,
                            shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                        )
                        .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 80.dp)
                ) {
                    // Label row
                    Text(
                        text = "Livro atual",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Cream.copy(alpha = 0.70f)
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
                        Cover(
                            title = currentBook!!.title,
                            author = currentBook!!.author,
                            coverUrl = currentBook?.coverUrl ?: "",
                            width = 108.dp,
                            height = 162.dp
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentBook!!.title,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Cream,
                                    lineHeight = 30.sp
                                ),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentBook!!.author,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Cream.copy(alpha = 0.70f)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "★ 4.5  ·  $totalChapters capítulos  ·  8 lendo",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Cream.copy(alpha = 0.80f)
                                )
                            )
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
                    TramabookCard(
                        modifier = Modifier.fillMaxWidth(),
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
                                    text = "Tua leitura",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                        color = Muted
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Ink)) {
                                            append("Cap. $currentChapIndex")
                                        }
                                        withStyle(SpanStyle(color = Muted, fontWeight = FontWeight.Normal)) {
                                            append(" de $totalChapters")
                                        }
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            // Circular ring progress indicator
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
                                            color = DividerSoft,
                                            startAngle = -90f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = stroke
                                            )
                                        )
                                        // progress arc
                                        drawArc(
                                            color = Terracota,
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
                                        color = Ink
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TbButton(
                            text = "Marcar progresso",
                            onClick = {
                                val nextChap = currentChapIndex + 1
                                if (nextChap <= chapters.size) {
                                    viewModel.updateBookProgress(currentBook!!.id, nextChap)
                                }
                            },
                            variant = TbButtonVariant.Terra,
                            size = TbButtonSize.Lg,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                color = Ink
                            )
                        )
                        val readCount = chapters.count { it.numero < currentChapIndex }
                        Text(
                            text = "$readCount lidos",
                            style = MaterialTheme.typography.bodySmall.copy(color = Muted)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chapters.forEach { chapter ->
                            val chapNumber = chapter.numero
                            val isCompleted = chapNumber < currentChapIndex
                            val isCurrent = chapNumber == currentChapIndex
                            val isLocked = chapNumber > currentChapIndex

                            val commentsFlow = remember(chapter.id) {
                                viewModel.getCommentsForChapter(chapter.id)
                            }
                            val chapterComments by commentsFlow.collectAsState(initial = emptyList())
                            val commentsCount = chapterComments.size

                            // Chapter row card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isLocked) {
                                        onNavigateToDiscussion(chapter.id, chapter.titulo)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isCurrent) Cream else CardSurface,
                                border = BorderStroke(
                                    width = if (isCurrent) 1.5.dp else 0.5.dp,
                                    color = if (isCurrent) Terracota else Divider
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
                                                    isLocked -> DividerSoft
                                                    isCurrent -> Terracota
                                                    else -> OlivaSoft
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isLocked -> Icon(
                                                imageVector = Icons.Outlined.Lock,
                                                contentDescription = "Bloqueado",
                                                tint = Muted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            isCurrent -> Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = "Capítulo atual",
                                                tint = Cream,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            else -> Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = "Concluído",
                                                tint = OlivaDeep,
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
                                                    color = Muted
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
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = if (isLocked) Muted else Ink
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
                                                color = Muted,
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

            // ── STATISTICS CARD ─────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-30).dp)
                        .padding(horizontal = 22.dp)
                ) {
                    Text(
                        text = "Estatísticas",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Ink
                        ),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    TramabookCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tempo de leitura",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Muted,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1h 45m",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(Divider)
                            )

                            Spacer(modifier = Modifier.width(24.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Velocidade média",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Muted,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "25 pág/h",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
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
    onNavigateToFrases: () -> Unit
) {
    val name by viewModel.userName.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allClubs by viewModel.allClubs.collectAsState()
    val activeClub by viewModel.activeClub.collectAsState()
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    val archivedClubs by viewModel.archivedClubsForUser.collectAsState()

    var isEditingProfile by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (isEditingProfile) {
        EditProfileView(
            initialName = name ?: "",
            initialEmail = email ?: "",
            initialAvatarUrl = currentUser?.avatarUrl ?: "",
            onSave = { newName, newEmail, newAvatar ->
                viewModel.updateUserProfile(newName, newEmail, newAvatar)
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
                            name = name ?: "Você",
                            avatarUrl = currentUser?.avatarUrl ?: "",
                            size = 72.dp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = name ?: "Usuário",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )
                            )
                            Text(
                                text = email ?: "contato@tramabook.com",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Muted)
                            )
                        }
                    }
                    IconButton(onClick = { isEditingProfile = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar Perfil",
                            tint = Terracota
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
                    // Card 1: Livros lidos
                    TramabookCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = "24",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = LiterataFontFamily,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "livros\nlidos",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = InterFontFamily,
                                color = Muted
                            )
                        )
                    }

                    // Card 2: Clubes ativos
                    TramabookCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = "${allClubs.size}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = LiterataFontFamily,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "clubes\nativos",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = InterFontFamily,
                                color = Muted
                            )
                        )
                    }

                    // Card 3: Frases guardadas (Oliva background, Cream text)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToFrases() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Oliva, contentColor = Cream),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = savedQuotes.size.toString(),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Cream
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "frases\nguardadas",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = InterFontFamily,
                                    color = Cream.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }
            }

            // ── Teus Clubes ─────────────────────────────────────────────
            item {
                TbSectionHeader(title = "Teus clubes")
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allClubs.forEach { club ->
                        val isActive = club.id == activeClub?.id
                        val clubColor = clubColorFor(club.cor)
                        val clubReadingText = if (club.id == "club_tramabook") {
                            "Lendo: A Metamorfose"
                        } else if (club.id == "club_filosofia") {
                            "Lendo: O Mito de Sísifo"
                        } else {
                            "Sem livro atual"
                        }

                        Card(
                            onClick = {
                                viewModel.selectActiveClub(club.id)
                                onNavigateToTab("home")
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Cream, contentColor = Ink),
                            border = BorderStroke(
                                1.dp,
                                if (isActive) Oliva else Divider
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
                                                    color = Ink
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
                                                color = Muted
                                            )
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowRight,
                                    contentDescription = "Selecionar",
                                    tint = Muted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // "+ Entrar em outro clube" — dashed outline button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                BorderStroke(1.5.dp, Divider),
                                shape = RoundedCornerShape(16.dp)
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
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = Muted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Entrar em outro clube",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Muted
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
                    Text(
                        text = "ARQUIVADOS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Muted,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        archivedClubs.forEach { club ->
                            val resolvedClubColor = clubColorFor(club.cor)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(0.5.dp, Divider, RoundedCornerShape(16.dp))
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
                                            color = Ink
                                        )
                                    )
                                    Text(
                                        text = "Arquivado · toque para reativar",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowRight,
                                    contentDescription = "Reativar",
                                    tint = Muted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Sair da conta ────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TbButton(
                    text = "Sair da conta",
                    onClick = { showLogoutDialog = true },
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Lg,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Cream,
            title = {
                Text(
                    "Deseja sair?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LiterataFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                )
            },
            text = {
                Text(
                    "Tem certeza que deseja desconectar da sua conta?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Muted)
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
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    var avatarUrl by remember { mutableStateOf(initialAvatarUrl) }

    // Preset avatar names (initials-based, matching react prototype)
    val presetNames = listOf("Bia", "Marina", "Rafael", "Júlia", "Leo", "Helena")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CardSoft)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Editar perfil",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = Ink
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Large avatar preview with edit badge ──
        item {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Avatar(
                    name = name.ifEmpty { "Você" },
                    avatarUrl = avatarUrl,
                    size = 100.dp
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Oliva, CircleShape)
                        .border(3.dp, CardSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Selecionar avatar",
                        tint = Cream,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // ── Preset avatar grid ──
        item {
            Text(
                text = "ESCOLHER UM AVATAR",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                presetNames.forEach { preset ->
                    val isSelected = avatarUrl == preset
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { avatarUrl = preset }
                    ) {
                        Avatar(
                            name = preset,
                            avatarUrl = "",
                            size = 44.dp,
                            ring = if (isSelected) Ink else null
                        )
                    }
                }
            }
        }

        // ── Nome field ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "NOME",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Muted
                    )
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Seu nome de leitor", color = Muted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Terracota,
                        unfocusedBorderColor = Divider,
                        focusedContainerColor = Cream,
                        unfocusedContainerColor = Cream,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
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
                        color = Muted
                    )
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("exemplo@email.com", color = Muted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Terracota,
                        unfocusedBorderColor = Divider,
                        focusedContainerColor = Cream,
                        unfocusedContainerColor = Cream,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    )
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
                TbButton(
                    text = "Salvar",
                    onClick = {
                        if (name.isNotBlank() && email.isNotBlank()) {
                            onSave(name, email, avatarUrl)
                        }
                    },
                    variant = TbButtonVariant.Terra,
                    size = TbButtonSize.Lg,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
