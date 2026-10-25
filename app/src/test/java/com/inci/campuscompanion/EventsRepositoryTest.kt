package com.inci.campuscompanion

import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class EventsRepositoryTest {

    private val fakeApi = object : CampusApi {
        override suspend fun getEvents(): List<EventDto> = listOf(
            EventDto(id = "e1", name = "Robotics Demo", startingDate = "2026-10-22 14:00", address = "Hall B"),
            EventDto(id = "e2", name = "Career Fair", startingDate = "2026-10-25 10:00", address = null)
        )
    }

    private val repository = EventsRepository(EventsRemoteDataSource(fakeApi))

    @Test
    fun mapsFieldsCorrectly() = runBlocking {
        val events = repository.getEvents()

        assertEquals(2, events.size)
        assertEquals("Robotics Demo", events[0].title)
        assertEquals("2026-10-22 14:00", events[0].timeLabel)
        assertEquals("Hall B", events[0].locationLabel)
    }

    @Test
    fun missingLocationBecomesTBA() = runBlocking {
        val events = repository.getEvents()

        assertEquals("TBA", events[1].locationLabel)
    }
}