package com.inci.campuscompanion.events

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import com.inci.campuscompanion.ui.EventsScreen
import com.inci.campuscompanion.ui.EventsViewModel
import kotlinx.coroutines.channels.Channel
import org.junit.Rule
import org.junit.Test
import java.io.IOException

// Drives the full data -> repository -> ViewModel -> Compose path without modifying the shared API.
class EventsRecoveryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun slowOfflineEmptyAndBadDataRecoverAfterRetry() {
        val responses = Channel<Result<List<EventDto>>>(Channel.UNLIMITED)
        val api = object : CampusApi {
            override suspend fun getEvents() = responses.receive().getOrThrow()
        }
        compose.setContent {
            val viewModel = remember { EventsViewModel(EventsRepository(EventsRemoteDataSource(api))) }
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MaterialTheme { EventsScreen(state, viewModel::refresh) }
        }
        // Request deliberately remains suspended: the screen stays responsive and shows Loading.
        compose.onNodeWithText("Refresh").assertIsNotEnabled()
        responses.trySend(Result.failure(IOException("Offline"))).getOrThrow()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Unable to connect. Check your internet connection and retry.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        val event = EventDto("1", "Recovery event", "2026-10-20T12:23:40.729Z", "2026-10-20T14:23:40.729Z")
        responses.trySend(Result.success(listOf(event))).getOrThrow()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Recovery event").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Recovery event").assertIsDisplayed()
        compose.onNodeWithText("Refresh").performClick()
        responses.trySend(Result.success(emptyList())).getOrThrow()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("No campus events yet.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("No campus events yet.").assertIsDisplayed()
        compose.onNodeWithText("Refresh").performClick()
        responses.trySend(Result.success(listOf(event.copy(startingDate = "invalid")))).getOrThrow()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("The server returned invalid event data. Please try again later.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        responses.trySend(Result.success(listOf(event))).getOrThrow()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Recovery event").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Recovery event").assertIsDisplayed()
        responses.close()
    }
}
