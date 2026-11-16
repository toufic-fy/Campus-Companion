package com.inci.campuscompanion

import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.notification.diffEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDiffTest {

    private fun event(id: String, title: String = "Title $id", location: String? = "Hall") =
        CampusEvent(
            id = id,
            title = title,
            description = "",
            startsAt = "2026-01-01T10:00:00Z",
            endsAt = "2026-01-01T12:00:00Z",
            timeLabel = "",
            locationLabel = location
        )

    @Test
    fun firstRunOnlySavesBaseline() {
        val diff = diffEvents(null, listOf(event("1"), event("2")))
        assertTrue(diff.newEvents.isEmpty())
        assertTrue(diff.changedEvents.isEmpty())
        assertEquals(2, diff.snapshot.size)
    }

    @Test
    fun unchangedEventsGiveNothing() {
        val events = listOf(event("1"), event("2"))
        val baseline = diffEvents(null, events).snapshot
        val diff = diffEvents(baseline, events)
        assertTrue(diff.newEvents.isEmpty())
        assertTrue(diff.changedEvents.isEmpty())
    }

    @Test
    fun newEventIsDetected() {
        val baseline = diffEvents(null, listOf(event("1"))).snapshot
        val diff = diffEvents(baseline, listOf(event("1"), event("2")))
        assertEquals(listOf("2"), diff.newEvents.map { it.id })
        assertTrue(diff.changedEvents.isEmpty())
    }

    @Test
    fun changedEventIsDetected() {
        val baseline = diffEvents(null, listOf(event("1"), event("2"))).snapshot
        val diff = diffEvents(baseline, listOf(event("1", title = "Moved"), event("2")))
        assertTrue(diff.newEvents.isEmpty())
        assertEquals(listOf("1"), diff.changedEvents.map { it.id })
    }

    @Test
    fun locationChangeCountsAsChange() {
        val baseline = diffEvents(null, listOf(event("1"))).snapshot
        val diff = diffEvents(baseline, listOf(event("1", location = "Lab")))
        assertEquals(listOf("1"), diff.changedEvents.map { it.id })
    }
}
