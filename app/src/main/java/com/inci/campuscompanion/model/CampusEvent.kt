package com.inci.campuscompanion.model

// Domain model used by the UI (independent of the API JSON)
data class CampusEvent(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val startDate: String,
    val endDate: String,
    val imageUrl: String?
)
