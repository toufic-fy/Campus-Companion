package com.inci.campuscompanion.data.local

import android.content.Context
import com.inci.campuscompanion.data.local.entity.EventSyncState
import com.inci.campuscompanion.data.local.entity.SeenEvent
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inci.campuscompanion.data.local.dao.EventDao
import com.inci.campuscompanion.data.local.dao.TaskDao
import com.inci.campuscompanion.data.local.entity.EventEntity
import com.inci.campuscompanion.data.local.entity.TaskEntity

@Database(entities = [TaskEntity::class, EventEntity::class, EventSyncState::class, SeenEvent::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `events` (" +
                        "`id` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`startsAt` TEXT NOT NULL, " +
                        "`endsAt` TEXT, " +
                        "`timeLabel` TEXT NOT NULL, " +
                        "`locationLabel` TEXT, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS event_sync_state (id INTEGER NOT NULL PRIMARY KEY)")
                db.execSQL("CREATE TABLE IF NOT EXISTS seen_events (id TEXT NOT NULL PRIMARY KEY)")
                db.execSQL("INSERT OR IGNORE INTO seen_events SELECT id FROM events")
                db.execSQL("INSERT OR IGNORE INTO event_sync_state SELECT 1 WHERE EXISTS (SELECT 1 FROM events)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campus_companion_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
