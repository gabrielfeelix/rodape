package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.example.ui.viewmodel.MainViewModel
import com.example.util.formatShortDate
import com.example.util.timeAgo

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
                                imageVector = RodapeIcons.Back,
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
                // Gate: em cold start clubBooks ainda está sincronizando e o
                // livro "não existe" por 1-2s — mostra loading em vez de erro.
                if (com.example.ui.components.rememberShowLoading(hasData = clubBooks.isNotEmpty())) {
                    com.example.ui.components.CenteredLoading()
                } else {
                    Text(
                        text = "Livro não encontrado.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = RodapeTheme.colors.muted
                    )
                }
            }
        }
        return
    }

    var tab by rememberSaveable { mutableStateOf("resumo") }

    // --- Quote dialog state ---
    var showQuoteDialog by rememberSaveable { mutableStateOf(false) }
    var quoteText by rememberSaveable { mutableStateOf("") }
    var quoteRef by rememberSaveable { mutableStateOf("") }

    // --- Flows ---
    val quotesFlow = remember(bookId) { viewModel.getSavedQuotesForBook(bookId) }
    val quotes by quotesFlow.collectAsState(initial = emptyList())
    val meetingDates by viewModel.finishedBooksMeetingDates.collectAsState()
    val dataEncontro = meetingDates[bookId]
    val isFavorite by remember(bookId) { viewModel.isBookFavoriteFlow(bookId) }.collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RodapeTheme.colors.olivaSoft, RodapeTheme.colors.paper)))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // ── HERO ────────────────────────────────────────────────────────────
        // Gradiente vive só na Column externa; repetir aqui criava uma costura
        // visível (o ramo reiniciava e batia em `paper` no fim do hero).
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Cover(
                    title = book.title,
                    author = book.author,
                    coverUrl = book.coverUrl,
                    width = 150.dp,
                    height = 224.dp,
                    // Sombra dramática do design (screens-book-detail.jsx:98)
                    modifier = Modifier.detailCoverShadow(cornerRadius = 3.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

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

                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        color = RodapeTheme.colors.muted
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 8.dp, start = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RodapeTheme.colors.cardSurface.copy(alpha = 0.85f))
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = RodapeIcons.Back,
                    contentDescription = "Voltar",
                    tint = RodapeTheme.colors.ink,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ♥ Favoritar — pessoal, cross-clube. Espelha a posição do botão Voltar.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RodapeTheme.colors.cardSurface.copy(alpha = 0.85f))
                    .clickable { viewModel.toggleBookFavorite(bookId, !isFavorite) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                    tint = if (isFavorite) RodapeTheme.colors.terracota else RodapeTheme.colors.ink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── BANNER (com data do encontro quando disponível) ─────────────────
        val bannerText = if (dataEncontro != null) {
            "Lido pelo clube · Encontro em ${formatShortDate(dataEncontro)}"
        } else {
            "Lido pelo clube"
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(RodapeRadii.md))
                .background(RodapeTheme.colors.oliva)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bannerText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = RodapeTheme.colors.cream
                ),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── TABS ─────────────────────────────────────────────────────────────
        val tabs = listOf(
            "resumo" to "Resumo",
            "frases" to "Frases",
            "chat" to "Chat",
            "avaliacoes" to "Avaliações",
            "historico" to "Histórico"
        )

        // C3: abas roláveis (padrão ScrollableTabRow) em vez de 5 slots weight(1f)
        // com maxLines=1+ellipsis. Em fonte A++ (1.3×) + escala do sistema, os
        // rótulos ("Avaliações") truncavam pra "Avaliaçõ…". Agora cada aba tem
        // largura intrínseca (não corta) e a fileira rola na horizontal se não couber.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { (key, label) ->
                val isSelected = tab == key
                Column(
                    modifier = Modifier
                        .clickable { tab = key }
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) RodapeTheme.colors.ink else RodapeTheme.colors.muted,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (isSelected) RodapeTheme.colors.terracota else RodapeTheme.colors.dividerSoft,
                                RoundedCornerShape(RodapeRadii.full)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Fade-through na troca de aba do livro (era troca seca). O SizeTransform
            // default do AnimatedContent acomoda a diferença de altura entre abas.
            // Specs hoisteados: transitionSpec não é contexto @Composable.
            val bdEnterSpec = rodapeTween<Float>(RodapeMotion.Dur.standard)
            val bdExitSpec = rodapeTween<Float>(RodapeMotion.Dur.fast)
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(bdEnterSpec) togetherWith fadeOut(bdExitSpec)
                },
                label = "bookDetailTab",
            ) { t ->
                when (t) {
                    "resumo" -> SummaryTab(viewModel = viewModel, bookId = bookId)
                    "frases" -> FrasesTab(
                        viewModel = viewModel,
                        quotes = quotes,
                        onShowQuoteDialog = { showQuoteDialog = true }
                    )
                    "chat" -> ChatTab(viewModel = viewModel, bookId = bookId)
                    "avaliacoes" -> RatingsTab(viewModel = viewModel, bookId = bookId)
                    "historico" -> HistoryTab(viewModel = viewModel, bookId = bookId, dataEncontro = dataEncontro)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ── SAVE QUOTE DIALOG ────────────────────────────────────────────────────
    if (showQuoteDialog) {
        RodapeDialog(
            onDismissRequest = {
                showQuoteDialog = false
                quoteText = ""
                quoteRef = ""
            },
            title = "Salvar frase",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quoteText,
                        onValueChange = { quoteText = it },
                        label = { Text("Frase do livro") },
                        placeholder = { Text("Escreva a frase aqui…") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quoteRef,
                        onValueChange = { quoteRef = it },
                        label = { Text("Capítulo (opcional)") },
                        placeholder = { Text("Ex: Cap. 5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (quoteText.isNotBlank()) {
                            viewModel.saveQuote(bookId, quoteText, quoteRef.trim())
                        }
                        showQuoteDialog = false
                        quoteText = ""
                        quoteRef = ""
                    },
                    enabled = quoteText.isNotBlank()
                ) {
                    Text(text = "Salvar", color = RodapeTheme.colors.oliva, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showQuoteDialog = false
                        quoteText = ""
                        quoteRef = ""
                    }
                ) {
                    Text(text = "Cancelar", color = RodapeTheme.colors.muted)
                }
            }
        )
    }
}

