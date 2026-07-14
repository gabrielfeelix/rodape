package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.*
import com.example.util.MeetingTime
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put


// ============================================================================
// RemoteRepository
//
// Substitui RodapeRepository (Room). Mantem EXATAMENTE as mesmas assinaturas
// publicas pra reduzir o impacto no MainViewModel a uma so troca de instancia.
//
// Estrategia:
//  - Leituras one-shot: `suspend fun ... = supabase.from("X").select { filter { ... } }`.
//  - Leituras reativas (Flow): polling via MutableStateFlow inicializada com select.
//    Pra MVP nao usamos Realtime WebSocket — UI re-le ao chamar a acao.
//    Trabalho de Realtime fica pra fase futura (decisao: simplicidade > vivacidade).
//  - Mutacoes: `supabase.from("X").upsert(...)` ou `.delete { filter { ... } }`.
//  - Operacoes privilegiadas: RPC SECURITY DEFINER (`create_club`, `join_club_with_code`,
//    `promote_member`, etc.).
//
// Erros: propagam excecao. UI ja tem fallback com `?: emptyList()` em quase tudo;
// onde nao tem, a tela fica vazia momentaneamente — sem crash.
// ============================================================================

@javax.inject.Singleton
internal class RemoteRepository @javax.inject.Inject constructor(
    private val engine: SyncEngine,
    private val supabase: SupabaseClient = Supabase.client,
) {
    // Ctor de conveniência p/ o DrainQueueWorker (F5.0): o worker roda em background e cria
    // um kernel PRÓPRIO e self-contained pra drenar a fila compartilhada (Room/pendingDao são
    // singletons de processo), sem depender do engine @Singleton da UI. A UI recebe o engine
    // do Hilt via o ctor @Inject acima — um engine só do lado da UI (antes a fachada criava o
    // seu, separado do @Singleton do Hilt: realtime duplicado).
    constructor(appContext: android.content.Context) : this(SyncEngine(appContext, Supabase.client))

    // ============================================================
    // KERNEL OFFLINE → SyncEngine (F3b)
    // ============================================================
    // A infra offline (SWR/TTL, write-queue + 25 handlers, realtime, cache
    // lifecycle) foi extraida SEM mudanca de logica pra SyncEngine.kt. Este
    // repo agora e uma FACHADA (strangler-fig): os metodos de dominio abaixo
    // ficaram intocados; os simbolos que eles usavam (dao, json, scope,
    // syncOnce, tryRemoteOrEnqueue, ensureRealtime, notifyLocalMutation,
    // markSynced, stateOf, Ttl) resolvem via os delegadores finos abaixo.

    // --- Delegadores pro kernel: mantem os call sites do dominio byte-a-byte. ---
    private val json get() = engine.json
    private val dao get() = engine.dao
    private val scope get() = engine.scope
    private fun markSynced(key: String) = engine.markSynced(key)
    private suspend fun syncOnce(key: String, ttlMs: Long, block: suspend () -> Unit) =
        engine.syncOnce(key, ttlMs, block)
    private suspend fun tryRemoteOrEnqueue(
        kind: String,
        payload: String,
        notifyTable: String? = null,
        block: suspend () -> Unit,
    ) = engine.tryRemoteOrEnqueue(kind, payload, notifyTable, block)
    private fun notifyLocalMutation(table: String) = engine.notifyLocalMutation(table)
    private fun ensureRealtime(
        table: String,
        filterColumn: String? = null,
        filterValue: String? = null,
        reload: suspend () -> Unit,
    ) = engine.ensureRealtime(table, filterColumn, filterValue, reload)
    private fun <T> stateOf(initial: T): MutableStateFlow<T> = engine.stateOf(initial)

    // --- API publica (VM/worker), inalterada — delega pra engine. ---
    suspend fun clearLocalCache() = engine.clearLocalCache()
    suspend fun clearLocalCacheNoDrain() = engine.clearLocalCacheNoDrain()
    suspend fun tryDrainPendingQueue(): Int = engine.tryDrainPendingQueue()
    val pendingMutationsCount: Flow<Int> get() = engine.pendingMutationsCount
    suspend fun forceRefresh() = engine.forceRefresh()
    fun close() = engine.close()

    // --- F3c: repos de domínio fatiados. A fachada delega, reusando o MESMO
    //     `engine` (não cria kernel novo). VMs migram pra injetar a interface no F5. ---
    private val progressRepo: com.example.data.remote.repo.ProgressRepository =
        com.example.data.remote.repo.OfflineFirstProgressRepository(engine, supabase, engine.dao)
    private val notificationRepo: com.example.data.remote.repo.NotificationRepository =
        com.example.data.remote.repo.OfflineFirstNotificationRepository(engine, supabase, engine.dao)
    private val quoteRepo: com.example.data.remote.repo.QuoteRepository =
        com.example.data.remote.repo.OfflineFirstQuoteRepository(engine, supabase, engine.dao)
    private val userRepo: com.example.data.remote.repo.UserRepository =
        com.example.data.remote.repo.OfflineFirstUserRepository(engine, supabase, engine.dao)
    private val moderationRepo: com.example.data.remote.repo.ModerationRepository =
        com.example.data.remote.repo.OfflineFirstModerationRepository(engine, supabase, engine.dao)
    private val votingRepo: com.example.data.remote.repo.VotingRepository =
        com.example.data.remote.repo.OfflineFirstVotingRepository(engine, supabase, engine.dao)
    private val discussionRepo: com.example.data.remote.repo.DiscussionRepository =
        com.example.data.remote.repo.OfflineFirstDiscussionRepository(engine, supabase, engine.dao)
    private val meetingRepo: com.example.data.remote.repo.MeetingRepository =
        com.example.data.remote.repo.OfflineFirstMeetingRepository(engine, supabase, engine.dao)
    private val bookRepo: com.example.data.remote.repo.BookRepository =
        com.example.data.remote.repo.OfflineFirstBookRepository(engine, supabase, engine.dao)
    private val clubRepo: com.example.data.remote.repo.ClubRepository =
        com.example.data.remote.repo.OfflineFirstClubRepository(engine, supabase, engine.dao)

    // ============================================================
    // USERS / PROFILES
    // ============================================================

    // F3c: delega pro UserRepository fatiado.
    fun getUserFlow(userId: String): Flow<User?> = userRepo.getUserFlow(userId)

    suspend fun getUser(userId: String): User? = userRepo.getUser(userId)

    fun getAllUsersFlow(): Flow<List<User>> = userRepo.getAllUsersFlow()

    suspend fun insertUser(user: User) = userRepo.insertUser(user)

    suspend fun updateFontScale(userId: String, scale: Float) =
        userRepo.updateFontScale(userId, scale)

    // ============================================================
    // CLUBS
    // ============================================================

    // F3c: delega pro ClubRepository fatiado.
    fun getClubFlow(clubId: String): Flow<Club?> = clubRepo.getClubFlow(clubId)

    suspend fun getClub(clubId: String): Club? = clubRepo.getClub(clubId)

    suspend fun getClubByCodigo(codigo: String): Club? = clubRepo.getClubByCodigo(codigo)

    fun getClubsForUser(userId: String): Flow<List<Club>> = clubRepo.getClubsForUser(userId)

    suspend fun getClubsForUserList(userId: String): List<Club> = clubRepo.getClubsForUserList(userId)

    suspend fun insertClub(club: Club) = clubRepo.insertClub(club)

    suspend fun createClubViaRpc(nome: String, descricao: String?, cor: String, privacidade: String): String =
        clubRepo.createClubViaRpc(nome, descricao, cor, privacidade)

    suspend fun joinClubWithCodeViaRpc(codigo: String): String? =
        clubRepo.joinClubWithCodeViaRpc(codigo)

    suspend fun leaveClubViaRpc(clubId: String) = clubRepo.leaveClubViaRpc(clubId)

    suspend fun deleteOwnAccountViaRpc() = clubRepo.deleteOwnAccountViaRpc()

    suspend fun regenerateInviteCodeViaRpc(clubId: String): String =
        clubRepo.regenerateInviteCodeViaRpc(clubId)

    suspend fun promoteMemberViaRpc(clubId: String, targetUserId: String) =
        clubRepo.promoteMemberViaRpc(clubId, targetUserId)

    suspend fun demoteAdminViaRpc(clubId: String, targetUserId: String) =
        clubRepo.demoteAdminViaRpc(clubId, targetUserId)

    suspend fun transferSuperAdminViaRpc(clubId: String, toUserId: String) =
        clubRepo.transferSuperAdminViaRpc(clubId, toUserId)

    suspend fun removeMemberViaRpc(clubId: String, targetUserId: String, motivo: String?) =
        clubRepo.removeMemberViaRpc(clubId, targetUserId, motivo)

    // F3c: delega pro VotingRepository fatiado.
    suspend fun closeVotingRoundViaRpc(roundId: String) = votingRepo.closeVotingRoundViaRpc(roundId)

    // ============================================================
    // CLUB MEMBERS
    // ============================================================

    suspend fun insertClubMember(member: ClubMember) = clubRepo.insertClubMember(member)

    fun getClubMembersFlow(clubId: String): Flow<List<User>> = clubRepo.getClubMembersFlow(clubId)

    suspend fun getClubMember(clubId: String, userId: String): ClubMember? =
        clubRepo.getClubMember(clubId, userId)

    suspend fun getClubMembersListOrderedByJoin(clubId: String): List<ClubMember> =
        clubRepo.getClubMembersListOrderedByJoin(clubId)

    fun getClubMembersRawFlow(clubId: String): Flow<List<ClubMember>> =
        clubRepo.getClubMembersRawFlow(clubId)

    suspend fun updateMemberPapel(clubId: String, userId: String, papel: String) =
        clubRepo.updateMemberPapel(clubId, userId, papel)

    suspend fun deleteClubMember(clubId: String, userId: String) =
        clubRepo.deleteClubMember(clubId, userId)

    suspend fun insertMemberRemoval(removal: MemberRemoval) = clubRepo.insertMemberRemoval(removal)

    fun getMemberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>> =
        clubRepo.getMemberRemovalsForClubFlow(clubId)

    // ============================================================
    // CLUB ADMIN
    // ============================================================

    suspend fun updateClubInfo(clubId: String, nome: String, descricao: String, cor: String, privacidade: String) =
        clubRepo.updateClubInfo(clubId, nome, descricao, cor, privacidade)

    suspend fun updateClubCodigo(clubId: String, codigo: String) =
        clubRepo.updateClubCodigo(clubId, codigo)

    suspend fun updateClubArquivado(clubId: String, arquivado: Boolean) =
        clubRepo.updateClubArquivado(clubId, arquivado)

    fun getArchivedClubsForUserFlow(userId: String): Flow<List<Club>> =
        clubRepo.getArchivedClubsForUserFlow(userId)

    // ============================================================
    // BOOKS / CLUB_BOOKS
    // ============================================================

    // F3c: delega pro BookRepository fatiado.
    suspend fun insertBook(book: Book) = bookRepo.insertBook(book)

    suspend fun uploadBookCover(clubId: String, bookId: String, bytes: ByteArray): String? =
        bookRepo.uploadBookCover(clubId, bookId, bytes)

    suspend fun insertClubBook(clubBook: ClubBook) = bookRepo.insertClubBook(clubBook)

    fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>> =
        bookRepo.getBookByStatusFlow(clubId, status)

    fun getClubBooksFlow(clubId: String): Flow<List<Book>> = bookRepo.getClubBooksFlow(clubId)

    suspend fun getClubBookStatus(clubId: String, bookId: String): String? =
        bookRepo.getClubBookStatus(clubId, bookId)

    suspend fun getBook(id: String): Book? = bookRepo.getBook(id)

    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String) =
        bookRepo.updateClubBookStatus(clubId, bookId, status)

    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?) =
        bookRepo.updateClubBookMeetingDate(clubId, bookId, dataEncontro)

    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> =
        bookRepo.getClubBooksByStatusFlow(clubId, status)

    suspend fun deleteClubBook(clubId: String, bookId: String) =
        bookRepo.deleteClubBook(clubId, bookId)

    // ============================================================
    // CHAPTERS
    // ============================================================

    suspend fun insertChapters(chapters: List<Chapter>) = bookRepo.insertChapters(chapters)

    fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>> =
        bookRepo.getChaptersForBookFlow(bookId)

    suspend fun deleteChaptersForBook(bookId: String) = bookRepo.deleteChaptersForBook(bookId)

    suspend fun saveChapters(bookId: String, chapters: List<Chapter>) =
        bookRepo.saveChapters(bookId, chapters)

    suspend fun getChapterTemplate(isbn: String): List<Pair<Int, String>>? =
        bookRepo.getChapterTemplate(isbn)

    suspend fun shareChapterTemplate(isbn: String, tituloLivro: String, chapters: List<Pair<Int, String>>, userId: String) =
        bookRepo.shareChapterTemplate(isbn, tituloLivro, chapters, userId)

    // ============================================================
    // USER PROGRESS
    // ============================================================

    // F3c: delega pro ProgressRepository fatiado (impl offline-first).
    suspend fun insertUserProgress(progress: UserProgress) =
        progressRepo.insertUserProgress(progress)

    fun getUserProgressFlow(userId: String, clubId: String, bookId: String): Flow<UserProgress?> =
        progressRepo.getUserProgressFlow(userId, clubId, bookId)

    suspend fun getUserProgress(userId: String, clubId: String, bookId: String): UserProgress? =
        progressRepo.getUserProgress(userId, clubId, bookId)

    fun getAllProgressForClubFlow(clubId: String): Flow<List<UserProgress>> =
        progressRepo.getAllProgressForClubFlow(clubId)

    // ============================================================
    // COMMENTS / REACTIONS
    // ============================================================

    // F3c: delega pro DiscussionRepository fatiado.
    suspend fun insertComment(comment: Comment) = discussionRepo.insertComment(comment)

    fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>> =
        discussionRepo.getCommentsForChapterFlow(chapterId, clubId)

    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>> =
        discussionRepo.getCommentsForBookFlow(bookId, clubId)

    suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String) =
        discussionRepo.softRemoveComment(commentId, removidoPor, motivo)

    suspend fun restoreComment(commentId: String) = discussionRepo.restoreComment(commentId)

    suspend fun editOwnComment(commentId: String, novoTexto: String) =
        discussionRepo.editOwnComment(commentId, novoTexto)

    suspend fun deleteOwnComment(commentId: String) = discussionRepo.deleteOwnComment(commentId)

    // ============================================================
    // MODERAÇÃO — denúncia, bloqueio, remoção (migration 0010)
    // ============================================================

    /** Denuncia um conteúdo. Idempotente por (reporter, tipo, alvo) — reenvio é no-op.
     *  Local-first via fila offline; não precisa de cache local (é write-only). */
    // F3c: delega pro ModerationRepository fatiado.
    suspend fun reportContent(
        reporterId: String,
        clubId: String,
        targetType: ReportTargetType,
        targetId: String,
        targetUserId: String,
        motivo: ReportReason,
        detalhe: String?,
    ) = moderationRepo.reportContent(reporterId, clubId, targetType, targetId, targetUserId, motivo, detalhe)

    suspend fun blockUser(me: String, blockedId: String) = moderationRepo.blockUser(me, blockedId)

    suspend fun unblockUser(me: String, blockedId: String) = moderationRepo.unblockUser(me, blockedId)

    fun observeBlockedIds(me: String): Flow<List<String>> = moderationRepo.observeBlockedIds(me)

    fun isBlockedFlow(me: String, other: String): Flow<Boolean> = moderationRepo.isBlockedFlow(me, other)

    suspend fun moderateRemoveContent(
        type: ReportTargetType,
        targetId: String,
        targetUserId: String,
        clubId: String,
        motivo: String?,
        removidoPor: String,
    ) = moderationRepo.moderateRemoveContent(type, targetId, targetUserId, clubId, motivo, removidoPor)

    suspend fun fetchPendingReports(clubId: String): List<ContentReport> =
        moderationRepo.fetchPendingReports(clubId)

    suspend fun dismissReport(reportId: String) = moderationRepo.dismissReport(reportId)

    fun getRemovedCommentsForClubFlow(clubId: String): Flow<List<Comment>> =
        moderationRepo.getRemovedCommentsForClubFlow(clubId)

    // ---- reactions ----

    suspend fun insertReaction(reaction: Reaction) = discussionRepo.insertReaction(reaction)

    suspend fun deleteReaction(reaction: Reaction) = discussionRepo.deleteReaction(reaction)

    fun getReactionsForCommentFlow(commentId: String): Flow<List<Reaction>> =
        discussionRepo.getReactionsForCommentFlow(commentId)

    fun getReactionsForChapterFlow(chapterId: String): Flow<List<Reaction>> =
        discussionRepo.getReactionsForChapterFlow(chapterId)

    // ============================================================
    // VOTES & VOTING ROUNDS
    // ============================================================

    // F3c: delega pro VotingRepository fatiado.
    suspend fun insertVote(vote: Vote) = votingRepo.insertVote(vote)

    suspend fun setUserVoteInRound(userId: String, roundId: String, bookId: String) =
        votingRepo.setUserVoteInRound(userId, roundId, bookId)

    suspend fun clearVotesForUserInClub(userId: String, clubId: String) =
        votingRepo.clearVotesForUserInClub(userId, clubId)

    fun getVotesForClubFlow(clubId: String): Flow<List<Vote>> = votingRepo.getVotesForClubFlow(clubId)

    fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>> = votingRepo.getVotesForRoundFlow(roundId)

    suspend fun getVotesForRound(roundId: String): List<Vote> = votingRepo.getVotesForRound(roundId)

    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String) =
        votingRepo.removeUserVoteForBookInRound(userId, roundId, bookId)

    suspend fun countUserVotesInRound(userId: String, roundId: String): Int =
        votingRepo.countUserVotesInRound(userId, roundId)

    suspend fun insertVotingRound(round: VotingRound) = votingRepo.insertVotingRound(round)

    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?> =
        votingRepo.getActiveVotingRoundFlow(clubId)

    suspend fun getActiveVotingRound(clubId: String): VotingRound? =
        votingRepo.getActiveVotingRound(clubId)

    suspend fun closeVotingRound(id: String, vencedoresJson: String) =
        votingRepo.closeVotingRound(id, vencedoresJson)

    // ============================================================
    // BOOK SUGGESTIONS
    // ============================================================

    // F3c: delega pro VotingRepository fatiado (sugestões vivem com votação).
    suspend fun insertBookSuggestion(suggestion: BookSuggestion) =
        votingRepo.insertBookSuggestion(suggestion)

    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?> =
        votingRepo.getBookSuggestionFlow(bookId, clubId)

    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>> =
        votingRepo.getBookSuggestionsForClubFlow(clubId)

    suspend fun deleteBookSuggestion(bookId: String, clubId: String) =
        votingRepo.deleteBookSuggestion(bookId, clubId)

    suspend fun deleteVotesForBook(bookId: String) = votingRepo.deleteVotesForBook(bookId)

    // ============================================================
    // MEETINGS
    // ============================================================

    // F3c: delega pro MeetingRepository fatiado.
    suspend fun insertMeeting(meeting: Meeting) = meetingRepo.insertMeeting(meeting)

    fun getLatestMeetingFlow(clubId: String): Flow<Meeting?> = meetingRepo.getLatestMeetingFlow(clubId)

    fun getAllMeetingsFlow(clubId: String): Flow<List<Meeting>> = meetingRepo.getAllMeetingsFlow(clubId)

    fun getScheduledMeetingsForClubFlow(clubId: String): Flow<List<Meeting>> =
        meetingRepo.getScheduledMeetingsForClubFlow(clubId)

    suspend fun getMeetingById(meetingId: String): Meeting? = meetingRepo.getMeetingById(meetingId)

    fun getMeetingByIdFlow(meetingId: String): Flow<Meeting?> = meetingRepo.getMeetingByIdFlow(meetingId)

    fun getMeetingsForBookFlow(clubId: String, bookId: String): Flow<List<Meeting>> =
        meetingRepo.getMeetingsForBookFlow(clubId, bookId)

    suspend fun getMeetingsForBookList(clubId: String, bookId: String): List<Meeting> =
        meetingRepo.getMeetingsForBookList(clubId, bookId)

    suspend fun updateMeetingStatus(meetingId: String, status: String) =
        meetingRepo.updateMeetingStatus(meetingId, status)

    suspend fun deleteMeeting(meetingId: String) = meetingRepo.deleteMeeting(meetingId)

    suspend fun deleteRsvpsForMeeting(meetingId: String) = meetingRepo.deleteRsvpsForMeeting(meetingId)

    suspend fun insertMeetingRsvp(rsvp: MeetingRsvp) = meetingRepo.insertMeetingRsvp(rsvp)

    fun getRsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>> =
        meetingRepo.getRsvpsForMeetingFlow(meetingId)

    fun getRsvpForMeetingOfUserFlow(meetingId: String, userId: String): Flow<MeetingRsvp?> =
        meetingRepo.getRsvpForMeetingOfUserFlow(meetingId, userId)

    suspend fun insertMeetingPattern(pattern: MeetingPattern) =
        meetingRepo.insertMeetingPattern(pattern)

    suspend fun generateMeetingsFromPattern(pattern: MeetingPattern, horizon: Int = 8) =
        meetingRepo.generateMeetingsFromPattern(pattern, horizon)

    fun getActiveMeetingPatternFlow(clubId: String): Flow<MeetingPattern?> =
        meetingRepo.getActiveMeetingPatternFlow(clubId)

    suspend fun getActiveMeetingPattern(clubId: String): MeetingPattern? =
        meetingRepo.getActiveMeetingPattern(clubId)

    suspend fun deactivateMeetingPatterns(clubId: String) =
        meetingRepo.deactivateMeetingPatterns(clubId)

    suspend fun insertMeetingMinutes(minutes: MeetingMinutes) =
        meetingRepo.insertMeetingMinutes(minutes)

    fun getMeetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?> =
        meetingRepo.getMeetingMinutesFlow(meetingId)

    suspend fun insertMeetingNote(note: MeetingNote) = meetingRepo.insertMeetingNote(note)

    fun getMeetingNoteFlow(meetingId: String, userId: String): Flow<MeetingNote?> =
        meetingRepo.getMeetingNoteFlow(meetingId, userId)

    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    // F3c: delega pro NotificationRepository fatiado.
    suspend fun insertNotification(notification: DbNotification) =
        notificationRepo.insertNotification(notification)

    suspend fun markAllNotificationsAsRead(userId: String) =
        notificationRepo.markAllNotificationsAsRead(userId)

    suspend fun markNotificationAsRead(id: String) =
        notificationRepo.markNotificationAsRead(id)

    fun getNotificationsFlow(userId: String): Flow<List<DbNotification>> =
        notificationRepo.getNotificationsFlow(userId)

    // ============================================================
    // SAVED QUOTES
    // ============================================================

    // F3c: delega pro QuoteRepository fatiado.
    suspend fun insertSavedQuote(quote: SavedQuote) = quoteRepo.insertSavedQuote(quote)

    suspend fun deleteSavedQuote(quote: SavedQuote) = quoteRepo.deleteSavedQuote(quote)

    fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>> =
        quoteRepo.getSavedQuotesForUserFlow(userId)

    fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>> =
        quoteRepo.getSavedQuotesForBookFlow(userId, bookId)

    // ============================================================
    // BOOK SUMMARIES / RATINGS
    // ============================================================

    suspend fun insertBookSummary(summary: BookSummary) = bookRepo.insertBookSummary(summary)

    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?> =
        bookRepo.getBookSummaryFlow(bookId, clubId)

    suspend fun insertBookRating(rating: BookRating) = bookRepo.insertBookRating(rating)

    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>> =
        bookRepo.getBookRatingsFlow(bookId, clubId)

    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?> =
        bookRepo.getBookRatingOfUserFlow(bookId, clubId, userId)

    suspend fun setBookFavorite(userId: String, bookId: String, favorite: Boolean) =
        bookRepo.setBookFavorite(userId, bookId, favorite)

    fun isBookFavoriteFlow(userId: String, bookId: String): Flow<Boolean> =
        bookRepo.isBookFavoriteFlow(userId, bookId)

    suspend fun anyClubIdForBook(bookId: String): String? = bookRepo.anyClubIdForBook(bookId)

    fun getFavoriteBooksForUserFlow(userId: String): Flow<List<Book>> =
        bookRepo.getFavoriteBooksForUserFlow(userId)

    // ============================================================
    // SEED — no-op (app nasce vazio em producao)
    // ============================================================

    suspend fun seedDatabase() {
        // 9B: app nasce vazio em producao. Nenhum seed.
    }
}
