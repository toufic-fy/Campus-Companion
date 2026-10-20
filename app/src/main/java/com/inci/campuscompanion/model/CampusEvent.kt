package com.inci.campuscompanion.model

data class CampusEvent(
    val id: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val location: String,
    val description: String
)
