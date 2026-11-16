package com.inci.campuscompanion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationConditionTest {

    private fun shouldPostNotification(userEnabled: Boolean, systemPermissionGranted: Boolean): Boolean {
        return userEnabled && systemPermissionGranted
    }

    @Test
    fun `notification posted when BOTH user enabled AND system permission granted`() {
        val result = shouldPostNotification(userEnabled = true, systemPermissionGranted = true)
        assertTrue("Notification must be allowed when both conditions are satisfied", result)
    }

    @Test
    fun `no notification when user disabled preference`() {
        val result = shouldPostNotification(userEnabled = false, systemPermissionGranted = true)
        assertFalse("Notification must NOT be allowed when user disabled preference", result)
    }

    @Test
    fun `no notification when system permission denied`() {
        val result = shouldPostNotification(userEnabled = true, systemPermissionGranted = false)
        assertFalse("Notification must NOT be allowed when system permission is denied", result)
    }

    @Test
    fun `no notification when BOTH user disabled AND system permission denied`() {
        val result = shouldPostNotification(userEnabled = false, systemPermissionGranted = false)
        assertFalse("Notification must NOT be allowed when both conditions are false", result)
    }
}
