package com.example.data.remote.repo

import com.example.data.model.BookSuggestion
import com.example.data.model.Vote
import com.example.data.model.VotingRound
import com.example.data.remote.BookSuggestionDto
import com.example.data.remote.BookSuggestionInsertDto
import com.example.data.remote.IdOnlyDto
import com.example.data.remote.SyncEngine
import com.example.data.remote.VoteDto
import com.example.data.remote.VoteInsertDto
import com.example.data.remote.VotingRoundDto
import com.example.data.remote.VotingRoundInsertDto
import com.example.data.remote.toIso
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

// F3c: votação — votes + voting_rounds + book_suggestions (o mapa junta os
// três: sugestões alimentam a rodada e deleteVotesForBook muta votes).
// closeVotingRoundViaRpc realocado de CLUBS pro repo semântico. Corpos
// movidos VERBATIM do RemoteRepository — comportamento idêntico.
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
    suspend fun closeVotingRoundViaRpc(roundId: String)
    suspend fun closeVotingRound(id: String, vencedoresJson: String)
    suspend fun insertBookSuggestion(suggestion: BookSuggestion)
    fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?>
    fun getBookSuggestionsForClubFlow(clubId: String): Flow<List<BookSuggestion>>
    suspend fun deleteBookSuggestion(bookId: String, clubId: String)
    suspend fun deleteVotesForBook(bookId: String)
}

internal class OfflineFirstVotingRepository @Inject constructor(
    engine: SyncEngine,
) : OfflineFirstRepository(engine), VotingRepository {

    override suspend fun insertVote(vote: Vote) {
        val roundId = vote.votingRoundId ?: return
        setUserVoteInRound(vote.userId, roundId, vote.clubBookId)
    }

    /**
     * Define o voto do usuário na rodada (VOTO ÚNICO). Troca ATÔMICA:
     *  - Local: apaga os votos do usuário na rodada + insere o novo (otimista). Sem
     *    isso o Room ficava com [A,B] (PK local permite N) e um reload com dado velho
     *    podava o novo e revertia pro antigo ("vira teu voto e volta, em loop").
     *  - Remoto: UM upsert com onConflict na PK (round,user) — substitui A por B numa
     *    operação só (sem delete+insert separados, que disparavam reload prematuro).
     *  - notifyTable só no sucesso: o reload só roda quando o servidor já tem B.
     */
    override suspend fun setUserVoteInRound(userId: String, roundId: String, bookId: String) {
        dao.deleteUserVotesInRound(roundId, userId)
        dao.upsertVotes(listOf(Vote(votingRoundId = roundId, clubBookId = bookId, userId = userId, votedAt = System.currentTimeMillis())))
        val payload = """{"votingRoundId":"$roundId","userId":"$userId","bookId":"$bookId"}"""
        tryRemoteOrEnqueue("insert_vote", payload, notifyTable = "votes") {
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
            notifyLocalMutation("votes")
        }
    }

    override fun getVotesForClubFlow(clubId: String): Flow<List<Vote>> {
        // Sem cache local especifico — esse flow nao e critico (UI tem flow por round).
        val flow = stateOf<List<Vote>>(emptyList())
        scope.launch {
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
        scope.launch { runCatching { reload() } }
        ensureRealtime("votes", filterColumn = "voting_round_id", filterValue = roundId, reload = reload)
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
        // Optimistic local + fila (espelha insertVote): desfazer voto offline agora
        // persiste. Antes era remoto-primeiro sem apagar a linha do Room, entao o
        // voto "voltava" ate um reload via Realtime.
        dao.deleteVote(roundId, userId, bookId)
        val payload = buildJsonObject {
            put("votingRoundId", roundId); put("userId", userId); put("bookId", bookId)
        }.toString()
        tryRemoteOrEnqueue("delete_vote", payload, notifyTable = "votes") {
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

    // ---- voting_rounds ----

    override suspend fun insertVotingRound(round: VotingRound) {
        // Local-first: grava no Room ANTES do remoto, pra a votação aparecer na
        // hora mesmo se o remoto demorar/falhar. Loga a falha (antes era engolida).
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
        }.onSuccess { notifyLocalMutation("voting_rounds") }
            .onFailure { android.util.Log.e("RodapeWrite", "insertVotingRound remoto falhou", it) }
    }

    override fun getActiveVotingRoundFlow(clubId: String): Flow<VotingRound?> {
        val reload: suspend () -> Unit = {
            runCatching {
                val r = getActiveVotingRound(clubId)
                if (r != null) dao.upsertVotingRound(r)
            }
        }
        scope.launch { runCatching { reload() } }
        ensureRealtime("voting_rounds", filterColumn = "club_id", filterValue = clubId, reload = reload)
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

    override suspend fun closeVotingRoundViaRpc(roundId: String) {
        runCatching {
            supabase.postgrest.rpc("close_voting_round", buildJsonObject {
                put("p_round_id", roundId)
            })
        }
    }

    override suspend fun closeVotingRound(id: String, vencedoresJson: String) {
        // Preferir RPC close_voting_round (que faz tudo: marca finished, promove vencedor, notifica).
        // Mas o MainViewModel hoje ja faz parte desse trabalho manualmente, entao usamos
        // a RPC + ignoramos o vencedoresJson legado.
        closeVotingRoundViaRpc(id)
        notifyLocalMutation("voting_rounds")
        notifyLocalMutation("club_books") // RPC promoveu vencedor (next/current)
        notifyLocalMutation("notifications") // RPC criou notifs pros membros
    }

    // ---- book_suggestions ----

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
        tryRemoteOrEnqueue("insert_book_suggestion", json.encodeToString(dto), notifyTable = "book_suggestions") {
            supabase.from("book_suggestions").upsert(dto)
        }
    }

    override fun getBookSuggestionFlow(bookId: String, clubId: String): Flow<BookSuggestion?> {
        scope.launch {
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
        scope.launch { runCatching { reload() } }
        ensureRealtime("book_suggestions", filterColumn = "club_id", filterValue = clubId, reload = reload)
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
            notifyLocalMutation("book_suggestions")
        }
    }

    override suspend fun deleteVotesForBook(bookId: String) {
        runCatching {
            supabase.from("votes").delete { filter { eq("book_id", bookId) } }
            notifyLocalMutation("votes")
        }
    }
}
