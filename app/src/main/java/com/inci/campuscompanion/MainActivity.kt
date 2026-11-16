package com.inci.campuscompanion

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import com.inci.campuscompanion.data.AppDatabase
import com.inci.campuscompanion.data.TaskRepository
import com.inci.campuscompanion.data.UserPreferencesRepository
import com.inci.campuscompanion.data.dataStore
import com.inci.campuscompanion.model.Task
import com.inci.campuscompanion.ui.MainViewModel
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        EventsNotification.createChannel(this)
        RefreshEventsWorker.schedulePeriodic(WorkManager.getInstance(applicationContext))

        val database = AppDatabase.getDatabase(applicationContext)
        val taskRepository = TaskRepository(database.taskDao())
        val preferencesRepository = UserPreferencesRepository(dataStore)

        val viewModel: MainViewModel by viewModels {
            MainViewModel.Factory(preferencesRepository, taskRepository)
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            CampusCompanionTheme(darkTheme = isDarkMode) {
                CampusApp(viewModel = viewModel)
            }
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
    onOpenEvents: () -> Unit = {},
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Campus events")
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

data class EventDto(
    val id: String,
    val name: String?,
    val description: String?,
    val address: String?,
    val startingDate: String?,
    val endingDate: String?,
    val avatar: String?,
    val createdAt: String?
)

interface CampusApi {
    @GET("events/event")
    suspend fun getEvents(): List<EventDto>

    companion object {
        private const val BASE_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/"

        val instance: CampusApi by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CampusApi::class.java)
        }
    }
}

data class CampusEvent(
    val id: String,
    val name: String,
    val description: String,
    val address: String,
    val startingDate: String,
    val endingDate: String
)

class EventsRepository(private val api: CampusApi) {

    suspend fun fetchEvents(): Result<List<CampusEvent>> =
        try {
            Result.success(api.getEvents().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
}

private fun EventDto.toDomain() = CampusEvent(
    id = id,
    name = name ?: "Untitled",
    description = description ?: "",
    address = address ?: "Unknown location",
    startingDate = startingDate?.take(10) ?: "",
    endingDate = endingDate?.take(10) ?: ""
)

sealed interface EventsUiState {
    data object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventNotificationPreferences(private val dataStore: DataStore<Preferences>) {

    private val enabledKey = booleanPreferencesKey("event_notifications_enabled")
    private val knownEventIdsKey = stringSetPreferencesKey("known_event_ids")

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[enabledKey] ?: false }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun isEnabled(): Boolean = notificationsEnabled.first()

    suspend fun knownEventIds(): Set<String>? = dataStore.data.first()[knownEventIdsKey]

    suspend fun saveKnownEventIds(ids: Set<String>) {
        dataStore.edit { it[knownEventIdsKey] = ids }
    }
}

class EventsViewModel(
    private val repository: EventsRepository,
    private val workManager: WorkManager,
    private val preferences: EventNotificationPreferences
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = preferences.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(enabled) }
    }

    private val _state = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val state: StateFlow<EventsUiState> = _state

