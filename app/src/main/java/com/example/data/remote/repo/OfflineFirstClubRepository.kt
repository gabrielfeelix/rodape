package com.example.data.remote.repo

import com.example.data.db.RodapeDao
import com.example.data.model.*
import com.example.data.remote.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstClubRepository @Inject constructor(
    private val engine: SyncEngine,
    private val supabase: SupabaseClient,
    private val dao: RodapeDao,
) : ClubRepository {

    override fun getClubFlow(clubId: String): Flow<Club?> {
        val reload: suspend () -> Unit = { syncClub(clubId); engine.markSynced("club:$clubId") }
        engine.scope.launch { engine.syncOnce("club:$clubId", Ttl.MED) { syncClub(clubId) } }
        engine.ensureRealtime("clubs", filterColumn = "id", filterValue = clubId, reload = reload)
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
        val club = supabase.from("clubs").select {
            filter { eq("codigo", codigo) }
            limit(1)
        }.decodeSingleOrNull<ClubDto>()?.toDomain()
        club
    }.getOrNull()

    override fun getClubsForUser(userId: String): Flow<List<Club>> {
        val reload: suspend () -> Unit = { syncClubsForUser(userId); engine.markSynced("clubs:user:$userId") }
        engine.scope.launch { engine.syncOnce("clubs:user:$userId", Ttl.MED) { syncClubsForUser(userId) } }
        engine.ensureRealtime("club_members", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.clubsActiveFlow()
    }

    private suspend fun syncClubsForUser(userId: String) {
        runCatching {
            val rows = supabase.from("club_members").select(Columns.raw("clubs!inner(*)")) {
                filter { eq("user_id", userId) }
            }.decodeList<JoinClubOnly>()
            val list = rows.map { it.club.toDomain() }
            dao.upsertClubs(list)
            dao.pruneClubsExcept(list.map { it.id })
        }
    }

    override suspend fun getClubsForUserList(userId: String): List<Club> = runCatching {
        supabase.from("club_members").select(Columns.raw("clubs!inner(*)")) {
            filter { eq("user_id", userId) }
        }.decodeList<JoinClubOnly>().map { it.club.toDomain() }.filter { !it.arquivado }
    }.getOrDefault(emptyList())

    override suspend fun insertClub(club: Club) {
        // App nao deve mais inserir clube diretamente — usa RPC create_club. No-op.
    }

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

    override suspend fun joinClubWithCodeViaRpc(codigo: String): String? {
        val resp = supabase.postgrest.rpc(
            function = "join_club_with_code",
            parameters = buildJsonObject { put("p_codigo", codigo.uppercase().trim()) }
        ).data
        return extractFieldFromRpcRow(resp, "club_id")
    }

    private fun extractIdFromRpcRow(raw: String): String? =
        extractFieldFromRpcRow(raw, "id")

    private fun extractFieldFromRpcRow(raw: String, field: String): String? {
        if (raw.isBlank() || raw == "null") return null
        return runCatching {
            val element = engine.json.parseToJsonElement(raw)
            val obj = when (element) {
                is JsonObject -> element
                is kotlinx.serialization.json.JsonArray -> element.firstOrNull() as? JsonObject
                else -> null
            }
            obj?.get(field)?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
        }.getOrNull()
    }

    override suspend fun leaveClubViaRpc(clubId: String) {
        supabase.postgrest.rpc("leave_club", buildJsonObject { put("p_club_id", clubId) })
    }

    override suspend fun deleteOwnAccountViaRpc() {
        supabase.postgrest.rpc("delete_own_account")
    }

    override suspend fun regenerateInviteCodeViaRpc(clubId: String): String {
        val resp = supabase.postgrest.rpc(
            "regenerate_invite_code",
            buildJsonObject { put("p_club_id", clubId) }
        ).data
        return resp.trim('"', ' ', '\n')
    }

    override suspend fun promoteMemberViaRpc(clubId: String, targetUserId: String) {
        supabase.postgrest.rpc("promote_member", buildJsonObject {
            put("p_club_id", clubId); put("p_target_user_id", targetUserId)
        })
    }

    override suspend fun demoteAdminViaRpc(clubId: String, targetUserId: String) {
        supabase.postgrest.rpc("demote_admin", buildJsonObject {
            put("p_club_id", clubId); put("p_target_user_id", targetUserId)
        })
    }

    override suspend fun transferSuperAdminViaRpc(clubId: String, toUserId: String) {
        supabase.postgrest.rpc("transfer_super_admin", buildJsonObject {
            put("p_club_id", clubId); put("p_target_user_id", toUserId)
        })
    }

    override suspend fun removeMemberViaRpc(clubId: String, targetUserId: String, motivo: String?) {
        supabase.postgrest.rpc("remove_member", buildJsonObject {
            put("p_club_id", clubId)
            put("p_target_user_id", targetUserId)
            put("p_motivo", motivo ?: "")
        })
    }

    override suspend fun insertClubMember(member: ClubMember) {
        // RPCs (create_club / join_club_with_code) ja cuidam. No-op.
    }

    override fun getClubMembersFlow(clubId: String): Flow<List<User>> {
        val key = "members:$clubId"
        val reload: suspend () -> Unit = { syncClubMembers(clubId); engine.markSynced(key) }
        engine.scope.launch { engine.syncOnce(key, Ttl.MED) { syncClubMembers(clubId) } }
        engine.ensureRealtime("club_members", filterColumn = "club_id", filterValue = clubId, reload = reload)
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
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("club_members", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.membersInClubFlow(clubId)
    }

    override suspend fun updateMemberPapel(clubId: String, userId: String, papel: String) {
        runCatching {
            supabase.from("club_members").update({ set("papel", papel) }) {
                filter {
                    eq("club_id", clubId)
                    eq("user_id", userId)
                }
            }
            engine.notifyLocalMutation("club_members")
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
            engine.notifyLocalMutation("club_members")
        }
    }

    override suspend fun insertMemberRemoval(removal: MemberRemoval) {
        // RPC remove_member ja cuida disso. No-op.
    }

    override fun getMemberRemovalsForClubFlow(clubId: String): Flow<List<MemberRemoval>> {
        engine.scope.launch {
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
        engine.scope.launch {
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
