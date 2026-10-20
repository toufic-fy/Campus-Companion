package com.inci.campuscompanion.data

import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: String,
    val name: String,
    val startingDate: String,
    val address: String? = null
)
