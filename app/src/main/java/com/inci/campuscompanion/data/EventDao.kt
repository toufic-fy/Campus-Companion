package com.inci.campuscompanion.data

import androidx.room.Dao
import androidx.room.Query
import com.inci.campuscompanion.data.repository.CampusEvent
@Dao
interface EventDao {
    @Suppress("AndroidUnresolvedRoomSqlReference")
    @Query("SELECT * FROM _events")
    suspend fun getAllOnce(): List<CampusEvent>
}