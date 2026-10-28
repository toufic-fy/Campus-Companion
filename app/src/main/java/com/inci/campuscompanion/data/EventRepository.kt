
package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(
    private val api: CampusApi = CampusApi.instance
) {
    suspend fun getEvents(): List<CampusEvent> {
        return api.getEvents().map { dto ->
            dto.toCampusEvent()
        }
    }
}
