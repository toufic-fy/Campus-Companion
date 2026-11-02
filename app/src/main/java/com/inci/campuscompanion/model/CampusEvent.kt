package com.inci.campuscompanion.model

data class CampusEvent(
    val id: String,
    val title: String,
    val description: String,
    val startsAt: String,
    val endsAt: String? = null,
    val timeLabel: String,
    val locationLabel: String? = null
)
