package com.example.data.remote.repo

import com.example.data.model.Comment
import com.example.data.model.ContentReport
import com.example.data.model.ReportReason
import com.example.data.model.ReportTargetType
import com.example.data.model.UserBlock
import com.example.data.remote.CommentDto
import com.example.data.remote.ContentReportDto
import com.example.data.remote.ContentReportInsertDto
import com.example.data.remote.SyncEngine
import com.example.data.remote.UserBlockDto
import com.example.data.remote.UserBlockInsertDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

// F3c: moderação — denúncia, bloqueio, remoção (migration 0010). Corpos
// movidos VERBATIM do RemoteRepository — comportamento idêntico.
interface ModerationRepository {
    suspend fun reportContent(
        reporterId: String,
        clubId: String,
        targetType: ReportTargetType,
        targetId: String,
        targetUserId: String,
        motivo: ReportReason,
        detalhe: String?,
    )
    suspend fun blockUser(me: String, blockedId: String)
    suspend fun unblockUser(me: String, blockedId: String)
    fun observeBlockedIds(me: String): Flow<List<String>>
    fun isBlockedFlow(me: String, other: String): Flow<Boolean>
    suspend fun moderateRemoveContent(
        type: ReportTargetType,
        targetId: String,
        targetUserId: String,
        clubId: String,
        motivo: String?,
        removidoPor: String,
    )
    suspend fun fetchPendingReports(clubId: String): List<ContentReport>
    suspend fun dismissReport(reportId: String)
    fun getRemovedCommentsForClubFlow(clubId: String): Flow<List<Comment>>
}

