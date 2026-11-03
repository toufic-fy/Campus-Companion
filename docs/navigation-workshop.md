# Workshop: Tasks → Events navigation

## What changed

| Step | Where |
|------|-------|
| 1. Add `androidx.navigation:navigation-compose:2.10.2` | `gradle/libs.versions.toml`, `app/build.gradle.kts` |
| 2. Create `CampusApp` with `rememberNavController()` | `ui/navigation/CampusApp.kt` |
| 3. `NavHost` with `tasks` as the start destination | `CampusApp` → `startDestination = CampusRoutes.TASKS` |
| 4. Register the `events` destination | `composable(CampusRoutes.EVENTS) { EventsScreen(...) }` |
| 5. Tasks button calls `navigate("events")` | `TasksScreen(onOpenEvents = ...)` → "Campus events" button |
| 6. System Back + top-app-bar Back | Back stack handles system Back; the arrow calls `navController.popBackStack()` |

The separate `EventActivity` (opened with an `Intent`) was removed. The app now has a
single activity, and both screens are destinations in one navigation graph.
The Events feed itself (Retrofit → repository → `CampusEventViewModel`) is unchanged.

## Manual test (user journey)

1. Launch the app → **Tasks** opens first.
2. Tap **Campus events** → **Events** appears and loads the feed.
3. Press system **Back** → returns to **Tasks**.
4. Open Events again, tap the **←** arrow in the top bar → returns to **Tasks**.
5. Press Back on Tasks → the app closes (Tasks is the root of the stack).
6. Double-tap **Campus events** quickly, then press Back once → you are on Tasks
   (only one Events entry was pushed).

## Verification questions

**Which composable owns NavController?**
`CampusApp`. It calls `rememberNavController()` and passes the controller only to the
`NavHost`. `TasksScreen` and `EventsScreen` never receive it.

**What route opens first?**
`"tasks"` (`CampusRoutes.TASKS`), set as `startDestination` of the `NavHost`.

**What callback does TasksScreen receive?**
`onOpenEvents: () -> Unit`. The NavHost implements it as `navController.navigate("events")`.

**Why does system Back return to Tasks?**
`navigate("events")` pushes Events on top of Tasks in the back stack. The NavHost
registers a back handler that pops the top entry, so Back removes Events and shows
Tasks again. The top-bar arrow does the same thing explicitly with `popBackStack()`.

**Does your Events screen need to know how navigation works?**
No. `EventsScreen` only gets an `onBack: () -> Unit` lambda. It does not know about
routes or the NavController, so it can be previewed, tested or reused anywhere.

## Explanation check — one navigation decision and its reason

**Screens receive callbacks (`onOpenEvents`, `onBack`) instead of the `NavController`.**
Navigation stays in one place (`CampusApp`), so the graph is easy to read and change.
The screens stay plain composables that are easy to preview and test with a fake lambda.

A second decision: `navigate("events") { launchSingleTop = true }` prevents a fast
double tap from pushing two copies of Events, which would otherwise need two Backs
to return to Tasks.
