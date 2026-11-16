package com.example.navigationworkshop

import kotlinx.coroutines.delay

class EventsRepository {

    suspend fun refreshEvents() {
        // Simulate fetching events from a server
        delay(2000)
    }
}