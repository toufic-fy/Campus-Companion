package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.model.CampusEvent
import kotlin.coroutines.cancellation.CancellationException

fun EventDto.toDomain(): CampusEvent {
    return CampusEvent(
        id = this.id ?: "",
        title = this.name ?: "Untitled Event",
        startDate = this.startingDate ?: "",
        endDate = this.endingDate ?: "",
        location = this.address ?: "Location TBD",
        description = this.description ?: ""
    )
}

class EventsRepository(private val api: CampusApi) {
    suspend fun loadEvents(): Result<List<CampusEvent>> {
        return runCatching {
            api.getEvents().map { it.toDomain() }
        }.onFailure { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }
        }
    }
}
