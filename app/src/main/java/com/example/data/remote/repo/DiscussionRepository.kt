package com.example.data.remote.repo

import com.example.data.model.Comment
import com.example.data.model.Reaction
import kotlinx.coroutines.flow.Flow

/** Discussão: comentários e reações (tabelas `comments`, `reactions`). Fatiado (F3c). */
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
