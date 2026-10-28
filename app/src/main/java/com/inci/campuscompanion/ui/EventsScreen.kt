package com.inci.campuscompanion.ui

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme

@Composable
fun EventsScreen(
    uiState: EventsUiState,
    onRefresh: () -> Unit,
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
            Text(text = "Campus Events", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onRefresh, enabled = uiState !is EventsUiState.Loading) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh events")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                EventsUiState.Loading -> CircularProgressIndicator()
                is EventsUiState.Error -> EventsError(message = uiState.message, onRetry = onRefresh)
                EventsUiState.Empty ->
                    Text(text = "No upcoming events.", style = MaterialTheme.typography.bodyLarge)
                is EventsUiState.Content -> EventList(events = uiState.events)
            }
        }
    }
}

@Composable
fun EventsError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Couldn't load events",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun EventList(
    events: List<CampusEvent>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(
            items = events,
            key = { it.id }
        ) { event ->
            EventRow(event = event)
        }
    }
}

@Composable
fun EventRow(
    event: CampusEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${event.startsAt} · ${event.location}",
                style = MaterialTheme.typography.bodySmall
            )
            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventsContentPreview() {
    CampusCompanionTheme(darkTheme = false) {
        EventsScreen(
            uiState = EventsUiState.Content(
                listOf(
                    CampusEvent("1", "Welcome Week Fair", "Main Quad", "Oct 22, 2026 · 10:00", "Meet clubs."),
                    CampusEvent("2", "Android Workshop", "Lab 3", "Oct 23, 2026 · 14:00", "")
                )
            ),
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EventsErrorPreview() {
    CampusCompanionTheme(darkTheme = false) {
        EventsScreen(
            uiState = EventsUiState.Error("Unable to resolve host"),
            onRefresh = {}
        )
    }
}
