
package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.local.dao.EventDao
import com.inci.campuscompanion.data.local.entity.toEntity
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EventsRepository(
    private val remoteDataSource: EventsRemoteDataSource,
    private val eventDao: EventDao
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

    suspend fun refreshEvents(): List<CampusEvent> =
        getEvents()

    // Refresh events, compare with Room, and return meaningful changes.
    suspend fun refreshEventsAndGetChanges(): List<CampusEvent> {

        val oldEvents = eventDao.getAllEvents()
        val newEvents = getEvents()

        // First synchronization: save events without notifying
        // about all existing events.
        if (oldEvents.isEmpty()) {
            eventDao.upsertEvents(newEvents.map { it.toEntity() })
            return emptyList()
        }

        val oldEventsById = oldEvents.associateBy { it.id }

        val changedEvents = newEvents.filter { newEvent ->

            val oldEvent = oldEventsById[newEvent.id]

            oldEvent == null ||
                    oldEvent.title != newEvent.title ||
                    oldEvent.description != newEvent.description ||
                    oldEvent.startsAt != newEvent.startsAt ||
                    oldEvent.endsAt != newEvent.endsAt ||
                    oldEvent.locationLabel != newEvent.locationLabel
        }

        // Save the latest events after comparison.
        eventDao.upsertEvents(newEvents.map { it.toEntity() })

        return changedEvents
    }

    private fun formatTimeLabel(
        startsAt: String,
        endsAt: String?
    ): String {

        val startDate = parseIsoDate(startsAt)
        val endDate = endsAt?.let { parseIsoDate(it) }

        if (startDate == null) {
            return if (endsAt.isNullOrBlank()) {
                startsAt
            } else {
                "$startsAt - $endsAt"
            }
        }

        val dateFormat = SimpleDateFormat(
            "MMM d, yyyy",
            Locale.getDefault()
        )

        val timeFormat = SimpleDateFormat(
            "h:mm a",
            Locale.getDefault()
        )

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
                val parser = SimpleDateFormat(
                    pattern,
                    Locale.getDefault()
                )

                if (pattern.endsWith("'Z'")) {
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                }

                return parser.parse(dateString)

            } catch (_: Exception) {
                // Try next pattern
            }
        }

        return null
    }
}
