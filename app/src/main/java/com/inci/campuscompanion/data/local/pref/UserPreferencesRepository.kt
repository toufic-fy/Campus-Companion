
package com.inci.campuscompanion.data.local.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")

        val EVENT_NOTIFICATIONS_ENABLED =
            booleanPreferencesKey("event_notifications_enabled")

        val EVENT_SNAPSHOT = stringPreferencesKey("event_snapshot")
    }

    val isDarkMode: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] ?: false
        }

    val areEventNotificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.EVENT_NOTIFICATIONS_ENABLED] ?: false
        }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun setEventNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EVENT_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun getEventSnapshot(): String? {
        return dataStore.data
            .map { preferences ->
                preferences[PreferencesKeys.EVENT_SNAPSHOT]
            }
            .catch { exception ->
                if (exception is IOException) {
                    emit(null)
                } else {
                    throw exception
                }
            }
            .first()
    }

    suspend fun saveEventSnapshot(snapshot: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EVENT_SNAPSHOT] = snapshot
        }
    }
}
