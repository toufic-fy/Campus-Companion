package com.inci.campuscompanion

import com.inci.campuscompanion.data.EventsRepository
import com.inci.campuscompanion.data.CampusApi
import com.inci.campuscompanion.model.EventDto
import com.inci.campuscompanion.data.EventsRemoteDataSource
import kotlinx.coroutines.test.runTest
import com.google.gson.JsonSyntaxException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

class EventsRepositoryTest {
    private val gson = Gson()

    @Test
    fun apiFieldsMapToUiDataAndExtraFieldsAreIgnored() = runTest {
        val events = repositoryFor(
            """[{"id":"1","name":"Campus meetup","startingDate":"2026-10-20T12:00:00Z",
                "address":"Main hall","avatar":"unused","description":"unused"}]"""
        ).getEvents()

        assertEquals("1", events.single().id)
        assertEquals("Campus meetup", events.single().title)
        assertEquals("2026-10-20T12:00:00Z", events.single().timeLabel)
        assertEquals("Main hall", events.single().locationLabel)
    }

    @Test
    fun missingAndNullLocationsUseTba() = runTest {
        val events = repositoryFor(
            """[{"id":"1","name":"First","startingDate":"Today"},
                {"id":"2","name":"Second","startingDate":"Tomorrow","address":null}]"""
        ).getEvents()
        assertEquals(listOf("TBA", "TBA"), events.map { it.locationLabel })
    }

    @Test
    fun emptyJsonArrayStaysEmpty() = runTest {
        assertEquals(emptyList<Any>(), repositoryFor("[]").getEvents())
    }

    @Test(expected = JsonSyntaxException::class)
    fun malformedJsonIsADataFailure() = runTest {
        repositoryFor("{invalid json").getEvents()
        Unit
    }

    private fun repositoryFor(body: String): EventsRepository {
        val api = object : CampusApi {
            override suspend fun getEvents(): List<EventDto> = gson.fromJson(body, object : TypeToken<List<EventDto>>() {}.type)
        }
        return EventsRepository(EventsRemoteDataSource(api))
    }
}
