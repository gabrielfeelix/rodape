package com.example.data.remote.repo

import com.example.data.db.RodapeDao
import com.example.data.model.*
import com.example.data.remote.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstDiscussionRepository @Inject constructor(
    private val engine: SyncEngine,
    private val supabase: SupabaseClient,
    private val dao: RodapeDao,
) : DiscussionRepository {

    override suspend fun insertComment(comment: Comment) {
        // 1. Optimistic local PRIMEIRO — UI ja mostra
        dao.upsertComment(comment)
        // 2. Tenta HTTP — se falhar, queue
        val payload = """{"id":"${comment.id}","chapterId":"${comment.chapterId}","clubId":"${comment.clubId}","userId":"${comment.userId}","texto":"${comment.texto.escapeJson()}"}"""
        engine.tryRemoteOrEnqueue("insert_comment", payload, notifyTable = "comments") {
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

    override fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>> {
        val key = "comments:ch:$chapterId"
        val reload: suspend () -> Unit = { syncCommentsForChapter(chapterId, clubId); engine.markSynced(key) }
        engine.scope.launch { engine.syncOnce(key, Ttl.FAST) { syncCommentsForChapter(chapterId, clubId) } }
        engine.ensureRealtime("comments", filterColumn = "club_id", filterValue = clubId, reload = reload)
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

    override fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>> {
        engine.scope.launch {
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

    override suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String) {
        // Optimistic local + fila: moderar offline agora persiste.
        dao.markCommentRemoved(commentId, removidoPor, motivo)
        val payload = buildJsonObject {
            put("id", commentId); put("by", removidoPor); put("motivo", motivo)
        }.toString()
        engine.tryRemoteOrEnqueue("remove_comment", payload, notifyTable = "comments") {
            supabase.from("comments").update({
                set("removido", true)
                set("removido_por", removidoPor)
                set("motivo_remocao", motivo)
            }) { filter { eq("id", commentId) } }
        }
    }

    override suspend fun restoreComment(commentId: String) {
        dao.markCommentRestored(commentId)
        val payload = buildJsonObject { put("id", commentId) }.toString()
        engine.tryRemoteOrEnqueue("restore_comment", payload, notifyTable = "comments") {
            supabase.from("comments").update({
                set("removido", false)
                set("removido_por", JsonNull)
                set("motivo_remocao", JsonNull)
            }) { filter { eq("id", commentId) } }
        }
    }

    override suspend fun editOwnComment(commentId: String, novoTexto: String) {
        dao.updateCommentText(commentId, novoTexto)
        val payload = buildJsonObject { put("id", commentId); put("texto", novoTexto) }.toString()
        engine.tryRemoteOrEnqueue("edit_comment", payload, notifyTable = "comments") {
            supabase.from("comments").update({ set("texto", novoTexto) }) {
                filter { eq("id", commentId) }
            }
        }
    }

    override suspend fun deleteOwnComment(commentId: String) {
        dao.deleteComment(commentId)
        val payload = buildJsonObject { put("id", commentId) }.toString()
        engine.tryRemoteOrEnqueue("delete_comment", payload, notifyTable = "comments") {
            supabase.from("comments").delete { filter { eq("id", commentId) } }
        }
    }

    override suspend fun insertReaction(reaction: Reaction) {
        dao.upsertReaction(reaction)
        val payload = """{"commentId":"${reaction.commentId}","userId":"${reaction.userId}","emoji":"${reaction.emoji}"}"""
        engine.tryRemoteOrEnqueue("insert_reaction", payload, notifyTable = "reactions") {
            supabase.from("reactions").upsert(reaction.toDto())
        }
    }

    override suspend fun deleteReaction(reaction: Reaction) {
        dao.deleteReaction(reaction.commentId, reaction.userId, reaction.emoji)
        val payload = """{"commentId":"${reaction.commentId}","userId":"${reaction.userId}","emoji":"${reaction.emoji}"}"""
        engine.tryRemoteOrEnqueue("delete_reaction", payload, notifyTable = "reactions") {
            supabase.from("reactions").delete {
                filter {
                    eq("comment_id", reaction.commentId)
                    eq("user_id", reaction.userId)
                    eq("emoji", reaction.emoji)
                }
            }
        }
    }

    override fun getReactionsForCommentFlow(commentId: String): Flow<List<Reaction>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("reactions").select {
                    filter { eq("comment_id", commentId) }
                }.decodeList<ReactionDto>().map { it.toDomain() }
                dao.replaceReactionsForComment(commentId, list)
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("reactions", filterColumn = "comment_id", filterValue = commentId, reload = reload)
        return dao.reactionsForCommentFlow(commentId)
    }

    override fun getReactionsForChapterFlow(chapterId: String): Flow<List<Reaction>> {
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
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("reactions", reload = reload)
        return dao.reactionsForChapterFlow(chapterId)
    }
}
