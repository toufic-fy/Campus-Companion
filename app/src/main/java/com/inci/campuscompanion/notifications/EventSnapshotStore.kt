package com.inci.campuscompanion.notifications

import android.content.Context
import com.inci.campuscompanion.model.CampusEvent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Step 6: remembers the events from the last background refresh,
 * so the next refresh can tell what actually changed.
 */
class EventSnapshotStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("event_snapshots", Context.MODE_PRIVATE)

    /** null = we never saved anything yet (first refresh). */
    fun load(): Map<String, EventSnapshot>? {
        val json = prefs.getString(KEY_EVENTS, null) ?: return null
        val array = JSONArray(json)
        return (0 until array.length()).associate { index ->
            val o = array.getJSONObject(index)
            val snapshot = EventSnapshot(
                id = o.getString("id"),
                title = o.getString("title"),
                startsAt = o.getString("startsAt"),
                endsAt = o.optStringOrNull("endsAt"),
                location = o.optStringOrNull("location")
            )
            snapshot.id to snapshot
        }
    }

    fun save(events: List<CampusEvent>) {
        val array = JSONArray()
        events.map { it.toSnapshot() }.forEach { s ->
            array.put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("startsAt", s.startsAt)
                    .put("endsAt", s.endsAt ?: JSONObject.NULL)
                    .put("location", s.location ?: JSONObject.NULL)
            )
        }
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    /**
     * TEST ONLY: pretend the first saved event used to be somewhere else,
     * so the next refresh sees a "location changed" update.
     * Returns false if nothing has been saved yet (run a refresh first).
     */
    fun simulateLocationChangeForTesting(): Boolean {
        val json = prefs.getString(KEY_EVENTS, null) ?: return false
        val array = JSONArray(json)
        if (array.length() == 0) return false
        array.getJSONObject(0).put("location", "Old test location")
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply()
        return true
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key)

    private companion object {
        const val KEY_EVENTS = "events"
    }
}
