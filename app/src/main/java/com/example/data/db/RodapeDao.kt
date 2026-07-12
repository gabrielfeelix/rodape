package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO unico do app. Cada metodo serve UM tipo de query no Room.
 *
 * Convencao:
 *  - `Flow<...>` retorna observable — UI assina e re-renderiza quando Room muda.
 *  - `suspend fun` retorna snapshot — usado pelo sync pra inserir/atualizar.
 *  - `replaceXxx(clubId, items)` faz "replace all" atomico via @Transaction —
 *    apaga tudo do clube + insere novo. Padrao quando o sync re-baixa lista
 *    inteira do servidor (mais simples que diff manual).
 *
 * O Room aqui e SO cache local. Source of truth = Supabase. Conflitos sao
 * resolvidos pela ordem dos eventos: sempre que sync chega, sobreescreve.
 */
@Dao
interface RodapeDao {

    // ====================== USERS / PROFILES ======================
    @Upsert
    suspend fun upsertUser(user: User)

    @Upsert
    suspend fun upsertUsers(users: List<User>)

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun userFlow(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun user(userId: String): User?

    // ====================== CLUBS ======================
    @Upsert
    suspend fun upsertClub(club: Club)

    @Upsert
    suspend fun upsertClubs(clubs: List<Club>)

    @Query("DELETE FROM clubs WHERE id NOT IN (:keepIds)")
    suspend fun pruneClubsExcept(keepIds: List<String>)

    @Query("SELECT * FROM clubs WHERE id = :clubId LIMIT 1")
    fun clubFlow(clubId: String): Flow<Club?>

    @Query("SELECT * FROM clubs WHERE id = :clubId LIMIT 1")
    suspend fun club(clubId: String): Club?

    @Query("SELECT * FROM clubs WHERE codigo = :codigo LIMIT 1")
    suspend fun clubByCodigo(codigo: String): Club?

    @Query("SELECT * FROM clubs WHERE arquivado = 0 ORDER BY criadoEm DESC")
    fun clubsActiveFlow(): Flow<List<Club>>

    @Query("SELECT * FROM clubs WHERE arquivado = 1 ORDER BY criadoEm DESC")
    fun clubsArchivedFlow(): Flow<List<Club>>

    // ====================== CLUB MEMBERS ======================
    @Upsert
    suspend fun upsertMembers(members: List<ClubMember>)

    @Query("DELETE FROM club_members WHERE clubId = :clubId AND userId NOT IN (:keepUserIds)")
    suspend fun pruneMembersInClubExcept(clubId: String, keepUserIds: List<String>)

    @Query("DELETE FROM club_members WHERE clubId = :clubId AND userId = :userId")
    suspend fun deleteMember(clubId: String, userId: String)

    @Query("SELECT * FROM club_members WHERE clubId = :clubId ORDER BY entrouEm ASC")
    fun membersInClubFlow(clubId: String): Flow<List<ClubMember>>

    @Query("SELECT * FROM club_members WHERE clubId = :clubId AND userId = :userId LIMIT 1")
    suspend fun member(clubId: String, userId: String): ClubMember?

    @Query("""
        SELECT u.* FROM users u
        INNER JOIN club_members m ON u.id = m.userId
        WHERE m.clubId = :clubId
        ORDER BY m.entrouEm ASC
    """)
    fun memberUsersInClubFlow(clubId: String): Flow<List<User>>

    // ====================== BOOKS ======================
    @Upsert
    suspend fun upsertBook(book: Book)

    @Upsert
    suspend fun upsertBooks(books: List<Book>)

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun book(id: String): Book?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun bookFlow(id: String): Flow<Book?>

    // ====================== CLUB_BOOKS ======================
    @Upsert
    suspend fun upsertClubBook(cb: ClubBook)

    @Upsert
    suspend fun upsertClubBooks(cbs: List<ClubBook>)

    @Query("DELETE FROM club_books WHERE clubId = :clubId AND bookId NOT IN (:keepBookIds)")
    suspend fun pruneClubBooksInClubExcept(clubId: String, keepBookIds: List<String>)

    @Query("DELETE FROM club_books WHERE clubId = :clubId AND bookId = :bookId")
    suspend fun deleteClubBook(clubId: String, bookId: String)

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN club_books cb ON cb.bookId = b.id
        WHERE cb.clubId = :clubId AND cb.status = :status
        ORDER BY cb.ordem ASC
    """)
    fun booksByStatusFlow(clubId: String, status: String): Flow<List<Book>>

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN club_books cb ON cb.bookId = b.id
        WHERE cb.clubId = :clubId
        ORDER BY cb.ordem ASC
    """)
    fun clubBooksFlow(clubId: String): Flow<List<Book>>

