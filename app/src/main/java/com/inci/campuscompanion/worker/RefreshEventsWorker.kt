package com.inci.campuscompanion.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.UserPreferencesRepository
import com.inci.campuscompanion.data.dataStore
import com.inci.campuscompanion.notifications.EventNotificationHelper
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

// Runs the Events refresh outside the screen, so it survives leaving Events
// or closing the app, and waits for a network connection (set in the request).
// The worker never touches the UI: it only saves to Room and returns a Result.
class RefreshEventsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = EventsRepository(
            eventDao = AppDatabase.getDatabase(applicationContext).eventDao()
        )

        val changedEvents = try {
            repository.refreshEvents()
        } catch (e: IOException) {
            // No connection / timeout: try again later with backoff.
            return Result.retry()
        } catch (e: HttpException) {
            // Server errors (5xx) may go away; other HTTP errors won't.
            return if (e.code() >= 500) Result.retry() else Result.failure()
        }

        // Checkpoint: notify only if BOTH the user's preference and the system allow it.
        // (EventNotificationHelper also re-checks the system permission before posting.)
        val userWantsUpdates = UserPreferencesRepository(applicationContext.dataStore)
            .eventNotificationsEnabled
            .first()
        if (changedEvents.isNotEmpty() && userWantsUpdates) {
            EventNotificationHelper(applicationContext).showEventUpdates(changedEvents)
        }

        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "refresh-campus-events"
    }
}
