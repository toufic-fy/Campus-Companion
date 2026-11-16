
package com.inci.campuscompanion.data

import com.inci.campuscompanion.model.CampusEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the JSON from the class mock API. Its keys are "id" (a string),
// "name" and "description"; other keys (dates, address, avatar) are ignored.
@Serializable
data class EventDto(
    val id: String,
    @SerialName("name")
    val title: String,
    val description: String = ""
) {
    fun toCampusEvent(): CampusEvent {
        return CampusEvent(
            id = id.toInt(),
            title = title,
            description = description
        )
    }
}