    @Query("SELECT status FROM club_books WHERE clubId = :clubId AND bookId = :bookId LIMIT 1")
    suspend fun clubBookStatus(clubId: String, bookId: String): String?

    @Query("SELECT * FROM club_books WHERE clubId = :clubId AND status = :status")
    fun clubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>>

    // ====================== CHAPTERS ======================
    @Upsert
    suspend fun upsertChapters(chapters: List<Chapter>)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersForBook(bookId: String)

    // Deleta SÓ os capítulos que saíram da lista (numero fora de keepNumeros).
    // Os que ficam são atualizados in-place por upsert — assim os comentários dos
    // capítulos mantidos NÃO são cascateados (bug P0 do delete-all+reinsert).
    @Query("DELETE FROM chapters WHERE bookId = :bookId AND numero NOT IN (:keepNumeros)")
    suspend fun deleteChaptersNotIn(bookId: String, keepNumeros: List<Int>)

    // Variante por ID (identidade estável de capítulo). Reordenar/renumerar não
    // remaneja mais os comentários: o vínculo comentário→capítulo é o id (uuid),
    // não o numero. Deleta só capítulos cujo id saiu da lista.
    @Query("DELETE FROM chapters WHERE bookId = :bookId AND id NOT IN (:keepIds)")
    suspend fun deleteChaptersNotInIds(bookId: String, keepIds: List<String>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY numero ASC")
    fun chaptersForBookFlow(bookId: String): Flow<List<Chapter>>

    // ====================== USER PROGRESS ======================
    @Upsert
    suspend fun upsertProgress(p: UserProgress)

    @Upsert
    suspend fun upsertProgresses(ps: List<UserProgress>)

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND clubId = :clubId AND bookId = :bookId LIMIT 1")
    fun progressFlow(userId: String, clubId: String, bookId: String): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND clubId = :clubId AND bookId = :bookId LIMIT 1")
    suspend fun progress(userId: String, clubId: String, bookId: String): UserProgress?

    @Query("SELECT * FROM user_progress WHERE clubId = :clubId")
    fun allProgressForClubFlow(clubId: String): Flow<List<UserProgress>>

    // ====================== COMMENTS ======================
    @Upsert
    suspend fun upsertComment(c: Comment)

    @Upsert
    suspend fun upsertComments(cs: List<Comment>)

    @Query("DELETE FROM comments WHERE chapterId = :chapterId AND clubId = :clubId AND id NOT IN (:keepIds)")
    suspend fun pruneCommentsInChapterExcept(chapterId: String, clubId: String, keepIds: List<String>)

    @Query("SELECT * FROM comments WHERE chapterId = :chapterId AND clubId = :clubId ORDER BY criadoEm ASC")
    fun commentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>>

    @Query("""
        SELECT c.* FROM comments c
        INNER JOIN chapters ch ON c.chapterId = ch.id
        WHERE ch.bookId = :bookId AND c.clubId = :clubId
        ORDER BY ch.numero ASC, c.criadoEm ASC
    """)
    fun commentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>>

    @Query("SELECT * FROM comments WHERE clubId = :clubId AND removido = 1 ORDER BY criadoEm DESC")
    fun removedCommentsForClubFlow(clubId: String): Flow<List<Comment>>

    // Ops locais (optimistic) — o remoto reconcilia depois via sync/fila.
    @Query("UPDATE comments SET texto = :texto WHERE id = :id")
    suspend fun updateCommentText(id: String, texto: String)

    @Query("UPDATE comments SET removido = 1, removidoPor = :by, motivoRemocao = :motivo WHERE id = :id")
    suspend fun markCommentRemoved(id: String, by: String?, motivo: String?)

    @Query("UPDATE comments SET removido = 0, removidoPor = NULL, motivoRemocao = NULL WHERE id = :id")
    suspend fun markCommentRestored(id: String)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteComment(id: String)

    // ====================== REACTIONS ======================
    @Upsert
    suspend fun upsertReactions(rs: List<Reaction>)

    @Query("DELETE FROM reactions WHERE commentId = :commentId AND NOT (commentId || userId || emoji) IN (:keepKeys)")
    suspend fun pruneReactionsForCommentExcept(commentId: String, keepKeys: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReaction(r: Reaction)

    @Query("DELETE FROM reactions WHERE commentId = :commentId AND userId = :userId AND emoji = :emoji")
    suspend fun deleteReaction(commentId: String, userId: String, emoji: String)

    @Query("SELECT * FROM reactions WHERE commentId = :commentId")
    fun reactionsForCommentFlow(commentId: String): Flow<List<Reaction>>

    @Query("""
        SELECT r.* FROM reactions r
        INNER JOIN comments c ON c.id = r.commentId
        WHERE c.chapterId = :chapterId
    """)
    fun reactionsForChapterFlow(chapterId: String): Flow<List<Reaction>>

    // ====================== VOTES ======================
    @Upsert
    suspend fun upsertVotes(vs: List<Vote>)

    @Query("DELETE FROM votes WHERE votingRoundId = :roundId AND NOT (votingRoundId || userId || clubBookId) IN (:keepKeys)")
    suspend fun pruneVotesInRoundExcept(roundId: String, keepKeys: List<String>)

    @Query("DELETE FROM votes WHERE votingRoundId = :roundId AND userId = :userId AND clubBookId = :bookId")
    suspend fun deleteVote(roundId: String, userId: String, bookId: String)

    @Query("SELECT * FROM votes WHERE votingRoundId = :roundId")
    fun votesForRoundFlow(roundId: String): Flow<List<Vote>>

    @Query("SELECT * FROM votes WHERE votingRoundId = :roundId")
    suspend fun votesForRound(roundId: String): List<Vote>

    // ====================== VOTING ROUNDS ======================
    @Upsert
    suspend fun upsertVotingRound(r: VotingRound)

    @Upsert
    suspend fun upsertVotingRounds(rs: List<VotingRound>)

    @Query("SELECT * FROM voting_rounds WHERE clubId = :clubId AND status = 'aberta' ORDER BY abertaEm DESC LIMIT 1")
    fun activeRoundForClubFlow(clubId: String): Flow<VotingRound?>

    // ====================== BOOK SUGGESTIONS ======================
    @Upsert
    suspend fun upsertBookSuggestions(bs: List<BookSuggestion>)

    @Query("DELETE FROM book_suggestions WHERE clubId = :clubId AND bookId NOT IN (:keepBookIds)")
    suspend fun pruneBookSuggestionsInClubExcept(clubId: String, keepBookIds: List<String>)

    @Query("DELETE FROM book_suggestions WHERE clubId = :clubId AND bookId = :bookId")
    suspend fun deleteBookSuggestion(clubId: String, bookId: String)

    @Query("SELECT * FROM book_suggestions WHERE clubId = :clubId AND bookId = :bookId LIMIT 1")
    fun bookSuggestionFlow(clubId: String, bookId: String): Flow<BookSuggestion?>

    @Query("SELECT * FROM book_suggestions WHERE clubId = :clubId")
    fun bookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>>

    // ====================== MEETINGS ======================
    @Upsert
    suspend fun upsertMeetings(ms: List<Meeting>)

    @Query("DELETE FROM meetings WHERE clubId = :clubId AND id NOT IN (:keepIds)")
    suspend fun pruneMeetingsInClubExcept(clubId: String, keepIds: List<String>)

    @Query("DELETE FROM meetings WHERE id = :meetingId")
    suspend fun deleteMeeting(meetingId: String)

    // Encontros gerados por recorrência têm id prefixado 'mtg_pat_'. Remove os
    // FUTUROS ao regenerar o padrão, sem tocar em encontros criados à mão.
    @Query("DELETE FROM meetings WHERE clubId = :clubId AND id LIKE 'mtg_pat_%' AND dataEpoch >= :now")
    suspend fun deleteFutureGeneratedMeetings(clubId: String, now: Long)

    @Query("SELECT * FROM meetings WHERE clubId = :clubId ORDER BY dataEpoch DESC")
    fun meetingsForClubFlow(clubId: String): Flow<List<Meeting>>

    // "Próximo encontro": o agendado mais PRÓXIMO no futuro (não o de rótulo
    // alfabeticamente maior). Ordena pelo instante real e filtra concluído/passado.
    @Query("SELECT * FROM meetings WHERE clubId = :clubId AND status = 'agendado' AND dataEpoch >= :now ORDER BY dataEpoch ASC LIMIT 1")
    fun nextMeetingForClubFlow(clubId: String, now: Long): Flow<Meeting?>

    @Query("SELECT * FROM meetings WHERE clubId = :clubId AND status = 'agendado' ORDER BY dataEpoch ASC")
    fun scheduledMeetingsForClubFlow(clubId: String): Flow<List<Meeting>>

    @Query("SELECT * FROM meetings WHERE id = :meetingId LIMIT 1")
    fun meetingByIdFlow(meetingId: String): Flow<Meeting?>

    @Query("SELECT * FROM meetings WHERE id = :meetingId LIMIT 1")
    suspend fun meetingById(meetingId: String): Meeting?

    @Query("SELECT * FROM meetings WHERE clubId = :clubId AND bookId = :bookId ORDER BY dataEpoch ASC")
    fun meetingsForBookFlow(clubId: String, bookId: String): Flow<List<Meeting>>

    // ====================== MEETING RSVPs ======================
    @Upsert
    suspend fun upsertMeetingRsvp(r: MeetingRsvp)

    @Upsert
    suspend fun upsertMeetingRsvps(rs: List<MeetingRsvp>)

    @Query("DELETE FROM meeting_rsvps WHERE meetingId = :meetingId AND NOT userId IN (:keepUserIds)")
    suspend fun pruneRsvpsForMeetingExcept(meetingId: String, keepUserIds: List<String>)

    @Query("DELETE FROM meeting_rsvps WHERE meetingId = :meetingId")
    suspend fun deleteAllRsvpsForMeeting(meetingId: String)

    @Query("SELECT * FROM meeting_rsvps WHERE meetingId = :meetingId")
    fun rsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>>

    @Query("SELECT * FROM meeting_rsvps WHERE meetingId = :meetingId AND userId = :userId LIMIT 1")
    fun rsvpOfUserForMeetingFlow(meetingId: String, userId: String): Flow<MeetingRsvp?>

    // ====================== MEETING PATTERNS ======================
    @Upsert
    suspend fun upsertMeetingPattern(p: MeetingPattern)

    @Query("SELECT * FROM meeting_patterns WHERE clubId = :clubId AND ativo = 1 LIMIT 1")
    fun activeMeetingPatternForClubFlow(clubId: String): Flow<MeetingPattern?>

    // ====================== MEETING MINUTES ======================
    @Upsert
    suspend fun upsertMeetingMinutes(m: MeetingMinutes)

    @Query("SELECT * FROM meeting_minutes WHERE meetingId = :meetingId LIMIT 1")
    fun meetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?>

    // ====================== MEETING NOTES (private) ======================
    @Upsert
    suspend fun upsertMeetingNote(n: MeetingNote)

    @Query("SELECT * FROM meeting_notes WHERE meetingId = :meetingId AND userId = :userId LIMIT 1")
    fun meetingNoteOfUserFlow(meetingId: String, userId: String): Flow<MeetingNote?>

    // ====================== NOTIFICATIONS ======================
    @Upsert
    suspend fun upsertNotifications(ns: List<DbNotification>)

    @Query("DELETE FROM notifications WHERE userId = :userId AND id NOT IN (:keepIds)")
    suspend fun pruneNotificationsForUserExcept(userId: String, keepIds: List<String>)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY criadoEm DESC")
    fun notificationsForUserFlow(userId: String): Flow<List<DbNotification>>

    @Query("UPDATE notifications SET lida = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    @Query("UPDATE notifications SET lida = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    // ====================== SAVED QUOTES ======================
    @Upsert
    suspend fun upsertSavedQuotes(qs: List<SavedQuote>)

    @Query("DELETE FROM saved_quotes WHERE userId = :userId AND id NOT IN (:keepIds)")
    suspend fun pruneSavedQuotesForUserExcept(userId: String, keepIds: List<String>)

    @Query("DELETE FROM saved_quotes WHERE id = :id")
    suspend fun deleteSavedQuote(id: String)

    @Query("SELECT * FROM saved_quotes WHERE userId = :userId ORDER BY criadoEm DESC")
    fun savedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>>

    @Query("SELECT * FROM saved_quotes WHERE userId = :userId AND bookId = :bookId ORDER BY criadoEm DESC")
    fun savedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>>

    // ====================== BOOK SUMMARIES ======================
    @Upsert
    suspend fun upsertBookSummary(s: BookSummary)

    @Query("SELECT * FROM book_summaries WHERE bookId = :bookId AND clubId = :clubId LIMIT 1")
    fun bookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?>

    // ====================== BOOK RATINGS ======================
    @Upsert
    suspend fun upsertBookRatings(rs: List<BookRating>)

    @Query("DELETE FROM book_ratings WHERE bookId = :bookId AND clubId = :clubId AND userId NOT IN (:keepUserIds)")
    suspend fun pruneBookRatingsExcept(bookId: String, clubId: String, keepUserIds: List<String>)

    @Query("SELECT * FROM book_ratings WHERE bookId = :bookId AND clubId = :clubId")
    fun bookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>>

    @Query("SELECT * FROM book_ratings WHERE bookId = :bookId AND clubId = :clubId AND userId = :userId LIMIT 1")
    fun bookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?>

    // ====================== MEMBER REMOVALS ======================
    @Upsert
    suspend fun upsertMemberRemovals(rs: List<MemberRemoval>)

    @Query("SELECT * FROM member_removals WHERE clubId = :clubId ORDER BY removedAt DESC")
    fun memberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>>

    // ====================== ATOMIC REPLACES ======================
    // Quando o sync re-baixa uma lista inteira, e mais seguro fazer prune+upsert
    // em uma so transacao do que individualmente.

    @Transaction
    suspend fun replaceCommentsInChapter(chapterId: String, clubId: String, comments: List<Comment>) {
        upsertComments(comments)
        pruneCommentsInChapterExcept(chapterId, clubId, comments.map { it.id })
    }

    @Transaction
    suspend fun replaceMembersInClub(clubId: String, members: List<ClubMember>, users: List<User>) {
        upsertUsers(users)
        upsertMembers(members)
        pruneMembersInClubExcept(clubId, members.map { it.userId })
    }

    @Transaction
    suspend fun replaceClubBooksInClub(clubId: String, items: List<ClubBook>, books: List<Book>) {
        upsertBooks(books)
        upsertClubBooks(items)
        pruneClubBooksInClubExcept(clubId, items.map { it.bookId })
    }

    @Transaction
    suspend fun replaceRsvpsForMeeting(meetingId: String, rsvps: List<MeetingRsvp>) {
        upsertMeetingRsvps(rsvps)
        pruneRsvpsForMeetingExcept(meetingId, rsvps.map { it.userId })
    }

    @Transaction
    suspend fun replaceVotesInRound(roundId: String, votes: List<Vote>) {
        upsertVotes(votes)
        pruneVotesInRoundExcept(roundId, votes.map { "$roundId${it.userId}${it.clubBookId}" })
    }

    @Transaction
    suspend fun replaceNotificationsForUser(userId: String, notifs: List<DbNotification>) {
        upsertNotifications(notifs)
        pruneNotificationsForUserExcept(userId, notifs.map { it.id })
    }

    @Transaction
    suspend fun replaceSavedQuotesForUser(userId: String, qs: List<SavedQuote>) {
        upsertSavedQuotes(qs)
        pruneSavedQuotesForUserExcept(userId, qs.map { it.id })
    }

    @Transaction
    suspend fun replaceMeetingsInClub(clubId: String, ms: List<Meeting>) {
        upsertMeetings(ms)
        pruneMeetingsInClubExcept(clubId, ms.map { it.id })
    }

    @Transaction
    suspend fun replaceBookSuggestionsInClub(clubId: String, bs: List<BookSuggestion>) {
        upsertBookSuggestions(bs)
        pruneBookSuggestionsInClubExcept(clubId, bs.map { it.bookId })
    }

    @Transaction
    suspend fun replaceReactionsForComment(commentId: String, rs: List<Reaction>) {
        upsertReactions(rs)
        pruneReactionsForCommentExcept(commentId, rs.map { "${it.commentId}${it.userId}${it.emoji}" })
    }

    /** Nuke geral do cache — chamado ao trocar de usuario / logout. */
    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM clubs")
    suspend fun clearClubs()

    @Query("DELETE FROM club_members")
    suspend fun clearClubMembers()

    @Query("DELETE FROM books")
    suspend fun clearBooks()

    @Query("DELETE FROM club_books")
    suspend fun clearClubBooks()

    @Query("DELETE FROM chapters")
    suspend fun clearChapters()

    @Query("DELETE FROM user_progress")
    suspend fun clearProgress()

    @Query("DELETE FROM comments")
    suspend fun clearComments()

    @Query("DELETE FROM reactions")
    suspend fun clearReactions()

    @Query("DELETE FROM votes")
    suspend fun clearVotes()

    @Query("DELETE FROM meetings")
    suspend fun clearMeetings()

    @Query("DELETE FROM meeting_rsvps")
    suspend fun clearMeetingRsvps()

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()

    @Query("DELETE FROM saved_quotes")
    suspend fun clearSavedQuotes()

    @Query("DELETE FROM book_summaries")
    suspend fun clearBookSummaries()

    @Query("DELETE FROM book_ratings")
    suspend fun clearBookRatings()

    @Query("DELETE FROM book_suggestions")
    suspend fun clearBookSuggestions()

    @Query("DELETE FROM voting_rounds")
    suspend fun clearVotingRounds()

    @Query("DELETE FROM meeting_patterns")
    suspend fun clearMeetingPatterns()

    @Query("DELETE FROM member_removals")
    suspend fun clearMemberRemovals()

    @Query("DELETE FROM meeting_minutes")
    suspend fun clearMeetingMinutes()

    @Query("DELETE FROM meeting_notes")
    suspend fun clearMeetingNotes()

    @Transaction
    suspend fun clearAll() {
        clearReactions()
        clearComments()
        clearVotes()
        clearMeetingRsvps()
        clearMeetingNotes()
        clearMeetingMinutes()
        clearMeetings()
        clearMeetingPatterns()
        clearBookRatings()
        clearBookSummaries()
        clearBookSuggestions()
        clearVotingRounds()
        clearClubBooks()
        clearProgress()
        clearChapters()
        clearBooks()
        clearSavedQuotes()
        clearNotifications()
        clearMemberRemovals()
        clearClubMembers()
        clearClubs()
        clearUsers()
    }
}
