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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstVotingRepository @Inject constructor(
    private val engine: SyncEngine,
    private val supabase: SupabaseClient,
    private val dao: RodapeDao,
) : VotingRepository {

    override suspend fun insertVote(vote: Vote) {
        val roundId = vote.votingRoundId ?: return
        setUserVoteInRound(vote.userId, roundId, vote.clubBookId)
    }

    override suspend fun setUserVoteInRound(userId: String, roundId: String, bookId: String) {
        dao.deleteUserVotesInRound(roundId, userId)
        dao.upsertVotes(listOf(Vote(votingRoundId = roundId, clubBookId = bookId, userId = userId, votedAt = System.currentTimeMillis())))
        val payload = """{"votingRoundId":"$roundId","userId":"$userId","bookId":"$bookId"}"""
        engine.tryRemoteOrEnqueue("insert_vote", payload, notifyTable = "votes") {
            supabase.from("votes").upsert(
                VoteInsertDto(votingRoundId = roundId, userId = userId, bookId = bookId)
            ) { onConflict = "voting_round_id,user_id" }
        }
    }

    override suspend fun clearVotesForUserInClub(userId: String, clubId: String) {
        runCatching {
            val roundIds = supabase.from("voting_rounds").select(Columns.raw("id")) {
                filter { eq("club_id", clubId) }
            }.decodeList<IdOnlyDto>().map { it.id }
            if (roundIds.isEmpty()) return@runCatching
            supabase.from("votes").delete {
                filter {
                    eq("user_id", userId)
                    isIn("voting_round_id", roundIds)
                }
            }
            engine.notifyLocalMutation("votes")
        }
    }

    override fun getVotesForClubFlow(clubId: String): Flow<List<Vote>> {
        // Sem cache local especifico — esse flow nao e critico (UI tem flow por round).
        val flow = engine.stateOf<List<Vote>>(emptyList())
        engine.scope.launch {
            flow.value = runCatching {
                val roundIds = supabase.from("voting_rounds").select(Columns.raw("id")) {
                    filter { eq("club_id", clubId) }
                }.decodeList<IdOnlyDto>().map { it.id }
                if (roundIds.isEmpty()) emptyList()
                else supabase.from("votes").select {
                    filter { isIn("voting_round_id", roundIds) }
                }.decodeList<VoteDto>().map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return flow.asStateFlow()
    }

    override fun getVotesForRoundFlow(roundId: String): Flow<List<Vote>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("votes").select {
                    filter { eq("voting_round_id", roundId) }
                }.decodeList<VoteDto>().map { it.toDomain() }
                dao.replaceVotesInRound(roundId, list)
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("votes", filterColumn = "voting_round_id", filterValue = roundId, reload = reload)
        return dao.votesForRoundFlow(roundId)
    }

    override suspend fun getVotesForRound(roundId: String): List<Vote> {
        // Prefere Room; se vazio, busca remoto
        val cached = dao.votesForRound(roundId)
        if (cached.isNotEmpty()) return cached
        return runCatching {
            supabase.from("votes").select {
                filter { eq("voting_round_id", roundId) }
            }.decodeList<VoteDto>().map { it.toDomain() }
                .also { dao.upsertVotes(it) }
        }.getOrDefault(emptyList())
    }

    override suspend fun removeUserVoteForBookInRound(userId: String, roundId: String, bookId: String) {
        // Optimistic local + fila (espelha insertVote): desfazer voto offline agora persiste.
        dao.deleteVote(roundId, userId, bookId)
        val payload = buildJsonObject {
            put("votingRoundId", roundId); put("userId", userId); put("bookId", bookId)
        }.toString()
        engine.tryRemoteOrEnqueue("delete_vote", payload, notifyTable = "votes") {
            supabase.from("votes").delete {
                filter {
                    eq("user_id", userId)
                    eq("voting_round_id", roundId)
                    eq("book_id", bookId)
                }
            }
        }
    }

    override suspend fun countUserVotesInRound(userId: String, roundId: String): Int = runCatching {
        supabase.from("votes").select {
            filter {
                eq("user_id", userId)
                eq("voting_round_id", roundId)
            }
        }.decodeList<VoteDto>().size
    }.getOrDefault(0)

    override suspend fun insertVotingRound(round: VotingRound) {
        // Local-first: grava no Room ANTES do remoto, pra a votação aparecer na hora.
        dao.upsertVotingRound(round)
        runCatching {
            supabase.from("voting_rounds").upsert(
                VotingRoundInsertDto(
                    id = round.id,
                    clubId = round.clubId,
                    criadoPor = round.criadoPor,
                    fechaEm = round.fechaEm.toIso(),
                    nLivros = round.nLivros,
                    cadencia = round.cadencia,
                    status = round.status,
                )
            )
        }.onSuccess { engine.notifyLocalMutation("voting_rounds") }
            .onFailure { android.util.Log.e("RodapeWrite", "insertVotingRound remoto falhou", it) }
    }

    override fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?> {
        val reload: suspend () -> Unit = {
            runCatching {
                val r = getActiveVotingRound(clubId)
                if (r != null) dao.upsertVotingRound(r)
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("voting_rounds", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.activeRoundForClubFlow(clubId)
    }

    override suspend fun getActiveVotingRound(clubId: String): VotingRound? = runCatching {
        supabase.from("voting_rounds").select {
            filter {
                eq("club_id", clubId)
                eq("status", "aberta")
            }
            order("aberta_em", Order.DESCENDING)
            limit(1)
        }.decodeSingleOrNull<VotingRoundDto>()?.toDomain()
    }.getOrNull()

    override suspend fun closeVotingRound(id: String, vencedoresJson: String) {
        // Preferir RPC close_voting_round (marca finished, promove vencedor, notifica).
        closeVotingRoundViaRpc(id)
        engine.notifyLocalMutation("voting_rounds")
        engine.notifyLocalMutation("club_books") // RPC promoveu vencedor (next/current)
        engine.notifyLocalMutation("notifications") // RPC criou notifs pros membros
    }

    override suspend fun closeVotingRoundViaRpc(roundId: String) {
        runCatching {
            supabase.postgrest.rpc("close_voting_round", buildJsonObject {
                put("p_round_id", roundId)
            })
        }
    }

    override suspend fun insertBookSuggestion(suggestion: BookSuggestion) {
        // Optimistic local-first + fila (P0-2): offline não perde mais a sugestão.
        dao.upsertBookSuggestions(listOf(suggestion))
        val dto = BookSuggestionInsertDto(
            id = suggestion.id,
            clubId = suggestion.clubId,
            bookId = suggestion.bookId,
            sugeridoPor = suggestion.suggestedByUserId,
            justificativa = suggestion.justificativa.ifBlank { null },
        )
        engine.tryRemoteOrEnqueue("insert_book_suggestion", engine.json.encodeToString(dto), notifyTable = "book_suggestions") {
            supabase.from("book_suggestions").upsert(dto)
        }
    }

    override fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?> {
        engine.scope.launch {
            runCatching {
                val s = supabase.from("book_suggestions").select {
                    filter {
                        eq("book_id", bookId)
                        eq("club_id", clubId)
                    }
                    limit(1)
                }.decodeSingleOrNull<BookSuggestionDto>()?.toDomain()
                if (s != null) dao.upsertBookSuggestions(listOf(s))
            }
        }
        return dao.bookSuggestionFlow(clubId, bookId)
    }

    override fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("book_suggestions").select {
                    filter { eq("club_id", clubId) }
                }.decodeList<BookSuggestionDto>().map { it.toDomain() }
                dao.replaceBookSuggestionsInClub(clubId, list)
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("book_suggestions", filterColumn = "club_id", filterValue = clubId, reload = reload)
        return dao.bookSuggestionsForClubFlow(clubId)
    }

    override suspend fun deleteBookSuggestion(bookId: String, clubId: String) {
        runCatching {
            supabase.from("book_suggestions").delete {
                filter {
                    eq("book_id", bookId)
                    eq("club_id", clubId)
                }
            }
            dao.deleteBookSuggestion(clubId, bookId)
            engine.notifyLocalMutation("book_suggestions")
        }
    }

    override suspend fun deleteVotesForBook(bookId: String) {
        runCatching {
            supabase.from("votes").delete { filter { eq("book_id", bookId) } }
            engine.notifyLocalMutation("votes")
        }
    }
}
