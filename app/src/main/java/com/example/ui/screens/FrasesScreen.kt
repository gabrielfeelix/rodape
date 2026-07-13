package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CenteredLoading
import com.example.ui.components.QuoteCard
import com.example.ui.components.rememberShowLoading
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.RodapeTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.formatShortDate
import com.example.util.shareTextContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrasesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
) {
    val quotes by viewModel.savedQuotes.collectAsState()
    val clubBooks by viewModel.clubBooks.collectAsState()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredQuotes = remember(quotes, query, clubBooks) {
        if (query.isBlank()) quotes
        else {
            val q = query.trim().lowercase()
            quotes.filter { quote ->
                val bookTitle = clubBooks.find { it.id == quote.bookId }?.title.orEmpty()
                quote.texto.lowercase().contains(q) ||
                    bookTitle.lowercase().contains(q) ||
                    quote.capituloRef.lowercase().contains(q)
            }
        }
    }

    // Distingue LOADING de EMPTY: no cold start local-first `quotes` chega vazio
    // antes do primeiro sync, mostrando "nenhuma frase" por engano.
    val showLoading = rememberShowLoading(hasData = quotes.isNotEmpty())

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
                actions = {
                    if (quotes.isNotEmpty()) {
                        IconButton(onClick = {
                            val payload = buildExportPayload(quotes, clubBooks)
                            shareTextContent(
                                context = context,
                                subject = "Minhas frases salvas no Rodapé",
                                text = payload
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Exportar/Compartilhar frases",
                                tint = RodapeTheme.colors.terracota
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Barra de busca (só aparece se há frases)
            if (quotes.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            "Buscar por frase, livro ou capítulo…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RodapeTheme.colors.muted,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = RodapeTheme.colors.muted)
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Limpar", tint = RodapeTheme.colors.muted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = RodapeTheme.colors.cream,
                        focusedContainerColor = RodapeTheme.colors.cream,
                        unfocusedBorderColor = RodapeTheme.colors.divider,
                        focusedBorderColor = RodapeTheme.colors.terracota,
                        cursorColor = RodapeTheme.colors.terracota,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (showLoading) {
                CenteredLoading()
            } else if (quotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 40.dp),
                    ) {
                        Icon(
                            Icons.Outlined.FormatQuote,
                            contentDescription = null,
                            tint = RodapeTheme.colors.muted,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = "Você ainda não guardou nenhuma frase.",
                            style = MaterialTheme.typography.titleMedium,
                            color = RodapeTheme.colors.muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Text(
                            text = "As frases que você marcar durante a leitura aparecem aqui. Abra um livro e toque em marcar frase enquanto lê.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RodapeTheme.colors.muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        com.example.ui.components.TbButton(
                            text = "Voltar",
                            onClick = onNavigateBack,
                            variant = com.example.ui.components.TbButtonVariant.OlivaSoft,
                            size = com.example.ui.components.TbButtonSize.Md,
                        )
                    }
                }
            } else if (filteredQuotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nada encontrado pra \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RodapeTheme.colors.muted,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        val label = if (query.isBlank()) {
                            "As frases que você guardou. ${quotes.size} no total."
                        } else {
                            "${filteredQuotes.size} de ${quotes.size} frases."
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = RodapeTheme.colors.muted,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }

                    items(filteredQuotes, key = { it.id }) { quote ->
                        // Livro fora do clube: fallback neutro. O capítulo fica só na
                        // linha de referência (QuoteCard), não no lugar do título.
                        val bookTitle = clubBooks.find { it.id == quote.bookId }?.title
                            ?: "Livro removido"

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = bookTitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = LiterataFontFamily,
                                fontStyle = FontStyle.Italic,
                                color = RodapeTheme.colors.muted,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                            QuoteCard(
                                texto = quote.texto,
                                ref = quote.capituloRef,
                                onDelete = {
                                    // Excluir com undo: destrutivo demais pra
                                    // sumir sem rede de proteção.
                                    viewModel.deleteQuote(quote)
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Frase excluída",
                                            actionLabel = "Desfazer",
                                            duration = SnackbarDuration.Short,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreQuote(quote)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildExportPayload(
    quotes: List<com.example.data.model.SavedQuote>,
    clubBooks: List<com.example.data.model.Book>
): String = buildString {
    append("📖 Frases salvas no Rodapé\n")
    append("─".repeat(28))
    append("\n\n")
    quotes.forEach { q ->
        val book = clubBooks.find { it.id == q.bookId }?.title ?: "—"
        append("\"${q.texto}\"\n")
        append("— $book")
        if (q.capituloRef.isNotBlank()) append(" · ${q.capituloRef}")
        append(" · ${formatShortDate(q.criadoEm)}\n\n")
    }
    append("(${quotes.size} ${if (quotes.size == 1) "frase" else "frases"} exportadas pelo app Rodapé)")
}
