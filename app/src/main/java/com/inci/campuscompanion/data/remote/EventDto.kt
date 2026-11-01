package com.inci.campuscompanion.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: String,
    val name: String,
    @SerialName("startingDate") val startsAt: String,
    @SerialName("endingDate") val endsAt: String? = null,
    val description: String? = null,
    val address: String? = null
)
