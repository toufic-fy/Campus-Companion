package com.inci.campuscompanion.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EventDto(
    val id: String,
    @SerializedName("name") val title: String,
    val description: String,
    @SerializedName("startingDate") val startsAt: String,
    @SerializedName("endingDate") val endsAt: String? = null,
    @SerializedName("address") val location: String? = null
)
