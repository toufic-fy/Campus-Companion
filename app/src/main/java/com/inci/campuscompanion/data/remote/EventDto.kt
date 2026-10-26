package com.inci.campuscompanion.data.remote

import com.google.gson.annotations.SerializedName

data class EventDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName(value = "title", alternate = ["name"])
    val title: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName(value = "date", alternate = ["startingDate"])
    val date: String? = null,

    @SerializedName(value = "location", alternate = ["address"])
    val location: String? = null
)
