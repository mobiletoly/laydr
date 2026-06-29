# Troubleshooting

Start with route validation for the module that owns `routes/`:

```bash
./gradlew :shared:checkLaydrRoutes
```

Android-only modules commonly use:

```bash
./gradlew :app:checkLaydrRoutes
```

Run `generateLaydrRoutes` only when generated source must be inspected or
compiled. Do not repair generated files by hand.

## Contents

- symptom map
- route declaration failures
- missing generated APIs
- missing runtime dependencies
- plugin resolution
- Nav3 adapter flags
- external targets
- payloads and results
- workflow failures

## Symptom Map

| Symptom | Likely owner |
| --- | --- |
| route root missing | plugin config or source-set layout |
| `Route.kt` has no route kind | authored route declaration |
| `LaydrRouteDef` missing | Compose generation or generated source |
| `LaydrRoutes` stale | generated output not refreshed |
| `LaydrRouteHost` or Nav3 classes missing | runtime dependency |
| `LaydrNavRoutes` missing | Nav3 adapter flag |
| external target rejected | app entry-point path or section ownership |
| payload/result sink missing | transient Nav3 entry state |
| workflow APIs missing | missing `laydr-workflow` or wrong source set |

Scanner errors usually mean route-tree problems. Kotlin compile errors usually
mean stale generated APIs, missing dependencies, or source-set wiring.

## Route Declaration Failures

Each `Route.kt` must contain exactly one supported declaration:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
internal val Route = LaydrRouteDef.screenWithLayoutValues(content = ::Screen)
internal val Route = LaydrRouteDef.layout(content = ::Layout)
internal val Route = LaydrRouteDef.screenAndLayout {
    screen(content = ::Screen)
    layout(content = ::Layout)
}
```

Core-only route graphs use `LaydrRouteDeclaration.*` equivalents.

Layout-only routes must have declared descendants and do not generate
`destination()`. Use `screen` for navigation targets, or `screenAndLayout`
when the route is both a target and an inherited layout.

Namespace-only directories must not contain direct Kotlin files. Add `Route.kt`
if the directory is a real screen/layout route, or move files beside a
declared descendant.

## Missing Generated APIs

If `LaydrRoutes`, `LaydrComposeRoutes`, or route-local `LaydrRouteDef` is
missing or stale:

1. Confirm the Laydr plugin is applied to the route-owning module.
2. Confirm the route root exists in the correct source set.
3. Confirm `compose.set(true)` when route files use `LaydrRouteDef`.
4. Run route validation or generation.
5. Refresh the Gradle model if the IDE is stale.

```bash
./gradlew :shared:checkLaydrRoutes
./gradlew :shared:generateLaydrRoutes
```

## Missing Runtime Dependency

The plugin wires generated source but does not add runtime libraries.

KMP dependencies belong in the route-owning source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.compose)
            implementation(libs.laydr.nav3.kmp)
            implementation(libs.laydr.workflow)
        }
    }
}
```

Android-only apps use normal Android dependencies:

```kotlin
dependencies {
    implementation(libs.laydr.compose)
    implementation(libs.laydr.nav3.androidx)
    implementation(libs.laydr.workflow)
}
```

Add only the runtime modules the app actually uses.

## Plugin Resolution

If Gradle cannot resolve plugin id `dev.goquick.laydr`, verify the version
catalog and plugin repositories. For local artifact consumption, add
`mavenLocal()` to `pluginManagement.repositories`. For active Laydr plus app
development, prefer:

```kotlin
pluginManagement {
    includeBuild("../laydr")
}

includeBuild("../laydr")
```

## Nav3 Adapter Flags

`nav3Kmp` and `nav3Androidx` are mutually exclusive. Both require
`compose.set(true)`.

KMP route module:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

Android-only route module:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
```

If `LaydrNavRoutes` is missing, enable exactly one adapter target and rerun
generation.

## External Targets

External target helpers can reject unknown, malformed, layout-only,
outside-section, or invalid dynamic-parameter targets. Inspect the structured
rejection reason. For ordinary in-app navigation, use generated destinations:

```kotlin
navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
```

## Payloads And Results

`requireLaydrNavPayload<T>()` throws, and
`laydrNavPayloadOrNull<T>()` returns `null`, when the entry was not launched
with that payload, was process-restored, or received the wrong runtime type.

`requireLaydrNavResultSink<T>()` throws, and
`laydrNavResultSinkOrNull<T>()` returns `null`, when the entry was not launched
with `pushForResult<T>(...)`, was process-restored, or was launched for another
result type.

Use nullable accessors when a route can recover:

```kotlin
val resultSink = laydrNavResultSinkOrNull<SignInResult>()
if (resultSink == null) {
    RecoveryUi()
    return
}
```

Completing a result does not pop the route. Call app-owned navigation
separately when the route should close.

## Workflow Failures

Workflow hosting is route-owned screen code. Add `laydr-workflow`, create the
workflow with `rememberLaydrWorkflow`, render and collect outputs with
`LaydrWorkflowHost(..., onOutput = ...)`, and map final outputs to app
behavior. Workflow files do not affect generated route output.
