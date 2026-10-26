package com.inci.campuscompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EventsScreen(
    state: EventsUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        androidx.compose.material3.TopAppBar(
            title = { Text("Campus Events") },
            actions = {
                TextButton(onClick = onRefresh, enabled = state !is EventsUiState.Loading) {
                    Text("Refresh")
                }
            }
        )
        Box(Modifier.fillMaxSize()) {
            when (state) {
                EventsUiState.Loading -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading events…")
                }

                EventsUiState.Empty -> EmptyEvents(onRefresh)
                is EventsUiState.Error -> ErrorEvents(state.message, onRefresh)
                is EventsUiState.Content -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${state.events.size} events · Times shown in your device timezone",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    items(state.events, key = { it.id }) { event ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(event.title, style = MaterialTheme.typography.titleLarge)
                                Text("Starts: ${event.startTime}")
                                Text("Ends: ${event.endTime ?: "Not specified"}")
                                Text(event.address, style = MaterialTheme.typography.labelLarge)
                                HorizontalDivider()
                                Text(event.description)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyEvents(onRefresh: () -> Unit) {
    StatusMessage(
        title = "No events yet",
        message = "Check back later for upcoming campus events.",
        action = "Refresh",
        onAction = onRefresh
    )
}

@Composable
private fun ErrorEvents(message: String, onRetry: () -> Unit) {
    StatusMessage(
        title = "Unable to load events",
        message = message,
        action = "Retry",
        onAction = onRetry
    )
}

@Composable
private fun StatusMessage(title: String, message: String, action: String, onAction: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(message)
            Button(onClick = onAction) { Text(action) }
        }
    }
}
