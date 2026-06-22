# Nav3 KMP Adapter

`laydr-nav3-kmp` adapts generated Laydr route descriptors to JetBrains
Compose Multiplatform Navigation3.

The module targets the JetBrains artifact family:

```kotlin
org.jetbrains.androidx.navigation3:navigation3-ui
```

It does not target Google AndroidX Navigation 3. Use
`laydr-nav3-androidx` for Android-only apps. The adapters intentionally share
navigation semantics through `laydr-nav-runtime`, but keep platform keys,
platform entries, saved-state wiring, and dependency boundaries separate.

`LaydrRouteHost` and `laydr-nav3-kmp` are alternate hosts over the same
generated route contracts. `LaydrRouteHost` remains the plain Compose host for
path-in/content-out rendering. `laydr-nav3-kmp` lets an app keep a
Nav3-owned back stack and render entries with `NavDisplay`.
Optional Material adaptive list/detail helpers live in
`laydr-nav3-kmp-adaptive`.

## Golden Path

A typical Nav3 app should need four Laydr concepts:

1. Generated screen destinations are the app-facing navigation values.
2. Route-local `Route.kt` files declare how each route renders.
3. Nav3 app state or wiring helpers own validated section stacks.
4. The app still renders Nav3 directly with `NavDisplay`.

```kotlin
val destination = LaydrRoutes.Contacts.ById.destination(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
sections.navigator.push(destination)
```

Generated destinations validate through `LaydrRoutes.appGraph` before Laydr
mutates Nav3 state. Apps do not pass raw strings, route ids, or route
maps for ordinary in-app navigation.

For app shells, declare section specs plus remembered section runtime. Static
generated screen route objects expose their default destination, so section
roots do not repeat `destination()` calls:

```kotlin
private data class TabSpec(val label: String)

val sectionSpecs = listOf(
    laydrNavSection(LaydrRoutes.Contacts, TabSpec("Contacts")),
    laydrNavSection(LaydrRoutes.Profile, TabSpec("Profile")),
)

val sections = rememberLaydrNavSections(
    routeDefinitions = LaydrComposeRoutes.definitions,
    sectionSpecs = sectionSpecs,
    notFoundContent = { notFound -> ... },
)
```

Apps that want less repeated setup can use `rememberLaydrNavSections`.
It assembles a `LaydrNavSectionSet`, scene support, selected-section state,
one stack per section, payload/result storage, and the generated entry
provider, but it still returns the pieces an app passes to `NavDisplay`:

```kotlin
val wiring = rememberLaydrNavSections(
    routeDefinitions = LaydrComposeRoutes.definitions,
    sectionSpecs = listOf(
        laydrNavSection(LaydrRoutes.Contacts, TabSpec("Contacts")),
        laydrNavSection(LaydrRoutes.Profile, TabSpec("Profile")),
    ),
    notFoundContent = { notFound -> ... },
)
```

Apps that want one validated Laydr stack without sections can use
`rememberLaydrNavStack`. It returns a `LaydrNavStack`: a managed stack owner
that exposes the Nav3 back stack, generated entry provider, scene strategies,
and a route-facing `LaydrNavStackNavigator`.

```kotlin
val stack = rememberLaydrNavStack(
    routeDefinitions = LaydrComposeRoutes.definitions,
    initialDestination = LaydrRoutes.Home.destination(),
    notFoundContent = { notFound -> ... },
)
```

`LaydrNavStack` owns Laydr route key creation, payload storage, typed result
sinks, launch metadata transport, entry cleanup, generated entry-provider
integration, and owner-facing reset. The app still owns where the stack lives,
which foreign Nav3 keys are mixed into it, how `NavDisplay` is rendered, and
what any entry metadata means.

For app-owned or mixed parent stacks, pass an existing `NavBackStack<NavKey>`:

```kotlin
val rootBackStack = rememberNavBackStack<NavKey>(MainShellRoute.Sections)
val rootStack = rememberLaydrNavStack(
    routeDefinitions = LaydrComposeRoutes.definitions,
    backStack = rootBackStack,
    notFoundContent = { notFound -> ... },
)
```

