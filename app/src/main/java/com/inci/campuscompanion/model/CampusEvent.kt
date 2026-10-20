package com.inci.campuscompanion.model

/**
 * UI-ready event. Server field names do not leak in here.
 */
data class CampusEvent(
    val id: String,
    val title: String,
    val timeLabel: String,
    val locationLabel: String,
    val description: String
)
