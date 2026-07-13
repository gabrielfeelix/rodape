package com.example.push

import com.example.data.remote.Supabase
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Registro do token FCM do device na tabela `device_tokens` (migration 0007).
 *
 * A RLS garante que cada usuário só grava o próprio token (auth.uid() = user_id),
 * então basta estar autenticado. Chamado em dois pontos:
 *  - [RodapeMessagingService.onNewToken] — quando o FCM rotaciona o token;
 *  - [sync] — no login/entrada no app (pega o token atual e registra).
 */
object PushTokens {

    @Serializable
    private data class Row(
        val token: String,
        @SerialName("user_id") val userId: String,
        val platform: String = "android",
    )

    /** Upsert de um token pro usuário logado. No-op silencioso se deslogado/offline. */
    suspend fun upsert(token: String) {
        val uid = Supabase.client.auth.currentUserOrNull()?.id ?: return
        runCatching {
            Supabase.client.from("device_tokens").upsert(Row(token = token, userId = uid))
        }
    }

    /**
     * Busca o token atual do FCM e registra. Seguro chamar sempre que a sessão
     * ficar autenticada (idempotente — upsert pela PK `token`).
     */
    fun sync(scope: CoroutineScope) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (!token.isNullOrBlank()) scope.launch { upsert(token) }
        }
    }
}
