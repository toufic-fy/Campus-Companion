package com.inci.campuscompanion.data.remote

import com.google.gson.annotations.SerializedName

data class EventDto(
    val id: String,

    @SerializedName("name")
    val title: String,

    @SerializedName("startingDate")
    val startsAt: String,

    @SerializedName("address")
    val location: String? ,

    val description: String?
)
