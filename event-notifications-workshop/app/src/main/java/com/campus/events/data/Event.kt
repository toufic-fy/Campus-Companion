package com.campus.events.data

/** Domain model used by the UI and the notification layer. */
data class Event(
    val id: String,
    val title: String,
    val location: String,
    val startsAt: String,
    val description: String,
)
