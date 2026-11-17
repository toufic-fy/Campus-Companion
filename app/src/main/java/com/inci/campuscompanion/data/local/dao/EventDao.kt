package com.inci.campuscompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.inci.campuscompanion.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startsAt ASC")
    fun observeEvents(): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    // The cache mirrors the latest successful response, so events removed remotely disappear too.
    @Transaction
    suspend fun replaceAll(events: List<EventEntity>) {
        deleteAll()
        upsertEvents(events)
    }
}
