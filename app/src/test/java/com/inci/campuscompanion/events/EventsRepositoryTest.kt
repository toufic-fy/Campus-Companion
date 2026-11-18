package com.inci.campuscompanion.events

import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.InvalidEventDataException
import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class EventsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: EventsRepository
    private val valid = """{"id":"1","name":"Campus meetup","startingDate":"2026-10-20T12:23:40.729Z","endingDate":"2026-10-20T14:23:40.729Z","address":null,"description":"Meet students","avatar":"ignored"}"""

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/api/v1/events/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build().create(CampusApi::class.java)
        repository = EventsRepository(EventsRemoteDataSource(api))
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun mapsActualContractAndRequestsCorrectPath() = runTest {
        server.enqueue(MockResponse().setBody("[$valid]"))
        val event = repository.getEvents().single()
        assertEquals("Campus meetup", event.title)
        assertEquals("TBA", event.locationLabel)
        assertEquals("Meet students", event.description)
        assertTrue(event.timeLabel.contains("2026"))
        assertEquals("/api/v1/events/event", server.takeRequest().path)
    }

    @Test fun emptyResponseIsSuccessfulEmptyContent() = runTest {
        server.enqueue(MockResponse().setBody("[]"))
        assertTrue(repository.getEvents().isEmpty())
    }

    @Test fun httpFailureIsNotAnEmptyList() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        try { repository.getEvents(); fail("Expected HTTP failure") }
        catch (error: HttpException) { assertEquals(503, error.code()) }
    }

    @Test fun missingRequiredFieldIsInvalidData() = runTest {
        server.enqueue(MockResponse().setBody("[{\"id\":\"1\"}]"))
        try { repository.getEvents(); fail("Expected serialization failure") }
        catch (_: SerializationException) { }
    }

    @Test fun invalidDateAndDuplicateIdsAreRejected() = runTest {
        for (body in listOf("[${valid.replace("2026-10-20T12:23:40.729Z", "invalid")}]", "[$valid,$valid]")) {
            server.enqueue(MockResponse().setBody(body))
            try { repository.getEvents(); fail("Expected invalid data") }
            catch (_: InvalidEventDataException) { }
        }
    }
}
