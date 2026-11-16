package com.campus.events.data.remote

import android.content.Context
import com.campus.events.data.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Shape of an event as the server sends it. */
data class EventDto(
    val id: String,
    val name: String,
    val venue: String,
    val start: String,
    val details: String,
)

fun EventDto.toDomain() = Event(
    id = id,
    title = name,
    location = venue,
    startsAt = start,
    description = details,
)

/**
 * Stand-in for the Events API so the workshop runs without a backend.
 *
 * The server always has a few base events. [publishNewEvent] simulates staff publishing a new
 * one; the count is persisted so the UI and the background worker see the same "server".
 */
class FakeEventsApi(context: Context) {

    private val prefs = context.getSharedPreferences("fake_events_server", Context.MODE_PRIVATE)

    suspend fun fetchEvents(): List<EventDto> = withContext(Dispatchers.IO) {
        delay(600) // pretend network latency
        val published = prefs.getInt(KEY_PUBLISHED, 0)
        baseEvents + (1..published).map(::extraEvent)
    }

    /** Simulates a new event appearing on the server. Returns its id. */
    fun publishNewEvent(): String {
        val next = prefs.getInt(KEY_PUBLISHED, 0) + 1
        prefs.edit().putInt(KEY_PUBLISHED, next).apply()
        return extraEvent(next).id
    }

    private fun extraEvent(n: Int): EventDto {
        val template = extraTemplates[(n - 1) % extraTemplates.size]
        val suffix = if (n > extraTemplates.size) " #$n" else ""
        return template.copy(id = "evt-new-$n", name = template.name + suffix)
    }

    private companion object {
        const val KEY_PUBLISHED = "published_count"

        val baseEvents = listOf(
            EventDto("evt-1", "Welcome week fair", "Main quad", "Mon 10:00", "Meet clubs and societies."),
            EventDto("evt-2", "Library research workshop", "Library, room 2.04", "Tue 14:00", "Find and cite sources effectively."),
            EventDto("evt-3", "Career talk: Mobile development", "Lecture hall B", "Wed 16:00", "Alumni share how they got into Android."),
        )

        val extraTemplates = listOf(
            EventDto("", "Hackathon kickoff", "Innovation lab", "Fri 18:00", "Form teams and pitch ideas."),
            EventDto("", "Open-air movie night", "Sports field", "Sat 20:30", "Bring a blanket, popcorn provided."),
            EventDto("", "Android study group", "Computer lab 3", "Thu 12:00", "Review notifications and permissions."),
            EventDto("", "Volunteering info session", "Student union", "Mon 13:00", "Local organisations looking for help."),
        )
    }
}
