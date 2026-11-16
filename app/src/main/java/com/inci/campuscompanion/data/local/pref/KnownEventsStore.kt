package com.inci.campuscompanion.data.local.pref

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first

class KnownEventsStore(private val dataStore: DataStore<Preferences>) {

    private val knownIdsKey = stringSetPreferencesKey("known_event_ids")

    suspend fun getKnownIds(): Set<String>? = dataStore.data.first()[knownIdsKey]

    suspend fun saveKnownIds(ids: Set<String>) {
        dataStore.edit { it[knownIdsKey] = ids }
    }
}
