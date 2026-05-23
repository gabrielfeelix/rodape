package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TramabookDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserFlow(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): User?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // --- Clubs ---
    @Query("SELECT * FROM clubs WHERE id = :clubId")
    fun getClubFlow(clubId: String): Flow<Club?>

    @Query("SELECT * FROM clubs WHERE id = :clubId")
    suspend fun getClub(clubId: String): Club?

    @Query("SELECT * FROM clubs WHERE codigo = :codigo")
    suspend fun getClubByCodigo(codigo: String): Club?

    @Query("SELECT c.* FROM clubs c INNER JOIN club_members m ON c.id = m.clubId WHERE m.userId = :userId AND c.arquivado = 0")
    fun getClubsForUser(userId: String): Flow<List<Club>>

    @Query("SELECT c.* FROM clubs c INNER JOIN club_members m ON c.id = m.clubId WHERE m.userId = :userId AND c.arquivado = 0")
    suspend fun getClubsForUserList(userId: String): List<Club>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClub(club: Club)

    // --- Club Members ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClubMember(member: ClubMember)

    @Query("SELECT u.* FROM users u INNER JOIN club_members m ON u.id = m.userId WHERE m.clubId = :clubId")
    fun getClubMembersFlow(clubId: String): Flow<List<User>>

    @Query("SELECT * FROM club_members WHERE clubId = :clubId AND userId = :userId")
    suspend fun getClubMember(clubId: String, userId: String): ClubMember?

    // --- Books ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClubBook(clubBook: ClubBook)

    @Query("SELECT b.* FROM books b INNER JOIN club_books cb ON b.id = cb.bookId WHERE cb.clubId = :clubId AND cb.status = :status")
    fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>>

    @Query("SELECT b.* FROM books b INNER JOIN club_books cb ON b.id = cb.bookId WHERE cb.clubId = :clubId")
    fun getClubBooksFlow(clubId: String): Flow<List<Book>>

    @Query("SELECT cb.status FROM club_books cb WHERE cb.clubId = :clubId AND cb.bookId = :bookId")
    suspend fun getClubBookStatus(clubId: String, bookId: String): String?

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: String): Book?

    // --- Chapters ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY numero ASC")
    fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>>

    // --- User Progress ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProgress(progress: UserProgress)

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND clubId = :clubId AND bookId = :bookId")
    fun getUserProgressFlow(userId: String, clubId: String, bookId: String): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND clubId = :clubId AND bookId = :bookId")
    suspend fun getUserProgress(userId: String, clubId: String, bookId: String): UserProgress?

    @Query("SELECT up.* FROM user_progress up WHERE up.clubId = :clubId")
    fun getAllProgressForClubFlow(clubId: String): Flow<List<UserProgress>>

    // --- Comments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)

    @Query("SELECT * FROM comments WHERE chapterId = :chapterId AND clubId = :clubId ORDER BY criadoEm ASC")
    fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>>

    // --- Reactions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: Reaction)

    @Delete
    suspend fun deleteReaction(reaction: Reaction)

    @Query("SELECT * FROM reactions WHERE commentId = :commentId")
    fun getReactionsForCommentFlow(commentId: String): Flow<List<Reaction>>

    @Query("SELECT * FROM reactions WHERE commentId IN (SELECT id FROM comments WHERE chapterId = :chapterId)")
    fun getReactionsForChapterFlow(chapterId: String): Flow<List<Reaction>>

    // --- Votes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: Vote)

    @Query("DELETE FROM votes WHERE userId = :userId AND clubBookId IN (SELECT bookId FROM club_books WHERE clubId = :clubId)")
    suspend fun clearVotesForUserInClub(userId: String, clubId: String)

    @Query("SELECT * FROM votes WHERE clubBookId IN (SELECT bookId FROM club_books WHERE clubId = :clubId)")
    fun getVotesForClubFlow(clubId: String): Flow<List<Vote>>

    // --- Meetings & RSVP ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: Meeting)

    @Query("SELECT * FROM meetings WHERE clubId = :clubId ORDER BY id DESC LIMIT 1")
    fun getLatestMeetingFlow(clubId: String): Flow<Meeting?>

    @Query("SELECT * FROM meetings WHERE clubId = :clubId ORDER BY id DESC")
    fun getAllMeetingsFlow(clubId: String): Flow<List<Meeting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetingRsvp(rsvp: MeetingRsvp)

    @Query("SELECT * FROM meeting_rsvps WHERE meetingId = :meetingId")
    fun getRsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>>

    @Query("SELECT * FROM meeting_rsvps WHERE meetingId = :meetingId AND userId = :userId")
    fun getRsvpForMeetingOfUserFlow(meetingId: String, userId: String): Flow<MeetingRsvp?>

    // --- Notifications ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: DbNotification)

    @Query("UPDATE notifications SET lida = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: String)

    @Query("UPDATE notifications SET lida = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY criadoEm DESC")
    fun getNotificationsFlow(userId: String): Flow<List<DbNotification>>

    // --- Saved Quotes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedQuote(quote: SavedQuote)

    @Delete
    suspend fun deleteSavedQuote(quote: SavedQuote)

    @Query("SELECT * FROM saved_quotes WHERE userId = :userId ORDER BY criadoEm DESC")
    fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>>

    @Query("SELECT * FROM saved_quotes WHERE userId = :userId AND bookId = :bookId ORDER BY criadoEm DESC")
    fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>>

    // --- Book Summaries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSummary(summary: BookSummary)

    @Query("SELECT * FROM book_summaries WHERE bookId = :bookId AND clubId = :clubId LIMIT 1")
    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?>

    // --- Book Ratings ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookRating(rating: BookRating)

    @Query("SELECT * FROM book_ratings WHERE bookId = :bookId AND clubId = :clubId")
    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>>

    @Query("SELECT * FROM book_ratings WHERE bookId = :bookId AND clubId = :clubId AND userId = :userId LIMIT 1")
    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?>

    // --- Book Suggestions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookSuggestion(suggestion: BookSuggestion)

    @Query("SELECT * FROM book_suggestions WHERE bookId = :bookId AND clubId = :clubId LIMIT 1")
    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?>

    @Query("SELECT * FROM book_suggestions WHERE clubId = :clubId")
    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>>

    // --- Voting Rounds ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVotingRound(round: VotingRound)

    @Query("SELECT * FROM voting_rounds WHERE clubId = :clubId AND status = 'aberta' ORDER BY abertaEm DESC LIMIT 1")
    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?>

    @Query("SELECT * FROM voting_rounds WHERE clubId = :clubId AND status = 'aberta' ORDER BY abertaEm DESC LIMIT 1")
    suspend fun getActiveVotingRound(clubId: String): VotingRound?

    @Query("UPDATE voting_rounds SET status = 'fechada', vencedoresJson = :vencedoresJson WHERE id = :id")
    suspend fun closeVotingRound(id: String, vencedoresJson: String)

    // --- Votes (extra queries pra rodadas) ---
    @Query("SELECT * FROM votes WHERE votingRoundId = :roundId")
    fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>>

    @Query("SELECT * FROM votes WHERE votingRoundId = :roundId")
    suspend fun getVotesForRound(roundId: String): List<Vote>

    @Query("DELETE FROM votes WHERE userId = :userId AND votingRoundId = :roundId AND clubBookId = :bookId")
    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String)

    @Query("SELECT COUNT(*) FROM votes WHERE userId = :userId AND votingRoundId = :roundId")
    suspend fun countUserVotesInRound(userId: String, roundId: String): Int

    // --- ClubBook update ---
    @Query("UPDATE club_books SET status = :status WHERE clubId = :clubId AND bookId = :bookId")
    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String)

    @Query("UPDATE club_books SET dataEncontro = :dataEncontro WHERE clubId = :clubId AND bookId = :bookId")
    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?)

    @Query("SELECT * FROM club_books WHERE clubId = :clubId AND status = :status")
    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>>

    // --- Comments por livro (Chat retrospectivo) ---
    @Query("""
        SELECT c.* FROM comments c
        INNER JOIN chapters ch ON c.chapterId = ch.id
        WHERE ch.bookId = :bookId AND c.clubId = :clubId
        ORDER BY ch.numero ASC, c.criadoEm ASC
    """)
    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>>

    // --- Club admin actions (Fase 5) ---
    @Query("UPDATE clubs SET nome = :nome, descricao = :descricao, cor = :cor, privacidade = :privacidade WHERE id = :clubId")
    suspend fun updateClubInfo(clubId: String, nome: String, descricao: String, cor: String, privacidade: String)

    @Query("UPDATE clubs SET codigo = :codigo WHERE id = :clubId")
    suspend fun updateClubCodigo(clubId: String, codigo: String)

    @Query("UPDATE clubs SET arquivado = :arquivado WHERE id = :clubId")
    suspend fun updateClubArquivado(clubId: String, arquivado: Boolean)

    @Query("SELECT * FROM clubs WHERE arquivado = 1 AND id IN (SELECT clubId FROM club_members WHERE userId = :userId)")
    fun getArchivedClubsForUserFlow(userId: String): Flow<List<Club>>

    // --- Member admin actions ---
    @Query("UPDATE club_members SET papel = :papel WHERE clubId = :clubId AND userId = :userId")
    suspend fun updateMemberPapel(clubId: String, userId: String, papel: String)

    @Query("DELETE FROM club_members WHERE clubId = :clubId AND userId = :userId")
    suspend fun deleteClubMember(clubId: String, userId: String)

    @Query("SELECT * FROM club_members WHERE clubId = :clubId ORDER BY entrouEm ASC")
    suspend fun getClubMembersListOrderedByJoin(clubId: String): List<ClubMember>

    @Query("SELECT * FROM club_members WHERE clubId = :clubId")
    fun getClubMembersRawFlow(clubId: String): Flow<List<ClubMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberRemoval(removal: MemberRemoval)

    @Query("SELECT * FROM member_removals WHERE clubId = :clubId ORDER BY removedAt DESC")
    fun getMemberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>>

    // --- Meeting pattern ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetingPattern(pattern: MeetingPattern)

    @Query("SELECT * FROM meeting_patterns WHERE clubId = :clubId AND ativo = 1 LIMIT 1")
    fun getActiveMeetingPatternFlow(clubId: String): Flow<MeetingPattern?>

    @Query("SELECT * FROM meeting_patterns WHERE clubId = :clubId AND ativo = 1 LIMIT 1")
    suspend fun getActiveMeetingPattern(clubId: String): MeetingPattern?

    @Query("UPDATE meeting_patterns SET ativo = 0 WHERE clubId = :clubId")
    suspend fun deactivateMeetingPatterns(clubId: String)

    // --- Meeting CRUD extra ---
    @Query("DELETE FROM meetings WHERE id = :meetingId")
    suspend fun deleteMeeting(meetingId: String)

    @Query("DELETE FROM meeting_rsvps WHERE meetingId = :meetingId")
    suspend fun deleteRsvpsForMeeting(meetingId: String)

    // --- Comment moderation ---
    @Query("UPDATE comments SET removido = 1, removidoPor = :removidoPor, motivoRemocao = :motivo WHERE id = :commentId")
    suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String)

    @Query("UPDATE comments SET removido = 0, removidoPor = NULL, motivoRemocao = NULL WHERE id = :commentId")
    suspend fun restoreComment(commentId: String)

    @Query("SELECT * FROM comments WHERE clubId = :clubId AND removido = 1 ORDER BY criadoEm DESC")
    fun getRemovedCommentsForClubFlow(clubId: String): Flow<List<Comment>>

    // --- Chapters CRUD extra ---
    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersForBook(bookId: String)

    // --- Suggestion delete ---
    @Query("DELETE FROM club_books WHERE clubId = :clubId AND bookId = :bookId")
    suspend fun deleteClubBook(clubId: String, bookId: String)

    @Query("DELETE FROM book_suggestions WHERE bookId = :bookId AND clubId = :clubId")
    suspend fun deleteBookSuggestion(bookId: String, clubId: String)

    @Query("DELETE FROM votes WHERE clubBookId = :bookId")
    suspend fun deleteVotesForBook(bookId: String)
}
