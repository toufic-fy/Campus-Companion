package com.inci.campuscompanion

import com.inci.campuscompanion.data.EventsNetwork
import com.inci.campuscompanion.data.EventsRemoteDataSource
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.ui.EventsUiState
import com.inci.campuscompanion.ui.EventsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Slide 14 failure exercise: drives the real Retrofit -> repository -> ViewModel stack
 * against a local server whose behaviour can be switched between scenarios.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventsFailureExerciseTest {
    private enum class Mode { VALID, OFFLINE, EMPTY }

    @Volatile
    private var mode = Mode.VALID
    private val server = MockWebServer()

    private val validJson = """[
        {"id":"1","name":"Career Fair","startingDate":"2026-11-01T09:00:00Z","address":"Main Hall"},
        {"id":"2","name":"Robotics Club","startingDate":"2026-11-03T16:00:00Z"}
    ]"""

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                log("server", "${request.method} ${request.path} (mode=$mode)")
                if (mode == Mode.OFFLINE) {
                    return MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                }
                if (request.path != "/api/v1/events/event") {
                    return MockResponse().setResponseCode(404).setBody("Not found")
                }
                return MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(if (mode == Mode.EMPTY) "[]" else validJson)
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun scenario1_disconnectedNetworkShowsErrorInsteadOfCrashing() = runBlocking {
        mode = Mode.OFFLINE
        val state = settle(viewModelFor(validBaseUrl()), "scenario 1")
        assertTrue(state is EventsUiState.Error)
        assertEquals(
            "Could not connect. Check your internet connection and retry.",
            (state as EventsUiState.Error).message
        )
    }

    @Test
    fun scenario2_wrongBaseUrlShowsRetryableFailure() = runBlocking {
        val wrongPath = settle(viewModelFor(server.url("/api/v1/wrong/").toString()), "scenario 2a")
        assertTrue(wrongPath is EventsUiState.Error)

        val unknownHost = settle(viewModelFor("https://campus-events.invalid/api/v1/events/"), "scenario 2b")
        assertTrue(unknownHost is EventsUiState.Error)
    }

    @Test
    fun scenario3_emptyArrayShowsEmptyMessage() = runBlocking {
        mode = Mode.EMPTY
        val state = settle(viewModelFor(validBaseUrl()), "scenario 3")
        assertEquals(EventsUiState.Empty, state)
    }

    @Test
    fun scenario4_restoringEndpointAndRetryingReturnsContent() = runBlocking {
        mode = Mode.OFFLINE
        val viewModel = viewModelFor(validBaseUrl())
        assertTrue(settle(viewModel, "scenario 4 (before restore)") is EventsUiState.Error)

        mode = Mode.VALID
        log("scenario 4", "endpoint restored, pressing Retry")
        viewModel.refresh()
        val state = settle(viewModel, "scenario 4 (after Retry)")
        assertTrue(state is EventsUiState.Content)
        assertEquals(listOf("Career Fair", "Robotics Club"), (state as EventsUiState.Content).events.map { it.title })
    }

    private fun validBaseUrl() = server.url("/api/v1/events/").toString()

    private fun viewModelFor(baseUrl: String): EventsViewModel {
        log("setup", "base URL = $baseUrl")
        return EventsViewModel(EventsRepository(EventsRemoteDataSource(EventsNetwork.createApi(baseUrl))))
    }

    private suspend fun settle(viewModel: EventsViewModel, label: String): EventsUiState {
        val state = withTimeout(30_000) { viewModel.state.first { it !is EventsUiState.Loading } }
        log(label, "Loading -> $state")
        return state
    }

    private fun log(tag: String, message: String) = println("[EventsFailureExercise] $tag: $message")
}
