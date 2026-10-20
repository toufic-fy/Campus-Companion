package com.inci.campuscompanion

import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.data.toCampusEvent
import com.inci.campuscompanion.model.CampusEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventMappingTest {
    @Test
    fun completeDto_mapsToCampusEvent() {
        val dto = EventDto(7, " Career Fair ", "Main Hall", "2026-10-22 10:00", "Meet employers.")

        assertEquals(
            CampusEvent(7, "Career Fair", "Main Hall", "2026-10-22 10:00", "Meet employers."),
            dto.toCampusEvent()
        )
    }

    @Test
    fun missingOptionalFields_useDefaults() {
        val event = EventDto(1, "Hackathon", null, "", null).toCampusEvent()!!

        assertEquals("Location TBA", event.location)
        assertEquals("Time TBA", event.startsAt)
        assertEquals("", event.description)
    }

    @Test
    fun missingIdOrTitle_isDropped() {
        assertNull(EventDto(null, "Hackathon", null, null, null).toCampusEvent())
        assertNull(EventDto(1, "  ", null, null, null).toCampusEvent())
    }
}
