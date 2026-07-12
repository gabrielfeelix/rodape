package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.remote.AuthRepository
import com.example.data.remote.Supabase
import com.example.ui.auth.GoogleSignInHelper
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Supabase.client.handleDeeplinks(intent)
        // Triggering fresh compilation and preview deployment for the user
        enableEdgeToEdge()
        setContent {
            val fontScale by viewModel.fontScale.collectAsState()
            val baseDensity = LocalDensity.current
            // Acessibilidade: COMBINA a escala do sistema (config "Tamanho da fonte"
            // do Android, essencial pra baixa visão) com o ajuste interno, em vez de
            // substituí-la. Antes o app descartava baseDensity.fontScale e ignorava
            // a preferência do sistema.
            val scaledDensity = Density(
                density = baseDensity.density,
                fontScale = baseDensity.fontScale * fontScale
            )
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val sessionStatus by viewModel.sessionStatus.collectAsState()

                    // Guarda one-shot do deep link de recovery. Sem isso, cada
                    // re-emissao do StateFlow (re-subscribe / cold start) re-arrancava
                    // o usuario pra reset_password e limpava a back stack (popUpTo(0)).
                    // So volta a false quando a sessao vira NotAuthenticated — assim um
                    // novo link de recovery futuro ainda funciona.
                    var recoveryConsumed by rememberSaveable { mutableStateOf(false) }

                    // SEMPRE iniciar em welcome — esse e estado neutro/seguro. Se a sessao
                    // ja vier autenticada (cold start com sessao no storage), o LaunchedEffect
                    // abaixo redireciona pra main_tabs. Isso evita o bug onde:
                    //  1. App abre, Supabase ainda esta `Initializing` (lendo session do disco)
                    //  2. startDestination calculado nessa hora vira "welcome" (userId=null)
                    //  3. Session termina de carregar -> volta pra Authenticated, mas
                    //     NavHost ja montou em welcome e nao reage a mudancas no startDest.
                    // Tambem evita race entre logout e recompose.

                    LaunchedEffect(sessionStatus) {
                        val s = sessionStatus
                        when {
                            s is SessionStatus.Authenticated && s.session.type == "recovery" -> {
                                if (!recoveryConsumed) {
                                    recoveryConsumed = true
                                    navController.navigate("reset_password") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            s is SessionStatus.Authenticated -> {
                                // Logado: garante que esta em main_tabs (idempotente).
                                val current = navController.currentBackStackEntry?.destination?.route
                                if (current == "welcome" || current == null) {
                                    navController.navigate("main_tabs") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            s is SessionStatus.NotAuthenticated -> {
                                // Deslogado: libera o guard pra um futuro link de recovery.
                                recoveryConsumed = false
                                // garante que esta em welcome.
                                val current = navController.currentBackStackEntry?.destination?.route
                                if (current != null && current != "welcome" && current != "login" &&
                                    current != "signup" && current != "forgot_password" && current != "reset_password") {
                                    navController.navigate("welcome") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            // Initializing: nao faz nada, espera resolver.
                            else -> {}
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "welcome"
                    ) {
                    composable("welcome") {
                        WelcomeScreen(
                            onNavigateToLogin = { navController.navigate("login") },
                            onNavigateToSignUp = { navController.navigate("signup") },
                        )
                    }

                    composable("login") {
                        val ctx = LocalContext.current
                        val authRepo = remember { AuthRepository() }
                        val google = remember { GoogleSignInHelper(ctx) }
                        LoginScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSignUp = { navController.navigate("signup") },
                            onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                            onSignInWithEmail = { email, password ->
                                runCatching { authRepo.signInWithEmail(email, password) }
                            },
                            onSignInWithGoogle = {
                                runCatching {
                                    val token = google.getGoogleIdToken()
                                    authRepo.signInWithGoogleIdToken(token.idToken, token.rawNonce)
                                    Unit
                                }
                            },
                            onSignedIn = {
                                navController.navigate("main_tabs") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                        )
                    }

                    composable("signup") {
                        val ctx = LocalContext.current
                        val authRepo = remember { AuthRepository() }
                        val google = remember { GoogleSignInHelper(ctx) }
                        SignUpScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSignUp = { email, password, name ->
                                runCatching { authRepo.signUpWithEmail(email, password, name) }
                            },
                            onSignInWithGoogle = {
                                runCatching {
                                    val token = google.getGoogleIdToken()
                                    authRepo.signInWithGoogleIdToken(token.idToken, token.rawNonce)
                                    Unit
                                }
                            },
                            onSignedUp = {
                                // popBackStack(route="login") falha silenciosamente se o
                                // usuario veio Welcome -> SignUp (sem passar por Login).
                                // navigate explicito popUpTo welcome resolve nos dois caminhos.
                                navController.navigate("login") {
                                    popUpTo("welcome") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("welcome") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onGoogleSignedIn = {
                                navController.navigate("main_tabs") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                        )
                    }

                    composable("forgot_password") {
                        val authRepo = remember { AuthRepository() }
                        ForgotPasswordScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSendReset = { email -> runCatching { authRepo.sendPasswordResetEmail(email) } },
                        )
                    }

                    composable("reset_password") {
                        val authRepo = remember { AuthRepository() }
                        val resetScope = rememberCoroutineScope()
                        ResetPasswordScreen(
                            onUpdatePassword = { newPassword -> runCatching { authRepo.updatePassword(newPassword) } },
                            onPasswordUpdated = {
                                navController.navigate("main_tabs") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                            onCancel = {
                                // Sai do fluxo de recovery: encerra a sessao de recovery
                                // e volta pro welcome (estado neutro/seguro).
                                resetScope.launch { runCatching { authRepo.signOut() } }
                                navController.navigate("welcome") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable("create_club") {
                        CreateClubScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onCreateCompleted = { nome, descricao, cor, privacidade, onError ->
                                viewModel.createClub(
                                    nome, descricao, cor, privacidade,
                                    onCompleted = { _ ->
                                        // popUpTo na propria rota corrente + inclusive=true remove
                                        // create_club do back stack e cai em main_tabs (que sempre
                                        // existe). NAO usar popUpTo("welcome") — falha silenciosa
                                        // quando welcome nao esta no back stack (usuario logado
                                        // direto via cold start) e causa comportamento bizarro.
                                        navController.navigate("main_tabs") {
                                            popUpTo("create_club") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onError = onError,
                                )
                            }
                        )
                    }

                    composable("join_club") {
                        JoinClubScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onJoinWithCodeSubmit = { code, onResult ->
                                viewModel.joinClubWithCode(code) { success, errorMsg ->
                                    onResult(success, errorMsg)
                                    if (success) {
                                        // popUpTo na propria rota (join_club) + inclusive + singleTop,
                                        // igual ao create_club. NAO usar popUpTo("welcome"): welcome
                                        // ja saiu do back stack no login, entao vira no-op e empilha
                                        // main_tabs duplicado (voltar reabria a tela de codigo).
                                        navController.navigate("main_tabs") {
                                            popUpTo("join_club") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable("main_tabs") {
                        // Gate de onboarding: primeiro login deste usuario neste
                        // device mostra OnboardingScreen (avatar + apelido + fonte).
                        // Estado vem de DataStore (onboardedUsersFlow) cruzado com
                        // currentUserId — defensivo contra trocar de conta.
                        val needsOnboarding by viewModel.needsOnboarding.collectAsState()
                        val currentUser by viewModel.currentUser.collectAsState()
                        val supaName by viewModel.supabaseDisplayName.collectAsState()
                        val fontScale by viewModel.fontScale.collectAsState()
                        if (needsOnboarding) {
                            OnboardingScreen(
                                initialName = currentUser?.nome ?: supaName ?: "",
                                initialAvatarUrl = currentUser?.avatarUrl ?: "preset:leitor",
                                initialFontScale = fontScale,
                                onComplete = { nome, avatarUrl, scale ->
                                    viewModel.completeOnboarding(nome, avatarUrl, scale)
                                },
                            )
                        } else {
                            MainTabsScreen(
                                viewModel = viewModel,
                                onNavigateToNotifications = { navController.navigate("notifications") },
                                onNavigateToDiscussion = { chapterId, title ->
                                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                                    navController.navigate("discussion/$chapterId/$encodedTitle")
                                },
                                onNavigateToSuggestBook = { navController.navigate("suggest_book") },
                                onNavigateToJoinClub = { navController.navigate("join_club") },
                                onNavigateToCreateClub = { navController.navigate("create_club") },
                                onLogoutCompleted = {
                                    navController.navigate("welcome") {
                                        popUpTo("main_tabs") { inclusive = true }
                                    }
                                },
                                onNavigateToBookDetail = { bookId -> navController.navigate("book_detail/$bookId") },
                                onNavigateToFrases = { navController.navigate("frases") },
                                onNavigateToManageClub = { navController.navigate("manage_club") },
                                onNavigateToMeetingDetail = { mid -> navController.navigate("meeting_detail/$mid") },
                                onNavigateToAbout = { navController.navigate("about") }
                            )
                        }
                    }

                    composable("meeting_detail/{meetingId}") { backStackEntry ->
                        val mid = backStackEntry.arguments?.getString("meetingId") ?: ""
                        MeetingDetailScreen(
                            viewModel = viewModel,
                            meetingId = mid,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("manage_club") {
                        ManageClubScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToChapters = { navController.navigate("manage_chapters") },
                            onNavigateToModerationLog = { navController.navigate("moderation_log") }
                        )
                    }

                    composable("manage_chapters") {
                        ManageChaptersScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("moderation_log") {
                        ModerationLogScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("frases") {
                        FrasesScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("book_detail/{bookId}") { backStackEntry ->
                        val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                        BookDetailScreen(
                            viewModel = viewModel,
                            bookId = bookId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("notifications") {
                        NotificationsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDiscussion = { chapterId, title ->
                                val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                                navController.navigate("discussion/$chapterId/$encodedTitle")
                            },
                            onNavigateToTab = { tab -> viewModel.requestTab(tab) }
                        )
                    }

                    composable("discussion/{chapterId}/{title}") { backStackEntry ->
                        val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                        val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
                        val title = try { java.net.URLDecoder.decode(rawTitle, "UTF-8") } catch (e: Exception) { rawTitle }
                        DiscussionScreen(
                            viewModel = viewModel,
                            chapterId = chapterId,
                            chapterTitle = title,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("suggest_book") {
                        SuggestScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToAddManual = { navController.navigate("add_book_manual") }
                        )
                    }

                    composable("add_book_manual") {
                        AddBookManualScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onBookCreated = {
                                // Volta pra Suggest após criar — popBackStack 2x não funciona limpo
                                // então usa navigate com popUpTo na Suggest pra recriar a tela vazia
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("about") {
                        AboutScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                }
            }
            } // CompositionLocalProvider
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Supabase.client.handleDeeplinks(intent)
    }
}
