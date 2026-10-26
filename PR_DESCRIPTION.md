# Pull Request: Campus Events Feed with Retrofit and Jetpack Compose

## Summary
This pull request implements the Campus Events feed feature in `Campus-Companion` using Retrofit, Gson, Kotlin Coroutines, StateFlow, and Jetpack Compose. It fetches live campus event data from the mockapi endpoint, parses it into domain models, handles Loading / Content / Empty / Error states gracefully, and provides both Retry and Refresh actions for seamless user interaction.

---

## What I Implemented (Per Layer)

1. **Networking Layer:**
   - `CampusApi` (`data/remote/CampusApi.kt`): Retrofit service interface defining `@GET("events/event") suspend fun getEvents(): List<EventDto>`.
   - `EventDto` (`data/remote/EventDto.kt`): Data Transfer Object with `@SerializedName` annotations for `id`, `name`, `startingDate`, `endingDate`, `description`, `address`, `avatar`, and `createdAt`.
   - OkHttp Logging Interceptor for HTTP request/response debugging.

2. **Domain & Repository Layer:**
   - `CampusEvent` (`model/CampusEvent.kt`): Clean domain data model (`id`, `title`, `startDate`, `endDate`, `location`, `description`).
   - `EventsRepository` (`data/repository/EventsRepository.kt`): Coordinates API calls via `CampusApi.getEvents()`, maps `EventDto` to `CampusEvent` via `toDomain()`, handles fallback values for missing fields, and wraps network calls in `runCatching` (safely rethrowing `CancellationException`).

3. **State Management & ViewModel Layer:**
   - `EventsViewModel` (`ui/EventsViewModel.kt`): Manages UI state using `EventsUiState` (`Loading`, `Content`, `Error`). Exposes `StateFlow<EventsUiState>` via `asStateFlow()`.
   - `load()`: Sets state to `Loading`, launches a Coroutine in `viewModelScope`, invokes `EventsRepository.loadEvents()`, and updates state to `Content` (or `Error`).
   - `retry()`: Re-triggers `load()` on failure.

4. **UI Layer (Jetpack Compose):**
   - `EventsScreen` (`ui/EventsScreen.kt`): Collects `uiState` using `collectAsStateWithLifecycle()`.
     - **Loading State:** Displays a centered `CircularProgressIndicator`.
     - **Content State:** Displays a top "Refresh" button and a scrollable `LazyColumn` of `EventCard` composables with formatted dates (`SimpleDateFormat`). Shows a "No events available." message if empty.
     - **Error State:** Displays an error message and a "Retry" button.
   - Integrated alongside the existing task manager in `MainActivity.kt` using a two-tab `NavigationBar` ("Tasks" and "Events"), preserving all existing teammate code.

---

## Refresh-Flow Explanation (Layer by Layer)

When the user taps the **Refresh** button on `EventsScreen`:

1. **UI Layer (`EventsScreen.kt`):**
   The `Button`'s `onClick` lambda fires and calls `viewModel.load()`.

2. **ViewModel Layer (`EventsViewModel.kt`):**
   - `EventsViewModel.load()` immediately emits `EventsUiState.Loading` to `_uiState`, causing Compose to render the `CircularProgressIndicator`.
   - It launches a coroutine in `viewModelScope` and calls `repository.loadEvents()`.

3. **Repository Layer (`EventsRepository.kt`):**
   - `EventsRepository.loadEvents()` executes inside `runCatching` and invokes `api.getEvents()`.

4. **Network / API Layer (`CampusApi.kt` / Retrofit / OkHttp):**
   - Retrofit/OkHttp sends an HTTP `GET` request to `https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event`.
   - On receiving an `HTTP 200 OK` response with a JSON array, Gson deserializes it into `List<EventDto>`.

5. **Mapping Back Up:**
   - `EventsRepository` maps each `EventDto` to a `CampusEvent` using `EventDto.toDomain()`, providing default fallbacks for any missing fields, and returns `Result.success(events)`.

6. **UI State Emission & Re-composition:**
   - `EventsViewModel` receives the `Result` and sets `_uiState.value = EventsUiState.Content(events)` (or `EventsUiState.Error` on failure).
   - `EventsScreen` observes the `StateFlow` via `collectAsStateWithLifecycle()`, triggering Compose to re-compose and update the UI with the refreshed event cards.

---

## How I Tested
- Verified clean build (`./gradlew assembleDebug`) with 0 compile errors.
- Verified live HTTP 200 response and JSON parsing in Logcat filtered by `OkHttp`.
- Executed 4 failure & edge-case scenarios (Airplane mode, broken URL, empty JSON `[]`, and recovery test). See `FAILURE_REPORT.md` for full results and screenshots.