Laydr mutates only the trailing Laydr suffix after the last foreign key.
Foreign keys before that suffix are preserved. `push(...)` appends a Laydr
entry, `replace(...)` requires a Laydr key at the top, route-facing `back()`
returns `false` when the top entry is foreign, and owner-facing `reset(...)`
replaces only the trailing Laydr suffix. This lets an app keep shell marker
keys in the same Nav3 stack without making Laydr own root app policy.

The initial destination is always a generated screen destination. A route can
be both a parent directory and a stack target by declaring `screen`; it only
needs `screenAndLayout` when it also contributes inherited Laydr layout
behavior to descendant entries. Layout-only routes intentionally do not
produce destinations because Nav3 entries render screens.

Apps can opt into generated Nav3 KMP helper source:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

This emits `LaydrNavRoutes.kt` in the app's generated package. It is a
generated adapter convenience layer over `LaydrRoutes`, `LaydrComposeRoutes`,
and `laydr-nav3-kmp`:

```kotlin
val wiring = LaydrNavRoutes.rememberSections(
    sectionSpecs = listOf(
        LaydrNavRoutes.Contacts.section(TabSpec("Contacts")),
        LaydrNavRoutes.Profile.section(TabSpec("Profile")),
    ),
    notFoundContent = { notFound -> ... },
)
```

Generated stack helpers call the same runtime primitive:
`LaydrNavRoutes.rememberStack(initialDestination = ...)` creates a pure Laydr
stack, and `LaydrNavRoutes.rememberStack(backStack = ...)` attaches Laydr
management to an app-owned or mixed parent stack.

Managed stacks expose owner-facing `reset(destination)` and
`reset(LaydrNavLaunch(...))` for app-owned flows such as completing bootstrap
and entering the main app surface. Reset is not available through
`LaydrNavStackNavigator` because reset is root or owner policy.

These helpers reduce repeated shell and section setup. Route-local navigation
still passes generated destinations to runtime APIs such as
`navigator.push(destination)` or `navigator.select(route)`. Generated helpers
do not generate labels, icons, visibility rules, scaffolds, parent fullscreen
shells, retained state, or Navigation3 display policy.

`LaydrNavSection.data` carries app-owned labels, icons, visibility, or
other navigation-bar policy. Laydr validates generated routes, derives section
membership, remembers one stack per section, persists the selected section
root key with Compose saved state, and can add adaptive scene metadata when an
app passes optional scene support. Laydr does not infer section labels or
icons from route metadata.

Typed section selection is available when app UI should reveal a section
without mutating that section's stack:

```kotlin
NavigationBarItem(
    selected = section == sections.selectedSection,
    onClick = { sections.select(section) },
)

wiring.navigator.select(LaydrRoutes.Profile)
wiring.navigator.select(LaydrRoutes.Profile.destination())
```

Navigator selection validates generated destinations and declared section
ownership, then only updates selected-section state. It throws for invalid or
outside-section destinations. Owner selection by declared section object stays
on `LaydrNavSections`. String path and external-target selection stays
result-based and
non-throwing.

Dynamic section roots use the explicit-root section helper. The root
destination scopes membership, so repeated route ids are allowed when their
root parameters differ:

```kotlin
laydrNavSection(
    route = LaydrRoutes.Workspaces.ById,
    rootDestination = LaydrRoutes.Workspaces.ById.destination(
        id = LaydrRoutes.Workspaces.ById.id("alpha"),
    ),
)
```

Candidate destinations must be in the section route subtree and keep the same
root parameter values. Duplicate exact root keys are rejected.

Route-local shell navigation capabilities can depend on the small navigator
contract instead of the full controller:

```kotlin
internal class ContactsShellNavigation(
    private val navigator: LaydrNavSectionsNavigator,
) {
    fun openContact(id: String) {
        navigator.push(
            LaydrRoutes.Contacts.ById.destination(
                id = LaydrRoutes.Contacts.ById.id(id),
            ),
        )
    }

    fun openContactWithReturn(id: String) {
        navigator.pushWithReturn(
            LaydrRoutes.Contacts.ById.destination(
                id = LaydrRoutes.Contacts.ById.id(id),
            ),
        )
    }

    fun openProfile() {
        navigator.replace(LaydrRoutes.Profile.destination())
    }
}
```

