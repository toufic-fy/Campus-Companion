package com.inci.campuscompanion.data.remote.dto

import kotlinx.serialization.SerialName

data class EventDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("starts_at") val startsAt: String,
    val location: String? = null
)