    val refreshStatus: StateFlow<String?> = workManager
        .getWorkInfosForUniqueWorkFlow(RefreshEventsWorker.WORK_NAME)
        .map { infos -> infos.firstOrNull()?.toStatusText() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refreshInBackground() {
        RefreshEventsWorker.refreshNow(workManager)
    }

    init { load() }

    fun load() {
        _state.value = EventsUiState.Loading
        viewModelScope.launch {
            repository.fetchEvents()
                .onSuccess { _state.value = EventsUiState.Content(it) }
                .onFailure { _state.value = EventsUiState.Error(it.message ?: "Unknown error") }
        }
    }

    companion object {
        fun Factory(
            repository: EventsRepository,
            workManager: WorkManager,
            preferences: EventNotificationPreferences
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EventsViewModel(repository, workManager, preferences) as T
            }
    }
}

private fun WorkInfo.toStatusText(): String = when (state) {
    WorkInfo.State.ENQUEUED -> "Background refresh waiting for internet…"
    WorkInfo.State.RUNNING -> "Refreshing in background…"
    WorkInfo.State.SUCCEEDED -> {
        val count = outputData.getInt(RefreshEventsWorker.KEY_EVENT_COUNT, 0)
        val newCount = outputData.getInt(RefreshEventsWorker.KEY_NEW_EVENT_COUNT, 0)
        "Background refresh done: $count events, $newCount new"
    }
    WorkInfo.State.FAILED -> "Background refresh failed"
    WorkInfo.State.BLOCKED -> "Background refresh waiting…"
    WorkInfo.State.CANCELLED -> "Background refresh cancelled"
}

private const val ROUTE_TASKS = "tasks"
private const val ROUTE_EVENTS = "events"

@Composable
fun CampusApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    AppNavHost(navController = navController, viewModel = viewModel)
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_TASKS
    ) {
        composable(ROUTE_TASKS) {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                TaskScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                    tasks = tasks,
                    onToggleTask = { viewModel.toggleTaskCompletion(it) },
                    onAddTask = { title, description -> viewModel.upsertTask(title, description) },
                    onOpenEvents = { navController.navigate(ROUTE_EVENTS) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        composable(ROUTE_EVENTS) {
            val context = LocalContext.current
            val eventsViewModel: EventsViewModel = viewModel(
                factory = EventsViewModel.Factory(
                    repository = EventsRepository(CampusApi.instance),
                    workManager = WorkManager.getInstance(context.applicationContext),
                    preferences = EventNotificationPreferences(context.applicationContext.dataStore)
                )
            )
            val eventsState by eventsViewModel.state.collectAsStateWithLifecycle()
            val refreshStatus by eventsViewModel.refreshStatus.collectAsStateWithLifecycle()
            val notificationsEnabled by eventsViewModel.notificationsEnabled.collectAsStateWithLifecycle()

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                eventsViewModel.setNotificationsEnabled(granted)
            }

            EventsScreen(
                state = eventsState,
                refreshStatus = refreshStatus,
                notificationsEnabled = notificationsEnabled,
                onNotificationsChange = { enabled ->
                    if (enabled && !EventsNotification.canPost(context)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        eventsViewModel.setNotificationsEnabled(enabled)
                    }
                },
                onTestNotification = { EventsNotification.showTest(context) },
                onRetry = { eventsViewModel.load() },
                onBackgroundRefresh = { eventsViewModel.refreshInBackground() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    state: EventsUiState,
    refreshStatus: String?,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onTestNotification: () -> Unit,
    onRetry: () -> Unit,
    onBackgroundRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Events") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = refreshStatus ?: "No background refresh yet",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onBackgroundRefresh) {
                    Text("Refresh in background")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Event notifications",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsChange
                )
            }
            TextButton(
                onClick = onTestNotification,
                enabled = notificationsEnabled,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Send test notification")
            }
            HorizontalDivider()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is EventsUiState.Loading -> CircularProgressIndicator()

                    is EventsUiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRetry) { Text("Retry") }
                    }

                    is EventsUiState.Content -> Column(Modifier.fillMaxSize()) {
                        Button(onClick = onRetry, modifier = Modifier.padding(16.dp)) { Text("Refresh") }
                        if (state.events.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No events available")
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                                items(state.events, key = { it.id }) { e ->
                                    Column(Modifier.padding(vertical = 8.dp)) {
                                        Text(e.name, style = MaterialTheme.typography.titleMedium)
                                        Text(e.address, style = MaterialTheme.typography.bodyMedium)
                                        Text("${e.startingDate} → ${e.endingDate}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

object EventsNotification {

    const val CHANNEL_ID = "event_updates"
    private const val NEW_EVENTS_ID = 1001
    private const val TEST_ID = 1002

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New campus events"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    fun showTest(context: Context) {
        show(context, TEST_ID, "Event updates", "Notifications are working")
    }

    fun showNewEvents(context: Context, newEvents: List<CampusEvent>) {
        val title = if (newEvents.size == 1) "New campus event" else "${newEvents.size} new campus events"
        val text = newEvents.take(3).joinToString { it.name }
        show(context, NEW_EVENTS_ID, title, text)
    }

    @SuppressLint("MissingPermission")
    private fun show(context: Context, id: Int, title: String, text: String) {
        if (!canPost(context)) return

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

class RefreshEventsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = EventsRepository(CampusApi.instance)
        val preferences = EventNotificationPreferences(applicationContext.dataStore)

        val events = repository.fetchEvents().getOrElse {
            return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }

        val knownIds = preferences.knownEventIds()
        val newEvents = if (knownIds == null) emptyList() else events.filter { it.id !in knownIds }
        preferences.saveKnownEventIds(events.map { it.id }.toSet())

        if (newEvents.isNotEmpty() && preferences.isEnabled()) {
            EventsNotification.showNewEvents(applicationContext, newEvents)
        }

        return Result.success(
            workDataOf(
                KEY_EVENT_COUNT to events.size,
                KEY_NEW_EVENT_COUNT to newEvents.size
            )
        )
    }

    companion object {
        const val WORK_NAME = "refresh_events"
        const val PERIODIC_WORK_NAME = "refresh_events_periodic"
        const val KEY_EVENT_COUNT = "event_count"
        const val KEY_NEW_EVENT_COUNT = "new_event_count"
        private const val MAX_ATTEMPTS = 3

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        fun refreshNow(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<RefreshEventsWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun schedulePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<RefreshEventsWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}