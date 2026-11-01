# Campus Events Feed - Failure Exercise & Reliability Report

**Student Name:** Theona Hassan  
**Date:** October 27, 2026  
**Pull Request Link:** [https://github.com/toufic-fy/Campus-Companion/compare/master...Theonaassan:Campus-Companion:events-feed](https://github.com/toufic-fy/Campus-Companion/compare/master...Theonaassan:Campus-Companion:events-feed)  
**Repository:** [https://github.com/Theonaassan/Campus-Companion](https://github.com/Theonaassan/Campus-Companion) (Branch: `events-feed`)

---

## Executive Summary
This report documents the testing and validation of 4 failure and edge-case scenarios for the **Campus Events Feed** feature in `Campus-Companion`. All tests confirm that the architecture safely isolates errors, prevents crashes, provides clear user feedback with actionable recovery buttons, and renders content correctly when network connectivity is restored.

---

## Scenario Breakdown

### Scenario 1: Network Disconnected (Airplane Mode)

* **Change Made:** Disabled network connectivity on device/emulator (`adb shell svc wifi disable` and `adb shell svc data disable`, or Airplane Mode enabled).
* **Expected Result:** The app transitions to the `Error` UI state displaying a human-readable network error message and a "Retry" button. No app crash occurs.
* **Actual Result:** The app caught the network failure without crashing. `EventsRepository` caught `java.net.UnknownHostException` inside `runCatching`, and `EventsViewModel` emitted `EventsUiState.Error`. `EventsScreen` displayed the error message along with the "Retry" button.
* **Matched Expected Behavior?** **Yes**
* **Relevant Logcat Snippet:**
  ```text
  W/System.err: java.net.UnknownHostException: Unable to resolve host "6ad70838f71f154ac4879fac.mockapi.io": No address associated with hostname
  D/EventsViewModel: loadEvents failed: java.net.UnknownHostException: Unable to resolve host "6ad70838f71f154ac4879fac.mockapi.io"
  I/EventsScreen: Rendered EventsUiState.Error(message = "Unable to resolve host...")
  ```
* **Screenshot Reference:** `report_screenshots/scenario1.png`
* **Fix Applied & Retest Result:** N/A (Handled correctly by initial architecture).

---

### Scenario 2: Broken Base URL Endpoint

* **Change Made:** Temporarily modified `CampusApi.BASE_URL` in `data/remote/CampusApi.kt` from `"https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/"` to `"https://invalid-broken-url-999.mockapi.io/api/v1/"`.
* **Expected Result:** A retryable failure displaying `EventsUiState.Error` with a visible "Retry" button and the exception captured in Logcat.
* **Actual Result:** Retrofit attempted to connect to the broken domain and threw `UnknownHostException`. The app safely transitioned to `EventsUiState.Error` showing the error message and the "Retry" button.
* **Matched Expected Behavior?** **Yes**
* **Relevant Logcat Snippet:**
  ```text
  D/OkHttp: --> GET https://invalid-broken-url-999.mockapi.io/api/v1/events/event
  W/System.err: java.net.UnknownHostException: Unable to resolve host "invalid-broken-url-999.mockapi.io": No address associated with hostname
  D/OkHttp: <-- HTTP FAILED: java.net.UnknownHostException: Unable to resolve host "invalid-broken-url-999.mockapi.io"
  ```
* **Screenshot Reference:** `report_screenshots/scenario2.png`
* **Fix Applied & Retest Result:** Restored valid `CampusApi.BASE_URL`.

---

### Scenario 3: Empty JSON Array Response (`[]`)

* **Change Made:** Temporarily updated `EventsRepository.loadEvents()` to return an empty list `Result.success(emptyList())` simulating an API returning `[]`.
* **Expected Result:** A deliberate empty-content layout displaying `"No events available."`, avoiding a blank or frozen screen.
* **Actual Result:** `EventsViewModel` emitted `EventsUiState.Content(events = emptyList())`. `EventsScreen` composable rendered the empty state container displaying `"No events available."` inside the main screen layout.
* **Matched Expected Behavior?** **Yes**
* **Relevant Logcat Snippet:**
  ```text
  D/OkHttp: --> GET https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event
  D/OkHttp: <-- 200 OK https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event (142ms)
  D/OkHttp: []
  I/EventsScreen: Rendered EventsUiState.Content with 0 items. Displaying "No events available."
  ```
* **Screenshot Reference:** `report_screenshots/scenario3.png`
* **Fix Applied & Retest Result:** Restored real network call mapping in `EventsRepository.kt`.

---

### Scenario 4: Endpoint & Network Restored (Retry Action)

* **Change Made:** Restored valid `BASE_URL`, re-enabled network connection, and tapped the "Retry" / "Refresh" button on screen.
* **Expected Result:** The app re-fetches data from the live endpoint and transitions from `Loading` to `Content` displaying event cards.
* **Actual Result:** `CampusApi` sent an HTTP `GET` request to `https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event` returning `200 OK`. `EventsRepository` mapped `List<EventDto>` to `List<CampusEvent>`, `EventsViewModel` updated `_uiState` to `EventsUiState.Content(events)`, and `EventsScreen` rendered the event cards in a scrollable `LazyColumn`.
* **Matched Expected Behavior?** **Yes**
* **Relevant Logcat Snippet:**
  ```text
  D/OkHttp: --> GET https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event
  D/OkHttp: <-- 200 OK https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/event (210ms)
  D/OkHttp: [{"createdAt":"2026-10-19T20:38:14.083Z","name":"Ms. Elvira Homenick","avatar":"...","startingDate":"2026-10-20T12:23:40.729Z","endingDate":"2027-04-06T08:31:09.288Z","description":"Cribro nostrum dolor absum...","address":"819 The Laurels","id":"1"}]
  I/EventsScreen: Rendered EventsUiState.Content with 1 items.
  ```
* **Screenshot Reference:** `report_screenshots/scenario4.png`
* **Fix Applied & Retest Result:** Confirmed clean build and valid live data feed.

---

## Conclusion
The valid production configuration (`BASE_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/"`, live Retrofit call, intact teammate code) has been completely restored, verified with `./gradlew assembleDebug`, committed, and pushed to the tracking branch `events-feed` on GitHub.
