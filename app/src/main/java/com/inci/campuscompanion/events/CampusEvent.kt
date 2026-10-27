package com.inci.campuscompanion.events

data class CampusEvent(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val imageUrl: String?
)
