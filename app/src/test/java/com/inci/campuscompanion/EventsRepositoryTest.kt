package com.inci.campuscompanion

import com.inci.campuscompanion.data.local.dao.EventDao
import com.inci.campuscompanion.data.local.entity.EventEntity
import com.inci.campuscompanion.data.local.entity.EventsRefreshState
import com.inci.campuscompanion.data.remote.api.CampusApi
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.dto.EventDto
import com.inci.campuscompanion.data.repository.EventsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class EventsRepositoryTest {
    private class FakeApi : CampusApi {
        var events = emptyList<EventDto>()
        var fail = false
        override suspend fun getEvents(): List<EventDto> {
            if (fail) throw IOException("Offline")
            return events
        }
    }

    private class FakeEventDao : EventDao() {
        val rows = MutableStateFlow(emptyList<EventEntity>())
        var refreshed = false
        override fun observeEvents() = rows
        override suspend fun getEvents() = rows.value
        override suspend fun hasRefreshed() = refreshed
        override suspend fun clearEvents() { rows.value = emptyList() }
        override suspend fun insertEvents(events: List<EventEntity>) { rows.value = events }
        override suspend fun markRefreshed(state: EventsRefreshState) { refreshed = true }
    }

    private fun event(id: String, title: String = id) =
        EventDto(id, title, "Description", "2026-11-20T10:00:00Z")

    @Test fun baselineIsSilentAndRepeatsRemainSilentAfterRepositoryRecreation() = runBlocking {
        val api = FakeApi().apply { events = listOf(event("a")) }
        val dao = FakeEventDao()
        val repository = EventsRepository(EventsRemoteDataSource(api), dao)
        assertTrue(repository.refreshEvents().isEmpty())
        assertEquals(listOf("a"), dao.getEvents().map { it.id })
        val recreated = EventsRepository(EventsRemoteDataSource(api), dao)
        assertTrue(recreated.refreshEvents().isEmpty())
    }

    @Test fun onlyOneConcurrentRefreshReturnsTheNewEvent() = runBlocking {
        val api = FakeApi()
        val dao = FakeEventDao()
        val repository = EventsRepository(EventsRemoteDataSource(api), dao)
        repository.refreshEvents() // A confirmed empty response is a valid baseline.
        api.events = listOf(event("a"))
        val first = async { repository.refreshEvents() }
        val second = async { repository.refreshEvents() }
        assertEquals(listOf("a"), (first.await() + second.await()).map { it.id })
    }

    @Test fun changedTitleUpdatesCacheWithoutBeingTreatedAsANewEvent() = runBlocking {
        val api = FakeApi().apply { events = listOf(event("a")) }
        val dao = FakeEventDao()
        val repository = EventsRepository(EventsRemoteDataSource(api), dao)
        repository.refreshEvents()
        api.events = listOf(event("a", "Updated title"), event("b"))
        assertEquals(listOf("b"), repository.refreshEvents().map { it.id })
        assertEquals("Updated title", dao.getEvents().first().title)
        assertTrue(repository.refreshEvents().isEmpty())
    }

    @Test fun failedNetworkRequestPreservesCacheAndBaseline() = runBlocking {
        val api = FakeApi().apply { events = listOf(event("a")) }
        val dao = FakeEventDao()
        val repository = EventsRepository(EventsRemoteDataSource(api), dao)
        repository.refreshEvents()
        api.fail = true
        try {
            repository.refreshEvents()
            fail("Expected an offline error")
        } catch (_: IOException) { }
        assertEquals(listOf("a"), dao.getEvents().map { it.id })
        assertTrue(dao.hasRefreshed())
    }
}
