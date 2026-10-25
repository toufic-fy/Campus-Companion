package com.inci.campuscompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.inci.campuscompanion.model.CampusEvent
import com.inci.campuscompanion.ui.theme.CampusCompanionTheme

@Composable
fun EventsScreen(
    uiState: EventsUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        EventsUiState.Loading -> {
            CenteredContent(modifier) {
                CircularProgressIndicator()
            }
        }

        EventsUiState.Empty -> {
            CenteredContent(modifier) {
                Text(
                    text = "No upcoming events right now.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        is EventsUiState.Error -> {
            CenteredContent(modifier) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }

        is EventsUiState.Content -> {
            EventList(
                events = uiState.events,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun EventList(
    events: List<CampusEvent>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(
            items = events,
            key = { it.id }
        ) { event ->
            EventRow(event = event)
            HorizontalDivider()
        }
    }
}

@Composable
private fun EventRow(
    event: CampusEvent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${event.timeLabel} · ${event.locationLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CenteredContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
private val previewEvents = listOf(
    CampusEvent("e1", "Robotics Club Demo", "2026-10-22 14:00", "Hall B"),
    CampusEvent("e2", "Career Fair", "2026-10-25 10:00", "TBA")
)

@Preview(showBackground = true, name = "Loading")
@Composable
private fun EventsLoadingPreview() {
    CampusCompanionTheme { EventsScreen(EventsUiState.Loading, onRetry = {}) }
}

@Preview(showBackground = true, name = "Content")
@Composable
private fun EventsContentPreview() {
    CampusCompanionTheme { EventsScreen(EventsUiState.Content(previewEvents), onRetry = {}) }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun EventsEmptyPreview() {
    CampusCompanionTheme { EventsScreen(EventsUiState.Empty, onRetry = {}) }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun EventsErrorPreview() {
    CampusCompanionTheme {
        EventsScreen(
            EventsUiState.Error("Can't reach the server. Check your connection and try again."),
            onRetry = {}
        )
    }
}