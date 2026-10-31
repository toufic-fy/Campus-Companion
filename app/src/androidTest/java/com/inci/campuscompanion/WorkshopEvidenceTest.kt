package com.inci.campuscompanion

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.inci.campuscompanion.data.*
import com.inci.campuscompanion.ui.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** Exercises the real Retrofit -> repository -> ViewModel -> Compose path.
 * Test-only dependency injection never changes the production endpoint.
 */
class WorkshopEvidenceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun evidence(name: String, result: String) {
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        // Semantics can update before SurfaceFlinger presents the new frame.
        Thread.sleep(600)
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "failure-evidence")
        dir.mkdirs()
        checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Screenshot capture failed" }.useBitmap { bitmap ->
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        File(dir, "$name.txt").writeText(result)
        Log.i("WorkshopEvidence", "$name: $result")
    }

    private fun Bitmap.useBitmap(block: (Bitmap) -> Unit) {
        try { block(this) } finally { recycle() }
    }

    private fun show(api: CampusApi): EventsViewModel {
        lateinit var model: EventsViewModel
        compose.runOnUiThread { model = EventsViewModel(EventsRepository(EventsRemoteDataSource(api))) }
        compose.setContent {
            MaterialTheme {
                val state by model.state.collectAsStateWithLifecycle()
                EventsScreen(state, model::refresh)
            }
        }
        return model
    }

    private fun awaitText(text: String) {
        compose.waitUntil(60_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun awaitContent(model: EventsViewModel) {
        compose.waitUntil(60_000) { model.state.value is EventsUiState.Content }
        compose.waitForIdle()
        val count = (model.state.value as EventsUiState.Content).events.size
        compose.onNodeWithText("$count events", substring = true).assertIsDisplayed()
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).readBytes()
        }
    }

    @Test fun a_navigationTasksEventsSystemBack() {
        compose.setContent {
            MaterialTheme {
                CampusCompanionApp(false, {}, emptyList(), {}, { _, _ -> }, EventsUiState.Empty, {})
            }
        }
        compose.onNodeWithText("Open Events").assertIsDisplayed()
        evidence("navigation_tasks", "PASS: Tasks is the initial destination; Open Events is visible.")
        compose.onNodeWithText("Open Events").performClick()
        awaitText("Campus Events")
        evidence("navigation_events", "PASS: Open Events navigates to the events route.")
        Espresso.pressBack()
        awaitText("Open Events")
        evidence("navigation_back", "PASS: System Back pops Events and returns to Tasks.")
    }

    @Test fun b_wrongBaseUrlAndRestoreRetry() {
        var current = AppContainer.createApi("https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/invalid-workshop-path/")
        val delegatingApi = object : CampusApi {
            override suspend fun getEvents(): List<EventDto> = current.getEvents()
        }
        val model = show(delegatingApi)
        awaitText("The events feed could not be found. Please try again later.")
        compose.onNodeWithText("Retry").assertIsDisplayed()
        evidence("wrong_base_url", "PASS: Changed base URL to /api/v1/invalid-workshop-path/. Actual HTTP 404 -> Error; Retry displayed; no crash. No fix needed.")
        compose.runOnIdle { current = AppContainer.createApi() }
        compose.onNodeWithText("Retry").performClick()
        awaitContent(model)
        evidence("restored_endpoint", "PASS: Restored AppContainer.BASE_URL on the same repository and selected Retry. Content returned with ${(model.state.value as EventsUiState.Content).events.size} events. No fix needed.")
    }

    @Test fun c_emptyJsonArray() {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverCertificates = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientCertificates = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val server = MockWebServer()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()
        try {
            server.enqueue(MockResponse().setBody("[]").setHeader("Content-Type", "application/json"))
            val client = OkHttpClient.Builder().sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
                .callTimeout(15, TimeUnit.SECONDS).build()
            show(AppContainer.createApi(server.url("/").toString(), client))
            awaitText("No events yet")
            compose.onAllNodesWithText("Refresh").onFirst().assertIsDisplayed()
            evidence("empty_array", "PASS: Test-only local HTTPS MockWebServer returned HTTP 200 with []. Actual UI: No events yet, explanatory message and Refresh; no Error. No fix needed. Production shared API was not modified.")
        } finally { server.shutdown() }
    }

    @Test fun d_disconnectedNetworkAndRetry() {
        val model = show(AppContainer.createApi())
        awaitContent(model)
        evidence("live_content", "PASS: Live endpoint loaded ${(model.state.value as EventsUiState.Content).events.size} events before network disconnection.")
        try {
            shell("svc wifi disable")
            shell("svc data disable")
            // Wait for connectivity to settle before making a fresh request.
            Thread.sleep(2500)
            compose.onNodeWithText("Refresh").performClick()
            awaitText("Could not connect. Check your internet connection and retry.")
            compose.onNodeWithText("Retry").assertIsDisplayed()
            evidence("disconnected_network", "PASS: Disabled emulator Wi-Fi and mobile data with svc wifi disable and svc data disable, then Refresh. Actual result: connection Error and Retry, app remained running. No fix needed.")
        } finally {
            shell("svc wifi enable")
            shell("svc data enable")
        }
        Thread.sleep(5000)
        compose.onNodeWithText("Retry").performClick()
        awaitContent(model)
        evidence("network_restored", "PASS: Re-enabled Wi-Fi and mobile data and selected Retry. Content returned with ${(model.state.value as EventsUiState.Content).events.size} events. Network configuration restored.")
    }
}
