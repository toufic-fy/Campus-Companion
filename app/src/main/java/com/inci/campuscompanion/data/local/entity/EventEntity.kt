
package com.inci.campuscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.inci.campuscompanion.model.CampusEvent

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val startsAt: String,
    val endsAt: String?,
    val timeLabel: String,
    val locationLabel: String?
) {
    fun toDomain(): CampusEvent {
        return CampusEvent(
            id = id,
            title = title,
            description = description,
            startsAt = startsAt,
            endsAt = endsAt,
            timeLabel = timeLabel,
            locationLabel = locationLabel
        )
    }
}

fun CampusEvent.toEntity(): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        description = description,
        startsAt = startsAt,
        endsAt = endsAt,
        timeLabel = timeLabel,
        locationLabel = locationLabel
    )
}
