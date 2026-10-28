package com.inci.campuscompanion.data

import android.util.Log
import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.remote.EventDto
import com.inci.campuscompanion.model.CampusEvent
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException

class EventsRepository(private val api: CampusApi) {

    suspend fun getEvents(): Result<List<CampusEvent>> =
        try {
            Result.success(api.getEvents().mapNotNull { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Message carries the type: Log hides UnknownHostException stack traces
            Log.w(TAG, "Failed to load events: ${e.javaClass.name}: ${e.message}", e)
            Result.failure(e)
        }

    private companion object {
        const val TAG = "EventsRepository"
    }
}

fun EventDto.toDomain(): CampusEvent? {
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val title = name?.takeIf { it.isNotBlank() } ?: return null
    return CampusEvent(
        id = id,
        title = title,
        location = address?.takeIf { it.isNotBlank() } ?: "TBA",
        startsAt = startingDate?.let(::formatEventDate) ?: "Time TBA",
        description = description.orEmpty()
    )
}

// minSdk 24 has no java.time without desugaring, so parse the ISO-8601 UTC string with SimpleDateFormat.
// Unparseable dates fall back to the raw string instead of failing the whole list.
internal fun formatEventDate(iso: String): String {
    val parsed = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'").firstNotNullOfOrNull { pattern ->
        try {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(iso)
        } catch (e: ParseException) {
            null
        }
    } ?: return iso
    return SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(parsed)
}
