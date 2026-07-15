package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.OpenLibraryDoc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// F5: busca de livros (Open Library + Google Books). Estado por-tela — cada
// tela que busca tem a própria instância (Suggest e ManageClub não compartilham
// resultados; antes compartilhavam via MainViewModel sem necessidade).
// Corpos movidos verbatim do MainViewModel.
@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    // Backcompat: SuggestScreen ainda usa pro cross-check / criar suggestion via
    // createBookSuggestion(doc). Mantém List<OpenLibraryDoc> pra preservar a
    // chamada existente. searchResultsUnified é a versão completa com Google Books.
    private val _searchResults = MutableStateFlow<List<OpenLibraryDoc>>(emptyList())
    val searchResults: StateFlow<List<OpenLibraryDoc>> = _searchResults.asStateFlow()

    private val _searchResultsUnified = MutableStateFlow<List<com.example.data.search.UnifiedBookResult>>(emptyList())
    val searchResultsUnified: StateFlow<List<com.example.data.search.UnifiedBookResult>> = _searchResultsUnified.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    /**
     * Busca unificada (OL + GB fallback). Alimenta _searchResultsUnified pra Suggest.
     * Também alimenta _searchResults (OpenLibraryDoc) por backcompat com cross-check
     * de autor — só os resultados que vieram de OL têm representação como Doc.
     */
    fun searchOpenLibrary(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            _searchResults.value = emptyList()
            _searchResultsUnified.value = emptyList()
            return
        }

        _searchLoading.value = true
        viewModelScope.launch {
            try {
                // Busca unificada
                val unified = com.example.data.search.BookSearchService.searchBooks(q)
                _searchResultsUnified.value = unified

                // Converte TODAS as fontes (Open Library + Google Books) pra
                // OpenLibraryDoc — antes filtrava só OL, e livros que existiam
                // apenas no Google Books nunca apareciam na tela de sugerir.
                // (GB não tem coverI de OL; a capa cai pra capa gerada.)
                val docs = unified.map { u ->
                    OpenLibraryDoc(
                        title = u.title,
                        authorName = listOf(u.author),
                        firstPublishYear = u.firstPublishYear,
                        coverI = u.openlibraryRawCoverI,
                        isbn = u.isbn?.let { listOf(it) }
                    )
                }
                _searchResults.value = docs
            } catch (e: Exception) {
                android.util.Log.e("Rodape/VM", "Operacao falhou silenciosamente", e)
                _searchResults.value = emptyList()
                _searchResultsUnified.value = emptyList()
            } finally {
                _searchLoading.value = false
            }
        }
    }

    /**
     * Faz cross-check do autor com Google Books pra detectar conflitos da Open Library
     * (que às vezes retorna author errado quando cover_id é compartilhado).
     *
     * Retorna o autor encontrado no GB se diferente do fornecido, ou null se bate/falha.
     */
    fun verifyAuthorWithGoogleBooks(
        title: String,
        olAuthor: String,
        isbn: String,
        onResult: (gbAuthor: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val query = if (isbn.isNotBlank()) "isbn:$isbn"
                    else "intitle:${title.take(80)}"
                val gb = com.example.data.api.GoogleBooksApi.service.search(query)
                val gbAuthor = gb.items
                    ?.firstOrNull()
                    ?.volumeInfo
                    ?.authors
                    ?.firstOrNull()
                    .orEmpty()
                if (gbAuthor.isBlank()) {
                    onResult(null)
                    return@launch
                }
                // Normalize pra comparar (ignora case, trim)
                val olNorm = olAuthor.trim().lowercase()
                val gbNorm = gbAuthor.trim().lowercase()
                if (olNorm == gbNorm || olNorm.contains(gbNorm) || gbNorm.contains(olNorm)) {
                    onResult(null) // bateu
                } else {
                    onResult(gbAuthor.trim())
                }
            } catch (e: Exception) {
                android.util.Log.e("Rodape/VM", "Operacao falhou silenciosamente", e)
                onResult(null)
            }
        }
    }
}
