package com.example.data.remote.repo

import com.example.BuildConfig
import com.example.data.model.Book
import com.example.data.model.BookFavorite
import com.example.data.model.BookRating
import com.example.data.model.BookSummary
import com.example.data.model.Chapter
import com.example.data.model.ClubBook
import com.example.data.remote.BookDto
import com.example.data.remote.BookFavoriteDto
import com.example.data.remote.BookFavoriteInsertDto
import com.example.data.remote.BookRatingDto
import com.example.data.remote.BookRatingInsertDto
import com.example.data.remote.BookSummaryDto
import com.example.data.remote.BookSummaryInsertDto
import com.example.data.remote.ChapterDto
import com.example.data.remote.ChapterTemplateDto
import com.example.data.remote.ChapterTemplateEntryDto
import com.example.data.remote.ClubBookDto
import com.example.data.remote.IdOnlyDto
import com.example.data.remote.SyncEngine
import com.example.data.remote.Ttl
import com.example.data.remote.fromIso
import com.example.data.remote.toDto
import com.example.data.remote.toInsertDto
import com.example.data.remote.toIso
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

// F3c: livros — books/club_books + chapters + summaries/ratings + favorites
// (coeso como no DESIGN-ALVO; não pulverizar). Corpos movidos VERBATIM do
// RemoteRepository — comportamento idêntico.
interface BookRepository {
    suspend fun insertBook(book: Book)
    suspend fun uploadBookCover(clubId: String, bookId: String, bytes: ByteArray): String?
    suspend fun insertClubBook(clubBook: ClubBook)
    fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>>
    fun getClubBooksFlow(clubId: String): Flow<List<Book>>
    suspend fun getClubBookStatus(clubId: String, bookId: String): String?
    suspend fun getBook(id: String): Book?
    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String)
    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?)
    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>>
    suspend fun deleteClubBook(clubId: String, bookId: String)
    suspend fun insertChapters(chapters: List<Chapter>)
    fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>>
    suspend fun deleteChaptersForBook(bookId: String)
    suspend fun saveChapters(bookId: String, chapters: List<Chapter>)
    suspend fun getChapterTemplate(isbn: String): List<Pair<Int, String>>?
    suspend fun shareChapterTemplate(isbn: String, tituloLivro: String, chapters: List<Pair<Int, String>>, userId: String)
    suspend fun insertBookSummary(summary: BookSummary)
    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?>
    suspend fun insertBookRating(rating: BookRating)
    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>>
    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?>
    suspend fun setBookFavorite(userId: String, bookId: String, favorite: Boolean)
    fun isBookFavoriteFlow(userId: String, bookId: String): Flow<Boolean>
    suspend fun anyClubIdForBook(bookId: String): String?
    fun getFavoriteBooksForUserFlow(userId: String): Flow<List<Book>>
}

