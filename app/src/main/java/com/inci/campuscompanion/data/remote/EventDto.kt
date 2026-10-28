package com.inci.campuscompanion.data.remote

// Mirrors one object of GET events/event field for field; nullable so bad data can't crash Gson mapping.
data class EventDto(
    val id: String?,
    val createdAt: String?,
    val name: String?,
    val avatar: String?,
    val startingDate: String?,
    val endingDate: String?,
    val description: String?,
    val address: String?
)
