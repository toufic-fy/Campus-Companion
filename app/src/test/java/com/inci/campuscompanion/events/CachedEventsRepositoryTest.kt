package com.inci.campuscompanion.events

import com.inci.campuscompanion.data.*
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CachedEventsRepositoryTest {
    private fun event(id: String) = CampusEvent(id, "Event $id", "Today", "Campus", "")
    private class Cache : EventsCache {
        override val events = MutableStateFlow<List<CampusEvent>>(emptyList())
        var ready = false
        val seen = mutableSetOf<String>()
        override suspend fun initialized() = ready
        override suspend fun seenIds() = seen.toSet()
        override suspend fun replace(events: List<CampusEvent>) {
            this.events.value = events
            seen.addAll(events.map { it.id })
            ready = true
        }
    }

    @Test fun firstRefreshIsSilentAndOnlyNewIdsNotifyEvenAfterRemovalAndRestart() = runTest {
        val cache = Cache()
        var remote = listOf(event("1"))
        val notices = mutableListOf<List<CampusEvent>>()
        val source = object : EventsSource { override suspend fun getEvents() = remote }
        val repository = CachedEventsRepository(source, cache) { notices.add(it) }
        repository.getEvents()
        assertTrue(notices.isEmpty())
        remote = listOf(event("1"), event("2"))
        repository.getEvents()
        repository.getEvents()
        assertEquals(listOf(listOf(event("2"))), notices)
        remote = emptyList()
        repository.getEvents()
        remote = listOf(event("2"))
        CachedEventsRepository(source, cache) { notices.add(it) }.getEvents()
        assertEquals(1, notices.size)
    }

    @Test fun emptyInitialRefreshIsARealBaseline() = runTest {
        val cache = Cache()
        var remote = emptyList<CampusEvent>()
        val notices = mutableListOf<List<CampusEvent>>()
        val repository = CachedEventsRepository(object : EventsSource {
            override suspend fun getEvents() = remote
        }, cache) { notices.add(it) }
        repository.getEvents()
        remote = listOf(event("1"))
        repository.getEvents()
        assertEquals(listOf(remote), notices)
    }

    @Test fun failedRefreshPreservesCacheAndDoesNotMarkUnseenIds() = runTest {
        val cache = Cache()
        cache.replace(listOf(event("1")))
        val repository = CachedEventsRepository(object : EventsSource {
            override suspend fun getEvents(): List<CampusEvent> = throw IOException("offline")
        }, cache)
        try { repository.getEvents(); fail("Expected failure") } catch (_: IOException) { }
        assertEquals(listOf(event("1")), cache.events.value)
        assertEquals(setOf("1"), cache.seen)
    }

    @Test fun concurrentRefreshesDoNotDuplicateNotification() = runTest {
        val cache = Cache()
        cache.replace(emptyList())
        var count = 0
        val repository = CachedEventsRepository(object : EventsSource {
            override suspend fun getEvents() = listOf(event("1"))
        }, cache) { count++ }
        val a = async { repository.getEvents() }
        val b = async { repository.getEvents() }
        a.await(); b.await()
        assertEquals(1, count)
    }

    @Test fun notificationFailureDoesNotUndoSuccessfulRefresh() = runTest {
        val cache = Cache()
        cache.replace(emptyList())
        val repository = CachedEventsRepository(object : EventsSource {
            override suspend fun getEvents() = listOf(event("1"))
        }, cache) { throw SecurityException("revoked") }
        assertEquals(listOf(event("1")), repository.getEvents())
        assertEquals(listOf(event("1")), cache.events.value)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun unchangedEmptyRefreshesStillEmitSuccessAfterTheCacheTransaction() = runTest {
        val cache = Cache()
        val repository = CachedEventsRepository(object : EventsSource {
            override suspend fun getEvents() = emptyList<CampusEvent>()
        }, cache)
        val results = async(UnconfinedTestDispatcher(testScheduler)) {
            repository.refreshCompletions.take(2).toList()
        }
        repository.getEvents()
        assertTrue(cache.ready)
        repository.getEvents()
        assertEquals(2, results.await().size)
    }

}