internal class OfflineFirstModerationRepository @Inject constructor(
    engine: SyncEngine,
) : OfflineFirstRepository(engine), ModerationRepository {

    /** Denuncia um conteúdo. Idempotente por (reporter, tipo, alvo) — reenvio é no-op.
     *  Local-first via fila offline; não precisa de cache local (é write-only). */
    override suspend fun reportContent(
        reporterId: String,
        clubId: String,
        targetType: ReportTargetType,
        targetId: String,
        targetUserId: String,
        motivo: ReportReason,
        detalhe: String?,
    ) {
        val payload = buildJsonObject {
            put("reporterId", reporterId)
            put("clubId", clubId)
            put("targetType", targetType.wire)
            put("targetId", targetId)
            put("targetUserId", targetUserId)
            put("motivo", motivo.wire)
            if (!detalhe.isNullOrBlank()) put("detalhe", detalhe)
        }.toString()
        tryRemoteOrEnqueue("insert_report", payload) {
            supabase.from("content_reports").upsert(
                ContentReportInsertDto(
                    reporterId = reporterId, clubId = clubId,
                    targetType = targetType.wire, targetId = targetId,
                    targetUserId = targetUserId, motivo = motivo.wire,
                    detalhe = detalhe?.takeIf { it.isNotBlank() },
                )
            ) { onConflict = "reporter_id,target_type,target_id"; ignoreDuplicates = true }
        }
    }

    /** Bloqueia um usuário. Cache local imediato (some da UI na hora) + fila. */
    override suspend fun blockUser(me: String, blockedId: String) {
        if (me == blockedId) return
        dao.upsertUserBlock(UserBlock(me, blockedId, System.currentTimeMillis()))
        val payload = buildJsonObject { put("blockerId", me); put("blockedId", blockedId) }.toString()
        tryRemoteOrEnqueue("insert_user_block", payload) {
            supabase.from("user_blocks").upsert(UserBlockInsertDto(me, blockedId)) { ignoreDuplicates = true }
        }
    }

    /** Desbloqueia. */
    override suspend fun unblockUser(me: String, blockedId: String) {
        dao.deleteUserBlock(me, blockedId)
        val payload = buildJsonObject { put("blockerId", me); put("blockedId", blockedId) }.toString()
        tryRemoteOrEnqueue("delete_user_block", payload) {
            supabase.from("user_blocks").delete {
                filter { eq("blocker_id", me); eq("blocked_id", blockedId) }
            }
        }
    }

    /** Ids que EU bloqueei — pra esconder conteúdo desses usuários nas listas.
     *  Dispara um sync em background (padrão dos demais getters de Flow). */
    override fun observeBlockedIds(me: String): Flow<List<String>> {
        scope.launch { runCatching { syncMyBlocks(me) } }
        return dao.blockedIdsFlow(me)
    }

    override fun isBlockedFlow(me: String, other: String): Flow<Boolean> = dao.isBlockedFlow(me, other)

    private suspend fun syncMyBlocks(me: String) {
        val list = supabase.from("user_blocks").select {
            filter { eq("blocker_id", me) }
        }.decodeList<UserBlockDto>().map { it.toDomain() }
        dao.replaceUserBlocks(me, list)
    }

    private fun tableForTarget(type: ReportTargetType): String = when (type) {
        ReportTargetType.COMMENT -> "comments"
        ReportTargetType.SAVED_QUOTE -> "saved_quotes"
        ReportTargetType.BOOK_SUGGESTION -> "book_suggestions"
        ReportTargetType.BOOK_RATING -> "book_ratings"
        else -> "comments"
    }

    /** Admin remove conteúdo abusivo. Chama moderate_remove_content (checa admin no
     *  servidor) e reflete local otimista. targetId: id da linha (ou book_id p/ rating). */
    override suspend fun moderateRemoveContent(
        type: ReportTargetType,
        targetId: String,
        targetUserId: String,
        clubId: String,
        motivo: String?,
        removidoPor: String,
    ) {
        when (type) {
            ReportTargetType.COMMENT -> dao.markCommentRemoved(targetId, removidoPor, motivo)
            ReportTargetType.SAVED_QUOTE -> dao.markQuoteRemoved(targetId, removidoPor, motivo)
            ReportTargetType.BOOK_SUGGESTION -> dao.markSuggestionRemoved(targetId, removidoPor, motivo)
            ReportTargetType.BOOK_RATING -> dao.markRatingRemoved(targetId, clubId, targetUserId, removidoPor, motivo)
            else -> {}
        }
        val payload = buildJsonObject {
            put("type", type.wire)
            put("targetId", targetId)
            put("targetUserId", targetUserId)
            put("clubId", clubId)
            if (!motivo.isNullOrBlank()) put("motivo", motivo)
        }.toString()
        tryRemoteOrEnqueue("moderate_remove", payload, notifyTable = tableForTarget(type)) {
            supabase.postgrest.rpc("moderate_remove_content", buildJsonObject {
                put("p_type", type.wire)
                put("p_target_id", targetId)
                put("p_target_user_id", targetUserId)
                put("p_club_id", clubId)
                put("p_motivo", motivo)
            })
        }
    }

    /** Fila de denúncias pendentes de um clube (só admin lê — RLS garante). Online. */
    override suspend fun fetchPendingReports(clubId: String): List<ContentReport> = runCatching {
        supabase.from("content_reports").select {
            filter { eq("club_id", clubId); eq("status", "pendente") }
            order("created_at", Order.DESCENDING)
        }.decodeList<ContentReportDto>().map { it.toDomain() }
    }.getOrDefault(emptyList())

    /** Admin descarta denúncia (improcedente) sem remover conteúdo. */
    override suspend fun dismissReport(reportId: String) {
        runCatching {
            supabase.postgrest.rpc("dismiss_report", buildJsonObject { put("p_report_id", reportId) })
        }
    }

    override fun getRemovedCommentsForClubFlow(clubId: String): Flow<List<Comment>> {
        scope.launch {
            runCatching {
                val list = supabase.from("comments").select {
                    filter {
                        eq("club_id", clubId)
                        eq("removido", true)
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<CommentDto>().map { it.toDomain() }
                dao.upsertComments(list)
            }
        }
        return dao.removedCommentsForClubFlow(clubId)
    }
}
