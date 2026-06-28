# Laydr User Documentation

This documentation is for developers building apps with Laydr.

Laydr turns a Compose app's route tree into visible filesystem structure and
generates typed Kotlin wiring from that structure.

The practical change is small but important:

```text
src/commonMain/kotlin/routes/
  contacts/
    Route.kt
    Screen.kt
    by_id/
      Route.kt
      Screen.kt
```

Each route directory owns a tiny `Route.kt` declaration. Laydr validates that
tree and generates typed destinations, path builders, route maps, and Compose
entry wiring. Your app still owns Compose UI, navigation chrome, state,
dependencies, data loading, platform behavior, and product policy.

## Start Here

Read these first:

1. [Getting Started](getting-started.md) - build a small contacts route tree,
   render it with plain Compose, and run validation.
2. [Concepts](concepts.md) - learn the mental model and what Laydr does not
   own.
3. [Routes](routes.md) - choose the right route shape for screens, dynamic
   details, layouts, and namespace-only segments.
4. [Generated API Tour](generated-api.md) - choose the generated object for
   the task: destination, path, route content, host definitions, or Nav3
   helpers.
5. [Gradle](gradle.md) - copy the setup template for the module that owns
   routes.
6. [Troubleshooting](troubleshooting.md) - recover from scanner, generated
   API, Gradle, Compose, navigation, dependency, payload, result, and workflow
   errors.

## Choose A Runtime

Use the smallest runtime path that matches the app:

| App shape | Start with | Runtime |
| --- | --- | --- |
| Compose Multiplatform app with simple path state | [Getting Started](getting-started.md) | `LaydrRouteHost` |
| Compose Multiplatform app with Nav3 stacks or tabs | [Nav3 KMP](nav3-kmp.md) | `laydr-nav3-kmp` |
| Android-only Compose app with AndroidX Navigation 3 | [AndroidX Nav3](nav3-androidx.md) | `laydr-nav3-androidx` |

## Build With These Rules

- Put app routes in the route root for the module that owns navigation.
- Add `Route.kt` only when a directory is a screen, layout, or both.
- Keep route UI in nearby app-owned files such as `Screen.kt` and `Layout.kt`.
- Navigate inside the app with generated `destination(...)` values.
- Use generated `path(...)` helpers only when the app deliberately stores path
  strings.
- Use `LaydrRouteHost` for path-in/content-out Compose hosting.
- Use `laydr-nav3-kmp` when a KMP app wants Nav3 stacks, tabs, app Back,
  optional adaptive scenes, or `NavDisplay`.
- Use `laydr-nav3-androidx` when an Android-only app wants Google AndroidX
  Navigation 3.
- Keep dependencies, DI, ViewModels, state holders, repositories, auth,
  persistence, labels, icons, tabs, transitions, deep links, and platform
  policy app-owned.
- Never edit generated files under `build/generated/laydr/**`.

## Reference Pages

- [Gradle](gradle.md) - copyable setup templates, dependency coordinates,
  extension flags, generated source directories, and validation tasks.
- [Compose](compose.md) - `LaydrRouteHost`, route-local screen and layout
  definitions, inherited layouts, and layout values.
- [Route Dependencies](route-dependencies.md) - explicit parameters,
  narrow providers, DI options, feature facades, shell capabilities, previews,
  route payloads, and route results.
- [Navigation](navigation/README.md) - runtime choice, Nav3 KMP mental model,
  sections, stacks, Back, state, payloads, results, metadata, adaptive scenes,
  and external targets.
- [AndroidX Nav3](nav3-androidx.md) - Android-only Navigation 3 setup,
  generated helpers, adapter boundaries, and validation.
- [Route-Local Workflow](workflow.md) - private feature workflows inside an
  already matched route.
- [Examples](examples.md) - which runnable example to inspect and which
  validation commands to run.

## What Laydr Is Not

Laydr is not a replacement for Compose, Nav3, ViewModels, DI, repositories,
reducers, auth, browser history, Android intents, iOS universal links,
analytics, theming, or a full application architecture.

It gives those app-owned pieces stable generated route values and route entry
content to work with.
