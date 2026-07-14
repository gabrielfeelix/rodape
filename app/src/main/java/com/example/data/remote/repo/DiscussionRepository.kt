package com.example.data.remote.repo

import com.example.data.model.Comment
import com.example.data.model.Reaction
import com.example.data.remote.CommentDto
import com.example.data.remote.CommentInsertDto
import com.example.data.remote.IdOnlyDto
import com.example.data.remote.ReactionDto
import com.example.data.remote.SyncEngine
import com.example.data.remote.Ttl
import com.example.data.remote.escapeJson
import com.example.data.remote.toDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

// F3c: discussão — comments + reactions. Corpos movidos VERBATIM do
// RemoteRepository — comportamento idêntico.
interface DiscussionRepository {
    suspend fun insertComment(comment: Comment)
    fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>>
    fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>>
    suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String)
    suspend fun restoreComment(commentId: String)
    suspend fun editOwnComment(commentId: String, novoTexto: String)
    suspend fun deleteOwnComment(commentId: String)
    suspend fun insertReaction(reaction: Reaction)
    suspend fun deleteReaction(reaction: Reaction)
    fun getReactionsForCommentFlow(commentId: String): Flow<List<Reaction>>
    fun getReactionsForChapterFlow(chapterId: String): Flow<List<Reaction>>
}

internal class OfflineFirstDiscussionRepository @Inject constructor(
    engine: SyncEngine,
) : OfflineFirstRepository(engine), DiscussionRepository {

    override suspend fun insertComment(comment: Comment) {
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

    override fun getCommentsForChapterFlow(chapterId: String, clubId: String): Flow<List<Comment>> {
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

    override fun getCommentsForBookFlow(bookId: String, clubId: String): Flow<List<Comment>> {
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

    override suspend fun softRemoveComment(commentId: String, removidoPor: String, motivo: String) {
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

    override suspend fun restoreComment(commentId: String) {
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
    override suspend fun editOwnComment(commentId: String, novoTexto: String) {
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
    override suspend fun deleteOwnComment(commentId: String) {
        dao.deleteComment(commentId)
        val payload = buildJsonObject { put("id", commentId) }.toString()
        tryRemoteOrEnqueue("delete_comment", payload, notifyTable = "comments") {
            supabase.from("comments").delete { filter { eq("id", commentId) } }
        }
    }

    override suspend fun insertReaction(reaction: Reaction) {
        dao.upsertReaction(reaction)
        val payload = """{"commentId":"${reaction.commentId}","userId":"${reaction.userId}","emoji":"${reaction.emoji}"}"""
        tryRemoteOrEnqueue("insert_reaction", payload, notifyTable = "reactions") {
            supabase.from("reactions").upsert(reaction.toDto())
        }
    }

    override suspend fun deleteReaction(reaction: Reaction) {
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

    override fun getReactionsForCommentFlow(commentId: String): Flow<List<Reaction>> {
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
        scope.launch { runCatching { reload() } }
        ensureRealtime("reactions", reload = reload)
        return dao.reactionsForChapterFlow(chapterId)
    }
}
