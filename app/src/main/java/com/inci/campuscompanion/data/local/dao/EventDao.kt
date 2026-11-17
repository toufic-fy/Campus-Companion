package com.inci.campuscompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.inci.campuscompanion.data.local.entity.EventEntity
import com.inci.campuscompanion.data.local.entity.EventsRefreshState
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EventDao {
    @Query("SELECT * FROM events ORDER BY startsAt, id")
    abstract fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    abstract suspend fun getEvents(): List<EventEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM events_refresh_state WHERE id = 1)")
    abstract suspend fun hasRefreshed(): Boolean

    @Query("DELETE FROM events")
    protected abstract suspend fun clearEvents()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertEvents(events: List<EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun markRefreshed(state: EventsRefreshState)

    @Transaction
    open suspend fun replaceEvents(events: List<EventEntity>) {
        clearEvents()
        insertEvents(events)
        markRefreshed(EventsRefreshState())
    }
}
