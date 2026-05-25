package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rodape_prefs")

/**
 * Preferencias locais do device — NUNCA sessao do usuario.
 *
 * Sessao (userId/nome/email/active club) vive em:
 *  - Supabase Auth (sessionStatus): identidade real
 *  - AuthRepository.currentDisplayNameFlow / currentEmailFlow: dados do JWT
 *  - MainViewModel._activeClubId: estado em memoria (nao persiste cold-start)
 *
 * Aqui ficam so preferencias do device-app:
 *  - rated_app: usuario ja avaliou na Play Store?
 *  - engagement_count: contador pro prompt de avaliacao
 *  - font_scale: escala de fonte preferida neste dispositivo (super-pessoal,
 *    nao faz sentido sincronizar — telefone pequeno pode preferir fonte maior
 *    que o mesmo usuario no tablet)
 */
class DataStoreManager(private val context: Context) {

    companion object {
        val RATED_APP_KEY = booleanPreferencesKey("rated_app")
        val ENGAGEMENT_COUNT_KEY = intPreferencesKey("engagement_count")
        val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
        // ID do ultimo user que logou neste device. Usado pra detectar troca
        // de conta no mesmo aparelho e forcar limpeza do cache Room antes que
        // o usuario novo veja dados do antigo. Nao e segredo (auth.uid()),
        // mas mesmo assim fica so localmente.
        val LAST_USER_ID_KEY = stringPreferencesKey("last_user_id")
        // userIds que ja completaram o onboarding pos-primeiro-login.
        // Armazenado como CSV pra suportar multiplos users no mesmo device.
        val ONBOARDED_USERS_KEY = stringPreferencesKey("onboarded_users")
    }

    val ratedAppFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[RATED_APP_KEY] ?: false
    }

    val engagementCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[ENGAGEMENT_COUNT_KEY] ?: 0
    }

    suspend fun markAppRated() {
        context.dataStore.edit { prefs ->
            prefs[RATED_APP_KEY] = true
        }
    }

    suspend fun incrementEngagementCount() {
        context.dataStore.edit { prefs ->
            prefs[ENGAGEMENT_COUNT_KEY] = (prefs[ENGAGEMENT_COUNT_KEY] ?: 0) + 1
        }
    }

    val fontScaleFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[FONT_SCALE_KEY] ?: 1.0f
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[FONT_SCALE_KEY] = scale.coerceIn(0.85f, 1.40f)
        }
    }

    /**
     * Compat: chamado por funcoes antigas (`MainViewModel.logout`). Como nao
     * temos mais nada de sessao aqui, esta fun e no-op. Existe apenas pra
     * preservar a interface — o signOut real e feito por `AuthRepository`.
     */
    suspend fun clearSession() {
        // no-op: sessao agora vive em Supabase Auth, nao no DataStore.
    }

    /** Le o ultimo userId persistido neste device (ou null se nunca logou). */
    suspend fun lastUserId(): String? =
        context.dataStore.data.first()[LAST_USER_ID_KEY]

    /** Persiste o userId atual. Chamar no login E depois de limpar cache. */
    suspend fun setLastUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) prefs.remove(LAST_USER_ID_KEY)
            else prefs[LAST_USER_ID_KEY] = userId
        }
    }

    /** Flow do conjunto de userIds que ja viram o onboarding. */
    val onboardedUsersFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDED_USERS_KEY]?.split(",")?.filter { it.isNotBlank() }?.toSet().orEmpty()
    }

    suspend fun markOnboardingDone(userId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[ONBOARDED_USERS_KEY]
                ?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(userId)
            prefs[ONBOARDED_USERS_KEY] = current.joinToString(",")
        }
    }
}
