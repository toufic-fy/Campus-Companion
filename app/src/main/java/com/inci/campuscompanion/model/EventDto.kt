package com.inci.campuscompanion.model

data class EventDto(
    val id: String,
    val name: String,
    val startingDate: String,
    val address: String? = null
)

data class CampusEvent(
    val id: String,
    val title: String,
    val timeLabel: String,
    val locationLabel: String
)
