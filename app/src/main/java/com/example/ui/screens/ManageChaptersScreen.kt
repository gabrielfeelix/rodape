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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.TramabookCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.voting.ChapterFetchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageChaptersScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val currentBook by viewModel.currentBook.collectAsState()
    val chapters by viewModel.currentChapters.collectAsState()

    var draftList by remember(chapters) {
        mutableStateOf(chapters.map { it.numero to it.titulo }.toMutableList())
    }
    var fetching by remember { mutableStateOf(false) }
    var apiBanner by remember { mutableStateOf<String?>(null) }

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
                                            draftList = result.chapters.toMutableList()
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sem livro atual no clube.", style = MaterialTheme.typography.bodyLarge, color = Muted)
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

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(draftList) { idx, pair ->
                    TramabookCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Cap. ${pair.first}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Muted
                                ),
                                modifier = Modifier.width(60.dp)
                            )
                            OutlinedTextField(
                                value = pair.second,
                                onValueChange = { newTitle ->
                                    draftList = draftList.toMutableList().apply {
                                        this[idx] = pair.first to newTitle
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
                                            this[idx - 1] = pair
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
                                            this[idx + 1] = pair
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
                            val nextNum = (draftList.maxOfOrNull { it.first } ?: 0) + 1
                            draftList = (draftList + (nextNum to "")).toMutableList()
                        },
                        variant = TbButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
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
                    onClick = {
                        val normalized = draftList.mapIndexed { i, p -> (i + 1) to p.second }
                        viewModel.upsertChapters(currentBook!!.id, normalized)
                        onNavigateBack()
                    },
                    variant = TbButtonVariant.Terra,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
