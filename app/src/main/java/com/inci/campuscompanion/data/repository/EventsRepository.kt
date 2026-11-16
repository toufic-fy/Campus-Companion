
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

    suspend fun refreshEvents() {
        val fresh = remote.getEvents().map { dto ->
            EventEntity(
                id = dto.id,
                title = dto.title,
                timeLabel = dto.startsAt,
                locationLabel = dto.location ?: "TBA"
            )
        }

        eventDao.replaceEvents(fresh)
    }
}
