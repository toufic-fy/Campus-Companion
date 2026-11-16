package com.example.navigationworkshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.navigationworkshop.ui.theme.NavigationWorkshopTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NavigationWorkshopTheme {
                NavigationApp()
            }
        }
    }
}

@Composable
fun NavigationApp() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "tasks"
    ) {
        composable("tasks") {
            TasksScreen(
                onOpenEvents = {
                    navController.navigate("events")
                }
            )
        }

        composable("events") {
            EventsScreen()
        }
    }
}

@Composable
fun TasksScreen(onOpenEvents: () -> Unit) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Tasks Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Welcome to your Tasks!",
            modifier = Modifier.padding(16.dp)
        )

        Button(onClick = onOpenEvents) {
            Text("Open Events")
        }
    }
}

@Composable
fun EventsScreen() {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val scheduler = EventsRefreshScheduler(context)
    val workManager = WorkManager.getInstance(context)

    val workInfos by produceState(
        initialValue = emptyList<WorkInfo>(),
        workManager,
        lifecycleOwner
    ) {
        val liveData =
            workManager.getWorkInfosForUniqueWorkLiveData(
                EventsRefreshScheduler.WORK_NAME
            )

        val observer =
            androidx.lifecycle.Observer<List<WorkInfo>> {
                value = it
            }

        liveData.observe(lifecycleOwner, observer)

        awaitDispose {
            liveData.removeObserver(observer)
        }
    }

    val latestWork = workInfos.lastOrNull()

    val status = when (latestWork?.state) {
        WorkInfo.State.ENQUEUED -> "Queued"
        WorkInfo.State.RUNNING -> "Running"
        WorkInfo.State.SUCCEEDED -> "Finished"
        WorkInfo.State.FAILED -> "Failed"
        WorkInfo.State.CANCELLED -> "Cancelled"
        WorkInfo.State.BLOCKED -> "Blocked"
        null -> "Not started"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Events Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Welcome to Events!",
            modifier = Modifier.padding(16.dp)
        )

        Button(
            onClick = {
                scheduler.refreshEvents()
            }
        ) {
            Text("Refresh Events")
        }

        Text(
            text = "Refresh Status: $status",
            modifier = Modifier.padding(16.dp)
        )
    }
}