External targets stay on owner-facing `LaydrNavStack` or `LaydrNavSections`
entry-point APIs, not on route-facing navigators.

Apps render the selected stack directly:

```kotlin
NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    entryProvider = sections.entryProvider,
)
```

Apps can attach app-owned Nav3 metadata to generated entries without
wrapping the returned `NavEntry`:

```kotlin
val wiring = rememberLaydrNavSections(
    routeDefinitions = LaydrComposeRoutes.definitions,
    sectionSpecs = sectionSpecs,
    entryMetadata = { context ->
        metadataForAppRoute(context.key, context.match)
    },
    notFoundContent = { notFound -> ... },
)
```

The callback receives the resolved `LaydrNavKey` and `LaydrRouteMatch` for
resolved generated screen entries. State and shell entry providers also pass
`LaydrNavSectionPlacement`, which exposes the owning section, whether the entry
is the concrete section root, and renderable screen depth below that root.
Laydr does not call metadata callbacks for foreign, unknown, invalid,
layout-only, outside-section, or not-found entries. Metadata from optional scene
support is merged first, then app metadata is merged on top, so app metadata
wins collisions including Nav3 transition metadata keys. Laydr transports the
metadata but does not assign meaning to app labels, icons, tab identity,
analytics, transitions, or visual chrome.

`LaydrNavEntryMetadataKey<Value>` is an optional app-owned safety layer over
the same raw metadata boundary. A typed key still stores a stable string name
in `NavEntry.metadata`, and typed reads are best-effort casts from
`Map<String, Any>`. The key type reduces app-side string and cast mistakes, but
it does not reserve namespaces, prevent collisions, or introduce Laydr-owned
modal, fullscreen, transition, analytics, auth, or chrome policy.

Use `sections.back()` for user-facing Back in sectioned app shells. It restores
return-aware cross-section navigation points when present and otherwise pops
the selected section stack. Use `popSelectedStack()` only when app code
intentionally wants the mechanical section-local pop. `canShowBack` combines
return-aware Back with app-owned wide list/detail pane policy so wide layouts
can hide mechanical pops while still exposing cross-section returns.

`sections.currentPath` is logical selected-stack state. It is useful for
diagnostics, tests, and app-owned state decisions, but it is not synchronized
with the exact frame Nav3 is currently rendering. Do not use it as a
global animated-screen title unless the app also owns matching transition
state. Route path or title text that must stay visually synchronized with
screen content should live inside the route entry content rendered by
`NavDisplay`.

The route-local render declarations use the same generated Compose definitions
as `LaydrRouteHost`. Generated route rendering is app-context-free; route
dependencies come from ordinary app-owned Compose code. Route dependency
lookup should normally live in route entry composables, not in generated route
definitions or shell-level environment cascades.

Managed stack route payloads are the narrow exception for launch-time data
that belongs to one Nav3 entry. `LaydrNavStackNavigator` can attach an
app-owned payload through `LaydrNavLaunch` when pushing or replacing a
generated destination, and owner-facing `LaydrNavStack.reset(...)` can attach
the same launch data while replacing the trailing Laydr suffix. The generated
entry provider exposes that value only while rendering the matching entry.
The payload is typed at the route-local read site with
`laydrNavPayloadOrNull<T>()` or `requireLaydrNavPayload<T>()`.

Payloads are intentionally transient. They are not encoded into
`LaydrRouteKey`, path parameters, route metadata, entry metadata, or generated
route declarations, and they are not serialized with Nav3 saved-state stacks.
`LaydrNavKey` may carry an internal runtime entry token so repeated
payload-bearing pushes of the same destination can be distinct stack entries,
but route matching, section membership, scene support, and path conversion use
the base route id and parameters. After process restoration, the entry renders
without a payload and the app owns the fallback behavior.

This keeps Laydr responsible only for typed entry-scoped transport. Auth
policy, bootstrap policy, result handling, modal presentation, retained state,
and dependency ownership remain app code.

