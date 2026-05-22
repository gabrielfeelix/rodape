package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        // Triggering fresh compilation and preview deployment for the user
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currentUserId by viewModel.currentUserId.collectAsState()

                    val startDestination = if (currentUserId != null) {
                        "main_tabs"
                    } else {
                        "welcome"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                    composable("welcome") {
                        WelcomeScreen(
                            onNavigateToLogin = { navController.navigate("login") },
                            onNavigateToCreateClub = { navController.navigate("create_club") },
                            onNavigateToJoinClub = { navController.navigate("join_club") }
                        )
                    }

                    composable("login") {
                        LoginScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onLoginSuccess = { name, email ->
                                viewModel.login(name, email) {
                                    navController.navigate("main_tabs") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            }
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
                            onLogoutCompleted = {
                                navController.navigate("welcome") {
                                    popUpTo("main_tabs") { inclusive = true }
                                }
                            },
                            onNavigateToBookDetail = { bookId -> navController.navigate("book_detail/$bookId") }
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
                            onNavigateBack = { navController.popBackStack() }
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
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                }
            }
        }
    }
}
