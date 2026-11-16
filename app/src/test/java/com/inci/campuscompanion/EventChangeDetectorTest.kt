package com.inci.campuscompanion

import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.notifications.EventChangeDetector
import org.junit.Assert.assertEquals
import org.junit.Test

class EventChangeDetectorTest {

    private val talk = CampusEvent(1, "Tech talk", "Room 101")
    private val fair = CampusEvent(2, "Career fair", "Main hall")

    @Test
    fun firstLoad_returnsNothing() {
        val changes = EventChangeDetector.findNewOrChanged(emptyList(), listOf(talk, fair))
        assertEquals(emptyList<CampusEvent>(), changes)
    }

    @Test
    fun noChanges_returnsNothing() {
        val changes = EventChangeDetector.findNewOrChanged(listOf(talk, fair), listOf(talk, fair))
        assertEquals(emptyList<CampusEvent>(), changes)
    }

    @Test
    fun newEvent_isReturned() {
        val changes = EventChangeDetector.findNewOrChanged(listOf(talk), listOf(talk, fair))
        assertEquals(listOf(fair), changes)
    }

    @Test
    fun changedEvent_isReturned() {
        val movedTalk = talk.copy(description = "Room 202")
        val changes = EventChangeDetector.findNewOrChanged(listOf(talk, fair), listOf(movedTalk, fair))
        assertEquals(listOf(movedTalk), changes)
    }

    @Test
    fun removedEvent_isNotNotified() {
        val changes = EventChangeDetector.findNewOrChanged(listOf(talk, fair), listOf(talk))
        assertEquals(emptyList<CampusEvent>(), changes)
    }
}
