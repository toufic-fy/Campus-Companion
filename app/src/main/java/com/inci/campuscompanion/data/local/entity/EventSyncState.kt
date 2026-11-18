package com.inci.campuscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_sync_state")
data class EventSyncState(@PrimaryKey val id: Int = 1)

@Entity(tableName = "seen_events")
data class SeenEvent(@PrimaryKey val id: String)
