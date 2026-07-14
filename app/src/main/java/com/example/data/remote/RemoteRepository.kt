package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.*
import com.example.util.MeetingTime
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put


// ============================================================================
// RemoteRepository
//
// Substitui RodapeRepository (Room). Mantem EXATAMENTE as mesmas assinaturas
// publicas pra reduzir o impacto no MainViewModel a uma so troca de instancia.
//
// Estrategia:
//  - Leituras one-shot: `suspend fun ... = supabase.from("X").select { filter { ... } }`.
//  - Leituras reativas (Flow): polling via MutableStateFlow inicializada com select.
//    Pra MVP nao usamos Realtime WebSocket — UI re-le ao chamar a acao.
//    Trabalho de Realtime fica pra fase futura (decisao: simplicidade > vivacidade).
//  - Mutacoes: `supabase.from("X").upsert(...)` ou `.delete { filter { ... } }`.
//  - Operacoes privilegiadas: RPC SECURITY DEFINER (`create_club`, `join_club_with_code`,
//    `promote_member`, etc.).
//
// Erros: propagam excecao. UI ja tem fallback com `?: emptyList()` em quase tudo;
// onde nao tem, a tela fica vazia momentaneamente — sem crash.
// ============================================================================

class RemoteRepository(
    private val appContext: android.content.Context,
    private val supabase: SupabaseClient = Supabase.client,
) {

    // ============================================================
    // INFRA OFFLINE → SyncEngine (F3b)
    // ============================================================
    //
    // O kernel (SWR/TTL + fila offline com os 25 handlers + realtime/reloaders)
    // foi extraído SEM mudança de lógica pra SyncEngine.kt. Os métodos de
    // domínio abaixo continuam chamando os mesmos nomes — aqui só delegamos.
    // escapeJson/stateOf ficam neste arquivo porque só o código de domínio os
    // usa (movem junto com os repos no F3c).

    private val engine = SyncEngine(appContext, supabase)

    // F3c: repos de domínio fatiados (compartilham ESTA engine — a fachada só
    // delega; a API pública pro MainViewModel não muda até o F4b/F5).
    private val progressRepo = com.example.data.remote.repo.OfflineFirstProgressRepository(engine)
    private val notificationRepo = com.example.data.remote.repo.OfflineFirstNotificationRepository(engine)
    private val quoteRepo = com.example.data.remote.repo.OfflineFirstQuoteRepository(engine)
    private val moderationRepo = com.example.data.remote.repo.OfflineFirstModerationRepository(engine)
    private val discussionRepo = com.example.data.remote.repo.OfflineFirstDiscussionRepository(engine)
    private val votingRepo = com.example.data.remote.repo.OfflineFirstVotingRepository(engine)
    private val meetingRepo = com.example.data.remote.repo.OfflineFirstMeetingRepository(engine)
    private val userRepo = com.example.data.remote.repo.OfflineFirstUserRepository(engine)
    private val bookRepo = com.example.data.remote.repo.OfflineFirstBookRepository(engine)

    private val json = engine.json
    private val dao = engine.dao
    private val scope = engine.scope

    suspend fun clearLocalCache() = engine.clearLocalCache()
    suspend fun clearLocalCacheNoDrain() = engine.clearLocalCacheNoDrain()

    private fun markSynced(key: String) = engine.markSynced(key)
    private suspend fun syncOnce(key: String, ttlMs: Long, block: suspend () -> Unit) =
        engine.syncOnce(key, ttlMs, block)

    private suspend fun tryRemoteOrEnqueue(
        kind: String,
        payload: String,
        notifyTable: String? = null,
        block: suspend () -> Unit,
    ) = engine.tryRemoteOrEnqueue(kind, payload, notifyTable, block)

    suspend fun tryDrainPendingQueue(): Int = engine.tryDrainPendingQueue()

    /** StateFlow do tamanho da fila pra UI mostrar badge "X pendentes". */
    val pendingMutationsCount: Flow<Int> = engine.pendingMutationsCount

    suspend fun forceRefresh() = engine.forceRefresh()

    private fun notifyLocalMutation(table: String) = engine.notifyLocalMutation(table)

    private fun ensureRealtime(
        table: String,
        filterColumn: String? = null,
        filterValue: String? = null,
        reload: suspend () -> Unit,
    ) = engine.ensureRealtime(table, filterColumn, filterValue, reload)

    fun close() = engine.close()

    // ----------------------- Caches reativas -----------------------
    // Polling-based: cada Flow tem uma cache MutableStateFlow que e refreshada
    // sob demanda. Acoes de mutacao chamam refresh() depois pra UI atualizar.

    private fun <T> stateOf(initial: T): MutableStateFlow<T> = MutableStateFlow(initial)

    // ============================================================
    // USERS / PROFILES
    // ============================================================

    // F3c: movido pra repo/UserRepository.kt — fachada delega.
    fun getUserFlow(userId: String): Flow<User?> = userRepo.getUserFlow(userId)

    suspend fun getUser(userId: String): User? = userRepo.getUser(userId)

    /** Nao usado hoje (RLS limita visibilidade); mantido como stub vazio. */
    fun getAllUsersFlow(): Flow<List<User>> = userRepo.getAllUsersFlow()

    suspend fun insertUser(user: User) = userRepo.insertUser(user)

    suspend fun updateFontScale(userId: String, scale: Float) = userRepo.updateFontScale(userId, scale)

    // ============================================================
    // CLUBS
    // ============================================================

    fun getClubFlow(clubId: String): Flow<Club?> {
        val reload: suspend () -> Unit = { syncClub(clubId); markSynced("club:$clubId") }
        scope.launch { syncOnce("club:$clubId", Ttl.MED) { syncClub(clubId) } }
        ensureRealtime("clubs", filterColumn = "id", filterValue = clubId, reload = reload)
        return dao.clubFlow(clubId)
    }

    private suspend fun syncClub(clubId: String) {
        val c = runCatching {
            supabase.from("clubs").select {
                filter { eq("id", clubId) }
                limit(1)
            }.decodeSingleOrNull<ClubDto>()?.toDomain()
        }.getOrNull()
        if (c != null) dao.upsertClub(c)
    }

    suspend fun getClub(clubId: String): Club? {
        dao.club(clubId)?.let { return it }
        syncClub(clubId)
        return dao.club(clubId)
    }

    suspend fun getClubByCodigo(codigo: String): Club? = runCatching {
        // Pra entrar em clube por codigo, busca direto no servidor (clube pode
        // nao estar no cache local porque user nao e membro ainda).
        val club = supabase.from("clubs").select {
            filter { eq("codigo", codigo) }
            limit(1)
        }.decodeSingleOrNull<ClubDto>()?.toDomain()
        // Nao cacheamos — RLS deve impedir acesso se nao for membro.
        club
    }.getOrNull()

    fun getClubsForUser(userId: String): Flow<List<Club>> {
        val reload: suspend () -> Unit = { syncClubsForUser(userId); markSynced("clubs:user:$userId") }
        scope.launch { syncOnce("clubs:user:$userId", Ttl.MED) { syncClubsForUser(userId) } }
        ensureRealtime("club_members", filterColumn = "user_id", filterValue = userId, reload = reload)
        // clubs sem filtro dispararia reload a cada mudanca de QUALQUER clube publico;
        // filtra pelo membership via club_members acima. Mantemos clubs filtrado por
        // nada removido — mas com o reload escopado, o custo fica no membership.
        return dao.clubsActiveFlow()
    }

    private suspend fun syncClubsForUser(userId: String) {
        runCatching {
            // Escopa por MEMBERSHIP (join club_members do usuario), em vez de baixar
            // TODOS os clubes que o SELECT policy libera (publicos + os que sou membro).
            // Antes o cliente cacheava clubes publicos que o usuario nunca entrou —
            // poluia o switcher, o empty-state e a escolha de clube ativo.
            val rows = supabase.from("club_members").select(Columns.raw("clubs!inner(*)")) {
                filter { eq("user_id", userId) }
            }.decodeList<JoinClubOnly>()
            val list = rows.map { it.club.toDomain() }
            dao.upsertClubs(list)
            // Substitui: o que sumiu (saiu do clube) some do cache tambem.
            dao.pruneClubsExcept(list.map { it.id })
        }
    }

    @Serializable
    private data class JoinClubOnly(@SerialName("clubs") val club: ClubDto)

    suspend fun getClubsForUserList(userId: String): List<Club> = runCatching {
        // Escopado por membership (nao por "todos nao-arquivados"). Usado pra
        // escolher o proximo clube ativo ao sair/arquivar — nao pode cair num
        // clube publico que o usuario nem e membro.
        supabase.from("club_members").select(Columns.raw("clubs!inner(*)")) {
            filter { eq("user_id", userId) }
        }.decodeList<JoinClubOnly>().map { it.club.toDomain() }.filter { !it.arquivado }
    }.getOrDefault(emptyList())

    suspend fun insertClub(club: Club) {
        // App nao deve mais inserir clube diretamente — usa RPC create_club.
        // Mantido como no-op pra preservar interface.
    }

    /** RPC: create_club. Retorna o UUID do novo clube.
     *
     *  A funcao Postgres `create_club` esta declarada como `RETURNS clubs`, ou
     *  seja, devolve a ROW INTEIRA do novo clube como JSON (nao so o UUID).
     *  Precisamos decodificar como JsonObject e extrair o campo `id`. Bug
     *  anterior tratava o JSON inteiro como string -> filtros HTTP montavam
     *  URL invalida (?id=eq.{json...}) -> 400 Bad Request silencioso e clube
     *  ficava "perdido" pro cliente apesar de criado no banco. */
    suspend fun createClubViaRpc(
        nome: String,
        descricao: String?,
        cor: String,
        privacidade: String,
    ): String {
        val resp = supabase.postgrest.rpc(
            function = "create_club",
            parameters = buildJsonObject {
                put("p_nome", nome)
                put("p_descricao", descricao ?: "")
                put("p_cor", cor)
                put("p_privacidade", privacidade)
            },
        ).data
        return extractIdFromRpcRow(resp)
            ?: error("create_club: resposta sem campo id: $resp")
    }

    /** RPC: join_club_with_code. Retorna ROW de club_members (campo `club_id`).
     *  Mesmo padrao do create_club — extrai o id apos parsear.
     *  PROPAGA excecao (codigo invalido / nao encontrado / sem rede) pra VM
     *  distinguir os casos em vez de mostrar sempre "codigo errado". */
    suspend fun joinClubWithCodeViaRpc(codigo: String): String? {
        val resp = supabase.postgrest.rpc(
            function = "join_club_with_code",
            parameters = buildJsonObject { put("p_codigo", codigo.uppercase().trim()) }
        ).data
        return extractFieldFromRpcRow(resp, "club_id")
    }

    /** Parse helper: pega `id` da row retornada por uma RPC `RETURNS tabela`.
     *  Resposta vem como JSON: '{"id":"uuid","nome":"...",...}'. Em casos
     *  raros vem como array de 1 elemento: '[{"id":"uuid",...}]'. Tolera ambos. */
    private fun extractIdFromRpcRow(raw: String): String? =
        extractFieldFromRpcRow(raw, "id")

    private fun extractFieldFromRpcRow(raw: String, field: String): String? {
        if (raw.isBlank() || raw == "null") return null
        return runCatching {
            val element = json.parseToJsonElement(raw)
            val obj = when (element) {
                is JsonObject -> element
                is kotlinx.serialization.json.JsonArray -> element.firstOrNull() as? JsonObject
                else -> null
            }
            obj?.get(field)?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
        }.getOrNull()
    }

    /** RPC: leave_club. PROPAGA excecao — o caller so pode trocar o clube ativo
     *  APOS confirmar o sucesso. Antes engolia o erro e o app reportava "saiu"
     *  mesmo quando falhava (e o clube reaparecia no proximo sync). */
    suspend fun leaveClubViaRpc(clubId: String) {
        supabase.postgrest.rpc("leave_club", buildJsonObject { put("p_club_id", clubId) })
    }

    // F3c: deleteOwnAccountViaRpc realocado pro UserRepository (é a conta do
    // usuário, não o clube) — fachada delega.
    suspend fun deleteOwnAccountViaRpc() = userRepo.deleteOwnAccountViaRpc()

    /** RPC: regenerate_invite_code. PROPAGA excecao (antes retornava "" e a UI
     *  mostrava "Novo codigo: " em branco). */
    suspend fun regenerateInviteCodeViaRpc(clubId: String): String {
        val resp = supabase.postgrest.rpc(
            "regenerate_invite_code",
            buildJsonObject { put("p_club_id", clubId) }
        ).data
        return resp.trim('"', ' ', '\n')
    }

    /** RPC: promote_member. PROPAGA excecao (rede/RLS) pra UI mostrar o erro em
     *  vez de reportar sucesso falso e nao mudar o papel. */
    suspend fun promoteMemberViaRpc(clubId: String, targetUserId: String) {
        supabase.postgrest.rpc("promote_member", buildJsonObject {
            put("p_club_id", clubId); put("p_target_user_id", targetUserId)
        })
    }

    /** RPC: demote_admin. PROPAGA excecao (ver promote). */
    suspend fun demoteAdminViaRpc(clubId: String, targetUserId: String) {
        supabase.postgrest.rpc("demote_admin", buildJsonObject {
            put("p_club_id", clubId); put("p_target_user_id", targetUserId)
        })
    }

    /** RPC: transfer_super_admin. PROPAGA excecao (invariante super_admin/RLS). */
    suspend fun transferSuperAdminViaRpc(clubId: String, toUserId: String) {
        supabase.postgrest.rpc("transfer_super_admin", buildJsonObject {
            put("p_club_id", clubId); put("p_target_user_id", toUserId)
        })
    }

    /** RPC: remove_member. PROPAGA excecao (ex: admin tentando remover admin) pra
     *  UI mostrar o erro em vez de o toque nao fazer nada silenciosamente. */
    suspend fun removeMemberViaRpc(clubId: String, targetUserId: String, motivo: String?) {
        supabase.postgrest.rpc("remove_member", buildJsonObject {
            put("p_club_id", clubId)
            put("p_target_user_id", targetUserId)
            put("p_motivo", motivo ?: "")
        })
    }

    // F3c: closeVotingRoundViaRpc realocado pro VotingRepository (estava
    // fisicamente em CLUBS — mapa §2 nota) — fachada delega.
    suspend fun closeVotingRoundViaRpc(roundId: String) = votingRepo.closeVotingRoundViaRpc(roundId)

    // ============================================================
    // CLUB MEMBERS
    // ============================================================

    suspend fun insertClubMember(member: ClubMember) {
        // Nao inserimos membro diretamente — RPCs (create_club / join_club_with_code)
        // ja cuidam. Manter no-op preserva interface.
    }

    fun getClubMembersFlow(clubId: String): Flow<List<User>> {
        val key = "members:$clubId"
        val reload: suspend () -> Unit = { syncClubMembers(clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.MED) { syncClubMembers(clubId) } }
        ensureRealtime("club_members", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.memberUsersInClubFlow(clubId)
    }

    private suspend fun syncClubMembers(clubId: String) {
        runCatching {
            val rows = supabase.from("club_members").select(Columns.raw("user_id, papel, entrou_em, profiles!inner(*)")) {
                filter { eq("club_id", clubId) }
            }.decodeList<JoinMemberProfile>()
            val users = rows.map { it.profile.toDomain() }
            val members = rows.map { row ->
                ClubMember(
                    clubId = clubId,
                    userId = row.userId,
                    papel = row.papel ?: "member",
                    entrouEm = row.entrouEm.fromIso(),
                )
            }
            dao.replaceMembersInClub(clubId, members, users)
        }
    }

    @Serializable
    private data class JoinMemberProfile(
        @SerialName("user_id") val userId: String,
        val papel: String? = null,
        @SerialName("entrou_em") val entrouEm: String? = null,
        @SerialName("profiles") val profile: ProfileDto,
    )

    suspend fun getClubMember(clubId: String, userId: String): ClubMember? {
        dao.member(clubId, userId)?.let { return it }
        return runCatching {
            supabase.from("club_members").select {
                filter {
                    eq("club_id", clubId)
                    eq("user_id", userId)
                }
                limit(1)
            }.decodeSingleOrNull<ClubMemberDto>()?.toDomain()
                ?.also { dao.upsertMembers(listOf(it)) }
        }.getOrNull()
    }

    suspend fun getClubMembersListOrderedByJoin(clubId: String): List<ClubMember> = runCatching {
        supabase.from("club_members").select {
            filter { eq("club_id", clubId) }
            order("entrou_em", Order.ASCENDING)
        }.decodeList<ClubMemberDto>().map { it.toDomain() }
    }.getOrDefault(emptyList())

    fun getClubMembersRawFlow(clubId: String): Flow<List<ClubMember>> {
        val reload: suspend () -> Unit = { syncClubMembers(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("club_members", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.membersInClubFlow(clubId)
    }

    suspend fun updateMemberPapel(clubId: String, userId: String, papel: String) {
        // RLS bloqueia update direto de papel — use as RPCs *ViaRpc.
        // Caminho legado: o MainViewModel chama updateMemberPapel diretamente em alguns
        // fluxos (transferir super_admin). Mantemos um update direto que so funciona se
        // o usuario tiver permissao via RLS (caller_role super_admin).
        runCatching {
            supabase.from("club_members").update({ set("papel", papel) }) {
                filter {
                    eq("club_id", clubId)
                    eq("user_id", userId)
                }
            }
            notifyLocalMutation("club_members")
        }
    }

    suspend fun deleteClubMember(clubId: String, userId: String) {
        runCatching {
            supabase.from("club_members").delete {
                filter {
                    eq("club_id", clubId)
                    eq("user_id", userId)
                }
            }
            notifyLocalMutation("club_members")
        }
    }

    suspend fun insertMemberRemoval(removal: MemberRemoval) {
        // RPC remove_member ja cuida disso. Mantido no-op.
    }

    fun getMemberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>> {
        scope.launch {
            runCatching {
                val list = supabase.from("member_removals").select {
                    filter { eq("club_id", clubId) }
                    order("removed_at", Order.DESCENDING)
                }.decodeList<MemberRemovalDto>().map { it.toDomain() }
                dao.upsertMemberRemovals(list)
            }
        }
        return dao.memberRemovalsForClubFlow(clubId)
    }

    // ============================================================
    // CLUB ADMIN
    // ============================================================

    suspend fun updateClubInfo(clubId: String, nome: String, descricao: String, cor: String, privacidade: String) {
        runCatching {
            supabase.from("clubs").update({
                set("nome", nome)
                set("descricao", descricao)
                set("cor", cor)
                set("privacidade", privacidade)
            }) { filter { eq("id", clubId) } }
        }
    }

    suspend fun updateClubCodigo(clubId: String, codigo: String) {
        // Use a RPC regenerateInviteCodeViaRpc em vez deste path.
        runCatching {
            supabase.from("clubs").update({ set("codigo", codigo) }) {
                filter { eq("id", clubId) }
            }
        }
    }

    suspend fun updateClubArquivado(clubId: String, arquivado: Boolean) {
        runCatching {
            supabase.from("clubs").update({ set("arquivado", arquivado) }) {
                filter { eq("id", clubId) }
            }
        }
    }

    fun getArchivedClubsForUserFlow(userId: String): Flow<List<Club>> {
        scope.launch {
            runCatching {
                val list = supabase.from("clubs").select {
                    filter { eq("arquivado", true) }
                }.decodeList<ClubDto>().map { it.toDomain() }
                dao.upsertClubs(list)
            }
        }
        return dao.clubsArchivedFlow()
    }

    // ============================================================
    // BOOKS / CLUB_BOOKS
    // ============================================================

    // F3c: movido pra repo/BookRepository.kt (books/club_books/chapters) — fachada delega.
    suspend fun insertBook(book: Book) = bookRepo.insertBook(book)

    suspend fun uploadBookCover(clubId: String, bookId: String, bytes: ByteArray): String? =
        bookRepo.uploadBookCover(clubId, bookId, bytes)

    suspend fun insertClubBook(clubBook: ClubBook) = bookRepo.insertClubBook(clubBook)

    fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>> =
        bookRepo.getBookByStatusFlow(clubId, status)

    fun getClubBooksFlow(clubId: String): Flow<List<Book>> = bookRepo.getClubBooksFlow(clubId)

    suspend fun getClubBookStatus(clubId: String, bookId: String): String? =
        bookRepo.getClubBookStatus(clubId, bookId)

    suspend fun getBook(id: String): Book? = bookRepo.getBook(id)

    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String) =
        bookRepo.updateClubBookStatus(clubId, bookId, status)

    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?) =
        bookRepo.updateClubBookMeetingDate(clubId, bookId, dataEncontro)

    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> =
        bookRepo.getClubBooksByStatusFlow(clubId, status)

    suspend fun deleteClubBook(clubId: String, bookId: String) = bookRepo.deleteClubBook(clubId, bookId)

    suspend fun insertChapters(chapters: List<Chapter>) = bookRepo.insertChapters(chapters)

    fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>> =
        bookRepo.getChaptersForBookFlow(bookId)

    suspend fun deleteChaptersForBook(bookId: String) = bookRepo.deleteChaptersForBook(bookId)

    suspend fun saveChapters(bookId: String, chapters: List<Chapter>) =
        bookRepo.saveChapters(bookId, chapters)

    suspend fun getChapterTemplate(isbn: String): List<Pair<Int, String>>? =
        bookRepo.getChapterTemplate(isbn)

    suspend fun shareChapterTemplate(isbn: String, tituloLivro: String, chapters: List<Pair<Int, String>>, userId: String) =
        bookRepo.shareChapterTemplate(isbn, tituloLivro, chapters, userId)

    // ============================================================
    // USER PROGRESS
    // ============================================================

    // F3c: movido pra repo/ProgressRepository.kt — fachada delega.
    suspend fun insertUserProgress(progress: UserProgress) = progressRepo.insertUserProgress(progress)

    fun getUserProgressFlow(userId: String, clubId: String, bookId: String): Flow<UserProgress?> =
        progressRepo.getUserProgressFlow(userId, clubId, bookId)

    suspend fun getUserProgress(userId: String, clubId: String, bookId: String): UserProgress? =
        progressRepo.getUserProgress(userId, clubId, bookId)

    fun getAllProgressForClubFlow(clubId: String): Flow<List<UserProgress>> =
        progressRepo.getAllProgressForClubFlow(clubId)

    // ============================================================
    // COMMENTS / REACTIONS
    // ============================================================

    // F3c: movido pra repo/DiscussionRepository.kt — fachada delega.
    suspend fun insertComment(comment: Comment) = discussionRepo.insertComment(comment)

    fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>> =
        discussionRepo.getCommentsForChapterFlow(chapterId, clubId)

    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>> =
        discussionRepo.getCommentsForBookFlow(bookId, clubId)

    suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String) =
        discussionRepo.softRemoveComment(commentId, removidoPor, motivo)

    suspend fun restoreComment(commentId: String) = discussionRepo.restoreComment(commentId)

    suspend fun editOwnComment(commentId: String, novoTexto: String) =
        discussionRepo.editOwnComment(commentId, novoTexto)

    suspend fun deleteOwnComment(commentId: String) = discussionRepo.deleteOwnComment(commentId)

    // ============================================================
    // MODERAÇÃO — denúncia, bloqueio, remoção (migration 0010)
    // ============================================================

    // F3c: movido pra repo/ModerationRepository.kt — fachada delega.
    suspend fun reportContent(
        reporterId: String,
        clubId: String,
        targetType: ReportTargetType,
        targetId: String,
        targetUserId: String,
        motivo: ReportReason,
        detalhe: String?,
    ) = moderationRepo.reportContent(reporterId, clubId, targetType, targetId, targetUserId, motivo, detalhe)

    suspend fun blockUser(me: String, blockedId: String) = moderationRepo.blockUser(me, blockedId)

    suspend fun unblockUser(me: String, blockedId: String) = moderationRepo.unblockUser(me, blockedId)

    fun observeBlockedIds(me: String): Flow<List<String>> = moderationRepo.observeBlockedIds(me)

    fun isBlockedFlow(me: String, other: String): Flow<Boolean> = moderationRepo.isBlockedFlow(me, other)

    suspend fun moderateRemoveContent(
        type: ReportTargetType,
        targetId: String,
        targetUserId: String,
        clubId: String,
        motivo: String?,
        removidoPor: String,
    ) = moderationRepo.moderateRemoveContent(type, targetId, targetUserId, clubId, motivo, removidoPor)

    suspend fun fetchPendingReports(clubId: String): List<ContentReport> =
        moderationRepo.fetchPendingReports(clubId)

    suspend fun dismissReport(reportId: String) = moderationRepo.dismissReport(reportId)

    fun getRemovedCommentsForClubFlow(clubId: String): Flow<List<Comment>> =
        moderationRepo.getRemovedCommentsForClubFlow(clubId)

    // ---- reactions ----

    suspend fun insertReaction(reaction: Reaction) = discussionRepo.insertReaction(reaction)

    suspend fun deleteReaction(reaction: Reaction) = discussionRepo.deleteReaction(reaction)

    fun getReactionsForCommentFlow(commentId: String): Flow<List<Reaction>> =
        discussionRepo.getReactionsForCommentFlow(commentId)

    fun getReactionsForChapterFlow(chapterId: String): Flow<List<Reaction>> =
        discussionRepo.getReactionsForChapterFlow(chapterId)

    // IdOnlyDto agora é internal em RemoteDtos.kt (F3c — compartilhado por 4 domínios).

    // ============================================================
    // VOTES & VOTING ROUNDS
    // ============================================================

    // F3c: movido pra repo/VotingRepository.kt — fachada delega.
    suspend fun insertVote(vote: Vote) = votingRepo.insertVote(vote)

    suspend fun setUserVoteInRound(userId: String, roundId: String, bookId: String) =
        votingRepo.setUserVoteInRound(userId, roundId, bookId)

    suspend fun clearVotesForUserInClub(userId: String, clubId: String) =
        votingRepo.clearVotesForUserInClub(userId, clubId)

    fun getVotesForClubFlow(clubId: String): Flow<List<Vote>> = votingRepo.getVotesForClubFlow(clubId)

    fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>> = votingRepo.getVotesForRoundFlow(roundId)

    suspend fun getVotesForRound(roundId: String): List<Vote> = votingRepo.getVotesForRound(roundId)

    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String) =
        votingRepo.removeUserVoteForBookInRound(userId, roundId, bookId)

    suspend fun countUserVotesInRound(userId: String, roundId: String): Int =
        votingRepo.countUserVotesInRound(userId, roundId)

    suspend fun insertVotingRound(round: VotingRound) = votingRepo.insertVotingRound(round)

    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?> =
        votingRepo.getActiveVotingRoundFlow(clubId)

    suspend fun getActiveVotingRound(clubId: String): VotingRound? =
        votingRepo.getActiveVotingRound(clubId)

    suspend fun closeVotingRound(id: String, vencedoresJson: String) =
        votingRepo.closeVotingRound(id, vencedoresJson)

    // ============================================================
    // BOOK SUGGESTIONS
    // ============================================================

    // F3c: book_suggestions moraram pro VotingRepository (alimentam a rodada;
    // deleteVotesForBook muta votes) — fachada delega.
    suspend fun insertBookSuggestion(suggestion: BookSuggestion) =
        votingRepo.insertBookSuggestion(suggestion)

    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?> =
        votingRepo.getBookSuggestionFlow(bookId, clubId)

    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>> =
        votingRepo.getBookSuggestionsForClubFlow(clubId)

    suspend fun deleteBookSuggestion(bookId: String, clubId: String) =
        votingRepo.deleteBookSuggestion(bookId, clubId)

    suspend fun deleteVotesForBook(bookId: String) = votingRepo.deleteVotesForBook(bookId)

    // ============================================================
    // MEETINGS
    // ============================================================

    // F3c: movido pra repo/MeetingRepository.kt — fachada delega.
    suspend fun insertMeeting(meeting: Meeting) = meetingRepo.insertMeeting(meeting)

    fun getLatestMeetingFlow(clubId: String): Flow<Meeting?> = meetingRepo.getLatestMeetingFlow(clubId)

    fun getAllMeetingsFlow(clubId: String): Flow<List<Meeting>> = meetingRepo.getAllMeetingsFlow(clubId)

    fun getScheduledMeetingsForClubFlow(clubId: String): Flow<List<Meeting>> =
        meetingRepo.getScheduledMeetingsForClubFlow(clubId)

    suspend fun getMeetingById(meetingId: String): Meeting? = meetingRepo.getMeetingById(meetingId)

    fun getMeetingByIdFlow(meetingId: String): Flow<Meeting?> = meetingRepo.getMeetingByIdFlow(meetingId)

    fun getMeetingsForBookFlow(clubId: String, bookId: String): Flow<List<Meeting>> =
        meetingRepo.getMeetingsForBookFlow(clubId, bookId)

    suspend fun getMeetingsForBookList(clubId: String, bookId: String): List<Meeting> =
        meetingRepo.getMeetingsForBookList(clubId, bookId)

    suspend fun updateMeetingStatus(meetingId: String, status: String) =
        meetingRepo.updateMeetingStatus(meetingId, status)

    suspend fun deleteMeeting(meetingId: String) = meetingRepo.deleteMeeting(meetingId)

    suspend fun deleteRsvpsForMeeting(meetingId: String) = meetingRepo.deleteRsvpsForMeeting(meetingId)

    suspend fun insertMeetingRsvp(rsvp: MeetingRsvp) = meetingRepo.insertMeetingRsvp(rsvp)

    fun getRsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>> =
        meetingRepo.getRsvpsForMeetingFlow(meetingId)

    fun getRsvpForMeetingOfUserFlow(meetingId: String, userId: String): Flow<MeetingRsvp?> =
        meetingRepo.getRsvpForMeetingOfUserFlow(meetingId, userId)

    suspend fun insertMeetingPattern(pattern: MeetingPattern) = meetingRepo.insertMeetingPattern(pattern)

    suspend fun generateMeetingsFromPattern(pattern: MeetingPattern, horizon: Int = 8) =
        meetingRepo.generateMeetingsFromPattern(pattern, horizon)

    fun getActiveMeetingPatternFlow(clubId: String): Flow<MeetingPattern?> =
        meetingRepo.getActiveMeetingPatternFlow(clubId)

    suspend fun getActiveMeetingPattern(clubId: String): MeetingPattern? =
        meetingRepo.getActiveMeetingPattern(clubId)

    suspend fun deactivateMeetingPatterns(clubId: String) = meetingRepo.deactivateMeetingPatterns(clubId)

    suspend fun insertMeetingMinutes(minutes: MeetingMinutes) = meetingRepo.insertMeetingMinutes(minutes)

    fun getMeetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?> =
        meetingRepo.getMeetingMinutesFlow(meetingId)

    suspend fun insertMeetingNote(note: MeetingNote) = meetingRepo.insertMeetingNote(note)

    fun getMeetingNoteFlow(meetingId: String, userId: String): Flow<MeetingNote?> =
        meetingRepo.getMeetingNoteFlow(meetingId, userId)

    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    // F3c: movido pra repo/NotificationRepository.kt — fachada delega.
    suspend fun insertNotification(notification: DbNotification) =
        notificationRepo.insertNotification(notification)

    suspend fun markAllNotificationsAsRead(userId: String) =
        notificationRepo.markAllNotificationsAsRead(userId)

    suspend fun markNotificationAsRead(id: String) = notificationRepo.markNotificationAsRead(id)

    fun getNotificationsFlow(userId: String): Flow<List<DbNotification>> =
        notificationRepo.getNotificationsFlow(userId)

    // ============================================================
    // SAVED QUOTES
    // ============================================================

    // F3c: movido pra repo/QuoteRepository.kt — fachada delega.
    suspend fun insertSavedQuote(quote: SavedQuote) = quoteRepo.insertSavedQuote(quote)

    suspend fun deleteSavedQuote(quote: SavedQuote) = quoteRepo.deleteSavedQuote(quote)

    fun getSavedQuotesForUserFlow(userId: String): Flow<List<SavedQuote>> =
        quoteRepo.getSavedQuotesForUserFlow(userId)

    fun getSavedQuotesForBookFlow(userId: String, bookId: String): Flow<List<SavedQuote>> =
        quoteRepo.getSavedQuotesForBookFlow(userId, bookId)

    // ============================================================
    // BOOK SUMMARIES / RATINGS
    // ============================================================

    // F3c: movido pra repo/BookRepository.kt (summaries/ratings/favorites) — fachada delega.
    suspend fun insertBookSummary(summary: BookSummary) = bookRepo.insertBookSummary(summary)

    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?> =
        bookRepo.getBookSummaryFlow(bookId, clubId)

    suspend fun insertBookRating(rating: BookRating) = bookRepo.insertBookRating(rating)

    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>> =
        bookRepo.getBookRatingsFlow(bookId, clubId)

    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?> =
        bookRepo.getBookRatingOfUserFlow(bookId, clubId, userId)

    suspend fun setBookFavorite(userId: String, bookId: String, favorite: Boolean) =
        bookRepo.setBookFavorite(userId, bookId, favorite)

    fun isBookFavoriteFlow(userId: String, bookId: String): Flow<Boolean> =
        bookRepo.isBookFavoriteFlow(userId, bookId)

    suspend fun anyClubIdForBook(bookId: String): String? = bookRepo.anyClubIdForBook(bookId)

    fun getFavoriteBooksForUserFlow(userId: String): Flow<List<Book>> =
        bookRepo.getFavoriteBooksForUserFlow(userId)

    // ============================================================
    // SEED — no-op (app nasce vazio em producao)
    // ============================================================

    suspend fun seedDatabase() {
        // 9B: app nasce vazio em producao. Nenhum seed.
    }
}
