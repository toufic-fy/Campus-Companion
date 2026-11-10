
package com.inci.campuscompanion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.inci.campuscompanion.data.local.AppDatabase
import com.inci.campuscompanion.model.Task
import com.inci.campuscompanion.ui.tasks.TasksScreen
import com.inci.campuscompanion.ui.events.EventsScreen
import com.inci.campuscompanion.ui.view_model.CampusEventViewModel
import com.inci.campuscompanion.data.repository.EventsRepository
import com.inci.campuscompanion.data.remote.api.EventsRemoteDataSource
import com.inci.campuscompanion.data.remote.api.RetrofitClient
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    isDarkMode: Boolean,
    tasks: List<Task>,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleTask: (Task) -> Unit,
    onAddTask: (String, String) -> Unit
) {

    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = "tasks"
    ) {

        // TASKS SCREEN
        composable("tasks") {

            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->

                TasksScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    tasks = tasks,
                    onToggleTask = onToggleTask,
                    onAddTask = onAddTask,
                    onNavigateToEvents = {
                        navController.navigate("events")
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // EVENTS SCREEN
        composable("events") {

            val eventsViewModel: CampusEventViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {

                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(
                        modelClass: Class<T>
                    ): T {

                        val remoteDataSource =
                            EventsRemoteDataSource(
                                RetrofitClient.campusApi
                            )

                        val repository =
                            EventsRepository(
                                remoteDataSource,
                                AppDatabase.getDatabase(context).eventDao()
                            )


                        return CampusEventViewModel(
                            repository
                        ) as T
                    }
                }
            )

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Campus Events")
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->

                EventsScreen(
                    viewModel = eventsViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}
