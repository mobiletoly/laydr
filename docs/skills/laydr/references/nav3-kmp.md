# Nav3 KMP

Use this reference for apps using JetBrains Compose Multiplatform Navigation3
with Laydr. Use `nav3-androidx.md` for Android-only Google AndroidX
Navigation 3 apps.

## Golden Path

Prefer generated destinations:

```kotlin
sections.navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
```

Nav3 remains the runtime that owns back stacks and display:

```kotlin
NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    entryProvider = sections.entryProvider,
)
```

## Sections

```kotlin
val sectionSpecs = listOf(
    laydrNavSection(LaydrRoutes.Contacts, TabSpec("Contacts")),
    laydrNavSection(LaydrRoutes.Profile, TabSpec("Profile")),
)

val sections = rememberLaydrNavSections(
    routeDefinitions = LaydrComposeRoutes.definitions,
    sectionSpecs = sectionSpecs,
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

Section data is app-owned. Keep labels, icons, visibility, badges, ordering,
and navigation chrome outside Laydr.

Use typed section selection when an action should reveal a section without
mutating any stack:

```kotlin
sections.select(section)
wiring.navigator.select(LaydrRoutes.Profile.destination())
```

Typed selection throws for invalid or outside-section destinations. Path and
external-target selection returns structured accepted/rejected results.

## Wiring Helper

Use `rememberLaydrNavSections` to assemble a `LaydrNavSectionSet`, adaptive
scenes, selected-section state, payload/result storage, and entry provider
while still rendering with app-owned `NavDisplay`.

Use `rememberLaydrNavStack` when the app wants one validated Laydr stack
without sections:

```kotlin
val stack = rememberLaydrNavStack(
    routeDefinitions = LaydrComposeRoutes.definitions,
    initialDestination = LaydrRoutes.Home.destination(),
    notFoundContent = { notFound -> NotFound(notFound) },
)

NavDisplay(
    backStack = stack.backStack,
    onBack = { stack.navigator.back() },
    sceneStrategies = stack.sceneStrategies,
    entryProvider = stack.entryProvider,
)
```

For auth or bootstrap flows that replace the whole root stack, reset the
single stack to a generated destination:

```kotlin
stack.reset(LaydrRoutes.Main.destination())
```

Use `LaydrNavLaunch` for transient launch data and app-owned entry metadata
that belongs to one Nav3 entry:

```kotlin
stack.navigator.push(
    LaydrNavLaunch(
        destination = LaydrRoutes.Auth.SignIn.destination(),
        payload = SignInPayload(
            initialEmail = email,
            source = AuthLaunchSource.Boot,
        ),
        entryMetadata = LaydrNavEntryMetadata(
            AuthPresentationKey to AuthPresentation.Modal,
        ),
    ),
)
```

Prefer typed app-owned metadata keys when route or shell code reads metadata:

```kotlin
internal val AuthPresentationKey =
    laydrNavEntryMetadataKey<AuthPresentation>("app:presentation")

