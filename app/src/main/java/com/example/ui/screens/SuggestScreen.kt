package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.OpenLibraryDoc
import com.example.ui.components.BookCover
import com.example.ui.components.StandardCard
import com.example.ui.theme.Terracota
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
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
                title = { Text("Sugerir livro", style = MaterialTheme.typography.headlineLarge) },
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
                            if (selectedDoc != null) {
                                showJustifySheetForDoc = selectedDoc
                                justificationText = ""
                            }
                        },
                        enabled = selectedDoc != null,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Terracota,
                            disabledContentColor = Terracota.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("Adicionar", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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

            // Search bar input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar por título, autor ou ISBN") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true
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
                        text = if (query.trim().length < 3) "Comece a digitar pra encontrar livros." else "Nenhum livro de catálogo completo localizado.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredResults) { doc ->
                        val isSelected = selectedDoc == doc
                        val author = doc.authorName?.firstOrNull() ?: "Autor desconhecido"
                        val coverUrl = "https://covers.openlibrary.org/b/id/${doc.coverI}-M.jpg"

                        StandardCard(
                            onClick = {
                                selectedDoc = if (isSelected) null else doc
                            },
                            modifier = Modifier.then(
                                if (isSelected) {
                                    Modifier.border(2.dp, Terracota, RoundedCornerShape(16.dp))
                                } else {
                                    Modifier
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BookCover(coverUrl = coverUrl, width = 50.dp, height = 75.dp)

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = doc.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = FrauncesFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = author,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = InterFontFamily,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (doc.firstPublishYear != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${doc.firstPublishYear}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = InterFontFamily,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Selecionado",
                                        tint = Terracota,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Justification dialog sheet
    if (showJustifySheetForDoc != null) {
        val doc = showJustifySheetForDoc!!
        AlertDialog(
            onDismissRequest = { showJustifySheetForDoc = null },
            title = {
                Text(
                    text = "Por que esse livro?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Conta pro pessoal por que tu sugere esse. Opcional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = justificationText,
                        onValueChange = { justificationText = it },
                        placeholder = { Text("Ex: É um clássico excelente e curto...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createBookSuggestion(doc, justificationText) {
                            showJustifySheetForDoc = null
                            selectedDoc = null
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Terracota,
                        disabledContainerColor = Terracota.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "Adicionar",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showJustifySheetForDoc = null }) {
                    Text(
                        text = "Voltar",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        )
    }
}
