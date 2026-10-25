package com.inci.campuscompanion.data.remote.dto

import kotlinx.serialization.SerialName

data class EventDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("startingDate") val startsAt: String,
    @SerialName("endingDate") val endsAt: String? = null,
    val location: String? = null
)
