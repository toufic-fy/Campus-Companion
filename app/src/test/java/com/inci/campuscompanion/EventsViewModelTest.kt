package com.inci.campuscompanion

import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.CampusApi
import com.inci.campuscompanion.model.EventDto
import com.inci.campuscompanion.data.EventsRemoteDataSource
import com.inci.campuscompanion.ui.EventsUiState
import com.inci.campuscompanion.ui.EventsViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.google.gson.JsonSyntaxException
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val event = EventDto("1", "Campus meetup", "Today", "Main hall")

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialLoadAndRefreshShowLoadingThenContent() = runTest(dispatcher) {
        var calls = 0
        val viewModel = viewModelFor { calls++; listOf(event) }
        assertEquals(EventsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is EventsUiState.Content)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(2, calls)
    }

    @Test
    fun slowRefreshShowsLoadingUntilResponseArrives() = runTest(dispatcher) {
        var response = CompletableDeferred<List<EventDto>>()
        val viewModel = viewModelFor { response.await() }
        runCurrent()
        assertEquals(EventsUiState.Loading, viewModel.uiState.value)
        response.complete(listOf(event))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is EventsUiState.Content)

        response = CompletableDeferred()
        viewModel.refresh()
        runCurrent()
        assertEquals(EventsUiState.Loading, viewModel.uiState.value)
        response.complete(emptyList())
        advanceUntilIdle()
        assertEquals(EventsUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun emptyResponseShowsEmptyState() = runTest(dispatcher) {
        val viewModel = viewModelFor { emptyList() }
        advanceUntilIdle()
        assertEquals(EventsUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun disconnectedNetworkShowsErrorAndRetryRestoresContent() = runTest(dispatcher) {
        var connected = false
        val viewModel = viewModelFor {
            if (!connected) throw IOException("No network")
            listOf(event)
        }
        advanceUntilIdle()
        assertTrue(errorMessage(viewModel).contains("Network unavailable"))
        connected = true
        viewModel.refresh()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is EventsUiState.Content)
    }

    @Test
    fun timeoutShowsRetryableNetworkError() = runTest(dispatcher) {
        val viewModel = viewModelFor { throw SocketTimeoutException() }
        advanceUntilIdle()
        assertTrue(errorMessage(viewModel).contains("retry"))
    }

    @Test
    fun wrongEndpointShowsRequestErrorAndRestoringItAllowsRetry() = runTest(dispatcher) {
        var validEndpoint = false
        val viewModel = viewModelFor {
            if (!validEndpoint) throw httpFailure(404)
            listOf(event)
        }
        advanceUntilIdle()
        assertTrue(errorMessage(viewModel).contains("404"))
        validEndpoint = true
        viewModel.refresh()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is EventsUiState.Content)
    }

    @Test
    fun serverFailureShowsStatusAndRetry() = runTest(dispatcher) {
        val viewModel = viewModelFor { throw httpFailure(503) }
        advanceUntilIdle()
        assertEquals("Server error 503. Please retry.", errorMessage(viewModel))
    }

    @Test
    fun malformedDataShowsSafeError() = runTest(dispatcher) {
        val viewModel = viewModelFor { throw JsonSyntaxException("Broken payload") }
        advanceUntilIdle()
        assertEquals("Unable to load events. Please retry.", errorMessage(viewModel))
    }

    @Test
    fun cancellationIsNotPresentedAsNetworkFailure() = runTest(dispatcher) {
        val viewModel = viewModelFor { throw CancellationException() }
        advanceUntilIdle()
        assertEquals(EventsUiState.Loading, viewModel.uiState.value)
    }

    private fun errorMessage(viewModel: EventsViewModel) =
        (viewModel.uiState.value as EventsUiState.Error).message

    private fun httpFailure(code: Int) = HttpException(
        Response.error<List<EventDto>>(code, ResponseBody.create(null, "failure"))
    )

    private fun viewModelFor(load: suspend () -> List<EventDto>): EventsViewModel {
        val api = object : CampusApi {
            override suspend fun getEvents(): List<EventDto> = load()
        }
        return EventsViewModel(EventsRepository(EventsRemoteDataSource(api)))
    }
}
