# Campus Companion — Kris Andary

## Starting point and attribution

Workshop 1: https://github.com/toufic-fy/Campus-Companion/pull/29 (preserved on its existing branch).
This continuation uses the updated class master (15d31b2) and, as requested, Anthony Kerbage / Kerbagio’s Workshop 2 Room + WorkManager work:
https://github.com/toufic-fy/Campus-Companion/pull/66
Original commits f585f0e and a4cc9f1 remain in this branch’s history with their original author. They supply the Room events cache, migration, durable refresh and status. The current class base already includes the Retrofit feed and an initial navigation graph.

## Workshop 3 — Navigation Compose

MainActivity hosts CampusApp; CampusApp owns one remembered NavController. AppNavHost starts on `tasks`, opens `events`, and scopes the events ViewModel to that destination. The real cached feed is reused.

Tasks exposes `onOpenEvents` so it expresses user intent without depending on navigation machinery; previews/tests can supply a simple callback. The graph calls navigate with launchSingleTop to avoid duplicate Events entries. Both system Back and the visible toolbar Back pop the Events entry, revealing Tasks; we never navigate to another Tasks entry to return.

Manual journey: launch → Tasks → Campus events → Events → toolbar Back → Tasks; repeat using system Back. Tasks and dark-mode preference remain saved.

Validation results are recorded after running the checks, not inferred from another student’s PR.
