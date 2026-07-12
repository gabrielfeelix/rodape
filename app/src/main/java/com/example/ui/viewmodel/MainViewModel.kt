package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DataStoreManager
import com.example.data.api.OpenLibraryApi
import com.example.data.api.OpenLibraryDoc
import com.example.data.model.*
import com.example.data.remote.AuthRepository
import com.example.data.remote.RemoteRepository
import com.example.util.MeetingTime
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RemoteRepository(application.applicationContext)
    private val dataStoreManager = DataStoreManager(application)
    private val authRepository = AuthRepository()

    // Mostra um erro na tela quando uma ação de salvar FALHA de verdade. Antes o
    // app engolia a exceção e o usuário não sabia por que "não acontecia nada".
    private fun toastErro(msg: String, err: Throwable? = null) {
        android.util.Log.e("Rodape/VM", msg, err)
        runCatching {
            android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // --- Supabase session ---
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

    // currentUserId agora derivado direto do Supabase Auth (sem DataStore).
    val currentUserId: StateFlow<String?> = supabaseUserId

    val currentUser: StateFlow<User?> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getUserFlow(userId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    // Legacy: telas antigas leem userName/userEmail. Mapeamos pra Supabase.
    val userName: StateFlow<String?> = supabaseDisplayName
    val userEmail: StateFlow<String?> = supabaseEmail

    // activeClubId vira state em memoria. Auto-inicializado com o primeiro clube
    // ao logar; persiste so durante a sessao do app (cold-start cai no primeiro).
    private val _activeClubId = MutableStateFlow<String?>(null)
    val activeClubId: StateFlow<String?> = _activeClubId.asStateFlow()

    // App-level prefs (avaliação na Play Store + contador de engajamento)
    val ratedApp: StateFlow<Boolean> = dataStoreManager.ratedAppFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), false)

    val engagementCount: StateFlow<Int> = dataStoreManager.engagementCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), 0)

    /** Mostra o prompt de avaliação se: não avaliou ainda E já fez ≥3 ações de engajamento. */
    val shouldShowRatePrompt: StateFlow<Boolean> =
        combine(ratedApp, engagementCount) { rated, count -> !rated && count >= 3 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), false)

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

    /** Conjunto de userIds que ja viram o onboarding pos-login neste device. */
    val onboardedUsers: StateFlow<Set<String>> = dataStoreManager.onboardedUsersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** True quando o user logado ainda precisa ver o onboarding. */
    val needsOnboarding: StateFlow<Boolean> =
        combine(currentUserId, onboardedUsers) { uid, set ->
            uid != null && uid !in set
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Salva preferencias do onboarding e marca como concluido. */
    fun completeOnboarding(apelido: String, avatarUrl: String, scale: Float) {
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            val email = supabaseEmail.value.orEmpty()
            // Atualiza profile (nome + avatar). Email vem do JWT, nao mexer.
            runCatching { repository.insertUser(User(uid, apelido, email, avatarUrl)) }
            dataStoreManager.setFontScale(scale)
            dataStoreManager.markOnboardingDone(uid)
        }
    }

    // Clubs
    val allClubs: StateFlow<List<Club>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getClubsForUser(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    val activeClub: StateFlow<Club?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getClubFlow(clubId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyMap())

    // Current Lendo Agora Book
    val currentBook: StateFlow<Book?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) {
            repository.getBookByStatusFlow(clubId, "current").map { it.firstOrNull() }
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    // Chapters for Current Book
    val currentChapters: StateFlow<List<Chapter>> = currentBook.flatMapLatest { book ->
        if (book != null) repository.getChaptersForBookFlow(book.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Active User Progress
    val userProgress: StateFlow<UserProgress?> = combine(currentUserId, activeClubId, currentBook) { userId, clubId, book ->
        Triple(userId, clubId, book)
    }.flatMapLatest { (userId, clubId, book) ->
        if (userId != null && clubId != null && book != null) {
            repository.getUserProgressFlow(userId, clubId, book.id)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    // Club Books
    val clubBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getClubBooksFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    val suggestedBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookByStatusFlow(clubId, "suggested") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    val finishedBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookByStatusFlow(clubId, "finished") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Club Members Profile/Progress
    val clubMembers: StateFlow<List<User>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getClubMembersFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    val allProgressForClub: StateFlow<List<UserProgress>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getAllProgressForClubFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Meeting Info
    val latestMeeting: StateFlow<Meeting?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getLatestMeetingFlow(clubId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    val latestMeetingRsvps: StateFlow<List<MeetingRsvp>> = latestMeeting.flatMapLatest { meeting ->
        if (meeting != null) repository.getRsvpsForMeetingFlow(meeting.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Notifications
    val notifications: StateFlow<List<DbNotification>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getNotificationsFlow(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Saved Quotes
    val savedQuotes: StateFlow<List<SavedQuote>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getSavedQuotesForUserFlow(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // --- Fase 4 ---
    // Active voting round
    val activeVotingRound: StateFlow<VotingRound?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getActiveVotingRoundFlow(clubId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    // Votos da RODADA ATIVA — Room-backed (REATIVO): o voto otimista aparece na
    // hora. Antes a UI usava getVotesForClubFlow, um fetch de rede ÚNICO e
    // NÃO-reativo (MutableStateFlow preenchido uma vez), então votar/"trocar pra
    // esse" não refletia até sair e voltar da tela — parecia que "não ia".
    // Definido DEPOIS de activeVotingRound de propósito (o initializer lê o flow).
    val votesForActiveRound: StateFlow<List<Vote>> = activeVotingRound.flatMapLatest { round ->
        if (round != null) repository.getVotesForRoundFlow(round.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Book suggestions for current club, indexed by bookId
    val bookSuggestionsByBookId: StateFlow<Map<String, BookSuggestion>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookSuggestionsForClubFlow(clubId).map { list ->
            list.associateBy { it.bookId }
        } else flowOf(emptyMap())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyMap())

    // Next-queue books (status = "next")
    val nextBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookByStatusFlow(clubId, "next") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Papel do usuário atual no clube ativo: "super_admin" | "admin" | "member" | null
    // Emite null IMEDIATAMENTE ao trocar de clube (antes o stateIn segurava o papel
    // do clube ANTERIOR até o novo flow emitir — os guards de admin e o header
    // piscavam/misfire com o papel errado).
    val currentUserPapel: StateFlow<String?> = combine(currentUserId, activeClubId) { uid, cid -> Pair(uid, cid) }
        .flatMapLatest { (uid, cid) ->
            if (uid != null && cid != null) {
                flow<String?> {
                    emit(null) // reset ao trocar de clube
                    emitAll(
                        repository.getClubMembersRawFlow(cid).map { list ->
                            list.find { it.userId == uid }?.papel
                        }
                    )
                }
            } else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    val isCurrentUserAdmin: StateFlow<Boolean> = currentUserPapel
        .map { it == "admin" || it == "super_admin" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), false)

    val isCurrentUserSuperAdmin: StateFlow<Boolean> = currentUserPapel
        .map { it == "super_admin" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), false)

    // Membros do clube ativo (raw com papel)
    val activeClubMembersRaw: StateFlow<List<ClubMember>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getClubMembersRawFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Padrão de encontros do clube ativo
    val activeMeetingPattern: StateFlow<MeetingPattern?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getActiveMeetingPatternFlow(clubId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    // Comentários removidos do clube ativo
    val removedCommentsInActiveClub: StateFlow<List<Comment>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getRemovedCommentsForClubFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Clubes arquivados do usuário
    val archivedClubsForUser: StateFlow<List<Club>> = currentUserId.flatMapLatest { uid ->
        if (uid != null) repository.getArchivedClubsForUserFlow(uid) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Mapa bookId -> dataEncontro (do clube ativo, livros finished)
    val finishedBooksMeetingDates: StateFlow<Map<String, Long?>> = activeClubId.flatMapLatest { clubId ->
        if (clubId == null) flowOf(emptyMap())
        else repository.getClubBooksByStatusFlow(clubId, "finished").map { list ->
            list.associate { it.bookId to it.dataEncontro }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyMap())

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
        // App nasce vazio em producao — sem seed, sem auto-login fake.
        //
        // Defesa em profundidade contra leak de dados entre contas no mesmo
        // device: quando detectamos que o userId logado e DIFERENTE do que
        // persistimos no ultimo cold-start, limpamos o cache Room antes de
        // qualquer fluxo comecar a ler dele. Cobre 3 cenarios:
        //  1. App morto entre signOut() e clearLocalCache() no logout anterior
        //  2. Conta nova logando em device que ja teve outra conta antes
        //  3. Mesmo userId persistido mas diferente do logado agora
        // Tambem limpa quando NUNCA houve user antes mas o cache nao esta vazio
        // (instalacao antiga com dados orfaos).
        viewModelScope.launch {
            currentUserId.collect { newUserId ->
                if (newUserId == null) return@collect
                val lastId = runCatching { dataStoreManager.lastUserId() }.getOrNull()
                if (lastId != newUserId) {
                    android.util.Log.w(
                        "Rodape/VM",
                        "Troca/primeiro login (last=$lastId, new=$newUserId). Limpando cache local."
                    )
                    // SEM drenar: as mutations pendentes eram do usuario ANTERIOR e
                    // seriam reenviadas sob a sessao do novo (misattribution/RLS-reject).
                    runCatching { repository.clearLocalCacheNoDrain() }
                    runCatching { dataStoreManager.setLastActiveClubId(null) }
                    runCatching { dataStoreManager.setLastUserId(newUserId) }
                }
            }
        }

        // Quando o currentUserId mudar (login/logout), auto-seleciona o primeiro
        // clube do usuario como activeClub. Se ele nao for membro de nenhum,
        // activeClubId fica null e a UI mostra estado vazio com CTA de criar/entrar.
        viewModelScope.launch {
            allClubs.collect { clubs ->
                val current = _activeClubId.value
                if (clubs.isEmpty()) {
                    _activeClubId.value = null
                } else if (current == null || clubs.none { it.id == current }) {
                    // Restaura o último clube selecionado (se ainda é membro),
                    // em vez de sempre cair no primeiro. Power user volta pro
                    // clube onde estava.
                    val saved = dataStoreManager.lastActiveClubId()
                    _activeClubId.value = clubs.firstOrNull { it.id == saved }?.id
                        ?: clubs.first().id
                }
            }
        }
        // Auto-close reativo: fecha a rodada assim que ela vira "expirada", em vez
        // de um delay(500) fixo que rodava com activeClubId ainda null (nao fechava).
        viewModelScope.launch {
            activeVotingRound.collect { round ->
                if (round != null && round.status == "aberta" &&
                    System.currentTimeMillis() >= round.fechaEm
                ) {
                    maybeAutoCloseExpiredRound()
                }
            }
        }

        // Avatar inicial inteligente: quando detectamos profile com avatar default
        // 'preset:leitor' (que o trigger handle_new_user usa pra todos), escolhe um
        // preset adequado ao nome (genero pelo primeiro nome). So roda 1 vez por user.
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null && user.avatarUrl == "preset:leitor") {
                    val sugerido = com.example.ui.components.AvatarPicker.pickFor(user.nome)
                    if (sugerido != "preset:leitor") {
                        // Atualiza so o avatar — preserva o resto do profile.
                        repository.insertUser(user.copy(avatarUrl = sugerido))
                    }
                }
            }
        }
    }

    // --- Authentication Actions ---
    // O login real e feito por AuthRepository (email/senha + Google). Esta funcao
    // existe como compat com chamadas antigas (alguns lugares ainda invocam).
    fun login(name: String, email: String, onCompleted: () -> Unit) {
        // 9B: no-op compat. Login real e via AuthRepository nas telas de auth.
        onCompleted()
    }

    override fun onCleared() {
        super.onCleared()
        // Cancela subscriptions Realtime e loops de reload do repo (antes ficavam
        // rodando pra sempre, detached do ciclo de vida — leak de socket/coroutine).
        repository.close()
    }

    fun logout(onCompleted: () -> Unit) {
        viewModelScope.launch {
            // Drena a fila ENQUANTO ainda autenticado — senao o drain rodaria
            // deslogado (401) e descartaria mutations que o usuario achou salvas.
            runCatching { repository.tryDrainPendingQueue() }
            runCatching { authRepository.signOut() }
            dataStoreManager.clearSession()
            runCatching { repository.clearLocalCacheNoDrain() }
            runCatching { dataStoreManager.setLastUserId(null) }
            runCatching { dataStoreManager.setLastActiveClubId(null) }
            _activeClubId.value = null
            onCompleted()
        }
    }

    /** Logout que tambem encerra a sessao Supabase. UI antiga ainda pode chamar `logout()`
     *  durante a 9A — esse wrapper sera o canal unico apos a 9B/9C. */
    fun signOutSupabase(onCompleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.tryDrainPendingQueue() }
            runCatching { authRepository.signOut() }
            dataStoreManager.clearSession()
            // Limpa Room — evita vazar dados entre contas no mesmo device.
            runCatching { repository.clearLocalCacheNoDrain() }
            runCatching { dataStoreManager.setLastUserId(null) }
            runCatching { dataStoreManager.setLastActiveClubId(null) }
            _activeClubId.value = null
            onCompleted()
        }
    }

    fun selectActiveClub(clubId: String) {
        _activeClubId.value = clubId
        // Persiste pra restaurar no próximo cold start.
        viewModelScope.launch { dataStoreManager.setLastActiveClubId(clubId) }
    }

    /**
     * Exclui a conta do usuario (requisito da Play Store). Tenta o RPC
     * server-side; em sucesso, limpa sessao/cache local e desloga. Se o RPC
     * falhar (ex: ainda nao criado no Supabase), reporta erro pra UI oferecer
     * o fallback por email — nunca deixa o usuario num beco sem saida.
     */
    fun deleteAccount(onDeleted: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteOwnAccountViaRpc()
            } catch (e: Exception) {
                onError(e.message ?: "Não foi possível excluir a conta agora.")
                return@launch
            }
            runCatching { authRepository.signOut() }
            dataStoreManager.clearSession()
            // Conta excluida no servidor: nao adianta drenar (mutations pendentes
            // ficaram orfas). So limpa local.
            runCatching { repository.clearLocalCacheNoDrain() }
            runCatching { dataStoreManager.setLastUserId(null) }
            runCatching { dataStoreManager.setLastActiveClubId(null) }
            _activeClubId.value = null
            onDeleted()
        }
    }

    fun updateUserProfile(nome: String, email: String, avatarUrl: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: return@launch
            // email do supabase auth.users e gerido pelo Supabase; aqui so atualiza profile.
            val updatedUser = User(userId, nome, email, avatarUrl)
            repository.insertUser(updatedUser)
        }
    }

    // --- Club Actions ---
    fun createClub(
        nome: String,
        descricao: String,
        cor: String,
        privacidade: String,
        onCompleted: (String) -> Unit,
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                // RPC: cria clube + super_admin member + invite code unico, atomicamente.
                val clubId = repository.createClubViaRpc(nome, descricao, cor, privacidade)
                _activeClubId.value = clubId
                dataStoreManager.setLastActiveClubId(clubId)
                onCompleted(clubId)
            } catch (e: Exception) {
                android.util.Log.e("Rodape/VM", "createClub falhou", e)
                // Mensagem amigavel pra UI parar de mostrar loading e exibir o erro.
                onError(
                    com.example.ui.auth.AuthErrors.friendly(
                        e,
                        fallback = "Nao consegui criar o clube. Tente de novo em instantes."
                    )
                )
            }
        }
    }

    fun joinClubWithCode(code: String, onCompleted: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val clubId = repository.joinClubWithCodeViaRpc(code)
                if (clubId != null) {
                    _activeClubId.value = clubId
                    dataStoreManager.setLastActiveClubId(clubId)
                    // Inicializa progresso no livro atual, se existir.
                    val curBooks = repository.getBookByStatusFlow(clubId, "current").first()
                    val userId = currentUserId.value
                    curBooks.firstOrNull()?.let { b ->
                        if (userId != null) {
                            repository.insertUserProgress(UserProgress(userId, clubId, b.id, 1))
                        }
                    }
                    onCompleted(true, null)
                } else {
                    onCompleted(false, "Esse código não tá certo. Confere com quem te chamou.")
                }
            } catch (e: Exception) {
                // Mensagem específica do servidor (código inválido vs não encontrado
                // vs sem conexão) em vez de sempre "código errado".
                onCompleted(false, com.example.ui.auth.AuthErrors.friendly(
                    e, fallback = "Não consegui entrar no clube. Confere o código e a conexão."
                ))
            }
        }
    }

    // --- Book / Chapter progress actions ---
    fun updateBookProgress(bookId: String, currentChapter: Int) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: return@launch
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
            if (userId != null) repository.getSavedQuotesForBookFlow(userId, bookId)
            else flowOf(emptyList())
        }

    fun sendComment(chapterId: String, content: String) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: return@launch
            val clubId = activeClubId.value ?: return@launch
            
            val commentId = UUID.randomUUID().toString()
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
            val userId = currentUserId.value ?: return@launch
            val existing = repository.getReactionsForCommentFlow(commentId).first().find {
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
            val userId = currentUserId.value ?: return@launch
            // Rodada ativa vem do StateFlow (Room-backed) — funciona OFFLINE. Antes
            // usava getActiveVotingRound remoto: null offline -> voto virava no-op.
            val round = activeVotingRound.value ?: return@launch

            val myVote = repository.getVotesForRound(round.id)
                .firstOrNull { it.userId == userId }?.clubBookId
            // VOTO ÚNICO por rodada. Tocar no próprio voto desfaz; votar em outro TROCA
            // (upsert atômico com onConflict — sem delete+insert separados que causavam
            // o "vira teu voto e volta em loop"). nLivros = nº de VENCEDORES, não votos.
            if (myVote == bookId) {
                repository.removeUserVoteForBookInRound(userId, round.id, bookId)
            } else {
                repository.setUserVoteInRound(userId, round.id, bookId)
                bumpEngagement()
            }
        }
    }

    // --- Voting Round actions ---
    fun openVotingRound(nLivros: Int, durationDays: Int, cadencia: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: return@launch
            // Idempotência via estado LOCAL (Room) — instantâneo. Antes fazia um
            // GET remoto (getActiveVotingRound) que travava a ação por segundos:
            // o usuário clicava "Abrir votação" e "não acontecia nada" até a rede
            // responder, e só depois a rodada aparecia.
            if (activeVotingRound.value != null) return@launch

            val agora = System.currentTimeMillis()
            val round = VotingRound(
                id = UUID.randomUUID().toString(),
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
        // RPC close_voting_round faz tudo no servidor:
        //  - marca current atual como finished
        //  - promove vencedores (1º current, demais next)
        //  - cria notificacoes pra todos membros
        //  - marca round como fechada
        repository.closeVotingRoundViaRpc(round.id)
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
            val userId = currentUserId.value ?: return@launch
            repository.insertMeetingRsvp(MeetingRsvp(meetingId, userId, status))
            bumpEngagement()
        }
    }

    fun getRsvpOfUser(meetingId: String): Flow<MeetingRsvp?> {
        val userId = currentUserId.value ?: return flowOf(null)
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

                // Converte TODAS as fontes (Open Library + Google Books) pra
                // OpenLibraryDoc — antes filtrava só OL, e livros que existiam
                // apenas no Google Books nunca apareciam na tela de sugerir.
                // (GB não tem coverI de OL; a capa cai pra capa gerada.)
                val docs = unified.map { u ->
                    OpenLibraryDoc(
                        title = u.title,
                        authorName = listOf(u.author),
                        firstPublishYear = u.firstPublishYear,
                        coverI = u.openlibraryRawCoverI,
                        isbn = u.isbn?.let { listOf(it) }
                    )
                }
                _searchResults.value = docs
            } catch (e: Exception) {
                android.util.Log.e("Rodape/VM", "Operacao falhou silenciosamente", e)
                _searchResults.value = emptyList()
                _searchResultsUnified.value = emptyList()
            } finally {
                _searchLoading.value = false
            }
        }
    }

    /** Dedup: se o clube já tem esse livro (mesmo ISBN, ou mesmo openlibraryId, ou
     *  mesmo título), reusa o bookId em vez de criar um candidato duplicado. Antes,
     *  o mesmo livro sugerido por 3 pessoas virava 3 candidatos de voto separados. */
    private fun existingClubBookId(title: String, isbn: String, openlibraryId: String = ""): String? {
        val books = clubBooks.value
        val cleanIsbn = isbn.filter { it.isDigit() }
        if (cleanIsbn.isNotBlank()) {
            books.firstOrNull { it.isbn.filter { c -> c.isDigit() } == cleanIsbn }?.let { return it.id }
        }
        if (openlibraryId.isNotBlank()) {
            books.firstOrNull { it.openlibraryId.isNotBlank() && it.openlibraryId == openlibraryId }?.let { return it.id }
        }
        val normTitle = title.trim().lowercase()
        return books.firstOrNull { it.title.trim().lowercase() == normTitle }?.id
    }

    fun createBookSuggestion(
        doc: OpenLibraryDoc,
        justification: String,
        authorOverride: String? = null,
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch {
          try {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value ?: return@launch
            val isbn = doc.isbn?.firstOrNull() ?: ""

            // Reusa livro existente no clube (dedup) ou cria um novo.
            val existingId = existingClubBookId(doc.title, isbn)
            val bookId = existingId ?: UUID.randomUUID().toString()

            if (existingId == null) {
                val coverId = doc.coverI
                val coverUrl = if (coverId != null) "https://covers.openlibrary.org/b/id/${coverId}-M.jpg" else ""
                val finalAuthor = authorOverride?.trim()?.takeIf { it.isNotBlank() }
                    ?: doc.authorName?.firstOrNull()
                    ?: "Autor desconhecido"
                val newBook = Book(
                    id = bookId, title = doc.title, author = finalAuthor,
                    coverUrl = coverUrl, openlibraryId = "", isbn = isbn,
                    isManual = false, totalPaginas = null, editora = null,
                    anoPublicacao = doc.firstPublishYear, idioma = "pt"
                )
                repository.insertBook(newBook)
                repository.insertClubBook(ClubBook(clubId, bookId, "suggested", 0, null))
            }

            // SEMPRE grava a atribuição (quem sugeriu + quando), mesmo sem
            // justificativa — antes só gravava se justificativa != blank, então a
            // autoria se perdia e o histórico ficava genérico.
            repository.insertBookSuggestion(
                BookSuggestion(
                    id = UUID.randomUUID().toString(),
                    clubId = clubId,
                    bookId = bookId,
                    suggestedByUserId = userId,
                    justificativa = justification.trim(),
                    criadoEm = System.currentTimeMillis()
                )
            )

            onCompleted()
          } catch (e: Exception) {
            toastErro("Não consegui sugerir o livro. Tenta de novo.", e)
          }
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
          try {
            val clubId = activeClubId.value ?: return@launch
            val userId = currentUserId.value
            if (title.isBlank() || author.isBlank()) return@launch
            val cleanIsbn = isbn?.filter { it.isDigit() } ?: ""

            // Dedup: se o clube já tem esse livro, reusa (não recria candidato).
            existingClubBookId(title.trim(), cleanIsbn)?.let { existing ->
                if (userId != null) {
                    repository.insertBookSuggestion(
                        BookSuggestion(
                            id = UUID.randomUUID().toString(), clubId = clubId, bookId = existing,
                            suggestedByUserId = userId, justificativa = "",
                            criadoEm = System.currentTimeMillis()
                        )
                    )
                }
                bumpEngagement()
                onCreated(existing)
                return@launch
            }

            val bookId = UUID.randomUUID().toString()

            // Se coverPathOrUrl e file:// local, sobe pro bucket book-covers
            // e usa o signed URL no banco. Se ja for http(s), passa direto.
            val finalCoverUrl = when {
                coverPathOrUrl.startsWith("file://") -> {
                    val ctx = getApplication<Application>().applicationContext
                    val uri = android.net.Uri.parse(coverPathOrUrl)
                    val bytes = runCatching {
                        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull()
                    if (bytes != null) {
                        repository.uploadBookCover(clubId, bookId, bytes) ?: ""
                    } else ""
                }
                coverPathOrUrl.startsWith("http") -> coverPathOrUrl
                else -> coverPathOrUrl  // ja e signed url ou path
            }

            val newBook = Book(
                id = bookId,
                title = title.trim(),
                author = author.trim(),
                coverUrl = finalCoverUrl,
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
            if (userId != null) {
                repository.insertBookSuggestion(
                    BookSuggestion(
                        id = UUID.randomUUID().toString(), clubId = clubId, bookId = bookId,
                        suggestedByUserId = userId, justificativa = "",
                        criadoEm = System.currentTimeMillis()
                    )
                )
            }
            bumpEngagement()
            onCreated(bookId)
          } catch (e: Exception) {
            toastErro("Não consegui adicionar o livro. Tenta de novo.", e)
          }
        }
    }

    // --- Notifications actions ---
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            val userId = currentUserId.value ?: return@launch
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
            val userId = currentUserId.value ?: return@launch
            val clubId = activeClubId.value ?: return@launch
            val quote = SavedQuote(
                id = UUID.randomUUID().toString(),
                userId = userId,
                clubId = clubId,
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

    /** Undo do "excluir frase": reinsere a mesma quote (mesmo id). */
    fun restoreQuote(quote: SavedQuote) {
        viewModelScope.launch {
            repository.insertSavedQuote(quote)
        }
    }

    // --- Sync / offline ---

    /** Tamanho da fila offline — UI mostra "alterações aguardando conexão". */
    val pendingMutationsCount = repository.pendingMutationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), 0)

    /** Pull-to-refresh: força re-sync de todas as caches ativas, ignorando TTL. */
    fun forceRefresh(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.forceRefresh() }
            onDone()
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

    fun regenerateInviteCode(onResult: (String) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            try {
                val newCode = repository.regenerateInviteCodeViaRpc(clubId)
                if (newCode.isBlank()) onError("Não consegui gerar um novo código agora.")
                else onResult(newCode)
            } catch (e: Exception) {
                onError(com.example.ui.auth.AuthErrors.friendly(e, fallback = "Não consegui gerar um novo código agora."))
            }
        }
    }

    fun promoteMemberToAdmin(targetUserId: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            try {
                repository.promoteMemberViaRpc(clubId, targetUserId)
            } catch (e: Exception) {
                onError(com.example.ui.auth.AuthErrors.friendly(
                    e, fallback = "Não foi possível promover esse membro."
                ))
            }
        }
    }

    fun demoteAdminToMember(targetUserId: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            try {
                repository.demoteAdminViaRpc(clubId, targetUserId)
            } catch (e: Exception) {
                onError(com.example.ui.auth.AuthErrors.friendly(
                    e, fallback = "Não foi possível rebaixar esse admin."
                ))
            }
        }
    }

    fun transferSuperAdmin(toAdminUserId: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            try {
                repository.transferSuperAdminViaRpc(clubId, toAdminUserId)
            } catch (e: Exception) {
                onError(com.example.ui.auth.AuthErrors.friendly(
                    e, fallback = "Não foi possível transferir o super-admin."
                ))
            }
        }
    }

    fun removeMember(targetUserId: String, motivo: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            try {
                repository.removeMemberViaRpc(clubId, targetUserId, motivo.trim())
            } catch (e: Exception) {
                onError(com.example.ui.auth.AuthErrors.friendly(
                    e, fallback = "Não foi possível remover esse membro."
                ))
            }
        }
    }

    fun leaveActiveClub(onBlocked: (String) -> Unit = {}, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val meId = currentUserId.value ?: return@launch
            try {
                // Só troca de clube ativo APÓS confirmar o sucesso (a RPC agora
                // propaga exceção; antes engolia o erro e reportava "saiu" à toa).
                repository.leaveClubViaRpc(clubId)
                val outros = repository.getClubsForUserList(meId)
                val proximo = outros.firstOrNull()?.id
                _activeClubId.value = proximo
                dataStoreManager.setLastActiveClubId(proximo)
                onDone()
            } catch (e: Exception) {
                onBlocked(com.example.ui.auth.AuthErrors.friendly(
                    e, fallback = "Não foi possível sair do clube."
                ))
            }
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
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin
            // (o guard antigo lia currentUserPapel do cache, que fica null num clube
            // recém-carregado e bloqueava a ação de um admin de verdade sem aviso).
            repository.deactivateMeetingPatterns(clubId)
            val pattern = MeetingPattern(
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
            repository.insertMeetingPattern(pattern)
            // AGORA a recorrência gera encontros de verdade (antes o padrão era só
            // um rótulo e nenhum encontro nascia dele). Cria as próximas ocorrências.
            repository.generateMeetingsFromPattern(pattern)
        }
    }

    fun deactivateMeetingPattern() {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin
            // (o guard antigo lia currentUserPapel do cache, que fica null num clube
            // recém-carregado e bloqueava a ação de um admin de verdade sem aviso).
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
          try {
            val clubId = activeClubId.value ?: return@launch
            val id = meetingId ?: UUID.randomUUID().toString()
            // Preserva status atual se já existe, senão "agendado"
            val existing = repository.getMeetingById(id)
            // Calcula o instante real no FUSO LOCAL a partir do "DD/MM/YYYY" + "HH:mm"
            // e deriva o rótulo bonito (consistente com o que o servidor devolve).
            val epoch = MeetingTime.parseLocal(data, hora) ?: System.currentTimeMillis()
            val (label, horaLabel) = MeetingTime.epochToLabel(epoch)
            repository.insertMeeting(
                Meeting(
                    id = id,
                    clubId = clubId,
                    data = label,
                    hora = horaLabel,
                    local = local,
                    agenda = agenda,
                    bookId = bookId,
                    chapterStart = chapterStart,
                    chapterEnd = chapterEnd,
                    status = existing?.status ?: "agendado",
                    dataEpoch = epoch,
                )
            )
          } catch (e: Exception) {
            toastErro("Não consegui salvar o encontro. Tenta de novo.", e)
          }
        }
    }

    fun cancelMeeting(meetingId: String) {
        viewModelScope.launch {
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin
            // (o guard antigo lia currentUserPapel do cache, que fica null num clube
            // recém-carregado e bloqueava a ação de um admin de verdade sem aviso).
            repository.deleteRsvpsForMeeting(meetingId)
            repository.deleteMeeting(meetingId)
        }
    }

    fun removeComment(commentId: String, motivo: String) {
        viewModelScope.launch {
            val meId = currentUserId.value ?: return@launch
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin
            // (o guard antigo lia currentUserPapel do cache, que fica null num clube
            // recém-carregado e bloqueava a ação de um admin de verdade sem aviso).
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
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin
            // (o guard antigo lia currentUserPapel do cache, que fica null num clube
            // recém-carregado e bloqueava a ação de um admin de verdade sem aviso).
            repository.deleteVotesForBook(bookId)
            repository.deleteBookSuggestion(bookId, clubId)
            repository.deleteClubBook(clubId, bookId)
        }
    }

    fun changeCurrentBookManually(targetBookId: String) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin
            // (o guard antigo lia currentUserPapel do cache, que fica null num clube
            // recém-carregado e bloqueava a ação de um admin de verdade sem aviso).

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
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin
            // (o guard antigo lia currentUserPapel do cache, que fica null num clube
            // recém-carregado e bloqueava a ação de um admin de verdade sem aviso).
            val currentList = repository.getBookByStatusFlow(clubId, "current").first()
            currentList.forEach { b ->
                repository.updateClubBookStatus(clubId, b.id, "finished")
                repository.updateClubBookMeetingDate(clubId, b.id, System.currentTimeMillis())
            }
        }
    }

    fun upsertChapters(bookId: String, chapters: List<Chapter>) {
        viewModelScope.launch {
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin.
            // Salva por DIFF por id estável (uuid): editar/adicionar/reordenar NÃO
            // apaga nem remaneja a discussão dos capítulos mantidos, e o id uuid
            // sincroniza com o servidor (bugs P0-1 e B2 corrigidos).
            repository.saveChapters(bookId, chapters)
        }
    }

    /** Edita o texto do próprio comentário (RLS permite autor). */
    fun editComment(commentId: String, novoTexto: String) {
        viewModelScope.launch {
            val texto = novoTexto.trim()
            if (texto.isBlank() || texto.length > 4000) return@launch
            repository.editOwnComment(commentId, texto)
        }
    }

    /** Apaga o próprio comentário (hard delete; RLS "comments delete self"). */
    fun deleteOwnComment(commentId: String) {
        viewModelScope.launch {
            repository.deleteOwnComment(commentId)
        }
    }

    fun archiveClub() {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            if (currentUserPapel.value != "super_admin") return@launch
            repository.updateClubArquivado(clubId, true)
            val uid = currentUserId.value ?: return@launch
            val outros = repository.getClubsForUserList(uid)
            val proximo = outros.firstOrNull()?.id
            _activeClubId.value = proximo
            dataStoreManager.setLastActiveClubId(proximo)
        }
    }

    fun unarchiveClub(clubId: String) {
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            val member = repository.getClubMember(clubId, uid) ?: return@launch
            if (member.papel != "super_admin") return@launch
            repository.updateClubArquivado(clubId, false)
            _activeClubId.value = clubId
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
                android.util.Log.e("Rodape/VM", "Operacao falhou silenciosamente", e)
                onResult(null)
            }
        }
    }

    fun fetchChaptersOnline(book: Book, onResult: (com.example.util.voting.ChapterFetchResult) -> Unit) {
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
                val candidates = com.example.util.voting.ChapterFetcher.extractFromText(desc)
                val validated = com.example.util.voting.ChapterFetcher.validate(candidates)
                onResult(validated)
            } catch (e: Exception) {
                android.util.Log.e("Rodape/VM", "Operacao falhou silenciosamente", e)
                onResult(com.example.util.voting.ChapterFetchResult.Failed)
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
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    /**
     * Todos os encontros agendados (status = "agendado") do clube ativo.
     */
    val scheduledMeetingsInActiveClub: StateFlow<List<Meeting>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getScheduledMeetingsForClubFlow(clubId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    fun getMeetingByIdFlow(meetingId: String): Flow<Meeting?> = repository.getMeetingByIdFlow(meetingId)

    fun getMeetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?> =
        repository.getMeetingMinutesFlow(meetingId)

    fun getMyMeetingNoteFlow(meetingId: String): Flow<MeetingNote?> = currentUserId.flatMapLatest { uid ->
        if (uid != null) repository.getMeetingNoteFlow(meetingId, uid) else flowOf(null)
    }

    fun saveMeetingMinutes(meetingId: String, texto: String) {
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin
            // (o guard antigo lia currentUserPapel do cache, que fica null num clube
            // recém-carregado e bloqueava a ação de um admin de verdade sem aviso).
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
            // Sem guard de papel client-side: RLS/RPC do servidor já exige admin
            // (o guard antigo lia currentUserPapel do cache, que fica null num clube
            // recém-carregado e bloqueava a ação de um admin de verdade sem aviso).
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
                    // As notificações de "livro finalizado" são criadas server-side
                    // pelo trigger club_books_notify_finished (migration 0002). O
                    // insert via cliente falhava (sem INSERT policy + enum inválido).
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
