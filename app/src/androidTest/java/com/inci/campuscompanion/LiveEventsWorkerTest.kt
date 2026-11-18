package com.inci.campuscompanion

import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.worker.RefreshEventWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class LiveEventsWorkerTest {
    @Test fun suppliedEndpointRefreshesRoomThroughRealWorker() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = WorkManager.getInstance(context)
        val request = RefreshEventWorker.buildOneTimeWorkRequest()
        manager.enqueueUniqueWork("workshop-live-api-verification", ExistingWorkPolicy.KEEP, request).result.get()
        val result = withTimeout(90000) { manager.getWorkInfoByIdFlow(request.id).first { it?.state?.isFinished == true } }
        assertEquals(WorkInfo.State.SUCCEEDED, result!!.state)
        val cached = AppDatabase.getDatabase(context).eventDao().getEvents().first()
        assertTrue("The supplied live endpoint returned events", cached.isNotEmpty())
        println("LIVE_API_CACHED_EVENTS=${cached.size}")
    }
}
