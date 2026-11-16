package com.inci.campuscompanion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inci.campuscompanion.work.RefreshStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    state: EventsUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {}
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.refresh.status) {
        if (state.refresh.status == RefreshStatus.Current) listState.scrollToItem(0)
    }
    val busy = state.refreshPending || state.refresh.status == RefreshStatus.Queued ||
        state.refresh.status == RefreshStatus.Refreshing
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Campus Events") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh, enabled = !busy && !state.loadMore.loading) { Text("Refresh") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            header()
            Text(
                text = when (state.refresh.status) {
                    RefreshStatus.Idle -> "Ready to refresh."
                    RefreshStatus.Queued -> "Refresh queued. Waiting for connectivity or retry."
                    RefreshStatus.Refreshing -> "Refreshing events..."
                    RefreshStatus.Current -> "Events are up to date."
                    RefreshStatus.Failed -> state.refresh.errorMessage ?: "Refresh failed. Please retry."
                },
                modifier = Modifier.padding(16.dp)
            )
            if (state.refresh.status == RefreshStatus.Failed) {
                TextButton(onClick = onRefresh, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Retry")
                }
            }
            if (state.events.isNotEmpty()) {
                LazyColumn(Modifier.weight(1f), state = listState) {
                    items(state.events, key = { "event:${it.id}" }) { event ->
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium)
                            Text(event.timeLabel)
                            Text(event.locationLabel)
                        }
                        HorizontalDivider()
                    }
                    item(key = "pagination-footer") {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when {
                                state.loadMore.loading -> {
                                    CircularProgressIndicator()
                                    Text("Loading more events...")
                                }
                                state.loadMore.errorMessage != null -> {
                                    Text(state.loadMore.errorMessage)
                                    TextButton(onClick = onLoadMore, enabled = !busy) { Text("Retry loading more") }
                                }
                                state.canLoadMore -> TextButton(onClick = onLoadMore, enabled = !busy) {
                                    Text("Load more")
                                }
                                else -> Text("All events loaded.")
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    when (state.refresh.status) {
                        RefreshStatus.Refreshing -> CircularProgressIndicator()
                        RefreshStatus.Queued -> Text("Events will appear after refresh completes.")
                        RefreshStatus.Failed -> Text("No cached events available.")
                        else -> Text("No campus events yet.")
                    }
                }
            }
        }
    }
}
