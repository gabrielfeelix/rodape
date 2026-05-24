package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
}