Route results use the same boundary.
`LaydrNavStackNavigator.pushForResult<T>(LaydrNavLaunch(...))` registers a
transient one-shot callback for one tokenized entry, and route content can
complete or cancel it through a typed result sink. Completion delivers the
callback only; it does not pop, replace, reset, switch sections, or run
post-result app policy. If the entry leaves the stack before completion,
Laydr cancels the pending result once. Process-restored entries have no
pending result callbacks.

`LaydrNavSectionsNavigator.pushForResult<T>(LaydrNavLaunch(...))` uses the
same result boundary and adds section return semantics for cross-section
launches. The launch records the caller section stack as the next app Back
return point, so a selector route can complete or cancel its sink and then call
section Back to restore the caller. Same-section result launches remain ordinary
stack pushes with a result sink. Result completion itself still never mutates
navigation state.

Parent or root stack navigation is an explicit app-provided route capability,
not a global runtime singleton. `ProvideLaydrNavStackNavigator` installs a
`LaydrNavStackNavigator` only for the subtree where the app has approved parent
stack mutation. `laydrNavStackNavigatorOrNull()` lets reusable route content
detect that no parent capability is present, and
`requireLaydrNavStackNavigator()` fails at the route boundary with a provider
named diagnostic. Generated entry providers do not install this navigator
local; they continue to install only entry-scoped payload and result locals for
the currently rendered Laydr entry.

### Route-Adjacent Section Chrome

Section chrome can be route-adjacent without becoming route layout behavior.
Large apps may place app-owned presentational scaffold files beside the subtree
they visually frame, such as `routes/main/MainTabScaffold.kt` beside
`routes/main/Route.kt`. That placement keeps the product surface discoverable
from the route tree, but it does not move Nav3 policy into the route package.

The app shell still creates `rememberLaydrNavSections`, owns selected-section
state, app Back behavior, scene strategies, entry metadata, narrow shell
capability providers, and `NavDisplay`. It passes the presentational scaffold
tab items, selected state, callbacks, and a content slot that contains
`NavDisplay`. Feature dependencies remain route-local or feature-owned; the
shell should not become a provider cascade for unrelated routes.

Route `Layout.kt` files are the wrong ownership point for section stacks. They
wrap matched route content after Nav3 has selected an entry; they do not own
which section stack is selected, how app Back behaves, or how the selected
stack is displayed. A future Laydr helper may be considered only if it can
remove repeated low-policy composition without owning labels, icons, badges,
visibility, breakpoints, auth, DI, app Back, or product rendering policy.

### Sectioned Shells With Fullscreen Routes

Tabbed main sections plus chrome-hidden fullscreen routes should be modeled as
an app-owned parent shell above the sectioned Laydr Nav surface. The parent
shell owns which surface is visible and whether bars or rails render. The main
surface owns `rememberLaydrNavSections` and section stacks. Fullscreen entries can
still carry generated `LaydrScreenDestination` values, but they are selected by
the parent shell rather than by `LaydrNavSectionsNavigator`.

`LaydrNavSectionsNavigator` is intentionally bounded to declared sections. Route
content that opens another section should use typed `select`, `push`, or
`replace` on the section shell. Route content that opens a parent-owned
fullscreen destination should use an explicitly provided parent stack
navigator, such as `ProvideLaydrNavStackNavigator(rootStack.navigator)`.

## Optional Adaptive Scenes

Adaptive list/detail scenes are optional app-shell declarations. The app keeps
one list route, one detail route, and one route-owned UI flow; Laydr validates
the relationship and supplies Nav3 scene metadata and scene strategies.
Apps add `laydr-nav3-kmp-adaptive` only when they want this Material
adaptive behavior. On Android, that opt-in artifact inherits Material
adaptive's `compileSdk 37` requirement.

```kotlin
val adaptiveScenes = rememberLaydrNavAdaptiveScenes(
    LaydrRoutes.appGraph,
    laydrNavListDetailScene(
        list = LaydrRoutes.Contacts,
        detail = LaydrRoutes.Contacts.ById,
        detailPlaceholder = { EmptyContactDetail() },
    ),
)

val sections = rememberLaydrNavSections(
    routeDefinitions = LaydrComposeRoutes.definitions,
    sectionSpecs = sectionSpecs,
    sceneSupport = adaptiveScenes,
    notFoundContent = { notFound -> ... },
)

NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    sceneStrategies = adaptiveScenes.sceneStrategies,
    entryProvider = sections.entryProvider,
)
```

