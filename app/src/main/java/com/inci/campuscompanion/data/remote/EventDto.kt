package com.inci.campuscompanion.data.remote

import kotlinx.serialization.Serializable

// Names match the supplied MockAPI response, rather than the illustrative slide JSON.
@Serializable
data class EventDto(
    val id: String,
    val name: String,
    val startingDate: String,
    val endingDate: String? = null,
    val address: String? = null,
    val description: String = "",
    val avatar: String? = null,
    val createdAt: String? = null
)
