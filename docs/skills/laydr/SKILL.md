---
name: laydr-app
description: Create, bootstrap, edit, debug, review, or extend applications that use the Laydr Kotlin Multiplatform framework or Android-only Laydr support. Use when Codex needs to add Laydr to an app, work on filesystem route trees, route-local Route.kt declarations, generated Laydr destinations, Compose route definitions, Nav3 KMP or AndroidX Nav3 sections/app state/route payloads/route results, route-local workflows, generated output, or local validation workflows.
---

# Laydr App Development

Use this skill inside downstream application repositories that use Laydr, or
when adding Laydr to an application.

Laydr is KMP-first, Compose-native, filesystem-routed, generated where
generation removes real duplication, explicit, and low magic. It also supports
Android-only Compose apps through a separate AndroidX Navigation 3 adapter.
Keep app behavior visible in ordinary Kotlin and Compose code. Do not
introduce hidden runtime route registration, annotation-first routing, a
second navigation system, or framework-owned app architecture.

This package is self-contained. Do not assume the target project has the
Laydr framework checkout or network access available unless the user says so.
Do not edit the Laydr framework repository while working in a downstream app
unless the user explicitly asks for framework development.

## Package Map

- `dev.goquick.laydr:laydr-core`: framework-neutral route contracts, route
  maps, keys, destinations, matching, and path building.
- `dev.goquick.laydr:laydr-compose`: Compose Multiplatform route host and
  route definition contracts.
- `dev.goquick.laydr:laydr-nav3-kmp`: JetBrains Navigation3 KMP
  adapter, sections, app state, wiring, Back, and target helpers.
- `dev.goquick.laydr:laydr-nav3-kmp-adaptive`: optional Material adaptive
  Navigation3 scene helpers.
- `dev.goquick.laydr:laydr-nav3-androidx`: Google AndroidX Navigation 3
  adapter for Android-only Compose apps.
- `dev.goquick.laydr:laydr-workflow`: route-local workflow runtime and
  Compose hosting APIs.
- `dev.goquick.laydr` Gradle plugin: route generation and route validation.

## Default Workflow

1. Decide the current app state:
   - existing Laydr app
   - KMP app that needs Laydr added
   - new app/module requested by the user
2. For setup, dependencies, plugin wiring, or a new app skeleton, read
   `references/project-setup.md` before editing.
3. Identify the route root and inspect current structure before editing:
   - KMP default `src/commonMain/kotlin/routes`
   - Android-only default `src/main/kotlin/routes`
   - route-local `Route.kt`, `Screen.kt`, `Layout.kt`, and `Workflow.kt`
   - generated package and current Gradle tasks
4. For generated API use, read `references/generated-api.md` before naming
   generated objects, destinations, route maps, Compose definitions, or
   route-local helpers.
5. Edit app-owned source first:
   - route-local declarations and UI
   - app-owned route dependencies and navigation shell
   - app-owned workflow nodes and renderers
6. Do not hand-edit generated files. Regenerate or validate from source.
7. Run focused route validation, then broader platform validation when the
   change crosses runtime, source-set, or UI boundaries.
8. If route validation or generated API use fails, read
   `references/troubleshooting.md` before attempting broad rewrites.

Common commands, when the app does not provide wrapper scripts:

```bash
./gradlew :shared:checkLaydrRoutes
./gradlew :shared:generateLaydrRoutes
./gradlew :shared:compileKotlinDesktop
```

Prefer project-specific commands when the application defines them.

## Read References By Task

Load only the references needed for the current request:

- For adding Laydr to a KMP app, local composite setup, Gradle plugin wiring,
  local Maven artifacts, dependencies, or first route setup: read
  `references/project-setup.md`.
- For route trees, route kinds, dynamic directories, metadata, generated
  destinations, path builders, or scanner validation: read
  `references/routes.md`.
- For `LaydrRoutes`, `destination(...)`, `path(...)`, `appGraph`, `routeMap`,
  `LaydrComposeRoutes`, or `LaydrRouteDef`: read
  `references/generated-api.md`.
- For `LaydrRouteHost`, route-local screen/layout
  definitions, layout values, or not-found rendering: read
  `references/compose.md`.
- For route dependencies, Koin or app DI, `CompositionLocal`, feature facades,
  shell callbacks, route-local workflow dependencies, or previewable route
  entrypoints: read `references/dependencies.md`.
- For Nav3 KMP sections, app state, wiring helpers, route payloads, route
  results, app Back, adaptive scenes, external targets, or route-local
  navigator use: read `references/nav3-kmp.md`.
- For AndroidX Nav3 sections, app state, wiring helpers, route payloads, route
  results, app Back, external targets, or Android-only source-set setup: read
  `references/nav3-androidx.md`.
- For route-local `Workflow.kt`, workflow nodes, renderer registration, or
  output handling: read `references/workflow.md`.
- For invalid `Route.kt`, stale generated APIs,
  rejected external targets, missing runtime dependencies, or workflow setup
  failures: read `references/troubleshooting.md`.
- Before finalizing non-trivial app changes: read `references/validation.md`.

## Non-Negotiables

- Keep route ownership visible in filesystem directories.
- Keep route declarations in route-local `Route.kt` files.
- Keep `Screen.kt`, `Layout.kt`, `Workflow.kt`, and helper files app-owned
  unless generated output owns them.
- Prefer generated `destination(...)` values for in-app navigation.
- Use generated `path(...)` helpers only when the app deliberately stores or
  handles path strings.
- Keep labels, icons, visual navigation, breakpoints, auth, persistence,
  repositories, DI, ViewModels, reducers, and platform policy app-owned.
- Do not create generated or root-owned app contexts, umbrella feature
  providers, or shell-level provider cascades for route dependencies.
- Use `LaydrRouteHost` for path-in/content-out Compose hosting.
- Use `laydr-nav3-kmp` when Nav3 should own stacks and display.
- Use `laydr-nav3-androidx` for Android-only apps that use Google AndroidX
  Navigation 3.
- Use route-local workflow only for private feature flow under an already
  matched route.
- Do not document, use, or scaffold AndroidX adaptive Nav3 support as current
  behavior.
- Do not patch generated Laydr source by hand.

## Generated Files

Treat generated Laydr files under the build directory as generated output:

```text
build/generated/laydr/commonMain/kotlin/
build/generated/laydr/main/kotlin/
```

Route packages may also have generated `LaydrRouteDef.kt` helper source in that
output tree.

If generated APIs are stale or missing, fix authored route files and rerun
`checkLaydrRoutes` or `generateLaydrRoutes`.

## Stop Conditions

Stop and inspect before continuing when:

- the app does not appear to be a KMP project
- the route root is unclear or duplicated
- a generated file differs from route source expectations
- the requested change requires new Laydr framework behavior
- the requested change requires a planned but unimplemented adapter
- source-set or platform behavior is ambiguous
- validation failures appear unrelated to the current edit

Prefer a narrow source-grounded app fix over broad rewrites.
