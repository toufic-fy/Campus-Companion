package com.inci.campuscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.TaskRepository
import com.inci.campuscompanion.data.UserPreferencesRepository
import com.inci.campuscompanion.data.dataStore
import com.inci.campuscompanion.model.Task
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.ui.EventsUiState
import com.inci.campuscompanion.ui.EventsViewModel
import com.inci.campuscompanion.ui.EventsScreen
import com.inci.campuscompanion.ui.MainViewModel
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val taskRepository = TaskRepository(database.taskDao())
        val preferencesRepository = UserPreferencesRepository(dataStore)

        val viewModel: MainViewModel by viewModels {
            MainViewModel.Factory(preferencesRepository, taskRepository)
        }

        val eventsViewModel: EventsViewModel by viewModels { EventsViewModel.Factory }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()

            val eventsState by eventsViewModel.state.collectAsStateWithLifecycle()

            CampusCompanionTheme(darkTheme = isDarkMode) {
                CampusCompanionApp(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = viewModel::toggleDarkMode,
                    tasks = tasks,
                    onToggleTask = viewModel::toggleTaskCompletion,
                    onAddTask = { title, description -> viewModel.upsertTask(title, description) },
                    eventsState = eventsState,
                    onRefreshEvents = eventsViewModel::refresh
                )
            }
        }
    }
}

object Routes {
    const val TASKS = "tasks"
    const val EVENTS = "events"
}

// The app root owns navigation. Screens expose callbacks and do not receive NavController.
@Composable
fun CampusCompanionApp(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    tasks: List<Task>,
    onToggleTask: (Task) -> Unit,
    onAddTask: (String, String) -> Unit,
    eventsState: EventsUiState,
    onRefreshEvents: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.TASKS) {
        composable(Routes.TASKS) {
            Scaffold { padding ->
                TaskScreen(
                    isDarkMode, onToggleDarkMode, tasks, onToggleTask, onAddTask,
                    modifier = Modifier.padding(padding),
                    onOpenEvents = { navController.navigate(Routes.EVENTS) { launchSingleTop = true } }
                )
            }
        }
        composable(Routes.EVENTS) {
            EventsScreen(eventsState, onRefreshEvents, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun TaskScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    tasks: List<Task>,
    onToggleTask: (Task) -> Unit,
    onAddTask: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenEvents: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(onClick = onOpenEvents) { Text("Open Events") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onToggleDarkMode
                )
            }

            HorizontalDivider()

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks yet. Tap + to add one!",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                TaskList(
                    tasks = tasks,
                    onToggleTask = onToggleTask,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }

        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, description ->
                    onAddTask(title, description)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = task.title,
                maxLines = 1,
                minLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.description,
                maxLines = 2,
                minLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
fun TaskList(
    tasks: List<Task>,
    onToggleTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(
            items = tasks,
            key = { it.id }
        ) { task ->
            TaskRow(
                task = task,
                onToggle = { onToggleTask(task) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TaskPreview() {
    CampusCompanionTheme(darkTheme = false) {
        TaskScreen(
            isDarkMode = false,
            onToggleDarkMode = {},
            tasks = listOf(
                Task(1, "Task 1", "Description 1", false),
                Task(2, "Task 2", "Description 2", true)
            ),
            onToggleTask = {},
            onAddTask = { _, _ -> }
        )
    }
}
