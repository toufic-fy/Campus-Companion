package com.inci.campuscompanion

import androidx.room.Room
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.remote.api.CampusApi
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.dto.EventDto
import com.inci.campuscompanion.data.repository.EventsRepository
import android.app.NotificationManager
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.notifications.EventNotifications
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class NotificationWorkshopTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private val sample = CampusEvent("workshop-test", "Campus workshop test event", "Controlled test fixture", "", null, "", null)

    private fun capture(name: String) {
        val folder = File(context.getExternalFilesDir(null), "workshop-evidence").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { output ->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Test fun deniedThenAllowedPreferenceOffAndNotificationTap() = runBlocking {
        val preferences = UserPreferencesRepository(context.dataStore)
        preferences.setEventUpdatesEnabled(false)
        manager.cancelAll()
        compose.waitForIdle()
        compose.onNodeWithText("Campus events").assertIsDisplayed()
        capture("01-tasks-default")
        assertFalse(EventNotifications.systemAllows(context)) // runner revokes permission before this test
        compose.onNodeWithContentDescription("Enable event updates").performClick()
        val deny = device.wait(Until.findObject(By.res("com.android.permissioncontroller", "permission_deny_button")), 10000)
        assertNotNull("Contextual Android permission dialog must appear", deny)
        device.takeScreenshot(File(context.getExternalFilesDir(null), "workshop-evidence/02-permission-dialog.png"))
        deny.click()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Open notification settings").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(preferences.eventUpdatesEnabled.first())
        assertFalse(EventNotifications.show(context, listOf(sample)))
        assertTrue(manager.activeNotifications.isEmpty())
        capture("03-permission-denied")
        compose.onNodeWithText("Campus events").performClick()
        compose.onNodeWithText("Campus Events").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        device.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Send test notification").fetchSemanticsNodes().isNotEmpty() }
        preferences.setEventUpdatesEnabled(false)
        assertTrue(EventNotifications.systemAllows(context))
        assertFalse(EventNotifications.show(context, listOf(sample)))
        assertTrue(manager.activeNotifications.isEmpty())
        compose.waitForIdle()
        capture("04-preference-off-permission-granted")
        preferences.setEventUpdatesEnabled(true)
        compose.waitUntil(10000) { compose.onAllNodesWithText("Send test notification").fetchSemanticsNodes().isNotEmpty() }
        // Controlled remote fixtures exercise the real Room -> change policy -> notification path.
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            var remote = emptyList<EventDto>()
            val api = object : CampusApi { override suspend fun getEvents() = remote }
            val repo = EventsRepository(EventsRemoteDataSource(api), db)
            repo.refreshEvents { EventNotifications.show(context, it) }
            assertTrue(manager.activeNotifications.isEmpty())
            remote = listOf(EventDto("controlled-new", "New campus event fixture", "Test data", "2026-11-20T12:00:00Z"))
            repo.refreshEvents { assertTrue(EventNotifications.show(context, it)) }
            compose.waitUntil(10000) { manager.activeNotifications.size == 1 }
            assertEquals("New campus event", manager.activeNotifications.single().notification.extras.getString("android.title"))
            manager.cancelAll()
            compose.waitUntil(10000) { manager.activeNotifications.isEmpty() }
            repo.refreshEvents { fail("Unchanged data must not reach the notification helper") }
            assertTrue(manager.activeNotifications.isEmpty())
            preferences.setEventUpdatesEnabled(false)
            remote = remote + EventDto("off-new", "Preference-off fixture", "Test data", "2026-11-21T12:00:00Z")
            repo.refreshEvents { assertFalse(EventNotifications.show(context, it)) }
            assertEquals(2, repo.cachedEvents.first().size)
            assertTrue(manager.activeNotifications.isEmpty())
            preferences.setEventUpdatesEnabled(true)
            repo.refreshEvents { fail("Re-enabling must not notify about already confirmed events") }
        } finally { db.close() }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Send test notification").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Send test notification").performClick()
        compose.waitUntil(10000) { manager.activeNotifications.isNotEmpty() }
        assertEquals(1, manager.activeNotifications.size)
        device.openNotification()
        val title = device.wait(Until.findObject(By.text("Test campus event")), 10000)
        assertNotNull("Notification title must be visible in the shade", title)
        device.takeScreenshot(File(context.getExternalFilesDir(null), "workshop-evidence/05-notification-shade.png"))
        title.click()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Campus Events").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Campus Events").assertIsDisplayed()
        capture("06-notification-opens-events")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Campus events").assertIsDisplayed()
        assertTrue(manager.activeNotifications.isEmpty())
        preferences.setEventUpdatesEnabled(false)
    }
}
