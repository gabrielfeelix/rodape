package com.example.data.session

import com.example.data.DataStoreManager
import com.example.data.model.Book
import com.example.data.model.Chapter
import com.example.data.model.Club
import com.example.data.model.User
import com.example.data.model.VotingRound
import com.example.data.remote.AuthRepository
import com.example.data.remote.SyncEngine
import com.example.data.remote.repo.BookRepository
import com.example.data.remote.repo.ClubRepository
import com.example.data.remote.repo.UserRepository
import com.example.data.remote.repo.VotingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================================
// SessionManager — grafo de sessão compartilhado (F4b)
//
// Movido do MainViewModel SEM mudança de lógica: o estado que quase tudo lê
// (currentUserId, activeClubId, activeClub, currentBook, currentChapters,
// papel/admin) + os 4 observers do init + helpers cross-domain. É o nó que
// travava o split de VM por tela (F5): as VMs de tela injetam ISTO + os repos
// que precisam, em vez de todas dependerem do MainViewModel.
//
// @Singleton: sobrevive recreation de Activity (melhor que o by viewModels()
// de antes). Os observers rodam num scope próprio do processo.
// ============================================================================
@Singleton
class SessionManager @Inject internal constructor(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager,
    private val engine: SyncEngine,
    private val userRepo: UserRepository,
    private val clubRepo: ClubRepository,
    private val bookRepo: BookRepository,
    private val votingRepo: VotingRepository,
) {

    // Espelha o viewModelScope de onde este grafo veio (Main.immediate);
    // SupervisorJob: falha de um observer não derruba os outros.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ===================== Tier 1 — a espinha =====================

    // currentUserId derivado direto do Supabase Auth (sem DataStore).
    val currentUserId: StateFlow<String?> = authRepository.currentUserIdFlow
        .stateIn(scope, SharingStarted.Eagerly, null)

    val currentUser: StateFlow<User?> = currentUserId.flatMapLatest { userId ->
        if (userId != null) userRepo.getUserFlow(userId) else flowOf(null)
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), null)

    // activeClubId vira state em memoria. Auto-inicializado com o primeiro clube
    // ao logar; persiste so durante a sessao do app (cold-start cai no primeiro).
    private val _activeClubId = MutableStateFlow<String?>(null)
    val activeClubId: StateFlow<String?> = _activeClubId.asStateFlow()

    /** Escrita crua do clube ativo (login/logout/sair/arquivar). NÃO persiste —
     *  os call sites que persistiam continuam chamando setLastActiveClubId. */
    fun updateActiveClubId(clubId: String?) {
        _activeClubId.value = clubId
    }

    fun selectActiveClub(clubId: String) {
        _activeClubId.value = clubId
        // Persiste pra restaurar no próximo cold start.
        scope.launch { dataStoreManager.setLastActiveClubId(clubId) }
    }

    // ===================== Tier 2 — derivados =====================

    val allClubs: StateFlow<List<Club>> = currentUserId.flatMapLatest { userId ->
        if (userId != null) clubRepo.getClubsForUser(userId) else flowOf(emptyList())
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), emptyList())

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

    // Active voting round (o observer de auto-close lê daqui)
    val activeVotingRound: StateFlow<VotingRound?> = activeClubId.flatMapLatest { clubId ->
        if (clubId != null) votingRepo.getActiveVotingRoundFlow(clubId) else flowOf(null)
    }.stateIn(scope, SharingStarted.WhileSubscribed(60_000), null)

    // ===================== Helpers cross-domain =====================

    /** Chamado em pontos de engajamento (votar, comentar, salvar frase, RSVP, avaliar livro). */
    fun bumpEngagement() {
        scope.launch { dataStoreManager.incrementEngagementCount() }
    }

    /** Dedup: se o clube já tem esse livro (mesmo ISBN, ou mesmo openlibraryId, ou
     *  mesmo título), reusa o bookId em vez de criar um candidato duplicado. Antes,
     *  o mesmo livro sugerido por 3 pessoas virava 3 candidatos de voto separados. */
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

    // Promove o primeiro da fila ("next", ordenado por ordem) a "current". Retorna
    // o id promovido ou null se a fila está vazia. Faz o ciclo AVANÇAR sozinho ao
    // finalizar um livro — antes o clube ficava "sem leitura" com a fila cheia até
    // alguém trocar o livro manualmente em settings.
    suspend fun promoteNextQueuedBook(clubId: String): String? {
        val next = bookRepo.getBookByStatusFlow(clubId, "next").first()
        val head = next.firstOrNull() ?: return null
        bookRepo.updateClubBookStatus(clubId, head.id, "current")
        return head.id
    }

    /** Fecha a rodada ativa se já expirou (RPC faz tudo no servidor). */
    suspend fun maybeAutoCloseExpiredRound() {
        val clubId = activeClubId.value ?: return
        val round = votingRepo.getActiveVotingRound(clubId) ?: return
        if (System.currentTimeMillis() >= round.fechaEm) {
            votingRepo.closeVotingRoundViaRpc(round.id)
        }
    }

    // ===================== Observers do init (4) =====================

    init {
        // Defesa em profundidade contra leak de dados entre contas no mesmo
        // device: quando detectamos que o userId logado e DIFERENTE do que
        // persistimos no ultimo cold-start, limpamos o cache Room antes de
        // qualquer fluxo comecar a ler dele. Cobre 3 cenarios:
        //  1. App morto entre signOut() e clearLocalCache() no logout anterior
        //  2. Conta nova logando em device que ja teve outra conta antes
        //  3. Mesmo userId persistido mas diferente do logado agora
        scope.launch {
            currentUserId.collect { newUserId ->
                if (newUserId == null) return@collect
                val lastId = runCatching { dataStoreManager.lastUserId() }.getOrNull()
                if (lastId != newUserId) {
                    android.util.Log.w(
                        "Rodape/Session",
                        "Troca/primeiro login (last=$lastId, new=$newUserId). Limpando cache local."
                    )
                    // SEM drenar: as mutations pendentes eram do usuario ANTERIOR e
                    // seriam reenviadas sob a sessao do novo (misattribution/RLS-reject).
                    runCatching { engine.clearLocalCacheNoDrain() }
                    runCatching { dataStoreManager.setLastActiveClubId(null) }
                    runCatching { dataStoreManager.setLastUserId(newUserId) }
                }
            }
        }

        // Quando o currentUserId mudar (login/logout), auto-seleciona o primeiro
        // clube do usuario como activeClub. Se ele nao for membro de nenhum,
        // activeClubId fica null e a UI mostra estado vazio com CTA de criar/entrar.
        scope.launch {
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
        scope.launch {
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
        scope.launch {
            currentUser.collect { user ->
                if (user != null && user.avatarUrl == "preset:leitor") {
                    val sugerido = com.example.ui.components.AvatarPicker.pickFor(user.nome)
                    if (sugerido != "preset:leitor") {
                        // Atualiza so o avatar — preserva o resto do profile.
                        userRepo.insertUser(user.copy(avatarUrl = sugerido))
                    }
                }
            }
        }
    }
}
