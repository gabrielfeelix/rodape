package com.example.data.remote.repo

import com.example.data.model.SavedQuote
import kotlinx.coroutines.flow.Flow

/** Frases salvas (tabela `saved_quotes`). Fatiado da god-repo (F3c). */
interface QuoteRepository {
    suspend fun insertSavedQuote(quote: SavedQuote)
    suspend fun deleteSavedQuote(quote: SavedQuote)
    fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>>
    fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>>
}