// ── TAB: RESUMO ──────────────────────────────────────────────────────────────
@Composable
private fun SummaryTab(viewModel: MainViewModel, bookId: String) {
    val summaryFlow = remember(bookId) { viewModel.getBookSummaryFlow(bookId) }
    val summary by summaryFlow.collectAsState(initial = null)
    val members by viewModel.clubMembers.collectAsState()
    val membersById = remember(members) { members.associateBy { it.id } }
    var showEditDialog by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }

    val showLoading = rememberShowLoading(hasData = summary != null)
    if (showLoading) {
        SkeletonRowList(count = 3)
    } else if (summary == null) {
        RodapeCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Ninguém escreveu o resumo ainda. Que tal começar?",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    color = RodapeTheme.colors.muted
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TbButton(
            text = "Escrever resumo",
            onClick = { draftText = ""; showEditDialog = true },
            variant = TbButtonVariant.Outline,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        RodapeCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = summary!!.texto,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    color = RodapeTheme.colors.inkSoft,
                    lineHeight = 22.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val editorName = membersById[summary!!.lastEditorId]?.nome ?: "alguém"
        Text(
            text = "Editado por $editorName · ${timeAgo(summary!!.updatedAt)}",
            style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        TbButton(
            text = "Editar resumo",
            onClick = { draftText = summary!!.texto; showEditDialog = true },
            variant = TbButtonVariant.Outline,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showEditDialog) {
        RodapeDialog(
            onDismissRequest = { showEditDialog = false },
            title = "Resumo do livro",
            text = {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    placeholder = { Text("O clube leu esse livro — conta o que rolou.") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draftText.isNotBlank()) {
                        viewModel.saveBookSummary(bookId, draftText)
                    }
                    showEditDialog = false
                }) {
                    Text("Salvar", color = RodapeTheme.colors.oliva, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = RodapeTheme.colors.muted)
                }
            }
        )
    }
}

