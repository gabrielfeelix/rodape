package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Gutendex — API JSON pública/grátis/sem-chave sobre o Project Gutenberg
 * (gutendex.com). Usada pra achar o EPUB de clássicos de domínio público e
 * extrair o índice (nav/NCX). Cobre boa parte dos clássicos (inclui alguns em
 * português), mas não ficção contemporânea.
 */
@JsonClass(generateAdapter = true)
data class GutendexBook(
    @Json(name = "title") val title: String = "",
    @Json(name = "languages") val languages: List<String>? = null,
    // Mapa media-type -> URL. Ex.: "application/epub+zip" -> "https://.../pg123.epub"
    @Json(name = "formats") val formats: Map<String, String>? = null,
)

@JsonClass(generateAdapter = true)
data class GutendexResponse(
    @Json(name = "results") val results: List<GutendexBook>? = null,
)

interface GutendexService {
    @GET("books")
    suspend fun search(@Query("search") query: String): GutendexResponse
}

object GutendexApi {
    // Gutendex/Gutenberg bloqueiam User-Agent "padrão" (ex.: okhttp/urllib) com 403.
    // Interceptor garante um UA de navegador.
    private val client = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Android; Rodape reading club)")
                    .build()
            )
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gutendex.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: GutendexService = retrofit.create(GutendexService::class.java)
}
