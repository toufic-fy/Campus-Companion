package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CachedEventsSource : EventsSource {
    val cachedEvents: Flow<List<CampusEvent>>
}

class CachedEventsRepository(
    private val remote: EventsSource,
    private val cache: EventsCache,
    private val onNewEvents: suspend (List<CampusEvent>) -> Unit = {}
) : CachedEventsSource {
    override val cachedEvents = cache.events
    private val refreshMutex = Mutex()

    override suspend fun getEvents(): List<CampusEvent> = refreshMutex.withLock {
        val events = remote.getEvents() // A failed request never clears the cache.
        val initialized = cache.initialized()
        val seenIds = cache.seenIds()
        val newEvents = if (initialized) events.filter { it.id !in seenIds } else emptyList()
        cache.replace(events)
        if (newEvents.isNotEmpty()) {
            try {
                onNewEvents(newEvents)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Notification delivery must not turn a successful data refresh into a failure.
            }
        }
        events
    }
}
