package com.campus.events.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "notification_settings")

/** The product choice: does this person want Event updates from this app? */
data class NotificationSettings(
    val eventUpdatesEnabled: Boolean,
)

/**
 * Stores the user's *preference* only. Android's permission state is never copied here;
 * it is always queried from the system (see NotificationPermissionChecker).
 */
class NotificationSettingsRepository(private val context: Context) {

    val settings: Flow<NotificationSettings> = context.dataStore.data.map { prefs ->
        NotificationSettings(eventUpdatesEnabled = prefs[EVENT_UPDATES_ENABLED] ?: false)
    }

    /** Short human-readable description of what the last refresh decided (for the demo UI). */
    val lastRefreshOutcome: Flow<String?> = context.dataStore.data.map { it[LAST_OUTCOME] }

    suspend fun setEventUpdatesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[EVENT_UPDATES_ENABLED] = enabled }
    }

    suspend fun setLastRefreshOutcome(text: String) {
        context.dataStore.edit { it[LAST_OUTCOME] = text }
    }

    private companion object {
        val EVENT_UPDATES_ENABLED = booleanPreferencesKey("event_updates_enabled")
        val LAST_OUTCOME = stringPreferencesKey("last_refresh_outcome")
    }
}
