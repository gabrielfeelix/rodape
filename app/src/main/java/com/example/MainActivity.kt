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
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

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
            val scaledDensity = Density(
                density = baseDensity.density,
                fontScale = fontScale
            )
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val supabaseUserId by viewModel.supabaseUserId.collectAsState()
                    val sessionStatus by viewModel.sessionStatus.collectAsState()

                    val startDestination = if (supabaseUserId != null) "main_tabs" else "welcome"

                    // Quando o usuario abre o link de "esqueci minha senha", o Supabase
                    // hidrata uma sessao temporaria com source == External. Redirecionamos
                    // pra tela de definir nova senha.
                    LaunchedEffect(sessionStatus) {
                        val s = sessionStatus
                        if (s is SessionStatus.Authenticated && s.source is SessionSource.External) {
                            navController.navigate("reset_password") {
                                popUpTo("welcome") { inclusive = false }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
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
                                    android.util.Log.d("Rodape/Auth", "Got Google ID token (length=${token.idToken.length}), enviando pro Supabase...")
                                    authRepo.signInWithGoogleIdToken(token.idToken, token.rawNonce)
                                    android.util.Log.d("Rodape/Auth", "Supabase aceitou o Google ID token. Sessao ativa.")
                                    Unit
                                }.also { r ->
                                    r.exceptionOrNull()?.let { e ->
                                        android.util.Log.e("Rodape/Auth", "Google Sign-In falhou: ${e.message}", e)
                                    }
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
                        val authRepo = remember { AuthRepository() }
                        SignUpScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSignUp = { email, password, name ->
                                runCatching { authRepo.signUpWithEmail(email, password, name) }
                            },
                            onSignedUp = {
                                navController.popBackStack(route = "login", inclusive = false)
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
                        ResetPasswordScreen(
                            onUpdatePassword = { newPassword -> runCatching { authRepo.updatePassword(newPassword) } },
                            onPasswordUpdated = {
                                navController.navigate("main_tabs") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            },
                        )
                    }

                    composable("create_club") {
                        CreateClubScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onCreateCompleted = { nome, descricao, cor, privacidade ->
                                viewModel.createClub(nome, descricao, cor, privacidade) {
                                    navController.navigate("main_tabs") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
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
                                        navController.navigate("main_tabs") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable("main_tabs") {
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
