package com.inci.campuscompanion.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inci.campuscompanion.data.repository.CampusEvent
@Composable
fun EventsScreen(
    viewModel: EventsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is EventsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is EventsUiState.Content -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Add a refresh button at the top
                    Button(
                        onClick = { viewModel.fetchEvents() },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    ) {
                        Text("Refresh Feed")
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.events) { event ->
                            EventItem(event)
                        }
                    }
                }
            }
            is EventsUiState.Empty -> {
                Text(
                    text = "No campus events found.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is EventsUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    if (state.isRetryable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.fetchEvents() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventItem(event: CampusEvent) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = event.title, style = MaterialTheme.typography.titleMedium)
        Text(text = "${event.timeLabel} • ${event.locationLabel}", style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}