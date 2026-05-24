package com.example.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import java.security.MessageDigest
import java.util.UUID

/**
 * Encapsula o fluxo "Sign in with Google" via Credential Manager.
 *
 * Usa `GetSignInWithGoogleOption` (botao explicito de Sign-In) em vez do
 * `GetGoogleIdOption` (que e One Tap-style, mostra bottom sheet leve e
 * pode parecer "logar sem perguntar"). O usuario sempre ve o picker
 * cheio e confirma qual conta usar.
 */
class GoogleSignInHelper(private val context: Context) {

    data class Result(val idToken: String, val rawNonce: String)

    suspend fun getGoogleIdToken(): Result {
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = hashSha256(rawNonce)

        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = CredentialManager.create(context).getCredential(context, request)
        val credential = response.credential
        check(credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Credential retornada nao e um GoogleIdTokenCredential"
        }
        val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return Result(idToken = tokenCredential.idToken, rawNonce = rawNonce)
    }

    private fun hashSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
