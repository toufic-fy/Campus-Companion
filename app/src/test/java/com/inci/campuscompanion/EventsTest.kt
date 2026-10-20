package com.inci.campuscompanion

import com.inci.campuscompanion.data.*
import com.inci.campuscompanion.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EventsTest {
 private val valid = """[{"id":"1","name":"Campus meetup","startingDate":"2026-10-20T12:00:00Z","address":null}]"""
 private suspend fun withRepository(body: String, code: Int = 200, block: suspend (EventsRepository) -> Unit) {
  val server = MockWebServer()
  server.start()
  try {
   server.enqueue(MockResponse().setResponseCode(code).setBody(body).setHeader("Content-Type", "application/json"))
   val repo = EventsRepository(EventsRemoteDataSource(AppContainer.createApi(server.url("/api/v1/events/").toString())))
   block(repo)
   assertEquals("/api/v1/events/event", server.takeRequest().path)
  } finally { server.shutdown() }
 }
 @Test fun mapsActualContract() = runTest {
  withRepository(valid) { repo ->
   val result = repo.getEvents().single()
   assertEquals("Campus meetup", result.title)
   assertEquals("Location to be announced", result.locationLabel)
   assertEquals("Not specified", result.endLabel)
  }
 }
 @Test fun emptyIsSuccessful() = runTest { withRepository("[]") { assertTrue(it.getEvents().isEmpty()) } }
 @Test fun rejectsMalformedDates() = runTest {
  withRepository(valid.replace("2026-10-20T12:00:00Z", "bad date")) {
   try { it.getEvents(); fail("Expected invalid data") } catch (_: InvalidEventData) { }
  }
 }
 @Test fun rejectsMissingRequiredFields() = runTest {
  withRepository("""[{"id":"1"}]""") {
   try { it.getEvents(); fail("Expected invalid data") } catch (e: com.squareup.moshi.JsonDataException) {
    assertTrue(errorMessage(e).contains("invalid event data"))
   }
  }
 }
 @Test fun classifiesServerFailure() = runTest {
  withRepository("{}", 500) {
   try { it.getEvents(); fail("Expected HTTP failure") } catch (e: retrofit2.HttpException) {
    assertTrue(errorMessage(e).contains("server is unavailable"))
   }
  }
 }
 @Test fun classifiesConnectionFailure() { assertTrue(errorMessage(IOException()).contains("internet connection")) }
 @Test fun viewModelRetriesAfterFailure() = runTest {
  Dispatchers.setMain(StandardTestDispatcher(testScheduler))
  val server = MockWebServer()
  server.start()
  try {
   server.enqueue(MockResponse().setResponseCode(500))
   server.enqueue(MockResponse().setBody(valid))
   val model = EventsViewModel(EventsRepository(EventsRemoteDataSource(AppContainer.createApi(server.url("/").toString()))))
   assertTrue(model.state.value is EventsUiState.Loading)
   // Retrofit runs on OkHttp threads; await each observable completion with a bounded real timeout.
   val deadline = System.currentTimeMillis() + 10000
   while (model.state.value is EventsUiState.Loading && System.currentTimeMillis() < deadline) { testScheduler.runCurrent(); Thread.sleep(10) }
   assertTrue(model.state.value is EventsUiState.Error)
   model.refresh()
   testScheduler.runCurrent()
   val retryDeadline = System.currentTimeMillis() + 10000
   while (model.state.value is EventsUiState.Loading && System.currentTimeMillis() < retryDeadline) { testScheduler.runCurrent(); Thread.sleep(10) }
   assertTrue(model.state.value is EventsUiState.Content)
  } finally { server.shutdown(); Dispatchers.resetMain() }
 }
}
