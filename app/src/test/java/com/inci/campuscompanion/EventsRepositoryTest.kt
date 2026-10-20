package com.inci.campuscompanion

import com.inci.campuscompanion.data.CampusApi
import com.inci.campuscompanion.data.EventDto
import com.inci.campuscompanion.data.EventsRemoteDataSource
import com.inci.campuscompanion.data.EventsRepository
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventsRepositoryTest {
    private fun repository(load: suspend () -> List<EventDto>) = EventsRepository(
        EventsRemoteDataSource(object : CampusApi {
            override suspend fun getEvents() = load()
        })
    )

    @Test fun decodesActualApiFieldNamesAndIgnoresUnusedFields() {
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<EventDto>(
            """{"id":"1","name":"Meetup","startingDate":"2026-10-20","address":"Campus","avatar":"unused"}"""
        )
        assertEquals("Meetup", dto.name)
        assertEquals("Campus", dto.address)
    }

    @Test(expected = SerializationException::class)
    fun rejectsMissingRequiredApiFields() {
        Json.decodeFromString<EventDto>("""{"id":"1"}""")
    }

    @Test fun mapsApiFieldsAndMissingLocation() = runBlocking {
        val event = repository {
            listOf(EventDto("1", "Campus meetup", "2026-10-20T12:00:00Z"))
        }.getEvents().single()
        assertEquals("1", event.id)
        assertEquals("Campus meetup", event.title)
        assertEquals("2026-10-20T12:00:00Z", event.timeLabel)
        assertEquals("TBA", event.locationLabel)
    }

    @Test fun preservesEmptyResponse() = runBlocking {
        assertTrue(repository { emptyList() }.getEvents().isEmpty())
    }

    @Test fun connectionFailureIsNotEmptyContent() = runBlocking {
        try {
            repository { throw IOException("Offline") }.getEvents()
            throw AssertionError("Expected connection failure")
        } catch (expected: IOException) {
            assertEquals("Offline", expected.message)
        }
    }

    @Test fun rejectsInvalidEventData() = runBlocking {
        try {
            repository { listOf(EventDto("1", "", "date")) }.getEvents()
            throw AssertionError("Expected invalid data failure")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Invalid event data", expected.message)
        }
    }
}
