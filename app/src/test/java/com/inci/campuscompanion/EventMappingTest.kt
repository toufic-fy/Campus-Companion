package com.inci.campuscompanion

import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.data.toCampusEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class EventMappingTest {

    @Test
    fun mapsAllFields() {
        val dto = EventDto(
            id = "1",
            name = "Hackathon",
            description = "24h of building",
            address = "Main Hall",
            avatar = null,
            startingDate = "2026-10-20T12:23:40.729Z",
            endingDate = "2027-04-06T08:31:09.288Z",
            createdAt = null
        )

        val event = dto.toCampusEvent()

        assertEquals("1", event.id)
        assertEquals("Hackathon", event.title)
        assertEquals("Main Hall", event.location)
        assertEquals("2026-10-20", event.startsAt)
        assertEquals("2027-04-06", event.endsAt)
    }

    @Test
    fun missingFieldsGetDefaults() {
        val dto = EventDto(null, null, null, null, null, null, null, null)

        val event = dto.toCampusEvent()

        assertEquals("Untitled event", event.title)
        assertEquals("TBA", event.location)
        assertEquals("", event.description)
    }
}
