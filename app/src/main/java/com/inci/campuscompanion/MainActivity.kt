package com.inci.campuscompanion

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.TaskRepository
import com.inci.campuscompanion.data.UserPreferencesRepository
import com.inci.campuscompanion.data.dataStore
import com.inci.campuscompanion.model.Task
import com.inci.campuscompanion.ui.MainViewModel
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

object Routes {
    const val TASKS = "tasks"
    const val EVENTS = "events"
}

// Creates the channel, saves the preference, and posts notifications.
class EventNotifications(context: Context) {
    private val appContext = context.applicationContext

    private val preferences = appContext.getSharedPreferences(
        "event_notification_preferences",
        Context.MODE_PRIVATE
    )

    private val manager = appContext.getSystemService(
        Context.NOTIFICATION_SERVICE
    ) as NotificationManager

    var enabled: Boolean
        get() = preferences.getBoolean("enabled", false)
        set(value) {
            preferences.edit().putBoolean("enabled", value).apply()
            if (!value) {
                manager.cancel(UPDATE_NOTIFICATION_ID)
                manager.cancel(TEST_NOTIFICATION_ID)
            }
        }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates to campus events"
            }

            manager.createNotificationChannel(channel)
        }
    }

    fun canPostNotifications(): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            appContext.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            !manager.areNotificationsEnabled()
        ) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = manager.getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }

        return true
    }

    fun sendTest(): Boolean {
        return post(
            id = TEST_NOTIFICATION_ID,
            title = "Campus Companion",
            message = "Test notification. Tap to open Campus Events."
        )
    }

    // Compare successful results, including across app restarts.
    // The first successful result establishes the baseline silently.
    fun onEventsLoaded(events: List<CampusEvent>) {
        // Sort so a different server ordering does not cause a notification.
        val snapshot = Gson().toJson(
            events.sortedWith(
                compareBy(
                    { it.id },
                    { it.title },
                    { it.location },
                    { it.startTime },
                    { it.description }
                )
            )
        )

        val previous = preferences.getString("events_snapshot", null)

        preferences.edit()
            .putString("events_snapshot", snapshot)
            .apply()

        if (previous != null && previous != snapshot) {
            post(
                id = UPDATE_NOTIFICATION_ID,
                title = "Campus events updated",
                message = "The campus events list has changed. Tap to view it."
            )
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun post(id: Int, title: String, message: String): Boolean {
        createChannel()

        if (!enabled || !canPostNotifications()) {
            return false
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_EVENTS, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(appContext, CHANNEL_ID)
            } else {
                Notification.Builder(appContext)
            }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            manager.notify(id, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val CHANNEL_ID = "event_updates"
        const val EXTRA_OPEN_EVENTS = "open_events"
        private const val UPDATE_NOTIFICATION_ID = 2001
        private const val TEST_NOTIFICATION_ID = 2002
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var eventNotifications: EventNotifications

    private var notificationsEnabled by mutableStateOf(false)
    private var systemNotificationsAllowed by mutableStateOf(false)
    private var openEventsRequest by mutableIntStateOf(0)

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            systemNotificationsAllowed =
                eventNotifications.canPostNotifications()

            if (!granted) {
                Toast.makeText(
                    this,
                    "Event updates are selected, but Android permission is off. " +
                            "You can enable it in notification settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        eventNotifications = EventNotifications(applicationContext)
        eventNotifications.createChannel()

        notificationsEnabled = eventNotifications.enabled
        systemNotificationsAllowed =
            eventNotifications.canPostNotifications()

        handleNotificationIntent(intent)

        val database = AppDatabase.getDatabase(applicationContext)
        val taskRepository = TaskRepository(database.taskDao())
        val preferencesRepository = UserPreferencesRepository(dataStore)
        val eventsRepository = EventsRepository(NetworkModule.api)

        val viewModel: MainViewModel by viewModels {
            MainViewModel.Factory(
                preferencesRepository,
                taskRepository
            )
        }

        val eventsViewModel: EventsViewModel by viewModels {
            EventsViewModel.Factory(
                eventsRepository,
                eventNotifications
        val eventsRepository = EventsRepository(
            EventsRemoteDataSource(RetrofitClient.campusApi)
        )

        val tasksViewModel: TasksViewModel by viewModels {
            TasksViewModel.Factory(preferencesRepository, taskRepository)
        }

        val eventsViewModel: CampusEventViewModel by viewModels {
            CampusEventViewModel.Factory(
                eventsRepository = eventsRepository,
                workManager = WorkManager.getInstance(applicationContext)
            )
        }

        RefreshEventWorker.enqueueUniquePeriodicWork(applicationContext)

        setContent {
            val isDarkMode by
            viewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by
            viewModel.tasks.collectAsStateWithLifecycle()
            val eventsState by
            eventsViewModel.uiState.collectAsStateWithLifecycle()

            CampusCompanionTheme(darkTheme = isDarkMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AppNavigation(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = {
                            viewModel.toggleDarkMode(it)
                        },
                        tasks = tasks,
                        onToggleTask = {
                            viewModel.toggleTaskCompletion(it)
                        },
                        onAddTask = { title, description ->
                            viewModel.upsertTask(title, description)
                        },
                        eventsState = eventsState,
                        onRefreshEvents = {
                            eventsViewModel.refresh()
                        },
                        notificationsEnabled = notificationsEnabled,
                        systemNotificationsAllowed =
                            systemNotificationsAllowed,
                        onToggleNotifications =
                            ::toggleEventNotifications,
                        onTestNotification = {
                            val sent = eventNotifications.sendTest()

                            Toast.makeText(
                                this@MainActivity,
                                if (sent) {
                                    "Test notification sent"
                                } else {
                                    "Enable Event updates and allow notifications " +
                                            "in Android settings first."
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onOpenNotificationSettings =
                            ::openNotificationSettings,
                        openEventsRequest = openEventsRequest,
            val isDarkMode by tasksViewModel.isDarkMode.collectAsStateWithLifecycle()

            CampusCompanionTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CampusApp(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = tasksViewModel::toggleDarkMode,
                        tasksViewModel = tasksViewModel,
                        eventsViewModel = eventsViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::eventNotifications.isInitialized) {
            notificationsEnabled = eventNotifications.enabled
            systemNotificationsAllowed =
                eventNotifications.canPostNotifications()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (
            intent?.getBooleanExtra(
                EventNotifications.EXTRA_OPEN_EVENTS,
                false
            ) == true
        ) {
            openEventsRequest++
            intent.removeExtra(EventNotifications.EXTRA_OPEN_EVENTS)
        }
    }

    private fun toggleEventNotifications(enabled: Boolean) {
        eventNotifications.enabled = enabled
        notificationsEnabled = enabled

        // Request only when the user enables this feature.
        if (
            enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        systemNotificationsAllowed =
            eventNotifications.canPostNotifications()
    }

    private fun openNotificationSettings() {
        val settingsIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            } else {
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:$packageName")
                )
            }

        startActivity(settingsIntent)
    }
}

@Composable
fun AppNavigation(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    tasks: List<Task>,
    onToggleTask: (Task) -> Unit,
    onAddTask: (String, String) -> Unit,
    eventsState: EventsUiState,
    onRefreshEvents: () -> Unit,
    notificationsEnabled: Boolean,
    systemNotificationsAllowed: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onTestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    openEventsRequest: Int,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    LaunchedEffect(openEventsRequest) {
        if (openEventsRequest > 0) {
            navController.navigate(Routes.EVENTS) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.TASKS,
        modifier = modifier
    ) {
        composable(Routes.TASKS) {
            TaskScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                tasks = tasks,
                onToggleTask = onToggleTask,
                onAddTask = onAddTask,
                onOpenEvents = {
                    navController.navigate(Routes.EVENTS) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.EVENTS) {
            EventsScreen(
                state = eventsState,
                onRefresh = onRefreshEvents,
                notificationsEnabled = notificationsEnabled,
                systemNotificationsAllowed = systemNotificationsAllowed,
                onToggleNotifications = onToggleNotifications,
                onTestNotification = onTestNotification,
                onOpenNotificationSettings =
                    onOpenNotificationSettings
            )
        }
    }
}

// Events: model, network, repository

data class CampusEvent(
    val id: Int,
    val title: String,
    val location: String,
    val startTime: String,
    val description: String
)

data class EventDto(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("title") val title: String?,
    @field:SerializedName("location") val location: String?,
    @field:SerializedName("startTime") val startTime: String?,
    @field:SerializedName("description") val description: String?
)

interface CampusApi {
    @GET("events")
    suspend fun getEvents(): List<EventDto>
}

object NetworkModule {
    // Replace with your teacher's actual API URL.
    // The URL must end with "/".
    private const val BASE_URL = "https://example.com/api/"

    val api: CampusApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampusApi::class.java)
    }
}

class EventsRepository(private val api: CampusApi) {
    suspend fun getEvents(): Result<List<CampusEvent>> = try {
        Result.success(
            api.getEvents().mapNotNull { it.toDomain() }
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun EventDto.toDomain(): CampusEvent? {
    val safeId = id ?: return null
    val safeTitle = title?.takeIf { it.isNotBlank() } ?: return null

    return CampusEvent(
        id = safeId,
        title = safeTitle,
        location = location.orEmpty(),
        startTime = startTime.orEmpty(),
        description = description.orEmpty()
    )
}

sealed interface EventsUiState {
    data object Loading : EventsUiState

    data class Content(
        val events: List<CampusEvent>
    ) : EventsUiState

    data class Error(
        val message: String
    ) : EventsUiState
}

class EventsViewModel(
    private val repository: EventsRepository,
    private val notifications: EventNotifications
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<EventsUiState>(EventsUiState.Loading)

    val uiState: StateFlow<EventsUiState> =
        _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            _uiState.value = EventsUiState.Loading

            repository.getEvents().fold(
                onSuccess = { events ->
                    _uiState.value = EventsUiState.Content(events)

                    // Only successful, changed results can notify.
                    notifications.onEventsLoaded(events)
                },
                onFailure = {
                    _uiState.value =
                        EventsUiState.Error(it.toUserMessage())
                }
            )
        }
    }

    private fun Throwable.toUserMessage(): String =
        if (this is IOException) {
            "No connection. Check your internet and try again."
        } else {
            "Something went wrong while loading events."
        }

    class Factory(
        private val repository: EventsRepository,
        private val notifications: EventNotifications
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            require(
                modelClass.isAssignableFrom(EventsViewModel::class.java)
            )

            return EventsViewModel(repository, notifications) as T
        }
    }
}

// Events UI

@Composable
fun EventsScreen(
    state: EventsUiState,
    onRefresh: () -> Unit,
    notificationsEnabled: Boolean,
    systemNotificationsAllowed: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onTestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Campus Events",
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(
                onClick = onRefresh,
                enabled = state !is EventsUiState.Loading
            ) {
                Text("Refresh")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Event updates",
                    style = MaterialTheme.typography.titleMedium
                )

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onToggleNotifications
                )
            }

            Text(
                text = when {
                    !notificationsEnabled ->
                        "Event notifications are off."

                    !systemNotificationsAllowed ->
                        "Event updates are selected, but Android is " +
                                "blocking notifications. Open notification settings."

                    else ->
                        "Notify me when a refresh finds changed events."
                },
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(
                onClick = onTestNotification,
                enabled = notificationsEnabled &&
                        systemNotificationsAllowed
            ) {
                Text("Send test notification")
            }

            TextButton(onClick = onOpenNotificationSettings) {
                Text("Notification settings")
            }
        }

        HorizontalDivider()

        when (state) {
            EventsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is EventsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }

            is EventsUiState.Content -> {
                if (state.events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No events right now.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = state.events,
                            key = { it.id }
                        ) {
                            EventRow(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventRow(
    event: CampusEvent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val meta = listOf(event.startTime, event.location)
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (event.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Tasks UI

@Composable
fun TaskScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    tasks: List<Task>,
    onToggleTask: (Task) -> Unit,
    onAddTask: (String, String) -> Unit,
    onOpenEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            Button(
                onClick = onOpenEvents,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text("Open Events")
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

                Spacer(Modifier.height(8.dp))

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
                enabled = title.isNotBlank(),
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
fun CampusApp(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    tasksViewModel: TasksViewModel,
    eventsViewModel: CampusEventViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    AppNavHost(
        navController = navController,
        isDarkMode = isDarkMode,
        onToggleDarkMode = onToggleDarkMode,
        tasksViewModel = tasksViewModel,
        eventsViewModel = eventsViewModel,
        modifier = modifier
    )
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    tasksViewModel: TasksViewModel,
    eventsViewModel: CampusEventViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "tasks",
        modifier = modifier
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                maxLines = 1,
                minLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

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
            onAddTask = { _, _ -> },
            onOpenEvents = {}
        )
    }
}
        composable("tasks") {
            TaskRoute(
                viewModel = tasksViewModel,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onNavigateEvents = { navController.navigate("events") }
            )
        }
        composable("events") {
            EventsScreen(
                viewModel = eventsViewModel
            )
        }
    }
}
