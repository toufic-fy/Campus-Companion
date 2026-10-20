package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.CancellationException

class EventsRepository(private val api: CampusApi) {

    suspend fun getEvents(): Result<List<CampusEvent>> =
        try {
            Result.success(api.getEvents().mapNotNull { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}

fun EventDto.toDomain(): CampusEvent? {
    val id = id ?: return null
    val title = title?.takeIf { it.isNotBlank() } ?: return null
    return CampusEvent(
        id = id,
        title = title,
        location = location ?: "Location TBA",
        startsAt = startsAt ?: "Time TBA",
        description = description.orEmpty()
    )
}
