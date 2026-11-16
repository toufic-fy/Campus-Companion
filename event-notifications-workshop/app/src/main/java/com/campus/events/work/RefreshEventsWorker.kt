package com.campus.events.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.campus.events.appContainer
import com.campus.events.notifications.NotificationDecision
import com.campus.events.notifications.NotificationPolicy
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Worker -> Repository (fetch, map, compare, write cache) -> Policy may notify.
 * The cache is always updated; a notification is posted only when the user preference,
 * the system permission AND a meaningful change all allow it.
 */
class RefreshEventsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val c = applicationContext.appContainer

        val result = try {
            c.eventsRepository.refresh()
        } catch (e: IOException) {
            Log.w(TAG, "Refresh failed, will retry", e)
            return Result.retry()
        }

        val settings = c.notificationSettings.settings.first()
        val decision = NotificationPolicy.decide(
            eventUpdatesEnabled = settings.eventUpdatesEnabled,
            permissionGranted = c.permissionChecker.isGranted(),
            newEvents = result.newEvents,
        )

        val outcome = when (decision) {
            is NotificationDecision.NotifyOne -> {
                c.notificationHelper.show(decision.event)
                "Posted a notification for \"${decision.event.title}\"."
            }
            is NotificationDecision.NotifySummary -> {
                c.notificationHelper.showSummary(decision.events)
                "Posted one notification for ${decision.events.size} new events."
            }
            is NotificationDecision.Skip ->
                if (result.wasFirstSync) "First sync: cache filled, nothing to announce."
                else decision.reason.message
        }

        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
        c.notificationSettings.setLastRefreshOutcome("$time — $outcome")
        Log.i(TAG, outcome)
        return Result.success()
    }

    companion object {
        private const val TAG = "RefreshEventsWorker"
        private const val PERIODIC_WORK = "refresh-events-periodic"
        private const val ONE_TIME_WORK = "refresh-events-now"

        /** Deferrable background refresh; WorkManager is not an exact reminder clock. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshEventsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Run the same refresh flow immediately (used by the demo controls). */
        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshEventsWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
