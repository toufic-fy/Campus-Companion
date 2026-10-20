package com.inci.campuscompanion.data.remote

/**
 * Network data source: calls the Retrofit service.
 */
class EventsRemoteDataSource(
    private val api: CampusApi
) {
    suspend fun getEvents(page: Int, limit: Int): List<EventDto> =
        api.getEvents(page = page, limit = limit)
}
