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
import androidx.compose.foundation.layout.PaddingValues
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

        val eventsViewModel: EventsViewModel by viewModels {
            EventsViewModel.Factory(EventsRepository(CampusApi.instance))
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val tasks by viewModel.tasks.collectAsStateWithLifecycle()
            val eventsState by eventsViewModel.state.collectAsStateWithLifecycle()

            CampusCompanionTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EventsScreen(
                        state = eventsState,
                        onRetry = { eventsViewModel.load() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
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

class EventsViewModel(private val repository: EventsRepository) : ViewModel() {

    private val _state = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val state: StateFlow<EventsUiState> = _state

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
        fun Factory(repository: EventsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EventsViewModel(repository) as T
            }
    }
}

@Composable
fun EventsScreen(state: EventsUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            is EventsUiState.Loading -> CircularProgressIndicator()

            is EventsUiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }

            is EventsUiState.Content -> Column(Modifier.fillMaxSize()) {
                Button(onClick = onRetry, modifier = Modifier.padding(16.dp)) { Text("Refresh") }
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