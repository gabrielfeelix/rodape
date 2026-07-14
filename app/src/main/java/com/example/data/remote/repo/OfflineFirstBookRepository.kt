package com.example.data.remote.repo

import com.example.BuildConfig
import com.example.data.db.RodapeDao
import com.example.data.model.*
import com.example.data.remote.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstBookRepository @Inject constructor(
    private val engine: SyncEngine,
    private val supabase: SupabaseClient,
    private val dao: RodapeDao,
) : BookRepository {

    override suspend fun insertBook(book: Book) {
        // Optimistic local-first: escreve no Room ANTES de tentar Supabase.
        dao.upsertBook(book)
        val dto = book.toInsertDto()
        engine.tryRemoteOrEnqueue("insert_book", engine.json.encodeToString(dto), notifyTable = "books") {
            supabase.from("books").upsert(dto)
        }
    }

    override suspend fun uploadBookCover(clubId: String, bookId: String, bytes: ByteArray): String? = runCatching {
        val path = "$clubId/$bookId.jpg"
        val bucket = supabase.storage.from("book-covers")
        bucket.upload(path, bytes) {
            upsert = true
            contentType = io.ktor.http.ContentType.Image.JPEG
        }
        val signedUrl = bucket.createSignedUrl(path, kotlin.time.Duration.parse("365d"))
        "${BuildConfig.SUPABASE_URL}$signedUrl"
    }.getOrNull()

    override suspend fun insertClubBook(clubBook: ClubBook) {
        // Optimistic local-first + fila (P0-2): offline não perde mais a criação.
        dao.upsertClubBook(clubBook)
        val dto = clubBook.toDto()
        engine.tryRemoteOrEnqueue("insert_club_book", engine.json.encodeToString(dto), notifyTable = "club_books") {
            supabase.from("club_books").upsert(dto)
        }
    }

    override fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>> {
        val key = "clubBooks:$clubId"
        val reload: suspend () -> Unit = { syncClubBooks(clubId); engine.markSynced(key) }
        engine.scope.launch { engine.syncOnce(key, Ttl.MED) { syncClubBooks(clubId) } }
        engine.ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.booksByStatusFlow(clubId, status)
    }

    private suspend fun syncClubBooks(clubId: String) {
        runCatching {
            val rows = supabase.from("club_books").select(Columns.raw("book_id, status, ordem, data_encontro, books!inner(*)")) {
                filter { eq("club_id", clubId) }
            }.decodeList<JoinClubBookFull>()
            val books = rows.map { it.book.toDomain() }
            val clubBooks = rows.map { row ->
                ClubBook(
                    clubId = clubId,
                    bookId = row.bookId,
                    status = row.status,
                    ordem = row.ordem,
                    dataEncontro = row.dataEncontro?.fromIso(),
                )
            }
            dao.replaceClubBooksInClub(clubId, clubBooks, books)
        }
    }

    override fun getClubBooksFlow(clubId: String): Flow<List<Book>> {
        val key = "clubBooks:$clubId"
        val reload: suspend () -> Unit = { syncClubBooks(clubId); engine.markSynced(key) }
        engine.scope.launch { engine.syncOnce(key, Ttl.MED) { syncClubBooks(clubId) } }
        engine.ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.clubBooksFlow(clubId)
    }

    override suspend fun getClubBookStatus(clubId: String, bookId: String): String? = runCatching {
        supabase.from("club_books").select(Columns.raw("status")) {
            filter {
                eq("club_id", clubId)
                eq("book_id", bookId)
            }
            limit(1)
        }.decodeSingleOrNull<StatusOnlyDto>()?.status
    }.getOrNull()

    override suspend fun getBook(id: String): Book? {
        dao.book(id)?.let { return it }
        return runCatching {
            supabase.from("books").select {
                filter { eq("id", id) }
                limit(1)
            }.decodeSingleOrNull<BookDto>()?.toDomain()
                ?.also { dao.upsertBook(it) }
        }.getOrNull()
    }

    override suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String) {
        runCatching {
            supabase.from("club_books").update({ set("status", status) }) {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
            engine.notifyLocalMutation("club_books")
        }
    }

    override suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?) {
        runCatching {
            supabase.from("club_books").update({
                set("data_encontro", dataEncontro?.toIso())
            }) {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
            engine.notifyLocalMutation("club_books")
        }
    }

    override fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> {
        val reload: suspend () -> Unit = { syncClubBooks(clubId) }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.clubBooksByStatusFlow(clubId, status)
    }

    override suspend fun deleteClubBook(clubId: String, bookId: String) {
        runCatching {
            supabase.from("club_books").delete {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
            dao.deleteClubBook(clubId, bookId)
            engine.notifyLocalMutation("club_books")
        }
    }

    override suspend fun insertChapters(chapters: List<Chapter>) {
        if (chapters.isEmpty()) return
        runCatching {
            supabase.from("chapters").upsert(chapters.map { it.toDto() })
            dao.upsertChapters(chapters)
        }
    }

    override fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>> {
        engine.scope.launch {
            runCatching {
                val list = supabase.from("chapters").select {
                    filter { eq("book_id", bookId) }
                    order("numero", Order.ASCENDING)
                }.decodeList<ChapterDto>().map { it.toDomain() }
                dao.upsertChapters(list)
            }
        }
        return dao.chaptersForBookFlow(bookId)
    }

    override suspend fun deleteChaptersForBook(bookId: String) {
        runCatching {
            supabase.from("chapters").delete {
                filter { eq("book_id", bookId) }
            }
            dao.deleteChaptersForBook(bookId)
        }
    }

    override suspend fun saveChapters(bookId: String, chapters: List<Chapter>) {
        val keepIds = chapters.map { it.id }
        // Local-first: upsert (in-place) + deleta só os removidos, por id.
        dao.upsertChapters(chapters)
        if (keepIds.isNotEmpty()) dao.deleteChaptersNotInIds(bookId, keepIds) else dao.deleteChaptersForBook(bookId)
        engine.notifyLocalMutation("chapters")
        // Remoto: upsert todos (id uuid), depois deleta só os capítulos cujo id saiu.
        runCatching {
            if (chapters.isNotEmpty()) supabase.from("chapters").upsert(chapters.map { it.toDto() })
            val existing = supabase.from("chapters").select(Columns.raw("id")) {
                filter { eq("book_id", bookId) }
            }.decodeList<IdOnlyDto>().map { it.id }
            val removed = existing.filter { it !in keepIds }
            if (removed.isNotEmpty()) {
                supabase.from("chapters").delete {
                    filter { eq("book_id", bookId); isIn("id", removed) }
                }
            }
        }.onFailure { android.util.Log.w("Rodape/Repo", "saveChapters remoto falhou: ${it.message}") }
    }

    override suspend fun getChapterTemplate(isbn: String): List<Pair<Int, String>>? = runCatching {
        val row = supabase.from("chapter_templates").select {
            filter { eq("isbn", isbn) }
            limit(1)
        }.decodeSingleOrNull<ChapterTemplateDto>()
        row?.chapters?.map { it.numero to it.titulo }?.sortedBy { it.first }
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    override suspend fun shareChapterTemplate(isbn: String, tituloLivro: String, chapters: List<Pair<Int, String>>, userId: String) {
        runCatching {
            supabase.from("chapter_templates").upsert(
                ChapterTemplateDto(
                    isbn = isbn,
                    tituloLivro = tituloLivro,
                    chapters = chapters.map { ChapterTemplateEntryDto(it.first, it.second) },
                    contributedBy = userId,
                )
            ) { onConflict = "isbn" }
        }.onFailure { android.util.Log.w("Rodape/Repo", "shareChapterTemplate falhou: ${it.message}") }
    }

    override suspend fun insertBookSummary(summary: BookSummary) {
        runCatching {
            supabase.from("book_summaries").upsert(
                BookSummaryInsertDto(
                    bookId = summary.bookId,
                    clubId = summary.clubId,
                    texto = summary.texto,
                    lastEditorId = summary.lastEditorId.ifBlank { null },
                )
            )
            dao.upsertBookSummary(summary)
            engine.notifyLocalMutation("book_summaries")
        }
    }

    override fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?> {
        val reload: suspend () -> Unit = {
            runCatching {
                val s = supabase.from("book_summaries").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookSummaryDto>()?.toDomain()
                if (s != null) dao.upsertBookSummary(s)
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("book_summaries", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookSummaryFlow(bookId, clubId)
    }

    override suspend fun insertBookRating(rating: BookRating) {
        // Local-first + fila: grava no Room antes do remoto e enfileira se offline.
        dao.upsertBookRatings(listOf(rating))
        val payload = buildJsonObject {
            put("bookId", rating.bookId)
            put("clubId", rating.clubId)
            put("userId", rating.userId)
            put("stars", rating.stars.toString())
            put("comment", rating.comment)
        }.toString()
        engine.tryRemoteOrEnqueue("upsert_book_rating", payload, notifyTable = "book_ratings") {
            supabase.from("book_ratings").upsert(
                BookRatingInsertDto(
                    bookId = rating.bookId,
                    clubId = rating.clubId,
                    userId = rating.userId,
                    stars = rating.stars,
                    comment = rating.comment,
                )
            )
        }
    }

    override fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("book_ratings").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                }.decodeList<BookRatingDto>().map { it.toDomain() }
                dao.upsertBookRatings(list)
                dao.pruneBookRatingsExcept(bookId, clubId, list.map { it.userId })
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("book_ratings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookRatingsFlow(bookId, clubId)
    }

    override fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?> {
        engine.scope.launch {
            runCatching {
                val r = supabase.from("book_ratings").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookRatingDto>()?.toDomain()
                if (r != null) dao.upsertBookRatings(listOf(r))
            }
        }
        return dao.bookRatingOfUserFlow(bookId, clubId, userId)
    }

    override suspend fun setBookFavorite(userId: String, bookId: String, favorite: Boolean) {
        val payload = buildJsonObject {
            put("userId", userId)
            put("bookId", bookId)
        }.toString()
        if (favorite) {
            dao.upsertBookFavorites(listOf(BookFavorite(userId, bookId, System.currentTimeMillis())))
            engine.tryRemoteOrEnqueue("insert_book_favorite", payload, notifyTable = "book_favorites") {
                supabase.from("book_favorites").upsert(BookFavoriteInsertDto(userId = userId, bookId = bookId))
            }
        } else {
            dao.deleteBookFavorite(userId, bookId)
            engine.tryRemoteOrEnqueue("delete_book_favorite", payload, notifyTable = "book_favorites") {
                supabase.from("book_favorites").delete {
                    filter {
                        eq("user_id", userId)
                        eq("book_id", bookId)
                    }
                }
            }
        }
    }

    override fun isBookFavoriteFlow(userId: String, bookId: String): Flow<Boolean> =
        dao.isBookFavoriteFlow(userId, bookId)

    override suspend fun anyClubIdForBook(bookId: String): String? = dao.anyClubIdForBook(bookId)

    override fun getFavoriteBooksForUserFlow(userId: String): Flow<List<Book>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("book_favorites").select {
                    filter { eq("user_id", userId) }
                }.decodeList<BookFavoriteDto>()
                dao.upsertBookFavorites(list.map { BookFavorite(it.userId, it.bookId, it.createdAt.fromIso()) })
                dao.pruneBookFavoritesExcept(userId, list.map { it.bookId })
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("book_favorites", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.favoriteBooksFlow(userId)
    }
}
