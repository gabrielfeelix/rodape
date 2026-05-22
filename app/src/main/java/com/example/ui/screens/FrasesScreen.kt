package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.QuoteCard
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.Muted
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrasesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
) {
    val quotes by viewModel.savedQuotes.collectAsState()
    val clubBooks by viewModel.clubBooks.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Frases",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = LiterataFontFamily,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (quotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Tu ainda não guardou nenhuma frase.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = "As frases que tu guardou. ${quotes.size} no total.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                items(quotes, key = { it.id }) { quote ->
                    val bookTitle = clubBooks.find { it.id == quote.bookId }?.title
                        ?: quote.capituloRef

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = LiterataFontFamily,
                            fontStyle = FontStyle.Italic,
                            color = Muted,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        QuoteCard(
                            texto = quote.texto,
                            ref = quote.capituloRef,
                            onDelete = { viewModel.deleteQuote(quote) },
                        )
                    }
                }
            }
        }
    }
}
