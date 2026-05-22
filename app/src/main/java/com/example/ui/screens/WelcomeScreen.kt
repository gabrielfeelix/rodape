package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PillButton
import com.example.ui.components.StandardCard
import com.example.ui.theme.Terracota
import com.example.ui.theme.VerdeMusgo
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToCreateClub: () -> Unit,
    onNavigateToJoinClub: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Logo view
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo representation
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo do Tramabook",
                        modifier = Modifier.size(108.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Leituras juntas.",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 28.sp,
                            color = Terracota
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Um clube. Um livro. Conversa que não dá spoiler.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp)
                    )
                }
            }

            // Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PillButton(
                    text = "Criar um clube",
                    onClick = onNavigateToCreateClub
                )

                PillButton(
                    text = "Entrar num clube",
                    onClick = onNavigateToJoinClub,
                    isSecondary = true
                )

                Text(
                    text = "Já tenho conta",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = Terracota
                    ),
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(vertical = 12.dp, horizontal = 24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: (name: String, email: String) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isEmailValid = email.contains("@") && email.length >= 5
    val isPasswordValid = password.length >= 6
    val isNameValid = !isSignUp || name.trim().length >= 2
    val isFormValid = isEmailValid && isPasswordValid && isNameValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tramabook", style = MaterialTheme.typography.headlineLarge.copy(color = Terracota)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Terracota
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                StandardCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSignUp) "Criar conta" else "Bem-vindo de volta",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isSignUp) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Terracota.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, Terracota.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountCircle,
                                    contentDescription = "Dica",
                                    tint = Terracota,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Dica: Para ver o app cheio de conteúdo (Clubes, discussões, progresso), use o botão de atalho abaixo ou digite o email: voce@tramabook.com",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Terracota, fontWeight = FontWeight.Medium),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Segmented control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f),
                                RoundedCornerShape(26.dp)
                            )
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                RoundedCornerShape(26.dp)
                            )
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (!isSignUp) Terracota else Color.Transparent,
                                    RoundedCornerShape(22.dp)
                                )
                                .clickable { isSignUp = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Entrar",
                                color = if (!isSignUp) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (isSignUp) Terracota else Color.Transparent,
                                    RoundedCornerShape(22.dp)
                                )
                                .clickable { isSignUp = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Criar conta",
                                color = if (isSignUp) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isSignUp) {
                        Text(
                            text = "Nome",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Seu nome") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = "Email",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Seu email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Senha",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Mínimo 6 caracteres") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (!isSignUp) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                "Esqueci minha senha",
                                style = MaterialTheme.typography.labelSmall.copy(color = Terracota),
                                modifier = Modifier.clickable { }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PillButton(
                        text = if (isSignUp) "Criar conta" else "Entrar",
                        onClick = {
                            if (isFormValid) {
                                val resolvedName = if (isSignUp) name else email.substringBefore("@")
                                onLoginSuccess(resolvedName, email)
                            }
                        },
                        enabled = isFormValid
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "ou",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onLoginSuccess("Você", "voce@tramabook.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(28.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Entrar na Conta de Teste (Completa)",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { onLoginSuccess("Convidado Google", "demo@google.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, Terracota)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Google Icon",
                            tint = Terracota,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continuar com Google (Novo)",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Terracota
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClubScreen(
    onNavigateBack: () -> Unit,
    onCreateCompleted: (nome: String, descricao: String, cor: String, privacidade: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("convidados") }
    var selectedColorIndex by remember { mutableStateOf(0) }

    val colors = listOf(
        Color(0xFF8C4027), // Terracota from screenshot
        Color(0xFF4C663D), // Green from screenshot
        Color(0xFF5A5852), // Warm dark gray from screenshot
        Color(0xFFFDE1D8), // Pastel peach from screenshot
        Color(0xFF7A7973)  // Secondary slate gray from screenshot
    )

    val isNameValid = name.trim().length >= 3

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Novo clube", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                        )
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Standard white card container from the screenshot
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Como vocês querem chamar?",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "${name.length}/40",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 40) name = it },
                            placeholder = { 
                                Text(
                                    "Nome do clube (máx. 40)", 
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Terracota,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Conta um pouco",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "${description.length}/140",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        OutlinedTextField(
                            value = description,
                            onValueChange = { if (it.length <= 140) description = it },
                            placeholder = { 
                                Text(
                                    "Descrição curta", 
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Terracota,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Cor do clube",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    colors.forEachIndexed { index, color ->
                        val isSelected = selectedColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                                .background(color, CircleShape)
                                .clip(CircleShape)
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Quem pode entrar?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Selected if privacy == "convidados"
                    val isOption1 = privacy == "convidados"
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { privacy = "convidados" },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isOption1) Terracota.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isOption1) 1.5.dp else 0.5.dp,
                            color = if (isOption1) Terracota else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isOption1) Terracota.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (isOption1) Terracota else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Só convidados",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isOption1) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Terracota, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selecionado",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Option 2: Selected if privacy == "publico"
                    val isOption2 = privacy == "publico"
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { privacy = "publico" },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isOption2) Terracota.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isOption2) 1.5.dp else 0.5.dp,
                            color = if (isOption2) Terracota else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isOption2) Terracota.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    tint = if (isOption2) Terracota else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Aberto a quem tem link",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isOption2) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Terracota, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selecionado",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (isNameValid) {
                            val chosenColorHex = when (selectedColorIndex) {
                                0 -> "#8C4027"
                                1 -> "#4C663D"
                                2 -> "#5A5852"
                                3 -> "#FDE1D8"
                                else -> "#7A7973"
                            }
                            onCreateCompleted(name, description, chosenColorHex, privacy)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isNameValid,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Terracota,
                        contentColor = Color.White,
                        disabledContainerColor = Terracota.copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = "Criar clube",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinClubScreen(
    onNavigateBack: () -> Unit,
    onJoinWithCodeSubmit: (String, (Boolean, String?) -> Unit) -> Unit
) {
    var activeTabIsCode by remember { mutableStateOf(true) }
    var linkInput by remember { mutableStateOf("") }
    var codeErrorMsg by remember { mutableStateOf<String?>(null) }
    var linkErrorMsg by remember { mutableStateOf<String?>(null) }

    // Code entries: 6 characters
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    val fullOtpCode = otpValues.joinToString("").uppercase().trim()
    val isCodeComplete = fullOtpCode.length == 6

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Entrar num clube", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                        )
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            CustomBottomBar(
                selectedTab = "home",
                onTabSelected = { }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Segmented Control Com código / Com link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f),
                            RoundedCornerShape(26.dp)
                        )
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            RoundedCornerShape(26.dp)
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (activeTabIsCode) Color.White else Color.Transparent,
                                RoundedCornerShape(22.dp)
                            )
                            .clickable { activeTabIsCode = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Com código",
                            color = if (activeTabIsCode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (!activeTabIsCode) Color.White else Color.Transparent,
                                RoundedCornerShape(22.dp)
                            )
                            .clickable { activeTabIsCode = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Com link",
                            color = if (!activeTabIsCode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (activeTabIsCode) {
                            Text(
                                text = "Peça o código pro organizador",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                            ) {
                                for (i in 0..2) {
                                    OutlinedTextField(
                                        value = otpValues[i],
                                        onValueChange = { value ->
                                            if (value.length <= 1) {
                                                otpValues[i] = value
                                                if (value.isNotEmpty() && i < 5) {
                                                    focusRequesters[i + 1].requestFocus()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .width(44.dp)
                                            .height(56.dp)
                                            .focusRequester(focusRequesters[i]),
                                        shape = RoundedCornerShape(12.dp),
                                        placeholder = {
                                            Text(
                                                "-",
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Terracota,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                }

                                Text("-", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                                for (i in 3..5) {
                                    OutlinedTextField(
                                        value = otpValues[i],
                                        onValueChange = { value ->
                                            if (value.length <= 1) {
                                                otpValues[i] = value
                                                if (value.isNotEmpty() && i < 5) {
                                                    focusRequesters[i + 1].requestFocus()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .width(44.dp)
                                            .height(56.dp)
                                            .focusRequester(focusRequesters[i]),
                                        shape = RoundedCornerShape(12.dp),
                                        placeholder = {
                                            Text(
                                                "-",
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Terracota,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                }
                            }

                            if (codeErrorMsg != null) {
                                Text(
                                    text = codeErrorMsg!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = "Cole o link do clube abaixo",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = linkInput,
                                onValueChange = { linkInput = it },
                                placeholder = {
                                    Text(
                                        "https://tramabook.com/c/...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Terracota,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )

                            if (linkErrorMsg != null) {
                                Text(
                                    text = linkErrorMsg!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (activeTabIsCode) {
                                    if (isCodeComplete) {
                                        onJoinWithCodeSubmit(fullOtpCode) { success, errorMsg ->
                                            if (!success) {
                                                codeErrorMsg = errorMsg
                                            }
                                        }
                                    }
                                } else {
                                    val token = linkInput.trim()
                                    val cleanedCode = if (token.contains("/c/")) {
                                        token.substringAfter("/c/").substringBefore("/").trim().uppercase().take(6)
                                    } else if (token.contains("/club/")) {
                                        token.substringAfter("/club/").substringBefore("/").trim().uppercase().take(6)
                                    } else {
                                        token.substringAfterLast("/").trim().uppercase().take(6)
                                    }

                                    if (cleanedCode.isNotEmpty()) {
                                        onJoinWithCodeSubmit(cleanedCode) { success, errorMsg ->
                                            if (!success) {
                                                linkErrorMsg = errorMsg
                                            }
                                        }
                                    } else {
                                        linkErrorMsg = "Link inválido. Insira um link válido do Tramabook."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = if (activeTabIsCode) isCodeComplete else linkInput.trim().isNotEmpty(),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Terracota,
                                contentColor = Color.White,
                                disabledContainerColor = Terracota.copy(alpha = 0.4f),
                                disabledContentColor = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = "Confirmar",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Não tenho código",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Terracota,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .clickable { onNavigateBack() }
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
