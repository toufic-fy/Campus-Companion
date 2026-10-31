# Campus events

Run Campus Companion and select **Open Events** on the Tasks screen. Refresh fetches the feed again. System Back or the Events toolbar's Back action returns to Tasks. Tasks and the existing theme preference continue to work.

## Navigation workshop

`CampusCompanionApp` owns `rememberNavController()` and the `NavHost`. Its start destination is `Routes.TASKS`; `Routes.EVENTS` is the second destination. `TaskScreen` receives an `onOpenEvents` callback, and `EventsScreen` receives an `onBack` callback. Neither screen needs a NavController.

Opening Events pushes its destination above Tasks. System Back pops that entry, revealing Tasks. The toolbar Back callback uses `popBackStack()` for the same behavior. `launchSingleTop` prevents duplicate Events entries when navigation is requested repeatedly.

The navigation decision is to keep the controller in the root composable and pass callbacks to screens. This keeps the screens reusable and makes the Tasks -> Events -> Tasks behavior testable without putting navigation logic in the event feed.

## API contract

GET https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event

Response: JSON array. Required fields: id, name, startingDate. Optional fields: endingDate, description, address, avatar, createdAt. The app displays name, start/end dates, address and description. Portrait URLs are parsed but not displayed. Dates use the device timezone.

## Architecture

CampusApi (suspend Retrofit GET) -> EventsRemoteDataSource -> EventsRepository -> EventsViewModel StateFlow -> EventsScreen.

The repository validates required fields, dates and unique IDs, formats dates, supplies optional field fallbacks and sorts the feed. Compose renders state and sends callbacks; it does not create Retrofit or call endpoints.

Loading, Content, Empty and Error are distinct states. Connection/timeouts, HTTP rejection/server failures and malformed data have appropriate messages. Retry performs a fresh request. Cancellation is propagated and overlapping requests are ignored. No caching or authentication is added.

### What Refresh does

1. `EventsScreen` invokes the `onRefresh` callback supplied by `MainActivity`.
2. `EventsViewModel.refresh()` publishes Loading and starts a coroutine in `viewModelScope`.
3. `EventsRepository` calls `EventsRemoteDataSource`, which invokes the suspend `CampusApi.getEvents()` method.
4. Retrofit and OkHttp make the HTTPS GET; Moshi decodes its JSON array into `EventDto` objects.
5. The repository validates and maps those DTOs into sorted `CampusEvent` objects.
6. The ViewModel publishes Content, Empty, or Error through its StateFlow. Compose collects the state with lifecycle awareness and redraws the list, empty message, or retryable error.

Retry uses the same path. Network and HTTP exceptions become Error state instead of escaping into the UI; coroutine cancellation is rethrown.

## Build and checks

Open the repository in Android Studio, sync Gradle and run app. Use SDK 37 and the Android Studio bundled JDK. The existing minimum API 24 is retained using core library desugaring for java.time.

Run `gradlew.bat testDebugUnitTest assembleDebug` in one command. EventsTest uses MockWebServer to test the actual GET path, JSON mapping, empty arrays, missing fields, invalid dates, HTTP 500, connection failure messages and ViewModel recovery after Retry.

Manual checks: open Events with internet enabled; disable connectivity and Refresh; restore connectivity and Retry. An empty JSON array should display No events yet. Automated fixtures cover empty/malformed responses without modifying the shared API.

Run `gradlew.bat connectedDebugAndroidTest` with a connected emulator for `WorkshopEvidenceTest`. It exercises the actual Retrofit/repository/ViewModel/Compose path, an invalid base URL followed by Retry on the valid endpoint, a test-only HTTPS server returning `[]`, and emulator Wi-Fi/mobile-data disconnection followed by recovery. The navigation test also presses system Back. Test-only TLS certificates are trusted only by the injected fixture client; production HTTPS validation is unchanged.

The tests save PNG screenshots and result logs to the app's external `files/failure-evidence` directory. Connectivity is re-enabled in a `finally` block. The production base URL remains the supplied valid URL throughout the tests.

### Verified 31 October 2026

- Debug app and Android test APKs built successfully; 8 unit tests passed (7 events tests and the existing example test).
- All 4 `WorkshopEvidenceTest` instrumented tests passed on Pixel 9, API 37, in 71.219 seconds.
- Invalid base URL: HTTP 404 produced a visible Error and Retry; restoring the valid client and selecting Retry displayed 50 events.
- Empty HTTPS JSON array: the deliberate No events yet message and Refresh action were visible.
- Disabling emulator Wi-Fi and mobile data followed by Refresh produced a connection Error without a crash. Re-enabling both and selecting Retry displayed 50 events.
- Tasks was the launch destination; Open Events opened Events; system Back returned to Tasks.
- Production MainActivity also completed a cold launch without an AndroidRuntime error. The valid production endpoint and emulator connectivity were restored.
