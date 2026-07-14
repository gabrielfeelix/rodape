package com.example.data.remote.repo

import com.example.data.model.Club
import com.example.data.model.ClubMember
import com.example.data.model.MemberRemoval
import com.example.data.model.User
import com.example.data.remote.ClubDto
import com.example.data.remote.ClubMemberDto
import com.example.data.remote.MemberRemovalDto
import com.example.data.remote.ProfileDto
import com.example.data.remote.SyncEngine
import com.example.data.remote.Ttl
import com.example.data.remote.fromIso
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject

// F3c: clube — clubs + members + admin + RPCs SECURITY DEFINER (promote/
// demote/transfer/remove/leave/regenerate ficam aqui: são Members/Admin).
// Corpos movidos VERBATIM do RemoteRepository — comportamento idêntico.
interface ClubRepository {
    fun getClubFlow(clubId: String): Flow<Club?>
    suspend fun getClub(clubId: String): Club?
    suspend fun getClubByCodigo(codigo: String): Club?
    fun getClubsForUser(userId: String): Flow<List<Club>>
    suspend fun getClubsForUserList(userId: String): List<Club>
    suspend fun insertClub(club: Club)
    suspend fun createClubViaRpc(nome: String, descricao: String?, cor: String, privacidade: String): String
    suspend fun joinClubWithCodeViaRpc(codigo: String): String?
    suspend fun leaveClubViaRpc(clubId: String)
    suspend fun regenerateInviteCodeViaRpc(clubId: String): String
    suspend fun promoteMemberViaRpc(clubId: String, targetUserId: String)
    suspend fun demoteAdminViaRpc(clubId: String, targetUserId: String)
    suspend fun transferSuperAdminViaRpc(clubId: String, toUserId: String)
    suspend fun removeMemberViaRpc(clubId: String, targetUserId: String, motivo: String?)
    suspend fun insertClubMember(member: ClubMember)
    fun getClubMembersFlow(clubId: String): Flow<List<User>>
    suspend fun getClubMember(clubId: String, userId: String): ClubMember?
    suspend fun getClubMembersListOrderedByJoin(clubId: String): List<ClubMember>
    fun getClubMembersRawFlow(clubId: String): Flow<List<ClubMember>>
    suspend fun updateMemberPapel(clubId: String, userId: String, papel: String)
    suspend fun deleteClubMember(clubId: String, userId: String)
    suspend fun insertMemberRemoval(removal: MemberRemoval)
    fun getMemberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>>
    suspend fun updateClubInfo(clubId: String, nome: String, descricao: String, cor: String, privacidade: String)
    suspend fun updateClubCodigo(clubId: String, codigo: String)
    suspend fun updateClubArquivado(clubId: String, arquivado: Boolean)
    fun getArchivedClubsForUserFlow(userId: String): Flow<List<Club>>
}

