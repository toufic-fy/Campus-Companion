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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/**
 * Renders state and reports events. It never creates Retrofit
 * or calls an endpoint itself.
 */
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
            IconButton(
                onClick = onRefresh,
                enabled = uiState !is EventsUiState.Loading
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh events")
            }
        }

        HorizontalDivider()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (uiState) {
                EventsUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                is EventsUiState.Content -> EventList(events = uiState.events)

                EventsUiState.Empty -> CenteredMessage(
                    message = "No campus events right now. Check back later!",
                    onRetry = onRefresh,
                    buttonLabel = "Refresh",
                    modifier = Modifier.align(Alignment.Center)
                )

                is EventsUiState.Error -> CenteredMessage(
                    message = uiState.message,
                    onRetry = onRefresh,
                    buttonLabel = "Retry",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    onRetry: () -> Unit,
    buttonLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(buttonLabel)
        }
    }
}

@Composable
private fun EventList(
    events: List<CampusEvent>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = events, key = { it.id }) { event ->
            EventRow(event = event)
        }
    }
}

@Composable
private fun EventRow(
    event: CampusEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = event.timeLabel, style = MaterialTheme.typography.bodyMedium)
            Text(text = event.locationLabel, style = MaterialTheme.typography.bodyMedium)
            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
                    CampusEvent("1", "Tech Talk", "2026-10-21 14:00", "Main Hall", "Intro to Retrofit"),
                    CampusEvent("2", "Career Fair", "2026-10-25 10:00", "TBA", "")
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
            uiState = EventsUiState.Error("No connection. Check your internet and try again."),
            onRefresh = {}
        )
    }
}
