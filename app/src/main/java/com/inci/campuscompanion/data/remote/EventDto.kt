package com.inci.campuscompanion.data.remote

data class EventDto(
    val id: String,
    val name: String,
    val startingDate: String,
    val endingDate: String,
    val description: String,
    val address: String? = null,
    val avatar: String? = null,
    val createdAt: String? = null
)