
package com.inci.campuscompanion


import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.ui.MainViewModel
import com.inci.campuscompanion.ui.events.EventsScreen
import com.inci.campuscompanion.ui.events.EventsViewModel


@Composable
fun CampusApp(
    viewModel: MainViewModel,
    eventsViewModel: EventsViewModel
) {
    val navController = rememberNavController()
    AppNavHost(navController, viewModel, eventsViewModel)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    eventsViewModel: EventsViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val eventsState by eventsViewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = "tasks",
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
    ) {
        composable("tasks") {
            TaskScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                tasks = tasks,
                onToggleTask = { viewModel.toggleTaskCompletion(it) },
                onAddTask = { title, description ->
                    viewModel.upsertTask(title, description)
                },
                onOpenEvents = {
                    navController.navigate("events")
                }
            )
        }


        composable("events") {
            Scaffold(
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
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                EventsScreen(
                    uiState = eventsState,
                    onRetry = {
                        eventsViewModel.refreshEvents()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }

    }
}
