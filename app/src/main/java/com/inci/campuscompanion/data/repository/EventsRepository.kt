
package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.EventDao
import com.inci.campuscompanion.data.EventEntity
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventsRepository(
    private val remote: EventsRemoteDataSource,
    private val eventDao: EventDao
) {

    fun observeEvents(): Flow<List<CampusEvent>> =
        eventDao.getAllEvents().map { rows ->
            rows.map { row ->
                CampusEvent(
                    id = row.id,
                    title = row.title,
                    timeLabel = row.timeLabel,
                    locationLabel = row.locationLabel
                )
            }
        }

    suspend fun refreshEvents(): List<CampusEvent> {

        // Read existing cached Events
        val oldEvents = eventDao.getEventsSnapshot()
        val oldIds = oldEvents.map { it.id }.toSet()

        // Download fresh Events
        val freshEvents = remote.getEvents().map { dto ->
            EventEntity(
                id = dto.id,
                title = dto.title,
                timeLabel = dto.startsAt,
                locationLabel = dto.location ?: "TBA"
            )
        }

        // Detect newly added Events
        val newEvents = freshEvents.filter { event ->
            event.id !in oldIds
        }

        // Update the Room cache
        eventDao.replaceEvents(freshEvents)

        // Return newly detected Events
        return newEvents.map { event ->
            CampusEvent(
                id = event.id,
                title = event.title,
                timeLabel = event.timeLabel,
                locationLabel = event.locationLabel
            )
        }
    }
}
