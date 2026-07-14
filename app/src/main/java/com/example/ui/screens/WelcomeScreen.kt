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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Pill
import com.example.ui.components.PillVariant
import com.example.ui.components.staggeredEntrance
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.RodapeCard
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeIcons
import com.example.ui.theme.Cream
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ClubColors
import com.example.ui.theme.Divider
import com.example.ui.theme.Ink
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.Muted
import com.example.ui.theme.Oliva
import com.example.ui.theme.OlivaDark
import com.example.ui.theme.OlivaDeep
import com.example.ui.theme.OlivaSoft
import com.example.ui.theme.Paper
import com.example.ui.theme.PaperDeep
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.Terracota
import com.example.ui.theme.Tertiary
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    // B1: convidado cola o código aqui (antes de criar conta); o código é retido
    // e o join acontece automaticamente depois da auth.
    onNavigateWithInvite: (String) -> Unit = {},
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }
    // Design: claude-design/screens-onboarding.jsx (WelcomeScreen). Layout editorial
    // alinhado à esquerda + painel oliva curvado com lombadas de livro. Mantém
    // "Rodapé" (design diz "tramabook") e voz "você". Os CTAs são de AUTH porque o
    // app exige login antes de criar/entrar em clube; "Criar um clube"/"Entrar num
    // clube" acontecem DEPOIS, no estado vazio dentro do app.
    Scaffold(containerColor = RodapeTheme.colors.paper) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Wordmark (topo-esquerda) ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo do Rodapé",
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Rodapé",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = RodapeTheme.colors.ink
                    )
                )
            }

            // ── Hero (alinhado à esquerda) ───────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 28.dp, end = 28.dp, top = 44.dp)
            ) {
                // Entrada encenada do hero: pílula → headline → subhead (stagger).
                Box(modifier = Modifier.staggeredEntrance(index = 0)) {
                    Pill(text = "Clubes de leitura", variant = PillVariant.OliveDeep)
                }
                Spacer(modifier = Modifier.height(18.dp))
                // Cores hoistadas: buildAnnotatedString é lambda não-composable.
                val olivaColor = RodapeTheme.colors.oliva
                val terracotaColor = RodapeTheme.colors.terracota
                Text(
                    text = buildAnnotatedString {
                        append("Leituras\n")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = olivaColor)) { append("juntas") }
                        withStyle(SpanStyle(color = terracotaColor)) { append(".") }
                    },
                    modifier = Modifier.staggeredEntrance(index = 1),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 54.sp,
                        lineHeight = 54.sp,
                        letterSpacing = (-1.8).sp,
                        color = RodapeTheme.colors.ink
                    )
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Um clube. Um livro. Conversa que não dá spoiler.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = RodapeTheme.colors.muted
                    ),
                    modifier = Modifier
                        .widthIn(max = 290.dp)
                        .staggeredEntrance(index = 2)
                )
            }

            // ── Painel oliva curvado + lombadas + CTAs de auth ───────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RodapeTheme.colors.olivaDeep, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            ) {
                // Lombadas atravessando a curva (sobem acima da borda do painel).
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-24).dp, y = (-68).dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // "Livros pousando na prateleira": cada lombada sobe em stagger
                    // depois do hero (índices seguem pílula/headline/subhead).
                    Box(modifier = Modifier.staggeredEntrance(index = 3)) { BookSpine(80.dp, Color(0xFFD9C9B0)) }
                    Box(modifier = Modifier.staggeredEntrance(index = 4)) { BookSpine(110.dp, RodapeTheme.colors.terracota) }
                    Box(modifier = Modifier.staggeredEntrance(index = 5)) { BookSpine(92.dp, RodapeTheme.colors.cream, stroke = true) }
                    Box(modifier = Modifier.staggeredEntrance(index = 6)) { BookSpine(72.dp, RodapeTheme.colors.oliva) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 38.dp, bottom = 40.dp)
                ) {
                    // Linha de "prateleira" no topo do painel.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(RodapeTheme.colors.cream.copy(alpha = 0.10f))
                    )
                    Spacer(modifier = Modifier.height(26.dp))

                    // CTA primário: Criar conta (terra) + seta.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(RodapeRadii.full))
                            .background(RodapeTheme.colors.terracota)
                            .clickable { onNavigateToSignUp() }
                            // C4: TalkBack anuncia "botão" (era Box.clickable mudo).
                            .semantics { role = Role.Button },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Criar conta",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = RodapeTheme.colors.cream
                                )
                            )
                            Icon(
                                imageVector = RodapeIcons.Arrow,
                                contentDescription = null,
                                tint = RodapeTheme.colors.cream,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // CTA secundário: Entrar (contorno creme sobre o oliva).
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(RodapeRadii.full))
                            .border(1.dp, RodapeTheme.colors.cream.copy(alpha = 0.25f), RoundedCornerShape(RodapeRadii.full))
                            .clickable { onNavigateToLogin() }
                            .semantics { role = Role.Button },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Entrar",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = RodapeTheme.colors.cream
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // B1: caminho do convidado — cola o código já aqui (o entrante
                    // mais comum de um clube privado veio pra "entrar", não "criar").
                    Text(
                        text = "Tenho um convite",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = RodapeTheme.colors.cream.copy(alpha = 0.85f)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RodapeRadii.full))
                            .clickable { showInviteDialog = true }
                            .semantics { role = Role.Button }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            containerColor = RodapeTheme.colors.cream,
            title = {
                Text(
                    "Entrar com convite",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = RodapeTheme.colors.ink
                    )
                )
            },
            text = {
                Column {
                    Text(
                        "Cole o código que o organizador te passou. No próximo passo você cria a conta e já entra no clube.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            color = RodapeTheme.colors.muted
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.take(12) },
                        label = { Text("Código do convite") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RodapeTheme.colors.terracota,
                            focusedLabelColor = RodapeTheme.colors.terracota,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = inviteCode.trim().length >= 4,
                    onClick = {
                        showInviteDialog = false
                        onNavigateWithInvite(inviteCode.trim())
                    }
                ) {
                    Text("Continuar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancelar", color = RodapeTheme.colors.muted)
                }
            }
        )
    }
}

