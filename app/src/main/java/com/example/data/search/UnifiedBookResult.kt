package com.example.data.search

/**
 * Resultado unificado da busca de livros, vindo de Open Library OU Google Books.
 *
 * Mantém os campos comuns que a UI da Suggest precisa, mais [source] pra dedup
 * e debug. Quando criar uma sugestão a partir desse resultado, o ViewModel sabe
 * de onde veio (via [source]) caso queira aplicar cross-check de autor.
 */
data class UnifiedBookResult(
    val title: String,
    val author: String,            // primeiro autor (string única)
    val coverUrl: String?,         // URL https; null se não houver capa
    val firstPublishYear: Int?,
    val isbn: String?,             // ISBN-13 normalizado quando disponível
    val source: Source,
    val openlibraryRawCoverI: Long? = null  // pro caso de querer reconstruir URL OL
) {
    enum class Source { OPEN_LIBRARY, GOOGLE_BOOKS }

    /** Chave de dedup: ISBN-13 normalizado (sem hífens/espaços). */
    val dedupKey: String?
        get() = isbn?.filter { it.isDigit() }?.takeIf { it.length == 13 }
}
