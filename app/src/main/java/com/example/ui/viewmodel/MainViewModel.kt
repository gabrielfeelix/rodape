package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DataStoreManager
import com.example.data.api.OpenLibraryApi
import com.example.data.api.OpenLibraryDoc
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.remote.AuthRepository
import com.example.data.repository.RodapeRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = RodapeRepository(database.rodapeDao())
    private val dataStoreManager = DataStoreManager(application)
    private val authRepository = AuthRepository()

    // --- Supabase session (paralelo ao DataStore antigo — DataStore some na 9C) ---
    val sessionStatus: StateFlow<SessionStatus> = authRepository.sessionStatus

    val supabaseUserId: StateFlow<String?> = authRepository.currentUserIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Nome do usuario logado vindo direto do JWT do Supabase (user_metadata.full_name).
     *  Disponivel imediatamente apos login (sem precisar query no Postgres).
     *  Cai pra null se nao autenticado. */
    val supabaseDisplayName: StateFlow<String?> = authRepository.currentDisplayNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val supabaseEmail: StateFlow<String?> = authRepository.currentEmailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    // App-level prefs (avaliação na Play Store + contador de engajamento)
    val ratedApp: StateFlow<Boolean> = dataStoreManager.ratedAppFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val engagementCount: StateFlow<Int> = dataStoreManager.engagementCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Mostra o prompt de avaliação se: não avaliou ainda E já fez ≥3 ações de engajamento. */
    val shouldShowRatePrompt: StateFlow<Boolean> =
        combine(ratedApp, engagementCount) { rated, count -> !rated && count >= 3 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun markAppRated() {
        viewModelScope.launch { dataStoreManager.markAppRated() }
    }

    /** Chamado em pontos de engajamento (votar, comentar, salvar frase, RSVP, avaliar livro). */
    fun bumpEngagement() {
        viewModelScope.launch { dataStoreManager.incrementEngagementCount() }
    }

    /** Marca como rated sem mostrar prompt de novo (pra "Agora não, talvez mais tarde" também silenciar). */
    fun dismissRatePromptForever() {
        viewModelScope.launch { dataStoreManager.markAppRated() }
    }

    val fontScale: StateFlow<Float> = dataStoreManager.fontScaleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    fun setFontScale(scale: Float) {
        viewModelScope.launch { dataStoreManager.setFontScale(scale) }
    }

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

    // --- Fase 4 ---
    // Active voting round
    val activeVotingRound: StateFlow<VotingRound?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getActiveVotingRoundFlow(clubId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Book suggestions for current club, indexed by bookId
    val bookSuggestionsByBookId: StateFlow<Map<String, BookSuggestion>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookSuggestionsForClubFlow(clubId).map { list ->
            list.associateBy { it.bookId }
        } else flowOf(emptyMap())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Next-queue books (status = "next")
    val nextBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookByStatusFlow(clubId, "next") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Papel do usuário atual no clube ativo: "super_admin" | "admin" | "member" | null
    val currentUserPapel: StateFlow<String?> = combine(currentUserId, activeClubId) { uid, cid -> Pair(uid, cid) }
        .flatMapLatest { (uid, cid) ->
            if (uid != null && cid != null) {
                repository.getClubMembersRawFlow(cid).map { list ->
                    list.find { it.userId == uid }?.papel
                }
            } else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isCurrentUserAdmin: StateFlow<Boolean> = currentUserPapel
        .map { it == "admin" || it == "super_admin" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isCurrentUserSuperAdmin: StateFlow<Boolean> = currentUserPapel
        .map { it == "super_admin" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Membros do clube ativo (raw com papel)
    val activeClubMembersRaw: StateFlow<List<ClubMember>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getClubMembersRawFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Padrão de encontros do clube ativo
    val activeMeetingPattern: StateFlow<MeetingPattern?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getActiveMeetingPatternFlow(clubId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Comentários removidos do clube ativo
    val removedCommentsInActiveClub: StateFlow<List<Comment>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getRemovedCommentsForClubFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Clubes arquivados do usuário
    val archivedClubsForUser: StateFlow<List<Club>> = currentUserId.flatMapLatest { uid ->
        if (uid != null) repository.getArchivedClubsForUserFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mapa bookId -> dataEncontro (do clube ativo, livros finished)
    val finishedBooksMeetingDates: StateFlow<Map<String, Long?>> = activeClubId.flatMapLatest { clubId ->
        if (clubId == null) flowOf(emptyMap())
        else repository.getClubBooksByStatusFlow(clubId, "finished").map { list ->
            list.associate { it.bookId to it.dataEncontro }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Books search results from Open Library
    // Backcompat: SuggestScreen ainda usa pro cross-check / criar suggestion via createBookSuggestion(doc).
    // Mantém List<OpenLibraryDoc> pra preservar a chamada existente.
    // O novo searchResultsUnified (abaixo) é a versão completa com Google Books incluído.
    private val _searchResults = MutableStateFlow<List<OpenLibraryDoc>>(emptyList())
    val searchResults: StateFlow<List<OpenLibraryDoc>> = _searchResults.asStateFlow()

    private val _searchResultsUnified = MutableStateFlow<List<com.example.data.search.UnifiedBookResult>>(emptyList())
    val searchResultsUnified: StateFlow<List<com.example.data.search.UnifiedBookResult>> = _searchResultsUnified.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    // Pedido de tab solicitada por outro local (ex: notificações). MainTabs observa e troca.
    private val _requestedTab = MutableStateFlow<String?>(null)
    val requestedTab: StateFlow<String?> = _requestedTab.asStateFlow()

    fun requestTab(tab: String) { _requestedTab.value = tab }
    fun consumeRequestedTab() { _requestedTab.value = null }

    init {
        // Initialize & Seed Database if empty
        viewModelScope.launch {
            repository.seedDatabase()
            // Auto login with default demo user "Você" if session is empty and seed just completed
            dataStoreManager.userIdFlow.first()?.let {
                // Already logged in
            } ?: run {
                // Log in default demo user "Você" to show pristine preloaded data!
                dataStoreManager.saveSession("user_voce", "Você", "voce@rodape.com")
                dataStoreManager.saveActiveClubId("club_mari")
            }
            // Dá um respiro pra activeClubId hidratar antes de tentar fechar rodada expirada
            kotlinx.coroutines.delay(500)
            maybeAutoCloseExpiredRound()
        }
    }

    // --- Authentication Actions ---
    fun login(name: String, email: String, onCompleted: () -> Unit) {
        viewModelScope.launch {
            val isDemo = email.equals("voce@rodape.com", ignoreCase = true)
            val userId = if (isDemo) "user_voce" else "user_${UUID.randomUUID().toString().take(6)}"
            val userName = if (isDemo) "Você" else name
            
            // Avatar inicial: preset aleatório dos 12 ilustrados (usuário pode trocar no Perfil)
            val randomPresets = listOf(
                "preset:pequeno_principe", "preset:don_quixote", "preset:petalas",
                "preset:indigena", "preset:detetive", "preset:joana_darc",
                "preset:leitor", "preset:leitora", "preset:mago",
                "preset:emilia", "preset:fantasma", "preset:alice"
            )
            val newUser = User(
                userId,
                userName,
                email,
                if (isDemo) "preset:leitora" else randomPresets.random()
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

    /** Logout que tambem encerra a sessao Supabase. UI antiga ainda pode chamar `logout()`
     *  durante a 9A — esse wrapper sera o canal unico apos a 9B/9C. */
    fun signOutSupabase(onCompleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
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
                criadoEm = System.currentTimeMillis(),
                arquivado = false
            )
            repository.insertClub(newClub)

            // creator vira super_admin do clube
            repository.insertClubMember(ClubMember(clubId, creatorId, "super_admin", System.currentTimeMillis()))
            
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
            isbn = "9780141439976",
            isManual = false,
            totalPaginas = null,
            editora = null,
            anoPublicacao = 1895,
            idioma = "pt"
        )
        repository.insertBook(b)
        repository.insertClubBook(ClubBook(clubId, b.id, "current", 1, null))

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
            agenda = "Conectar, bater papo e discutir capítulo 1\nEscolher as metas de leitura",
            bookId = null,
            chapterStart = null,
            chapterEnd = null,
            status = "agendado"
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
                val curBooks = database.rodapeDao().getBookByStatusFlow(club.id, "current").firstOrNull()
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
                criadoEm = System.currentTimeMillis(),
                removido = false,
                removidoPor = null,
                motivoRemocao = null
            )
            repository.insertComment(newComment)
            bumpEngagement()
        }
    }

    // --- Reactions ---
    fun getReactionsForChapter(chapterId: String): Flow<List<Reaction>> {
        return repository.getReactionsForChapterFlow(chapterId)
    }

    fun toggleReaction(commentId: String, emoji: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            val existing = database.rodapeDao().getReactionsForCommentFlow(commentId).first().find {
                it.userId == userId && it.emoji == emoji
            }
            if (existing != null) {
                repository.deleteReaction(existing)
            } else {
                repository.insertReaction(Reaction(commentId, userId, emoji))
            }
        }
    }

    // --- Votes (rodada) ---
    fun voteForBook(bookId: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            val clubId = activeClubId.value ?: return@launch
            val round = repository.getActiveVotingRound(clubId) ?: return@launch

            // Se já votou neste livro, desfaz
            val existingForBook = repository.getVotesForRound(round.id)
                .firstOrNull { it.userId == userId && it.clubBookId == bookId }
            if (existingForBook != null) {
                repository.removeUserVoteForBookInRound(userId, round.id, bookId)
                return@launch
            }

            // Se atingiu o limite N, não faz nada
            val count = repository.countUserVotesInRound(userId, round.id)
            if (count >= round.nLivros) return@launch

            repository.insertVote(Vote(bookId, userId, System.currentTimeMillis(), round.id))
            bumpEngagement()
        }
    }

    // --- Voting Round actions ---
    fun openVotingRound(nLivros: Int, durationDays: Int, cadencia: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: return@launch
            // Idempotência: se já existe rodada aberta, ignora
            val existing = repository.getActiveVotingRound(clubId)
            if (existing != null) return@launch

            val agora = System.currentTimeMillis()
            val round = VotingRound(
                id = "round_${UUID.randomUUID().toString().take(8)}",
                clubId = clubId,
                criadoPor = userId,
                abertaEm = agora,
                fechaEm = agora + durationDays.toLong() * 24 * 60 * 60 * 1000L,
                nLivros = nLivros.coerceIn(1, 12),
                cadencia = cadencia,
                status = "aberta",
                vencedoresJson = "[]"
            )
            repository.insertVotingRound(round)
        }
    }

    fun closeActiveVotingRound() {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val round = repository.getActiveVotingRound(clubId) ?: return@launch
            closeRoundInternal(round, clubId)
        }
    }

    fun maybeAutoCloseExpiredRound() {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val round = repository.getActiveVotingRound(clubId) ?: return@launch
            if (System.currentTimeMillis() >= round.fechaEm) {
                closeRoundInternal(round, clubId)
            }
        }
    }

    private suspend fun closeRoundInternal(round: VotingRound, clubId: String) {
        val votes = repository.getVotesForRound(round.id)
        val suggestions = repository.getBookSuggestionsForClubFlow(clubId)
            .first()
            .associateBy { it.bookId }

        val winners = com.example.voting.VotingTally.rank(
            votes = votes,
            suggestionsByBookId = suggestions,
            n = round.nLivros
        )

        // Marcar current atual como finished com dataEncontro = now
        val currentList = repository.getBookByStatusFlow(clubId, "current").first()
        currentList.forEach { b ->
            repository.updateClubBookStatus(clubId, b.id, "finished")
            repository.updateClubBookMeetingDate(clubId, b.id, System.currentTimeMillis())
        }

        // Promover vencedores: 1º vira current, demais viram next
        winners.forEachIndexed { idx, bookId ->
            val newStatus = if (idx == 0) "current" else "next"
            repository.updateClubBookStatus(clubId, bookId, newStatus)
        }

        // Marcar rodada como fechada
        val vencedoresJson = JSONArray().apply { winners.forEach { put(it) } }.toString()
        repository.closeVotingRound(round.id, vencedoresJson)

        // Notificar todos os membros do clube
        val members = repository.getClubMembersFlow(clubId).first()
        val titles = winners.mapNotNull { repository.getBook(it)?.title }
        val payload = JSONArray().apply { titles.forEach { put(it) } }.toString()
        members.forEach { member ->
            repository.insertNotification(
                DbNotification(
                    id = "ntf_${UUID.randomUUID()}",
                    userId = member.id,
                    clubId = clubId,
                    tipo = "voting_closed",
                    payloadJson = "{\"titulos\":$payload,\"n\":${winners.size}}",
                    lida = false,
                    criadoEm = System.currentTimeMillis()
                )
            )
        }
    }

    // --- Book detail flows e ações ---
    fun getBookSummaryFlow(bookId: String): Flow<BookSummary?> =
        activeClubId.flatMapLatest { clubId ->
            if (clubId != null) repository.getBookSummaryFlow(bookId, clubId) else flowOf(null)
        }

    fun getBookRatingsFlow(bookId: String): Flow<List<BookRating>> =
        activeClubId.flatMapLatest { clubId ->
            if (clubId != null) repository.getBookRatingsFlow(bookId, clubId) else flowOf(emptyList())
        }

    fun getBookRatingOfCurrentUserFlow(bookId: String): Flow<BookRating?> =
        combine(activeClubId, currentUserId) { c, u -> Pair(c, u) }
            .flatMapLatest { (c, u) ->
                if (c != null && u != null) repository.getBookRatingOfUserFlow(bookId, c, u)
                else flowOf(null)
            }

    fun getBookSuggestionFlow(bookId: String): Flow<BookSuggestion?> =
        activeClubId.flatMapLatest { clubId ->
            if (clubId != null) repository.getBookSuggestionFlow(bookId, clubId) else flowOf(null)
        }

    fun getCommentsForBookFlow(bookId: String): Flow<List<Comment>> =
        activeClubId.flatMapLatest { clubId ->
            if (clubId != null) repository.getCommentsForBookFlow(bookId, clubId) else flowOf(emptyList())
        }

    fun saveBookSummary(bookId: String, texto: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: return@launch
            if (texto.isBlank()) return@launch
            repository.insertBookSummary(
                BookSummary(bookId, clubId, texto.trim(), userId, System.currentTimeMillis())
            )
        }
    }

    fun saveBookRating(bookId: String, stars: Int, comment: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: return@launch
            if (stars !in 1..5) return@launch
            repository.insertBookRating(
                BookRating(bookId, clubId, userId, stars, comment.trim(), System.currentTimeMillis())
            )
            bumpEngagement()
        }
    }

    fun setBookMeetingDate(bookId: String, dataEncontro: Long?) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            repository.updateClubBookMeetingDate(clubId, bookId, dataEncontro)
        }
    }

    // --- RSVP ---
    fun rsvpMeeting(meetingId: String, status: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "user_voce"
            repository.insertMeetingRsvp(MeetingRsvp(meetingId, userId, status))
            bumpEngagement()
        }
    }

    fun getRsvpOfUser(meetingId: String): Flow<MeetingRsvp?> {
        val userId = currentUserId.value ?: "user_voce"
        return repository.getRsvpForMeetingOfUserFlow(meetingId, userId)
    }

    // --- Book suggestions search ---
    /**
     * Busca unificada (OL + GB fallback). Alimenta _searchResultsUnified pra Suggest.
     * Também alimenta _searchResults (OpenLibraryDoc) por backcompat com cross-check
     * de autor — só os resultados que vieram de OL têm representação como Doc.
     */
    fun searchOpenLibrary(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            _searchResults.value = emptyList()
            _searchResultsUnified.value = emptyList()
            return
        }

        _searchLoading.value = true
        viewModelScope.launch {
            try {
                // Busca unificada
                val unified = com.example.data.search.BookSearchService.searchBooks(q)
                _searchResultsUnified.value = unified

                // Pra backcompat com createBookSuggestion(doc), alimenta _searchResults
                // só com os de OL convertidos pra OpenLibraryDoc
                val olDocs = unified
                    .filter { it.source == com.example.data.search.UnifiedBookResult.Source.OPEN_LIBRARY }
                    .map { u ->
                        OpenLibraryDoc(
                            title = u.title,
                            authorName = listOf(u.author),
                            firstPublishYear = u.firstPublishYear,
                            coverI = u.openlibraryRawCoverI,
                            isbn = u.isbn?.let { listOf(it) }
                        )
                    }
                _searchResults.value = olDocs
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
                _searchResultsUnified.value = emptyList()
            } finally {
                _searchLoading.value = false
            }
        }
    }

    fun createBookSuggestion(
        doc: OpenLibraryDoc,
        justification: String,
        authorOverride: String? = null,
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: "user_voce"
            val bookId = "book_sug_${UUID.randomUUID().toString().take(6)}"
            val coverId = doc.coverI
            val coverUrl = if (coverId != null) {
                "https://covers.openlibrary.org/b/id/${coverId}-M.jpg"
            } else {
                ""
            }

            val finalAuthor = authorOverride?.trim()?.takeIf { it.isNotBlank() }
                ?: doc.authorName?.firstOrNull()
                ?: "Autor desconhecido"

            val newBook = Book(
                id = bookId,
                title = doc.title,
                author = finalAuthor,
                coverUrl = coverUrl,
                openlibraryId = "",
                isbn = doc.isbn?.firstOrNull() ?: "",
                isManual = false,
                totalPaginas = null,
                editora = null,
                anoPublicacao = doc.firstPublishYear,
                idioma = "pt"
            )
            repository.insertBook(newBook)

            // Insert suggestion relation
            repository.insertClubBook(ClubBook(clubId, bookId, "suggested", 0, null))

            // Persistir justificativa de verdade
            if (justification.isNotBlank()) {
                repository.insertBookSuggestion(
                    BookSuggestion(
                        id = "bs_${UUID.randomUUID().toString().take(8)}",
                        clubId = clubId,
                        bookId = bookId,
                        suggestedByUserId = userId,
                        justificativa = justification.trim(),
                        criadoEm = System.currentTimeMillis()
                    )
                )
            }

            onCompleted()
        }
    }

    /**
     * Cadastro manual de livro — quando nenhuma API achou.
     * Cria Book com isManual=true + ClubBook como "suggested".
     * Retorna o bookId via callback pra UI navegar/selecionar de volta.
     */
    fun createManualBook(
        title: String,
        author: String,
        isbn: String?,
        anoPublicacao: Int?,
        editora: String?,
        totalPaginas: Int?,
        idioma: String,
        coverPathOrUrl: String,
        onCreated: (bookId: String) -> Unit
    ) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (title.isBlank() || author.isBlank()) return@launch
            val bookId = "book_man_${UUID.randomUUID().toString().take(8)}"
            val cleanIsbn = isbn?.filter { it.isDigit() } ?: ""

            val newBook = Book(
                id = bookId,
                title = title.trim(),
                author = author.trim(),
                coverUrl = coverPathOrUrl,
                openlibraryId = "",
                isbn = cleanIsbn,
                isManual = true,
                totalPaginas = totalPaginas,
                editora = editora?.trim()?.takeIf { it.isNotBlank() },
                anoPublicacao = anoPublicacao,
                idioma = idioma.ifBlank { "pt" }
            )
            repository.insertBook(newBook)
            repository.insertClubBook(ClubBook(clubId, bookId, "suggested", 0, null))
            bumpEngagement()
            onCreated(bookId)
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
            bumpEngagement()
        }
    }

    fun deleteQuote(quote: SavedQuote) {
        viewModelScope.launch {
            repository.deleteSavedQuote(quote)
        }
    }

    // ============================================================
    // ADMIN ACTIONS — Fase 5
    // ============================================================

    fun editClubInfo(nome: String, descricao: String, cor: String, privacidade: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (nome.trim().length < 3) return@launch
            repository.updateClubInfo(clubId, nome.trim(), descricao.trim(), cor, privacidade)
        }
    }

    fun regenerateInviteCode(onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val newCode = (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
            repository.updateClubCodigo(clubId, newCode)
            onResult(newCode)
        }
    }

    fun promoteMemberToAdmin(targetUserId: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value != "super_admin") return@launch
            val target = repository.getClubMember(clubId, targetUserId) ?: return@launch
            if (target.papel != "member") return@launch
            repository.updateMemberPapel(clubId, targetUserId, "admin")

            val clubName = activeClub.value?.nome ?: ""
            val promotedBy = currentUser.value?.nome ?: ""
            repository.insertNotification(
                DbNotification(
                    id = "ntf_${UUID.randomUUID()}",
                    userId = targetUserId,
                    clubId = clubId,
                    tipo = "promoted_to_admin",
                    payloadJson = "{\"clubName\":\"$clubName\",\"promotedBy\":\"$promotedBy\"}",
                    lida = false,
                    criadoEm = System.currentTimeMillis()
                )
            )
        }
    }

    fun demoteAdminToMember(targetUserId: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value != "super_admin") return@launch
            val target = repository.getClubMember(clubId, targetUserId) ?: return@launch
            if (target.papel != "admin") return@launch
            repository.updateMemberPapel(clubId, targetUserId, "member")
        }
    }

    fun transferSuperAdmin(toAdminUserId: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val meId = currentUserId.value ?: return@launch
            if (currentUserPapel.value != "super_admin") return@launch
            val target = repository.getClubMember(clubId, toAdminUserId) ?: return@launch
            if (target.papel != "admin") return@launch

            repository.updateMemberPapel(clubId, meId, "admin")
            repository.updateMemberPapel(clubId, toAdminUserId, "super_admin")

            val clubName = activeClub.value?.nome ?: ""
            val fromUserName = currentUser.value?.nome ?: ""
            repository.insertNotification(
                DbNotification(
                    id = "ntf_${UUID.randomUUID()}",
                    userId = toAdminUserId,
                    clubId = clubId,
                    tipo = "super_admin_transferred",
                    payloadJson = "{\"clubName\":\"$clubName\",\"fromUser\":\"$fromUserName\"}",
                    lida = false,
                    criadoEm = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeMember(targetUserId: String, motivo: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val meId = currentUserId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            if (targetUserId == meId) return@launch
            val target = repository.getClubMember(clubId, targetUserId) ?: return@launch
            if (target.papel == "super_admin") return@launch

            repository.insertMemberRemoval(
                MemberRemoval(
                    id = "rem_${UUID.randomUUID()}",
                    clubId = clubId,
                    userId = targetUserId,
                    removedByUserId = meId,
                    motivo = motivo.trim(),
                    removedAt = System.currentTimeMillis()
                )
            )

            repository.deleteClubMember(clubId, targetUserId)

            val clubName = activeClub.value?.nome ?: ""
            val motivoEscapado = motivo.trim().replace("\\", "\\\\").replace("\"", "\\\"")
            repository.insertNotification(
                DbNotification(
                    id = "ntf_${UUID.randomUUID()}",
                    userId = targetUserId,
                    clubId = clubId,
                    tipo = "member_removed",
                    payloadJson = "{\"clubName\":\"$clubName\",\"motivo\":\"$motivoEscapado\"}",
                    lida = false,
                    criadoEm = System.currentTimeMillis()
                )
            )
        }
    }

    fun leaveActiveClub(onBlocked: (String) -> Unit = {}, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val meId = currentUserId.value ?: return@launch
            val me = repository.getClubMember(clubId, meId) ?: return@launch

            if (me.papel == "super_admin") {
                val members = repository.getClubMembersListOrderedByJoin(clubId)
                val firstAdmin = members.firstOrNull { it.userId != meId && it.papel == "admin" }
                if (firstAdmin == null) {
                    onBlocked("Você é o único administrador. Promova alguém a admin antes de sair, ou arquive o clube.")
                    return@launch
                }
                repository.updateMemberPapel(clubId, firstAdmin.userId, "super_admin")
                val clubName = activeClub.value?.nome ?: ""
                val myName = currentUser.value?.nome ?: ""
                repository.insertNotification(
                    DbNotification(
                        id = "ntf_${UUID.randomUUID()}",
                        userId = firstAdmin.userId,
                        clubId = clubId,
                        tipo = "super_admin_transferred",
                        payloadJson = "{\"clubName\":\"$clubName\",\"fromUser\":\"$myName\"}",
                        lida = false,
                        criadoEm = System.currentTimeMillis()
                    )
                )
            }

            repository.deleteClubMember(clubId, meId)
            val outros = repository.getClubsForUserList(meId)
            if (outros.isNotEmpty()) {
                dataStoreManager.saveActiveClubId(outros.first().id)
            } else {
                dataStoreManager.saveActiveClubId("")
            }
            onDone()
        }
    }

    fun upsertMeetingPattern(
        diaSemana: Int,
        hora: String,
        local: String,
        agendaTemplate: String,
        tipoRecorrencia: String,
        valorRecorrencia: Int
    ) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            repository.deactivateMeetingPatterns(clubId)
            repository.insertMeetingPattern(
                MeetingPattern(
                    id = "pattern_${clubId}_${System.currentTimeMillis()}",
                    clubId = clubId,
                    diaSemana = diaSemana,
                    hora = hora,
                    local = local,
                    agendaTemplate = agendaTemplate,
                    ativo = true,
                    tipoRecorrencia = tipoRecorrencia,
                    valorRecorrencia = valorRecorrencia
                )
            )
        }
    }

    fun deactivateMeetingPattern() {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            repository.deactivateMeetingPatterns(clubId)
        }
    }

    fun upsertMeeting(
        meetingId: String?,
        data: String,
        hora: String,
        local: String,
        agenda: String,
        bookId: String? = null,
        chapterStart: Int? = null,
        chapterEnd: Int? = null
    ) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            val id = meetingId ?: "meet_${UUID.randomUUID().toString().take(8)}"
            // Preserva status atual se já existe, senão "agendado"
            val existing = repository.getMeetingById(id)
            repository.insertMeeting(
                Meeting(
                    id = id,
                    clubId = clubId,
                    data = data,
                    hora = hora,
                    local = local,
                    agenda = agenda,
                    bookId = bookId,
                    chapterStart = chapterStart,
                    chapterEnd = chapterEnd,
                    status = existing?.status ?: "agendado"
                )
            )
        }
    }

    fun cancelMeeting(meetingId: String) {
        viewModelScope.launch {
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            repository.deleteRsvpsForMeeting(meetingId)
            repository.deleteMeeting(meetingId)
        }
    }

    fun removeComment(commentId: String, motivo: String) {
        viewModelScope.launch {
            val meId = currentUserId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            repository.softRemoveComment(commentId, meId, motivo.trim())
        }
    }

    fun restoreRemovedComment(commentId: String) {
        viewModelScope.launch {
            if (currentUserPapel.value != "super_admin") return@launch
            repository.restoreComment(commentId)
        }
    }

    fun removeSuggestion(bookId: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            repository.deleteVotesForBook(bookId)
            repository.deleteBookSuggestion(bookId, clubId)
            repository.deleteClubBook(clubId, bookId)
        }
    }

    fun changeCurrentBookManually(targetBookId: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch

            val currentList = repository.getBookByStatusFlow(clubId, "current").first()
            currentList.forEach { b ->
                repository.updateClubBookStatus(clubId, b.id, "finished")
                repository.updateClubBookMeetingDate(clubId, b.id, System.currentTimeMillis())
            }
            repository.updateClubBookStatus(clubId, targetBookId, "current")
        }
    }

    fun markCurrentBookFinished() {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            val currentList = repository.getBookByStatusFlow(clubId, "current").first()
            currentList.forEach { b ->
                repository.updateClubBookStatus(clubId, b.id, "finished")
                repository.updateClubBookMeetingDate(clubId, b.id, System.currentTimeMillis())
            }
        }
    }

    fun upsertChapters(bookId: String, novos: List<Pair<Int, String>>) {
        viewModelScope.launch {
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            repository.deleteChaptersForBook(bookId)
            val chapters = novos.map { (numero, titulo) ->
                Chapter(
                    id = "ch_${bookId}_${numero}",
                    bookId = bookId,
                    numero = numero,
                    titulo = titulo
                )
            }
            repository.insertChapters(chapters)
        }
    }

    fun archiveClub() {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value != "super_admin") return@launch
            repository.updateClubArquivado(clubId, true)
            val uid = currentUserId.value ?: return@launch
            val outros = repository.getClubsForUserList(uid)
            if (outros.isNotEmpty()) {
                dataStoreManager.saveActiveClubId(outros.first().id)
            } else {
                dataStoreManager.saveActiveClubId("")
            }
        }
    }

    fun unarchiveClub(clubId: String) {
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            val member = repository.getClubMember(clubId, uid) ?: return@launch
            if (member.papel != "super_admin") return@launch
            repository.updateClubArquivado(clubId, false)
            dataStoreManager.saveActiveClubId(clubId)
        }
    }

    /**
     * Faz cross-check do autor com Google Books pra detectar conflitos da Open Library
     * (que às vezes retorna author errado quando cover_id é compartilhado).
     *
     * Retorna o autor encontrado no GB se diferente do fornecido, ou null se bate/falha.
     */
    fun verifyAuthorWithGoogleBooks(
        title: String,
        olAuthor: String,
        isbn: String,
        onResult: (gbAuthor: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val query = if (isbn.isNotBlank()) "isbn:$isbn"
                    else "intitle:${title.take(80)}"
                val gb = com.example.data.api.GoogleBooksApi.service.search(query)
                val gbAuthor = gb.items
                    ?.firstOrNull()
                    ?.volumeInfo
                    ?.authors
                    ?.firstOrNull()
                    .orEmpty()
                if (gbAuthor.isBlank()) {
                    onResult(null)
                    return@launch
                }
                // Normalize pra comparar (ignora case, trim)
                val olNorm = olAuthor.trim().lowercase()
                val gbNorm = gbAuthor.trim().lowercase()
                if (olNorm == gbNorm || olNorm.contains(gbNorm) || gbNorm.contains(olNorm)) {
                    onResult(null) // bateu
                } else {
                    onResult(gbAuthor.trim())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun fetchChaptersOnline(book: Book, onResult: (com.example.voting.ChapterFetchResult) -> Unit) {
        viewModelScope.launch {
            try {
                val query = if (book.isbn.isNotBlank()) "isbn:${book.isbn}"
                    else "intitle:${book.title}+inauthor:${book.author}"
                val gb = com.example.data.api.GoogleBooksApi.service.search(query)
                val desc = gb.items
                    ?.firstOrNull()
                    ?.volumeInfo
                    ?.description
                    .orEmpty()
                val candidates = com.example.voting.ChapterFetcher.extractFromText(desc)
                val validated = com.example.voting.ChapterFetcher.validate(candidates)
                onResult(validated)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(com.example.voting.ChapterFetchResult.Failed)
            }
        }
    }

    // ============================================================
    // Múltiplos encontros por livro (Fase 6)
    // ============================================================

    /**
     * Encontros agendados ou concluídos do livro atual do clube ativo, ordenados.
     */
    val meetingsForCurrentBook: StateFlow<List<Meeting>> =
        combine(activeClubId, currentBook) { clubId, book -> Pair(clubId, book) }
            .flatMapLatest { (clubId, book) ->
                if (clubId != null && book != null) repository.getMeetingsForBookFlow(clubId, book.id)
                else flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Todos os encontros agendados (status = "agendado") do clube ativo.
     */
    val scheduledMeetingsInActiveClub: StateFlow<List<Meeting>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getScheduledMeetingsForClubFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getMeetingByIdFlow(meetingId: String): Flow<Meeting?> = repository.getMeetingByIdFlow(meetingId)

    fun getMeetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?> =
        repository.getMeetingMinutesFlow(meetingId)

    fun getMyMeetingNoteFlow(meetingId: String): Flow<MeetingNote?> = currentUserId.flatMapLatest { uid ->
        if (uid != null) repository.getMeetingNoteFlow(meetingId, uid) else flowOf(null)
    }

    fun saveMeetingMinutes(meetingId: String, texto: String) {
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            if (texto.isBlank()) return@launch
            repository.insertMeetingMinutes(
                MeetingMinutes(
                    meetingId = meetingId,
                    texto = texto.trim(),
                    lastEditorId = uid,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveMyMeetingNote(meetingId: String, texto: String) {
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            repository.insertMeetingNote(
                MeetingNote(
                    meetingId = meetingId,
                    userId = uid,
                    texto = texto.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Marca encontro como concluído. Se for o último agendado do livro atual,
     * promove o livro a finished automaticamente.
     */
    fun concludeMeeting(meetingId: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value !in setOf("admin", "super_admin")) return@launch
            val meeting = repository.getMeetingById(meetingId) ?: return@launch
            repository.updateMeetingStatus(meetingId, "concluido")

            // Se o encontro está vinculado a um livro, verificar se ainda há encontros agendados
            val bookId = meeting.bookId ?: return@launch
            val remaining = repository.getMeetingsForBookList(clubId, bookId)
                .filter { it.id != meetingId && it.status == "agendado" }
            if (remaining.isEmpty()) {
                // Promover livro a finished
                val currentList = repository.getBookByStatusFlow(clubId, "current").first()
                val isCurrentBook = currentList.any { it.id == bookId }
                if (isCurrentBook) {
                    repository.updateClubBookStatus(clubId, bookId, "finished")
                    repository.updateClubBookMeetingDate(clubId, bookId, System.currentTimeMillis())
                    // Notificar membros
                    val members = repository.getClubMembersFlow(clubId).first()
                    val book = repository.getBook(bookId)
                    val titleEsc = (book?.title ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
                    members.forEach { m ->
                        repository.insertNotification(
                            DbNotification(
                                id = "ntf_${UUID.randomUUID()}",
                                userId = m.id,
                                clubId = clubId,
                                tipo = "book_finished",
                                payloadJson = "{\"bookTitle\":\"$titleEsc\"}",
                                lida = false,
                                criadoEm = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Sugestão automática de range de capítulos pro próximo encontro do livro atual.
     * Retorna par (start, end) baseado nos encontros já agendados.
     */
    suspend fun suggestNextChapterRange(): Pair<Int, Int>? {
        val clubId = activeClubId.value ?: return null
        val book = currentBook.value ?: return null
        val totalChapters = currentChapters.value.size
        if (totalChapters == 0) return null
        val existing = repository.getMeetingsForBookList(clubId, book.id)
            .filter { it.chapterEnd != null }
        val lastCovered = existing.maxOfOrNull { it.chapterEnd ?: 0 } ?: 0
        if (lastCovered >= totalChapters) return null
        val start = lastCovered + 1
        val end = (start + 5).coerceAtMost(totalChapters) // sugere ~6 caps
        return start to end
    }
}
