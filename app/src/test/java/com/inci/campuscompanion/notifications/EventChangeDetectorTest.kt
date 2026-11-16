package com.inci.campuscompanion.notifications

import com.inci.campuscompanion.model.CampusEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventChangeDetectorTest {

    private fun event(id: String, title: String = "Event $id", startsAt: String = "2026-11-20T10:00:00Z") =
        CampusEvent(
            id = id,
            title = title,
            description = "desc",
            startsAt = startsAt,
            timeLabel = "Nov 20, 2026 • 10:00 AM",
            locationLabel = "Hall A"
        )

    @Test
    fun firstRun_onlyRecordsBaseline_noNotification() {
        val changes = EventChangeDetector.detect(null, listOf(event("1"), event("2")))
        assertFalse(changes.isMeaningful)
    }

    @Test
    fun unchangedData_isNotMeaningful() {
        val events = listOf(event("1"), event("2"))
        val snapshot = EventChangeDetector.snapshotOf(events)
        assertFalse(EventChangeDetector.detect(snapshot, events).isMeaningful)
    }

    @Test
    fun newEvent_isDetected() {
        val snapshot = EventChangeDetector.snapshotOf(listOf(event("1")))
        val changes = EventChangeDetector.detect(snapshot, listOf(event("1"), event("2", title = "Career Fair")))
        assertEquals(listOf("2"), changes.newEvents.map { it.id })
        assertTrue(changes.updatedEvents.isEmpty())
        assertEquals("New event: Career Fair", changes.notificationTitle())
    }

    @Test
    fun changedTime_isAnUpdate() {
        val snapshot = EventChangeDetector.snapshotOf(listOf(event("1")))
        val changes = EventChangeDetector.detect(
            snapshot,
            listOf(event("1", startsAt = "2026-11-21T10:00:00Z"))
        )
        assertTrue(changes.newEvents.isEmpty())
        assertEquals(listOf("1"), changes.updatedEvents.map { it.id })
    }

    @Test
    fun removedEvent_aloneDoesNotNotify() {
        val snapshot = EventChangeDetector.snapshotOf(listOf(event("1"), event("2")))
        assertFalse(EventChangeDetector.detect(snapshot, listOf(event("1"))).isMeaningful)
    }
}
