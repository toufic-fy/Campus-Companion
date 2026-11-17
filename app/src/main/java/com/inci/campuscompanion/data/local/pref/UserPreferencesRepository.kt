package com.inci.campuscompanion.data.local.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val EVENT_UPDATES_ENABLED = booleanPreferencesKey("event_updates_enabled")
        val LAST_SEEN_EVENT_IDS = stringSetPreferencesKey("last_seen_event_ids")
    }

    private val preferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val isDarkMode: Flow<Boolean> = preferences
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] ?: false
        }

    /** The product choice. Off by default: the student opts in to event notifications. */
    val eventUpdatesEnabled: Flow<Boolean> = preferences
        .map { preferences ->
            preferences[PreferencesKeys.EVENT_UPDATES_ENABLED] ?: false
        }

    /** Event ids from the last confirmed refresh; null until the first refresh finishes. */
    val lastSeenEventIds: Flow<Set<String>?> = preferences
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_EVENT_IDS]
        }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun setLastSeenEventIds(ids: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_EVENT_IDS] = ids
        }
    }

    suspend fun setEventUpdatesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EVENT_UPDATES_ENABLED] = enabled
        }
    }
}
