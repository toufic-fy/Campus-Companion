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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val EVENT_NOTIFICATIONS_ENABLED = booleanPreferencesKey("event_notifications_enabled")
        val SEEN_EVENTS_SNAPSHOT = stringSetPreferencesKey("seen_events_snapshot")
    }

    private val safeData: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val isDarkMode: Flow<Boolean> = safeData
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] ?: false
        }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }

    /** Product choice: event notifications are OFF until the student turns them on. */
    val eventNotificationsEnabled: Flow<Boolean> = safeData
        .map { preferences ->
            preferences[PreferencesKeys.EVENT_NOTIFICATIONS_ENABLED] ?: false
        }

    suspend fun setEventNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EVENT_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /** Last list of events the app has seen; null before the first successful refresh. */
    suspend fun getSeenEventsSnapshot(): Set<String>? =
        safeData.first()[PreferencesKeys.SEEN_EVENTS_SNAPSHOT]

    suspend fun setSeenEventsSnapshot(snapshot: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEEN_EVENTS_SNAPSHOT] = snapshot
        }
    }
}