val presentation = entry.metadata[AuthPresentationKey]
```

Typed keys still write to Nav3's raw `Map<String, Any>` metadata. They do not
reserve key names, prevent collisions, or make modal, fullscreen, transition,
analytics, or chrome policy a Laydr concern.

For mixed parent stacks, pass the app-owned `NavBackStack<NavKey>` to
`rememberLaydrNavStack(backStack = ...)`. Laydr mutates only the trailing
Laydr suffix after the last foreign key. Foreign keys remain app-owned and
must be rendered by the app's `NavDisplay` entry provider.

Read the current entry payload inside route-local Compose content:

```kotlin
@Composable
internal fun Screen(route: LaydrRoutes.Auth.SignIn.Destination) {
    val payload = laydrNavPayloadOrNull<SignInPayload>()
        ?: SignInPayload(initialEmail = "", source = AuthLaunchSource.Unknown)

    SignInScreen(initialEmail = payload.initialEmail)
}
```

Use `requireLaydrNavPayload<T>()` only when the route cannot render without
the payload. Payloads are transient, entry-scoped, and not saved state,
route identity, route metadata, dependencies, modal policy, or result
handling. Process-restored entries have no payload; app code owns fallback
behavior.

Use `pushForResult<T>` when the caller needs a one-shot answer from the
specific entry it launched:

```kotlin
stack.navigator.pushForResult<SignInResult>(
    launch = LaydrNavLaunch(
        destination = LaydrRoutes.Auth.SignIn.destination(),
        payload = SignInPayload(initialEmail = email),
    ),
    onCancel = { signInCanceled() },
) { result ->
    handleSignInResult(result)
}
```

The route entry completes or cancels the pending result locally:

```kotlin
@Composable
internal fun Screen(route: LaydrRoutes.Auth.SignIn.Destination) {
    val navigator = requireLaydrNavStackNavigator()
    val resultSink = requireLaydrNavResultSink<SignInResult>()

    SignInScreen(
        onSignedIn = { userId ->
            resultSink.complete(SignInResult.SignedIn(userId))
            navigator.back()
        },
        onDismiss = {
            resultSink.cancel()
            navigator.back()
        },
    )
}
```

This is the v0 route-result surface. Completion only invokes the callback; it
does not pop the stack or choose the next route. Pending results are transient
and cancel once if their entry leaves the stack without completion.

For `LaydrNavSectionsNavigator`, `pushForResult` also records a return point
when the result launch crosses from one section to another. The launched route
still completes or cancels its sink locally and explicitly calls
`navigator.back()` when it should leave; Back restores the caller section stack.
Same-section result launches behave like ordinary stack pushes with a result
sink.

For required payloads or result sinks, route code should use nullable accessors
and recover explicitly when a destination-only or process-restored entry lacks
transient state:

```kotlin
val navigator = requireLaydrNavStackNavigator()
val resultSink = laydrNavResultSinkOrNull<SignInResult>()

if (resultSink == null) {
    LaunchedEffect(Unit) {
        navigator.back()
    }
    return
}
```

The recovery destination, Back versus reset, and any recovery UI are app
policy. Do not add or recommend helpers that auto-close while reading a
payload or result sink.

The managed stack still does not render `NavDisplay` or own app root policy.
`LaydrNavStackNavigator` exposes route-facing push, replace, Back, and
push-for-result only. Owner-facing reset and external-target helpers stay on
`LaydrNavStack`.

When route content needs to launch onto an app-approved parent or root stack,
wrap the approved subtree with `ProvideLaydrNavStackNavigator(rootStack.navigator)`.
Inside that subtree, use `requireLaydrNavStackNavigator()` for required parent
stack workflows or `laydrNavStackNavigatorOrNull()` for optional behavior:

```kotlin
val parentNavigator = requireLaydrNavStackNavigator()

