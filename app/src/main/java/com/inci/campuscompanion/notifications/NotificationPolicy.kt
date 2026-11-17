package com.inci.campuscompanion.notifications

/** First successful refresh establishes a baseline; only later new IDs notify. */
fun <T> newlyPublishedEvents(
    previousIds: Set<String>,
    refreshedEvents: List<T>,
    hasRefreshed: Boolean,
    id: (T) -> String,
): List<T> = if (!hasRefreshed) emptyList() else
    refreshedEvents.distinctBy(id).filter { id(it) !in previousIds }

fun canNotify(eventUpdatesEnabled: Boolean, systemAllowsNotifications: Boolean): Boolean =
    eventUpdatesEnabled && systemAllowsNotifications
