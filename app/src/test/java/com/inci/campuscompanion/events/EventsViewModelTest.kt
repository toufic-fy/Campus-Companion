package com.inci.campuscompanion.events

import com.inci.campuscompanion.data.EventsSource
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.ui.EventsUiState
import com.inci.campuscompanion.ui.EventsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun loadingBecomesContentAndRefreshDoesNotOverlap() = runTest(dispatcher) {
        var calls = 0
        val source = object : EventsSource {
            override suspend fun getEvents(): List<CampusEvent> {
                calls++
                return listOf(CampusEvent("1", "Meetup", "Today", "Campus", ""))
            }
        }
        val viewModel = EventsViewModel(source)
        assertEquals(EventsUiState.Loading, viewModel.uiState.value)
        viewModel.refresh()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is EventsUiState.Content)
        assertEquals(1, calls)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(2, calls)
    }

    @Test fun networkErrorCanRecoverToEmptyAfterRetry() = runTest(dispatcher) {
        var fail = true
        val source = object : EventsSource {
            override suspend fun getEvents(): List<CampusEvent> {
                if (fail) throw IOException("Offline")
                return emptyList()
            }
        }
        val viewModel = EventsViewModel(source)
        advanceUntilIdle()
        val error = viewModel.uiState.value as EventsUiState.Error
        assertTrue(error.message.contains("connection"))
        fail = false
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(EventsUiState.Empty, viewModel.uiState.value)
    }

    @Test fun invalidDataIsLoggedWithoutLeakingTheResponse() = runTest(dispatcher) {
        val messages = mutableListOf<String>()
        val logger = java.util.logging.Logger.getAnonymousLogger().apply {
            useParentHandlers = false
            addHandler(object : java.util.logging.Handler() {
                override fun publish(record: java.util.logging.LogRecord) { messages.add(record.message) }
                override fun flush() { }
                override fun close() { }
            })
        }
        val source = object : EventsSource {
            override suspend fun getEvents(): List<CampusEvent> =
                throw kotlinx.serialization.SerializationException("private server response")
        }
        val viewModel = EventsViewModel(source, logger)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is EventsUiState.Error)
        assertEquals(listOf("Events request failed: SerializationException"), messages)
        assertFalse(messages.joinToString().contains("private server response"))
    }

}
