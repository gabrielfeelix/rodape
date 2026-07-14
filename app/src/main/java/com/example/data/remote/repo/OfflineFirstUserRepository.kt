package com.example.data.remote.repo

import com.example.data.db.RodapeDao
import com.example.data.model.User
import com.example.data.remote.ProfileDto
import com.example.data.remote.ProfileUpdateDto
import com.example.data.remote.SyncEngine
import com.example.data.remote.Ttl
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineFirstUserRepository @Inject constructor(
    private val engine: SyncEngine,
    private val supabase: SupabaseClient,
    private val dao: RodapeDao,
) : UserRepository {

    override fun getUserFlow(userId: String): Flow<User?> {
        // Reload manual (via realtime/mutation) ignora TTL — sempre re-busca.
        val reload: suspend () -> Unit = { syncUser(userId); engine.markSynced("user:$userId") }
        // Trigger on view: respeita TTL.
        engine.scope.launch { engine.syncOnce("user:$userId", Ttl.SLOW) { syncUser(userId) } }
        engine.ensureRealtime("profiles", filterColumn = "id", filterValue = userId, reload = reload)
        return dao.userFlow(userId)
    }

    private suspend fun syncUser(userId: String) {
        val u = runCatching {
            supabase.from("profiles").select {
                filter { eq("id", userId) }
                limit(1)
            }.decodeSingleOrNull<ProfileDto>()?.toDomain()
        }.getOrNull()
        if (u != null) dao.upsertUser(u)
    }

    override suspend fun getUser(userId: String): User? {
        // Snapshot: prefere Room (rapido), busca remoto se nao tem.
        dao.user(userId)?.let { return it }
        syncUser(userId)
        return dao.user(userId)
    }

    /** Nao usado hoje (RLS limita visibilidade); mantido como stub vazio. */
    override fun getAllUsersFlow(): Flow<List<User>> = flowOf(emptyList())

    override suspend fun insertUser(user: User) {
        // Apenas atualiza profile do USUARIO LOGADO (RLS bloqueia outros).
        // Para criar profile de outro usuario o trigger handle_new_user e quem faz.
        //
        // Local-first: grava no Room ANTES do remoto e enfileira se offline. Antes
        // era remoto-primeiro dentro de runCatching SEM escrita local — editar o
        // perfil (nome/avatar) offline sumia sem aviso e a saudacao nao mudava.
        val partes = user.nome.trim().split(" ", limit = 2)
        val nome = partes.firstOrNull()?.ifBlank { user.nome } ?: user.nome
        val sobrenome = if (partes.size > 1) partes[1].trim().ifBlank { null } else null
        val avatarKey = user.avatarUrl.ifBlank { "preset:leitor" }
        val pronome = user.pronome?.trim()?.ifBlank { null }
        dao.upsertUser(user.copy(avatarUrl = avatarKey, pronome = pronome))
        engine.notifyLocalMutation("profiles")
        val payload = buildJsonObject {
            put("id", user.id)
            put("nome", nome)
            if (sobrenome != null) put("sobrenome", sobrenome)
            put("avatarKey", avatarKey)
            if (pronome != null) put("pronome", pronome)
        }.toString()
        engine.tryRemoteOrEnqueue("upsert_profile", payload) {
            supabase.from("profiles").upsert(
                ProfileUpdateDto(id = user.id, nome = nome, sobrenome = sobrenome, avatarKey = avatarKey, pronome = pronome)
            )
        }
    }

    override suspend fun updateFontScale(userId: String, scale: Float) {
        runCatching {
            supabase.from("profiles").update({ set("font_scale", scale) }) {
                filter { eq("id", userId) }
            }
        }
    }
}
