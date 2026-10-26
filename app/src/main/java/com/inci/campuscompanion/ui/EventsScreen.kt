package com.inci.campuscompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inci.campuscompanion.model.CampusEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row

@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Campus Events",
                style = MaterialTheme.typography.headlineMedium
            )

            TextButton(
                onClick = { viewModel.loadEvents() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh events"
                )
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        when (val currentState = state) {

            is EventsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text("Loading events...")
                    }
                }
            }

            is EventsUiState.Content -> {
                if (currentState.events.isEmpty()) {
                    Text("No events available.")
                } else {
                    val listState = rememberLazyListState()

                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val totalItemsCount = listState.layoutInfo.totalItemsCount
                            val lastVisibleItemIndex =
                                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 2
                        }
                    }

                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value) {
                            viewModel.loadNextPage()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = currentState.events,
                            key = { event -> event.id }
                        ) { event ->
                            EventCard(event)
                        }

                        if (currentState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (currentState.hasReachedEnd && currentState.events.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No more events",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is EventsUiState.Error -> {
                Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.padding(4.dp))

                Button(
                    onClick = { viewModel.loadEvents() }
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun EventCard(event: CampusEvent) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge
            )

            Text(text = "Date: ${event.date}")

            Text(
                text = "Location: ${event.location ?: "Not specified"}"
            )

            event.description?.let { description ->
                Text(text = description)
            }
        }
    }
}
