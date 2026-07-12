package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpenLibraryDoc(
    // Default "" evita que UM doc sem "title" faça o Moshi lançar
    // JsonDataException e zerar a página inteira de resultados. Docs com
    // título em branco são filtrados na SuggestScreen.
    @Json(name = "title") val title: String = "",
    @Json(name = "author_name") val authorName: List<String>?,
    @Json(name = "first_publish_year") val firstPublishYear: Int?,
    @Json(name = "cover_i") val coverI: Long?,
    @Json(name = "isbn") val isbn: List<String>?
)

@JsonClass(generateAdapter = true)
data class OpenLibrarySearchResponse(
    @Json(name = "docs") val docs: List<OpenLibraryDoc>?
)

// Índice (table_of_contents) de uma EDIÇÃO. É a ÚNICA fonte pública/grátis/sem-chave
// com capítulos, mas cobertura é enviesada pra técnico/inglês — ficção e livros em
// português quase sempre vêm vazios. Best-effort: se vier, ótimo; se não, o admin
// gera N capítulos manualmente.
@JsonClass(generateAdapter = true)
data class OpenLibraryTocEntry(
    @Json(name = "label") val label: String? = null,
    @Json(name = "title") val title: String? = null,
)

@JsonClass(generateAdapter = true)
data class OpenLibraryEdition(
    @Json(name = "table_of_contents") val tableOfContents: List<OpenLibraryTocEntry>? = null,
)

interface OpenLibraryService {
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("language") language: String? = null,   // ex: "por" pra português
        @Query("subject") subject: String? = null,     // ex: "fiction", "brazilian literature"
        @Query("limit") limit: Int = 20
    ): OpenLibrarySearchResponse

    // /isbn/<isbn>.json redireciona (302) pra /books/<OLID>.json — OkHttp segue sozinho.
    @GET("isbn/{isbn}.json")
    suspend fun editionByIsbn(@Path("isbn") isbn: String): OpenLibraryEdition
}

object OpenLibraryApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://openlibrary.org/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: OpenLibraryService = retrofit.create(OpenLibraryService::class.java)
}
