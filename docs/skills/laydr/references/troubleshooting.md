# Troubleshooting

Start with route validation:

```bash
./gradlew :shared:checkLaydrRoutes
```

Run generation only when generated source must be inspected or compiled:

```bash
./gradlew :shared:generateLaydrRoutes
```

## Missing Route Dependencies

Generated Compose definitions do not receive a global app context. Resolve
route dependencies from app-owned Compose code such as parameters,
`CompositionLocal`, `remember`, or Compose DI.
Use `references/dependencies.md` for the default route entrypoint pattern.

## Invalid `Route.kt`

Each route directory must contain one supported route-kind declaration:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
internal val Route = LaydrRouteDef.layout(content = ::Layout)
internal val Route = LaydrRouteDef.screenAndLayout {
    screen(content = ::Screen)
    layout(content = ::Layout)
}
```

Core-only route graphs use `LaydrRouteDeclaration.*` equivalents.

## Bad Route Directories

Common route-tree failures:

- missing `Route.kt`
- layout-only route with no child route
- invalid directory name
- dynamic directory not named `by_<lowercase_snake_case>`
- duplicate generated route names
- child route under a non-route directory

Fix authored route source. Do not patch generated output.

## Missing Runtime Dependency

The Gradle plugin wires generated source but does not add runtime libraries.
Add the module used by the app:

```kotlin
implementation(libs.laydr.compose)
implementation(libs.laydr.nav3.kmp)
implementation(libs.laydr.workflow)
```

Follow the target app's version catalog or composite build pattern when it has
one.

If the target app uses Laydr artifacts from Maven local, confirm
`mavenLocal()` is present in dependency repositories.

## Plugin Not Found

If Gradle cannot resolve plugin id `dev.goquick.laydr`, either use
`mavenLocal()` in `pluginManagement.repositories` when Laydr artifacts are
already installed locally, or consume the Laydr checkout through:

```kotlin
pluginManagement {
    includeBuild("../laydr")
}

includeBuild("../laydr")
```

## Stale Generated APIs

If `LaydrRoutes`, `LaydrComposeRoutes`, or `LaydrRouteDef` is missing or
stale, rerun route validation/generation from source. If needed, use the app's
normal clean workflow and regenerate.

## Missing Generated Destination

If `LaydrRoutes.Main.destination()` or a similar generated destination is
missing, check the route kind. `destination()` is generated only for screen
routes. Change a layout-only route to `screen` when it should be a navigation
target. Use `screenAndLayout` only when it should also wrap descendants with
inherited Laydr layout behavior.

## Nav3 Rejections

External target and path helpers can reject unknown, layout-only, malformed,
or outside-section targets. Inspect the structured reason. For ordinary app
navigation, use generated destinations:

```kotlin
appState.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
```

## Missing Or Wrong Nav3 Payload

`requireLaydrNavPayload<T>()` throws, and `laydrNavPayloadOrNull<T>()`
returns `null`, when the current entry was not launched with a payload, was
process-restored, used destination-only navigation, or received a payload with
the wrong runtime type.

Pass launch-time data through `LaydrNavLaunch`:

```kotlin
stack.navigator.push(
    LaydrNavLaunch(
        destination = LaydrRoutes.Auth.SignIn.destination(),
        payload = SignInPayload(initialEmail = email),
    ),
)
```

Prefer explicit payload classes over maps or nullable values. Payloads are
transient, so route code must own fallback behavior:

```kotlin
val navigator = requireLaydrNavStackNavigator()
val payload = laydrNavPayloadOrNull<SignInPayload>()

if (payload == null) {
    LaunchedEffect(Unit) {
        navigator.back()
    }
    return
}
```

The fallback destination is app policy. Laydr exposes nullable accessors; it
does not auto-close restored entries.

## Missing Or Wrong Nav3 Result Sink

`requireLaydrNavResultSink<T>()` throws, and
`laydrNavResultSinkOrNull<T>()` returns `null`, when the current entry was not
launched with `pushForResult<T>(...)`, was process-restored, used
destination-only navigation, or was launched for another result type.

Launch the entry through `pushForResult<T>(LaydrNavLaunch(...))`:

```kotlin
stack.navigator.pushForResult<SignInResult>(
    launch = LaydrNavLaunch(
        destination = LaydrRoutes.Auth.SignIn.destination(),
    ),
) { result ->
    handleSignInResult(result)
}
```

Use `laydrNavResultSinkOrNull<T>()` when the route can recover from a
destination-only or process-restored entry:

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

Completing a result does not pop the route. Call app-owned navigation
separately when the route should close.

## Workflow Failures

Workflow hosting is app-owned screen code. Add `laydr-workflow`, host the
workflow from `Screen.kt`, and keep route navigation through generated
destinations.
