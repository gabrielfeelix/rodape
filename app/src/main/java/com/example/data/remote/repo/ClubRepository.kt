package com.example.data.remote.repo

import com.example.data.model.Club
import com.example.data.model.ClubMember
import com.example.data.model.MemberRemoval
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Clubes: clube em si (`clubs`), membros (`club_members`), admin e as RPCs
 * privilegiadas (create_club, join_club_with_code, promote/demote/transfer/
 * remove, leave, regenerate_invite_code, delete_own_account). Repo coeso do
 * clube (F3c) — membros+admin ficam junto do clube.
 */
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
    suspend fun deleteOwnAccountViaRpc()
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
