package com.inci.campuscompanion.data.repository

import androidx.room.withTransaction
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.local.entity.EventSyncState
import com.inci.campuscompanion.data.local.entity.SeenEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.inci.campuscompanion.data.local.entity.toEntity
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EventsRepository(
    private val remoteDataSource: EventsRemoteDataSource,
    private val database: AppDatabase
) {

    private val eventDao = database.eventDao()

    val hasConfirmedRefresh: Flow<Boolean> = eventDao.observeBaseline()

    val cachedEvents: Flow<List<CampusEvent>> = eventDao.getEvents()
        .map { entities -> entities.map { it.toDomain() } }

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

    // One shared lock covers manual/periodic workers and prevents duplicate change decisions.
    suspend fun refreshEvents(onNewEvents: suspend (List<CampusEvent>) -> Unit = {}): List<CampusEvent> = refreshMutex.withLock {
        val events = getEvents().distinctBy { it.id }
        val newEvents = database.withTransaction {
            val cachedIds = eventDao.cachedIds().toSet()
            val knownIds = eventDao.seenIds().toSet() + cachedIds
            val baselineExists = eventDao.hasBaseline() || cachedIds.isNotEmpty()
            val changes = if (baselineExists) events.filter { it.id !in knownIds } else emptyList()
            eventDao.replaceEvents(events.map { it.toEntity() })
            eventDao.rememberIds((knownIds + events.map { it.id }).map { SeenEvent(it) })
            eventDao.markSynced(EventSyncState())
            changes
        }
        // Baseline advances even when permission or the preference blocks delivery.
        if (newEvents.isNotEmpty()) onNewEvents(newEvents)
        events
    }

    companion object { private val refreshMutex = Mutex() }

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
