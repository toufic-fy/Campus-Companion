package com.inci.campuscompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.inci.campuscompanion.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startsAt")
    fun getEvents(): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Transaction
    suspend fun replaceEvents(events: List<EventEntity>) {
        deleteAllEvents()
        upsertEvents(events)
    }
}
