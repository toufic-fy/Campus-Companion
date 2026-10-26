package com.inci.campuscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


data class EventDto(
    @SerializedName("id", alternate = ["eventId", "event_id"]) val id: String? = null,
    @SerializedName("title", alternate = ["name", "eventName"]) val title: String? = null,
    @SerializedName("location", alternate = ["place", "venue", "body"]) val location: String? = null,
    @SerializedName("date", alternate = ["startTime", "dateTime", "when"]) val date: String? = null
)

interface CampusApi {
    @GET("posts")
    suspend fun getEvents(): List<EventDto>
}

object ApiProvider {
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    val api: CampusApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampusApi::class.java)
    }
}

data class CampusEvent(
    val id: String,
    val title: String,
    val location: String,
    val date: String
)

private fun EventDto.toDomain() = CampusEvent(
    id = id.orEmpty(),
    title = title?.takeIf { it.isNotBlank() } ?: "Untitled event",
    location = location?.takeIf { it.isNotBlank() } ?: "Location TBA",
    date = date?.takeIf { it.isNotBlank() } ?: "Date unknown"
)

class EventsRepository(private val api: CampusApi = ApiProvider.api) {
    suspend fun fetchEvents(): List<CampusEvent> = withContext(Dispatchers.IO) {
        api.getEvents().map { it.toDomain() }
    }
}


sealed interface EventsUiState {
    object Loading : EventsUiState
    data class Content(val events: List<CampusEvent>) : EventsUiState
    data class Error(val message: String) : EventsUiState
}

class EventsViewModel : ViewModel() {
    private val repository = EventsRepository()

    private val _state = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val state: StateFlow<EventsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = EventsUiState.Loading
        viewModelScope.launch {
            try {
                _state.value = EventsUiState.Content(repository.fetchEvents())
            } catch (e: CancellationException) {
                throw e // never swallow coroutine cancellation
            } catch (e: Exception) {
                _state.value = EventsUiState.Error(e.localizedMessage ?: "Something went wrong")
            }
        }
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                EventsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(vm: EventsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Events") },
                actions = { TextButton(onClick = { vm.refresh() }) { Text("Refresh") } }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val s = state) {
                is EventsUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is EventsUiState.Content ->
                    // No "key" on purpose: duplicate or empty ids can't crash the list.
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(s.events) { _, event -> EventCard(event) }
                    }

                is EventsUiState.Error ->
                    Column(
                        Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { vm.refresh() }) { Text("Retry") }
                    }
            }
        }
    }
}

@Composable
fun EventCard(event: CampusEvent) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text(event.location, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(event.date, style = MaterialTheme.typography.bodySmall)
        }
    }
}