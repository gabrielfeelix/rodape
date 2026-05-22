package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DataStoreManager
import com.example.data.api.OpenLibraryApi
import com.example.data.api.OpenLibraryDoc
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.TramabookRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TramabookRepository(database.tramabookDao())
    private val dataStoreManager = DataStoreManager(application)

    // Session state
    val currentUserId: StateFlow<String?> = dataStoreManager.userIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentUser: StateFlow<User?> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getUserFlow(userId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userName: StateFlow<String?> = dataStoreManager.userNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userEmail: StateFlow<String?> = dataStoreManager.userEmailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeClubId: StateFlow<String?> = dataStoreManager.activeClubIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Clubs
    val allClubs: StateFlow<List<Club>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getClubsForUser(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeClub: StateFlow<Club?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getClubFlow(clubId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentBooksMap: StateFlow<Map<String, String>> = currentUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyMap())
        } else {
            repository.getClubsForUser(userId).flatMapLatest { clubs ->
                if (clubs.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val flowsList = clubs.map { club ->
                        repository.getBookByStatusFlow(club.id, "current").map { books ->
                            club.id to (books.firstOrNull()?.title ?: "Sem livro atual")
                        }
                    }
                    combine(flowsList) { pairs ->
                        pairs.toMap()
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Current Lendo Agora Book
    val currentBook: StateFlow<Book?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) {
            repository.getBookByStatusFlow(clubId, "current").map { it.firstOrNull() }
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Chapters for Current Book
    val currentChapters: StateFlow<List<Chapter>> = currentBook.flatMapLatest { book ->
        if (book != null) repository.getChaptersForBookFlow(book.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active User Progress
    val userProgress: StateFlow<UserProgress?> = combine(currentUserId, activeClubId, currentBook) { userId, clubId, book ->
        Triple(userId, clubId, book)
    }.flatMapLatest { (userId, clubId, book) ->
        if (userId != null && clubId != null && book != null) {
            repository.getUserProgressFlow(userId, clubId, book.id)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Club Books
    val clubBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getClubBooksFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestedBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookByStatusFlow(clubId, "suggested") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val finishedBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookByStatusFlow(clubId, "finished") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Club Members Profile/Progress
    val clubMembers: StateFlow<List<User>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getClubMembersFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProgressForClub: StateFlow<List<UserProgress>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getAllProgressForClubFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Meeting Info
    val latestMeeting: StateFlow<Meeting?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getLatestMeetingFlow(clubId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestMeetingRsvps: StateFlow<List<MeetingRsvp>> = latestMeeting.flatMapLatest { meeting ->
        if (meeting != null) repository.getRsvpsForMeetingFlow(meeting.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Votes
    val suggestionsAndVotes: StateFlow<List<Vote>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getVotesForClubFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications: StateFlow<List<DbNotification>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getNotificationsFlow(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Quotes
    val savedQuotes: StateFlow<List<SavedQuote>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getSavedQuotesForUserFlow(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Books search results from Open Library
    private val _searchResults = MutableStateFlow<List<OpenLibraryDoc>>(emptyList())
    val searchResults: StateFlow<List<OpenLibraryDoc>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    init {
        // Initialize & Seed Database if empty
        viewModelScope.launch {
            repository.seedDatabase()
            // Auto login with default demo user "Você" if session is empty and seed just completed
            dataStoreManager.userIdFlow.first()?.let {
                // Already logged in
            } ?: run {
                // Log in default demo user "Você" to show pristine preloaded data!
                dataStoreManager.saveSession("user_voce", "Você", "voce@tramabook.com")
                dataStoreManager.saveActiveClubId("club_mari")
            }
        }
    }

    // --- Authentication Actions ---
    fun login(name: String, email: String, onCompleted: () -> Unit) {
        viewModelScope.launch {
            val isDemo = email.equals("voce@tramabook.com", ignoreCase = true)
            val userId = if (isDemo) "user_voce" else "user_${UUID.randomUUID().toString().take(6)}"
            val userName = if (isDemo) "Você" else name
            
            val newUser = User(
                userId, 
                userName, 
                email, 
                if (isDemo) "https://lh3.googleusercontent.com/aida-public/AB6AXuCVScro7b5L7FyxSBjNpeqetGOxXZcJe5_EViRuBb5j15OIqZzjjFE8AD5HxgnDcV__koM3NJtsawXA84KY9YNkGFN7fhPvCmJozzDXIkaDWzjObrvzqA2QOSHYCkvK6No2M6UEtsJXEoOaqY7O0WDiVtrhyaKZIqMxGEdP732KB_qtc7_tWeZHNZ9WEOJp6PTJnWMO-kidNZ_0LEvCMirIjMy140n059Elt4YwhfPZbjqKivR3NRgIsXyLxp8THGS41Y3roxiIJS8" 
                else "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120"
            )
            repository.insertUser(newUser)

            dataStoreManager.saveSession(userId, userName, email)
            
            // Check if user already in any clubs
            val clubs = repository.getClubsForUserList(userId)
            if (clubs.isNotEmpty()) {
                dataStoreManager.saveActiveClubId(clubs.first().id)
            } else {
                if (isDemo) {
                    dataStoreManager.saveActiveClubId("club_mari")
                }
            }
            onCompleted()
        }
    }

    fun logout(onCompleted: () -> Unit) {
        viewModelScope.launch {
            dataStoreManager.clearSession()
            onCompleted()
        }
    }

    fun selectActiveClub(clubId: String) {
        viewModelScope.launch {
            dataStoreManager.saveActiveClubId(clubId)
        }
    }

    fun updateUserProfile(nome: String, email: String, avatarUrl: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            val updatedUser = User(userId, nome, email, avatarUrl)
            repository.insertUser(updatedUser)
            dataStoreManager.saveSession(userId, nome, email)
        }
    }

    // --- Club Actions ---
    fun createClub(nome: String, descricao: String, cor: String, privacidade: String, onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            val creatorId = currentUserId.value ?: "user_voce"
            val clubId = "club_${UUID.randomUUID().toString().take(6)}"
            val uniqueCode = (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
            
            val newClub = Club(
                id = clubId,
                nome = nome,
                descricao = descricao,
                codigo = uniqueCode,
                cor = cor,
                privacidade = privacidade,
                criadorId = creatorId,
                criadoEm = System.currentTimeMillis()
            )
            repository.insertClub(newClub)
            
            // creator is member/admin
            repository.insertClubMember(ClubMember(clubId, creatorId, "admin", System.currentTimeMillis()))
            
            // Auto seed the new club with basic initial books/meetings so user is not stuck on empty screens
            seedNewClubData(clubId)

            dataStoreManager.saveActiveClubId(clubId)
            onCompleted(clubId)
        }
    }

    private suspend fun seedNewClubData(clubId: String) {
        // Seed default book: "A Máquina do Tempo" or another masterpiece
        val b = Book(
            id = "book_time_machine_${clubId}",
            title = "A Máquina do Tempo",
            author = "H.G. Wells",
            coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXu30qDj_uGAutNzuhJf1UoXnERY2860xqj5zCrqAYOU2b2zdgvtxIdZGmYqHceK4nnTiYy5WZkVaXZQV25jfbIazcL96ywdPqJtL3AnikceDzM3FX3BxCB_KEBhlg6CRsdbLO0RQr8JcD7M8qTguP83hrTC2kPonrWNJlbx227ZwPU3hMFK83Q1453Tf6w967QAQmzfJTqBjrtfI-gNUdLG9EK1j0NsrKvoBVc5X1TDJswZ5fNU6xSM_YB4JfnzZA166xiLu1iIYvA",
            openlibraryId = "OL12345M",
            isbn = "9780141439976"
        )
        repository.insertBook(b)
        repository.insertClubBook(ClubBook(clubId, b.id, "current", 1))

        // Populate basic chapters
        val chList = (1..5).map { num ->
            Chapter("ch_${num}_${clubId}", b.id, num, "Capítulo $num")
        }
        repository.insertChapters(chList)

        // Init default user progress to Chapter 1
        repository.insertUserProgress(UserProgress(currentUserId.value ?: "user_voce", clubId, b.id, 1))

        // Create standard meeting
        val meet = Meeting(
            id = "meet_${clubId}",
            clubId = clubId,
            data = "Próxima semana",
            hora = "20h",
            local = "Online / Discussão por chamada",
            agenda = "Conectar, bater papo e discutir capítulo 1\nEscolher as metas de leitura"
        )
        repository.insertMeeting(meet)
    }

    fun joinClubWithCode(code: String, onCompleted: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            val club = repository.getClubByCodigo(code.uppercase().trim())
            if (club != null) {
                // Enroll as member
                repository.insertClubMember(ClubMember(club.id, userId, "member", System.currentTimeMillis()))
                dataStoreManager.saveActiveClubId(club.id)
                
                // Add default book progress for this new user
                val curBooks = database.tramabookDao().getBookByStatusFlow(club.id, "current").firstOrNull()
                curBooks?.firstOrNull()?.let { b ->
                    repository.insertUserProgress(UserProgress(userId, club.id, b.id, 1))
                }

                onCompleted(true, null)
            } else {
                onCompleted(false, "Esse código não tá certo. Confere com quem te chamou.")
            }
        }
    }

    // --- Book / Chapter progress actions ---
    fun updateBookProgress(bookId: String, currentChapter: Int) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            val clubId = activeClubId.value ?: return@launch
            repository.insertUserProgress(UserProgress(userId, clubId, bookId, currentChapter))
        }
    }

    // --- Interaction / Discussions ---
    fun getCommentsForChapter(chapterId: String): Flow<List<Comment>> {
        val clubId = activeClubId.value ?: ""
        return repository.getCommentsForChapterFlow(chapterId, clubId)
    }

    fun getSavedQuotesForBook(bookId: String): Flow<List<SavedQuote>> =
        currentUserId.flatMapLatest { userId ->
            repository.getSavedQuotesForBookFlow(userId ?: "user_voce", bookId)
        }

    fun sendComment(chapterId: String, content: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            val clubId = activeClubId.value ?: return@launch
            
            val commentId = "com_${UUID.randomUUID()}"
            val newComment = Comment(
                id = commentId,
                chapterId = chapterId,
                clubId = clubId,
                userId = userId,
                texto = content,
                criadoEm = System.currentTimeMillis()
            )
            repository.insertComment(newComment)
        }
    }

    // --- Reactions ---
    fun getReactionsForChapter(chapterId: String): Flow<List<Reaction>> {
        return repository.getReactionsForChapterFlow(chapterId)
    }

    fun toggleReaction(commentId: String, emoji: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            val existing = database.tramabookDao().getReactionsForCommentFlow(commentId).first().find {
                it.userId == userId && it.emoji == emoji
            }
            if (existing != null) {
                repository.deleteReaction(existing)
            } else {
                repository.insertReaction(Reaction(commentId, userId, emoji))
            }
        }
    }

    // --- Votes ---
    fun voteForBook(bookId: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            val clubId = activeClubId.value ?: return@launch
            
            // clear user previous votes in this club first
            repository.clearVotesForUserInClub(userId, clubId)
            
            // Register vote
            repository.insertVote(Vote(bookId, userId, System.currentTimeMillis()))
        }
    }

    // --- RSVP ---
    fun rsvpMeeting(meetingId: String, status: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            repository.insertMeetingRsvp(MeetingRsvp(meetingId, userId, status))
        }
    }

    fun getRsvpOfUser(meetingId: String): Flow<MeetingRsvp?> {
        val userId = currentUserId.value ?: "user_voce"
        return repository.getRsvpForMeetingOfUserFlow(meetingId, userId)
    }

    // --- Book suggestions search ---
    fun searchOpenLibrary(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        _searchLoading.value = true
        viewModelScope.launch {
            try {
                val response = OpenLibraryApi.service.searchBooks(q)
                _searchResults.value = response.docs ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
            } finally {
                _searchLoading.value = false
            }
        }
    }

    fun createBookSuggestion(doc: OpenLibraryDoc, justification: String, onCompleted: () -> Unit) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val bookId = "book_sug_${UUID.randomUUID().toString().take(6)}"
            val coverId = doc.coverI
            val coverUrl = if (coverId != null) {
                "https://covers.openlibrary.org/b/id/${coverId}-M.jpg"
            } else {
                ""
            }

            val newBook = Book(
                id = bookId,
                title = doc.title,
                author = doc.authorName?.firstOrNull() ?: "Autor desconhecido",
                coverUrl = coverUrl,
                openlibraryId = "",
                isbn = doc.isbn?.firstOrNull() ?: ""
            )
            repository.insertBook(newBook)
            
            // Insert suggestion relation
            repository.insertClubBook(ClubBook(clubId, bookId, "suggested", 0))

            // Keep justification as first comment or notification element of book
            onCompleted()
        }
    }

    // --- Notifications actions ---
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            repository.markAllNotificationsAsRead(userId)
        }
    }

    fun markNotificationAsRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notifId)
        }
    }

    // --- Saved Quotes actions ---
    fun saveQuote(bookId: String, texto: String, capituloRef: String) {
        viewModelScope.launch {
            if (texto.isBlank()) return@launch
            val quote = SavedQuote(
                id = "quote_${UUID.randomUUID()}",
                userId = currentUserId.value ?: "user_voce",
                clubId = activeClubId.value ?: "",
                bookId = bookId,
                texto = texto.trim(),
                capituloRef = capituloRef,
                criadoEm = System.currentTimeMillis()
            )
            repository.insertSavedQuote(quote)
        }
    }

    fun deleteQuote(quote: SavedQuote) {
        viewModelScope.launch {
            repository.deleteSavedQuote(quote)
        }
    }
}
