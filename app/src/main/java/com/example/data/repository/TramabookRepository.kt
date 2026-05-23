package com.example.data.repository

import com.example.data.db.TramabookDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class TramabookRepository(private val dao: TramabookDao) {

    // --- Users ---
    fun getUserFlow(userId: String): Flow<User?> = dao.getUserFlow(userId)
    suspend fun getUser(userId: String): User? = dao.getUser(userId)
    fun getAllUsersFlow(): Flow<List<User>> = dao.getAllUsersFlow()
    suspend fun insertUser(user: User) = dao.insertUser(user)

    // --- Clubs ---
    fun getClubFlow(clubId: String): Flow<Club?> = dao.getClubFlow(clubId)
    suspend fun getClub(clubId: String): Club? = dao.getClub(clubId)
    suspend fun getClubByCodigo(codigo: String): Club? = dao.getClubByCodigo(codigo)
    fun getClubsForUser(userId: String): Flow<List<Club>> = dao.getClubsForUser(userId)
    suspend fun getClubsForUserList(userId: String) = dao.getClubsForUserList(userId)
    suspend fun insertClub(club: Club) = dao.insertClub(club)

    // --- Members ---
    suspend fun insertClubMember(member: ClubMember) = dao.insertClubMember(member)
    fun getClubMembersFlow(clubId: String): Flow<List<User>> = dao.getClubMembersFlow(clubId)
    suspend fun getClubMember(clubId: String, userId: String): ClubMember? = dao.getClubMember(clubId, userId)

    // --- Books ---
    suspend fun insertBook(book: Book) = dao.insertBook(book)
    suspend fun insertClubBook(clubBook: ClubBook) = dao.insertClubBook(clubBook)
    fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>> = dao.getBookByStatusFlow(clubId, status)
    fun getClubBooksFlow(clubId: String): Flow<List<Book>> = dao.getClubBooksFlow(clubId)
    suspend fun getClubBookStatus(clubId: String, bookId: String) = dao.getClubBookStatus(clubId, bookId)
    suspend fun getBook(id: String): Book? = dao.getBook(id)

    // --- Chapters ---
    suspend fun insertChapters(chapters: List<Chapter>) = dao.insertChapters(chapters)
    fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>> = dao.getChaptersForBookFlow(bookId)

    // --- User Progress ---
    suspend fun insertUserProgress(progress: UserProgress) = dao.insertUserProgress(progress)
    fun getUserProgressFlow(userId: String, clubId: String, bookId: String): Flow<UserProgress?> = dao.getUserProgressFlow(userId, clubId, bookId)
    suspend fun getUserProgress(userId: String, clubId: String, bookId: String) = dao.getUserProgress(userId, clubId, bookId)
    fun getAllProgressForClubFlow(clubId: String): Flow<List<UserProgress>> = dao.getAllProgressForClubFlow(clubId)

    // --- Comments & Reactions ---
    suspend fun insertComment(comment: Comment) = dao.insertComment(comment)
    fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>> = dao.getCommentsForChapterFlow(chapterId, clubId)
    
    suspend fun insertReaction(reaction: Reaction) = dao.insertReaction(reaction)
    suspend fun deleteReaction(reaction: Reaction) = dao.deleteReaction(reaction)
    fun getReactionsForCommentFlow(commentId: String): Flow<List<Reaction>> = dao.getReactionsForCommentFlow(commentId)
    fun getReactionsForChapterFlow(chapterId: String): Flow<List<Reaction>> = dao.getReactionsForChapterFlow(chapterId)

    // --- Votes ---
    suspend fun insertVote(vote: Vote) = dao.insertVote(vote)
    suspend fun clearVotesForUserInClub(userId: String, clubId: String) = dao.clearVotesForUserInClub(userId, clubId)
    fun getVotesForClubFlow(clubId: String): Flow<List<Vote>> = dao.getVotesForClubFlow(clubId)

    // --- Meetings ---
    suspend fun insertMeeting(meeting: Meeting) = dao.insertMeeting(meeting)
    fun getLatestMeetingFlow(clubId: String): Flow<Meeting?> = dao.getLatestMeetingFlow(clubId)
    fun getAllMeetingsFlow(clubId: String): Flow<List<Meeting>> = dao.getAllMeetingsFlow(clubId)
    suspend fun insertMeetingRsvp(rsvp: MeetingRsvp) = dao.insertMeetingRsvp(rsvp)
    fun getRsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>> = dao.getRsvpsForMeetingFlow(meetingId)
    fun getRsvpForMeetingOfUserFlow(meetingId: String, userId: String): Flow<MeetingRsvp?> = dao.getRsvpForMeetingOfUserFlow(meetingId, userId)

    // --- Notifications ---
    suspend fun insertNotification(notification: DbNotification) = dao.insertNotification(notification)
    suspend fun markAllNotificationsAsRead(userId: String) = dao.markAllNotificationsAsRead(userId)
    suspend fun markNotificationAsRead(id: String) = dao.markNotificationAsRead(id)
    fun getNotificationsFlow(userId: String): Flow<List<DbNotification>> = dao.getNotificationsFlow(userId)

    // --- Saved Quotes ---
    suspend fun insertSavedQuote(quote: SavedQuote) = dao.insertSavedQuote(quote)
    suspend fun deleteSavedQuote(quote: SavedQuote) = dao.deleteSavedQuote(quote)
    fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>> = dao.getSavedQuotesForUserFlow(userId)
    fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>> = dao.getSavedQuotesForBookFlow(userId, bookId)

    // --- Book Summaries ---
    suspend fun insertBookSummary(summary: BookSummary) = dao.insertBookSummary(summary)
    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?> = dao.getBookSummaryFlow(bookId, clubId)

    // --- Book Ratings ---
    suspend fun insertBookRating(rating: BookRating) = dao.insertBookRating(rating)
    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>> = dao.getBookRatingsFlow(bookId, clubId)
    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?> = dao.getBookRatingOfUserFlow(bookId, clubId, userId)

    // --- Book Suggestions ---
    suspend fun insertBookSuggestion(suggestion: BookSuggestion) = dao.insertBookSuggestion(suggestion)
    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?> = dao.getBookSuggestionFlow(bookId, clubId)
    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>> = dao.getBookSuggestionsForClubFlow(clubId)

    // --- Voting Rounds ---
    suspend fun insertVotingRound(round: VotingRound) = dao.insertVotingRound(round)
    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?> = dao.getActiveVotingRoundFlow(clubId)
    suspend fun getActiveVotingRound(clubId: String): VotingRound? = dao.getActiveVotingRound(clubId)
    suspend fun closeVotingRound(id: String, vencedoresJson: String) = dao.closeVotingRound(id, vencedoresJson)

    fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>> = dao.getVotesForRoundFlow(roundId)
    suspend fun getVotesForRound(roundId: String): List<Vote> = dao.getVotesForRound(roundId)
    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String) = dao.removeUserVoteForBookInRound(userId, roundId, bookId)
    suspend fun countUserVotesInRound(userId: String, roundId: String): Int = dao.countUserVotesInRound(userId, roundId)

    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String) = dao.updateClubBookStatus(clubId, bookId, status)
    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?) = dao.updateClubBookMeetingDate(clubId, bookId, dataEncontro)
    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> = dao.getClubBooksByStatusFlow(clubId, status)

    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>> = dao.getCommentsForBookFlow(bookId, clubId)

    // --- Club admin (Fase 5) ---
    suspend fun updateClubInfo(clubId: String, nome: String, descricao: String, cor: String, privacidade: String) =
        dao.updateClubInfo(clubId, nome, descricao, cor, privacidade)
    suspend fun updateClubCodigo(clubId: String, codigo: String) = dao.updateClubCodigo(clubId, codigo)
    suspend fun updateClubArquivado(clubId: String, arquivado: Boolean) = dao.updateClubArquivado(clubId, arquivado)
    fun getArchivedClubsForUserFlow(userId: String): Flow<List<Club>> = dao.getArchivedClubsForUserFlow(userId)

    // --- Member admin ---
    suspend fun updateMemberPapel(clubId: String, userId: String, papel: String) =
        dao.updateMemberPapel(clubId, userId, papel)
    suspend fun deleteClubMember(clubId: String, userId: String) = dao.deleteClubMember(clubId, userId)
    suspend fun getClubMembersListOrderedByJoin(clubId: String): List<ClubMember> =
        dao.getClubMembersListOrderedByJoin(clubId)
    fun getClubMembersRawFlow(clubId: String): Flow<List<ClubMember>> = dao.getClubMembersRawFlow(clubId)
    suspend fun insertMemberRemoval(removal: MemberRemoval) = dao.insertMemberRemoval(removal)
    fun getMemberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>> =
        dao.getMemberRemovalsForClubFlow(clubId)

    // --- Meeting pattern ---
    suspend fun insertMeetingPattern(pattern: MeetingPattern) = dao.insertMeetingPattern(pattern)
    fun getActiveMeetingPatternFlow(clubId: String): Flow<MeetingPattern?> =
        dao.getActiveMeetingPatternFlow(clubId)
    suspend fun getActiveMeetingPattern(clubId: String): MeetingPattern? = dao.getActiveMeetingPattern(clubId)
    suspend fun deactivateMeetingPatterns(clubId: String) = dao.deactivateMeetingPatterns(clubId)

    // --- Meeting CRUD ---
    suspend fun deleteMeeting(meetingId: String) = dao.deleteMeeting(meetingId)
    suspend fun deleteRsvpsForMeeting(meetingId: String) = dao.deleteRsvpsForMeeting(meetingId)

    // --- Comment moderation ---
    suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String) =
        dao.softRemoveComment(commentId, removidoPor, motivo)
    suspend fun restoreComment(commentId: String) = dao.restoreComment(commentId)
    fun getRemovedCommentsForClubFlow(clubId: String): Flow<List<Comment>> =
        dao.getRemovedCommentsForClubFlow(clubId)

    // --- Chapters CRUD ---
    suspend fun deleteChaptersForBook(bookId: String) = dao.deleteChaptersForBook(bookId)

    // --- Suggestion delete ---
    suspend fun deleteClubBook(clubId: String, bookId: String) = dao.deleteClubBook(clubId, bookId)
    suspend fun deleteBookSuggestion(bookId: String, clubId: String) = dao.deleteBookSuggestion(bookId, clubId)
    suspend fun deleteVotesForBook(bookId: String) = dao.deleteVotesForBook(bookId)

    suspend fun seedDatabase() {
        // Only seed if no clubs exist
        val list = dao.getClubsForUserList("user_voce")
        if (list.isNotEmpty()) return

        // 1. Insert default users (Perfect matching of screenshots profiles/avatars/names)
        val voceUser = User(
            "user_voce", 
            "Beatriz Almeida", 
            "bla.almeida@gmail.com", 
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCVScro7b5L7FyxSBjNpeqetGOxXZcJe5_EViRuBb5j15OIqZzjjFE8AD5HxgnDcV__koM3NJtsawXA84KY9YNkGFN7fhPvCmJozzDXIkaDWzjObrvzqA2QOSHYCkvK6No2M6UEtsJXEoOaqY7O0WDiVtrhyaKZIqMxGEdP732KB_qtc7_tWeZHNZ9WEOJp6PTJnWMO-kidNZ_0LEvCMirIjMy140n059Elt4YwhfPZbjqKivR3NRgIsXyLxp8THGS41Y3roxiIJS8"
        )
        val marina = User(
            "user_marina", 
            "Marina", 
            "marina@tramabook.com", 
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDNrQIc-uks79AhvG8KFuLCYcl4DjJUuM52Y5AgBip1lo6uzcbLjEM_b6FRtPOCWtOvRmxCauZY8HUNXHYcxqAq8Ru_uEjttmNSr0hkhe3nGSMc9eKCInzWUUVBjFXu661dJly69n_621424sKASXlLy4qZl02CkvL5jHSh4Ugw0NVyiAQqJZEO1FSQDuAcxYM0uNvorusQRgslEly3FypUo7Gh2-xJdH48tz3k1156ZbBA7wshhsw0aiYXoxM-FkWs649mS3THXPo"
        )
        val rafael = User(
            "user_lucas", 
            "Rafael", 
            "rafael@tramabook.com", 
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCZNr2l26igYmRuuPixuo5CSHO1NRgXQjv3z1YeNdPCLPKjS22ay2o4Miv2f79xlBorMuVfPa1OR-NfGojrnPL7eIwFtOcgwodZ9alpIYkPvKmwS56lM0eC1uFEeukUG5OP_aRVaQWVjmHuoXkk0FA7UR3KE-pREvpXywe-4Y90gtdeitqLEXY3j5CldPWEfjyyYGgaYdKilR5HyLMZTNzlNOR_yMGF2ay_P828oiif7padxTiFKgIYlkpQZpU43wuzwAwp3Xu6iQw"
        )
        val julia = User(
            "user_sofia", 
            "Júlia", 
            "julia@tramabook.com", 
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDmRnrAWaFr7Wx-eXV7PJHdL-IIO1hVWCZ9_j4iHEGuBJ5l771EGpSy5KIkJjGjBG8qjNVxScA2rX1Rmf5mvqI0kh_sGIqmkh1Z1mg4UrGr2MaVmbuEeZqgC6e-y5OBR5ZM0HNEOhUgbWA964sjj8t2iASjEQj3NzdwpwGKE59VqCwdW0DOQZGxO6Ie20T3I_bfSuCihVTOhH6meMGLjWf6paH0fXDOm4w96aQnlLIYJh4EpSOSp35kR5QYlnyG63Vpvu4KTjYLn8c"
        )
        val leo = User(
            "user_bia", 
            "Leo", 
            "leo@tramabook.com", 
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDMijlzVy38WV-sXZNMrfRzKg80WzVzet0p_xPl50BKJlm8Tkq83LzxgPdQef-GP91ARXui7cKs_rLe1gvWL1qDBFyNkZbeFniZyfV6YKjKVIFqSYHmczGRCzd_Ryxq2ApR-W8_obaScXrmMi9bmeflGXiOVKe22Ur_IcyUwtVLar_mEKKs16uaWoCgNCeeiQfoiIlG4aslg8AGW0aLKQqXQofQRiZV7nJJ-RwGgfQva9bgDeugJ7W3-LDPVpcS8mF_KTEDo7OTHc8"
        )
        val joao = User(
            "user_joao", 
            "João", 
            "joao@tramabook.com", 
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAbEfpJxbUBzrGk8x1LLdzNG0I3py8eInEEyWmrQvjDGvTtVA7thIUCs6UY4jrgXkv559Vq6PFwIxnyz5RLmakmuALUHqvk5usJUkjqR1MHD56C9hf1iL2YmWWZZOPK1wdoKj-uIK5kCrP5EeGO0YtpMMGq1AJlcmJ2UoEAa_o0k9b_vL5QnMDyKfse4CUron9X8WT4636vfK-219kvqoS_ptvPkLmDpNQAKNrUJR0Q-nxZxCAJjCq-JJjCdXF-xmksNN0oFVdUvXI"
        )

        dao.insertUser(voceUser)
        dao.insertUser(marina)
        dao.insertUser(rafael)
        dao.insertUser(julia)
        dao.insertUser(leo)
        dao.insertUser(joao)

        // 2. Insert Club
        val clubMari = Club(
            id = "club_mari",
            nome = "Leituras de domingo",
            descricao = "Um clubinho clássico de leitura íntima para tomar vinho e conversar livremente sobre livros excelentes.",
            codigo = "XK7M2P",
            cor = "0", // terracota selection
            privacidade = "convidados",
            criadorId = "user_marina",
            criadoEm = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L,
            arquivado = false
        )
        dao.insertClub(clubMari)

        // Club Members
        dao.insertClubMember(ClubMember("club_mari", "user_marina", "super_admin", System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L))
        dao.insertClubMember(ClubMember("club_mari", "user_voce", "admin", System.currentTimeMillis() - 28 * 24 * 60 * 60 * 1000L))
        dao.insertClubMember(ClubMember("club_mari", "user_lucas", "member", System.currentTimeMillis() - 28 * 24 * 60 * 60 * 1000L))
        dao.insertClubMember(ClubMember("club_mari", "user_sofia", "member", System.currentTimeMillis() - 27 * 24 * 60 * 60 * 1000L))
        dao.insertClubMember(ClubMember("club_mari", "user_bia", "member", System.currentTimeMillis() - 26 * 24 * 60 * 60 * 1000L))
        dao.insertClubMember(ClubMember("club_mari", "user_joao", "member", System.currentTimeMillis() - 25 * 24 * 60 * 60 * 1000L))

        // 3. Current Book (Clarice Lispector - A Hora da Estrela)
        val metamorfose = Book(
            id = "book_metamorfose",
            title = "A Hora da Estrela",
            author = "Clarice Lispector",
            coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBhPfzTZmY3wRWCswfxpQpV6cnRVtvVQ_clAZ6aRrJwLbSLuuZAznGJWsV8tDhLo1qLI2EKURf-YLHuE5DYluQkjenJqOXMOYVc6BjKHn0KTeuuLg-MnrtT5jEQjgTOmzqdrVd7ae-tzaaCEcIDCentBoirhdfx1PnyHFd7TvarUjbu6_7fTxwE3qxK1rnuTXnYg5vbeIeYj3PLQIO7wV2WPu-Ee1hjpYyAgTA-L9i6pt9R9aM5jH1F1sUeSPOdTV-zhuUxOgCWPB0",
            openlibraryId = "OL26214150M",
            isbn = "9788535911121"
        )
        dao.insertBook(metamorfose)
        dao.insertClubBook(ClubBook("club_mari", "book_metamorfose", "current", 1, null))

        // 13 chapters
        val chs = listOf(
            Chapter("ch_1", "book_metamorfose", 1, "A culpa é minha"),
            Chapter("ch_2", "book_metamorfose", 2, "Deixa eu pelo menos pensar"),
            Chapter("ch_3", "book_metamorfose", 3, "Ela não sabia nada"),
            Chapter("ch_4", "book_metamorfose", 4, "A assofiação do café"),
            Chapter("ch_5", "book_metamorfose", 5, "Uma noiva fria"),
            Chapter("ch_6", "book_metamorfose", 6, "A raiva reprimida"),
            Chapter("ch_7", "book_metamorfose", 7, "Encontro com Olímpico"),
            Chapter("ch_8", "book_metamorfose", 8, "Vidente de mentira"),
            Chapter("ch_9", "book_metamorfose", 9, "O estrondo do carro"),
            Chapter("ch_10", "book_metamorfose", 10, "A melodia muda"),
            Chapter("ch_11", "book_metamorfose", 11, "A exaustão de viver"),
            Chapter("ch_12", "book_metamorfose", 12, "Um corpo leve"),
            Chapter("ch_13", "book_metamorfose", 13, "O fim estelar")
        )
        dao.insertChapters(chs)

        // Progresses matching screenshot exactly (Marina=9, Rafael=8, Júlia=Terminou/13, Leo=8, Beatriz/user_voce=8)
        dao.insertUserProgress(UserProgress("user_voce", "club_mari", "book_metamorfose", 8))
        dao.insertUserProgress(UserProgress("user_marina", "club_mari", "book_metamorfose", 9))
        dao.insertUserProgress(UserProgress("user_lucas", "club_mari", "book_metamorfose", 8))
        dao.insertUserProgress(UserProgress("user_sofia", "club_mari", "book_metamorfose", 13)) // Finished!
        dao.insertUserProgress(UserProgress("user_bia", "club_mari", "book_metamorfose", 8))
        dao.insertUserProgress(UserProgress("user_joao", "club_mari", "book_metamorfose", 1))

        // 4. Chapter 7 comments matching screenshot exactly!
        val c1 = Comment("c_1", "ch_7", "club_mari", "user_marina", "Esse capítulo me deixou com um nó. A vidente fala umas coisas que a Macabéa nem entende direito, mas a gente sente o peso de cada uma. Clarice é cruel e doce ao mesmo tempo.", System.currentTimeMillis() - 2 * 3600 * 1000, false, null, null)
        val c2 = Comment("c_2", "ch_7", "club_mari", "user_voce", "Concordo, Marina. Tinha lido um trecho desse capítulo solto e não tinha entendido nada. No contexto inteiro faz muito mais sentido — sobretudo a previsão do estrangeiro.", System.currentTimeMillis() - 1 * 3600 * 1000, false, null, null)
        val c3 = Comment("c_3", "ch_7", "club_mari", "user_lucas", "Reparem como o narrador (Rodrigo SM) some quase por completo nessa parte. É como se ele deixasse a Macabéa respirar sozinha por uma página inteira.", System.currentTimeMillis() - 30 * 60 * 1000, false, null, null)

        dao.insertComment(c1)
        dao.insertComment(c2)
        dao.insertComment(c3)

        dao.insertReaction(Reaction("c_1", "user_lucas", "❤️"))
        dao.insertReaction(Reaction("c_1", "user_sofia", "🤯"))
        dao.insertReaction(Reaction("c_1", "user_bia", "✨"))
        dao.insertReaction(Reaction("c_2", "user_marina", "❤️"))
        dao.insertReaction(Reaction("c_2", "user_sofia", "✨"))
        dao.insertReaction(Reaction("c_3", "user_marina", "❤️"))
        dao.insertReaction(Reaction("c_3", "user_bia", "🤯"))

        // Chapter 1 comments
        val c4 = Comment("c_4", "ch_1", "club_mari", "user_marina", "Gregor Samsa acordando transformado em inseto gigante é possivelmente um dos começos de narrativa mais impressionantes que já li na vida. O absurdo é tratado com tanta naturalidade!", System.currentTimeMillis() - 5 * 24 * 3600 * 1000, false, null, null)
        val c5 = Comment("c_5", "ch_1", "club_mari", "user_voce", "Eu achei um tanto claustrofóbico. A descrição dele tentando virar o corpo pesado de concha de um lado para o outro na cama te faz se sentir meio preso também. Muito imersivo.", System.currentTimeMillis() - 4 * 24 * 3600 * 1000, false, null, null)
        dao.insertComment(c4)
        dao.insertComment(c5)

        // 5. Future meeting
        val meeting = Meeting(
            id = "meet_1",
            clubId = "club_mari",
            data = "DOMINGO, 24 DE OUTUBRO",
            hora = "19:00 — 21:00",
            local = "Café Lispector, Vila Madalena",
            agenda = "Discussão: A Hora da Estrela"
        )
        dao.insertMeeting(meeting)

        dao.insertMeetingRsvp(MeetingRsvp("meet_1", "user_marina", "Vou"))
        dao.insertMeetingRsvp(MeetingRsvp("meet_1", "user_lucas", "Vou"))
        dao.insertMeetingRsvp(MeetingRsvp("meet_1", "user_voce", "Vou"))
        dao.insertMeetingRsvp(MeetingRsvp("meet_1", "user_sofia", "Vou"))
        dao.insertMeetingRsvp(MeetingRsvp("meet_1", "user_bia", "Talvez"))
        dao.insertMeetingRsvp(MeetingRsvp("meet_1", "user_joao", "Não vou"))

        // Padrão de encontros: domingos 19h
        dao.insertMeetingPattern(
            MeetingPattern(
                id = "pattern_mari",
                clubId = "club_mari",
                diaSemana = java.util.Calendar.SUNDAY,
                hora = "19:00",
                local = "Café Lispector, Vila Madalena",
                agendaTemplate = "Discussão do livro atual",
                ativo = true
            )
        )

        // 6. Suggestions matching screenshot exactly
        val bookEstravagante = Book("sug_1", "Becos da memória", "Conceição Evaristo", "https://covers.openlibrary.org/b/id/8359489-M.jpg", "", "")
        val bookWoolf = Book("sug_2", "O quinze", "Rachel de Queiroz", "https://covers.openlibrary.org/b/id/10531551-M.jpg", "", "")
        val bookTimeMachine = Book("sug_3", "A Máquina do Tempo", "H.G. Wells", "https://lh3.googleusercontent.com/aida-public/AB6AXuA30qDj_uGAutNzuhJf1UoXnERY2860xqj5zCrqAYOU2b2zdgvtxIdZGmYqHceK4nnTiYy5WZkVaXZQV25jfbIazcL96ywdPqJtL3AnikceDzM3FX3BxCB_KEBhlg6CRsdbLO0RQr8JcD7M8qTguP83hrTC2kPonrWNJlbx227ZwPU3hMFK83Q1453Tf6w967QAQmzfJTqBjrtfI-gNUdLG9EK1j0NsrKvoBVc5X1TDJswZ5fNU6xSM_YB4JfnzZA166xiLu1iIYvA", "", "")

        dao.insertBook(bookEstravagante)
        dao.insertBook(bookWoolf)
        dao.insertBook(bookTimeMachine)

        dao.insertClubBook(ClubBook("club_mari", "sug_1", "suggested", 1, null))
        dao.insertClubBook(ClubBook("club_mari", "sug_2", "suggested", 2, null))
        dao.insertClubBook(ClubBook("club_mari", "sug_3", "suggested", 3, null))

        // Votes
        dao.insertVote(Vote("sug_1", "user_marina", System.currentTimeMillis(), "round_mari_1"))
        dao.insertVote(Vote("sug_1", "user_lucas", System.currentTimeMillis(), "round_mari_1"))
        dao.insertVote(Vote("sug_1", "user_sofia", System.currentTimeMillis(), "round_mari_1"))
        dao.insertVote(Vote("sug_2", "user_bia", System.currentTimeMillis(), "round_mari_1"))
        dao.insertVote(Vote("sug_2", "user_joao", System.currentTimeMillis(), "round_mari_1"))

        // Voting round ativa (7 dias à frente, N=1, cadência única)
        val agora = System.currentTimeMillis()
        val seteDias = 7L * 24 * 60 * 60 * 1000L
        val umMes = 30L * 24 * 60 * 60 * 1000L
        dao.insertVotingRound(
            VotingRound(
                id = "round_mari_1",
                clubId = "club_mari",
                criadoPor = "user_marina",
                abertaEm = agora,
                fechaEm = agora + seteDias,
                nLivros = 1,
                cadencia = "unica",
                status = "aberta",
                vencedoresJson = "[]"
            )
        )

        // Justificativas reais persistidas pra cada sugestão
        dao.insertBookSuggestion(
            BookSuggestion(
                id = "bs_1",
                clubId = "club_mari",
                bookId = "sug_1",
                suggestedByUserId = "user_marina",
                justificativa = "Tava querendo um livro brasileiro contemporâneo. Conceição Evaristo escreve memória e afeto como ninguém.",
                criadoEm = agora - 2 * 24 * 60 * 60 * 1000L
            )
        )
        dao.insertBookSuggestion(
            BookSuggestion(
                id = "bs_2",
                clubId = "club_mari",
                bookId = "sug_2",
                suggestedByUserId = "user_bia",
                justificativa = "Já é momento de revisitar Rachel de Queiroz — leitura curta e necessária.",
                criadoEm = agora - 1 * 24 * 60 * 60 * 1000L
            )
        )
        dao.insertBookSuggestion(
            BookSuggestion(
                id = "bs_3",
                clubId = "club_mari",
                bookId = "sug_3",
                suggestedByUserId = "user_lucas",
                justificativa = "Queria algo mais leve depois da intensidade da Clarice. Ficção científica clássica.",
                criadoEm = agora - 3 * 60 * 60 * 1000L
            )
        )

        // 7. Finished books (Estante) matching screenshot exactly
        val bFinished1 = Book("fin_1", "Olhos d'água", "Conceição Evaristo", "https://covers.openlibrary.org/b/id/8359489-M.jpg", "", "")
        val bFinished2 = Book("fin_2", "Pedro Páramo", "Juan Rulfo", "https://covers.openlibrary.org/b/id/11105432-M.jpg", "", "")
        val bFinished3 = Book("fin_3", "Torto arado", "Itamar Vieira Junior", "https://covers.openlibrary.org/b/id/12865231-M.jpg", "", "")

        dao.insertBook(bFinished1)
        dao.insertBook(bFinished2)
        dao.insertBook(bFinished3)

        dao.insertClubBook(ClubBook("club_mari", "fin_1", "finished", 1, agora - 3 * umMes))
        dao.insertClubBook(ClubBook("club_mari", "fin_2", "finished", 2, agora - 2 * umMes))
        dao.insertClubBook(ClubBook("club_mari", "fin_3", "finished", 3, agora - 1 * umMes))

        // Resumos wiki dos finished
        dao.insertBookSummary(
            BookSummary(
                bookId = "fin_1",
                clubId = "club_mari",
                texto = "Coletânea de contos da Conceição Evaristo. Cada conto é um soco no estômago — mulheres negras periféricas em situações que a gente prefere não ver. A escrita é direta mas atravessada de poesia. A discussão do clube ficou centrada em \"Maria\" e \"Ana Davenga\".",
                lastEditorId = "user_marina",
                updatedAt = agora - 5 * 24 * 60 * 60 * 1000L
            )
        )
        dao.insertBookSummary(
            BookSummary(
                bookId = "fin_3",
                clubId = "club_mari",
                texto = "Romance que ganhou o Jabuti em 2020. Duas irmãs no sertão da Chapada Diamantina herdam uma faca e uma língua. Itamar Vieira Junior constrói uma narrativa em três partes que vai do mítico ao histórico sem perder o chão.",
                lastEditorId = "user_voce",
                updatedAt = agora - 12 * 24 * 60 * 60 * 1000L
            )
        )

        // Avaliações dos finished
        dao.insertBookRating(BookRating("fin_1", "club_mari", "user_marina", 5, "Conceição é incontornável.", agora - 5 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_1", "club_mari", "user_voce", 5, "", agora - 4 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_1", "club_mari", "user_sofia", 4, "Achei intenso demais às vezes, mas brilhante.", agora - 3 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_2", "club_mari", "user_marina", 4, "", agora - 10 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_2", "club_mari", "user_lucas", 3, "Não conectei muito.", agora - 9 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_3", "club_mari", "user_voce", 5, "Um dos melhores livros que já li.", agora - 12 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_3", "club_mari", "user_marina", 5, "Sim.", agora - 11 * 24 * 60 * 60 * 1000L))
        dao.insertBookRating(BookRating("fin_3", "club_mari", "user_bia", 4, "", agora - 10 * 24 * 60 * 60 * 1000L))

        // 8. Visual notifications List
        dao.insertNotification(DbNotification("not_1", "user_voce", "club_mari", "comment_on_chapter", "{\"chapterId\":\"ch_7\",\"chapterTitle\":\"Encontro com Olímpico\",\"userName\":\"Marina\"}", false, System.currentTimeMillis() - 1 * 3600 * 1000))
        dao.insertNotification(DbNotification("not_2", "user_voce", "club_mari", "next_book_decided", "{\"bookTitle\":\"Torto arado\",\"clubName\":\"Leituras de domingo\"}", false, System.currentTimeMillis() - 5 * 3600 * 1000))
        dao.insertNotification(DbNotification("not_3", "user_voce", "club_mari", "meeting_reminder", "{\"meetingId\":\"meet_1\",\"date\":\"DOMINGO, 24 DE OUTUBRO\"}", false, System.currentTimeMillis() - 1 * 24 * 3600 * 1000))
        dao.insertNotification(DbNotification("not_4", "user_voce", "club_mari", "member_finished", "{\"bookTitle\":\"O Pequeno Príncipe\",\"userName\":\"Ana\"}", true, System.currentTimeMillis() - 2 * 24 * 3600 * 1000))
        dao.insertNotification(DbNotification("not_5", "user_voce", "club_mari", "comment_on_chapter", "{\"chapterId\":\"ch_3\",\"chapterTitle\":\"A partida\",\"userName\":\"Lucas T.\"}", true, System.currentTimeMillis() - 3 * 24 * 3600 * 1000))

        // 9. Frases guardadas de exemplo
        dao.insertSavedQuote(SavedQuote("quote_seed_1", "user_voce", "club_mari", "book_metamorfose",
            "A culpa é minha, dizia Macabéa sem saber bem de quê.", "Cap. 1", System.currentTimeMillis() - 6 * 24 * 3600 * 1000L))
        dao.insertSavedQuote(SavedQuote("quote_seed_2", "user_voce", "club_mari", "book_metamorfose",
            "Ela acreditava em anjos e, porque acreditava, eles existiam.", "Cap. 7", System.currentTimeMillis() - 3 * 24 * 3600 * 1000L))
        dao.insertSavedQuote(SavedQuote("quote_seed_3", "user_voce", "club_mari", "fin_3",
            "A terra é a nossa carta de alforria sem assinatura.", "Torto arado", System.currentTimeMillis() - 12 * 24 * 3600 * 1000L))
        dao.insertSavedQuote(SavedQuote("quote_seed_4", "user_voce", "club_mari", "fin_1",
            "O choro de minha mãe não desaguava em rio. Empoçava nos olhos.", "Olhos d'água", System.currentTimeMillis() - 20 * 24 * 3600 * 1000L))
    }
}
