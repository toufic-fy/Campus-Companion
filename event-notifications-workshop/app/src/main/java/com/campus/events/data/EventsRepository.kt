package com.campus.events.data

import com.campus.events.data.local.EventDao
import com.campus.events.data.local.toDomain
import com.campus.events.data.local.toEntity
import com.campus.events.data.remote.FakeEventsApi
import com.campus.events.data.remote.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Result of one refresh. The repository reports *what changed*; it does not decide whether
 * anybody should be notified. That is the notification policy's job.
 */
data class RefreshResult(
    val newEvents: List<Event>,
    /** True when the cache was empty: the first sync is a baseline, not "news". */
    val wasFirstSync: Boolean,
)

class EventsRepository(
    private val api: FakeEventsApi,
    private val dao: EventDao,
) {
    /** Room remains the single source of truth for the UI. */
    val events: Flow<List<Event>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun event(id: String): Flow<Event?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun anyCachedEvent(): Event? = dao.getFirst()?.toDomain()

    /**
     * 1. fetch and map remote DTOs  2. compare with the cache  3. write the cache.
     * Only events whose id was not cached before count as new, so refreshing unchanged data
     * never produces repeated notifications.
     */
    suspend fun refresh(): RefreshResult {
        val remote = api.fetchEvents().map { it.toDomain() }
        val cachedIds = dao.getAllIds().toSet()
        val firstSync = cachedIds.isEmpty()
        val newEvents = if (firstSync) emptyList() else remote.filter { it.id !in cachedIds }
        dao.replaceAll(remote.map { it.toEntity() })
        return RefreshResult(newEvents = newEvents, wasFirstSync = firstSync)
    }
}
