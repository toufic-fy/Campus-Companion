package com.inci.campuscompanion

import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.remote.*
import com.inci.campuscompanion.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventsFeedTest {
    private val event = """[{"id":"1","name":"Campus meetup","startingDate":"2026-10-20T12:23:40.729Z","address":null,"extra":"ignored"}]"""
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = MockWebServer()
        server.start()
    }

    @After fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    private fun viewModel(baseUrl: String = server.url("/").toString()) =
        EventsViewModel(EventsRepository(EventsRemoteDataSource(CampusApiFactory.create(baseUrl))))

    private suspend fun result(vm: EventsViewModel): EventsUiState = withTimeout(5_000) {
        vm.state.first { it !is EventsUiState.Loading }
    }

    @Test fun mapsActualJsonContractAndMissingLocation() = runBlocking {
        server.enqueue(MockResponse().setBody(event))
        val state = result(viewModel()) as EventsUiState.Content
        assertEquals("Campus meetup", state.events.single().title)
        assertEquals("Location TBA", state.events.single().locationLabel)
        assertTrue(state.events.single().timeLabel.contains("2026"))
        assertEquals("/event", server.takeRequest().path)
        println("MAPPING: JSON name -> title; startingDate -> timeLabel; null address -> Location TBA")
    }

    @Test fun emptyJsonArrayProducesEmptyState() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))
        assertEquals(EventsUiState.Empty, result(viewModel()))
        println("EMPTY RESPONSE: HTTP 200 [] -> Empty (not Error)")
    }

    @Test fun unavailableNetworkSocketProducesError() = runBlocking {
        val url = server.url("/").toString()
        server.shutdown()
        val state = result(viewModel(url)) as EventsUiState.Error
        assertTrue(state.message.contains("Check your connection"))
        println("UNAVAILABLE SOCKET: stopped local server -> Error: ${state.message}")
    }

    @Test fun wrongBaseUrlThenRestoredEndpointReturnsContentOnRetry() = runBlocking {
        var baseUrl = server.url("/wrong/").toString()
        val api = object : CampusApi {
            override suspend fun getEvents() = CampusApiFactory.create(baseUrl).getEvents()
        }
        server.enqueue(MockResponse().setResponseCode(404))
        val vm = EventsViewModel(EventsRepository(EventsRemoteDataSource(api)))
        val error = result(vm) as EventsUiState.Error
        assertTrue(error.message.contains("404"))
        assertEquals("/wrong/event", server.takeRequest().path)
        println("BAD BASE URL: /wrong/event -> HTTP 404 -> Error: ${error.message}")
        baseUrl = server.url("/").toString()
        server.enqueue(MockResponse().setBody(event))
        vm.refresh()
        assertTrue(result(vm) is EventsUiState.Content)
        assertEquals("/event", server.takeRequest().path)
        println("RESTORED ENDPOINT: Retry callback -> /event -> Content")
    }

    @Test fun malformedJsonProducesSafeError() = runBlocking {
        server.enqueue(MockResponse().setBody("[{invalid JSON]"))
        val state = result(viewModel()) as EventsUiState.Error
        assertTrue(state.message.contains("invalid event data"))
        println("MALFORMED JSON: Error: ${state.message}")
    }

    @Test fun invalidDateProducesSafeError() = runBlocking {
        server.enqueue(MockResponse().setBody(event.replace("2026-10-20T12:23:40.729Z", "not-a-date")))
        val state = result(viewModel()) as EventsUiState.Error
        assertTrue(state.message.contains("invalid event data"))
    }

    @Test fun serverFailureProducesRetryableError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        val state = result(viewModel()) as EventsUiState.Error
        assertTrue(state.message.contains("try again later"))
    }
}
