package com.example.data.remote.repo

import com.example.data.model.Comment
import com.example.data.model.ContentReport
import com.example.data.model.ReportReason
import com.example.data.model.ReportTargetType
import kotlinx.coroutines.flow.Flow

/**
 * Moderação: denúncias, bloqueios e remoção admin (tabelas `content_reports`,
 * `user_blocks` + RPCs `moderate_remove_content`/`dismiss_report`). Fatiado (F3c).
 */
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