internal class OfflineFirstClubRepository @Inject constructor(
    engine: SyncEngine,
) : OfflineFirstRepository(engine), ClubRepository {

    override fun getClubFlow(clubId: String): Flow<Club?> {
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

    override suspend fun getClub(clubId: String): Club? {
        dao.club(clubId)?.let { return it }
        syncClub(clubId)
        return dao.club(clubId)
    }

    override suspend fun getClubByCodigo(codigo: String): Club? = runCatching {
        // Pra entrar em clube por codigo, busca direto no servidor (clube pode
        // nao estar no cache local porque user nao e membro ainda).
        val club = supabase.from("clubs").select {
            filter { eq("codigo", codigo) }
            limit(1)
        }.decodeSingleOrNull<ClubDto>()?.toDomain()
        // Nao cacheamos — RLS deve impedir acesso se nao for membro.
        club
    }.getOrNull()

    override fun getClubsForUser(userId: String): Flow<List<Club>> {
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

    override suspend fun getClubsForUserList(userId: String): List<Club> = runCatching {
        // Escopado por membership (nao por "todos nao-arquivados"). Usado pra
        // escolher o proximo clube ativo ao sair/arquivar — nao pode cair num
        // clube publico que o usuario nem e membro.
        supabase.from("club_members").select(Columns.raw("clubs!inner(*)")) {
            filter { eq("user_id", userId) }
        }.decodeList<JoinClubOnly>().map { it.club.toDomain() }.filter { !it.arquivado }
    }.getOrDefault(emptyList())

    override suspend fun insertClub(club: Club) {
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
    override suspend fun createClubViaRpc(
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
    override suspend fun joinClubWithCodeViaRpc(codigo: String): String? {
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
    override suspend fun leaveClubViaRpc(clubId: String) {
        supabase.postgrest.rpc("leave_club", buildJsonObject { put("p_club_id", clubId) })
    }

    /** RPC: regenerate_invite_code. PROPAGA excecao (antes retornava "" e a UI
     *  mostrava "Novo codigo: " em branco). */
    override suspend fun regenerateInviteCodeViaRpc(clubId: String): String {
        val resp = supabase.postgrest.rpc(
            "regenerate_invite_code",
            buildJsonObject { put("p_club_id", clubId) }
        ).data
        return resp.trim('"', ' ', '\n')
    }

    /** RPC: promote_member. PROPAGA excecao (rede/RLS) pra UI mostrar o erro em
     *  vez de reportar sucesso falso e nao mudar o papel. */
    override suspend fun promoteMemberViaRpc(clubId: String, targetUserId: String) {
        supabase.postgrest.rpc("promote_member", buildJsonObject {
            put("p_club_id", clubId); put("p_target_user_id", targetUserId)
        })
    }

    /** RPC: demote_admin. PROPAGA excecao (ver promote). */
    override suspend fun demoteAdminViaRpc(clubId: String, targetUserId: String) {
        supabase.postgrest.rpc("demote_admin", buildJsonObject {
            put("p_club_id", clubId); put("p_target_user_id", targetUserId)
        })
    }

    /** RPC: transfer_super_admin. PROPAGA excecao (invariante super_admin/RLS). */
    override suspend fun transferSuperAdminViaRpc(clubId: String, toUserId: String) {
        supabase.postgrest.rpc("transfer_super_admin", buildJsonObject {
            put("p_club_id", clubId); put("p_target_user_id", toUserId)
        })
    }

    /** RPC: remove_member. PROPAGA excecao (ex: admin tentando remover admin) pra
     *  UI mostrar o erro em vez de o toque nao fazer nada silenciosamente. */
    override suspend fun removeMemberViaRpc(clubId: String, targetUserId: String, motivo: String?) {
        supabase.postgrest.rpc("remove_member", buildJsonObject {
            put("p_club_id", clubId)
            put("p_target_user_id", targetUserId)
            put("p_motivo", motivo ?: "")
        })
    }

    // ============================================================
    // CLUB MEMBERS
    // ============================================================

    override suspend fun insertClubMember(member: ClubMember) {
        // Nao inserimos membro diretamente — RPCs (create_club / join_club_with_code)
        // ja cuidam. Manter no-op preserva interface.
    }

    override fun getClubMembersFlow(clubId: String): Flow<List<User>> {
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

    override suspend fun getClubMember(clubId: String, userId: String): ClubMember? {
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

    override suspend fun getClubMembersListOrderedByJoin(clubId: String): List<ClubMember> = runCatching {
        supabase.from("club_members").select {
            filter { eq("club_id", clubId) }
            order("entrou_em", Order.ASCENDING)
        }.decodeList<ClubMemberDto>().map { it.toDomain() }
    }.getOrDefault(emptyList())

    override fun getClubMembersRawFlow(clubId: String): Flow<List<ClubMember>> {
        val reload: suspend () -> Unit = { syncClubMembers(clubId) }
        scope.launch { runCatching { reload() } }
        ensureRealtime("club_members", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.membersInClubFlow(clubId)
    }

    override suspend fun updateMemberPapel(clubId: String, userId: String, papel: String) {
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

    override suspend fun deleteClubMember(clubId: String, userId: String) {
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

    override suspend fun insertMemberRemoval(removal: MemberRemoval) {
        // RPC remove_member ja cuida disso. Mantido no-op.
    }

    override fun getMemberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>> {
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

    override suspend fun updateClubInfo(clubId: String, nome: String, descricao: String, cor: String, privacidade: String) {
        runCatching {
            supabase.from("clubs").update({
                set("nome", nome)
                set("descricao", descricao)
                set("cor", cor)
                set("privacidade", privacidade)
            }) { filter { eq("id", clubId) } }
        }
    }

    override suspend fun updateClubCodigo(clubId: String, codigo: String) {
        // Use a RPC regenerateInviteCodeViaRpc em vez deste path.
        runCatching {
            supabase.from("clubs").update({ set("codigo", codigo) }) {
                filter { eq("id", clubId) }
            }
        }
    }

    override suspend fun updateClubArquivado(clubId: String, arquivado: Boolean) {
        runCatching {
            supabase.from("clubs").update({ set("arquivado", arquivado) }) {
                filter { eq("id", clubId) }
            }
        }
    }

    override fun getArchivedClubsForUserFlow(userId: String): Flow<List<Club>> {
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
}
