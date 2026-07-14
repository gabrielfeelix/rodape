package com.example.data.remote.repo

import com.example.data.db.RodapeDao
import com.example.data.model.SavedQuote
import com.example.data.remote.SavedQuoteDto
import com.example.data.remote.SavedQuoteInsertDto
import com.example.data.remote.SyncEngine
import com.example.data.remote.escapeJson
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstQuoteRepository @Inject constructor(
    private val engine: SyncEngine,
    private val supabase: SupabaseClient,
    private val dao: RodapeDao,
) : QuoteRepository {

    override suspend fun insertSavedQuote(quote: SavedQuote) {
        dao.upsertSavedQuotes(listOf(quote))
        val cap = quote.capituloRef.escapeJson()
        val payload = """{"id":"${quote.id}","userId":"${quote.userId}","clubId":"${quote.clubId}","bookId":"${quote.bookId}","texto":"${quote.texto.escapeJson()}","capituloRef":"$cap"}"""
        engine.tryRemoteOrEnqueue("insert_saved_quote", payload, notifyTable = "saved_quotes") {
            supabase.from("saved_quotes").upsert(
                SavedQuoteInsertDto(
                    id = quote.id,
                    userId = quote.userId,
                    clubId = quote.clubId,
                    bookId = quote.bookId,
                    texto = quote.texto,
                    capituloRef = quote.capituloRef.ifBlank { null },
                )
            )
        }
    }

    override suspend fun deleteSavedQuote(quote: SavedQuote) {
        runCatching {
            supabase.from("saved_quotes").delete { filter { eq("id", quote.id) } }
            dao.deleteSavedQuote(quote.id)
            engine.notifyLocalMutation("saved_quotes")
        }
    }

    override fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("saved_quotes").select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<SavedQuoteDto>().map { it.toDomain() }
                dao.replaceSavedQuotesForUser(userId, list)
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("saved_quotes", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.savedQuotesForUserFlow(userId)
    }

    override fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>> {
        engine.scope.launch {
            runCatching {
                val list = supabase.from("saved_quotes").select {
                    filter {
                        eq("user_id", userId)
                        eq("book_id", bookId)
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<SavedQuoteDto>().map { it.toDomain() }
                dao.upsertSavedQuotes(list)
            }
        }
        return dao.savedQuotesForBookFlow(userId, bookId)
    }
}
