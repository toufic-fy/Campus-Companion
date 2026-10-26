package com.inci.campuscompanion.data.remote

// Exact shape of one JSON object returned by the API
data class EventDto(
    val id: String,
    val name: String?,
    val avatar: String?,
    val description: String?,
    val address: String?,
    val startingDate: String?,
    val endingDate: String?,
    val createdAt: String?
)
