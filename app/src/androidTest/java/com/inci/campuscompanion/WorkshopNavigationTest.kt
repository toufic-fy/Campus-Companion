package com.inci.campuscompanion

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test

class WorkshopNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun tasksOpensFirstAndBothBackActionsReturnToTasks() {
        compose.onNodeWithText("Campus events").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        compose.onNodeWithText("Campus events").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitForIdle()
        compose.onNodeWithText("Campus events").assertIsDisplayed()
    }

    @Test fun notificationIntentWhileAppIsOpenOpensEventsAndKeepsTasksInBackStack() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("campus://events/test-event"),
                activity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        compose.onNodeWithText("Campus events").assertIsDisplayed()
    }
}