internal class OfflineFirstBookRepository @Inject constructor(
    engine: SyncEngine,
) : OfflineFirstRepository(engine), BookRepository {

    override suspend fun insertBook(book: Book) {
        // Optimistic local-first: escreve no Room ANTES de tentar Supabase pra
        // UI nunca ficar "fantasma" (livro sumiu so porque rede falhou ou rate
        // limit do Supabase respondeu 429). Se o remoto falhar, loga e segue —
        // a proxima sync vai reconciliar.
        // Room já emite pro Flow após o upsert local (UI otimista). O
        // notifyLocalMutation (re-fetch remoto + prune) SÓ roda após o remoto
        // confirmar — senão o re-fetch corre na frente da escrita e PODA a linha
        // otimista ainda-não-sincronizada (item "pisca e some").
        dao.upsertBook(book)
        // Offline-first REAL: se o remoto falhar (offline/429/5xx), ENFILEIRA em vez
        // de só logar. Antes a criação local-only era podada no próximo sync (a linha
        // nunca chegava ao servidor) — perda silenciosa (P0-2).
        val dto = book.toInsertDto()
        tryRemoteOrEnqueue("insert_book", json.encodeToString(dto), notifyTable = "books") {
            supabase.from("books").upsert(dto)
        }
    }

    /**
     * Sobe bytes da capa pro bucket `book-covers` no path `<clubId>/<bookId>.jpg`.
     * Retorna URL pra usar em `books.cover_url`.
     *
     * Bucket e privado — geramos signed URL com expiracao longa (1 ano) e a guardamos
     * no banco. Quando expirar, regeneramos. Pra clubes ativos isso significa que
     * a URL sempre esta valida na pratica.
     *
     * Path por clube garante isolamento via RLS: so members do clube podem
     * ler/escrever ali (policy book_covers_*_members).
     */
    override suspend fun uploadBookCover(clubId: String, bookId: String, bytes: ByteArray): String? = runCatching {
        val path = "$clubId/$bookId.jpg"
        val bucket = supabase.storage.from("book-covers")
        // Upload sobreescreve se ja existir (caso usuario troque a capa).
        bucket.upload(path, bytes) {
            upsert = true
            contentType = io.ktor.http.ContentType.Image.JPEG
        }
        // Signed URL valida por 1 ano (max permitido pelo Supabase Storage).
        val signedUrl = bucket.createSignedUrl(path, kotlin.time.Duration.parse("365d"))
        // signed URL ja vem com prefixo do servidor
        "${BuildConfig.SUPABASE_URL}$signedUrl"
    }.getOrNull()

    override suspend fun insertClubBook(clubBook: ClubBook) {
        // Optimistic local-first + fila (P0-2): offline não perde mais a criação.
        dao.upsertClubBook(clubBook)
        val dto = clubBook.toDto()
        tryRemoteOrEnqueue("insert_club_book", json.encodeToString(dto), notifyTable = "club_books") {
            supabase.from("club_books").upsert(dto)
        }
    }

    override fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>> {
        val key = "clubBooks:$clubId"
        val reload: suspend () -> Unit = { syncClubBooks(clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.MED) { syncClubBooks(clubId) } }
        ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
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

    @Serializable
    private data class JoinClubBookFull(
        @SerialName("book_id") val bookId: String,
        val status: String,
        val ordem: Int = 0,
        @SerialName("data_encontro") val dataEncontro: String? = null,
        @SerialName("books") val book: BookDto,
    )

    override fun getClubBooksFlow(clubId: String): Flow<List<Book>> {
        val key = "clubBooks:$clubId"
        val reload: suspend () -> Unit = { syncClubBooks(clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.MED) { syncClubBooks(clubId) } }
        ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
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

    @Serializable
    private data class StatusOnlyDto(val status: String)

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
            // Cache local sera atualizado via notifyLocalMutation -> syncClubBooks.
            notifyLocalMutation("club_books")
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
            notifyLocalMutation("club_books")
        }
    }

    override fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> {
        val reload: suspend () -> Unit = { syncClubBooks(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.clubBooksByStatusFlow(clubId, status)
    }

    // Helper antigo abaixo removido — `syncClubBooks` cuida de tudo via Room.
    @Suppress("UNUSED")
    private fun _oldClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> {
        val flow = stateOf<List<ClubBook>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("club_books").select {
                    filter {
                        eq("club_id", clubId)
                        eq("status", status)
                    }
                }.decodeList<ClubBookDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
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
            notifyLocalMutation("club_books")
        }
    }

    // ============================================================
    // CHAPTERS
    // ============================================================

    override suspend fun insertChapters(chapters: List<Chapter>) {
        if (chapters.isEmpty()) return
        runCatching {
            supabase.from("chapters").upsert(chapters.map { it.toDto() })
            dao.upsertChapters(chapters)
        }
    }

    override fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>> {
        scope.launch {
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

    /**
     * Salva a lista de capítulos por DIFF por ID ESTÁVEL (uuid). A identidade do
     * capítulo é o `id` (uuid), NÃO o numero:
     *  - P0-1: antes o id era `ch_<bookId>_<numero>` (texto) enviado pra coluna
     *    `uuid` do servidor -> Postgres rejeitava (22P02), o erro era engolido e
     *    o capítulo NUNCA sincronizava (comentários iam pra dead-letter). Agora o
     *    id é uuid de verdade (gerado na tela), aceito pelo servidor.
     *  - B2: como o vínculo comentário→capítulo é o id (uuid) e não o numero,
     *    reordenar/renumerar capítulos NÃO remaneja mais os comentários.
     * Capítulos que ficam são atualizados in-place (upsert); só os removidos
     * (id fora da lista) são apagados — a discussão dos mantidos é preservada.
     */
    override suspend fun saveChapters(bookId: String, chapters: List<Chapter>) {
        val keepIds = chapters.map { it.id }
        // Local-first: upsert (in-place) + deleta só os removidos, por id.
        dao.upsertChapters(chapters)
        if (keepIds.isNotEmpty()) dao.deleteChaptersNotInIds(bookId, keepIds) else dao.deleteChaptersForBook(bookId)
        notifyLocalMutation("chapters")
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

    // ---- Índice compartilhado por ISBN (crowdsourcing entre TODOS os clubes) ----

    /** Busca o índice de capítulos que ALGUÉM já cadastrou pra este ISBN. Um
     *  cadastro serve o app inteiro. Retorna null se ninguém compartilhou ainda. */
    override suspend fun getChapterTemplate(isbn: String): List<Pair<Int, String>>? = runCatching {
        val row = supabase.from("chapter_templates").select {
            filter { eq("isbn", isbn) }
            limit(1)
        }.decodeSingleOrNull<ChapterTemplateDto>()
        row?.chapters?.map { it.numero to it.titulo }?.sortedBy { it.first }
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    /** Compartilha (ou atualiza) o índice deste ISBN com a comunidade. */
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

    // ============================================================
    // BOOK SUMMARIES / RATINGS
    // ============================================================

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
            notifyLocalMutation("book_summaries")
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
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_summaries", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookSummaryFlow(bookId, clubId)
    }

    override suspend fun insertBookRating(rating: BookRating) {
        // Local-first + fila: grava no Room antes do remoto e enfileira se
        // offline. Antes era remoto-primeiro dentro de runCatching — avaliar um
        // livro sem internet sumia sem aviso (o dao.upsert nem rodava).
        dao.upsertBookRatings(listOf(rating))
        val payload = buildJsonObject {
            put("bookId", rating.bookId)
            put("clubId", rating.clubId)
            put("userId", rating.userId)
            put("stars", rating.stars.toString())
            put("comment", rating.comment)
        }.toString()
        tryRemoteOrEnqueue("upsert_book_rating", payload, notifyTable = "book_ratings") {
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
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_ratings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookRatingsFlow(bookId, clubId)
    }

    override fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?> {
        scope.launch {
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

    // ---- book_favorites ----
    // Favorito PESSOAL de livro (cross-clube). Local-first + fila offline, igual
    // book_ratings: grava no Room na hora e enfileira se offline (idempotente).
    override suspend fun setBookFavorite(userId: String, bookId: String, favorite: Boolean) {
        val payload = buildJsonObject {
            put("userId", userId)
            put("bookId", bookId)
        }.toString()
        if (favorite) {
            dao.upsertBookFavorites(listOf(BookFavorite(userId, bookId, System.currentTimeMillis())))
            tryRemoteOrEnqueue("insert_book_favorite", payload, notifyTable = "book_favorites") {
                supabase.from("book_favorites").upsert(BookFavoriteInsertDto(userId = userId, bookId = bookId))
            }
        } else {
            dao.deleteBookFavorite(userId, bookId)
            tryRemoteOrEnqueue("delete_book_favorite", payload, notifyTable = "book_favorites") {
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
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_favorites", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.favoriteBooksFlow(userId)
    }
}
