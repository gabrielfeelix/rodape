package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.Refresh
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
    var seeded by remember { mutableStateOf(chapters.isNotEmpty()) }
    LaunchedEffect(chapters) {
        if (!seeded && chapters.isNotEmpty()) {
            draftList = chapters.map { ChapterDraft(keepOrNewId(it.id), it.titulo) }.toMutableList()
            seeded = true
        }
    }
    var fetching by remember { mutableStateOf(false) }
    var apiBanner by remember { mutableStateOf<String?>(null) }
    var showSaveConfirm by remember { mutableStateOf(false) }
    var genCount by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capítulos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (currentBook != null) {
                        IconButton(
                            onClick = {
                                fetching = true
                                apiBanner = null
                                viewModel.fetchChaptersOnline(currentBook!!) { result ->
                                    fetching = false
                                    when (result) {
                                        is ChapterFetchResult.Success -> {
                                            draftList = result.chapters
                                                .map { ChapterDraft(UUID.randomUUID().toString(), it.second) }
                                                .toMutableList()
                                            apiBanner = "Encontramos ${result.chapters.size} capítulos. Revisa e salva."
                                        }
                                        is ChapterFetchResult.Failed -> {
                                            apiBanner = "Não encontramos os capítulos. Adicione manualmente."
                                        }
                                    }
                                }
                            },
                            enabled = !fetching
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Buscar online")
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
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Sem livro atual no clube.",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Defina o livro atual em Gerenciar clube › Livro atual antes de cadastrar capítulos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            if (apiBanner != null) {
                Surface(
                    color = OlivaSoft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Text(
                        apiBanner!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OlivaDark,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (fetching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Terracota)
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

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(draftList, key = { _, d -> d.id }) { idx, draft ->
                    RodapeCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                // Numero = posição na lista (recalculado ao salvar). O
                                // id (uuid) é a identidade real; o numero é só ordem.
                                "Cap. ${idx + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Muted
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
                            IconButton(
                                onClick = {
                                    if (idx > 0) {
                                        draftList = draftList.toMutableList().apply {
                                            val tmp = this[idx - 1]
                                            this[idx - 1] = draft
                                            this[idx] = tmp
                                        }
                                    }
                                },
                                enabled = idx > 0
                            ) { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Subir") }
                            IconButton(
                                onClick = {
                                    if (idx < draftList.size - 1) {
                                        draftList = draftList.toMutableList().apply {
                                            val tmp = this[idx + 1]
                                            this[idx + 1] = draft
                                            this[idx] = tmp
                                        }
                                    }
                                },
                                enabled = idx < draftList.size - 1
                            ) { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Descer") }
                            IconButton(onClick = {
                                draftList = draftList.toMutableList().apply { removeAt(idx) }
                            }) { Icon(Icons.Outlined.Delete, contentDescription = "Remover", tint = Terracota) }
                        }
                    }
                }
                item {
                    TbButton(
                        text = "+ Adicionar capítulo",
                        onClick = {
                            draftList = (draftList + ChapterDraft(UUID.randomUUID().toString(), "")).toMutableList()
                        },
                        variant = TbButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                item {
                    // Gerador rápido: cria N capítulos de uma vez (títulos opcionais).
                    // As APIs de livro não fornecem a lista de capítulos de forma
                    // confiável, então o caminho prático é informar quantos são.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = genCount,
                            onValueChange = { genCount = it.filter { c -> c.isDigit() }.take(3) },
                            placeholder = { Text("Nº de capítulos") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TbButton(
                            text = "Gerar",
                            onClick = {
                                val n = genCount.toIntOrNull() ?: 0
                                if (n in 1..500) {
                                    draftList = (draftList + (1..n).map {
                                        ChapterDraft(UUID.randomUUID().toString(), "")
                                    }).toMutableList()
                                    genCount = ""
                                }
                            },
                            variant = TbButtonVariant.OlivaSoft,
                            enabled = (genCount.toIntOrNull() ?: 0) in 1..500,
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
                    onClick = onNavigateBack,
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
                AlertDialog(
                    onDismissRequest = { showSaveConfirm = false },
                    title = { Text("Salvar capítulos?") },
                    text = {
                        Text(
                            "Editar os capítulos altera o cronograma de leitura do clube. " +
                                "Os comentários dos capítulos mantidos são preservados."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            // numero = posição (1..N contígua); id (uuid) é a identidade
                            // estável que preserva os comentários ao reordenar.
                            val bookId = currentBook!!.id
                            val chaptersToSave = draftList.mapIndexed { i, d ->
                                Chapter(id = d.id, bookId = bookId, numero = i + 1, titulo = d.titulo)
                            }
                            viewModel.upsertChapters(bookId, chaptersToSave)
                            showSaveConfirm = false
                            onNavigateBack()
                        }) { Text("Salvar", color = Terracota, fontWeight = FontWeight.SemiBold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveConfirm = false }) {
                            Text("Cancelar", color = Muted)
                        }
                    }
                )
            }
        }
    }
}
