package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EventsRepository(private val remote: EventsRemoteDataSource) {
    suspend fun getEvents(): List<CampusEvent> = remote.getEvents().map { dto ->
        require(dto.id.isNotBlank() && dto.name.isNotBlank()) { "Invalid event identity" }
        CampusEvent(
            id = dto.id,
            title = dto.name,
            timeLabel = formatDate(dto.startingDate),
            locationLabel = dto.address?.takeIf { it.isNotBlank() } ?: "Location TBA",
            description = dto.description
        )
    }

    private fun formatDate(value: String): String {
        // MockAPI emits UTC dates with variable fractional precision.
        val normalized = value.replace(Regex("\\.\\d+Z$"), "Z")
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val position = java.text.ParsePosition(0)
        val date = parser.parse(normalized, position)
        require(date != null && position.index == normalized.length) { "Invalid event date" }
        return SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a z", Locale.getDefault()).format(date)
    }
}
