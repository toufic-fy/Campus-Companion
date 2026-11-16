package com.inci.campuscompanion.navigation

// One string per destination in the NavHost.
object Routes {
    const val TASKS = "tasks"
    const val EVENTS = "events"

    // Used by the event notification to open the Events destination.
    const val EVENTS_DEEP_LINK = "campuscompanion://events"
}
