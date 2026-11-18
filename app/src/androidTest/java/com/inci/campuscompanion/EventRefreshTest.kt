package com.inci.campuscompanion

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.remote.api.CampusApi
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.dto.EventDto
import com.inci.campuscompanion.data.repository.EventsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class EventRefreshTest {
    private class FakeApi : CampusApi {
        var events = listOf<EventDto>()
        var fail = false
        override suspend fun getEvents(): List<EventDto> {
            if (fail) throw IOException("Simulated disconnected network")
            return events
        }
    }
    private fun event(id: String) = EventDto(id, "Campus event $id", "Description", "2026-11-20T12:00:00Z")

    @Test fun baselineUnchangedNewAndReappearingEvents() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val api = FakeApi()
            val repo = EventsRepository(EventsRemoteDataSource(api), db)
            var notifications = 0
            api.events = listOf(event("1"))
            repo.refreshEvents { notifications++ }
            assertEquals(0, notifications)
            repo.refreshEvents { notifications++ }
            assertEquals(0, notifications)
            api.events = listOf(event("1"), event("2"), event("3"))
            repo.refreshEvents { assertEquals(listOf("2", "3"), it.map { e -> e.id }); notifications++ }
            assertEquals(1, notifications)
            assertEquals(3, repo.cachedEvents.first().size)
            api.events = emptyList()
            repo.refreshEvents { notifications++ }
            assertTrue(repo.cachedEvents.first().isEmpty())
            api.events = listOf(event("2"))
            repo.refreshEvents { notifications++ }
            assertEquals(1, notifications)
        } finally { db.close() }
    }

    @Test fun emptyFirstRefreshIsAConfirmedBaselineAndConcurrentRefreshesNotifyOnce() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val api = FakeApi()
            val repo = EventsRepository(EventsRemoteDataSource(api), db)
            repo.refreshEvents()
            api.events = listOf(event("1"))
            var notifications = 0
            coroutineScope { (1..5).map { async { EventsRepository(EventsRemoteDataSource(api), db).refreshEvents { notifications++ } } }.awaitAll() }
            assertEquals(1, notifications)
        } finally { db.close() }
    }

    @Test fun failedRefreshPreservesCacheAndDoesNotAdvanceBaseline() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val api = FakeApi()
            val repo = EventsRepository(EventsRemoteDataSource(api), db)
            api.events = listOf(event("1"))
            repo.refreshEvents()
            api.fail = true
            try { repo.refreshEvents(); fail("Expected network error") } catch (_: IOException) { }
            assertEquals(listOf("1"), repo.cachedEvents.first().map { it.id })
            api.fail = false
            api.events = listOf(event("1"), event("2"))
            var newIds = emptyList<String>()
            repo.refreshEvents { newIds = it.map { event -> event.id } }
            assertEquals(listOf("2"), newIds)
        } finally { db.close() }
    }
}
