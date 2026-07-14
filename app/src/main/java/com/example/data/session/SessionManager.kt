package com.example.data.session

import com.example.data.DataStoreManager
import com.example.data.model.*
import com.example.data.remote.AuthRepository
import com.example.data.remote.SyncEngine
import com.example.data.remote.repo.BookRepository
import com.example.data.remote.repo.ClubRepository
import com.example.data.remote.repo.UserRepository
import com.example.data.remote.repo.VotingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Grafo de sessão compartilhado — o "nó" que quase toda tela lê.
 *
 * Segura o estado de sessão (currentUserId/activeClubId + derivados de clube/livro/papel),
 * os observers de sessão do antigo `MainViewModel.init` e os helpers cross-domain.
 *
 * F5.1: virou `@Singleton` com scope próprio (sobrevive à recreation da Activity — melhor que
 * o `by viewModels()` de antes) e injeta as interfaces de repo fatiadas do F3c (User/Club/Book/
 * Voting) + o SyncEngine @Singleton, em vez da fachada RemoteRepository. Android-free (testável
 * sem Robolectric). Comportamento dos flows é o mesmo — só a origem (repo injetado) e o scope
 * (app em vez de VM) mudaram.
 */
@Singleton
internal class SessionManager @Inject constructor(
    private val userRepo: UserRepository,
    private val clubRepo: ClubRepository,
    private val bookRepo: BookRepository,
    private val votingRepo: VotingRepository,
    private val engine: SyncEngine,
    private val dataStoreManager: DataStoreManager,
    private val authRepository: AuthRepository,
) {
    // Scope da sessão: vive pelo processo (Main.immediate, como era o viewModelScope). Não é
    // cancelado por clear de VM — o SessionManager @Singleton persiste entre recreations.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // currentUserId derivado direto do Supabase Auth (sem DataStore). Eager: usado em quase
    // tudo, precisa de valor imediato.
    val currentUserId: StateFlow<String?> = authRepository.currentUserIdFlow
        .stateIn(scope, SharingStarted.Eagerly, null)

    val currentUser: StateFlow<User?> = currentUserId.flatMapLatest { userId ->
        if (userId != null) userRepo.getUserFlow(userId) else flowOf(null)
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), null)

    // Clubs
    val allClubs: StateFlow<List<Club>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) clubRepo.getClubsForUser(userId) else flowOf(emptyList())
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // activeClubId vira state em memoria. Auto-inicializado com o primeiro clube ao logar;
    // persiste so durante a sessao do app (cold-start cai no primeiro).
    private val _activeClubId = MutableStateFlow<String?>(null)
    val activeClubId: StateFlow<String?> = _activeClubId.asStateFlow()

    val activeClub: StateFlow<Club?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) clubRepo.getClubFlow(clubId) else flowOf(null)
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), null)

    val currentBooksMap: StateFlow<Map<String, String>> = currentUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyMap())
        } else {
            clubRepo.getClubsForUser(userId).flatMapLatest { clubs ->
                if (clubs.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val flowsList = clubs.map { club ->
                        bookRepo.getBookByStatusFlow(club.id, "current").map { books ->
                            club.id to (books.firstOrNull()?.title ?: "Sem livro atual")
                        }
                    }
                    combine(flowsList) { pairs ->
                        pairs.toMap()
                    }
                }
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), emptyMap())

    // Current Lendo Agora Book
    val currentBook: StateFlow<Book?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) {
            bookRepo.getBookByStatusFlow(clubId, "current").map { it.firstOrNull() }
        } else {
            flowOf(null)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), null)

    // Chapters for Current Book
    val currentChapters: StateFlow<List<Chapter>> = currentBook.flatMapLatest { book ->
        if (book != null) bookRepo.getChaptersForBookFlow(book.id) else flowOf(emptyList())
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Club Books
    val clubBooks: StateFlow<List<Book>> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) bookRepo.getClubBooksFlow(clubId) else flowOf(emptyList())
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), emptyList())

    // Papel do usuário atual no clube ativo: "super_admin" | "admin" | "member" | null.
    // Emite null IMEDIATAMENTE ao trocar de clube (senão o stateIn segurava o papel do
    // clube ANTERIOR até o novo flow emitir — guards de admin e header piscavam).
    val currentUserPapel: StateFlow<String?> = combine(currentUserId, activeClubId) { uid, cid -> Pair(uid, cid) }
        .flatMapLatest { (uid, cid) ->
            if (uid != null && cid != null) {
                flow<String?> {
                    emit(null) // reset ao trocar de clube
                    emitAll(
                        clubRepo.getClubMembersRawFlow(cid).map { list ->
                            list.find { it.userId == uid }?.papel
                        }
                    )
                }
            } else flowOf(null)
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(60_000), null)

    val isCurrentUserAdmin: StateFlow<Boolean> = currentUserPapel
        .map { it == "admin" || it == "super_admin" }
        .stateIn(scope, SharingStarted.WhileSubscribed(60_000), false)

    val isCurrentUserSuperAdmin: StateFlow<Boolean> = currentUserPapel
        .map { it == "super_admin" }
        .stateIn(scope, SharingStarted.WhileSubscribed(60_000), false)

    // Active voting round (session Tier 2) — o observer de auto-close abaixo depende dele.
    val activeVotingRound: StateFlow<VotingRound?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) votingRepo.getActiveVotingRoundFlow(clubId) else flowOf(null)
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), null)

    init {
        // App nasce vazio em producao — sem seed, sem auto-login fake.
        //
        // Defesa em profundidade contra leak de dados entre contas no mesmo device: quando
        // detectamos que o userId logado e DIFERENTE do que persistimos no ultimo cold-start,
        // limpamos o cache Room antes de qualquer fluxo comecar a ler dele.
        scope.launch {
            currentUserId.collect { newUserId ->
                if (newUserId == null) return@collect
                val lastId = runCatching { dataStoreManager.lastUserId() }.getOrNull()
                if (lastId != newUserId) {
                    android.util.Log.w(
                        "Rodape/VM",
                        "Troca/primeiro login (last=$lastId, new=$newUserId). Limpando cache local."
                    )
                    // SEM drenar: as mutations pendentes eram do usuario ANTERIOR e seriam
                    // reenviadas sob a sessao do novo (misattribution/RLS-reject).
                    runCatching { engine.clearLocalCacheNoDrain() }
                    runCatching { dataStoreManager.setLastActiveClubId(null) }
                    runCatching { dataStoreManager.setLastUserId(newUserId) }
                }
            }
        }

        // Quando o currentUserId mudar (login/logout), auto-seleciona o primeiro clube do
        // usuario como activeClub. Se ele nao for membro de nenhum, activeClubId fica null e
        // a UI mostra estado vazio com CTA de criar/entrar.
        scope.launch {
            allClubs.collect { clubs ->
                val current = _activeClubId.value
                if (clubs.isEmpty()) {
                    _activeClubId.value = null
                } else if (current == null || clubs.none { it.id == current }) {
                    // Restaura o último clube selecionado (se ainda é membro), em vez de sempre
                    // cair no primeiro. Power user volta pro clube onde estava.
                    val saved = dataStoreManager.lastActiveClubId()
                    _activeClubId.value = clubs.firstOrNull { it.id == saved }?.id
                        ?: clubs.first().id
                }
            }
        }

        // Auto-close reativo: fecha a rodada assim que ela vira "expirada", em vez de um
        // delay(500) fixo que rodava com activeClubId ainda null (nao fechava).
        scope.launch {
            activeVotingRound.collect { round ->
                if (round != null && round.status == "aberta" &&
                    System.currentTimeMillis() >= round.fechaEm
                ) {
                    maybeAutoCloseExpiredRound()
                }
            }
        }
    }

    // --- Writers de sessão ---

    /**
     * Set cru do clube ativo, SEM persistir (createClub/join/leave/archive/unarchive já
     * escolhem o próximo em memória; logout/delete passam null). Persistência só no
     * [selectActiveClub], que é a escolha explícita do usuário.
     */
    fun setActiveClub(clubId: String?) {
        _activeClubId.value = clubId
    }

    fun selectActiveClub(clubId: String) {
        _activeClubId.value = clubId
        // Persiste pra restaurar no próximo cold start.
        scope.launch { dataStoreManager.setLastActiveClubId(clubId) }
    }

    // --- Helpers cross-domain ---

    /** Chamado em pontos de engajamento (votar, comentar, salvar frase, RSVP, avaliar livro). */
    fun bumpEngagement() {
        scope.launch { dataStoreManager.incrementEngagementCount() }
    }

    /**
     * Dedup: se o clube já tem esse livro (mesmo ISBN, ou mesmo openlibraryId, ou mesmo
     * título), reusa o bookId em vez de criar um candidato duplicado.
     */
    fun existingClubBookId(title: String, isbn: String, openlibraryId: String = ""): String? {
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

    suspend fun promoteNextQueuedBook(clubId: String): String? {
        val next = bookRepo.getBookByStatusFlow(clubId, "next").first()
        val head = next.firstOrNull() ?: return null
        bookRepo.updateClubBookStatus(clubId, head.id, "current")
        return head.id
    }

    /** Auto-close usado pelo observer de rodada expirada. Mesma ação do RPC server-side. */
    private suspend fun maybeAutoCloseExpiredRound() {
        val clubId = activeClubId.value ?: return
        val round = votingRepo.getActiveVotingRound(clubId) ?: return
        if (System.currentTimeMillis() >= round.fechaEm) {
            // RPC close_voting_round faz tudo no servidor (finished + promove + notifica + fecha).
            votingRepo.closeVotingRoundViaRpc(round.id)
        }
    }
}
