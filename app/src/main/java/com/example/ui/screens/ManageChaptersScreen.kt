package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.RodapeCard
import com.example.ui.components.SkeletonRowList
import com.example.ui.components.rememberShowLoading
import com.example.data.model.Chapter
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.voting.ChapterFetchResult
import java.util.UUID

// Rascunho de capítulo com IDENTIDADE ESTÁVEL (uuid). O id acompanha a linha por
// toda edição (reordenar/renumerar), então o vínculo comentário→capítulo é o id,
// não a posição — corrige o remapeamento de comentários (B2). O id é um uuid de
// verdade, aceito pela coluna uuid do servidor (corrige o sync, P0-1).
private data class ChapterDraft(val id: String, val titulo: String)

// Mantém o id se já for uuid válido; senão gera um novo (migra ids legados
// "ch_<bookId>_<numero>", que nunca sincronizaram, pra uuid no próximo save).
private fun keepOrNewId(id: String): String =
    runCatching { UUID.fromString(id); id }.getOrElse { UUID.randomUUID().toString() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageChaptersScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val currentBook by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()

    // Seed do rascunho UMA vez. Usar remember(chapters) reseta a lista quando um
    // reload em tempo real troca a referência de `chapters` no meio da edição,
    // apagando alterações não salvas. Seed uma vez e não sobrescreve depois.
    var draftList by remember {
        mutableStateOf(chapters.map { ChapterDraft(keepOrNewId(it.id), it.titulo) }.toMutableList())
    }
    // Snapshot do rascunho carregado/salvo. Comparar com draftList diz se há edições
    // não salvas (confirma o descarte ao sair) sem precisar marcar "dirty" à mão em
    // cada mutação.
    var baseline by remember { mutableStateOf(draftList.toList()) }
    var seeded by remember { mutableStateOf(chapters.isNotEmpty()) }
    LaunchedEffect(chapters) {
        if (!seeded && chapters.isNotEmpty()) {
            val seededDraft = chapters.map { ChapterDraft(keepOrNewId(it.id), it.titulo) }.toMutableList()
            draftList = seededDraft
            baseline = seededDraft.toList()
            seeded = true
        }
    }
    var fetching by remember { mutableStateOf(false) }
    var apiBanner by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }
    var showFetchConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var genCount by rememberSaveable { mutableStateOf("") }
    var shareToCommunity by rememberSaveable { mutableStateOf(true) }

    val hasUnsavedEdits = draftList != baseline

    // Executa a busca online e SOBRESCREVE o rascunho. Só deve ser chamada depois
    // de confirmar quando já existem capítulos (a confirmação fica em requestFetch).
    val runFetch = {
        val book = currentBook
        if (book != null) {
            fetching = true
            apiBanner = null
            apiError = false
            viewModel.fetchChaptersOnline(book) { result ->
                fetching = false
                when (result) {
                    is ChapterFetchResult.Success -> {
                        draftList = result.chapters
                            .map { ChapterDraft(UUID.randomUUID().toString(), it.second) }
                            .toMutableList()
                        apiError = false
                        apiBanner = "Encontramos ${result.chapters.size} capítulos. Revise e salve."
                    }
                    is ChapterFetchResult.Failed -> {
                        apiError = true
                        apiBanner = "Não encontramos os capítulos. Adicione manualmente."
                    }
                }
            }
        }
    }
    // Buscar sobrescreve a lista inteira: se já há capítulos, confirma antes (P0 —
    // sem isso, um toque apagava o índice sem aviso e sem desfazer).
    val requestFetch = {
        if (draftList.isNotEmpty()) showFetchConfirm = true else runFetch()
    }
    val addChapter = {
        draftList = (draftList + ChapterDraft(UUID.randomUUID().toString(), "")).toMutableList()
    }
    val generateChapters = {
        val n = genCount.toIntOrNull() ?: 0
        if (n in 1..500) {
            draftList = (draftList + (1..n).map {
                ChapterDraft(UUID.randomUUID().toString(), "")
            }).toMutableList()
            genCount = ""
        }
    }
    // Sair descarta o rascunho: confirma quando há edições não salvas (P1).
    val requestDiscard = {
        if (hasUnsavedEdits) showDiscardConfirm = true else onNavigateBack()
    }

    BackHandler { requestDiscard() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capítulos") },
                navigationIcon = {
                    IconButton(onClick = requestDiscard) {
                        Icon(RodapeIcons.Back, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Botão com TEXTO visível (P1): a busca online estava escondida atrás
                    // de um ícone com rótulo só de acessibilidade. Aparece quando já há
                    // capítulos; no estado vazio, o caminho primário fica no corpo da tela.
                    if (currentBook != null && draftList.isNotEmpty()) {
                        TextButton(onClick = requestFetch, enabled = !fetching) {
                            Text("Buscar capítulos online", color = RodapeTheme.colors.terracota)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentBook == null) {
                // Beco sem saída (U6): sem livro atual o admin não tem o que fazer aqui.
                // Ícone + microcopy orientam pra ação certa. Não há callback de navegação
                // pra "definir livro" nesta assinatura, então guiamos por texto + o back do topo.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        RodapeIcons.Book,
                        contentDescription = null,
                        tint = RodapeTheme.colors.muted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Sem livro atual no clube.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = RodapeTheme.colors.ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Defina o livro atual em Gerenciar clube › Livro atual antes de cadastrar capítulos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RodapeTheme.colors.muted,
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            if (apiBanner != null) {
                // Falha usa tom terracota (P2): o verde OlivaSoft é cor de sucesso e
                // dava a impressão errada quando a busca NÃO encontrava os capítulos.
                Surface(
                    color = if (apiError) RodapeTheme.colors.terracotaSoft else RodapeTheme.colors.olivaSoft,
                    shape = RoundedCornerShape(RodapeRadii.sm),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Text(
                        apiBanner!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (apiError) RodapeTheme.colors.terracotaDark else RodapeTheme.colors.olivaDark,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (fetching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = RodapeTheme.colors.terracota)
            }

            // Local-first: o Room emite lista vazia no cold start antes do 1º sync.
            // Mostra skeleton dentro da janela de graça em vez do estado vazio piscando.
            val showChaptersLoading = rememberShowLoading(hasData = chapters.isNotEmpty())
            if (showChaptersLoading) {
                SkeletonRowList(
                    count = 4,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                return@Column
            }

            if (draftList.isEmpty()) {
                // Estado vazio (P3): sem capítulos, a tela ficava quase em branco.
                // Aqui o título dá contexto e os três caminhos ficam à mão — buscar
                // (ação primária), gerar numerados e adicionar manualmente.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        RodapeIcons.Book,
                        contentDescription = null,
                        tint = RodapeTheme.colors.muted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Este livro ainda não tem capítulos.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = RodapeTheme.colors.ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Cadastre os capítulos para o clube discutir e marcar o progresso da leitura.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RodapeTheme.colors.muted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TbButton(
                        text = "Buscar capítulos online",
                        onClick = requestFetch,
                        variant = TbButtonVariant.Terra,
                        enabled = !fetching,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ChapterGeneratorSection(
                        genCount = genCount,
                        onGenCountChange = { genCount = it.filter { c -> c.isDigit() }.take(3) },
                        onGenerate = generateChapters,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TbButton(
                        text = "Adicionar manualmente",
                        onClick = addChapter,
                        variant = TbButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(draftList, key = { _, d -> d.id }) { idx, draft ->
                    // 3.7: linha COLAPSADA em "Cap.N · título · ⋮" — subir/descer/
                    // remover moram no menu (eram 3 botões espremidos ao lado do
                    // campo). Reordenar ANIMA (animateItem). Terracota reservado
                    // pro destrutivo (Remover), dentro do menu.
                    RodapeCard(modifier = Modifier.fillMaxWidth().animateItem()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                // Numero = posição na lista (recalculado ao salvar). O
                                // id (uuid) é a identidade real; o numero é só ordem.
                                "Cap. ${idx + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = RodapeTheme.colors.muted
                                ),
                                modifier = Modifier.width(60.dp)
                            )
                            OutlinedTextField(
                                value = draft.titulo,
                                onValueChange = { newTitle ->
                                    draftList = draftList.toMutableList().apply {
                                        this[idx] = draft.copy(titulo = newTitle)
                                    }
                                },
                                placeholder = { Text("Título (opcional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            var showRowMenu by remember(draft.id) { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showRowMenu = true }) {
                                    Icon(
                                        RodapeIcons.MoreV,
                                        contentDescription = "Ações do capítulo ${idx + 1}",
                                        tint = RodapeTheme.colors.muted,
                                    )
                                }
                                DropdownMenu(
                                    expanded = showRowMenu,
                                    onDismissRequest = { showRowMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Subir") },
                                        enabled = idx > 0,
                                        leadingIcon = { Icon(RodapeIcons.ChevU, contentDescription = null) },
                                        onClick = {
                                            showRowMenu = false
                                            if (idx > 0) {
                                                draftList = draftList.toMutableList().apply {
                                                    val tmp = this[idx - 1]
                                                    this[idx - 1] = draft
                                                    this[idx] = tmp
                                                }
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Descer") },
                                        enabled = idx < draftList.size - 1,
                                        leadingIcon = { Icon(RodapeIcons.ChevD, contentDescription = null) },
                                        onClick = {
                                            showRowMenu = false
                                            if (idx < draftList.size - 1) {
                                                draftList = draftList.toMutableList().apply {
                                                    val tmp = this[idx + 1]
                                                    this[idx + 1] = draft
                                                    this[idx] = tmp
                                                }
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Remover", color = RodapeTheme.colors.terracota) },
                                        leadingIcon = {
                                            Icon(RodapeIcons.Trash, contentDescription = null, tint = RodapeTheme.colors.terracota)
                                        },
                                        onClick = {
                                            showRowMenu = false
                                            draftList = draftList.toMutableList().apply { removeAt(idx) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    TbButton(
                        text = "+ Adicionar capítulo",
                        onClick = addChapter,
                        variant = TbButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                item {
                    // Gerador rápido: cria N capítulos numerados de uma vez (títulos
                    // opcionais). As APIs de livro não fornecem a lista de capítulos de
                    // forma confiável, então o caminho prático é informar quantos são.
                    ChapterGeneratorSection(
                        genCount = genCount,
                        onGenCountChange = { genCount = it.filter { c -> c.isDigit() }.take(3) },
                        onGenerate = generateChapters,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TbButton(
                    text = "Cancelar",
                    onClick = requestDiscard,
                    variant = TbButtonVariant.Outline,
                    modifier = Modifier.weight(1f)
                )
                TbButton(
                    text = "Salvar",
                    onClick = { showSaveConfirm = true },
                    variant = TbButtonVariant.Terra,
                    modifier = Modifier.weight(1f)
                )
            }

            if (showSaveConfirm) {
                val hasIsbn = currentBook?.isbn?.isNotBlank() == true
                AlertDialog(
                    onDismissRequest = { showSaveConfirm = false },
                    title = { Text("Salvar capítulos?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Editar os capítulos altera o cronograma de leitura do clube. " +
                                    "Os comentários dos capítulos mantidos são preservados."
                            )
                            if (hasIsbn) {
                                // Crowdsourcing: compartilhar o índice por ISBN ajuda TODOS
                                // os clubes que lerem este livro depois (um cadastro serve o
                                // app inteiro). Marcado por padrão pra a comunidade crescer.
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { shareToCommunity = !shareToCommunity }
                                ) {
                                    Checkbox(
                                        checked = shareToCommunity,
                                        onCheckedChange = { shareToCommunity = it }
                                    )
                                    Text(
                                        "Compartilhar este índice com a comunidade (ajuda outros clubes)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            // numero = posição (1..N contígua); id (uuid) é a identidade
                            // estável que preserva os comentários ao reordenar.
                            val book = currentBook!!
                            val chaptersToSave = draftList.mapIndexed { i, d ->
                                Chapter(id = d.id, bookId = book.id, numero = i + 1, titulo = d.titulo)
                            }
                            viewModel.upsertChapters(book.id, chaptersToSave)
                            if (hasIsbn && shareToCommunity) {
                                viewModel.shareChapterTemplate(
                                    book, chaptersToSave.map { it.numero to it.titulo }
                                )
                            }
                            showSaveConfirm = false
                            onNavigateBack()
                        }) { Text("Salvar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveConfirm = false }) {
                            Text("Cancelar", color = RodapeTheme.colors.muted)
                        }
                    }
                )
            }

            if (showFetchConfirm) {
                // P0: buscar online sobrescreve a lista inteira. Confirma antes de
                // descartar o que já existe (não há como desfazer depois).
                AlertDialog(
                    onDismissRequest = { showFetchConfirm = false },
                    title = { Text("Substituir os capítulos atuais?") },
                    text = {
                        Text(
                            "Isso substitui os capítulos atuais. Você ainda pode revisar e ajustar antes de salvar."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showFetchConfirm = false
                            runFetch()
                        }) { Text("Continuar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFetchConfirm = false }) {
                            Text("Cancelar", color = RodapeTheme.colors.muted)
                        }
                    }
                )
            }

            if (showDiscardConfirm) {
                // P1: sair (voltar/cancelar/back) descartava o rascunho sem aviso.
                AlertDialog(
                    onDismissRequest = { showDiscardConfirm = false },
                    title = { Text("Descartar as alterações?") },
                    text = {
                        Text(
                            "Você tem capítulos editados que ainda não foram salvos. Se sair agora, você perde essas alterações."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showDiscardConfirm = false
                            onNavigateBack()
                        }) { Text("Descartar", color = RodapeTheme.colors.terracota, fontWeight = FontWeight.SemiBold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardConfirm = false }) {
                            Text("Continuar editando", color = RodapeTheme.colors.muted)
                        }
                    }
                )
            }
        }
    }
}

// Gerador "informe N capítulos", com microcopy explicando pra que serve (P2). Fica
// num composable só pra ser reusado no estado vazio e no rodapé da lista.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterGeneratorSection(
    genCount: String,
    onGenCountChange: (String) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Não sabe os títulos? Gere N capítulos numerados.",
            style = MaterialTheme.typography.bodySmall,
            color = RodapeTheme.colors.muted
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = genCount,
                onValueChange = onGenCountChange,
                placeholder = { Text("Nº de capítulos") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TbButton(
                text = "Gerar",
                onClick = onGenerate,
                variant = TbButtonVariant.OlivaSoft,
                enabled = (genCount.toIntOrNull() ?: 0) in 1..500,
            )
        }
    }
}
