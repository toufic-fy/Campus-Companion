package com.inci.campuscompanion

import com.inci.campuscompanion.data.CampusApi
import com.inci.campuscompanion.data.EventDto
import com.inci.campuscompanion.data.EventsRemoteDataSource
import com.inci.campuscompanion.data.EventsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewEventsDetectionTest {
    private val cache = MemoryEventsDao()
    private var ids = listOf("1", "2", "3")
    private val repo = EventsRepository(
        EventsRemoteDataSource(object : CampusApi {
            override suspend fun getEvents(page: Int, limit: Int) =
                ids.map { EventDto(it, "Event $it", "date", "Campus") }
        }), cache
    )

    @Test fun firstRefreshIsNotReportedAsNew() = runBlocking {
        assertTrue(repo.refreshEvents().isEmpty())
    }

    @Test fun laterRefreshReturnsOnlyUnseenEvents() = runBlocking {
        repo.refreshEvents()
        ids = listOf("4", "1", "2", "5")
        assertEquals(listOf("4", "5"), repo.refreshEvents().map { it.id })
    }

    @Test fun unchangedFeedReturnsNothing() = runBlocking {
        repo.refreshEvents()
        assertTrue(repo.refreshEvents().isEmpty())
    }
}
