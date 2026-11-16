package com.campus.events.notifications

import com.campus.events.data.Event
import org.junit.Assert.assertEquals
import org.junit.Test

/** The user states from the workshop's test slide. */
class NotificationPolicyTest {

    private val event = Event("evt-new-1", "Hackathon kickoff", "Innovation lab", "Fri 18:00", "")
    private val other = Event("evt-new-2", "Movie night", "Sports field", "Sat 20:30", "")

    @Test
    fun permissionDenied_noNotification() {
        val d = NotificationPolicy.decide(eventUpdatesEnabled = true, permissionGranted = false, listOf(event))
        assertEquals(NotificationDecision.Skip(SkipReason.PERMISSION_DENIED), d)
    }

    @Test
    fun preferenceOff_noNotificationEvenIfPermissionGranted() {
        val d = NotificationPolicy.decide(eventUpdatesEnabled = false, permissionGranted = true, listOf(event))
        assertEquals(NotificationDecision.Skip(SkipReason.PREFERENCE_OFF), d)
    }

    @Test
    fun noNewEvents_noNotification() {
        val d = NotificationPolicy.decide(eventUpdatesEnabled = true, permissionGranted = true, emptyList())
        assertEquals(NotificationDecision.Skip(SkipReason.NO_NEW_EVENTS), d)
    }

    @Test
    fun newEventAndAllowed_oneNotification() {
        val d = NotificationPolicy.decide(eventUpdatesEnabled = true, permissionGranted = true, listOf(event))
        assertEquals(NotificationDecision.NotifyOne(event), d)
    }

    @Test
    fun severalNewEvents_oneSummaryNotification() {
        val d = NotificationPolicy.decide(eventUpdatesEnabled = true, permissionGranted = true, listOf(event, other))
        assertEquals(NotificationDecision.NotifySummary(listOf(event, other)), d)
    }

    @Test
    fun exitQuestion_threeNewEvents_preferenceOn_permissionDenied_doesNotNotify() {
        val three = listOf(event, other, event.copy(id = "evt-new-3"))
        val d = NotificationPolicy.decide(eventUpdatesEnabled = true, permissionGranted = false, three)
        assertEquals(NotificationDecision.Skip(SkipReason.PERMISSION_DENIED), d)
    }
}
