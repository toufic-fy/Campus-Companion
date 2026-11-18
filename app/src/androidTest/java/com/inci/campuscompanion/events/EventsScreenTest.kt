package com.inci.campuscompanion.events

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.ui.EventsScreen
import com.inci.campuscompanion.ui.EventsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EventsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadingDisablesRefresh() {
        compose.setContent { MaterialTheme { EventsScreen(EventsUiState.Loading, {}) } }
        compose.onNodeWithText("Refresh").assertIsNotEnabled()
    }

    @Test fun emptyStateHasRefresh() {
        var calls = 0
        compose.setContent { MaterialTheme { EventsScreen(EventsUiState.Empty, { calls++ }) } }
        compose.onNodeWithText("No campus events yet.").assertIsDisplayed()
        compose.onNodeWithText("Refresh").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun errorShowsRetryAndInvokesCallback() {
        var calls = 0
        compose.setContent {
            MaterialTheme { EventsScreen(EventsUiState.Error("Check your connection"), { calls++ }) }
        }
        compose.onNodeWithText("Check your connection").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun contentDisplaysMappedFields() {
        val event = CampusEvent("1", "Campus meetup", "20 Oct 2026, 15:00", "Main campus", "Meet students")
        compose.setContent { MaterialTheme { EventsScreen(EventsUiState.Content(listOf(event)), {}) } }
        for (text in listOf(event.title, event.timeLabel, event.locationLabel, event.description)) {
            compose.onNodeWithText(text).assertIsDisplayed()
        }
    }
}
