package com.inci.campuscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.data.local.pref.UserPreferencesRepository
import com.inci.campuscompanion.data.local.pref.dataStore
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.data.repository.TaskRepository
import com.inci.campuscompanion.ui.navigation.AppNavHost
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import com.inci.campuscompanion.ui.view_model.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val taskRepository = TaskRepository(database.taskDao())
        val preferencesRepository = UserPreferencesRepository(dataStore)
        val eventsRepository = EventsRepository(EventsRemoteDataSource(RetrofitClient.campusApi))

        val viewModel: MainViewModel by viewModels {
            MainViewModel.Factory(preferencesRepository, taskRepository)
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            CampusCompanionTheme(darkTheme = isDarkMode) {
                CampusApp(
                    mainViewModel = viewModel,
                    eventsRepository = eventsRepository
                )
            }
        }
    }
}

@Composable
fun CampusApp(
    mainViewModel: MainViewModel,
    eventsRepository: EventsRepository
) {
    val navController = rememberNavController()
    AppNavHost(
        navController = navController,
        mainViewModel = mainViewModel,
        eventsRepository = eventsRepository
    )
}
