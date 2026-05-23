package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rodape_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val ACTIVE_CLUB_ID_KEY = stringPreferencesKey("active_club_id")
        val RATED_APP_KEY = booleanPreferencesKey("rated_app")
        val ENGAGEMENT_COUNT_KEY = intPreferencesKey("engagement_count")
    }

    val userIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    val userNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    val userEmailFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY]
    }

    val activeClubIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_CLUB_ID_KEY]
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

    suspend fun saveSession(userId: String, name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
            prefs[USER_NAME_KEY] = name
            prefs[USER_EMAIL_KEY] = email
        }
    }

    suspend fun saveActiveClubId(clubId: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_CLUB_ID_KEY] = clubId
        }
    }

    suspend fun clearSession() {
        // Mantém RATED_APP_KEY e ENGAGEMENT_COUNT_KEY: preferências do app não dependem da sessão
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(ACTIVE_CLUB_ID_KEY)
        }
    }
}