On wide layouts, Nav3 can render `[Contacts]` as the list pane plus the
declared placeholder, or `[Contacts, Contacts.ById("ada")]` as list and detail
entries together. On compact layouts, the same stack remains normal
single-entry Nav3 behavior. A direct replace or path navigation to a
registered detail route initializes that stack as `[list, detail]` so the wide
scene has both entries available. Navigation bars and rails remain section
based; a detail route under Contacts keeps the Contacts section selected.

## Advanced APIs

Apps that intentionally manage Nav3 stacks directly can convert a
generated destination to the adapter's generic serializable key:

```kotlin
LaydrRoutes.Items.ById.destination(
    id = LaydrRoutes.Items.ById.id("alpha"),
).navKey()
```

The key shape is stable and small:

```kotlin
LaydrNavKey(routeId = "Items.ById", parameters = mapOf("id" to "alpha"))
```

For one custom stack, apps can remember a managed Laydr stack from generated
Compose route definitions:

```kotlin
val stack = rememberLaydrNavStack(
    routeDefinitions = LaydrComposeRoutes.definitions,
    initialDestination = LaydrRoutes.Contacts.destination(),
    notFoundContent = { notFound -> ... },
)
```

The stack validates generated screen destinations before route-facing push or
replace operations, can owner-reset the trailing Laydr suffix to one generated
destination, can expand registered detail destinations to `[list, detail]`,
exposes the current path, exposes route-facing pop availability, and exposes
recognized list/detail stack facts. Apps still pass `stack.backStack` to
`NavDisplay` directly and use `stack.entryProvider` as
the entry provider:

```kotlin
NavDisplay(
    backStack = stack.backStack,
    onBack = { stack.navigator.back() },
    entryProvider = stack.entryProvider,
)
```

URL-like targets can enter a single stack through owner-facing external-target
helpers:

```kotlin
stack.replaceExternalTarget("/contacts/ada")
stack.pushExternalTarget("/contacts/ada/edit")
```

`pushExternalTarget(...)` and `replaceExternalTarget(...)` return
`LaydrNavExternalTargetResult`. Accepted results carry the resolved Nav3 key.
Rejected results include `UnsupportedPath`, `UnknownRoute`,
`LayoutOnlyRoute`, or `InvalidParameters`. Rejected operations leave the stack
unchanged. Generated-destination operations stay strict and throw for invalid
destinations.

URL-like targets can enter through external target helpers:

```kotlin
stack.replaceExternalTarget("https://example.test/contacts/ada?mode=preview#notes")
```

External target results preserve the original input, normalized route path,
raw query, and raw fragment. Query, fragment, scheme, and authority never
participate in route matching. Laydr does not own platform deep-link dispatch,
browser history, host-as-route conventions, query parsing, or fragment
routing.

The entry provider can also be created directly:

```kotlin
laydrNavEntryProvider(
    routeDefinitions = LaydrComposeRoutes.definitions,
    notFoundContent = { notFound -> ... },
)
```

The entry provider returns Nav3 KMP's `(NavKey) -> NavEntry<NavKey>`
shape. It consumes the same generated route-owned Compose definitions used by
`LaydrRouteHost`. Layout wrapping and screen-owned layout values follow the
same inherited layout semantics as `LaydrRouteHost`. Nav3 key
resolution is synchronous, but route-owned screen definitions are invoked
inside the `NavEntry` composable body so screens can derive layout values from
Compose state. Invalid, unknown, foreign, and layout-only keys render the
app-provided not-found content. The callback receives
`LaydrNavNotFound` with the raw key, optional Laydr key, optional
display path, and a reason enum. When adaptive scenes are configured, the same
entry provider attaches metadata only to resolved matching list or detail
screen routes; not-found entries receive no scene metadata.

Nav3 KMP saved-state back stacks need the Laydr key serializer
registered in the runtime configuration. The controller supplies Laydr's
configuration by default; low-level stack code can still use it directly:

```kotlin
val backStack = rememberNavBackStack(
    laydrNavSavedStateConfiguration(),
    LaydrRoutes.Contacts.destination().navKey(),
)
```

Low-level adapter APIs consume `LaydrRoutes.routeMap`:

```kotlin
laydrNavKeyForPath(path, LaydrRoutes.routeMap)
key.path(LaydrRoutes.routeMap)
```

`laydrNavKeyForPath(...)` returns keys only for screen routes. Paths
that match layout-only routes return `null` because Nav3 entries render
screens, not layout wrappers by themselves. Use these helpers when app code
intentionally manages Nav3 keys or stacks directly; controller path
APIs are the preferred entry point when the Laydr controller owns stack
mutation.

## Sectioned Path And Target Helpers

Tabbed shells can declare Nav3 sections from generated route refs and
app-owned section data:

```kotlin
val sectionSet = rememberLaydrNavSectionSet(
    LaydrRoutes.appGraph,
    laydrNavSection(LaydrRoutes.Contacts, TabSpec("Contacts")),
    laydrNavSection(LaydrRoutes.Profile, TabSpec("Profile")),
)
```

The section runtime creates one validated stack per declared section and
selects the owning section before mutating a stack:

```kotlin
val sections = rememberLaydrNavSections(
    routeDefinitions = LaydrComposeRoutes.definitions,
    sectionSpecs = sectionSpecs,
    notFoundContent = { notFound -> ... },
)

NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    entryProvider = sections.entryProvider,
)
```

Route-facing section navigation can pass generated destinations directly to
`sections.navigator.push(destination)` or
`sections.navigator.replace(destination)`. The runtime
validates the destination against `LaydrRoutes.appGraph`, derives the declared
owning section from the generated route graph, switches the selected section,
and mutates only that section's stack. Destinations outside every declared
section fail before stack mutation. Use `pushWithReturn(destination)` or
`replaceWithReturn(destination)` on `sections.navigator` when a cross-section
drill-in should let `sections.back()` restore the source section and target
section stacks. Return points are saved with Compose saved state alongside the
selected section and Nav3 back stacks when the recorded stacks contain Laydr
Nav keys. Apps still own Material navigation bars, labels, icons, resources,
visibility, ordering, breakpoint policy, and not-found rendering.

Apps that own a parent fullscreen Nav3 stack can keep that stack app-owned
while using typed Laydr helpers: `pushLaydr`, `replaceTopWithLaydr`,
`replaceTopLaydrIf`, `topIsLaydrRoute`, `topIsInLaydrRouteTree`, and
`removeEntriesAboveLast`.

External path strings can enter a tabbed shell through the same section graph:

```kotlin
sectionSet.sectionForPath("/contacts/ada")

sections.selectPath("/profile")
sections.replacePath("/contacts/ada")
sections.pushPath("/contacts/ada/edit")
```

Path operations return `LaydrNavPathResult` and leave the selected
section plus all stacks unchanged when a path is unsupported, unknown,
layout-only, invalid, or outside every declared section. The adapter does not
own browser history, Android intent handling, iOS universal links, query
strings, fragments, or not-found UI.

The section runtime has matching URL-like target helpers:

```kotlin
sectionSet.sectionForExternalTarget("/contacts/ada?mode=preview")

sections.selectExternalTarget("/profile#top")
sections.replaceExternalTarget("https://example.test/contacts/ada")
sections.pushExternalTarget("/contacts/ada/edit?source=mail")
```

External target operations return `LaydrNavExternalTargetResult` and
leave selection plus stacks unchanged on rejection. `sections.canReturn`,
`sections.canPopSelectedStack`, `sections.canShowBack(...)`, and
`sections.listDetailStackState` expose stack facts for app bars and adaptive
shell decisions, but the app still owns breakpoint and Back affordance policy.

Apps that use generated route-owned definitions must enable Compose generation
in their shared module:

```kotlin
laydr {
    compose.set(true)
}
```

The adapter does not own navigation policy, retained state, ViewModels,
breakpoint choice, browser history, or deep links. Apps continue to own those
Nav3 decisions.