parentNavigator.pushForResult<SignInResult>(
    launch = LaydrNavLaunch(
        destination = LaydrRoutes.Auth.SignIn.destination(),
        payload = SignInPayload(initialEmail = email),
    ),
    onCancel = { signInCanceled() },
) { result ->
    handleSignInResult(result)
}
```

Do not invent a global root navigator or thread feature-specific root
callbacks through generated route contexts. The app chooses where to provide
the parent stack capability. Generated entry providers install payload and
result locals for the current entry, but they do not install parent stack
navigators implicitly.

When the app enables generated Nav3 KMP helpers:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

prefer `LaydrNavRoutes` for repeated section wiring:

```kotlin
val wiring = LaydrNavRoutes.rememberSections(
    sectionSpecs = listOf(
        LaydrNavRoutes.Contacts.section(TabSpec("Contacts")),
        LaydrNavRoutes.Profile.section(TabSpec("Profile")),
    ),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

Use `LaydrNavRoutes.rememberStack(initialDestination = ...)` for generated
single-stack setup, or `LaydrNavRoutes.rememberStack(backStack = ...)` when
the app owns a mixed parent stack.

The initial destination must be a generated screen destination. If a parent
route is also a navigation target, declare it as `screen` even when it has
child routes. Use `screenAndLayout` only when that parent also contributes
inherited Laydr layout behavior to descendants. Layout-only routes do not
generate `destination()` because they are not renderable stack entries.

Dynamic section roots require an explicit root destination:

```kotlin
LaydrNavRoutes.Workspaces.ById.section(
    rootDestination = LaydrRoutes.Workspaces.ById.destination(
        id = LaydrRoutes.Workspaces.ById.id("alpha"),
    ),
    sectionData = TabSpec("Workspace"),
)
```

## Entry Metadata

Use `entryMetadata` on `rememberLaydrNavSections` or `laydrNavEntryProvider`
when an app-owned `NavDisplay` needs metadata for
transitions, analytics tags, route-depth classification, or chrome hints:

```kotlin
internal val ChromeModeKey =
    laydrNavEntryMetadataKey<ChromeMode>("app:chrome")

entryMetadata = { context ->
    mapOf(
        ChromeModeKey to chromeModeForAppRoute(
            key = context.key,
            match = context.match,
            section = context.placement.section,
            depth = context.placement.depthFromSectionRoot,
        ),
    )
}
```

Section runtime callbacks receive a `LaydrNavKey`, the resolved
`LaydrRouteMatch`, route placement, and section placement. Use placement
instead of hard-coded route-id switches for tab identity, root checks, and
route depth. Standalone `laydrNavEntryProvider` receives route placement too.
Not-found, foreign, invalid, layout-only, or outside-section keys do not
receive app metadata.
Adaptive scene metadata is merged first and app metadata wins key collisions,
including transition keys. Laydr transports metadata but does not own labels,
icons, tabs, analytics, or visual chrome policy.
Typed metadata keys are optional app-owned helpers over the same raw metadata
map:

```kotlin
val chromeMode = entry.metadata[ChromeModeKey]
```

## Sectioned Shell Plus Fullscreen Routes

For tabbed main sections plus chrome-hidden fullscreen destinations, keep
`rememberLaydrNavSections` inside the main section surface and add an app-owned
parent shell above it. The parent shell decides when chrome is hidden and opens
fullscreen generated destinations. `LaydrNavSectionsNavigator` is bounded to
declared sections; use an explicitly provided parent stack navigator for
fullscreen routes outside those sections.

When the parent shell owns a `MutableList<NavKey>`, prefer `pushLaydr`,
`replaceTopWithLaydr`, `replaceTopLaydrIf`, route predicates, and
`removeEntriesAboveLast` over constructing `LaydrNavKey` or comparing route id
strings by hand.

## App Back

Use `sections.back()` for user-facing Back in sectioned shells.

Use `popSelectedStack()` only for mechanical selected-section pops.

Use `canGoBack` for a general Back affordance and `canReturn` when the UI must
distinguish cross-section return points from ordinary stack pops.

Do not use `currentPath` as a visually synchronized animated title unless the
app also owns matching transition state.

## Route-Local Navigator

Route-local shell navigation capabilities can depend on
`LaydrNavSectionsNavigator`:

```kotlin
navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
navigator.pushWithReturn(LaydrRoutes.Profile.destination())
navigator.replace(LaydrRoutes.Contacts.destination())
```

Use return-aware operations when app Back should return to the source section
stack.
Use `pushForResult` when the caller needs a one-shot answer from the launched
entry. Cross-section section result launches are already return-aware; do not
invent a separate wrapper or thread callbacks through route contexts.

## Adaptive Scenes

Use list/detail scene specs when the same routes should render as a two-pane
scene on wide layouts:

```kotlin
laydrNavListDetailScene(
    list = LaydrRoutes.Contacts,
    detail = LaydrRoutes.Contacts.ById,
    detailPlaceholder = { EmptyContactDetail() },
)
```

Pass scene strategies to `NavDisplay`.

## External Targets

Use path and external-target helpers for app entry points, not normal in-app
navigation. Rejected operations should leave stacks unchanged and surface the
structured rejection reason.
