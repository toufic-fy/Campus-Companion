package com.inci.campuscompanion.model

data class CampusEvent(
    val id: Int,
    val title: String,
    val location: String,
    val startsAt: String,
    val description: String
)
