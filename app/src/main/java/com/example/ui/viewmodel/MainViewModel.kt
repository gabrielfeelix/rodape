package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DataStoreManager
import com.example.data.ThemeMode
import com.example.data.api.OpenLibraryApi
import com.example.data.api.OpenLibraryDoc
import com.example.data.model.*
import com.example.data.remote.AuthRepository
import com.example.data.remote.RemoteRepository
import com.example.data.session.SessionManager
import com.example.util.MeetingTime
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.util.UUID
import javax.inject.Inject

// F4b: dependências via Hilt (fachada/DataStore/Auth @Singleton) + grafo de
// sessão movido pro SessionManager — este VM consome os flows de lá por alias,
// então a UI não muda. O split por tela (F5) vai esvaziando este arquivo.
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: RemoteRepository,
    private val dataStoreManager: DataStoreManager,
    private val authRepository: AuthRepository,
    private val session: SessionManager,
) : AndroidViewModel(application) {

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

    // F4b: espinha da sessão vem do SessionManager (aliases — mesma API pra UI).
    val supabaseUserId: StateFlow<String?> = session.currentUserId

    /** Nome do usuario logado vindo direto do JWT do Supabase (user_metadata.full_name).
     *  Disponivel imediatamente apos login (sem precisar query no Postgres).
     *  Cai pra null se nao autenticado. */
    val supabaseDisplayName: StateFlow<String?> = authRepository.currentDisplayNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val supabaseEmail: StateFlow<String?> = authRepository.currentEmailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // currentUserId agora derivado direto do Supabase Auth (sem DataStore).
    val currentUserId: StateFlow<String?> = supabaseUserId

    val currentUser: StateFlow<User?> = session.currentUser

    // Legacy: telas antigas leem userName/userEmail. Mapeamos pra Supabase.
    val userName: StateFlow<String?> = supabaseDisplayName
    val userEmail: StateFlow<String?> = supabaseEmail

    // activeClubId mora no SessionManager (writer via session.updateActiveClubId).
    val activeClubId: StateFlow<String?> = session.activeClubId

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
    fun bumpEngagement() = session.bumpEngagement()

    /** Marca como rated sem mostrar prompt de novo (pra "Agora não, talvez mais tarde" também silenciar). */
    fun dismissRatePromptForever() {
        viewModelScope.launch { dataStoreManager.markAppRated() }
    }

    val fontScale: StateFlow<Float> = dataStoreManager.fontScaleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    fun setFontScale(scale: Float) {
        viewModelScope.launch { dataStoreManager.setFontScale(scale) }
    }

    val themeMode: StateFlow<ThemeMode> = dataStoreManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { dataStoreManager.setThemeMode(mode) }
    }

    /** Registra o token FCM do device pro usuário logado (push F1). Idempotente. */
    fun syncPushToken() {
        com.example.push.PushTokens.sync(viewModelScope)
    }

    /**
     * Intro de primeiro uso ("como funciona"). `null` = ainda lendo do disco
     * (nao renderiza nada pra nao piscar), `false` = mostrar intro, `true` = ja
     * viu, segue pro welcome.
     */
    val introSeen: StateFlow<Boolean?> = dataStoreManager.introSeenFlow
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markIntroSeen() {
        viewModelScope.launch { dataStoreManager.markIntroSeen() }
    }

    /**
     * B1: código de convite capturado no Welcome (antes de criar conta). Fica
     * retido até a auth concluir; aí a UI faz o join automático. Em memória de
     * propósito — se o app fechar no meio, o usuário usa o join de dentro do app.
     */
    private val _pendingInviteCode = MutableStateFlow<String?>(null)
    val pendingInviteCode: StateFlow<String?> = _pendingInviteCode.asStateFlow()
    fun setPendingInviteCode(code: String) {
        _pendingInviteCode.value = code.trim().uppercase().ifBlank { null }
    }
    fun consumePendingInviteCode() { _pendingInviteCode.value = null }

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

    // Clubs — grafo de sessão: aliases do SessionManager (F4b).
    val allClubs: StateFlow<List<Club>> = session.allClubs

    val activeClub: StateFlow<Club?> = session.activeClub

    val currentBooksMap: StateFlow<Map<String, String>> = session.currentBooksMap

    // Current Lendo Agora Book
    val currentBook: StateFlow<Book?> = session.currentBook

    // Chapters for Current Book
    val currentChapters: StateFlow<List<Chapter>> = session.currentChapters

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
    val clubBooks: StateFlow<List<Book>> = session.clubBooks

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

    // Saved Quotes — esconde as removidas por moderação (0010).
    val savedQuotes: StateFlow<List<SavedQuote>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getSavedQuotesForUserFlow(userId).map { qs -> qs.filterNot { it.removido } }
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Livros favoritos do usuário (♥ pessoal, cross-clube). Mesmo padrão de savedQuotes.
    val favoriteBooks: StateFlow<List<Book>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) repository.getFavoriteBooksForUserFlow(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // --- Fase 4 ---
    // Active voting round
    val activeVotingRound: StateFlow<VotingRound?> = session.activeVotingRound

    // Votos da RODADA ATIVA — Room-backed (REATIVO): o voto otimista aparece na
    // hora. Antes a UI usava getVotesForClubFlow, um fetch de rede ÚNICO e
    // NÃO-reativo (MutableStateFlow preenchido uma vez), então votar/"trocar pra
    // esse" não refletia até sair e voltar da tela — parecia que "não ia".
    // Definido DEPOIS de activeVotingRound de propósito (o initializer lê o flow).
    val votesForActiveRound: StateFlow<List<Vote>> = activeVotingRound.flatMapLatest { round ->
        if (round != null) repository.getVotesForRoundFlow(round.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Book suggestions for current club, indexed by bookId — esconde removidas (0010).
    val bookSuggestionsByBookId: StateFlow<Map<String, BookSuggestion>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookSuggestionsForClubFlow(clubId).map { list ->
            list.filterNot { it.removido }.associateBy { it.bookId }
        } else flowOf(emptyMap())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyMap())

    // Next-queue books (status = "next")
    val nextBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) repository.getBookByStatusFlow(clubId, "next") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Papel do usuário atual no clube ativo — aliases do SessionManager (F4b).
    val currentUserPapel: StateFlow<String?> = session.currentUserPapel

    val isCurrentUserAdmin: StateFlow<Boolean> = session.isCurrentUserAdmin

    val isCurrentUserSuperAdmin: StateFlow<Boolean> = session.isCurrentUserSuperAdmin

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

    // ===================== MODERAÇÃO (0010) =====================
    // Usuários que EU bloqueei — as telas escondem o conteúdo desses ids.
    val blockedIds: StateFlow<Set<String>> = currentUserId.flatMapLatest { uid ->
        if (uid != null) repository.observeBlockedIds(uid).map { it.toSet() } else flowOf(emptySet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), emptySet())

    /** Denuncia um conteúdo (chat, citação, resenha, sugestão, perfil). */
    fun reportContent(
        targetType: ReportTargetType,
        targetId: String,
        targetUserId: String,
        motivo: ReportReason,
        detalhe: String?,
        onDone: () -> Unit = {},
    ) {
        val me = currentUserId.value ?: return
        val clubId = activeClubId.value ?: return
        viewModelScope.launch {
            repository.reportContent(me, clubId, targetType, targetId, targetUserId, motivo, detalhe)
            onDone()
        }
    }

    fun blockUser(userId: String, onDone: () -> Unit = {}) {
        val me = currentUserId.value ?: return
        if (me == userId) return
        viewModelScope.launch { repository.blockUser(me, userId); onDone() }
    }

    fun unblockUser(userId: String) {
        val me = currentUserId.value ?: return
        viewModelScope.launch { repository.unblockUser(me, userId) }
    }

    /** Remoção por admin (comentário/citação/resenha/sugestão). */
    fun moderateRemove(
        targetType: ReportTargetType,
        targetId: String,
        targetUserId: String,
        motivo: String? = null,
        onDone: () -> Unit = {},
    ) {
        val me = currentUserId.value ?: return
        val clubId = activeClubId.value ?: return
        viewModelScope.launch {
            repository.moderateRemoveContent(targetType, targetId, targetUserId, clubId, motivo, me)
            onDone()
        }
    }

    // Fila de denúncias pendentes (admin). Carregada sob demanda na tela.
    private val _pendingReports = MutableStateFlow<List<ContentReport>>(emptyList())
    val pendingReports: StateFlow<List<ContentReport>> = _pendingReports.asStateFlow()
    private val _pendingReportsLoading = MutableStateFlow(false)
    val pendingReportsLoading: StateFlow<Boolean> = _pendingReportsLoading.asStateFlow()

    fun refreshPendingReports() {
        val clubId = activeClubId.value ?: return
        viewModelScope.launch {
            _pendingReportsLoading.value = true
            val raw = repository.fetchPendingReports(clubId)
            _pendingReports.value = raw
            _pendingReportsLoading.value = false
        }
    }

    fun dismissReport(reportId: String) {
        viewModelScope.launch { repository.dismissReport(reportId); refreshPendingReports() }
    }

    /** Remove o conteúdo denunciado e marca a denúncia como resolvida. */
    fun resolveReportByRemoving(report: ContentReport) {
        moderateRemove(report.targetType, report.targetId, report.targetUserId, "Denúncia procedente") {
            refreshPendingReports()
        }
    }

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

    // F4b: os 4 observers do antigo init{} (guard de troca de conta, auto-seleção
    // de clube, auto-close de votação, avatar-default) moraram pro SessionManager
    // — rodam no scope do processo, não dependem mais deste VM existir.

    // --- Authentication Actions ---
    // O login real e feito por AuthRepository (email/senha + Google). Esta funcao
    // existe como compat com chamadas antigas (alguns lugares ainda invocam).
    fun login(name: String, email: String, onCompleted: () -> Unit) {
        // 9B: no-op compat. Login real e via AuthRepository nas telas de auth.
        onCompleted()
    }

    // F4b: sem onCleared/repository.close() — a engine é @Singleton do processo
    // (compartilhada com SessionManager e DrainQueueWorker); fechar aqui mataria
    // o backbone offline pros outros consumidores.

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
            session.updateActiveClubId(null)
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
            session.updateActiveClubId(null)
            onCompleted()
        }
    }

    fun selectActiveClub(clubId: String) = session.selectActiveClub(clubId)

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
            session.updateActiveClubId(null)
            onDeleted()
        }
    }

    fun updateUserProfile(nome: String, email: String, avatarUrl: String, pronome: String? = null) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: return@launch
            // email do supabase auth.users e gerido pelo Supabase; aqui so atualiza profile.
            val updatedUser = User(userId, nome, email, avatarUrl, pronome?.trim()?.ifBlank { null })
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
                // B4: timeout pra não travar em "Criando…" se a rede morrer no meio.
                val clubId = withTimeoutOrNull(15_000L) {
                    repository.createClubViaRpc(nome, descricao, cor, privacidade)
                }
                if (clubId == null) {
                    onError("Demorou demais. Verifique a conexão e tente de novo.")
                    return@launch
                }
                session.updateActiveClubId(clubId)
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
                // B4: timeout pra não travar em "Entrando…". Sentinela "" separa
                // "código errado" (RPC devolve null) de "estourou o tempo".
                val raw = withTimeoutOrNull(15_000L) {
                    repository.joinClubWithCodeViaRpc(code) ?: ""
                }
                if (raw == null) {
                    onCompleted(false, "Demorou demais. Verifique a conexão e tente de novo.")
                    return@launch
                }
                val clubId = raw.ifEmpty { null }
                if (clubId != null) {
                    session.updateActiveClubId(clubId)
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
                    onCompleted(false, "Esse código não está certo. Confira com quem te chamou.")
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
            // Reação ÚNICA por pessoa (estilo WhatsApp/Teams): cada um dá no máximo
            // uma. Tocar na própria reação desfaz; escolher outra TROCA (remove a
            // anterior antes de aplicar a nova) — antes o PK incluía emoji e deixava
            // empilhar ❤+🔥+👍 da mesma pessoa.
            val mine = repository.getReactionsForCommentFlow(commentId).first()
                .filter { it.userId == userId }
            val same = mine.find { it.emoji == emoji }
            if (same != null) {
                repository.deleteReaction(same)
            } else {
                mine.forEach { repository.deleteReaction(it) }
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
        viewModelScope.launch { session.maybeAutoCloseExpiredRound() }
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

    // --- Favoritos (♥ pessoal, cross-clube) ---
    fun isBookFavoriteFlow(bookId: String): Flow<Boolean> =
        currentUserId.flatMapLatest { userId ->
            if (userId != null) repository.isBookFavoriteFlow(userId, bookId) else flowOf(false)
        }

    fun toggleBookFavorite(bookId: String, favorite: Boolean) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: return@launch
            repository.setBookFavorite(userId, bookId, favorite)
            if (favorite) bumpEngagement()
        }
    }

    // Abre o detalhe (club-scoped) de um favorito a partir do Perfil: garante um
    // clube ativo que contém o livro antes de navegar, senão o BookDetail não o acha.
    fun openFavoriteBook(bookId: String, navigate: (String) -> Unit) {
        viewModelScope.launch {
            repository.anyClubIdForBook(bookId)?.let { selectActiveClub(it) }
            navigate(bookId)
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

    /** Dedup por ISBN/openlibraryId/título — mora no SessionManager (F4b). */
    private fun existingClubBookId(title: String, isbn: String, openlibraryId: String = ""): String? =
        session.existingClubBookId(title, isbn, openlibraryId)

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
    // F5: pendingMutationsCount/forceRefresh moraram pra ui/sync/SyncViewModel.

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
                session.updateActiveClubId(proximo)
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

    // Busca → define como leitura atual, direto do diálogo "Trocar livro": cria o
    // livro (se novo) e o promove a "current" finalizando o anterior. Reaproveita a
    // dedup (existingClubBookId) e a mesma transição de changeCurrentBookManually.
    fun setSearchedBookAsCurrent(result: com.example.data.search.UnifiedBookResult) {
        viewModelScope.launch {
            val clubId = activeClubId.value ?: return@launch
            val isbn = result.isbn ?: ""
            val existingId = existingClubBookId(result.title, isbn)
            val bookId = existingId ?: UUID.randomUUID().toString()
            if (existingId == null) {
                repository.insertBook(
                    Book(
                        id = bookId, title = result.title, author = result.author,
                        coverUrl = result.coverUrl ?: "", openlibraryId = "", isbn = isbn,
                        isManual = false, totalPaginas = null, editora = null,
                        anoPublicacao = result.firstPublishYear, idioma = "pt"
                    )
                )
                repository.insertClubBook(ClubBook(clubId, bookId, "suggested", 0, null))
            }
            val currentList = repository.getBookByStatusFlow(clubId, "current").first()
            currentList.forEach { b ->
                repository.updateClubBookStatus(clubId, b.id, "finished")
                repository.updateClubBookMeetingDate(clubId, b.id, System.currentTimeMillis())
            }
            repository.updateClubBookStatus(clubId, bookId, "current")
            _searchResults.value = emptyList()
            _searchResultsUnified.value = emptyList()
        }
    }

    // Promove o primeiro da fila a "current" — mora no SessionManager (F4b).
    private suspend fun promoteNextQueuedBook(clubId: String): String? =
        session.promoteNextQueuedBook(clubId)

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
            // Loop avança: o próximo da fila vira o livro atual (se houver).
            promoteNextQueuedBook(clubId)
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
            session.updateActiveClubId(proximo)
            dataStoreManager.setLastActiveClubId(proximo)
        }
    }

    fun unarchiveClub(clubId: String) {
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            val member = repository.getClubMember(clubId, uid) ?: return@launch
            if (member.papel != "super_admin") return@launch
            repository.updateClubArquivado(clubId, false)
            session.updateActiveClubId(clubId)
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

    /**
     * Busca capítulos automaticamente, em cascata (primeiro que acertar vence):
     *  1) Índice compartilhado pela COMUNIDADE (por ISBN) — o que outro clube já
     *     cadastrou serve todo mundo. Melhor fonte pra ficção PT-BR.
     *  2) Open Library table_of_contents — bom pra técnico/inglês.
     *  3) EPUB do Project Gutenberg — clássicos de domínio público.
     *  4) Descrição do Google Books (fraco) — último recurso.
     */
    fun fetchChaptersOnline(book: Book, onResult: (com.example.util.voting.ChapterFetchResult) -> Unit) {
        viewModelScope.launch {
            try {
                val isbn = book.isbn.trim()
                // 1) COMUNIDADE (crowdsourcing por ISBN)
                if (isbn.isNotBlank()) {
                    val community = repository.getChapterTemplate(isbn)
                    if (!community.isNullOrEmpty()) {
                        onResult(com.example.util.voting.ChapterFetchResult.Success(community))
                        return@launch
                    }
                }
                // 2) Open Library
                if (isbn.isNotBlank()) {
                    val edition = runCatching {
                        com.example.data.api.OpenLibraryApi.service.editionByIsbn(isbn)
                    }.getOrNull()
                    val toc = edition?.tableOfContents.orEmpty().map { it.label to it.title }
                    val fromOl = com.example.util.voting.ChapterFetcher.fromOpenLibraryToc(toc)
                    if (fromOl is com.example.util.voting.ChapterFetchResult.Success) {
                        onResult(fromOl)
                        return@launch
                    }
                }
                // 3) EPUB (Project Gutenberg — domínio público)
                val fromEpub = fetchChaptersFromEpub(book)
                if (fromEpub is com.example.util.voting.ChapterFetchResult.Success) {
                    onResult(fromEpub)
                    return@launch
                }
                // 4) Fallback: descrição do Google Books
                val query = if (isbn.isNotBlank()) "isbn:$isbn"
                    else "intitle:${book.title}+inauthor:${book.author}"
                val gb = com.example.data.api.GoogleBooksApi.service.search(query)
                val desc = gb.items?.firstOrNull()?.volumeInfo?.description.orEmpty()
                val candidates = com.example.util.voting.ChapterFetcher.extractFromText(desc)
                onResult(com.example.util.voting.ChapterFetcher.validate(candidates))
            } catch (e: Exception) {
                android.util.Log.e("Rodape/VM", "Operacao falhou silenciosamente", e)
                onResult(com.example.util.voting.ChapterFetchResult.Failed)
            }
        }
    }

    /** Compartilha o índice deste livro (por ISBN) com a comunidade. */
    fun shareChapterTemplate(book: Book, chapters: List<Pair<Int, String>>) {
        val isbn = book.isbn.trim()
        if (isbn.isBlank() || chapters.isEmpty()) return
        viewModelScope.launch {
            val uid = currentUserId.value ?: return@launch
            repository.shareChapterTemplate(isbn, book.title, chapters, uid)
        }
    }

    private val epubHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .callTimeout(java.time.Duration.ofSeconds(25))
            .build()
    }

    // Acha o EPUB no Gutenberg (por título+autor) e extrai o índice. Best-effort.
    private suspend fun fetchChaptersFromEpub(book: Book): com.example.util.voting.ChapterFetchResult =
        runCatching {
            val query = listOf(book.title, book.author).filter { it.isNotBlank() }.joinToString(" ")
            if (query.isBlank()) return com.example.util.voting.ChapterFetchResult.Failed
            val resp = com.example.data.api.GutendexApi.service.search(query)
            val norm = { s: String -> s.lowercase().filter { it.isLetterOrDigit() || it == ' ' } }
            val wantTitle = norm(book.title)
            val candidate = resp.results?.firstOrNull { b ->
                val hasEpub = b.formats?.keys?.any { it.startsWith("application/epub+zip") } == true
                val titleOk = wantTitle.isNotBlank() && (norm(b.title).contains(wantTitle) || wantTitle.contains(norm(b.title)))
                hasEpub && titleOk
            } ?: return com.example.util.voting.ChapterFetchResult.Failed
            val epubUrl = candidate.formats!!.entries.first { it.key.startsWith("application/epub+zip") }.value
            val bytes = downloadBytes(epubUrl) ?: return com.example.util.voting.ChapterFetchResult.Failed
            com.example.util.voting.ChapterFetcher.validate(com.example.util.voting.EpubTocParser.parse(bytes))
        }.getOrDefault(com.example.util.voting.ChapterFetchResult.Failed)

    private suspend fun downloadBytes(url: String, maxBytes: Long = 12_000_000L): ByteArray? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val req = okhttp3.Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; Rodape reading club)")
                    .build()
                epubHttpClient.newCall(req).execute().use { resp ->
                    val body = resp.body
                    if (!resp.isSuccessful || body == null) return@use null
                    val len = body.contentLength()
                    if (len in 1..maxBytes || len == -1L) body.bytes() else null
                }
            }.getOrNull()
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
                    // Loop avança: promove o próximo da fila a atual (se houver).
                    promoteNextQueuedBook(clubId)
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
