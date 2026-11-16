package com.inci.campuscompanion

import com.inci.campuscompanion.notifications.canNotify
import com.inci.campuscompanion.notifications.newlyPublishedEvents
import org.junit.Assert.*
import org.junit.Test

class NotificationPolicyTest {
    @Test fun preferenceAndSystemPermissionAreBothRequired() {
        assertFalse(canNotify(false, false))
        assertFalse(canNotify(false, true))
        assertFalse(canNotify(true, false))
        assertTrue(canNotify(true, true))
    }

    @Test fun firstSuccessfulRefreshIsSilent() {
        assertEquals(emptyList<String>(), newlyPublishedEvents(emptySet(), listOf("a"), false) { it })
    }

    @Test fun unchangedOrRemovedEventsAreSilent() {
        assertEquals(emptyList<String>(), newlyPublishedEvents(setOf("a", "b"), listOf("a"), true) { it })
    }

    @Test fun onlyNewDistinctIdsAreReturned() {
        assertEquals(listOf("b", "c"),
            newlyPublishedEvents(setOf("a"), listOf("a", "b", "b", "c"), true) { it })
    }

    @Test fun eventAfterConfirmedEmptyRefreshIsNew() {
        assertEquals(listOf("a"), newlyPublishedEvents(emptySet(), listOf("a"), true) { it })
    }
}
