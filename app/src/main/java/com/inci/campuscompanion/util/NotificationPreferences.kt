
package com.inci.campuscompanion.util

import android.content.Context

object NotificationPreferences {

    private const val PREFS_NAME = "notification_preferences"
    private const val KEY_ENABLED = "notifications_enabled"

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
