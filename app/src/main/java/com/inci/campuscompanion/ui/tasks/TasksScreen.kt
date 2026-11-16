
package com.inci.campuscompanion.ui.tasks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inci.campuscompanion.model.Task
import com.inci.campuscompanion.notifications.NotificationHelper
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import com.inci.campuscompanion.ui.view_model.TasksViewModel

@Composable
fun TaskRoute(
    viewModel: TasksViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onNavigateEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    val notificationsEnabled by viewModel.areEventNotificationsEnabled
        .collectAsStateWithLifecycle()

    val context = LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.toggleEventNotifications(true)
            }
        }

    fun handleNotificationToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.toggleEventNotifications(false)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted =
                context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (permissionGranted) {
                viewModel.toggleEventNotifications(true)
            } else {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        } else {
            viewModel.toggleEventNotifications(true)
        }
    }

    TaskContent(
        isDarkMode = isDarkMode,
        onToggleDarkMode = onToggleDarkMode,
        notificationsEnabled = notificationsEnabled,
        onToggleNotifications = ::handleNotificationToggle,
        tasks = tasks,
        onToggleTask = viewModel::toggleTaskCompletion,
        onAddTask = viewModel::upsertTask,
        onNavigateEvents = onNavigateEvents,
        modifier = modifier
    )
}

@Composable
fun TaskContent(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    tasks: List<Task>,
    onToggleTask: (Task) -> Unit,
    onAddTask: (String, String) -> Unit,
    onNavigateEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Event notifications",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )
                }

                Button(onClick = onNavigateEvents) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "Events"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Events")
                }

                Button(
                    onClick = {
                        NotificationHelper.showTestNotification(context)
                    }
                ) {
                    Text("Test Notification")
                }
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
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task"
            )
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
        TaskContent(
            isDarkMode = false,
            onToggleDarkMode = {},
            notificationsEnabled = false,
            onToggleNotifications = {},
            tasks = listOf(
                Task(1, "Task 1", "Description 1", false),
                Task(2, "Task 2", "Description 2", true)
            ),
            onToggleTask = {},
            onAddTask = { _, _ -> },
            onNavigateEvents = {}
        )
    }
}