/** Lombada de livro decorativa (ilustração da Welcome). Ref: screens-onboarding.jsx BookSpine. */
@Composable
private fun BookSpine(height: Dp, color: Color, stroke: Boolean = false) {
    val shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(height)
            .clip(shape)
            .background(color)
            .then(if (stroke) Modifier.border(1.dp, Color.Black.copy(alpha = 0.06f), shape) else Modifier)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(0.4f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Black.copy(alpha = 0.15f))
            )
            Spacer(modifier = Modifier.weight(0.6f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onSignInWithEmail: suspend (email: String, password: String) -> Result<Unit>,
    onSignInWithGoogle: suspend () -> Result<Unit>,
    onSignedIn: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val passwordFocusRequester = remember { FocusRequester() }

    val isEmailValid = email.contains("@") && email.length >= 5
    // B5: o login só valida a conta no servidor — não deve impor uma regra de tamanho
    // (o "mín. 6" divergia do cadastro "8 forte" e podia barrar senha legítima).
    val isPasswordValid = password.isNotEmpty()
    val isFormValid = isEmailValid && isPasswordValid

    val submitLogin: () -> Unit = {
        if (isFormValid && !isLoading) {
            isLoading = true
            errorMsg = null
            scope.launch {
                val result = onSignInWithEmail(email, password)
                isLoading = false
                result.fold(
                    onSuccess = { onSignedIn() },
                    onFailure = { errorMsg = com.example.ui.auth.AuthErrors.friendly(it, "Falha ao entrar") },
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = RodapeIcons.Back,
                            contentDescription = "Voltar",
                            tint = RodapeTheme.colors.ink
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RodapeTheme.colors.paper)
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
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Design: screens-onboarding.jsx (LoginScreen) — headline serif à
                    // esquerda, sem cartão. "Oi de novo." + subtítulo.
                    Text(
                        text = "Oi de novo.",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 36.sp,
                            letterSpacing = (-0.8).sp,
                            color = RodapeTheme.colors.ink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sua leitura está esperando você.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = RodapeTheme.colors.muted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim(); errorMsg = null },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text("Senha") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                    tint = RodapeTheme.colors.muted,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
                        enabled = !isLoading,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        modifier = Modifier.align(Alignment.End),
                        enabled = !isLoading,
                    ) {
                        Text("Esqueci minha senha", color = RodapeTheme.colors.terracota)
                    }

                    errorMsg?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                        )
                    }

                    // Explica por que "Entrar" fica desabilitado, em vez de um botão
                    // cinza mudo.
                    val faltamLogin = buildList {
                        if (!isEmailValid) add("um email válido")
                        if (!isPasswordValid) add("a senha")
                    }
                    if (faltamLogin.isNotEmpty() && (email.isNotEmpty() || password.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Falta: " + faltamLogin.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { submitLogin() },
                        enabled = isFormValid && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(RodapeRadii.full),
                        colors = ButtonDefaults.buttonColors(containerColor = RodapeTheme.colors.terracota),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text(
                                "Entrar",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Aviso sutil pra explicar o comportamento padrao de sessao
                    // persistente — o Supabase mantem a sessao no SessionStorage
                    // ate logout explicito, e o user quer saber disso sem ter
                    // que clicar num checkbox "lembrar de mim" decorativo.
                    Text(
                        text = "Você continuará conectado nesse dispositivo.",
                        style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    // Botao oficial Google Sign-In seguindo branding guidelines:
                    // fundo branco, borda fina cinza, logo G multi-color, texto preto
                    // em Roboto Medium. https://developers.google.com/identity/branding-guidelines
                    OutlinedButton(
                        onClick = {
                            isLoading = true
                            errorMsg = null
                            scope.launch {
                                val result = onSignInWithGoogle()
                                isLoading = false
                                result.fold(
                                    onSuccess = { onSignedIn() },
                                    onFailure = { errorMsg = com.example.ui.auth.AuthErrors.friendly(it, "Falha no Google Sign-In") },
                                )
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(RodapeRadii.full),
                        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1F1F1F),
                        ),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_g_logo),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Continuar com Google",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F1F1F),
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToSignUp, enabled = !isLoading) {
                        Text("Ainda não tem conta? Cadastre-se", color = RodapeTheme.colors.olivaDark)
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClubScreen(
    onNavigateBack: () -> Unit,
    // O segundo callback recebe (onError: (String) -> Unit) pra UI poder
    // sair do estado de loading e mostrar a mensagem.
    onCreateCompleted: (nome: String, descricao: String, cor: String, privacidade: String, onError: (String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("convidados") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val isNameValid = name.trim().length >= 3

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Novo clube",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
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

                // Input card aligned to design system
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RodapeRadii.md),
                    color = RodapeTheme.colors.cardSurface,
                    border = BorderStroke(1.dp, RodapeTheme.colors.divider),
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
                                    color = RodapeTheme.colors.ink
                                )
                            )
                            Text(
                                text = "${name.length}/40",
                                style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                            )
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 40) name = it },
                            placeholder = {
                                Text(
                                    "Nome do clube (máx. 40)",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = RodapeTheme.colors.muted
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RodapeRadii.sm),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RodapeTheme.colors.terracota,
                                unfocusedBorderColor = RodapeTheme.colors.divider,
                                focusedContainerColor = RodapeTheme.colors.cream,
                                unfocusedContainerColor = RodapeTheme.colors.cream
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Conte um pouco",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = RodapeTheme.colors.ink
                                )
                            )
                            Text(
                                text = "${description.length}/140",
                                style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                            )
                        }
                        OutlinedTextField(
                            value = description,
                            onValueChange = { if (it.length <= 140) description = it },
                            placeholder = {
                                Text(
                                    "Descrição curta",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = RodapeTheme.colors.muted
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            shape = RoundedCornerShape(RodapeRadii.sm),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RodapeTheme.colors.terracota,
                                unfocusedBorderColor = RodapeTheme.colors.divider,
                                focusedContainerColor = RodapeTheme.colors.cream,
                                unfocusedContainerColor = RodapeTheme.colors.cream
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
                    ClubColors.forEachIndexed { index, clubColor ->
                        val isSelected = selectedColorIndex == index
                        val colorName = clubColorLabel(clubColor.id)
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) RodapeTheme.colors.ink else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                                .background(clubColor.bg, CircleShape)
                                .clip(CircleShape)
                                // A11y: swatch sem rótulo é inacessível pra cego.
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { selectedColorIndex = index },
                                )
                                .semantics { contentDescription = "Cor $colorName" }
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
                        shape = RoundedCornerShape(RodapeRadii.md),
                        color = if (isOption1) RodapeTheme.colors.terracota.copy(alpha = 0.04f) else RodapeTheme.colors.cardSurface,
                        border = BorderStroke(
                            width = if (isOption1) 1.5.dp else 1.dp,
                            color = if (isOption1) RodapeTheme.colors.terracota else RodapeTheme.colors.divider
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
                                        if (isOption1) RodapeTheme.colors.terracota.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = RodapeIcons.Lock,
                                    contentDescription = null,
                                    tint = if (isOption1) RodapeTheme.colors.terracota else MaterialTheme.colorScheme.onSurfaceVariant
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
                                        .background(RodapeTheme.colors.terracota, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = RodapeIcons.Check,
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
                        shape = RoundedCornerShape(RodapeRadii.md),
                        color = if (isOption2) RodapeTheme.colors.terracota.copy(alpha = 0.04f) else RodapeTheme.colors.cardSurface,
                        border = BorderStroke(
                            width = if (isOption2) 1.5.dp else 1.dp,
                            color = if (isOption2) RodapeTheme.colors.terracota else RodapeTheme.colors.divider
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
                                        if (isOption2) RodapeTheme.colors.terracota.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = RodapeIcons.Share,
                                    contentDescription = null,
                                    tint = if (isOption2) RodapeTheme.colors.terracota else MaterialTheme.colorScheme.onSurfaceVariant
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
                                        .background(RodapeTheme.colors.terracota, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = RodapeIcons.Check,
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
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
                if (!isNameValid && name.isNotEmpty()) {
                    Text(
                        text = "O nome do clube precisa de pelo menos 3 letras.",
                        style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
                TbButton(
                    text = if (isSubmitting) "Criando…" else "Criar clube",
                    onClick = {
                        if (isNameValid && !isSubmitting) {
                            isSubmitting = true
                            errorMsg = null
                            onCreateCompleted(
                                name, description, selectedColorIndex.toString(), privacy
                            ) { msg ->
                                isSubmitting = false
                                errorMsg = msg
                            }
                        }
                    },
                    variant = TbButtonVariant.Terra,
                    size = TbButtonSize.Lg,
                    enabled = isNameValid && !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
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
    var codeErrorMsg by remember { mutableStateOf<String?>(null) }
    // Trava o submit durante o request — sem isso, toque duplo = dois joins.
    var isJoining by remember { mutableStateOf(false) }

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
                            fontWeight = FontWeight.Bold
                        )
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Título da seção. O convite por link foi adiado até ter domínio
                // próprio, então aqui só entra por código — isto é um rótulo, não um
                // botão/aba (o visual de pill anterior sugeria um toque que não existia).
                Text(
                    "Com código",
                    color = RodapeTheme.colors.ink,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RodapeRadii.md),
                    color = RodapeTheme.colors.cardSurface,
                    border = BorderStroke(1.dp, RodapeTheme.colors.divider),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (i in 0..5) {
                                    OutlinedTextField(
                                        value = otpValues[i],
                                        onValueChange = { value ->
                                            when {
                                                // Colar o codigo inteiro (ou varios chars):
                                                // distribui a partir da caixa atual.
                                                value.length > 1 -> {
                                                    val chars = value.filter { !it.isWhitespace() }.take(6 - i)
                                                    chars.forEachIndexed { offset, c ->
                                                        otpValues[i + offset] = c.toString()
                                                    }
                                                    val nextFocus = (i + chars.length).coerceAtMost(5)
                                                    focusRequesters[nextFocus].requestFocus()
                                                }
                                                // Backspace numa caixa ja vazia: volta pra anterior.
                                                value.isEmpty() && otpValues[i].isEmpty() && i > 0 -> {
                                                    focusRequesters[i - 1].requestFocus()
                                                }
                                                value.length <= 1 -> {
                                                    otpValues[i] = value
                                                    if (value.isNotEmpty() && i < 5) {
                                                        focusRequesters[i + 1].requestFocus()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .focusRequester(focusRequesters[i])
                                            // A11y: sem isso o TalkBack anuncia "caixa de
                                            // edição" 6x sem contexto do código de convite.
                                            .semantics {
                                                contentDescription = "Dígito ${i + 1} de 6 do código de convite"
                                            },
                                        shape = RoundedCornerShape(RodapeRadii.sm),
                                        placeholder = {
                                            Text(
                                                "-",
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = RodapeTheme.colors.terracota,
                                            unfocusedBorderColor = RodapeTheme.colors.divider,
                                            focusedContainerColor = RodapeTheme.colors.cream,
                                            unfocusedContainerColor = RodapeTheme.colors.cream
                                        )
                                    )
                                    if (i == 2) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
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

                        Spacer(modifier = Modifier.height(16.dp))

                        TbButton(
                            text = if (isJoining) "Entrando…" else "Confirmar",
                            onClick = {
                                if (isCodeComplete) {
                                    isJoining = true
                                    codeErrorMsg = null
                                    onJoinWithCodeSubmit(fullOtpCode) { success, errorMsg ->
                                        isJoining = false
                                        if (!success) {
                                            codeErrorMsg = errorMsg
                                        }
                                    }
                                }
                            },
                            variant = TbButtonVariant.Terra,
                            size = TbButtonSize.Lg,
                            enabled = !isJoining && isCodeComplete,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Não tenho código",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = RodapeTheme.colors.terracota,
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

// Nome acessível de cada cor de clube (para TalkBack nos swatches).
private fun clubColorLabel(id: String): String = when (id) {
    "olive" -> "verde oliva"
    "terracotta" -> "terracota"
    "plum" -> "ameixa"
    "mustard" -> "mostarda"
    "ink" -> "azul-tinta"
    else -> id
}
