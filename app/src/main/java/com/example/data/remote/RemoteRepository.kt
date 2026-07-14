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

    fun getUserFlow(userId: String): Flow<User?> {
        // Reload manual (via realtime/mutation) ignora TTL — sempre re-busca.
        val reload: suspend () -> Unit = { syncUser(userId); markSynced("user:$userId") }
        // Trigger on view: respeita TTL.
        scope.launch { syncOnce("user:$userId", Ttl.SLOW) { syncUser(userId) } }
        ensureRealtime("profiles", filterColumn = "id", filterValue = userId, reload = reload)
        return dao.userFlow(userId)
    }

    private suspend fun syncUser(userId: String) {
        val u = runCatching {
            supabase.from("profiles").select {
                filter { eq("id", userId) }
                limit(1)
            }.decodeSingleOrNull<ProfileDto>()?.toDomain()
        }.getOrNull()
        if (u != null) dao.upsertUser(u)
    }

    suspend fun getUser(userId: String): User? {
        // Snapshot: prefere Room (rapido), busca remoto se nao tem.
        dao.user(userId)?.let { return it }
        syncUser(userId)
        return dao.user(userId)
    }

    /** Nao usado hoje (RLS limita visibilidade); mantido como stub vazio. */
    fun getAllUsersFlow(): Flow<List<User>> = kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertUser(user: User) {
        // Apenas atualiza profile do USUARIO LOGADO (RLS bloqueia outros).
        // Para criar profile de outro usuario o trigger handle_new_user e quem faz.
        //
        // Local-first: grava no Room ANTES do remoto e enfileira se offline. Antes
        // era remoto-primeiro dentro de runCatching SEM escrita local — editar o
        // perfil (nome/avatar) offline sumia sem aviso e a saudacao nao mudava.
        val partes = user.nome.trim().split(" ", limit = 2)
        val nome = partes.firstOrNull()?.ifBlank { user.nome } ?: user.nome
        val sobrenome = if (partes.size > 1) partes[1].trim().ifBlank { null } else null
        val avatarKey = user.avatarUrl.ifBlank { "preset:leitor" }
        val pronome = user.pronome?.trim()?.ifBlank { null }
        dao.upsertUser(user.copy(avatarUrl = avatarKey, pronome = pronome))
        notifyLocalMutation("profiles")
        val payload = buildJsonObject {
            put("id", user.id)
            put("nome", nome)
            if (sobrenome != null) put("sobrenome", sobrenome)
            put("avatarKey", avatarKey)
            if (pronome != null) put("pronome", pronome)
        }.toString()
        tryRemoteOrEnqueue("upsert_profile", payload) {
            supabase.from("profiles").upsert(
                ProfileUpdateDto(id = user.id, nome = nome, sobrenome = sobrenome, avatarKey = avatarKey, pronome = pronome)
            )
        }
    }

    suspend fun updateFontScale(userId: String, scale: Float) {
        runCatching {
            supabase.from("profiles").update({ set("font_scale", scale) }) {
                filter { eq("id", userId) }
            }
        }
    }

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

    /**
     * Exclui a propria conta via RPC SECURITY DEFINER `delete_own_account`
     * (apaga dados do usuario + auth.users). O RPC precisa existir no Supabase
     * — SQL documentado em docs/release/account-deletion.sql. Propaga a exceptin
     * pra UI decidir o fallback (email) se o RPC ainda nao estiver criado.
     */
    suspend fun deleteOwnAccountViaRpc() {
        supabase.postgrest.rpc("delete_own_account")
    }

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

    suspend fun closeVotingRoundViaRpc(roundId: String) {
        runCatching {
            supabase.postgrest.rpc("close_voting_round", buildJsonObject {
                put("p_round_id", roundId)
            })
        }
    }

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

    suspend fun insertBook(book: Book) {
        // Optimistic local-first: escreve no Room ANTES de tentar Supabase pra
        // UI nunca ficar "fantasma" (livro sumiu so porque rede falhou ou rate
        // limit do Supabase respondeu 429). Se o remoto falhar, loga e segue —
        // a proxima sync vai reconciliar.
        // Room já emite pro Flow após o upsert local (UI otimista). O
        // notifyLocalMutation (re-fetch remoto + prune) SÓ roda após o remoto
        // confirmar — senão o re-fetch corre na frente da escrita e PODA a linha
        // otimista ainda-não-sincronizada (item "pisca e some").
        dao.upsertBook(book)
        // Offline-first REAL: se o remoto falhar (offline/429/5xx), ENFILEIRA em vez
        // de só logar. Antes a criação local-only era podada no próximo sync (a linha
        // nunca chegava ao servidor) — perda silenciosa (P0-2).
        val dto = book.toInsertDto()
        tryRemoteOrEnqueue("insert_book", json.encodeToString(dto), notifyTable = "books") {
            supabase.from("books").upsert(dto)
        }
    }

    /**
     * Sobe bytes da capa pro bucket `book-covers` no path `<clubId>/<bookId>.jpg`.
     * Retorna URL pra usar em `books.cover_url`.
     *
     * Bucket e privado — geramos signed URL com expiracao longa (1 ano) e a guardamos
     * no banco. Quando expirar, regeneramos. Pra clubes ativos isso significa que
     * a URL sempre esta valida na pratica.
     *
     * Path por clube garante isolamento via RLS: so members do clube podem
     * ler/escrever ali (policy book_covers_*_members).
     */
    suspend fun uploadBookCover(clubId: String, bookId: String, bytes: ByteArray): String? = runCatching {
        val path = "$clubId/$bookId.jpg"
        val bucket = supabase.storage.from("book-covers")
        // Upload sobreescreve se ja existir (caso usuario troque a capa).
        bucket.upload(path, bytes) {
            upsert = true
            contentType = io.ktor.http.ContentType.Image.JPEG
        }
        // Signed URL valida por 1 ano (max permitido pelo Supabase Storage).
        val signedUrl = bucket.createSignedUrl(path, kotlin.time.Duration.parse("365d"))
        // signed URL ja vem com prefixo do servidor
        "${BuildConfig.SUPABASE_URL}$signedUrl"
    }.getOrNull()

    suspend fun insertClubBook(clubBook: ClubBook) {
        // Optimistic local-first + fila (P0-2): offline não perde mais a criação.
        dao.upsertClubBook(clubBook)
        val dto = clubBook.toDto()
        tryRemoteOrEnqueue("insert_club_book", json.encodeToString(dto), notifyTable = "club_books") {
            supabase.from("club_books").upsert(dto)
        }
    }

    fun getBookByStatusFlow(clubId: String, status: String): Flow<List<Book>> {
        val key = "clubBooks:$clubId"
        val reload: suspend () -> Unit = { syncClubBooks(clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.MED) { syncClubBooks(clubId) } }
        ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.booksByStatusFlow(clubId, status)
    }

    private suspend fun syncClubBooks(clubId: String) {
        runCatching {
            val rows = supabase.from("club_books").select(Columns.raw("book_id, status, ordem, data_encontro, books!inner(*)")) {
                filter { eq("club_id", clubId) }
            }.decodeList<JoinClubBookFull>()
            val books = rows.map { it.book.toDomain() }
            val clubBooks = rows.map { row ->
                ClubBook(
                    clubId = clubId,
                    bookId = row.bookId,
                    status = row.status,
                    ordem = row.ordem,
                    dataEncontro = row.dataEncontro?.fromIso(),
                )
            }
            dao.replaceClubBooksInClub(clubId, clubBooks, books)
        }
    }

    @Serializable
    private data class JoinClubBookFull(
        @SerialName("book_id") val bookId: String,
        val status: String,
        val ordem: Int = 0,
        @SerialName("data_encontro") val dataEncontro: String? = null,
        @SerialName("books") val book: BookDto,
    )

    fun getClubBooksFlow(clubId: String): Flow<List<Book>> {
        val key = "clubBooks:$clubId"
        val reload: suspend () -> Unit = { syncClubBooks(clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.MED) { syncClubBooks(clubId) } }
        ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.clubBooksFlow(clubId)
    }

    suspend fun getClubBookStatus(clubId: String, bookId: String): String? = runCatching {
        supabase.from("club_books").select(Columns.raw("status")) {
            filter {
                eq("club_id", clubId)
                eq("book_id", bookId)
            }
            limit(1)
        }.decodeSingleOrNull<StatusOnlyDto>()?.status
    }.getOrNull()

    @Serializable
    private data class StatusOnlyDto(val status: String)

    suspend fun getBook(id: String): Book? {
        dao.book(id)?.let { return it }
        return runCatching {
            supabase.from("books").select {
                filter { eq("id", id) }
                limit(1)
            }.decodeSingleOrNull<BookDto>()?.toDomain()
                ?.also { dao.upsertBook(it) }
        }.getOrNull()
    }

    suspend fun updateClubBookStatus(clubId: String, bookId: String, status: String) {
        runCatching {
            supabase.from("club_books").update({ set("status", status) }) {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
            // Cache local sera atualizado via notifyLocalMutation -> syncClubBooks.
            notifyLocalMutation("club_books")
        }
    }

    suspend fun updateClubBookMeetingDate(clubId: String, bookId: String, dataEncontro: Long?) {
        runCatching {
            supabase.from("club_books").update({
                set("data_encontro", dataEncontro?.toIso())
            }) {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
            notifyLocalMutation("club_books")
        }
    }

    fun getClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> {
        val reload: suspend () -> Unit = { syncClubBooks(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("club_books", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.clubBooksByStatusFlow(clubId, status)
    }

    // Helper antigo abaixo removido — `syncClubBooks` cuida de tudo via Room.
    @Suppress("UNUSED")
    private fun _oldClubBooksByStatusFlow(clubId: String, status: String): Flow<List<ClubBook>> {
        val flow = stateOf<List<ClubBook>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                supabase.from("club_books").select {
                    filter {
                        eq("club_id", clubId)
                        eq("status", status)
                    }
                }.decodeList<ClubBookDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    suspend fun deleteClubBook(clubId: String, bookId: String) {
        runCatching {
            supabase.from("club_books").delete {
                filter {
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
            }
            dao.deleteClubBook(clubId, bookId)
            notifyLocalMutation("club_books")
        }
    }

    // ============================================================
    // CHAPTERS
    // ============================================================

    suspend fun insertChapters(chapters: List<Chapter>) {
        if (chapters.isEmpty()) return
        runCatching {
            supabase.from("chapters").upsert(chapters.map { it.toDto() })
            dao.upsertChapters(chapters)
        }
    }

    fun getChaptersForBookFlow(bookId: String): Flow<List<Chapter>> {
        scope.launch {
            runCatching {
                val list = supabase.from("chapters").select {
                    filter { eq("book_id", bookId) }
                    order("numero", Order.ASCENDING)
                }.decodeList<ChapterDto>().map { it.toDomain() }
                dao.upsertChapters(list)
            }
        }
        return dao.chaptersForBookFlow(bookId)
    }

    suspend fun deleteChaptersForBook(bookId: String) {
        runCatching {
            supabase.from("chapters").delete {
                filter { eq("book_id", bookId) }
            }
            dao.deleteChaptersForBook(bookId)
        }
    }

    /**
     * Salva a lista de capítulos por DIFF por ID ESTÁVEL (uuid). A identidade do
     * capítulo é o `id` (uuid), NÃO o numero:
     *  - P0-1: antes o id era `ch_<bookId>_<numero>` (texto) enviado pra coluna
     *    `uuid` do servidor -> Postgres rejeitava (22P02), o erro era engolido e
     *    o capítulo NUNCA sincronizava (comentários iam pra dead-letter). Agora o
     *    id é uuid de verdade (gerado na tela), aceito pelo servidor.
     *  - B2: como o vínculo comentário→capítulo é o id (uuid) e não o numero,
     *    reordenar/renumerar capítulos NÃO remaneja mais os comentários.
     * Capítulos que ficam são atualizados in-place (upsert); só os removidos
     * (id fora da lista) são apagados — a discussão dos mantidos é preservada.
     */
    suspend fun saveChapters(bookId: String, chapters: List<Chapter>) {
        val keepIds = chapters.map { it.id }
        // Local-first: upsert (in-place) + deleta só os removidos, por id.
        dao.upsertChapters(chapters)
        if (keepIds.isNotEmpty()) dao.deleteChaptersNotInIds(bookId, keepIds) else dao.deleteChaptersForBook(bookId)
        notifyLocalMutation("chapters")
        // Remoto: upsert todos (id uuid), depois deleta só os capítulos cujo id saiu.
        runCatching {
            if (chapters.isNotEmpty()) supabase.from("chapters").upsert(chapters.map { it.toDto() })
            val existing = supabase.from("chapters").select(Columns.raw("id")) {
                filter { eq("book_id", bookId) }
            }.decodeList<IdOnlyDto>().map { it.id }
            val removed = existing.filter { it !in keepIds }
            if (removed.isNotEmpty()) {
                supabase.from("chapters").delete {
                    filter { eq("book_id", bookId); isIn("id", removed) }
                }
            }
        }.onFailure { android.util.Log.w("Rodape/Repo", "saveChapters remoto falhou: ${it.message}") }
    }

    // ---- Índice compartilhado por ISBN (crowdsourcing entre TODOS os clubes) ----

    /** Busca o índice de capítulos que ALGUÉM já cadastrou pra este ISBN. Um
     *  cadastro serve o app inteiro. Retorna null se ninguém compartilhou ainda. */
    suspend fun getChapterTemplate(isbn: String): List<Pair<Int, String>>? = runCatching {
        val row = supabase.from("chapter_templates").select {
            filter { eq("isbn", isbn) }
            limit(1)
        }.decodeSingleOrNull<ChapterTemplateDto>()
        row?.chapters?.map { it.numero to it.titulo }?.sortedBy { it.first }
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    /** Compartilha (ou atualiza) o índice deste ISBN com a comunidade. */
    suspend fun shareChapterTemplate(isbn: String, tituloLivro: String, chapters: List<Pair<Int, String>>, userId: String) {
        runCatching {
            supabase.from("chapter_templates").upsert(
                ChapterTemplateDto(
                    isbn = isbn,
                    tituloLivro = tituloLivro,
                    chapters = chapters.map { ChapterTemplateEntryDto(it.first, it.second) },
                    contributedBy = userId,
                )
            ) { onConflict = "isbn" }
        }.onFailure { android.util.Log.w("Rodape/Repo", "shareChapterTemplate falhou: ${it.message}") }
    }

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

    suspend fun insertComment(comment: Comment) {
        // 1. Optimistic local PRIMEIRO — UI ja mostra
        dao.upsertComment(comment)
        // 2. Tenta HTTP — se falhar, queue
        val payload = """{"id":"${comment.id}","chapterId":"${comment.chapterId}","clubId":"${comment.clubId}","userId":"${comment.userId}","texto":"${comment.texto.escapeJson()}"}"""
        tryRemoteOrEnqueue("insert_comment", payload, notifyTable = "comments") {
            supabase.from("comments").upsert(
                CommentInsertDto(
                    id = comment.id,
                    chapterId = comment.chapterId,
                    clubId = comment.clubId,
                    userId = comment.userId,
                    texto = comment.texto,
                )
            )
        }
    }

    // escapeJson agora é top-level internal em SyncEngine.kt (F3c) — os repos
    // de domínio e a fachada usam o mesmo.

    fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>> {
        val key = "comments:ch:$chapterId"
        val reload: suspend () -> Unit = { syncCommentsForChapter(chapterId, clubId); markSynced(key) }
        scope.launch { syncOnce(key, Ttl.FAST) { syncCommentsForChapter(chapterId, clubId) } }
        ensureRealtime("comments", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.commentsForChapterFlow(chapterId, clubId)
    }

    private suspend fun syncCommentsForChapter(chapterId: String, clubId: String) {
        runCatching {
            val list = supabase.from("comments").select {
                filter {
                    eq("chapter_id", chapterId)
                    eq("club_id", clubId)
                }
                order("created_at", Order.ASCENDING)
            }.decodeList<CommentDto>().map { it.toDomain() }
            dao.replaceCommentsInChapter(chapterId, clubId, list)
        }
    }

    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>> {
        scope.launch {
            runCatching {
                // Join via embed pra trazer chapter.numero pra ordenacao
                val list = supabase.from("comments").select(Columns.raw("*, chapters!inner(numero,book_id)")) {
                    filter {
                        eq("club_id", clubId)
                        eq("chapters.book_id", bookId)
                    }
                }.decodeList<CommentDto>().map { it.toDomain() }
                dao.upsertComments(list)
            }
        }
        // Reactive flow do Room ja faz JOIN com chapters via DAO query
        return dao.commentsForBookFlow(bookId, clubId)
    }

    suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String) {
        // Optimistic local + fila: moderar offline agora persiste (antes era
        // remoto-primeiro e nao fazia nada offline).
        dao.markCommentRemoved(commentId, removidoPor, motivo)
        val payload = buildJsonObject {
            put("id", commentId); put("by", removidoPor); put("motivo", motivo)
        }.toString()
        tryRemoteOrEnqueue("remove_comment", payload, notifyTable = "comments") {
            supabase.from("comments").update({
                set("removido", true)
                set("removido_por", removidoPor)
                set("motivo_remocao", motivo)
            }) { filter { eq("id", commentId) } }
        }
    }

    suspend fun restoreComment(commentId: String) {
        dao.markCommentRestored(commentId)
        val payload = buildJsonObject { put("id", commentId) }.toString()
        tryRemoteOrEnqueue("restore_comment", payload, notifyTable = "comments") {
            supabase.from("comments").update({
                set("removido", false)
                set("removido_por", JsonNull)
                set("motivo_remocao", JsonNull)
            }) { filter { eq("id", commentId) } }
        }
    }

    /** Edita o texto do PRÓPRIO comentário (RLS permite autor enquanto não removido).
     *  Local-first + fila offline. */
    suspend fun editOwnComment(commentId: String, novoTexto: String) {
        dao.updateCommentText(commentId, novoTexto)
        val payload = buildJsonObject { put("id", commentId); put("texto", novoTexto) }.toString()
        tryRemoteOrEnqueue("edit_comment", payload, notifyTable = "comments") {
            supabase.from("comments").update({ set("texto", novoTexto) }) {
                filter { eq("id", commentId) }
            }
        }
    }

    /** Apaga o PRÓPRIO comentário (hard delete; RLS "comments delete self" na migration 0003).
     *  Local-first + fila offline. */
    suspend fun deleteOwnComment(commentId: String) {
        dao.deleteComment(commentId)
        val payload = buildJsonObject { put("id", commentId) }.toString()
        tryRemoteOrEnqueue("delete_comment", payload, notifyTable = "comments") {
            supabase.from("comments").delete { filter { eq("id", commentId) } }
        }
    }

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

    suspend fun insertReaction(reaction: Reaction) {
        dao.upsertReaction(reaction)
        val payload = """{"commentId":"${reaction.commentId}","userId":"${reaction.userId}","emoji":"${reaction.emoji}"}"""
        tryRemoteOrEnqueue("insert_reaction", payload, notifyTable = "reactions") {
            supabase.from("reactions").upsert(reaction.toDto())
        }
    }

    suspend fun deleteReaction(reaction: Reaction) {
        dao.deleteReaction(reaction.commentId, reaction.userId, reaction.emoji)
        val payload = """{"commentId":"${reaction.commentId}","userId":"${reaction.userId}","emoji":"${reaction.emoji}"}"""
        tryRemoteOrEnqueue("delete_reaction", payload, notifyTable = "reactions") {
            supabase.from("reactions").delete {
                filter {
                    eq("comment_id", reaction.commentId)
                    eq("user_id", reaction.userId)
                    eq("emoji", reaction.emoji)
                }
            }
        }
    }

    fun getReactionsForCommentFlow(commentId: String): Flow<List<Reaction>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("reactions").select {
                    filter { eq("comment_id", commentId) }
                }.decodeList<ReactionDto>().map { it.toDomain() }
                dao.replaceReactionsForComment(commentId, list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("reactions", filterColumn = "comment_id", filterValue = commentId, reload = reload)
        return dao.reactionsForCommentFlow(commentId)
    }

    fun getReactionsForChapterFlow(chapterId: String): Flow<List<Reaction>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val commentIds = supabase.from("comments").select(Columns.raw("id")) {
                    filter { eq("chapter_id", chapterId) }
                }.decodeList<IdOnlyDto>().map { it.id }
                if (commentIds.isNotEmpty()) {
                    val list = supabase.from("reactions").select {
                        filter { isIn("comment_id", commentIds) }
                    }.decodeList<ReactionDto>().map { it.toDomain() }
                    dao.upsertReactions(list)
                }
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("reactions", reload = reload)
        return dao.reactionsForChapterFlow(chapterId)
    }

    @Serializable
    private data class IdOnlyDto(val id: String)

    // ============================================================
    // VOTES & VOTING ROUNDS
    // ============================================================

    suspend fun insertVote(vote: Vote) {
        val roundId = vote.votingRoundId ?: return
        setUserVoteInRound(vote.userId, roundId, vote.clubBookId)
    }

    /**
     * Define o voto do usuário na rodada (VOTO ÚNICO). Troca ATÔMICA:
     *  - Local: apaga os votos do usuário na rodada + insere o novo (otimista). Sem
     *    isso o Room ficava com [A,B] (PK local permite N) e um reload com dado velho
     *    podava o novo e revertia pro antigo ("vira teu voto e volta, em loop").
     *  - Remoto: UM upsert com onConflict na PK (round,user) — substitui A por B numa
     *    operação só (sem delete+insert separados, que disparavam reload prematuro).
     *  - notifyTable só no sucesso: o reload só roda quando o servidor já tem B.
     */
    suspend fun setUserVoteInRound(userId: String, roundId: String, bookId: String) {
        dao.deleteUserVotesInRound(roundId, userId)
        dao.upsertVotes(listOf(Vote(votingRoundId = roundId, clubBookId = bookId, userId = userId, votedAt = System.currentTimeMillis())))
        val payload = """{"votingRoundId":"$roundId","userId":"$userId","bookId":"$bookId"}"""
        tryRemoteOrEnqueue("insert_vote", payload, notifyTable = "votes") {
            supabase.from("votes").upsert(
                VoteInsertDto(votingRoundId = roundId, userId = userId, bookId = bookId)
            ) { onConflict = "voting_round_id,user_id" }
        }
    }

    suspend fun clearVotesForUserInClub(userId: String, clubId: String) {
        runCatching {
            val roundIds = supabase.from("voting_rounds").select(Columns.raw("id")) {
                filter { eq("club_id", clubId) }
            }.decodeList<IdOnlyDto>().map { it.id }
            if (roundIds.isEmpty()) return@runCatching
            supabase.from("votes").delete {
                filter {
                    eq("user_id", userId)
                    isIn("voting_round_id", roundIds)
                }
            }
            notifyLocalMutation("votes")
        }
    }

    fun getVotesForClubFlow(clubId: String): Flow<List<Vote>> {
        // Sem cache local especifico — esse flow nao e critico (UI tem flow por round).
        val flow = stateOf<List<Vote>>(emptyList())
        scope.launch {
            flow.value = runCatching {
                val roundIds = supabase.from("voting_rounds").select(Columns.raw("id")) {
                    filter { eq("club_id", clubId) }
                }.decodeList<IdOnlyDto>().map { it.id }
                if (roundIds.isEmpty()) emptyList()
                else supabase.from("votes").select {
                    filter { isIn("voting_round_id", roundIds) }
                }.decodeList<VoteDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("votes").select {
                    filter { eq("voting_round_id", roundId) }
                }.decodeList<VoteDto>().map { it.toDomain() }
                dao.replaceVotesInRound(roundId, list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("votes", filterColumn = "voting_round_id", filterValue = roundId, reload = reload)
        return dao.votesForRoundFlow(roundId)
    }

    suspend fun getVotesForRound(roundId: String): List<Vote> {
        // Prefere Room; se vazio, busca remoto
        val cached = dao.votesForRound(roundId)
        if (cached.isNotEmpty()) return cached
        return runCatching {
            supabase.from("votes").select {
                filter { eq("voting_round_id", roundId) }
            }.decodeList<VoteDto>().map { it.toDomain() }
                .also { dao.upsertVotes(it) }
        }.getOrDefault(emptyList())
    }

    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String) {
        // Optimistic local + fila (espelha insertVote): desfazer voto offline agora
        // persiste. Antes era remoto-primeiro sem apagar a linha do Room, entao o
        // voto "voltava" ate um reload via Realtime.
        dao.deleteVote(roundId, userId, bookId)
        val payload = buildJsonObject {
            put("votingRoundId", roundId); put("userId", userId); put("bookId", bookId)
        }.toString()
        tryRemoteOrEnqueue("delete_vote", payload, notifyTable = "votes") {
            supabase.from("votes").delete {
                filter {
                    eq("user_id", userId)
                    eq("voting_round_id", roundId)
                    eq("book_id", bookId)
                }
            }
        }
    }

    suspend fun countUserVotesInRound(userId: String, roundId: String): Int = runCatching {
        supabase.from("votes").select {
            filter {
                eq("user_id", userId)
                eq("voting_round_id", roundId)
            }
        }.decodeList<VoteDto>().size
    }.getOrDefault(0)

    // ---- voting_rounds ----

    suspend fun insertVotingRound(round: VotingRound) {
        // Local-first: grava no Room ANTES do remoto, pra a votação aparecer na
        // hora mesmo se o remoto demorar/falhar. Loga a falha (antes era engolida).
        dao.upsertVotingRound(round)
        runCatching {
            supabase.from("voting_rounds").upsert(
                VotingRoundInsertDto(
                    id = round.id,
                    clubId = round.clubId,
                    criadoPor = round.criadoPor,
                    fechaEm = round.fechaEm.toIso(),
                    nLivros = round.nLivros,
                    cadencia = round.cadencia,
                    status = round.status,
                )
            )
        }.onSuccess { notifyLocalMutation("voting_rounds") }
            .onFailure { android.util.Log.e("RodapeWrite", "insertVotingRound remoto falhou", it) }
    }

    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?> {
        val reload: suspend () -> Unit = {
            runCatching {
                val r = getActiveVotingRound(clubId)
                if (r != null) dao.upsertVotingRound(r)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("voting_rounds", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.activeRoundForClubFlow(clubId)
    }

    suspend fun getActiveVotingRound(clubId: String): VotingRound? = runCatching {
        supabase.from("voting_rounds").select {
            filter {
                eq("club_id", clubId)
                eq("status", "aberta")
            }
            order("aberta_em", Order.DESCENDING)
            limit(1)
        }.decodeSingleOrNull<VotingRoundDto>()?.toDomain()
    }.getOrNull()

    suspend fun closeVotingRound(id: String, vencedoresJson: String) {
        // Preferir RPC close_voting_round (que faz tudo: marca finished, promove vencedor, notifica).
        // Mas o MainViewModel hoje ja faz parte desse trabalho manualmente, entao usamos
        // a RPC + ignoramos o vencedoresJson legado.
        closeVotingRoundViaRpc(id)
        notifyLocalMutation("voting_rounds")
        notifyLocalMutation("club_books") // RPC promoveu vencedor (next/current)
        notifyLocalMutation("notifications") // RPC criou notifs pros membros
    }

    // ============================================================
    // BOOK SUGGESTIONS
    // ============================================================

    suspend fun insertBookSuggestion(suggestion: BookSuggestion) {
        // Optimistic local-first + fila (P0-2): offline não perde mais a sugestão.
        dao.upsertBookSuggestions(listOf(suggestion))
        val dto = BookSuggestionInsertDto(
            id = suggestion.id,
            clubId = suggestion.clubId,
            bookId = suggestion.bookId,
            sugeridoPor = suggestion.suggestedByUserId,
            justificativa = suggestion.justificativa.ifBlank { null },
        )
        tryRemoteOrEnqueue("insert_book_suggestion", json.encodeToString(dto), notifyTable = "book_suggestions") {
            supabase.from("book_suggestions").upsert(dto)
        }
    }

    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?> {
        scope.launch {
            runCatching {
                val s = supabase.from("book_suggestions").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookSuggestionDto>()?.toDomain()
                if (s != null) dao.upsertBookSuggestions(listOf(s))
            }
        }
        return dao.bookSuggestionFlow(clubId, bookId)
    }

    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("book_suggestions").select {
                    filter { eq("club_id", clubId) }
                }.decodeList<BookSuggestionDto>().map { it.toDomain() }
                dao.replaceBookSuggestionsInClub(clubId, list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_suggestions", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookSuggestionsForClubFlow(clubId)
    }

    suspend fun deleteBookSuggestion(bookId: String, clubId: String) {
        runCatching {
            supabase.from("book_suggestions").delete {
                filter {
                    eq("book_id", bookId)
                    eq("club_id", clubId)
                }
            }
            dao.deleteBookSuggestion(clubId, bookId)
            notifyLocalMutation("book_suggestions")
        }
    }

    suspend fun deleteVotesForBook(bookId: String) {
        runCatching {
            supabase.from("votes").delete { filter { eq("book_id", bookId) } }
            notifyLocalMutation("votes")
        }
    }

    // ============================================================
    // MEETINGS
    // ============================================================

    suspend fun insertMeeting(meeting: Meeting) {
        // Instante real: prefere dataEpoch (já calculado no fuso local pela VM);
        // fallback pra parse do rótulo. NUNCA interpreta hora local como UTC.
        val epoch = if (meeting.dataEpoch != 0L) meeting.dataEpoch
            else MeetingTime.parseLocal(meeting.data, meeting.hora) ?: System.currentTimeMillis()
        val dataIso = MeetingTime.epochToIso(epoch)
        // Garante que o Room guarde o epoch mesmo se a VM não preencheu.
        val toStore = if (meeting.dataEpoch != 0L) meeting else meeting.copy(dataEpoch = epoch)
        // Local-first: grava no Room antes do remoto pra o encontro aparecer na hora.
        // notifyLocalMutation (re-fetch + prune) só após o remoto confirmar, senão
        // o re-sync poda o encontro otimista ainda-não-sincronizado.
        dao.upsertMeetings(listOf(toStore))
        val dto = MeetingInsertDto(
            id = meeting.id,
            clubId = meeting.clubId,
            data = dataIso,
            local = meeting.local.ifBlank { null },
            agenda = meeting.agenda.ifBlank { null },
            bookId = meeting.bookId,
            chapterStart = meeting.chapterStart,
            chapterEnd = meeting.chapterEnd,
            status = meeting.status,
        )
        // Offline-first + fila (P0-2): offline não perde mais o encontro. O push do
        // livro (FK meetings_book_id_fkey) fica DENTRO do bloco pra também rodar no
        // replay da fila (o handler insert_meeting repete a mesma lógica).
        tryRemoteOrEnqueue("insert_meeting", json.encodeToString(dto), notifyTable = "meetings") {
            meeting.bookId?.let { bid ->
                dao.book(bid)?.let { b -> runCatching { supabase.from("books").upsert(b.toInsertDto()) } }
            }
            supabase.from("meetings").upsert(dto)
        }
    }

    private suspend fun syncMeetingsForClub(clubId: String) {
        runCatching {
            val list = supabase.from("meetings").select {
                filter { eq("club_id", clubId) }
                order("data", Order.DESCENDING)
            }.decodeList<MeetingDto>().map { it.toDomain() }
            dao.replaceMeetingsInClub(clubId, list)
        }
    }

    fun getLatestMeetingFlow(clubId: String): Flow<Meeting?> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        // "Próximo encontro" = agendado futuro mais próximo (instante real).
        return dao.nextMeetingForClubFlow(clubId, System.currentTimeMillis())
    }

    fun getAllMeetingsFlow(clubId: String): Flow<List<Meeting>> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.meetingsForClubFlow(clubId)
    }

    fun getScheduledMeetingsForClubFlow(clubId: String): Flow<List<Meeting>> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.scheduledMeetingsForClubFlow(clubId)
    }

    suspend fun getMeetingById(meetingId: String): Meeting? {
        dao.meetingById(meetingId)?.let { return it }
        return runCatching {
            supabase.from("meetings").select {
                filter { eq("id", meetingId) }
                limit(1)
            }.decodeSingleOrNull<MeetingDto>()?.toDomain()
                ?.also { dao.upsertMeetings(listOf(it)) }
        }.getOrNull()
    }

    fun getMeetingByIdFlow(meetingId: String): Flow<Meeting?> {
        scope.launch { runCatching { getMeetingById(meetingId) } }
        return dao.meetingByIdFlow(meetingId)
    }

    fun getMeetingsForBookFlow(clubId: String, bookId: String): Flow<List<Meeting>> {
        val reload: suspend () -> Unit = { syncMeetingsForClub(clubId) }
        scope.launch { runCatching { reload() } }
        // Realtime: encontros do livro atualizam ao vivo entre dispositivos (antes
        // era one-shot e ficava desatualizado até refresh manual).
        ensureRealtime("meetings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.meetingsForBookFlow(clubId, bookId)
    }

    suspend fun getMeetingsForBookList(clubId: String, bookId: String): List<Meeting> = runCatching {
        supabase.from("meetings").select {
            filter {
                eq("club_id", clubId)
                eq("book_id", bookId)
            }
            order("data", Order.ASCENDING)
        }.decodeList<MeetingDto>().map { it.toDomain() }
            .also { dao.upsertMeetings(it) }
    }.getOrDefault(emptyList())

    suspend fun updateMeetingStatus(meetingId: String, status: String) {
        runCatching {
            supabase.from("meetings").update({ set("status", status) }) {
                filter { eq("id", meetingId) }
            }
            notifyLocalMutation("meetings")
        }
    }

    suspend fun deleteMeeting(meetingId: String) {
        runCatching {
            supabase.from("meetings").delete { filter { eq("id", meetingId) } }
            dao.deleteMeeting(meetingId)
            notifyLocalMutation("meetings")
        }
    }

    suspend fun deleteRsvpsForMeeting(meetingId: String) {
        runCatching {
            supabase.from("meeting_rsvps").delete { filter { eq("meeting_id", meetingId) } }
            dao.deleteAllRsvpsForMeeting(meetingId)
            notifyLocalMutation("meeting_rsvps")
        }
    }

    // ---- meeting_rsvps ----

    suspend fun insertMeetingRsvp(rsvp: MeetingRsvp) {
        dao.upsertMeetingRsvp(rsvp)
        val statusEnum = rsvpToEnum(rsvp.status)
        val payload = """{"meetingId":"${rsvp.meetingId}","userId":"${rsvp.userId}","status":"$statusEnum"}"""
        tryRemoteOrEnqueue("insert_meeting_rsvp", payload, notifyTable = "meeting_rsvps") {
            supabase.from("meeting_rsvps").upsert(
                MeetingRsvpInsertDto(
                    meetingId = rsvp.meetingId,
                    userId = rsvp.userId,
                    status = statusEnum,
                )
            )
        }
    }

    fun getRsvpsForMeetingFlow(meetingId: String): Flow<List<MeetingRsvp>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("meeting_rsvps").select {
                    filter { eq("meeting_id", meetingId) }
                }.decodeList<MeetingRsvpDto>().map { it.toDomain() }
                dao.replaceRsvpsForMeeting(meetingId, list)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meeting_rsvps", filterColumn = "meeting_id", filterValue = meetingId, reload = reload)
        return dao.rsvpsForMeetingFlow(meetingId)
    }

    fun getRsvpForMeetingOfUserFlow(meetingId: String, userId: String): Flow<MeetingRsvp?> {
        scope.launch {
            runCatching {
                val r = supabase.from("meeting_rsvps").select {
                    filter {
                        eq("meeting_id", meetingId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<MeetingRsvpDto>()?.toDomain()
                if (r != null) dao.upsertMeetingRsvp(r)
            }
        }
        return dao.rsvpOfUserForMeetingFlow(meetingId, userId)
    }

    // ---- meeting_patterns ----

    suspend fun insertMeetingPattern(pattern: MeetingPattern) {
        // Local-first: grava o padrão no Room ANTES do remoto (separado), pra ele
        // aparecer na hora e não sumir se a rede falhar (antes era tudo num
        // runCatching remoto-primeiro que engolia a escrita local).
        dao.upsertMeetingPattern(pattern)
        notifyLocalMutation("meeting_patterns")
        runCatching {
            supabase.from("meeting_patterns").upsert(
                MeetingPatternInsertDto(
                    id = pattern.id,
                    clubId = pattern.clubId,
                    diaSemana = pattern.diaSemana,
                    hora = pattern.hora,
                    local = pattern.local.ifBlank { null },
                    agendaTemplate = pattern.agendaTemplate.ifBlank { null },
                    ativo = pattern.ativo,
                    tipoRecorrencia = pattern.tipoRecorrencia,
                    valorRecorrencia = pattern.valorRecorrencia,
                )
            )
        }.onFailure { android.util.Log.w("Rodape/Repo", "insertMeetingPattern remote falhou: ${it.message}") }
    }

    /**
     * Gera encontros concretos a partir de um padrão de recorrência — o que
     * fazia a recorrência FINALMENTE funcionar (antes o padrão era só um rótulo,
     * nenhum encontro nascia dele). Idempotente: id determinístico por data
     * (`mtg_pat_<clubId>_<yyyymmdd>`), então re-gerar não duplica. Antes de gerar,
     * remove encontros auto-gerados FUTUROS (caso o dia/hora do padrão tenha
     * mudado) — nunca toca em encontros criados à mão.
     *
     * [horizon] = quantas ocorrências à frente gerar (default 8).
     */
    suspend fun generateMeetingsFromPattern(pattern: MeetingPattern, horizon: Int = 8) {
        if (pattern.tipoRecorrencia == "unica" || !pattern.ativo) return
        val now = System.currentTimeMillis()
        val epochs = MeetingTime.nextOccurrenceEpochs(
            tipo = pattern.tipoRecorrencia,
            diaSemana = pattern.diaSemana,
            valor = pattern.valorRecorrencia,
            hora = pattern.hora,
            fromEpoch = now,
            count = horizon,
        )
        if (epochs.isEmpty()) return

        // Limpa auto-gerados futuros (local + remoto) antes de recriar.
        runCatching { dao.deleteFutureGeneratedMeetings(pattern.clubId, now) }
        runCatching {
            supabase.from("meetings").delete {
                filter {
                    eq("club_id", pattern.clubId)
                    like("id", "mtg_pat_%")
                    gte("data", MeetingTime.epochToIso(now))
                }
            }
        }

        val zone = java.time.ZoneId.systemDefault()
        val meetings = epochs.map { epoch ->
            val ymd = java.time.Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate()
            val id = "mtg_pat_${pattern.clubId}_%04d%02d%02d".format(ymd.year, ymd.monthValue, ymd.dayOfMonth)
            val (label, horaStr) = MeetingTime.epochToLabel(epoch)
            Meeting(
                id = id, clubId = pattern.clubId,
                data = label, hora = horaStr,
                local = pattern.local, agenda = pattern.agendaTemplate,
                bookId = null, chapterStart = null, chapterEnd = null,
                status = "agendado", dataEpoch = epoch,
            )
        }
        meetings.forEach { insertMeeting(it) }
    }

    fun getActiveMeetingPatternFlow(clubId: String): Flow<MeetingPattern?> {
        scope.launch {
            runCatching {
                val p = getActiveMeetingPattern(clubId)
                if (p != null) dao.upsertMeetingPattern(p)
            }
        }
        return dao.activeMeetingPatternForClubFlow(clubId)
    }

    suspend fun getActiveMeetingPattern(clubId: String): MeetingPattern? = runCatching {
        supabase.from("meeting_patterns").select {
            filter {
                eq("club_id", clubId)
                eq("ativo", true)
            }
            limit(1)
        }.decodeSingleOrNull<MeetingPatternDto>()?.toDomain()
    }.getOrNull()

    suspend fun deactivateMeetingPatterns(clubId: String) {
        runCatching {
            supabase.from("meeting_patterns").update({ set("ativo", false) }) {
                filter { eq("club_id", clubId) }
            }
            notifyLocalMutation("meeting_patterns")
        }
    }

    // ---- meeting_minutes / meeting_notes ----

    suspend fun insertMeetingMinutes(minutes: MeetingMinutes) {
        runCatching {
            supabase.from("meeting_minutes").upsert(
                MeetingMinutesInsertDto(
                    meetingId = minutes.meetingId,
                    texto = minutes.texto,
                    lastEditorId = minutes.lastEditorId.ifBlank { null },
                )
            )
            notifyLocalMutation("meeting_minutes")
        }
    }

    fun getMeetingMinutesFlow(meetingId: String): Flow<MeetingMinutes?> {
        val reload: suspend () -> Unit = {
            runCatching {
                val m = supabase.from("meeting_minutes").select {
                    filter { eq("meeting_id", meetingId) }
                    limit(1)
                }.decodeSingleOrNull<MeetingMinutesDto>()?.toDomain()
                if (m != null) dao.upsertMeetingMinutes(m)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("meeting_minutes", filterColumn = "meeting_id", filterValue = meetingId, reload = reload)
        return dao.meetingMinutesFlow(meetingId)
    }

    suspend fun insertMeetingNote(note: MeetingNote) {
        runCatching {
            supabase.from("meeting_notes").upsert(
                MeetingNoteInsertDto(
                    meetingId = note.meetingId,
                    userId = note.userId,
                    texto = note.texto,
                )
            )
            dao.upsertMeetingNote(note)
            notifyLocalMutation("meeting_notes")
        }
    }

    fun getMeetingNoteFlow(meetingId: String, userId: String): Flow<MeetingNote?> {
        scope.launch {
            runCatching {
                val n = supabase.from("meeting_notes").select {
                    filter {
                        eq("meeting_id", meetingId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<MeetingNoteDto>()?.toDomain()
                if (n != null) dao.upsertMeetingNote(n)
            }
        }
        return dao.meetingNoteOfUserFlow(meetingId, userId)
    }

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

    suspend fun insertBookSummary(summary: BookSummary) {
        runCatching {
            supabase.from("book_summaries").upsert(
                BookSummaryInsertDto(
                    bookId = summary.bookId,
                    clubId = summary.clubId,
                    texto = summary.texto,
                    lastEditorId = summary.lastEditorId.ifBlank { null },
                )
            )
            dao.upsertBookSummary(summary)
            notifyLocalMutation("book_summaries")
        }
    }

    fun getBookSummaryFlow(bookId: String, clubId: String): Flow<BookSummary?> {
        val reload: suspend () -> Unit = {
            runCatching {
                val s = supabase.from("book_summaries").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookSummaryDto>()?.toDomain()
                if (s != null) dao.upsertBookSummary(s)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_summaries", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookSummaryFlow(bookId, clubId)
    }

    suspend fun insertBookRating(rating: BookRating) {
        // Local-first + fila: grava no Room antes do remoto e enfileira se
        // offline. Antes era remoto-primeiro dentro de runCatching — avaliar um
        // livro sem internet sumia sem aviso (o dao.upsert nem rodava).
        dao.upsertBookRatings(listOf(rating))
        val payload = buildJsonObject {
            put("bookId", rating.bookId)
            put("clubId", rating.clubId)
            put("userId", rating.userId)
            put("stars", rating.stars.toString())
            put("comment", rating.comment)
        }.toString()
        tryRemoteOrEnqueue("upsert_book_rating", payload, notifyTable = "book_ratings") {
            supabase.from("book_ratings").upsert(
                BookRatingInsertDto(
                    bookId = rating.bookId,
                    clubId = rating.clubId,
                    userId = rating.userId,
                    stars = rating.stars,
                    comment = rating.comment,
                )
            )
        }
    }

    fun getBookRatingsFlow(bookId: String, clubId: String): Flow<List<BookRating>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("book_ratings").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                }.decodeList<BookRatingDto>().map { it.toDomain() }
                dao.upsertBookRatings(list)
                dao.pruneBookRatingsExcept(bookId, clubId, list.map { it.userId })
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_ratings", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookRatingsFlow(bookId, clubId)
    }

    fun getBookRatingOfUserFlow(bookId: String, clubId: String, userId: String): Flow<BookRating?> {
        scope.launch {
            runCatching {
                val r = supabase.from("book_ratings").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                        eq("user_id", userId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookRatingDto>()?.toDomain()
                if (r != null) dao.upsertBookRatings(listOf(r))
            }
        }
        return dao.bookRatingOfUserFlow(bookId, clubId, userId)
    }

    // ---- book_favorites ----
    // Favorito PESSOAL de livro (cross-clube). Local-first + fila offline, igual
    // book_ratings: grava no Room na hora e enfileira se offline (idempotente).
    suspend fun setBookFavorite(userId: String, bookId: String, favorite: Boolean) {
        val payload = buildJsonObject {
            put("userId", userId)
            put("bookId", bookId)
        }.toString()
        if (favorite) {
            dao.upsertBookFavorites(listOf(BookFavorite(userId, bookId, System.currentTimeMillis())))
            tryRemoteOrEnqueue("insert_book_favorite", payload, notifyTable = "book_favorites") {
                supabase.from("book_favorites").upsert(BookFavoriteInsertDto(userId = userId, bookId = bookId))
            }
        } else {
            dao.deleteBookFavorite(userId, bookId)
            tryRemoteOrEnqueue("delete_book_favorite", payload, notifyTable = "book_favorites") {
                supabase.from("book_favorites").delete {
                    filter {
                        eq("user_id", userId)
                        eq("book_id", bookId)
                    }
                }
            }
        }
    }

    fun isBookFavoriteFlow(userId: String, bookId: String): Flow<Boolean> =
        dao.isBookFavoriteFlow(userId, bookId)

    suspend fun anyClubIdForBook(bookId: String): String? = dao.anyClubIdForBook(bookId)

    fun getFavoriteBooksForUserFlow(userId: String): Flow<List<Book>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("book_favorites").select {
                    filter { eq("user_id", userId) }
                }.decodeList<BookFavoriteDto>()
                dao.upsertBookFavorites(list.map { BookFavorite(it.userId, it.bookId, it.createdAt.fromIso()) })
                dao.pruneBookFavoritesExcept(userId, list.map { it.bookId })
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_favorites", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.favoriteBooksFlow(userId)
    }

    // ============================================================
    // SEED — no-op (app nasce vazio em producao)
    // ============================================================

    suspend fun seedDatabase() {
        // 9B: app nasce vazio em producao. Nenhum seed.
    }
}
