package com.inci.campuscompanion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var notificationUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) notificationUri = eventUri(intent)
        // The graph handles the link after its Tasks start entry exists.
        intent = Intent(intent).setData(null)
        setContent { CampusApp(notificationUri, onLinkConsumed = { notificationUri = null }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Consume the incoming URI without replacing the Activity’s original launch intent.
        notificationUri = eventUri(intent)
    }

    private fun eventUri(intent: Intent): Uri? = intent.data?.takeIf {
        intent.action == Intent.ACTION_VIEW && it.scheme == "campus" && it.host == "events" && it.pathSegments.size == 1
    }
}
