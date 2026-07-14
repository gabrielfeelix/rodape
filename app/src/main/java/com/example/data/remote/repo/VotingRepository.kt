package com.example.data.remote.repo

import com.example.data.model.BookSuggestion
import com.example.data.model.Vote
import com.example.data.model.VotingRound
import kotlinx.coroutines.flow.Flow

/**
 * Votação: votos, rodadas e sugestões de livro (tabelas `votes`, `voting_rounds`,
 * `book_suggestions` + RPC `close_voting_round`). Fatiado da god-repo (F3c).
 */
interface VotingRepository {
    suspend fun insertVote(vote: Vote)
    suspend fun setUserVoteInRound(userId: String, roundId: String, bookId: String)
    suspend fun clearVotesForUserInClub(userId: String, clubId: String)
    fun getVotesForClubFlow(clubId: String): Flow<List<Vote>>
    fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>>
    suspend fun getVotesForRound(roundId: String): List<Vote>
    suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String)
    suspend fun countUserVotesInRound(userId: String, roundId: String): Int
    suspend fun insertVotingRound(round: VotingRound)
    fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?>
    suspend fun getActiveVotingRound(clubId: String): VotingRound?
    suspend fun closeVotingRound(id: String, vencedoresJson: String)
    suspend fun closeVotingRoundViaRpc(roundId: String)
    suspend fun insertBookSuggestion(suggestion: BookSuggestion)
    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?>
    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>>
    suspend fun deleteBookSuggestion(bookId: String, clubId: String)
    suspend fun deleteVotesForBook(bookId: String)
}
