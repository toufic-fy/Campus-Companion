package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.data.local.dao.EventDao
import com.inci.campuscompanion.data.local.entity.EventEntity
import com.inci.campuscompanion.notifications.newlyPublishedEvents
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EventsRepository(
    private val remoteDataSource: EventsRemoteDataSource,
    private val eventDao: EventDao,
) {
    private val refreshMutex = Mutex()

    val events = eventDao.observeEvents().map { entities -> entities.map { it.toDomain() } }

    suspend fun getCachedEvents(): List<CampusEvent> = eventDao.getEvents()
        .sortedWith(compareBy<EventEntity> { it.startsAt }.thenBy { it.id })
        .map { it.toDomain() }

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

    suspend fun refreshEvents(): List<CampusEvent> = refreshMutex.withLock {
        val refreshed = getEvents().distinctBy { it.id }
        val newEvents = newlyPublishedEvents(
            previousIds = eventDao.getEvents().map { it.id }.toSet(),
            refreshedEvents = refreshed,
            hasRefreshed = eventDao.hasRefreshed(),
            id = { it.id },
        )
        // Commit the snapshot first: retries/restarts do not notify for the same data.
        eventDao.replaceEvents(refreshed.map {
            EventEntity(it.id, it.title, it.description, it.startsAt, it.endsAt, it.locationLabel)
        })
        newEvents
    }

    private fun EventEntity.toDomain() = CampusEvent(
        id, title, description, startsAt, endsAt,
        formatTimeLabel(startsAt, endsAt), location,
    )

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
                // Try next pattern
            }
        }
        return null
    }
}
