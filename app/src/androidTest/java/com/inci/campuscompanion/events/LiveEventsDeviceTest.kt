package com.inci.campuscompanion.events

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.inci.campuscompanion.MainActivity
import java.io.FileInputStream
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Uses the real MainActivity, production Retrofit client and live HTTPS endpoint. */
class LiveEventsDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun shell(command: String): String = automation.executeShellCommand(command).use { fd ->
        FileInputStream(fd.fileDescriptor).bufferedReader().use { it.readText() }
    }

    private fun restoreConnectivity() {
        shell("cmd connectivity airplane-mode disable")
        shell("svc wifi enable")
        shell("svc data enable")
    }

    private fun awaitContent() {
        compose.waitUntil(45_000) {
            compose.onAllNodesWithTag("events-content").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("events-content").assertIsDisplayed()
    }

    private fun capture(name: String, result: String) {
        compose.waitForIdle()
        shell("mkdir -p /sdcard/Download/charbel-events-evidence")
        shell("screencap -p /sdcard/Download/charbel-events-evidence/$name.png")
        Log.i("CampusExercise", "$name: $result")
    }

    @Test fun actualAppLoadsLiveEventsAndRecoversAfterDeviceNetworkIsDisabled() {
        try {
            compose.onNodeWithText("Events", useUnmergedTree = true).performClick()
            awaitContent()
            capture("01-live-content", "Real MainActivity loaded event cards from the production HTTPS API")

            shell("cmd connectivity airplane-mode enable")
            shell("svc wifi disable")
            shell("svc data disable")
            Thread.sleep(3_000)
            assertEquals("1", shell("settings get global airplane_mode_on").trim())
            compose.onNodeWithText("Refresh").performClick()
            compose.waitUntil(45_000) {
                compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Unable to connect. Check your internet connection and retry.").assertIsDisplayed()
            capture("02-real-offline-error", "Airplane mode on, Wi-Fi/data disabled; real network request failed; Retry visible")

            restoreConnectivity()
            Thread.sleep(8_000)
            assertEquals("0", shell("settings get global airplane_mode_on").trim())
            compose.onNodeWithText("Retry").performClick()
            awaitContent()
            capture("03-live-retry-recovery", "Connectivity restored; Retry fetched live event cards again")
        } finally {
            restoreConnectivity()
        }
    }
}
