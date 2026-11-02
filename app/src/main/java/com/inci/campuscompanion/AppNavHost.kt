
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.inci.campuscompanion.model.Task
import com.inci.campuscompanion.ui.tasks.TasksScreen

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
    NavHost(
        navController = navController,
        startDestination = "tasks"
    ) {
        composable("tasks") {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                Text(
                    text = "Events Screen",
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
