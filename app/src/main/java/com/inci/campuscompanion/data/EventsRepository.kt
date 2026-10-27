package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.network.EventDto
import com.inci.campuscompanion.data.network.EventsRemoteDataSource
import com.inci.campuscompanion.model.CampusEvent
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EventsRepository(private val remote: EventsRemoteDataSource) {

    suspend fun getEvents(): List<CampusEvent> =
        remote.getEvents().map { it.toCampusEvent() }

    private fun EventDto.toCampusEvent() = CampusEvent(
        id = id,
        title = name,
        description = description.orEmpty(),
        timeLabel = formatDate(startingDate),
        locationLabel = address ?: "TBA"
    )

    // Server dates are ISO-8601 in UTC, e.g. 2026-10-20T12:23:40.729Z.
    private fun formatDate(raw: String?): String {
        if (raw == null) return "Date TBA"
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return try {
            val date = parser.parse(raw) ?: return raw
            SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()).format(date)
        } catch (e: ParseException) {
            raw
        }
    }
}
