# Nav3 KMP Address Book

This is a runnable Address Book example for `laydr-nav3-kmp`.

It demonstrates Laydr as a companion to JetBrains Compose Multiplatform
Nav3. The minimal model is:

- App code navigates with generated `LaydrRoutes.*.destination(...)` values.
- Each screen route owns local `Route.kt` and `Screen.kt` files.
- `Route.kt` binds a nearby `::Screen` entrypoint and stays route wiring only.
- The app shell remembers `LaydrNavSections`.
- Nav3 still renders the selected stack with app-owned `NavDisplay`.

The example also includes typed Nav3 sections, opt-in adaptive
list/detail scenes, structured path helpers, and route-owned Compose
definitions from common source across desktop, Android, and iOS targets. The
Android and desktop modules are thin launchers for the shared app.

The route tree is:

Only `Route.kt` declares routes. `Screen.kt` files in this example are
app-owned UI entrypoints and implementation files called by those route
declarations.

```text
routes/
  contacts/
    Route.kt
    Screen.kt
    by_id/
      Route.kt
      Screen.kt
      edit/
        Route.kt
        Screen.kt
  profile/
    Route.kt
    Screen.kt
```

The app opens at `/contacts`, builds generated Laydr screen destinations,
pushes or replaces those destinations through `LaydrNavSectionsNavigator`, and
renders `wiring.selectedBackStack` with Nav3 `NavDisplay`. Each route
owns its screen render declaration in `Route.kt`, binds nearby `::Screen`
with generated app-context-free helpers, and Nav3 renders through generated
`LaydrComposeRoutes.definitions`. The app provides its small example context
with a `CompositionLocal`; route-local screen entrypoints read it from
ordinary Compose code, and Laydr does not require a route-root app context
file.

The tab shell declares Nav3 sections from `LaydrRoutes.appGraph` for
Contacts and Profile and attaches typed tab data to each section. Laydr creates
one stack per section, validates generated destinations, chooses the owning
section, switches tabs before push or replace, persists the selected section
root key, and preserves each tab's root entry on selected-stack pops. Labels
and Material navigation bar rendering remain app-owned.

The app shell opts into `laydr-nav3-kmp-adaptive` and declares one
adaptive list/detail scene from generated refs:
`LaydrRoutes.Contacts` is the list pane, `LaydrRoutes.Contacts.ById` is the
detail pane, and the app provides a placeholder for wide layouts before a
contact is selected. The same list and detail routes are used on compact
layouts, where Nav3 renders the current stack entry as a normal screen.
Direct navigation to `/contacts/{id}` initializes the stack as
`[Contacts, Contacts.ById]` so wide layouts have both entries available. The
app shell uses `wiring.canShowBack(...)` and
`wiring.listDetailStackState` for header back-button decisions instead of
inspecting raw Nav3 keys.

The header and route-local actions use generated typed destinations for normal
in-app navigation: Ada replaces with `LaydrRoutes.Contacts.ById.destination`,
Edit Ada pushes `LaydrRoutes.Contacts.ById.Edit.destination`, and Profile
replaces with `LaydrRoutes.Profile.destination`. Path helpers remain available
for strict app paths, and external target helpers accept URL-like inputs while
preserving raw query and fragment text for app-owned policy. Both return
structured rejection reasons when a target is unsupported, unknown,
layout-only, invalid, or outside every declared section.

The entry provider accepts Nav3 `NavKey` values. Stale, invalid,
foreign, and layout-only keys render the example's not-found content instead
of being redirected to a tab, with structured not-found context.

Useful checks:

```sh
./gradlew :examples:nav3-kmp:shared:checkLaydrRoutes
./gradlew :examples:nav3-kmp:androidApp:compileDebugKotlin
./gradlew :examples:nav3-kmp:desktopApp:compileKotlin
./gradlew :examples:nav3-kmp:desktopApp:run --dry-run
```
