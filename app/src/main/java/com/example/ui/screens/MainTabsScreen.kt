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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
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
import com.example.ui.theme.VerdeMusgo
import com.example.ui.viewmodel.MainViewModel

import androidx.compose.foundation.lazy.LazyRow
import com.example.ui.theme.FrauncesFontFamily
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
    onLogoutCompleted: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("home") }
    var showBottomSheet by remember { mutableStateOf(false) }

    val activeClub by viewModel.activeClub.collectAsState()
    val allClubs by viewModel.allClubs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val unreadNotificationsCount by viewModel.notifications.map { notifs ->
        notifs.count { !it.lida }
    }.collectAsState(initial = 0)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 16.dp)) {
                        MemberAvatar(
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
                    val clubName = if (rawName.length > 22) rawName.take(22) + "..." else rawName
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showBottomSheet = true }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = clubName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Terracota,
                                fontFamily = FrauncesFontFamily,
                                fontSize = 22.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Trocar de clube",
                            tint = Terracota,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                actions = {
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
                    onNavigateToSuggestBook = onNavigateToSuggestBook
                )
                "profile" -> ProfileScreenTab(
                    viewModel = viewModel,
                    onLogoutCompleted = onLogoutCompleted,
                    onNavigateToTab = { selectedTab = it },
                    onNavigateToJoinClub = onNavigateToJoinClub
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
                        .background(Color.LightGray)
                        .clip(CircleShape)
                )

                Text(
                    text = "Trocar de clube",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FrauncesFontFamily,
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
                        val clubColor = when (club.cor) {
                            "0" -> Color(0xFF8C4027)
                            "1" -> Color(0xFF4C663D)
                            "2" -> Color(0xFF5A5852)
                            "3" -> Color(0xFFFDE1D8)
                            "4" -> Color(0xFF7A7973)
                            else -> try { Color(android.graphics.Color.parseColor(club.cor)) } catch (e: Exception) { Terracota }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
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
                                    .background(clubColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = club.nome.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = club.nome,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FrauncesFontFamily,
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
                                    color = VerdeMusgo.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "atual",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = VerdeMusgo,
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

                OutlinedButton(
                    onClick = {
                        showBottomSheet = false
                        onNavigateToJoinClub()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    border = BorderStroke(1.dp, Terracota),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Terracota)
                ) {
                    Text(
                        text = "+ Entrar em outro clube",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Terracota.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
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
    val activeColor = Color.White
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (selected) activeColor else inactiveColor
    val textColor = if (selected) Terracota else inactiveColor

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 28.dp),
                onClick = onClick
            )
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) Terracota else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
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
                    append("A galera tá ")
                    withStyle(SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontFamily = FrauncesFontFamily,
                        color = VerdeMusgo
                    )) {
                        append("esperando.")
                    }
                },
                style = MaterialTheme.typography.displayMedium.copy(
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
                        .background(Color(0xFF1B221B))
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
                                modifier = Modifier.width(72.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayNameStr,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.5f),
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dayNumber,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FrauncesFontFamily
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = finalMonthLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            // Vertical Divider Line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(84.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
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
                                    // "em 3 dias" pill
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFF1E9DB), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "em 3 dias",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1B221B)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = meeting?.agenda?.ifEmpty { "Discussão: A Hora da Estrela" } ?: "Discussão: A Hora da Estrela",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FrauncesFontFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
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
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = meeting?.hora ?: "19:00 — 21:00",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f)
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
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = meeting?.local ?: "Café Lispector, Vila Madalena",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.6f),
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
                                .background(Color.White.copy(alpha = 0.12f))
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
                                    MemberAvatar(
                                        name = u.nome,
                                        avatarUrl = u.avatarUrl,
                                        size = 28.dp,
                                        modifier = Modifier.border(1.5.dp, Color(0xFF1B221B), CircleShape)
                                    )
                                }
                                if (confirmedUsers.size > 3) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                            .border(1.5.dp, Color(0xFF1B221B), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${confirmedUsers.size - 3}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (confirmedUsers.isEmpty()) "Ninguém confirmado" else "${confirmedUsers.size} confirmaram",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                )
                            }

                            // RSVP Toggle box interaction "Eu vou >"
                            val isParticipating = rsvps.any { it.userId == (viewModel.currentUserId.value ?: "user_voce") && it.status == "Vou" }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isParticipating) VerdeMusgo else Color.White)
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
                                        color = if (isParticipating) Color.White else Color(0xFF1B221B),
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab("book") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BookCover(
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
                                fontFamily = FrauncesFontFamily,
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
                            LinearProgressIndicator(
                                progress = { visualPct },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Terracota,
                                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                            )
                            Text(
                                text = "${(visualPct * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Ver livro",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
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
                        fontFamily = FrauncesFontFamily,
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

                    val (badgeText, badgeBg, badgeColor) = when {
                        memChap >= totalChaps -> {
                            Triple("Terminou", VerdeMusgo.copy(alpha = 0.12f), VerdeMusgo)
                        }
                        else -> {
                            Triple("Cap. $memChap", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(72.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            MemberAvatar(
                                name = member.nome,
                                avatarUrl = member.avatarUrl,
                                size = 56.dp,
                                modifier = Modifier
                                    .border(1.5.dp, if (isCurrentUser) Terracota else Color.Transparent, CircleShape)
                            )

                            // Small popped-up progress badge cropped over the bottom of the avatar
                            Box(
                                modifier = Modifier
                                    .offset(y = 8.dp)
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .background(badgeBg, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
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
            StandardCard {
                Text(
                    text = activeClub?.descricao ?: "Um clubinho clássico de leitura íntima para tomar vinho e conversar livremente sobre livros excelentes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful Book Header Info - Center Aligned Cover with brown backdrop
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .width(220.dp)
                            .height(290.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Terracota,
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            BookCover(
                                coverUrl = currentBook?.coverUrl ?: "",
                                width = 160.dp,
                                height = 240.dp,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }

                // Title
                Text(
                    text = currentBook!!.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Author
                Text(
                    text = currentBook!!.author,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Extra tagline statistics
                val currentChap = currentChapIndex
                val totalChapters = chapters.size
                Text(
                    text = "$currentChap de $totalChapters capítulos  •  ★ 4.5 do clube  •  8 leitores",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Marcar progresso Button
                Button(
                    onClick = {
                        val nextChap = currentChapIndex + 1
                        if (nextChap <= chapters.size) {
                            viewModel.updateBookProgress(currentBook!!.id, nextChap)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Terracota,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Marcar progresso",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "Capítulos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                // List of Chapters nested inside a single gorgeous white Card/Surface
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        chapters.forEachIndexed { index, chapter ->
                            val chapNumber = chapter.numero
                            
                            val isCompleted = chapNumber < currentChapIndex
                            val isCurrent = chapNumber == currentChapIndex
                            val isLocked = chapNumber > currentChapIndex

                            val commentsFlow = remember(chapter.id) {
                                viewModel.getCommentsForChapter(chapter.id)
                            }
                            val chapterComments by commentsFlow.collectAsState(initial = emptyList())
                            val commentsCount = chapterComments.size

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isLocked) {
                                        onNavigateToDiscussion(chapter.id, chapter.titulo)
                                    }
                            ) {
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isCurrent) {
                                                Modifier
                                                    .background(Terracota.copy(alpha = 0.06f))
                                                    .drawBehind {
                                                        drawRect(
                                                            color = Terracota,
                                                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                                                        )
                                                    }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(horizontal = 24.dp, vertical = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isCompleted) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(VerdeMusgo, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = "Concluído",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .border(1.5.dp, Terracota, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Empty inner circle
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Lock,
                                                    contentDescription = "Bloqueado",
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "Capítulo $chapNumber",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLocked) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            )
                                            Text(
                                                text = if (isLocked) "Chega aqui pra liberar" else chapter.titulo,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = if (isLocked) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            )
                                        }
                                    }

                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFDE1D8), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Atual",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Terracota,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    } else if (isCompleted) {
                                        Text(
                                            text = if (commentsCount > 0) "$commentsCount comentários" else "—",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Estatísticas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Tempo de leitura",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1h 45m",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Velocidade média",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "25 pág/h",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
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

// --- PROFILE TAB ---
@Composable
fun ProfileScreenTab(
    viewModel: MainViewModel,
    onLogoutCompleted: () -> Unit,
    onNavigateToTab: (String) -> Unit,
    onNavigateToJoinClub: () -> Unit
) {
    val name by viewModel.userName.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val allClubs by viewModel.allClubs.collectAsState()
    val activeClub by viewModel.activeClub.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
                        // Large circular avatar profile
                        MemberAvatar(name = name ?: "Você", avatarUrl = currentUser?.avatarUrl ?: "", size = 72.dp)
                        Column {
                            Text(
                                text = name ?: "Usuário",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp)
                            )
                            Text(
                                text = email ?: "contato@tramabook.com",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Stat Cards side-by-side
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                    val statsBg = if (darkTheme) Color(0xFF2A2520) else Color(0xFFFBF7EE)

                    // Card 1: Livros lidos
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = statsBg),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "24",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = FrauncesFontFamily,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Terracota
                                )
                            )
                            Text(
                                text = "livros lidos",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Card 2: Clubes ativos
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = statsBg),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "3",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = FrauncesFontFamily,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Terracota
                                )
                            )
                            Text(
                                text = "clubes ativos",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Teus Clubes Section
            item {
                SectionHeader(title = "Teus clubes")
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allClubs.forEach { club ->
                        val isActive = club.id == activeClub?.id
                        val clubReadingText = if (club.id == "club_tramabook") {
                            "Lendo: A Metamorfose"
                        } else if (club.id == "club_filosofia") {
                            "Lendo: O Mito de Sísifo"
                        } else {
                            "Sem livro atual"
                        }

                        val avatarColor = remember(club.id) {
                            val hash = club.id.hashCode()
                            when (kotlin.math.abs(hash) % 3) {
                                0 -> VerdeMusgo
                                1 -> Terracota
                                else -> Color(0xFFD4A373)
                            }
                        }

                        StandardCard(
                            onClick = {
                                viewModel.selectActiveClub(club.id)
                                onNavigateToTab("home")
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(avatarColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = club.nome.take(1).uppercase(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = club.nome,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = FrauncesFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = clubReadingText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = InterFontFamily,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .background(VerdeMusgo.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "atual",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = VerdeMusgo,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowRight,
                                        contentDescription = "Selecionar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Join other club outlined button
                    OutlinedButton(
                        onClick = onNavigateToJoinClub,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.5.dp, Terracota)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Entrar em outro clube",
                            tint = Terracota,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Entrar em outro clube",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Terracota
                            )
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Tema e preferências")
                StandardCard {
                    Text(
                        text = "Aparência",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom segmented control for Theme selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f),
                                RoundedCornerShape(24.dp)
                            )
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val themeModes = listOf("system" to "Sistema", "light" to "Claro", "dark" to "Escuro")
                        themeModes.forEach { (mode, label) ->
                            val isSelected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (isSelected) Terracota else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { viewModel.updateThemeMode(mode) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Terracota)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ExitToApp,
                        contentDescription = "Sair",
                        tint = Terracota
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Sair da conta",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = Terracota)
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Deseja sair?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    "Tem certeza que deseja desconectar da sua conta?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout {
                            onLogoutCompleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                ) {
                    Text("Sair", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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

    val avatars = listOf(
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCVScro7b5L7FyxSBjNpeqetGOxXZcJe5_EViRuBb5j15OIqZzjjFE8AD5HxgnDcV__koM3NJtsawXA84KY9YNkGFN7fhPvCmJozzDXIkaDWzjObrvzqA2QOSHYCkvK6No2M6UEtsJXEoOaqY7O0WDiVtrhyaKZIqMxGEdP732KB_qtc7_tWeZHNZ9WEOJp6PTJnWMO-kidNZ_0LEvCMirIjMy140n059Elt4YwhfPZbjqKivR3NRgIsXyLxp8THGS41Y3roxiIJS8",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=120",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=120",
        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=120",
        "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=120"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Alterar dados do perfil",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )
        }

        item {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                MemberAvatar(name = name.ifEmpty { "Você" }, avatarUrl = avatarUrl, size = 96.dp)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Terracota, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Selecionar Foto",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "Escolha um avatar",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                avatars.forEach { url ->
                    val isSelected = avatarUrl == url
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Terracota else Color.Transparent,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable { avatarUrl = url }
                    ) {
                        MemberAvatar(name = "User", avatarUrl = url, size = 48.dp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome", color = Terracota) },
                placeholder = { Text("Seu nome de leitor") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Terracota,
                    focusedLabelColor = Terracota,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }

        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail", color = Terracota) },
                placeholder = { Text("exemplo@email.com") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Terracota,
                    focusedLabelColor = Terracota,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Terracota)
                ) {
                    Text("Cancelar", color = Terracota, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }

                Button(
                    onClick = {
                        if (name.isNotBlank() && email.isNotBlank()) {
                            onSave(name, email, avatarUrl)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Terracota,
                        contentColor = Color.White
                    )
                ) {
                    Text("Salvar", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
