# Troubleshooting

Start with the route check for the module that owns `routes/`:

```sh
./gradlew :shared:checkLaydrRoutes
```

For Android-only modules:

```sh
./gradlew :app:checkLaydrRoutes
```

Use `generateLaydrRoutes` when you need generated source to compile or
inspect. Do not repair generated files by hand.

## Start Here

Use this first pass before reading the whole page:

| Symptom | Go to |
| --- | --- |
| Gradle cannot resolve `dev.goquick.laydr` | [The Laydr Plugin Is Not Found](#the-laydr-plugin-is-not-found) |
| `checkLaydrRoutes` says the route root is missing | [The Route Root Is Missing](#the-route-root-is-missing) |
| `Route.kt` cannot resolve `LaydrRouteDef` | [`LaydrRouteDef` Is Missing](#laydrroutedef-is-missing) |
| `LaydrRoutes` is missing a route you added | [Generated APIs Look Stale](#generated-apis-look-stale) |
| generated source compiles but runtime classes are missing | [Runtime Dependency Is Missing](#runtime-dependency-is-missing) |
| `LaydrNavRoutes` cannot be imported | [`LaydrNavRoutes` Is Missing](#laydrnavroutes-is-missing) |
| Nav3 payload or result access fails | [Payloads](#a-nav3-payload-is-missing-or-has-the-wrong-type) or [Results](#a-nav3-result-sink-is-missing-or-has-the-wrong-type) |
| workflow APIs are unavailable | [Workflow Setup Fails](#workflow-setup-fails) |

If the symptom is unclear, run `checkLaydrRoutes` first. Scanner errors point
to route-tree problems. Kotlin compile errors usually point to generated API,
runtime dependency, or source-set wiring problems.

## The Laydr Plugin Is Not Found

Symptom: Gradle cannot resolve plugin id `dev.goquick.laydr`.

Cause: the app's plugin repositories cannot resolve the Laydr plugin marker,
or the version catalog points at a version that is not available.

Fix: check the `laydr` plugin version in the version catalog and make sure
`pluginManagement.repositories` contains the repository that hosts the Laydr
plugin marker, such as `gradlePluginPortal()` or `mavenCentral()`.

## The Route Root Is Missing

Symptom: `checkLaydrRoutes` reports that the route root does not exist.

Cause: Laydr is looking in the default route root for the module type.

Fix: create the route root in the right source set:

```text
src/commonMain/kotlin/routes  # KMP
src/main/kotlin/routes        # Android-only
```

Or set `routesDirectory` explicitly when the app intentionally uses a
different source layout.

## `Route.kt` Has No Route Kind

Symptom: `Route.kt must declare exactly one route kind`.

Cause: the route file does not contain exactly one supported route
declaration.

Fix: declare one route kind:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
internal val Route = LaydrRouteDef.screenWithLayoutValues(content = ::Screen)
internal val Route = LaydrRouteDef.layout(content = ::Layout)
internal val Route = LaydrRouteDef.screenAndLayout {
    screen(content = ::Screen)
    layout(content = ::Layout)
}
```

For core-only generation, use `LaydrRouteDeclaration.screen`,
`LaydrRouteDeclaration.layout`, or
`LaydrRouteDeclaration.screenAndLayout`.

## `LaydrRouteDef` Is Missing

Symptom: `Route.kt` cannot resolve `LaydrRouteDef`.

Cause: Compose generation is not enabled, or generated source has not been
created for the route package yet.

Fix: enable Compose generation and run validation or generation:

```kotlin
laydr {
    compose.set(true)
}
```

```sh
./gradlew :shared:checkLaydrRoutes
./gradlew :shared:generateLaydrRoutes
```

## A Segment Directory Contains Kotlin Files

Symptom: validation reports `Kotlin files under route segment directories
require Route.kt`.

Cause: a directory under `routes/` can be namespace-only only when it contains
child segment directories and no direct Kotlin files.

Fix: add `Route.kt` if the directory is a screen or layout. If it is only a
path grouping segment, move implementation files beside a declared descendant
route or outside the route tree.

## A Dynamic Directory Is Rejected

Symptom: validation says a dynamic segment directory must be `by_` plus a
lowercase snake case parameter.

Cause: dynamic segments must be named like `by_id` or `by_user_id`.

Fix:

```text
routes/users/by_user_id/Route.kt
```

Then call the generated lower camel parameter:

```kotlin
LaydrRoutes.Users.ByUserId.destination(
    userId = LaydrRoutes.Users.ByUserId.userId("ada"),
)
```

## A Layout Route Is A Leaf

Symptom: validation reports that a layout-only route directory must contain
child routes.

Cause: a `layout` route wraps declared descendants but is not itself a screen
endpoint.

Fix: add a declared descendant route, or change the declaration to `screen`
when the directory should be addressable. Use `screenAndLayout` only when the
route should also wrap descendant content with inherited Laydr layout
behavior.

## A Generated Destination Is Missing

Symptom: `LaydrRoutes.Main.destination()` does not compile.

Cause: `destination()` is generated only for screen routes. Layout-only routes
cannot be pushed onto a stack.

Fix: declare the route as `screen` if it should be a navigation target. Use
`screenAndLayout` if it should be both a navigation target and an inherited
layout wrapper.

## Generated Names Collide

Symptom: validation reports duplicate generated route names, or generated API
names are not what you expected.

Cause: two route directories normalize to the same generated Kotlin name.

Fix: rename one directory so every route has a unique generated name. Keep
directory names lowercase snake case.

## Runtime Dependency Is Missing

Symptom: generated source exists, but compilation cannot resolve
`LaydrRouteHost`, Nav3 APIs, or workflow Compose APIs.

Cause: the Gradle plugin wires generated source but does not add runtime
dependencies.

Fix: add the runtime modules your app uses:

```kotlin
commonMain.dependencies {
    implementation(libs.laydr.compose)
}
```

Add Nav3 KMP only when the KMP module uses Nav3 KMP:

```kotlin
commonMain.dependencies {
    implementation(libs.laydr.nav3.kmp)
}
```

Add workflow only when a route hosts route-local workflow state:

```kotlin
commonMain.dependencies {
    implementation(libs.laydr.workflow)
}
```

For Android-only apps:

```kotlin
dependencies {
    implementation(libs.laydr.workflow)
}
```

For Android-only AndroidX Navigation 3 apps:

```kotlin
dependencies {
    implementation(libs.laydr.compose)
    implementation(libs.laydr.nav3.androidx)
}
```

For Android-only `LaydrRouteHost` apps, keep `laydr-compose` and omit
`laydr-nav3-androidx`.

## Android compileSdk Is Too Low

Symptom: `checkAarMetadata` reports that AndroidX Navigation 3 or
`androidx.compose.material3.adaptive:*` requires a newer `compileSdk`.

Cause: the app depends on AndroidX Navigation 3, or on
`laydr-nav3-kmp-adaptive`, which opts into Material adaptive list/detail
scenes.

Fix: raise the Android consumer's `compileSdk` to the floor required by the
resolved AndroidX artifacts. If the only failing dependency is the adaptive
artifact and the app does not use adaptive scenes, remove
`laydr-nav3-kmp-adaptive`.

## Both Nav3 Helper Targets Are Enabled

Symptom: route validation fails because `adapters.nav3Kmp` and
`adapters.nav3Androidx` are both enabled.

Cause: one route tree can generate only one app-facing `LaydrNavRoutes` helper
object.

Fix: choose the adapter for the module:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)       // KMP module
        nav3Androidx.set(false)
    }
}
```

or:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Androidx.set(true)  // Android-only module
        nav3Kmp.set(false)
    }
}
```

## Nav3 Helpers Need Compose Generation

Symptom: route validation fails after enabling `adapters.nav3Kmp` or
`adapters.nav3Androidx`.

Cause: generated Nav3 entry providers need `LaydrComposeRoutes`, which is
created only when `compose.set(true)` is enabled.

Fix:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

Use `nav3Androidx.set(true)` instead when the route tree belongs to an
Android-only module.

## `LaydrNavRoutes` Is Missing

Symptom: app code cannot import `LaydrNavRoutes`.

Cause: no generated Nav3 helper target is enabled, or both targets were
disabled.

Fix: enable exactly one Nav3 helper target for the route tree:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

Use `nav3Androidx.set(true)` instead for Android-only apps.

## Generated APIs Look Stale

Symptom: `LaydrRoutes` is missing a route you added, or a removed route is
still visible in generated source.

Cause: generated output has not been refreshed.

Fix: run route validation or generation from source:

```sh
./gradlew :shared:checkLaydrRoutes
./gradlew :shared:generateLaydrRoutes
```

If stale files persist, delete build output through normal Gradle clean
workflows and regenerate. Do not edit generated files.

## Route Dependencies Are Missing

Symptom: route validation passes, but a route-local screen or layout cannot
access app services.

Cause: generated Compose route definitions are app-context-free. Route-local
composables must resolve dependencies from app-owned Compose code.

Fix: use normal Compose tools such as explicit parameters,
`CompositionLocal`, `remember`, ViewModels, or app-owned DI. See
[Route Dependencies](route-dependencies.md).

## Nav3 Rejects A Path Or External Target

Symptom: `pushExternalTarget`, `pushPath`, `replaceExternalTarget`, or a
similar helper returns a rejected result.

Cause: the target may be unknown, malformed, layout-only, outside declared
sections, or invalid for a dynamic parameter.

Fix: inspect the structured rejection reason. For ordinary in-app navigation,
use a generated screen destination instead.

Use external target helpers for strict app entry points, not normal button
navigation. See [Advanced Navigation Topics](navigation/advanced.md).

## A Nav3 Payload Is Missing Or Has The Wrong Type

Symptom: `requireLaydrNavPayload<T>()` throws, or
`laydrNavPayloadOrNull<T>()` returns `null`.

Cause: the current entry was not launched with a payload, was process-restored,
was launched through a destination-only API, or was launched with a payload
whose runtime type is not assignable to `T`.

Fix: pass the payload through `LaydrNavLaunch` when the route needs transient
launch-time data.

Use nullable accessors when the route can recover from missing transient data:

```kotlin
val payload = laydrNavPayloadOrNull<SignInPayload>()
```

The recovery behavior is app policy.
See [Payloads And Results](navigation/payloads-results.md).

## A Nav3 Result Sink Is Missing Or Has The Wrong Type

Symptom: `requireLaydrNavResultSink<T>()` throws, or
`laydrNavResultSinkOrNull<T>()` returns `null`.

Cause: the current entry was not launched with `pushForResult<T>(...)`, was
process-restored, used destination-only navigation, or was launched for a
different result type.

Fix: launch with `pushForResult<T>(LaydrNavLaunch(...))`.

Use the same result type in the route-local sink. Completing a result does not
pop the route; call app-owned navigation separately when the route should
close. See [Payloads And Results](navigation/payloads-results.md).

## Workflow Setup Fails

Symptom: workflow host APIs are unavailable, or workflow outputs do not trigger
app behavior.

Cause: workflow hosting is app-owned. Laydr does not generate workflow host
glue or navigation behavior.

Fix: add `laydr-workflow`, host the workflow from a route-owned screen
composable, collect outputs there, and map outputs to app behavior.

```kotlin
commonMain.dependencies {
    implementation(libs.laydr.workflow)
}
```

For Android-only apps:

```kotlin
dependencies {
    implementation(libs.laydr.workflow)
}
```
