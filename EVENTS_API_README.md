# Campus Events API — Charbel

Branch: `feature/charbel-events-api`

## Implemented

- HTTPS GET `https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event`.
- Internet permission and Retrofit with Kotlin serialization.
- `EventDto` mirrors the live JSON (`id`, `name`, `startingDate`, `endingDate`, `address`, `description`). Unused API fields are ignored.
- `CampusApi` → `EventsRemoteDataSource` → `EventsRepository` → `EventsViewModel` → `EventsScreen`.
- Repository maps name to title, formats UTC timestamps in the device's local time, and uses TBA for missing/blank addresses. Invalid required values, invalid dates, reversed ranges, and duplicate IDs produce errors.
- ViewModel uses viewModelScope, preserves cancellation, prevents overlapping requests, and exposes Loading, Content, Empty, and Error states.
- UI shows progress, event cards, empty message, classified error messages, Retry, and Refresh.
- Tasks and DataStore dark-mode behavior remain available under Tasks. Events uses the same theme.
- No Room caching, authentication, sync, or writes to the API.

## Open and verify

Open this folder in Android Studio. Use the original project's configured SDK/toolchain (compile/target SDK 37, Gradle 9.6.0, daemon JDK 25). No original toolchain versions were changed.

Windows terminal:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

macOS/Linux:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Then run on an emulator/device:

1. Select Events; confirm loading then event cards.
2. Tap Refresh; confirm loading then content.
3. Disable networking and tap Refresh; wait up to the 30-second timeout. Confirm an error with Retry, without a crash.
4. Restore networking, tap Retry; confirm content returns.
5. Empty JSON and malformed data paths are covered by the included tests using a local MockWebServer; they never modify the shared API.
6. Return to Tasks; add/complete a task and toggle Dark Mode.

## Validation status

Verified on GitHub Actions for source commit `080a528e4e0950ff927f8bbf295a226dfb55442b`:

- Debug APK build: passed.
- JVM unit tests: passed.
- Android lint: passed.
- Android API 35 emulator instrumentation tests: all 7 passed, with zero failures, errors, or skipped tests.
- Safe diagnostic logging: implemented and tested; error types are logged without response bodies or exception messages.
- Recovery exercise: slow Loading -> offline Error -> Retry -> Content -> Empty -> invalid-data Error -> Retry -> Content: passed.
- Live endpoint: read successfully; all 50 returned records matched required fields, timestamps, date ranges, and unique IDs at verification time.

Verification run and downloadable APK/test reports:
https://github.com/ghsoub/Campus-Companion/actions/runs/40370914686

The controlled recovery exercise remains as a deterministic test of empty and invalid responses. A separate `LiveEventsDeviceTest` now launches the real MainActivity and its production Retrofit client. It successfully loaded the live event feed, enabled the emulator's airplane mode and disabled Wi-Fi/mobile data, observed a real UnknownHostException and the error/Retry screen, restored connectivity, pressed Retry, and displayed live events again.

Screenshots and logcat evidence are included in the verification run's artifact under `evidence/`:

- `01-live-content.png`: production app displaying live event cards.
- `02-real-offline-error.png`: airplane mode on, real network failure, Retry displayed.
- `03-live-retry-recovery.png`: connectivity restored and live content returned.
- `logcat.txt`: safe diagnostics and each completed device exercise step.

This was executed on an Android emulator, not a physical phone. The shared API was only read; its records were not modified. Future server changes and different devices can still affect behavior.

## Submission

The completed work is published on `feature/charbel-events-api` in your own repository:

https://github.com/ghsoub/Campus-Companion/tree/feature/charbel-events-api

Pull request for teacher review:
https://github.com/toufic-fy/Campus-Companion/pull/26

The teacher reviews and merges the pull request.

If using the source ZIP instead of the Git bundle, copy the changed files into your own clone and run the teacher's branch/add/commit/push commands. The ZIP does not include `.git`.

## Explain one refresh

Refresh calls EventsViewModel.refresh(). It launches a coroutine and sets Loading. The repository asks the remote data source, which invokes Retrofit's suspend GET. The repository validates/maps DTOs. The ViewModel publishes Content, Empty, or a classified Error. Compose observes state and renders it; Compose never constructs Retrofit or calls the API.
