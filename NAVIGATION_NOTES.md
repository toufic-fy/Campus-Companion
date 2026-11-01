# Navigation Implementation Notes

### Questions & Answers

1. **Which composable owns NavController?**
   `AppNavHost` is the sole composable that owns `navController` via `rememberNavController()`.

2. **What route opens first?**
   The `Routes.TASKS` (`"tasks"`) route opens first as the designated `startDestination` in `AppNavHost`.

3. **What callback does TasksScreen receive?**
   `TasksScreen` receives the `onOpenEvents: () -> Unit` lambda callback, keeping it completely decoupled from `NavController`.

4. **Why does system Back return to Tasks?**
   Navigation Compose automatically maintains an internal back stack. Navigating from `"tasks"` to `"events"` pushes `"events"` onto the stack, so pressing system Back pops `"events"` off and exposes `"tasks"`.

5. **Does the Events screen need to know how navigation works?**
   No, `EventsPlaceholderScreen` is a stateless composable that takes only a `Modifier` parameter and is entirely decoupled from navigation.

---

### Explanation Check

* **Navigation Decision Made:** Named the placeholder destination `EventsPlaceholderScreen` instead of `EventsScreen`.
* **Reason:** The project already contained an existing live events feed composable named `EventsScreen`. Creating `EventsPlaceholderScreen` prevented naming conflicts and preserved all existing project code intact.
