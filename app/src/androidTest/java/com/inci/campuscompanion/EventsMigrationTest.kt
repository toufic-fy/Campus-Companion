package com.inci.campuscompanion

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EventsMigrationTest {
    @Test fun upgradingVersionOnePreservesTasksAndCreatesUsableEventsCache() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "events-migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, isCompleted INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO tasks VALUES (1, 'Keep me', 'Existing task', 1)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        helper.writableDatabase
        helper.close()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            val task = database.taskDao().getTasks().first().single()
            assertEquals("Keep me", task.title)
            assertTrue(task.isCompleted)
            database.eventDao().replace(listOf(EventEntity("1", "Event", "Today", "Campus", "")))
            assertEquals("Event", database.eventDao().observeEvents().first().single().title)
            assertTrue(database.eventDao().syncState()!!.initialized)
            assertEquals("1", database.eventDao().seenEvents().single().id)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
