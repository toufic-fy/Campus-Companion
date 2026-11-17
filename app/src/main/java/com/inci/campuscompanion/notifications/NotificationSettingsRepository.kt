package com.inci.campuscompanion.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.notificationDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "notification_settings")

class NotificationSettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.notificationDataStore

    private val eventUpdatesKey = booleanPreferencesKey("event_updates_enabled")

    val settings: Flow<NotificationSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            NotificationSettings(
                eventUpdatesEnabled = prefs[eventUpdatesKey] ?: false
            )
        }

    suspend fun setEventUpdatesEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[eventUpdatesKey] = enabled
        }
    }
}