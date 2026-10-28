package com.inci.campuscompanion.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    @SerialName("starts_at") val startsAt: String,
    val location: String? = null
)