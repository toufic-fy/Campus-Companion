package com.inci.campuscompanion

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.worker.RefreshEventsWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class WorkshopWorkManagerTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun shell(command: String) = instrumentation.uiAutomation.executeShellCommand(command).use {
        FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
    }
    private fun connected() {
        shell("cmd connectivity airplane-mode disable")
        shell("svc wifi enable")
        shell("svc data enable")
    }
    private fun await(timeoutMs: Long, predicate: () -> Boolean) {
        val end = android.os.SystemClock.elapsedRealtime() + timeoutMs
        while (!predicate()) {
            if (android.os.SystemClock.elapsedRealtime() >= end) fail("WorkManager condition timed out")
            Thread.sleep(250)
        }
    }

    @Test fun offlineRefreshIsUniqueAndCompletesAfterNetworkReturns(): Unit = runBlocking {
        val context = instrumentation.targetContext
        val manager = WorkManager.getInstance(context)
        fun work() = manager.getWorkInfosForUniqueWork(RefreshEventsWorker.MANUAL_NAME).get(10, TimeUnit.SECONDS)
        try {
            manager.cancelUniqueWork(RefreshEventsWorker.MANUAL_NAME).result.get(10, TimeUnit.SECONDS)
            shell("cmd connectivity airplane-mode enable")
            shell("svc wifi disable")
            shell("svc data disable")
            Thread.sleep(3_000)
            repeat(5) { RefreshEventsWorker.refreshNow(context) }
            await(15_000) { work().count { !it.state.isFinished } == 1 }
            val pending = work().single { !it.state.isFinished }
            assertEquals(WorkInfo.State.ENQUEUED, pending.state)
            Log.i("CampusExercise", "workshop-workmanager-offline: five taps; one unique ENQUEUED request while offline")
            connected()
            await(120_000) { work().any { it.id == pending.id && it.state.isFinished } }
            assertEquals(WorkInfo.State.SUCCEEDED, work().single { it.id == pending.id }.state)
            assertTrue(AppDatabase.getDatabase(context).eventDao().observeEvents().first().isNotEmpty())
            Log.i("CampusExercise", "workshop-workmanager-reconnected: same request succeeded automatically; Room has live events")
        } finally {
            connected()
            manager.cancelUniqueWork(RefreshEventsWorker.MANUAL_NAME).result.get(10, TimeUnit.SECONDS)
        }
    }
}
