package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    viewModel: MainViewModel,
    bookId: String,
    onNavigateBack: () -> Unit
) {
    val clubBooks by viewModel.clubBooks.collectAsState()
    val book = clubBooks.find { it.id == bookId }

    if (book == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalhe do livro") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Voltar"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Livro não encontrado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted
                )
            }
        }
        return
    }

    // --- Internal tabs state ---
    var tab by remember { mutableStateOf("resumo") }

    // --- Quote dialog state ---
    var showQuoteDialog by remember { mutableStateOf(false) }
    var quoteText by remember { mutableStateOf("") }

    // --- Quotes flow ---
    val quotesFlow = remember(bookId) { viewModel.getSavedQuotesForBook(bookId) }
    val quotes by quotesFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OlivaSoft, Paper)))
    ) {
        // ── HERO ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(OlivaSoft, Paper)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cover
                Cover(
                    title = book.title,
                    author = book.author,
                    coverUrl = book.coverUrl,
                    width = 150.dp,
                    height = 224.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Author
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        color = Muted
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Back button — top-left
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, start = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardSurface.copy(alpha = 0.85f))
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Ink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── READ-STATUS BANNER ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Oliva)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tu leu este livro com o clube.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = Cream
                ),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── INTERNAL TABS ────────────────────────────────────────────────────
        val tabs = listOf("resumo" to "Resumo", "frases" to "Frases", "historico" to "Histórico")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            tabs.forEach { (key, label) ->
                val isSelected = tab == key
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { tab = key },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Ink else Muted
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (isSelected) Terracota else DividerSoft,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── TAB CONTENT ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            when (tab) {
                // ── RESUMO ───────────────────────────────────────────────────
                "resumo" -> {
                    TramabookCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Um livro que o clube leu junto. As conversas sobre ele renderam — toca em Frases pra rever o que ficou.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = InkSoft,
                                lineHeight = 22.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Terracota, CircleShape)
                        )
                        Text(
                            text = "O QUE O CLUBE ACHOU",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Terracota
                            )
                        )
                    }

                    TramabookCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "O clube adorou — as discussões ficaram acesas até tarde. Todo mundo saiu com uma visão diferente sobre o livro.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = InkSoft,
                                lineHeight = 22.sp
                            )
                        )
                    }
                }

                // ── FRASES ───────────────────────────────────────────────────
                "frases" -> {
                    if (quotes.isEmpty()) {
                        Text(
                            text = "Nenhuma frase guardada deste livro ainda.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = Muted
                            ),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        quotes.forEach { quote ->
                            TramabookCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "“${quote.texto}”",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 16.sp,
                                        color = InkSoft,
                                        lineHeight = 24.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = quote.capituloRef,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = InterFontFamily,
                                        color = Muted
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TbButton(
                        text = "Salvar uma frase",
                        onClick = { showQuoteDialog = true },
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Md,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── HISTÓRICO ────────────────────────────────────────────────
                "historico" -> {
                    val milestones = listOf(
                        "Sugerido" to "O livro chegou na lista do clube.",
                        "Leitura começou" to "O clube começou a ler junto.",
                        "Encontro do clube" to "O clube se reuniu pra discutir."
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        milestones.forEachIndexed { index, (title, desc) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Timeline column: dot + line
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(Oliva, CircleShape)
                                    )
                                    if (index < milestones.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(56.dp)
                                                .background(OlivaSoft)
                                        )
                                    }
                                }

                                // Content
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = LiterataFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Ink
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = InterFontFamily,
                                            color = Muted
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(if (index < milestones.size - 1) 44.dp else 0.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── SAVE QUOTE DIALOG ────────────────────────────────────────────────────
    if (showQuoteDialog) {
        AlertDialog(
            onDismissRequest = {
                showQuoteDialog = false
                quoteText = ""
            },
            title = {
                Text(
                    text = "Salvar frase",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = quoteText,
                    onValueChange = { quoteText = it },
                    label = { Text("Frase do livro") },
                    placeholder = { Text("Escreve a frase aqui…") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (quoteText.isNotBlank()) {
                            viewModel.saveQuote(bookId, quoteText, "Cap.")
                        }
                        showQuoteDialog = false
                        quoteText = ""
                    }
                ) {
                    Text(
                        text = "Salvar",
                        color = Oliva,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showQuoteDialog = false
                        quoteText = ""
                    }
                ) {
                    Text(
                        text = "Cancelar",
                        color = Muted
                    )
                }
            }
        )
    }
}
