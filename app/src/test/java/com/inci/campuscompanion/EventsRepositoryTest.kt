package com.inci.campuscompanion

import com.inci.campuscompanion.data.EventsNetwork
import com.inci.campuscompanion.data.EventsRemoteDataSource
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.InvalidEventDataException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class EventsRepositoryTest {
    private val validJson = """[
        {"id":"2","name":"Second","startingDate":"2026-11-02T12:00:00Z","address":"Hall B"},
        {"id":"1","name":"First","startingDate":"2026-11-01T12:00:00Z","endingDate":"2026-11-01T15:00:00Z","description":"Talk"}
    ]"""

    @Test
    fun mapsAndSortsEventsFromTheExpectedEndpoint() = runBlocking {
        withServer(validJson) { server, repository ->
            val events = repository.getEvents()
            assertEquals(listOf("1", "2"), events.map { it.id })
            assertEquals("First", events.first().title)
            assertEquals("Location to be announced", events.first().address)
            assertEquals("No description available.", events.last().description)
            assertEquals("/api/v1/events/event", server.takeRequest().path)
        }
    }

    @Test
    fun anEmptyResponseIsDifferentFromAnError() = runBlocking {
        withServer("[]") { _, repository -> assertTrue(repository.getEvents().isEmpty()) }
    }

    @Test
    fun malformedDatesAreRejected() = runBlocking {
        withServer(validJson.replace("2026-11-01T12:00:00Z", "not-a-date")) { _, repository ->
            try {
                repository.getEvents()
                throw AssertionError("Expected invalid event data")
            } catch (_: InvalidEventDataException) {
                // Expected: malformed service data is surfaced as a repository failure.
            }
        }
    }

    @Test
    fun serverErrorsRemainRetryableHttpFailures() = runBlocking {
        withServer("{}", status = 503) { _, repository ->
            try {
                repository.getEvents()
                throw AssertionError("Expected an HTTP failure")
            } catch (error: HttpException) {
                assertEquals(503, error.code())
            }
        }
    }

    private suspend fun withServer(
        body: String,
        status: Int = 200,
        block: suspend (MockWebServer, EventsRepository) -> Unit
    ) {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(status)
                    .setHeader("Content-Type", "application/json")
                    .setBody(body)
            )
            val repository = EventsRepository(
                EventsRemoteDataSource(
                    EventsNetwork.createApi(server.url("/api/v1/events/").toString())
                )
            )
            block(server, repository)
        } finally {
            server.shutdown()
        }
    }
}
