package com.example.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Wrapper fino sobre Supabase Auth. Cada metodo propaga excecoes —
 * a UI trata via try/catch + Snackbar nesta fase (sem Result/sealed).
 */
class AuthRepository(private val supabase: SupabaseClient = Supabase.client) {

    val sessionStatus: StateFlow<SessionStatus> = supabase.auth.sessionStatus

    /** UUID do usuario logado, ou null se nao autenticado. */
    val currentUserIdFlow: Flow<String?> = sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.id
    }

    /** Email do usuario logado (de auth.users), ou null. */
    val currentEmailFlow: Flow<String?> = sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.email
    }

    /** Nome de exibicao do usuario logado: prefere user_metadata.full_name
     *  (preenchido por cadastro email/senha ou pelo Google), cai pro email
     *  antes do @ se nao houver. Null se nao autenticado. */
    val currentDisplayNameFlow: Flow<String?> = sessionStatus.map { status ->
        val user = (status as? SessionStatus.Authenticated)?.session?.user ?: return@map null
        val meta = user.userMetadata
        val fullName = runCatching { meta?.get("full_name")?.jsonPrimitive?.contentOrNull }.getOrNull()
        val name = runCatching { meta?.get("name")?.jsonPrimitive?.contentOrNull }.getOrNull()
        fullName ?: name ?: user.email?.substringBefore("@")
    }

    /** Snapshot do usuario logado (null se nao autenticado). */
    val currentUser: UserInfo?
        get() = (sessionStatus.value as? SessionStatus.Authenticated)?.session?.user

    suspend fun signUpWithEmail(email: String, password: String, displayName: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            // displayName vira user_metadata.full_name; o trigger handle_new_user
            // le esse campo e popula profiles.nome.
            data = buildJsonObject {
                put("full_name", JsonPrimitive(displayName))
            }
        }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String? = null) {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            this.nonce = rawNonce
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        supabase.auth.resetPasswordForEmail(email)
    }

    /** Chamada depois que o deep link de reset trouxe a sessao temporaria. */
    suspend fun updatePassword(newPassword: String) {
        supabase.auth.updateUser {
            password = newPassword
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}
