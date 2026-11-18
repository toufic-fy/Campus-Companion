package com.inci.campuscompanion.data.remote

import kotlinx.serialization.Serializable

// Names match the supplied MockAPI response. Unknown fields (e.g. avatar) are ignored.
@Serializable
data class EventDto(
    val id: String,
    val name: String,
    val startingDate: String,
    val endingDate: String,
    val address: String? = null,
    val description: String = ""
)
