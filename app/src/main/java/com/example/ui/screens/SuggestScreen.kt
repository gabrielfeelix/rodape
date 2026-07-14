package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.api.OpenLibraryDoc
import com.example.ui.components.Cover
import com.example.ui.components.ErrorState
import com.example.ui.components.RodapeDialog
import com.example.ui.components.ThemedRadio
import com.example.ui.components.SkeletonRowList
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.theme.Cream
import com.example.ui.theme.Divider
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.LiterataFontFamily
import androidx.compose.material3.Icon
import com.example.ui.theme.RodapeTheme
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.RodapeIcons
import com.example.ui.theme.Terracota
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddManual: () -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val loading by viewModel.searchLoading.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedDoc by remember { mutableStateOf<OpenLibraryDoc?>(null) }
    var showJustifySheetForDoc by remember { mutableStateOf<OpenLibraryDoc?>(null) }
    var justificationText by remember { mutableStateOf("") }
    // Cross-check de autor com Google Books
    var verifying by remember { mutableStateOf(false) }
    var gbConflictAuthor by remember { mutableStateOf<String?>(null) }
    var pickedAuthor by remember { mutableStateOf<String?>(null) }

    // Flag de "busca por digitação em andamento" — cobre tanto o debounce
    // (antes de o loading do VM ligar) quanto o próprio loading. Serve pra
    // mostrar skeleton em vez de área vazia/"nada achado" (erro ≠ vazio).
    var searchInFlight by remember { mutableStateOf(false) }

    // Implement 400ms Debounce explicitly in LaunchedEffect/Kotlin
    LaunchedEffect(query) {
        // R8: ao mudar/limpar a busca, a seleção antiga deixa de ser válida —
        // o doc some da lista, então o botão "Adicionar" não pode seguir ativo.
        selectedDoc = null
        if (query.trim().length >= 3) {
            searchInFlight = true
            delay(400)
            viewModel.searchOpenLibrary(query)
        } else {
            searchInFlight = false
            // Apagar a busca pra 0-2 chars deve limpar os hits antigos —
            // searchOpenLibrary("") é tratado como "limpar" no ViewModel.
            viewModel.searchOpenLibrary("")
        }
    }

    // Quando o loading do VM cai pra false, a busca terminou — desliga o
    // skeleton pra o empty/erro real poder aparecer.
    LaunchedEffect(loading) {
        if (!loading) searchInFlight = false
    }

    // Exclude books with missing covers or missing author_name
    val filteredResults = remember(searchResults) {
        // Não exige mais capa OL (coverI): livros do Google Books não têm e
        // eram descartados aqui. Sem capa, usamos a capa gerada. Só exige
        // título e autor pra evitar resultados vazios.
        searchResults.filter { doc ->
            doc.title.isNotBlank() && !doc.authorName.isNullOrEmpty()
        }
    }

    // R8: "Adicionar" só habilita se a seleção AINDA está entre os itens
    // visíveis (resultados da busca, ou populares quando não há busca ativa).
    // Sem isso, ao mudar/limpar a busca o botão seguia ativo apontando pra um
    // doc que já sumiu da lista.
    val selectedStillVisible = selectedDoc?.let { sel ->
        if (query.trim().length < 3) com.example.data.PopularBooks.list.contains(sel)
        else filteredResults.contains(sel)
    } ?: false

    // "Buscando": há query válida e o resultado dela ainda não chegou.
    val searching = loading || searchInFlight

    // Inicia o fluxo de sugestão pro doc: cross-check de autor com o Google Books
    // e, na sequência, a folha de justificativa. Reusado pela ação do topo e pela
    // barra inferior fixa "Sugerir selecionado".
    val beginSuggest: (OpenLibraryDoc) -> Unit = { doc ->
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
                            imageVector = RodapeIcons.Back,
                            contentDescription = "Voltar",
                            tint = RodapeTheme.colors.terracota
                        )
                    }
                },
                // 3.13: morreu o CTA "Sugerir" duplicado do topo — a barra
                // inferior "Sugerir selecionado" (perto da seleção) é o único
                // caminho; o spinner de verificação foi pra ela (loading).
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Confirmação perto do item: assim que há uma seleção válida, a barra
            // fixa "Sugerir selecionado" aparece — sem precisar subir até o topo.
            if (selectedStillVisible) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp
                ) {
                    TbButton(
                        text = "Sugerir selecionado",
                        onClick = { selectedDoc?.let { beginSuggest(it) } },
                        variant = TbButtonVariant.Terra,
                        enabled = !verifying,
                        loading = verifying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
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
                        color = RodapeTheme.colors.muted
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RodapeRadii.sm),
                leadingIcon = {
                    Icon(
                        imageVector = RodapeIcons.Search,
                        contentDescription = "Buscar",
                        tint = RodapeTheme.colors.muted
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        if (query.trim().length >= 3) {
                            viewModel.searchOpenLibrary(query)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = RodapeTheme.colors.cream,
                    focusedContainerColor = RodapeTheme.colors.cream,
                    unfocusedBorderColor = RodapeTheme.colors.divider,
                    focusedBorderColor = RodapeTheme.colors.terracota,
                    unfocusedLeadingIconColor = RodapeTheme.colors.muted,
                    focusedLeadingIconColor = RodapeTheme.colors.muted,
                    cursorColor = RodapeTheme.colors.terracota,
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (searching && query.trim().length >= 3) {
                // Enquanto a busca roda (debounce de 400ms + loading do VM), mostra
                // skeleton. O empty/erro só aparece quando a busca termina de fato —
                // sem isso a tela piscava "não achamos nada" antes de buscar.
                SkeletonRowList(
                    count = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )
            } else if (query.trim().length < 3) {
                // Lista de populares (pré-preenchida) — mostrada quando ainda não
                // há busca ativa. Paginada: 15 por vez, "load more" ao rolar.
                val popular = remember { com.example.data.PopularBooks.list }
                var visibleCount by rememberSaveable { mutableStateOf(15) }
                val listState = rememberLazyListState()

                // Load-more por limiar: quando o último item visível chega perto
                // do fim da fatia atual, revela mais 15.
                LaunchedEffect(listState, popular.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                        .collect { last ->
                            if (last >= visibleCount - 3 && visibleCount < popular.size) {
                                visibleCount = (visibleCount + 15).coerceAtMost(popular.size)
                            }
                        }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = "Populares — toque para selecionar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RodapeTheme.colors.muted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(popular.take(visibleCount)) { doc ->
                            BookResultRow(
                                doc = doc,
                                isSelected = selectedDoc == doc,
                                onClick = {
                                    selectedDoc = if (selectedDoc == doc) null else doc
                                }
                            )
                        }
                    }
                }
            } else if (filteredResults.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (query.trim().length < 3) {
                        Text(
                            text = "Comece a digitar pra encontrar livros.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = RodapeTheme.colors.muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        // Busca vazia pode ser "nada encontrado" OU erro de rede: o
                        // ErrorState padroniza copy + retry; "Cadastrar manualmente"
                        // fica como ação secundária.
                        ErrorState(
                            title = "Não achamos esse livro",
                            description = "Verifique a conexão e tente de novo, ou cadastre manualmente.",
                            onRetry = {
                                keyboardController?.hide()
                                viewModel.searchOpenLibrary(query)
                            },
                            action = {
                                TbButton(
                                    text = "Cadastrar manualmente",
                                    leadingIcon = RodapeIcons.Book,
                                    onClick = onNavigateToAddManual,
                                    variant = TbButtonVariant.Terra,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                )
                            }
                        )
                    }
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
                        BookResultRow(
                            doc = doc,
                            isSelected = selectedDoc == doc,
                            onClick = {
                                selectedDoc = if (selectedDoc == doc) null else doc
                            }
                        )
                    }

                    // Rodapé da lista: link discreto "Não achei meu livro"
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(RodapeRadii.sm))
                                .clickable { onNavigateToAddManual() }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Não achei meu livro · ",
                                style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
                            )
                            Text(
                                text = "cadastrar manualmente",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = RodapeTheme.colors.terracota,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
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
        RodapeDialog(
            onDismissRequest = { showJustifySheetForDoc = null },
            title = "Por que esse livro?",
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (gbConflictAuthor != null) {
                        // Banner de conflito de autor
                        Surface(
                            color = RodapeTheme.colors.warningSoft,
                            shape = RoundedCornerShape(RodapeRadii.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = RodapeIcons.Warning,
                                        contentDescription = null,
                                        tint = RodapeTheme.colors.warning,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        "Conflito de autor detectado",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = RodapeTheme.colors.warning
                                        )
                                    )
                                }
                                Text(
                                    "As fontes discordam. Qual autor está correto?",
                                    style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.warning)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { pickedAuthor = olAuthor }
                                ) {
                                    ThemedRadio(selected = pickedAuthor == olAuthor, onClick = { pickedAuthor = olAuthor })
                                    Text("$olAuthor (Open Library)", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { pickedAuthor = gbConflictAuthor }
                                ) {
                                    ThemedRadio(selected = pickedAuthor == gbConflictAuthor, onClick = { pickedAuthor = gbConflictAuthor })
                                    Text("${gbConflictAuthor} (Google Books)", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Text(
                        text = "Conte pro pessoal por que você sugere esse. Opcional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RodapeTheme.colors.muted
                    )

                    OutlinedTextField(
                        value = justificationText,
                        onValueChange = { justificationText = it },
                        placeholder = {
                            Text(
                                "Ex: É um clássico excelente e curto...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RodapeTheme.colors.muted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(RodapeRadii.sm),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = RodapeTheme.colors.cream,
                            focusedContainerColor = RodapeTheme.colors.cream,
                            unfocusedBorderColor = RodapeTheme.colors.divider,
                            focusedBorderColor = RodapeTheme.colors.terracota,
                            cursorColor = RodapeTheme.colors.terracota,
                        )
                    )
                }
            },
            confirmButton = {
                TbButton(
                    text = "Sugerir",
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

/**
 * Linha de resultado reutilizável — usada tanto pela busca quanto pela lista de
 * populares. Mesmo visual + a11y de antes; a URL da capa vira "" quando não há
 * coverI (o Cover então desenha a capa de iniciais em vez de pedir id/null-M.jpg).
 */
@Composable
private fun BookResultRow(
    doc: OpenLibraryDoc,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val author = doc.authorName?.firstOrNull() ?: "Autor desconhecido"
    val coverUrl = doc.coverI?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" } ?: ""

    // Result row — Terracota border when selected
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RodapeRadii.sm))
            .background(if (isSelected) RodapeTheme.colors.cream else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) RodapeTheme.colors.terracota else RodapeTheme.colors.divider,
                shape = RoundedCornerShape(RodapeRadii.sm)
            )
            .clickable { onClick() }
            // A11y: TalkBack lê a linha como UMA opção única
            // em vez de título/autor/ano soltos.
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = buildString {
                    append(doc.title)
                    append(", ")
                    append(author)
                    if (isSelected) append(", selecionado")
                }
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
                color = RodapeTheme.colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily
                ),
                color = RodapeTheme.colors.muted,
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
                    color = RodapeTheme.colors.muted
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(RodapeTheme.colors.terracota),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = RodapeIcons.Check,
                    contentDescription = "Selecionado",
                    tint = RodapeTheme.colors.cream,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
