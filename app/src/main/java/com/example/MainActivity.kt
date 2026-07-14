package com.example

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.data.ThemeMode
import com.example.data.remote.AuthRepository
import com.example.data.remote.Supabase
import com.example.ui.auth.GoogleSignInHelper
import androidx.compose.foundation.isSystemInDarkTheme
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.example.ui.theme.LocalSharedTransitionScope
import com.example.ui.theme.LocalNavAnimatedScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination
import androidx.navigation.toRoute
import kotlin.reflect.KClass
import com.example.ui.navigation.About
import com.example.ui.navigation.AddBookManual
import com.example.ui.navigation.BookDetail
import com.example.ui.navigation.CreateClub
import com.example.ui.navigation.Discussion
import com.example.ui.navigation.ForgotPassword
import com.example.ui.navigation.Frases
import com.example.ui.navigation.JoinClub
import com.example.ui.navigation.Login
import com.example.ui.navigation.MainTabs
import com.example.ui.navigation.ManageChapters
import com.example.ui.navigation.ManageClub
import com.example.ui.navigation.MeetingDetail
import com.example.ui.navigation.ModerationLog
import com.example.ui.navigation.ModerationQueue
import com.example.ui.navigation.Notifications
import com.example.ui.navigation.ResetPassword
import com.example.ui.navigation.Signup
import com.example.ui.navigation.SuggestBook
import com.example.ui.navigation.Welcome
import com.example.ui.screens.*
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RodapeMotion
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // 4.1: splash de marca (fundo paper + ícone) no cold start — precisa vir
        // ANTES do super.onCreate pra trocar o tema Starting → real sem flash.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Supabase.client.handleDeeplinks(intent)
        // Triggering fresh compilation and preview deployment for the user
        enableEdgeToEdge()
        setContent {
            val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
            val baseDensity = LocalDensity.current
            // Acessibilidade: COMBINA a escala do sistema (config "Tamanho da fonte"
            // do Android, essencial pra baixa visão) com o ajuste interno, em vez de
            // substituí-la. Antes o app descartava baseDensity.fontScale e ignorava
            // a preferência do sistema.
            val scaledDensity = Density(
                density = baseDensity.density,
                fontScale = baseDensity.fontScale * fontScale
            )
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // 4.1: ícones da status/nav bar acompanham o TEMA DO APP (não só o
            // do sistema) — tema escuro pede ícones claros e vice-versa. O
            // enableEdgeToEdge default usa o uiMode do sistema, que diverge
            // quando o usuário força claro/escuro dentro do app.
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                )
            }
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()

                    // Push (F1): pedido de permissão de notificação (Android 13+).
                    // Resultado não importa aqui — se negar, o app só não vibra.
                    val context = LocalContext.current
                    val notifPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }
                    // Guard: só pede/registra uma vez por processo (evita re-prompt a
                    // cada re-emissão do sessionStatus).
                    var pushSetupDone by rememberSaveable { mutableStateOf(false) }

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
                                    navController.navigate(ResetPassword) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            s is SessionStatus.Authenticated -> {
                                // Logado: garante que esta em main_tabs (idempotente).
                                // hasRoute<T>() em vez de comparar route string: com nav
                                // type-safe o route vira o nome qualificado do @Serializable.
                                val current = navController.currentBackStackEntry?.destination
                                if (current == null || current.matchesRoute(Welcome::class)) {
                                    navController.navigate(MainTabs) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                // Push F1: registra o token FCM e pede permissão (1x).
                                if (!pushSetupDone) {
                                    pushSetupDone = true
                                    viewModel.syncPushToken()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                                        != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            }
                            s is SessionStatus.NotAuthenticated -> {
                                // Deslogado: libera o guard pra um futuro link de recovery.
                                recoveryConsumed = false
                                // garante que esta em welcome (se nao ja estiver numa tela de auth).
                                val current = navController.currentBackStackEntry?.destination
                                val onAuthScreen = current != null && (
                                    current.matchesRoute(Welcome::class) || current.matchesRoute(Login::class) ||
                                    current.matchesRoute(Signup::class) || current.matchesRoute(ForgotPassword::class) ||
                                    current.matchesRoute(ResetPassword::class)
                                )
                                if (current != null && !onAuthScreen) {
                                    navController.navigate(Welcome) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            // Initializing: nao faz nada, espera resolver.
                            else -> {}
                        }
                    }

                    // Transições padrão de navegação (2.7): direcionais — avançar
                    // desliza da direita (slide 1/5 + fade, emphasizedDecelerate),
                    // voltar devolve pra direita (accelerate). O eixo comunica a
                    // hierarquia; antes as trocas de tela eram secas. Os lambdas de
                    // transição NÃO são @Composable, então reduced-motion é lido
                    // aqui fora e capturado (vira None = instantâneo).
                    val reducedMotionNav = LocalReducedMotion.current
                    SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    NavHost(
                        navController = navController,
                        startDestination = Welcome,
                        enterTransition = {
                            if (reducedMotionNav) EnterTransition.None
                            else slideInHorizontally(
                                animationSpec = tween(RodapeMotion.Dur.emphasized, easing = RodapeMotion.Ease.emphasizedDecelerate),
                                initialOffsetX = { it / 5 },
                            ) + fadeIn(tween(RodapeMotion.Dur.standard))
                        },
                        exitTransition = {
                            if (reducedMotionNav) ExitTransition.None
                            else fadeOut(tween(RodapeMotion.Dur.fast))
                        },
                        popEnterTransition = {
                            if (reducedMotionNav) EnterTransition.None
                            else fadeIn(tween(RodapeMotion.Dur.standard))
                        },
                        popExitTransition = {
                            if (reducedMotionNav) ExitTransition.None
                            else slideOutHorizontally(
                                animationSpec = tween(RodapeMotion.Dur.emphasized, easing = RodapeMotion.Ease.emphasizedAccelerate),
                                targetOffsetX = { it / 5 },
                            ) + fadeOut(tween(RodapeMotion.Dur.standard))
                        },
                    ) {
                    composable<Welcome> {
                        // Intro de primeiro uso antes do welcome. introSeen == null:
                        // ainda lendo do DataStore — não renderiza nada (evita piscar
                        // a intro pra quem já viu, ou o welcome pra quem ainda vai ver).
                        val introSeen by viewModel.introSeen.collectAsStateWithLifecycle()
                        when (introSeen) {
                            null -> { /* aguarda o DataStore (1 frame) */ }
                            false -> IntroScreen(onFinished = { viewModel.markIntroSeen() })
                            else -> WelcomeScreen(
                                onNavigateToLogin = { navController.navigate(Login) },
                                onNavigateToSignUp = { navController.navigate(Signup) },
                                onNavigateWithInvite = { code ->
                                    // B1: retém o código e manda criar conta; o join
                                    // automático acontece ao chegar em main_tabs.
                                    viewModel.setPendingInviteCode(code)
                                    navController.navigate(Signup)
                                },
                            )
                        }
                    }

                    composable<Login> {
                        val ctx = LocalContext.current
                        val authRepo = remember { AuthRepository() }
                        val google = remember { GoogleSignInHelper(ctx) }
                        LoginScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSignUp = { navController.navigate(Signup) },
                            onNavigateToForgotPassword = { navController.navigate(ForgotPassword) },
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
                                navController.navigate(MainTabs) {
                                    popUpTo(Welcome) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<Signup> {
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
                                navController.navigate(Login) {
                                    popUpTo(Welcome) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToLogin = {
                                navController.navigate(Login) {
                                    popUpTo(Welcome) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onGoogleSignedIn = {
                                navController.navigate(MainTabs) {
                                    popUpTo(Welcome) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<ForgotPassword> {
                        val authRepo = remember { AuthRepository() }
                        ForgotPasswordScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSendReset = { email -> runCatching { authRepo.sendPasswordResetEmail(email) } },
                        )
                    }

                    composable<ResetPassword> {
                        val authRepo = remember { AuthRepository() }
                        val resetScope = rememberCoroutineScope()
                        ResetPasswordScreen(
                            onUpdatePassword = { newPassword -> runCatching { authRepo.updatePassword(newPassword) } },
                            onPasswordUpdated = {
                                navController.navigate(MainTabs) {
                                    popUpTo(Welcome) { inclusive = true }
                                }
                            },
                            onCancel = {
                                // Sai do fluxo de recovery: encerra a sessao de recovery
                                // e volta pro welcome (estado neutro/seguro).
                                resetScope.launch { runCatching { authRepo.signOut() } }
                                navController.navigate(Welcome) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable<CreateClub> {
                        CreateClubScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onCreateCompleted = { nome, descricao, cor, privacidade, onError ->
                                viewModel.createClub(
                                    nome, descricao, cor, privacidade,
                                    onCompleted = { _ ->
                                        // popUpTo na propria rota corrente + inclusive=true remove
                                        // create_club do back stack e cai em main_tabs (que sempre
                                        // existe). NAO usar popUpTo(Welcome) — falha silenciosa
                                        // quando welcome nao esta no back stack (usuario logado
                                        // direto via cold start) e causa comportamento bizarro.
                                        navController.navigate(MainTabs) {
                                            popUpTo(CreateClub) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onError = onError,
                                )
                            }
                        )
                    }

                    composable<JoinClub> {
                        JoinClubScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onJoinWithCodeSubmit = { code, onResult ->
                                viewModel.joinClubWithCode(code) { success, errorMsg ->
                                    onResult(success, errorMsg)
                                    if (success) {
                                        // popUpTo na propria rota (join_club) + inclusive + singleTop,
                                        // igual ao create_club. NAO usar popUpTo(Welcome): welcome
                                        // ja saiu do back stack no login, entao vira no-op e empilha
                                        // main_tabs duplicado (voltar reabria a tela de codigo).
                                        navController.navigate(MainTabs) {
                                            popUpTo(JoinClub) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable<MainTabs> {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                        // Gate de onboarding: primeiro login deste usuario neste
                        // device mostra OnboardingScreen (avatar + apelido + fonte).
                        // Estado vem de DataStore (onboardedUsersFlow) cruzado com
                        // currentUserId — defensivo contra trocar de conta.
                        val needsOnboarding by viewModel.needsOnboarding.collectAsStateWithLifecycle()
                        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                        val supaName by viewModel.supabaseDisplayName.collectAsStateWithLifecycle()
                        val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
                        // B1: chegou aqui com um convite pendente (fluxo do convidado)
                        // → entra no clube automaticamente e limpa o código.
                        val pendingInvite by viewModel.pendingInviteCode.collectAsStateWithLifecycle()
                        LaunchedEffect(pendingInvite) {
                            pendingInvite?.let { code ->
                                viewModel.joinClubWithCode(code) { _, _ -> }
                                viewModel.consumePendingInviteCode()
                            }
                        }
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
                                onNavigateToNotifications = { navController.navigate(Notifications) },
                                onNavigateToDiscussion = { chapterId, title ->
                                    // Sem URLEncoder: a Navigation type-safe encoda o arg.
                                    navController.navigate(Discussion(chapterId, title))
                                },
                                onNavigateToSuggestBook = { navController.navigate(SuggestBook) },
                                onNavigateToJoinClub = { navController.navigate(JoinClub) },
                                onNavigateToCreateClub = { navController.navigate(CreateClub) },
                                onLogoutCompleted = {
                                    navController.navigate(Welcome) {
                                        popUpTo(MainTabs) { inclusive = true }
                                    }
                                },
                                onNavigateToBookDetail = { bookId -> navController.navigate(BookDetail(bookId)) },
                                onNavigateToFrases = { navController.navigate(Frases) },
                                onNavigateToManageClub = { navController.navigate(ManageClub) },
                                onNavigateToManageChapters = { navController.navigate(ManageChapters) },
                                onNavigateToMeetingDetail = { mid -> navController.navigate(MeetingDetail(mid)) },
                                onNavigateToAbout = { navController.navigate(About) }
                            )
                        }
                        } // LocalNavAnimatedScope
                    }

                    composable<MeetingDetail> { backStackEntry ->
                        val mid = backStackEntry.toRoute<MeetingDetail>().meetingId
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                        MeetingDetailScreen(
                            viewModel = viewModel,
                            meetingId = mid,
                            onNavigateBack = { navController.popBackStack() }
                        )
                        }
                    }

                    composable<ManageClub> {
                        ManageClubScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToChapters = { navController.navigate(ManageChapters) },
                            onNavigateToModerationLog = { navController.navigate(ModerationLog) },
                            onNavigateToModerationQueue = { navController.navigate(ModerationQueue) }
                        )
                    }

                    composable<ManageChapters> {
                        ManageChaptersScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<ModerationLog> {
                        ModerationLogScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<ModerationQueue> {
                        ModerationQueueScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<Frases> {
                        FrasesScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<BookDetail> { backStackEntry ->
                        val bookId = backStackEntry.toRoute<BookDetail>().bookId
                        BookDetailScreen(
                            viewModel = viewModel,
                            bookId = bookId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<Notifications> {
                        NotificationsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDiscussion = { chapterId, title ->
                                navController.navigate(Discussion(chapterId, title))
                            },
                            onNavigateToTab = { tab -> viewModel.requestTab(tab) }
                        )
                    }

                    composable<Discussion> { backStackEntry ->
                        val route = backStackEntry.toRoute<Discussion>()
                        DiscussionScreen(
                            viewModel = viewModel,
                            chapterId = route.chapterId,
                            chapterTitle = route.title,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable<SuggestBook> {
                        SuggestScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToAddManual = { navController.navigate(AddBookManual) }
                        )
                    }

                    composable<AddBookManual> {
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

                    composable<About> {
                        AboutScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                    } // CompositionLocalProvider(sharedScope)
                    } // SharedTransitionLayout
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

// Checa se o destino atual é uma rota type-safe [clazz]. Com Navigation type-safe
// o `route` é o serialName do @Serializable (= nome qualificado da classe); pra
// rotas com args vem "…Classe/{arg}", então corta no primeiro '/' ou '?'.
// (O hasRoute<T>() reified fica sombreado pelo membro hasRoute(String, Bundle?).)
private fun NavDestination?.matchesRoute(clazz: KClass<*>): Boolean =
    this?.route?.substringBefore("/")?.substringBefore("?") == clazz.qualifiedName
