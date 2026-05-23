package com.example.voting

import com.example.data.model.BookSuggestion
import com.example.data.model.Vote
import org.junit.Assert.assertEquals
import org.junit.Test

class VotingTallyTest {

    private fun sug(bookId: String, criadoEm: Long) = BookSuggestion(
        id = "bs_$bookId", clubId = "c", bookId = bookId,
        suggestedByUserId = "u", justificativa = "", criadoEm = criadoEm
    )

    private fun v(bookId: String, userId: String) = Vote(
        clubBookId = bookId, userId = userId, votedAt = 0L, votingRoundId = "r1"
    )

    @Test
    fun `top N por contagem decrescente`() {
        val votes = listOf(
            v("a", "u1"), v("a", "u2"), v("a", "u3"),
            v("b", "u1"), v("b", "u2"),
            v("c", "u1")
        )
        val sugs = mapOf("a" to sug("a", 1), "b" to sug("b", 2), "c" to sug("c", 3))
        val result = VotingTally.rank(votes, sugs, n = 2)
        assertEquals(listOf("a", "b"), result)
    }

    @Test
    fun `empate desempata pelo criadoEm mais antigo`() {
        val votes = listOf(
            v("a", "u1"), v("a", "u2"),
            v("b", "u1"), v("b", "u2")
        )
        val sugs = mapOf("a" to sug("a", 100), "b" to sug("b", 50))
        val result = VotingTally.rank(votes, sugs, n = 1)
        assertEquals(listOf("b"), result)
    }

    @Test
    fun `N maior que disponivel retorna todos`() {
        val votes = listOf(v("a", "u1"))
        val sugs = mapOf("a" to sug("a", 1))
        val result = VotingTally.rank(votes, sugs, n = 5)
        assertEquals(listOf("a"), result)
    }

    @Test
    fun `sem votos retorna lista vazia`() {
        val result = VotingTally.rank(emptyList(), emptyMap(), n = 3)
        assertEquals(emptyList<String>(), result)
    }
}
