package com.inci.campuscompanion

import com.inci.campuscompanion.data.EventEntity
import com.inci.campuscompanion.model.CampusEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventUpdateDetectionTest {

    @Test
    fun `detects new event when event does not exist in cache`() {
        val cachedEventsMap = mapOf<String, EventEntity>()

        val remoteEvent = CampusEvent(
            id = "101",
            title = "Tech Symposium 2026",
            description = "Annual Tech Event",
            date = "2026-11-15",
            location = "Main Auditorium"
        )

        val cached = cachedEventsMap[remoteEvent.id]
        val isNewOrModified = cached == null ||
                cached.title != remoteEvent.title ||
                cached.description != remoteEvent.description ||
                cached.date != remoteEvent.date ||
                cached.location != remoteEvent.location

        assertTrue("New event should be detected as update", isNewOrModified)
    }

    @Test
    fun `detects modified event when title or location changes`() {
        val cachedEntity = EventEntity(
            id = "101",
            title = "Old Title",
            description = "Description",
            date = "2026-11-15",
            location = "Room 101"
        )
        val cachedEventsMap = mapOf(cachedEntity.id to cachedEntity)

        val remoteEvent = CampusEvent(
            id = "101",
            title = "New Updated Title",
            description = "Description",
            date = "2026-11-15",
            location = "Room 101"
        )

        val cached = cachedEventsMap[remoteEvent.id]
        val isNewOrModified = cached == null ||
                cached.title != remoteEvent.title ||
                cached.description != remoteEvent.description ||
                cached.date != remoteEvent.date ||
                cached.location != remoteEvent.location

        assertTrue("Modified event should be detected as update", isNewOrModified)
    }

    @Test
    fun `ignores unchanged event when cached data is identical`() {
        val cachedEntity = EventEntity(
            id = "101",
            title = "Tech Symposium 2026",
            description = "Annual Tech Event",
            date = "2026-11-15",
            location = "Main Auditorium"
        )
        val cachedEventsMap = mapOf(cachedEntity.id to cachedEntity)

        val remoteEvent = CampusEvent(
            id = "101",
            title = "Tech Symposium 2026",
            description = "Annual Tech Event",
            date = "2026-11-15",
            location = "Main Auditorium"
        )

        val cached = cachedEventsMap[remoteEvent.id]
        val isNewOrModified = cached == null ||
                cached.title != remoteEvent.title ||
                cached.description != remoteEvent.description ||
                cached.date != remoteEvent.date ||
                cached.location != remoteEvent.location

        assertFalse("Unchanged event should NOT be detected as update", isNewOrModified)
    }
}
