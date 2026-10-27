# Campus Events feed

The Events tab fetches the class MockAPI endpoint. The Tasks tab retains Room-backed tasks and the dark-mode preference.

## API contract

`GET https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event`

The inspected response is a JSON array. `EventDto` reads `id`, `name`, `startingDate`, `address`, and `description`; unused fields are ignored. The repository requires a nonblank ID, name, and start time, maps `name` to `title`, preserves the supplied timestamp as `timeLabel`, and maps a missing/blank address to `TBA`.

## What Refresh does

1. `EventsScreen` invokes its callback. It has no Retrofit dependency.
2. `EventsViewModel.refresh()` prevents overlapping requests, publishes Loading, and starts a coroutine in `viewModelScope`.
3. `EventsRepository` calls `EventsRemoteDataSource`, which invokes `CampusApi.getEvents()`.
4. Retrofit combines the HTTPS base URL with `event`, executes the suspend GET through OkHttp, and Gson decodes the JSON array into DTOs. Requests have a 20-second overall timeout.
5. The repository validates and maps each DTO to `CampusEvent`.
6. The ViewModel publishes Content for a nonempty result, Empty for `[]`, or a retryable Error for HTTP, connection, or data failures. Cancellation propagates rather than being shown as an error.
7. Lifecycle-aware state collection in the Activity triggers Compose to render the new state. Retry follows the same path.

## Reproduce validation

Use Android Studio's JDK and the repository's existing Gradle/SDK versions. Configure `local.properties` with your Android SDK path; do not commit it.

```sh
bash gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest
bash gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.inci.campuscompanion.EventsFailureExerciseTest
```

The instrumented exercise requires an emulator with internet. It temporarily disables emulator Wi-Fi and mobile data, tests an invalid HTTPS base URL, supplies a test-only HTTP 200 body of `[]` through an OkHttp interceptor (using real Retrofit/Gson parsing), and restores the live API before pressing Retry. It does not edit or delete shared MockAPI records. Its `finally` block enables the emulator radios and restores the live API delegate.

Screenshots and `observations.txt` are written to the target app's external files directory under `exercise/`. Gradle XML/HTML results live under `app/build/outputs/androidTest-results` and `app/build/reports`. An unexecuted test is not evidence of a pass; consult the submitted failure-exercise report for actual observations and limitations.
