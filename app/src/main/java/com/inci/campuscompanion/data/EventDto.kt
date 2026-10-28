
package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.CampusEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: Int,
    val title: String,
    @SerialName("body")
    val description: String
) {
    fun toCampusEvent(): CampusEvent {
        return CampusEvent(
            id = id,
            title = title.replaceFirstChar { it.uppercase() },
            description = description
        )
    }
}
