package com.inci.campuscompanion.model

data class CampusEvent(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val startsAt: String,
    val endsAt: String
)
