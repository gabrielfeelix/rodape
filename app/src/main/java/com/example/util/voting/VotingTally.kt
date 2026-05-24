package com.example.util.voting

import com.example.data.model.BookSuggestion
import com.example.data.model.Vote

object VotingTally {
    fun rank(
        votes: List<Vote>,
        suggestionsByBookId: Map<String, BookSuggestion>,
        n: Int
    ): List<String> {
        val tally = votes.groupingBy { it.clubBookId }.eachCount()
        return tally.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { suggestionsByBookId[it.key]?.criadoEm ?: Long.MAX_VALUE }
            )
            .map { it.key }
            .take(n)
    }
}
