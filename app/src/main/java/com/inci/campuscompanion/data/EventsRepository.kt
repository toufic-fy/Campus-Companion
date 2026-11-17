package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface EventsSource {
    suspend fun getEvents(): List<CampusEvent>
}

class InvalidEventDataException : Exception("Invalid event data")

class EventsRepository(private val remote: EventsRemoteDataSource) : EventsSource {
    override suspend fun getEvents(): List<CampusEvent> {
        val events = remote.getEvents().map { dto ->
            if (dto.id.isBlank() || dto.name.isBlank()) throw InvalidEventDataException()
            val start = parseDate(dto.startingDate)
            val end = parseDate(dto.endingDate)
            if (end.before(start)) throw InvalidEventDataException()
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            CampusEvent(
                id = dto.id,
                title = dto.name.trim(),
                timeLabel = "${formatter.format(start)} – ${formatter.format(end)}",
                locationLabel = dto.address?.trim()?.takeIf { it.isNotEmpty() } ?: "TBA",
                description = dto.description.trim()
            )
        }
        if (events.map { it.id }.distinct().size != events.size) throw InvalidEventDataException()
        return events
    }

    private fun parseDate(value: String): Date {
        // MockAPI supplies UTC ISO-8601 timestamps with milliseconds.
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = ParsePosition(0)
        val date = parser.parse(value, position)
        if (date == null || position.index != value.length) throw InvalidEventDataException()
        return date
    }
}
