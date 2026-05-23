package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.api.OpenLibraryDoc
import com.example.ui.components.Cover
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.theme.Cream
import com.example.ui.theme.Divider
import com.example.ui.theme.Ink
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.Muted
import com.example.ui.theme.Terracota
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val loading by viewModel.searchLoading.collectAsState()

    var selectedDoc by remember { mutableStateOf<OpenLibraryDoc?>(null) }
    var showJustifySheetForDoc by remember { mutableStateOf<OpenLibraryDoc?>(null) }
    var justificationText by remember { mutableStateOf("") }
    // Cross-check de autor com Google Books
    var verifying by remember { mutableStateOf(false) }
    var gbConflictAuthor by remember { mutableStateOf<String?>(null) }
    var pickedAuthor by remember { mutableStateOf<String?>(null) }

    // Implement 400ms Debounce explicitly in LaunchedEffect/Kotlin
    LaunchedEffect(query) {
        if (query.trim().length >= 3) {
            delay(400)
            viewModel.searchOpenLibrary(query)
        }
    }

    // Exclude books with missing covers or missing author_name
    val filteredResults = remember(searchResults) {
        searchResults.filter { doc ->
            doc.coverI != null && !doc.authorName.isNullOrEmpty()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sugerir livro",
                        style = MaterialTheme.typography.headlineLarge
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
                actions = {
                    TextButton(
                        onClick = {
                            val doc = selectedDoc ?: return@TextButton
                            justificationText = ""
                            gbConflictAuthor = null
                            pickedAuthor = null
                            verifying = true
                            viewModel.verifyAuthorWithGoogleBooks(
                                title = doc.title,
                                olAuthor = doc.authorName?.firstOrNull().orEmpty(),
                                isbn = doc.isbn?.firstOrNull().orEmpty()
                            ) { gbAuthor ->
                                verifying = false
                                gbConflictAuthor = gbAuthor // null se bate
                                showJustifySheetForDoc = doc
                            }
                        },
                        enabled = selectedDoc != null && !verifying,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Terracota,
                            disabledContentColor = Terracota.copy(alpha = 0.4f)
                        )
                    ) {
                        if (verifying) {
                            CircularProgressIndicator(
                                color = Terracota,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                "Adicionar",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search bar input — rounded, Cream background, Divider border
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        "Buscar por título, autor ou ISBN",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Buscar",
                        tint = Muted
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Cream,
                    focusedContainerColor = Cream,
                    unfocusedBorderColor = Divider,
                    focusedBorderColor = Terracota,
                    unfocusedLeadingIconColor = Muted,
                    focusedLeadingIconColor = Muted,
                    cursorColor = Terracota,
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Terracota)
                }
            } else if (filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.trim().length < 3)
                            "Comece a digitar pra encontrar livros."
                        else
                            "Nenhum livro de catálogo completo localizado.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Muted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredResults) { doc ->
                        val isSelected = selectedDoc == doc
                        val author = doc.authorName?.firstOrNull() ?: "Autor desconhecido"
                        val coverUrl = "https://covers.openlibrary.org/b/id/${doc.coverI}-M.jpg"

                        // Result row — Terracota border when selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Cream else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) Terracota else Divider,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    selectedDoc = if (isSelected) null else doc
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Phase-1 Cover component
                            Cover(
                                title = doc.title,
                                author = author,
                                coverUrl = coverUrl,
                                width = 48.dp,
                                height = 72.dp
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Ink,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = InterFontFamily
                                    ),
                                    color = Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (doc.firstPublishYear != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${doc.firstPublishYear}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = InterFontFamily
                                        ),
                                        color = Muted
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Terracota),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selecionado",
                                        tint = Cream,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Justification dialog
    if (showJustifySheetForDoc != null) {
        val doc = showJustifySheetForDoc!!
        val olAuthor = doc.authorName?.firstOrNull().orEmpty()
        // Se nunca escolheu, default = OL (autor original do search)
        if (pickedAuthor == null) pickedAuthor = olAuthor
        AlertDialog(
            onDismissRequest = { showJustifySheetForDoc = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Por que esse livro?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (gbConflictAuthor != null) {
                        // Banner de conflito de autor
                        Surface(
                            color = Color(0xFFFFF4D6),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "⚠️ Conflito de autor detectado",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF8B6B00)
                                    )
                                )
                                Text(
                                    "As fontes discordam. Qual autor está correto?",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8B6B00))
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { pickedAuthor = olAuthor }
                                ) {
                                    RadioButton(selected = pickedAuthor == olAuthor, onClick = { pickedAuthor = olAuthor })
                                    Text("$olAuthor (Open Library)", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { pickedAuthor = gbConflictAuthor }
                                ) {
                                    RadioButton(selected = pickedAuthor == gbConflictAuthor, onClick = { pickedAuthor = gbConflictAuthor })
                                    Text("${gbConflictAuthor} (Google Books)", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Text(
                        text = "Conta pro pessoal por que tu sugere esse. Opcional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted
                    )

                    OutlinedTextField(
                        value = justificationText,
                        onValueChange = { justificationText = it },
                        placeholder = {
                            Text(
                                "Ex: É um clássico excelente e curto...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Muted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Cream,
                            focusedContainerColor = Cream,
                            unfocusedBorderColor = Divider,
                            focusedBorderColor = Terracota,
                            cursorColor = Terracota,
                        )
                    )
                }
            },
            confirmButton = {
                TbButton(
                    text = "Adicionar",
                    onClick = {
                        viewModel.createBookSuggestion(
                            doc = doc,
                            justification = justificationText,
                            authorOverride = pickedAuthor
                        ) {
                            showJustifySheetForDoc = null
                            selectedDoc = null
                            gbConflictAuthor = null
                            pickedAuthor = null
                            onNavigateBack()
                        }
                    },
                    variant = TbButtonVariant.Terra,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TbButton(
                    text = "Voltar",
                    onClick = {
                        showJustifySheetForDoc = null
                        gbConflictAuthor = null
                        pickedAuthor = null
                    },
                    variant = TbButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
