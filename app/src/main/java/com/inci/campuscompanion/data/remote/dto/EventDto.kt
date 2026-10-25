package com.inci.campuscompanion.data.remote.dto

import kotlinx.serialization.SerialName

data class EventDto(
    val id: String,
    @SerialName("title") val title: String,
    val description: String,
    @SerialName("startingDate") val startsAt: String,
    @SerialName("endingDate") val endsAt: String? = null,
    @SerialName("address") val location: String? = null
)
