package com.example.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.example.ui.components.TbButton
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.ui.components.SegmentedControl
import com.example.ui.components.SkeletonBox
import com.example.ui.components.SkeletonText
import com.example.ui.components.rememberShowLoading
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.shelfCoverShadow
import com.example.ui.theme.RodapeIcons
import com.example.ui.theme.RodapeRadii
import com.example.ui.theme.Ink
import com.example.ui.theme.Muted
import com.example.ui.theme.RodapeTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.formatShortDate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Composable
fun ShelfTabScreen(
    viewModel: MainViewModel,
    onNavigateToBookDetail: (String) -> Unit,
    onNavigateToSuggest: () -> Unit = {}
) {
    val finishedBooks by viewModel.finishedBooks.collectAsStateWithLifecycle()
    val meetingDates by viewModel.finishedBooksMeetingDates.collectAsStateWithLifecycle()
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
    val ratingsByBook by ratingsFlow.collectAsStateWithLifecycle(initialValue = emptyMap())

    // "Favoritos" = favoritos PESSOAIS de verdade (♥), não mais a média do clube
    // ≥ 4.5 (que enganava — o usuário não escolhia nada). Mostra os livros que ESTE
    // usuário favoritou e que o clube já leu (interseção).
    val favoriteBooks by viewModel.favoriteBooks.collectAsStateWithLifecycle()
    val favoriteIds = remember(favoriteBooks) { favoriteBooks.map { it.id }.toSet() }
    val displayedBooks = if (filterBy == "Favoritos") {
        finishedBooks.filter { it.id in favoriteIds }
    } else finishedBooks

    // Distingue LOADING de EMPTY: no cold start local-first `finishedBooks` chega
    // vazio antes do primeiro sync, mostrando o empty state por engano.
    val showLoading = rememberShowLoading(hasData = finishedBooks.isNotEmpty())

    // Contagem de favoritos LIDOS (interseção) pro rótulo do filtro.
    val favoritesCount = remember(finishedBooks, favoriteIds) {
        finishedBooks.count { it.id in favoriteIds }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 3.9: filtro vira SegmentedControl de verdade (track oliva, target 48dp,
        // semântica de seleção) com ♥ + contagem — era Pill em Box clickable.
        SegmentedControl(
            options = listOf("Todos", "Favoritos"),
            selected = filterBy,
            onSelect = { filterBy = it },
            label = { if (it == "Favoritos") "♥ Favoritos ($favoritesCount)" else it },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )

        if (showLoading) {
            SkeletonCoverGrid()
        } else if (displayedBooks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (filterBy == "Favoritos") {
                    // Empty do filtro "Favoritos" ≠ empty geral: há livros lidos,
                    // só faltam favoritos. Copy honesta — o ♥ existe de verdade agora.
                    Text(
                        text = "Você ainda não favoritou nenhum livro. Toque no ♥ na página de um livro pra guardá-lo aqui.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = RodapeTheme.colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        // Prateleira vazia ilustrada (3.9): lombadas esmaecidas +
                        // UMA terracota ("o primeiro livro de vocês") sobre a linha
                        // da prateleira. Geometria pura — sem vetor arriscado.
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(Modifier.size(width = 14.dp, height = 44.dp).clip(RoundedCornerShape(RodapeRadii.xs)).background(RodapeTheme.colors.dividerSoft))
                            Box(Modifier.size(width = 16.dp, height = 58.dp).clip(RoundedCornerShape(RodapeRadii.xs)).background(RodapeTheme.colors.terracota))
                            Box(Modifier.size(width = 13.dp, height = 50.dp).clip(RoundedCornerShape(RodapeRadii.xs)).background(RodapeTheme.colors.divider))
                            Box(Modifier.size(width = 15.dp, height = 40.dp).clip(RoundedCornerShape(RodapeRadii.xs)).background(RodapeTheme.colors.dividerSoft))
                        }
                        Box(
                            Modifier
                                .width(120.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(RodapeRadii.full))
                                .background(RodapeTheme.colors.divider)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhum livro lido ainda pelo clube. Quando vocês terminarem um livro, ele aparece aqui.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = RodapeTheme.colors.muted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        TbButton(
                            text = "Sugerir uma leitura",
                            onClick = onNavigateToSuggest
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                // Adaptativo: 2 colunas no celular, 5-6 no tablet automaticamente
                // (era Fixed(2) → 2 capas gigantes esticadas na largura do tablet).
                // minSize alinhado à largura de capa no phone pra não regredir lá.
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                // Gutter 12dp (3.9): capas maiores e mais juntas = estante, não catálogo.
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(displayedBooks, key = { _, b -> b.id }) { i, book ->
                    // "Prateleira enchendo": cada capa pousa em stagger (fade+rise)
                    // conforme entra na composição.
                    Box(modifier = Modifier.staggeredEntrance(index = i)) {
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
}

/** Grid de skeletons de capa enquanto a estante carrega (mesmo layout 2 colunas). */
@Composable
private fun SkeletonCoverGrid() {
    // UM relógio de shimmer pro grid inteiro (6 capas + 12 linhas), varredura síncrona.
    val shimmer = com.example.ui.components.rememberShimmerProgress()
    val p = remember(shimmer) { { shimmer.value } }
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
                        // Capa skeleton no MESMO raio da capa real (xs=3) — era 6 (drift).
                        SkeletonBox(modifier = Modifier.width(100.dp).height(150.dp), cornerRadius = RodapeRadii.xs, progress = p)
                        SkeletonText(width = 90.dp, height = 12.dp, progress = p)
                        SkeletonText(width = 60.dp, height = 10.dp, progress = p)
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
    // 3.9 cover-first: SEM card branco — a CAPA é o objeto (maior, com sombra
    // tingida de prateleira); título/autor/estrelas menores e mudos por baixo.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RodapeRadii.sm))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        Box(modifier = Modifier.shelfCoverShadow(cornerRadius = RodapeRadii.xs)) {
            Cover(
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                width = 128.dp,
                height = 192.dp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = book.title,
            // C3: 2 linhas — em fonte grande (A++), o título cortava no ellipsis.
            style = MaterialTheme.typography.titleSmall.copy(color = RodapeTheme.colors.ink),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall.copy(color = RodapeTheme.colors.muted),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        if (dataEncontroLabel != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Encontro em $dataEncontroLabel",
                style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted, fontSize = 11.sp),
                textAlign = TextAlign.Center
            )
        }
        // Sem rating → nada (5 estrelas vazias liam como "0 = odiei").
        if (rating > 0f) {
            Spacer(modifier = Modifier.height(6.dp))
            RatingStars(rating = rating, size = 13.dp)
        }
    }
}
