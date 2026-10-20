package com.inci.campuscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.inci.campuscompanion.data.remote.CampusApi
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.ui.EventsScreen
import com.inci.campuscompanion.ui.EventsViewModel
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val campusApi = CampusApi.create()
        val eventsRepository = EventsRepository(campusApi)

        val viewModel: EventsViewModel by viewModels {
            EventsViewModel.Factory(eventsRepository)
        }

        setContent {
            CampusCompanionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EventsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
