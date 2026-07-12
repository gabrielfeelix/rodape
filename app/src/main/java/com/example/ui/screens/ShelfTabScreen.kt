package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.ui.components.CenteredLoading
import com.example.ui.components.Cover
import com.example.ui.components.Pill
import com.example.ui.components.PillVariant
import com.example.ui.components.RatingStars
import com.example.ui.components.RodapeCard
import com.example.ui.components.rememberShowLoading
import com.example.ui.theme.Ink
import com.example.ui.theme.Muted
import com.example.ui.viewmodel.MainViewModel
import com.example.util.formatShortDate

@Composable
fun ShelfTabScreen(
    viewModel: MainViewModel,
    onNavigateToBookDetail: (String) -> Unit
) {
    val finishedBooks by viewModel.finishedBooks.collectAsState()
    val meetingDates by viewModel.finishedBooksMeetingDates.collectAsState()
    var filterBy by remember { mutableStateOf("Todos") }

    // Cache per-book ratings averages reactively
    val ratingsByBook = finishedBooks.associate { book ->
        val ratingsFlow = remember(book.id) { viewModel.getBookRatingsFlow(book.id) }
        val ratings by ratingsFlow.collectAsState(initial = emptyList())
        val avg = if (ratings.isNotEmpty()) ratings.sumOf { it.stars }.toFloat() / ratings.size else 0f
        book.id to avg
    }

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
            CenteredLoading()
        } else if (displayedBooks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nenhum livro lido ainda pelo clube. Quando vocês terminarem um livro, ele aparece aqui.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
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
