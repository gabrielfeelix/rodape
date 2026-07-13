package com.example.data.search

import com.example.data.api.GoogleBooksApi
import com.example.data.api.OpenLibraryApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Busca unificada de livros — primeiro Open Library (preferida por ter mais
 * metadados consistentes + cover_i confiável), depois Google Books como
 * complemento quando OL retornou poucos resultados.
 *
 * Estratégia (decidida no spec 7a):
 *  - Sempre OL primeiro com language=por
 *  - Se OL retornou ≥ 5: usa só OL (evita ruído)
 *  - Se OL retornou < 5: busca GB em paralelo, faz merge
 *  - Dedup: só por ISBN-13 quando AMBOS têm. Sem ISBN entram sem dedup.
 *  - Ordem final: OL primeiro, depois GB que não está em OL
 */
object BookSearchService {

    private const val MIN_OL_RESULTS_THRESHOLD = 5

    suspend fun searchBooks(
        query: String,
        subject: String? = null,
        languageHint: String = "por"  // OL usa código de 3 letras; GB usa 2 ("pt")
    ): List<UnifiedBookResult> {
        if (query.trim().isEmpty()) return emptyList()
        val q = query.trim()

        return coroutineScope {
            // Sempre dispara OL primeiro
            val olDeferred = async {
                runCatching {
                    OpenLibraryApi.service.searchBooks(
                        query = q,
                        language = languageHint,
                        subject = subject
                    )
                }.getOrNull()
            }
            val olResp = olDeferred.await()
            val olResults: List<UnifiedBookResult> = olResp?.docs.orEmpty()
                // H1: NÃO derrubar livro sem capa — o app já mostra capa-placeholder.
                // Antes o filtro escondia da busca livros válidos só por não ter capa.
                .filter { !it.authorName.isNullOrEmpty() }
                .map { doc ->
                    UnifiedBookResult(
                        title = doc.title,
                        author = doc.authorName?.firstOrNull().orEmpty(),
                        coverUrl = doc.coverI?.let {
                            "https://covers.openlibrary.org/b/id/$it-M.jpg"
                        },
                        firstPublishYear = doc.firstPublishYear,
                        isbn = doc.isbn?.firstOrNull(),
                        source = UnifiedBookResult.Source.OPEN_LIBRARY,
                        openlibraryRawCoverI = doc.coverI
                    )
                }

            if (olResults.size >= MIN_OL_RESULTS_THRESHOLD) {
                return@coroutineScope olResults
            }

            // OL retornou pouca coisa — busca GB pra complementar
            val gbResp = runCatching {
                GoogleBooksApi.service.search(
                    query = q,
                    langRestrict = "pt",
                    printType = "books"
                )
            }.getOrNull()

            val gbResults: List<UnifiedBookResult> = gbResp?.items.orEmpty()
                .mapNotNull { item ->
                    val info = item.volumeInfo ?: return@mapNotNull null
                    val title = info.title ?: return@mapNotNull null
                    val author = info.authors?.firstOrNull() ?: return@mapNotNull null
                    val isbn = info.industryIdentifiers
                        ?.firstOrNull { it.type == "ISBN_13" }
                        ?.identifier
                        ?: info.industryIdentifiers
                            ?.firstOrNull { it.type == "ISBN_10" }
                            ?.identifier
                    val year = info.publishedDate
                        ?.take(4)
                        ?.toIntOrNull()
                    UnifiedBookResult(
                        title = title,
                        author = author,
                        coverUrl = info.imageLinks?.thumbnail
                            ?.replace("http://", "https://"),
                        firstPublishYear = year,
                        isbn = isbn,
                        source = UnifiedBookResult.Source.GOOGLE_BOOKS
                    )
                }

            // Merge com dedup por ISBN-13 quando ambos têm
            val olKeys = olResults.mapNotNull { it.dedupKey }.toSet()
            val gbFiltered = gbResults.filterNot { it.dedupKey != null && it.dedupKey in olKeys }

            olResults + gbFiltered
        }
    }
}
