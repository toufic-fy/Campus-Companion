package com.inci.campuscompanion

import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.inci.campuscompanion.data.*
import com.inci.campuscompanion.model.CampusEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.FileInputStream

/** Exercises the real permission dialog, OS notification and its PendingIntent.
 * New-event responses use an isolated Room cache; the class API is never changed.
 */
class WorkshopNotificationsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun shell(command: String) = instrumentation.uiAutomation.executeShellCommand(command).use {
        FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
    }
    private fun capture(name: String, result: String) {
        compose.waitForIdle()
        shell("mkdir -p /sdcard/Download/charbel-events-evidence")
        shell("screencap -p /sdcard/Download/charbel-events-evidence/$name.png")
        Log.i("CampusExercise", "$name: $result")
    }

    @Test fun optInDenialAllowedDeliveryDeduplicationAndNotificationTap() = runBlocking {
        if (Build.VERSION.SDK_INT < 33) return@runBlocking
        val context = instrumentation.targetContext
        val graph = (context.applicationContext as CampusApplication).graph
        val manager = context.getSystemService(NotificationManager::class.java)
        val device = UiDevice.getInstance(instrumentation)
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        manager.cancelAll()
        graph.preferences.setEventUpdatesEnabled(false)
        compose.waitForIdle()
        try {
            assertFalse("Fresh install must not prompt on launch", graph.notifications.hasRuntimePermission())
            assertFalse(graph.preferences.eventUpdatesEnabled.first())
            assertFalse(graph.notifications.showTest())
            compose.onNodeWithContentDescription("Event updates").performClick()
            val deny = device.wait(Until.findObject(By.res("com.android.permissioncontroller", "permission_deny_button")), 10_000)
            assertNotNull("Opt-in must request Android permission", deny)
            deny!!.click()
            compose.waitUntil(10_000) { !graph.notifications.hasRuntimePermission() &&
                compose.onAllNodesWithText("Notifications blocked. You can allow them in system settings.").fetchSemanticsNodes().isNotEmpty() }
            assertTrue(graph.preferences.eventUpdatesEnabled.first())
            assertFalse(graph.notifications.showTest())
            assertTrue(manager.activeNotifications.isEmpty())
            compose.onNodeWithText("Campus events").assertIsDisplayed()
            capture("workshop-notifications-denied", "Permission denied; preference retained; no notification; app usable")

            compose.onNodeWithContentDescription("Event updates").performClick()
            compose.waitUntil(5_000) { runBlocking { !graph.preferences.eventUpdatesEnabled.first() } }
            compose.onNodeWithContentDescription("Event updates").performClick()
            val allow = device.wait(Until.findObject(By.res("com.android.permissioncontroller", "permission_allow_button")), 10_000)
            assertNotNull("Re-enabling updates should offer permission", allow)
            allow!!.click()
            compose.waitUntil(10_000) { graph.notifications.systemAllowsNotifications() }
            compose.onNodeWithText("Send test").performClick()
            compose.waitUntil(10_000) { manager.activeNotifications.isNotEmpty() }
            val testNotice = manager.activeNotifications.single()
            assertEquals("Event updates are enabled", testNotice.notification.extras.getString("android.title"))
            assertTrue(testNotice.notification.smallIcon != null)
            assertTrue(testNotice.notification.flags and android.app.Notification.FLAG_AUTO_CANCEL != 0)
            testNotice.notification.contentIntent.send()
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
            compose.onNodeWithText("Campus events").assertIsDisplayed()
            manager.cancelAll()

            var response = emptyList<CampusEvent>()
            var deliveries = 0
            val repository = CachedEventsRepository(object : EventsSource {
                override suspend fun getEvents() = response
            }, RoomEventsCache(database.eventDao())) {
                if (graph.notifications.showNewEvents(it)) deliveries++
            }
            repository.getEvents() // A genuinely empty first response is a silent baseline.
            assertTrue(manager.activeNotifications.isEmpty())
            response = listOf(CampusEvent("notification-test-event", "Workshop new event", "Today", "Campus", ""))
            repository.getEvents()
            repository.getEvents()
            assertEquals(1, deliveries)
            assertEquals(1, manager.activeNotifications.size)
            val eventNotice = manager.activeNotifications.single()
            assertEquals("Workshop new event", eventNotice.notification.extras.getString("android.text"))
            capture("workshop-notifications-new-event", "New event posted once; unchanged refresh did not duplicate it")
            eventNotice.notification.contentIntent.send()
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
            compose.onNodeWithText("Campus events").assertIsDisplayed()
            manager.cancelAll()

            graph.preferences.setEventUpdatesEnabled(false)
            response = response + CampusEvent("suppressed-event", "Preference off", "Today", "Campus", "")
            repository.getEvents()
            assertEquals(2, database.eventDao().observeEvents().first().size)
            assertTrue(manager.activeNotifications.isEmpty())
            graph.preferences.setEventUpdatesEnabled(true)
            repository.getEvents()
            assertEquals(1, deliveries)
            assertTrue(manager.activeNotifications.isEmpty())
            capture("workshop-notifications-policy", "Preference off suppresses delivery while cache updates; enabling later does not replay unchanged events")
        } finally {
            graph.preferences.setEventUpdatesEnabled(false)
            manager.cancelAll()
            database.close()
        }
    }
}
