package com.inci.campuscompanion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.TaskRepository
import com.inci.campuscompanion.data.UserPreferencesRepository
import com.inci.campuscompanion.data.dataStore
import com.inci.campuscompanion.model.Task
import com.inci.campuscompanion.ui.EventsScreen
import com.inci.campuscompanion.ui.MainViewModel
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import com.inci.campuscompanion.util.NotificationHelper

private object AppRoutes {
    const val TASKS = "tasks"
    const val EVENTS = "events"
}

class MainActivity : ComponentActivity() {

    private var destinationFromIntent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(applicationContext)
        handleDeepLink(intent)

        val database = AppDatabase.getDatabase(applicationContext)
        val taskRepository = TaskRepository(database.taskDao())
        val preferencesRepository = UserPreferencesRepository(dataStore)

        val viewModel: MainViewModel by viewModels {
            MainViewModel.Factory(preferencesRepository, taskRepository)
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val isEventNotificationsEnabled by viewModel.isEventNotificationsEnabled.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()
            val context = LocalContext.current

            CampusCompanionTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                LaunchedEffect(destinationFromIntent) {
                    if (destinationFromIntent == AppRoutes.EVENTS) {
                        navController.navigate(AppRoutes.EVENTS) {
                            launchSingleTop = true
                        }
                        destinationFromIntent = null
                    }
                }

                AppNavigation(
                    navController = navController,
                    isDarkMode = isDarkMode,
                    isEventNotificationsEnabled = isEventNotificationsEnabled,
                    tasks = tasks,
                    onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                    onToggleEventNotifications = { viewModel.toggleEventNotifications(it) },
                    onToggleTask = { viewModel.toggleTaskCompletion(it) },
                    onAddTask = { title, description ->
                        viewModel.upsertTask(title, description)
                    },
                    onSendTestNotification = { viewModel.sendTestNotification(context) },
                    onDeleteOneCachedEvent = { viewModel.deleteOneCachedEvent(context) },
                    onRunRefreshNow = { viewModel.runRefreshNow(context) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        if ((data?.scheme == "campus" && data.host == "events") ||
            intent?.getStringExtra("destination") == "events"
        ) {
            destinationFromIntent = AppRoutes.EVENTS
        }
    }
}

/** NavHost starts on Tasks and manages the back stack. */
@Composable
fun AppNavigation(
    navController: NavHostController,
    isDarkMode: Boolean,
    isEventNotificationsEnabled: Boolean,
    tasks: List<Task>,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleEventNotifications: (Boolean) -> Unit,
    onToggleTask: (Task) -> Unit,
    onAddTask: (String, String) -> Unit,
    onSendTestNotification: () -> Unit = {},
    onDeleteOneCachedEvent: () -> Unit = {},
    onRunRefreshNow: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = AppRoutes.TASKS) {
        composable(AppRoutes.TASKS) {
            TaskScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                isEventNotificationsEnabled = isEventNotificationsEnabled,
                onToggleEventNotifications = onToggleEventNotifications,
                tasks = tasks,
                onToggleTask = onToggleTask,
                onAddTask = onAddTask,
                onOpenEvents = { navController.navigate(AppRoutes.EVENTS) },
                onSendTestNotification = onSendTestNotification,
                onDeleteOneCachedEvent = onDeleteOneCachedEvent,
                onRunRefreshNow = onRunRefreshNow
            )
        }
        composable(AppRoutes.EVENTS) {
            EventsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun TaskScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    isEventNotificationsEnabled: Boolean,
    onToggleEventNotifications: (Boolean) -> Unit,
    tasks: List<Task>,
    onToggleTask: (Task) -> Unit,
    onAddTask: (String, String) -> Unit,
    onOpenEvents: () -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    onDeleteOneCachedEvent: () -> Unit = {},
    onRunRefreshNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val hasSystemPermission: Boolean = remember(context) {
        NotificationHelper.hasNotificationPermission(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleEventNotifications(true)
        } else {
            onToggleEventNotifications(false)
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    Button(
                        onClick = onOpenEvents,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Events")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddDialog = true }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dark Mode toggle
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

            // Event Update Notifications toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Event Update Notifications",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Notify when campus events are created or updated",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEventNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleEventNotifications(true)
                                }
                            } else {
                                onToggleEventNotifications(false)
                            }
                        }
                    )
                }

                if (isEventNotificationsEnabled && !hasSystemPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "System notification permission is currently disabled in Android settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider()

            // DEBUG / TESTING SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Debug / Testing Tools",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSendTestNotification,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send test notification", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onDeleteOneCachedEvent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete 1 cached event", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onRunRefreshNow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Run refresh now (WorkManager)", style = MaterialTheme.typography.labelSmall)
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
