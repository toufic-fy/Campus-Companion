package com.inci.campuscompanion

import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.ui.view_model.EventsUiState
import com.inci.campuscompanion.ui.view_model.RefreshStatus
import com.inci.campuscompanion.ui.view_model.eventsUiStateOf
import com.inci.campuscompanion.ui.view_model.refreshStatusOf
import com.inci.campuscompanion.worker.RefreshEventWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class EventsRefreshTest {
    private val event = CampusEvent(
        id = "1",
        title = "Career Fair",
        description = "",
        startsAt = "2026-11-20T09:00:00Z",
        timeLabel = "Nov 20, 2026 • 9:00 AM"
    )

    @Test
    fun workStatesBecomeReadableLabels() {
        assertEquals(RefreshStatus.WaitingForNetwork, refreshStatusOf(WorkInfo.State.ENQUEUED))
        assertEquals(RefreshStatus.Refreshing, refreshStatusOf(WorkInfo.State.RUNNING))
        assertEquals(RefreshStatus.UpToDate, refreshStatusOf(WorkInfo.State.SUCCEEDED))
        assertEquals(RefreshStatus.Failed, refreshStatusOf(WorkInfo.State.FAILED))
        assertNull(refreshStatusOf(null))
    }

    @Test
    fun cachedEventsStayVisibleWhileOfflineOrFailed() {
        assertEquals(EventsUiState.Success(listOf(event)), eventsUiStateOf(listOf(event), RefreshStatus.WaitingForNetwork))
        assertEquals(EventsUiState.Success(listOf(event)), eventsUiStateOf(listOf(event), RefreshStatus.Failed))
    }

    @Test
    fun emptyCacheMeansLoadingUntilARefreshFinishes() {
        assertEquals(EventsUiState.Loading, eventsUiStateOf(emptyList(), RefreshStatus.WaitingForNetwork))
        assertEquals(EventsUiState.Success(emptyList()), eventsUiStateOf(emptyList(), RefreshStatus.UpToDate))
        assertTrue(eventsUiStateOf(emptyList(), RefreshStatus.Failed) is EventsUiState.Error)
    }

    @Test
    fun networkFailuresAreRetriedAndBadDataFails() {
        assertEquals(ListenableWorker.Result.retry(), RefreshEventWorker.resultFor(IOException("offline")))
        assertEquals(ListenableWorker.Result.failure(), RefreshEventWorker.resultFor(IllegalStateException("bad data")))
    }
}
