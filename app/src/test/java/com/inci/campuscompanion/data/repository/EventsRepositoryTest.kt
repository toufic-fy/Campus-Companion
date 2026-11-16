package com.inci.campuscompanion.data.repository

import com.inci.campuscompanion.data.remote.api.CampusApi
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.dto.EventDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

open class EventsRepositoryTest {

    private class FakeCampusApi(private val events: List<EventDto>) : CampusApi {
        override suspend fun getEvents(): List<EventDto> = events
    }

    @Test
    fun getEvents_returnsMappedCampusEvents() = runBlocking {
        val dtos = listOf(
            EventDto(
                id = "1",
                title = "Tech Symposium",
                description = "Annual technology gathering",
                startsAt = "2025-05-10T10:00:00Z",
                endsAt = "2025-05-10T12:00:00Z",
                location = "Auditorium A"
            )
        )
        val repository = EventsRepository(EventsRemoteDataSource(FakeCampusApi(dtos)))

        val result = repository.getEvents()

        assertEquals(1, result.size)
        val event = result[0]
        assertEquals("1", event.id)
        assertEquals("Tech Symposium", event.title)
        assertEquals("Annual technology gathering", event.description)
        assertEquals("2025-05-10T10:00:00Z", event.startsAt)
        assertEquals("2025-05-10T12:00:00Z", event.endsAt)
        assertEquals("Auditorium A", event.locationLabel)
        assertTrue(event.timeLabel.isNotBlank())
        assertTrue(event.timeLabel.contains("2025"))
    }

    @Test
    fun getEvents_formatsTimeLabel_withStartDateOnly() = runBlocking {
        val dtos = listOf(
            EventDto(
                id = "2",
                title = "Orientation",
                description = "Welcome event",
                startsAt = "2025-09-01T09:00:00Z",
                endsAt = null,
                location = "Main Hall"
            )
        )
        val repository = EventsRepository(EventsRemoteDataSource(FakeCampusApi(dtos)))

        val result = repository.getEvents()

        assertEquals(1, result.size)
        assertEquals("Main Hall", result[0].locationLabel)
        assertTrue(result[0].timeLabel.isNotBlank())
    }

    @Test
    fun getEvents_formatsTimeLabel_withUnparseableDate() = runBlocking {
        val dtos = listOf(
            EventDto(
                id = "3",
                title = "Custom Time Event",
                description = "Special schedule",
                startsAt = "TBD",
                endsAt = "TBD",
                location = "Room 101"
            )
        )
        val repository = EventsRepository(EventsRemoteDataSource(FakeCampusApi(dtos)))

        val result = repository.getEvents()

        assertEquals(1, result.size)
        assertEquals("TBD - TBD", result[0].timeLabel)
    }

    @Test
    fun getEvents_returnsEmptyList_whenNoEvents() = runBlocking {
        val repository = EventsRepository(EventsRemoteDataSource(FakeCampusApi(emptyList())))

        val result = repository.getEvents()

        assertTrue(result.isEmpty())
    }
}
