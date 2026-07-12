package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.ui.components.Cover
import com.example.ui.components.Pill
import com.example.ui.components.PillVariant
import com.example.ui.components.RatingStars
import com.example.ui.components.RodapeCard
import com.example.ui.components.SkeletonBox
import com.example.ui.components.SkeletonText
import com.example.ui.components.rememberShowLoading
import com.example.ui.theme.Ink
import com.example.ui.theme.Muted
import com.example.ui.viewmodel.MainViewModel
import com.example.util.formatShortDate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Composable
fun ShelfTabScreen(
    viewModel: MainViewModel,
    onNavigateToBookDetail: (String) -> Unit
) {
    val finishedBooks by viewModel.finishedBooks.collectAsState()
    val meetingDates by viewModel.finishedBooksMeetingDates.collectAsState()
    var filterBy by remember { mutableStateOf("Todos") }

    // Média por-livro reativa, sem N+1 posicional.
    // Antes: `finishedBooks.associate { remember(...) + collectAsState(...) }`
    // criava um coletor por posição da lista, recriado a cada recomposição e
    // memoizado por índice (frágil se a lista reordena/muda de tamanho).
    // Agora: UM único flow combinado, memoizado pela identidade de
    // `finishedBooks`, com um único collectAsState. Precisa continuar no nível
    // da lista porque o filtro "Favoritos" abaixo depende das médias antes do
    // `items(...)` (mover 100% pra dentro do card quebraria esse filtro).
    val ratingsFlow = remember(finishedBooks) {
        if (finishedBooks.isEmpty()) {
            flowOf(emptyMap<String, Float>())
        } else {
            combine(
                finishedBooks.map { book ->
                    viewModel.getBookRatingsFlow(book.id).map { ratings ->
                        val avg = if (ratings.isNotEmpty()) {
                            ratings.sumOf { it.stars }.toFloat() / ratings.size
                        } else {
                            0f
                        }
                        book.id to avg
                    }
                }
            ) { pairs -> pairs.toMap() }
        }
    }
    val ratingsByBook by ratingsFlow.collectAsState(initial = emptyMap())

    val displayedBooks = if (filterBy == "Favoritos") {
        finishedBooks.filter { (ratingsByBook[it.id] ?: 0f) >= 4.5f }
    } else finishedBooks

    // Distingue LOADING de EMPTY: no cold start local-first `finishedBooks` chega
    // vazio antes do primeiro sync, mostrando o empty state por engano.
    val showLoading = rememberShowLoading(hasData = finishedBooks.isNotEmpty())

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Todos", "Favoritos").forEach { option ->
                val isSelected = filterBy == option
                Box(modifier = Modifier.clickable { filterBy = option }) {
                    Pill(
                        text = option,
                        variant = if (isSelected) PillVariant.OliveDeep else PillVariant.Default
                    )
                }
            }
        }

        if (showLoading) {
            SkeletonCoverGrid()
        } else if (displayedBooks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (filterBy == "Favoritos") {
                    // Empty do filtro "Favoritos" ≠ empty geral: há livros lidos,
                    // só faltam favoritos. Copy honesta (bug U2).
                    Text(
                        text = "Nenhum favorito ainda. Toque na estrela de um livro pra marcá-lo como favorito.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Book,
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhum livro lido ainda pelo clube. Quando vocês terminarem um livro, ele aparece aqui.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Muted,
                            textAlign = TextAlign.Center
                        )
                        // CTA "Sugerir uma leitura" omitido: não há callback de
                        // navegação pra sugerir livro no escopo desta tela (só
                        // `viewModel` e `onNavigateToBookDetail`).
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(displayedBooks, key = { it.id }) { book ->
                    ShelfBookCard(
                        book = book,
                        rating = ratingsByBook[book.id] ?: 0f,
                        dataEncontroLabel = meetingDates[book.id]?.let { formatShortDate(it) },
                        onClick = { onNavigateToBookDetail(book.id) }
                    )
                }
            }
        }
    }
}

/** Grid de skeletons de capa enquanto a estante carrega (mesmo layout 2 colunas). */
@Composable
private fun SkeletonCoverGrid() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(2) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonBox(modifier = Modifier.width(100.dp).height(150.dp), cornerRadius = 6.dp)
                        SkeletonText(width = 90.dp, height = 12.dp)
                        SkeletonText(width = 60.dp, height = 10.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfBookCard(
    book: Book,
    rating: Float,
    dataEncontroLabel: String?,
    onClick: () -> Unit
) {
    RodapeCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Cover(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                width = 100.dp,
                height = 150.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 14.sp, color = Ink),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyLarge.copy(color = Muted, fontSize = 12.sp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (dataEncontroLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Encontro em $dataEncontroLabel",
                    style = MaterialTheme.typography.labelSmall.copy(color = Muted, fontSize = 11.sp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            RatingStars(rating = rating, size = 16.dp)
        }
    }
}
