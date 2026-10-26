package com.inci.campuscompanion.data

import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.model.CampusEvent
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EventsRepository(private val campusApi: CampusApi) {

    suspend fun getEvents(): List<CampusEvent> =
        campusApi.getEvents().map { it.toCampusEvent() }
}

fun EventDto.toCampusEvent(): CampusEvent {
    val start = formatDate(startingDate)
    val end = formatDate(endingDate)
    return CampusEvent(
        id = id,
        title = name.ifBlank { "Untitled event" },
        dateLabel = when {
            start != null && end != null -> "$start – $end"
            start != null -> start
            else -> "Date TBA"
        },
        locationLabel = address?.takeIf { it.isNotBlank() } ?: "TBA",
        description = description.orEmpty()
    )
}

// Accepts ISO-8601 strings ("2026-11-03T18:00:00.000Z") or Unix timestamps.
// Anything else is shown as received.
private fun formatDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val date = raw.toLongOrNull()?.let { Date(if (it < 100_000_000_000L) it * 1000 else it) }
        ?: parseIsoDate(raw)
        ?: return raw
    return SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault()).format(date)
}

private fun parseIsoDate(raw: String): Date? {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return try {
        parser.parse(raw.take(19))
    } catch (e: ParseException) {
        null
    }
}
