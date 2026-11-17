package com.inci.campuscompanion

import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.notification.EventUpdatePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventUpdatePolicyTest {
    private fun event(id: String) = CampusEvent(
        id = id,
        title = "Event $id",
        description = "",
        startsAt = "2026-11-20T09:00:00Z",
        timeLabel = "Nov 20, 2026"
    )

    @Test
    fun firstRefreshNeverCountsAsNew() {
        assertTrue(EventUpdatePolicy.newEventsSince(null, listOf(event("1"), event("2"))).isEmpty())
    }

    @Test
    fun unchangedDataHasNothingNew() {
        assertTrue(EventUpdatePolicy.newEventsSince(setOf("1", "2"), listOf(event("1"), event("2"))).isEmpty())
    }

    @Test
    fun onlyNewlyPublishedEventsAreReturned() {
        val new = EventUpdatePolicy.newEventsSince(setOf("1"), listOf(event("1"), event("2"), event("3")))
        assertEquals(listOf("2", "3"), new.map { it.id })
    }

    @Test
    fun notifyOnlyWhenPreferencePermissionAndChangeAllAgree() {
        val new = listOf(event("3"))
        assertTrue(EventUpdatePolicy.shouldNotify(preferenceEnabled = true, permissionGranted = true, newEvents = new))
        assertFalse(EventUpdatePolicy.shouldNotify(preferenceEnabled = false, permissionGranted = true, newEvents = new))
        // Exit-question scenario: updates enabled but permission denied -> no notification.
        assertFalse(EventUpdatePolicy.shouldNotify(preferenceEnabled = true, permissionGranted = false, newEvents = new))
        assertFalse(EventUpdatePolicy.shouldNotify(preferenceEnabled = true, permissionGranted = true, newEvents = emptyList()))
    }
}