// ── TAB: FRASES ──────────────────────────────────────────────────────────────
@Composable
private fun FrasesTab(
    viewModel: MainViewModel,
    quotes: List<com.example.data.model.SavedQuote>,
    onShowQuoteDialog: () -> Unit
) {
    val members by viewModel.clubMembers.collectAsState()
    val membersById = remember(members) { members.associateBy { it.id } }
    val showLoading = rememberShowLoading(hasData = quotes.isNotEmpty())
    if (showLoading) {
        SkeletonRowList(count = 3)
    } else if (quotes.isEmpty()) {
        Text(
            text = "Nenhuma frase guardada deste livro ainda.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = InterFontFamily,
                color = RodapeTheme.colors.muted
            ),
            modifier = Modifier.padding(vertical = 16.dp)
        )
    } else {
        // Confirmação antes de excluir — antes apagava direto, sem volta.
        var quoteToDelete by remember { mutableStateOf<com.example.data.model.SavedQuote?>(null) }
        quoteToDelete?.let { pending ->
            RodapeDialog(
                onDismissRequest = { quoteToDelete = null },
                title = "Excluir frase?",
                text = { Text("A frase salva some pra todo o clube.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteQuote(pending)
                        quoteToDelete = null
                    }) { Text("Excluir") }
                },
                dismissButton = {
                    TextButton(onClick = { quoteToDelete = null }) { Text("Voltar") }
                },
            )
        }
        quotes.forEach { quote ->
            val author = membersById[quote.userId]
            val authorName = author?.nome ?: "Membro"
            val authorAvatar = author?.avatarUrl ?: ""

            Column {
                // 3.8: lixeira saiu de dentro do keepsake — long-press abre a
                // confirmação de apagar (mesmo dialog de antes).
                QuoteCard(
                    texto = quote.texto,
                    ref = quote.capituloRef,
                    onLongPress = { quoteToDelete = quote }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                ) {
                    Avatar(name = authorName, avatarUrl = authorAvatar, size = 20.dp)
                    Text(
                        text = "Salva por $authorName",
                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    TbButton(
        text = "Salvar uma frase",
        onClick = onShowQuoteDialog,
        variant = TbButtonVariant.Outline,
        size = TbButtonSize.Md,
        modifier = Modifier.fillMaxWidth()
    )
}

// ── TAB: CHAT ────────────────────────────────────────────────────────────────
@Composable
private fun ChatTab(viewModel: MainViewModel, bookId: String) {
    val chaptersFlow = remember(bookId) { viewModel.getCommentsForBookFlow(bookId) }
    val comments by chaptersFlow.collectAsState(initial = emptyList())
    val members by viewModel.clubMembers.collectAsState()
    val membersById = remember(members) { members.associateBy { it.id } }

    // Buscar chapters: precisamos resolver chapterId -> Chapter pra header.
    // Como `currentChapters` é só do livro atual do clube, usar uma fonte mais geral:
    // como Comment já tem chapterId, agrupamos por chapterId.
    // Para mostrar título do capítulo, precisamos consultar os chapters do livro.
    // Reusamos currentChapters quando bate, senão fallback genérico.
    val currentChapters by viewModel.currentChapters.collectAsState()
    val chapterById = remember(currentChapters) { currentChapters.associateBy { it.id } }

    val showLoading = rememberShowLoading(hasData = comments.isNotEmpty())
    if (showLoading) {
        SkeletonRowList(count = 3)
    } else if (comments.isEmpty()) {
        Text(
            text = "Esse livro não rendeu conversa por aqui.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = InterFontFamily,
                color = RodapeTheme.colors.muted
            ),
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = "(somente leitura — comente em \"Livro atual\")",
            style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    } else {
        var lastChapterId: String? = null
        comments.forEach { c ->
            val chapter = chapterById[c.chapterId]
            if (c.chapterId != lastChapterId) {
                lastChapterId = c.chapterId
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(RodapeTheme.colors.dividerSoft))
                    Text(
                        text = if (chapter != null) "CAP. ${chapter.numero} · ${chapter.titulo}" else "CAPÍTULO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = RodapeTheme.colors.olivaMid,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(RodapeTheme.colors.dividerSoft))
                }
            }

            val author = membersById[c.userId]
            val authorName = author?.nome ?: "Membro"
            val authorAvatar = author?.avatarUrl ?: ""

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Avatar(name = authorName, avatarUrl = authorAvatar, size = 32.dp)
                Column(modifier = Modifier.weight(1f)) {
                    // 3.6: mesma bolha Literata do DiscussionScreen — aqui em modo
                    // leitura (histórico, sem reações): cream + borda leve + corpo Literata.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RodapeTheme.colors.cream, RoundedCornerShape(RodapeRadii.sm))
                            .border(0.5.dp, RodapeTheme.colors.divider, RoundedCornerShape(RodapeRadii.sm))
                            .clip(RoundedCornerShape(RodapeRadii.sm))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = authorName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RodapeTheme.colors.ink
                                    )
                                )
                                Text(
                                    text = timeAgo(c.criadoEm),
                                    style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                                )
                            }
                            if (c.removido) {
                                Text(
                                    text = "[mensagem removida pela moderação]",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontSize = 14.5.sp,
                                        lineHeight = 21.sp,
                                        color = RodapeTheme.colors.muted,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                )
                            } else {
                                Text(
                                    text = c.texto,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = LiterataFontFamily,
                                        fontSize = 14.5.sp,
                                        lineHeight = 21.sp,
                                        color = RodapeTheme.colors.ink
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "(somente leitura — comente em \"Livro atual\")",
            style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// ── TAB: AVALIAÇÕES ──────────────────────────────────────────────────────────
@Composable
private fun RatingsTab(viewModel: MainViewModel, bookId: String) {
    val ratingsFlow = remember(bookId) { viewModel.getBookRatingsFlow(bookId) }
    val ratings by ratingsFlow.collectAsState(initial = emptyList())
    val myRatingFlow = remember(bookId) { viewModel.getBookRatingOfCurrentUserFlow(bookId) }
    val myRating by myRatingFlow.collectAsState(initial = null)
    val members by viewModel.clubMembers.collectAsState()
    val membersById = remember(members) { members.associateBy { it.id } }

    var showRatingDialog by remember { mutableStateOf(false) }
    var draftStars by remember { mutableStateOf(0) }
    var draftComment by remember { mutableStateOf("") }

    val avg = if (ratings.isNotEmpty()) ratings.sumOf { it.stars }.toFloat() / ratings.size else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (ratings.isEmpty()) {
            // Estado vazio distinto: "0.0 de 5" com estrelas vazias parecia nota
            // ZERO, não "ainda não avaliado" — enganava e desmotivava.
            RatingStars(rating = 0f, size = 28.dp, spacing = 4.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ninguém avaliou ainda — seja o primeiro.",
                style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
            )
        } else {
            RatingStars(rating = avg, size = 28.dp, spacing = 4.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${"%.1f".format(avg)} de 5 · ${ratings.size} ${if (ratings.size == 1) "avaliação" else "avaliações"}",
                style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.muted)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    TbButton(
        text = if (myRating != null) "Editar minha avaliação" else "Avaliar este livro",
        onClick = {
            draftStars = myRating?.stars ?: 0
            draftComment = myRating?.comment ?: ""
            showRatingDialog = true
        },
        variant = TbButtonVariant.Terra,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(20.dp))

    val sortedRatings = remember(ratings) { ratings.sortedByDescending { it.updatedAt } }
    val showLoading = rememberShowLoading(hasData = ratings.isNotEmpty())
    if (showLoading) {
        SkeletonRowList(count = 3)
    } else {
        sortedRatings.forEach { r ->
            val author = membersById[r.userId]
            val authorName = author?.nome ?: "Membro"
            val authorAvatar = author?.avatarUrl ?: ""

            RodapeCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Avatar(name = authorName, avatarUrl = authorAvatar, size = 28.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authorName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = RodapeTheme.colors.ink
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RatingStars(rating = r.stars.toFloat(), size = 12.dp)
                            Text(
                                text = timeAgo(r.updatedAt),
                                style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted)
                            )
                        }
                    }
                }
                if (r.comment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = r.comment,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = RodapeTheme.colors.inkSoft,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    val totalMembers = members.size
    val ratedCount = ratings.size
    if (totalMembers > ratedCount) {
        Text(
            text = "${totalMembers - ratedCount} ${if (totalMembers - ratedCount == 1) "membro ainda não avaliou" else "membros ainda não avaliaram"}",
            style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }

    if (showRatingDialog) {
        RodapeDialog(
            onDismissRequest = { showRatingDialog = false },
            title = "Avaliar livro",
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RatingStarsInput(selected = draftStars, onChange = { draftStars = it })
                    OutlinedTextField(
                        value = draftComment,
                        onValueChange = { draftComment = it },
                        placeholder = { Text("Conte o que você achou (opcional)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draftStars in 1..5) {
                            viewModel.saveBookRating(bookId, draftStars, draftComment)
                            showRatingDialog = false
                        }
                    },
                    enabled = draftStars in 1..5
                ) {
                    Text("Salvar", color = RodapeTheme.colors.oliva, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) {
                    Text("Cancelar", color = RodapeTheme.colors.muted)
                }
            }
        )
    }
}

// ── TAB: HISTÓRICO ───────────────────────────────────────────────────────────
@Composable
private fun HistoryTab(viewModel: MainViewModel, bookId: String, dataEncontro: Long?) {
    val suggestionFlow = remember(bookId) { viewModel.getBookSuggestionFlow(bookId) }
    val suggestion by suggestionFlow.collectAsState(initial = null)
    val members by viewModel.clubMembers.collectAsState()
    val isAdmin by viewModel.isCurrentUserAdmin.collectAsState()

    val suggester = suggestion?.let { s -> members.find { it.id == s.suggestedByUserId }?.nome }

    // 3.6: timeline SÓ com marcos que têm dado real — "Leitura começou" era
    // hardcoded (sempre presente, sem data) e fingia ser data-driven; "Encontro"
    // sem data idem. Sem dado → o marco não entra.
    val milestones = buildList<Pair<String, String>> {
        add(
            "Sugerido${suggester?.let { " por $it" } ?: ""}" to
            (suggestion?.let { "em ${formatShortDate(it.criadoEm)}" } ?: "O livro chegou na lista do clube.")
        )
        dataEncontro?.let {
            add("Encontro do clube" to "em ${formatShortDate(it)}")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        milestones.forEachIndexed { index, (title, desc) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    Box(modifier = Modifier.size(14.dp).background(RodapeTheme.colors.oliva, CircleShape))
                    if (index < milestones.size - 1) {
                        Box(modifier = Modifier.width(2.dp).height(56.dp).background(RodapeTheme.colors.olivaSoft))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = RodapeTheme.colors.ink
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            color = RodapeTheme.colors.muted
                        )
                    )
                    Spacer(modifier = Modifier.height(if (index < milestones.size - 1) 44.dp else 0.dp))
                }
            }
        }
    }

    if (isAdmin) {
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(thickness = 0.5.dp, color = RodapeTheme.colors.divider)
        Spacer(modifier = Modifier.height(12.dp))
        Overline(text = "ADMIN", color = RodapeTheme.colors.terracota)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Data do encontro:",
                style = MaterialTheme.typography.bodyMedium.copy(color = RodapeTheme.colors.ink)
            )
            Text(
                text = dataEncontro?.let { formatShortDate(it) } ?: "não definida",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = RodapeTheme.colors.terracota,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TbButton(
                text = "Marcar como hoje",
                onClick = { viewModel.setBookMeetingDate(bookId, System.currentTimeMillis()) },
                variant = TbButtonVariant.Outline,
                size = TbButtonSize.Sm
            )
            if (dataEncontro != null) {
                TbButton(
                    text = "Limpar",
                    onClick = { viewModel.setBookMeetingDate(bookId, null) },
                    variant = TbButtonVariant.Outline,
                    size = TbButtonSize.Sm
                )
            }
        }
    }
}
