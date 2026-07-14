package com.example.data.remote.repo

import com.example.data.db.RodapeDao
import com.example.data.model.UserProgress
import com.example.data.remote.SyncEngine
import com.example.data.remote.UserProgressDto
import com.example.data.remote.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Impl offline-first do progresso de leitura. Bodies movidos verbatim da
 * RemoteRepository (F3c) — mesma lógica local-first: grava no Room, tenta
 * remoto/enfileira, realtime reconcilia. Só a infra passou a vir do kernel
 * injetado (`engine.tryRemoteOrEnqueue/ensureRealtime/scope`); `dao` e
 * `supabase` são injetados direto (mesmos singletons do DataModule).
 */
@Singleton
internal class OfflineFirstProgressRepository @Inject constructor(
    private val engine: SyncEngine,
    private val supabase: SupabaseClient,
    private val dao: RodapeDao,
) : ProgressRepository {

    override suspend fun insertUserProgress(progress: UserProgress) {
        // Local-first: grava no Room ANTES de tentar o remoto e, se a rede
        // falhar, enfileira pra retry. Antes era remoto-primeiro dentro de
        // runCatching — offline, o progresso de leitura sumia sem aviso.
        dao.upsertProgress(progress)
        // NÃO notificar antes do remoto confirmar: notifyLocalMutation dispara um
        // reload (replace+prune) que sobrescrevia o progresso OTIMISTA com o valor
        // ANTIGO do servidor — "marcar progresso" avançava e revertia na hora
        // ("pisca e some"). Agora o notify vai como notifyTable e só roda no sucesso
        // (aí o servidor já tem o novo valor e o reload reconcilia sem reverter).
        val payload = buildJsonObject {
            put("userId", progress.userId)
            put("clubId", progress.clubId)
            put("bookId", progress.bookId)
            put("currentChapter", progress.currentChapter.toString())
        }.toString()
        engine.tryRemoteOrEnqueue("upsert_user_progress", payload, notifyTable = "user_progress") {
            supabase.from("user_progress").upsert(progress.toDto())
        }
    }

    override fun getUserProgressFlow(userId: String, clubId: String, bookId: String): Flow<UserProgress?> {
        val reload: suspend () -> Unit = {
            val p = runCatching {
                supabase.from("user_progress").select {
                    filter {
                        eq("user_id", userId)
                        eq("club_id", clubId)
                        eq("book_id", bookId)
                    }
                    limit(1)
                }.decodeSingleOrNull<UserProgressDto>()?.toDomain()
            }.getOrNull()
            if (p != null) dao.upsertProgress(p)
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("user_progress", filterColumn = "user_id", filterValue = userId, reload = reload)
        return dao.progressFlow(userId, clubId, bookId)
    }

    override suspend fun getUserProgress(userId: String, clubId: String, bookId: String): UserProgress? {
        dao.progress(userId, clubId, bookId)?.let { return it }
        return runCatching {
            supabase.from("user_progress").select {
                filter {
                    eq("user_id", userId)
                    eq("club_id", clubId)
                    eq("book_id", bookId)
                }
                limit(1)
            }.decodeSingleOrNull<UserProgressDto>()?.toDomain()
                ?.also { dao.upsertProgress(it) }
        }.getOrNull()
    }

    override fun getAllProgressForClubFlow(clubId: String): Flow<List<UserProgress>> {
        val reload: suspend () -> Unit = {
            runCatching {
                val list = supabase.from("user_progress").select {
                    filter { eq("club_id", clubId) }
                }.decodeList<UserProgressDto>().map { it.toDomain() }
                dao.upsertProgresses(list)
            }
        }
        engine.scope.launch { runCatching { reload() } }
        engine.ensureRealtime("user_progress", reload = reload)
        return dao.allProgressForClubFlow(clubId)
    }
}
