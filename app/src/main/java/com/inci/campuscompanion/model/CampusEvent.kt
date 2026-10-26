package com.inci.campuscompanion.model

data class CampusEvent(
    val id: String,
    val title: String,
    val description: String?,
    val date: String,
    val location: String?
)
