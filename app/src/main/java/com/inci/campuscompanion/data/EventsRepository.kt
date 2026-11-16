package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.model.EventsFeed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EventsRepository(
    private val remote: EventsRemoteDataSource,
    private val cache: EventsDao
) {
    private val refreshLock = Mutex()

    val events = cache.observeEvents().map { rows ->
        rows.map { CampusEvent(it.id, it.title, it.timeLabel, it.locationLabel) }
    }

    val feed = combine(events, cache.observePagination()) { events, pagination ->
        EventsFeed(events, pagination?.nextPage != null)
    }

    suspend fun hasCachedFeed(): Boolean = cache.getPagination() != null

    suspend fun getEvents(page: Int = 1): List<CampusEvent> {
        require(page > 0)
        val result = remote.getEvents(page, PAGE_SIZE)
        require(result.size <= PAGE_SIZE) { "Invalid page size" }
        val events = result.map { dto ->
            require(dto.id.isNotBlank() && dto.name.isNotBlank() && dto.startingDate.isNotBlank()) {
                "Invalid event data"
            }
            CampusEvent(
                id = dto.id,
                title = dto.name,
                timeLabel = dto.startingDate,
                locationLabel = dto.address ?: "TBA"
            )
        }
        require(events.map { it.id }.distinct().size == events.size) { "Duplicate event IDs" }
        return events
    }

    /**
     * Replaces the cached first page and returns the events that were not cached before.
     * The very first load returns an empty list, so a fresh install is not reported as "new".
     */
    suspend fun refreshEvents(): List<CampusEvent> = refreshLock.withLock {
        val previousIds = if (cache.getPagination() != null) cache.getEventIds().toSet() else null
        val events = getEvents(1)
        cache.replaceEvents(
            events.mapIndexed { index, event -> event.cachedAt(index) },
            nextPage = if (events.size == PAGE_SIZE) 2 else null
        )
        if (previousIds == null) emptyList() else events.filterNot { it.id in previousIds }
    }

    suspend fun loadNextPage() = refreshLock.withLock {
        val pagination = cache.getPagination() ?: return@withLock
        val page = pagination.nextPage ?: return@withLock
        val events = getEvents(page)
        cache.appendPage(
            events.mapIndexed { index, event -> event.cachedAt((page - 1) * PAGE_SIZE + index) },
            expected = pagination,
            hasAnotherPage = events.size == PAGE_SIZE
        )
    }

    private fun CampusEvent.cachedAt(position: Int) =
        CachedEvent(id, title, timeLabel, locationLabel, position)

    companion object {
        const val PAGE_SIZE = 10
    }
}
