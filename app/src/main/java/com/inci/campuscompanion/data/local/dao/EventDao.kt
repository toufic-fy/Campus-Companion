package com.inci.campuscompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.inci.campuscompanion.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EventDao {
    @Query("SELECT * FROM events ORDER BY startsAt")
    abstract fun observeEvents(): Flow<List<EventEntity>>

    @Upsert
    abstract suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM events")
    abstract suspend fun clear()

    @Transaction
    open suspend fun replaceAll(events: List<EventEntity>) {
        clear()
        upsertAll(events)
    }
}
