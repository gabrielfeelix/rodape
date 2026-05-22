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

    @Query("SELECT c.* FROM clubs c INNER JOIN club_members m ON c.id = m.clubId WHERE m.userId = :userId")
    fun getClubsForUser(userId: String): Flow<List<Club>>

    @Query("SELECT c.* FROM clubs c INNER JOIN club_members m ON c.id = m.clubId WHERE m.userId = :userId")
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
}
