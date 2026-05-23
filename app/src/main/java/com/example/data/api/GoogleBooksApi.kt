package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GoogleBooksVolume(
    @Json(name = "volumeInfo") val volumeInfo: GoogleBooksVolumeInfo?
)

@JsonClass(generateAdapter = true)
data class GoogleBooksVolumeInfo(
    @Json(name = "title") val title: String?,
    @Json(name = "authors") val authors: List<String>?,
    @Json(name = "description") val description: String?
)

@JsonClass(generateAdapter = true)
data class GoogleBooksSearchResponse(
    @Json(name = "items") val items: List<GoogleBooksVolume>?
)

interface GoogleBooksService {
    @GET("books/v1/volumes")
    suspend fun search(@Query("q") query: String): GoogleBooksSearchResponse
}

object GoogleBooksApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: GoogleBooksService = retrofit.create(GoogleBooksService::class.java)
}
