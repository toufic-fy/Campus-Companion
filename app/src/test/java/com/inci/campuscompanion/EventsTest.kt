package com.inci.campuscompanion

import com.inci.campuscompanion.data.*
import com.inci.campuscompanion.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

@OptIn(ExperimentalCoroutinesApi::class)
class EventsTest {
    private val eventJson = """[{"id":"1","name":"Campus talk","startingDate":"2026-10-26T09:00:00Z","address":null,"extra":"ignored"}]"""

    @Test fun actualWireNamesMapAndUnknownFieldsAreIgnored() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody(eventJson))
            val result = EventsRepository(EventsRemoteDataSource(CampusApi.create(server.url("/api/v1/events/").toString()))).getEvents()
            assertEquals("Campus talk", result.single().title)
            assertEquals("TBA", result.single().locationLabel)
            assertEquals("/api/v1/events/event", server.takeRequest().path)
        } finally { server.shutdown() }
    }

    @Test fun emptyJsonIsAnEmptyList() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("[]"))
            assertTrue(EventsRepository(EventsRemoteDataSource(CampusApi.create(server.url("/").toString()))).getEvents().isEmpty())
        } finally { server.shutdown() }
    }

    @Test fun missingRequiredFieldIsRejected() = runBlocking {
        val api = object : CampusApi { override suspend fun getEvents() = listOf(EventDto(id = "1")) }
        try {
            EventsRepository(EventsRemoteDataSource(api)).getEvents()
            fail("Expected invalid data to fail")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun httpFailureIsNotEmptyContent() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(404))
            try {
                CampusApi.create(server.url("/").toString()).getEvents()
                fail("Expected HTTP error")
            } catch (e: HttpException) { assertTrue(EventsViewModel.errorMessage(e).contains("404")) }
        } finally { server.shutdown() }
    }

    @Test fun malformedJsonFailsParsing() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("not-json"))
            try {
                CampusApi.create(server.url("/").toString()).getEvents()
                fail("Expected malformed JSON to fail")
            } catch (e: com.google.gson.stream.MalformedJsonException) {
                assertTrue(EventsViewModel.errorMessage(e).contains("invalid event data"))
            }
        } finally { server.shutdown() }
    }

    @Test fun serverErrorInvitesRetryLater() {
        val error = HttpException(retrofit2.Response.error<Any>(503,
            okhttp3.ResponseBody.create(null, "unavailable")))
        assertTrue(EventsViewModel.errorMessage(error).contains("retry later"))
    }

    @Test fun loadingErrorRetryContentAndEmpty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var mode = 0
            val api = object : CampusApi {
                override suspend fun getEvents(): List<EventDto> = when (mode) {
                    0 -> throw java.io.IOException("offline")
                    1 -> listOf(EventDto("1", "Talk", "Tomorrow"))
                    else -> emptyList()
                }
            }
            val vm = EventsViewModel(EventsRepository(EventsRemoteDataSource(api)))
            assertEquals(EventsUiState.Loading, vm.state.value)
            advanceUntilIdle()
            assertTrue(vm.state.value is EventsUiState.Error)
            mode = 1; vm.refresh()
            assertEquals(EventsUiState.Loading, vm.state.value)
            advanceUntilIdle()
            assertTrue(vm.state.value is EventsUiState.Content)
            mode = 2; vm.refresh(); advanceUntilIdle()
            assertEquals(EventsUiState.Empty, vm.state.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun cancellationIsNotAnError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val api = object : CampusApi { override suspend fun getEvents(): List<EventDto> = throw CancellationException() }
            val vm = EventsViewModel(EventsRepository(EventsRemoteDataSource(api)))
            advanceUntilIdle()
            assertEquals(EventsUiState.Loading, vm.state.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun repeatedRefreshDoesNotOverlapRequests() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var calls = 0
            val gate = CompletableDeferred<List<EventDto>>()
            val api = object : CampusApi { override suspend fun getEvents(): List<EventDto> { calls++; return gate.await() } }
            val vm = EventsViewModel(EventsRepository(EventsRemoteDataSource(api)))
            runCurrent(); vm.refresh(); vm.refresh(); runCurrent()
            assertEquals(1, calls)
            gate.complete(emptyList()); advanceUntilIdle()
            assertEquals(EventsUiState.Empty, vm.state.value)
        } finally { Dispatchers.resetMain() }
    }
}
