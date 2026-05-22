package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tramabook_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val ACTIVE_CLUB_ID_KEY = stringPreferencesKey("active_club_id")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode") // "system", "light", "dark"
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

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY] ?: "light"
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

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(ACTIVE_CLUB_ID_KEY)
        }
    }
}
