package com.inci.campuscompanion.data

import android.util.Log
import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.model.CampusEvent

class EventsRepository(
    private val api: CampusApi,
    private val eventDao: EventDao? = null
) {
    private val TAG = "EventNotification"

    suspend fun getEvents(page: Int, limit: Int = 5): List<CampusEvent> {
        return api.getEvents(page = page, limit = limit).mapIndexed { index, dto ->
            val formattedDate = dto.date?.let {
                if (it.contains("T")) it.substringBefore("T") else it
            } ?: "Date TBA"

            CampusEvent(
                id = dto.id ?: "${page}_$index",
                title = dto.title ?: "Untitled Event",
                description = dto.description,
                date = formattedDate,
                location = dto.location
            )
        }
    }

    suspend fun deleteOneCachedEvent(): Boolean {
        return if (eventDao != null) {
            val deletedCount = eventDao.deleteOneEvent()
            Log.d(TAG, "deleteOneCachedEvent called. Rows deleted: $deletedCount")
            deletedCount > 0
        } else {
            false
        }
    }

    /**
     * Fetches remote events from API, compares with Room cached events,
     * detects newly added or modified events, updates Room cache,
     * and returns only the list of updated/new CampusEvent objects.
     */
    suspend fun refreshAndDetectUpdates(page: Int = 1, limit: Int = 10): List<CampusEvent> {
        val fetchedEvents = getEvents(page = page, limit = limit)
        if (eventDao == null) {
            Log.d(TAG, "eventDao is null; returning ${fetchedEvents.size} fetched events.")
            return fetchedEvents
        }

        val cachedMap = eventDao.getAllEvents().associateBy { it.id }
        val newlyPublishedEvents = mutableListOf<CampusEvent>()
        val entitiesToCache = mutableListOf<EventEntity>()

        for (event in fetchedEvents) {
            val cached = cachedMap[event.id]
            val isNewOrModified = cached == null ||
                    cached.title != event.title ||
                    cached.description != event.description ||
                    cached.date != event.date ||
                    cached.location != event.location

            if (isNewOrModified) {
                newlyPublishedEvents.add(event)
            }

            entitiesToCache.add(
                EventEntity(
                    id = event.id,
                    title = event.title,
                    description = event.description,
                    date = event.date,
                    location = event.location
                )
            )
        }

        Log.d(
            TAG,
            "refreshAndDetectUpdates: Fetched ${fetchedEvents.size} events, Cache size was ${cachedMap.size}. New/modified events found: ${newlyPublishedEvents.size}"
        )

        if (entitiesToCache.isNotEmpty()) {
            eventDao.upsertEvents(entitiesToCache)
        }

        return newlyPublishedEvents
    }
}
