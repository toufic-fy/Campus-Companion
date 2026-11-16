
package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EventsRepository(
    private val remoteDataSource: EventsRemoteDataSource,
    private val preferencesRepository: UserPreferencesRepository
) {

    suspend fun getEvents(): List<CampusEvent> =
        remoteDataSource.getEvents().map { dto ->
            CampusEvent(
                id = dto.id,
                title = dto.title,
                description = dto.description,
                startsAt = dto.startsAt,
                endsAt = dto.endsAt,
                timeLabel = formatTimeLabel(dto.startsAt, dto.endsAt),
                locationLabel = dto.location
            )
        }

    suspend fun refreshEvents(): List<CampusEvent> = getEvents()

    suspend fun refreshEventsAndGetChanges(): List<CampusEvent> {
        val latestEvents = getEvents()
        val previousSnapshot = preferencesRepository.getEventSnapshot()

        // First refresh: save the baseline without reporting changes.
        if (previousSnapshot == null) {
            preferencesRepository.saveEventSnapshot(serializeEvents(latestEvents))
            return emptyList()
        }

        val previousEvents = deserializeEvents(previousSnapshot)
        val previousById = previousEvents.associateBy { it.id }

        val changedEvents = latestEvents.filter { event ->
            val previous = previousById[event.id]

            previous == null ||
                    previous.title != event.title ||
                    previous.description != event.description ||
                    previous.startsAt != event.startsAt ||
                    previous.endsAt != event.endsAt ||
                    previous.locationLabel != event.locationLabel
        }

        // Save the latest snapshot for the next refresh.
        preferencesRepository.saveEventSnapshot(serializeEvents(latestEvents))

        return changedEvents
    }

    private fun serializeEvents(events: List<CampusEvent>): String {
        val array = JSONArray()

        events.forEach { event ->
            val json = JSONObject().apply {
                put("id", event.id)
                put("title", event.title)
                put("description", event.description)
                put("startsAt", event.startsAt)
                put("endsAt", event.endsAt ?: JSONObject.NULL)
                put("locationLabel", event.locationLabel ?: JSONObject.NULL)
            }

            array.put(json)
        }

        return array.toString()
    }

    private fun deserializeEvents(snapshot: String): List<CampusEvent> {
        val array = JSONArray(snapshot)

        return List(array.length()) { index ->
            val json = array.getJSONObject(index)

            val startsAt = json.getString("startsAt")
            val endsAt = json.optString("endsAt")
                .takeUnless { it == "null" || it.isBlank() }

            CampusEvent(
                id = json.getString("id"),
                title = json.getString("title"),
                description = json.getString("description"),
                startsAt = startsAt,
                endsAt = endsAt,
                timeLabel = formatTimeLabel(startsAt, endsAt),
                locationLabel = json.optString("locationLabel")
                    .takeUnless { it == "null" || it.isBlank() }
            )
        }
    }

    private fun formatTimeLabel(startsAt: String, endsAt: String?): String {
        val startDate = parseIsoDate(startsAt)
        val endDate = endsAt?.let { parseIsoDate(it) }

        if (startDate == null) {
            return if (endsAt.isNullOrBlank()) startsAt else "$startsAt - $endsAt"
        }

        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        val dateStr = dateFormat.format(startDate)
        val startTimeStr = timeFormat.format(startDate)

        return if (endDate != null) {
            val endTimeStr = timeFormat.format(endDate)
            "$dateStr • $startTimeStr - $endTimeStr"
        } else {
            "$dateStr • $startTimeStr"
        }
    }

    private fun parseIsoDate(dateString: String): Date? {
        if (dateString.isBlank()) return null

        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )

        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.getDefault())

                if (pattern.endsWith("'Z'")) {
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                }

                return parser.parse(dateString)
            } catch (_: Exception) {
                // Try the next pattern.
            }
        }

        return null
    }
}
