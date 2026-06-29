---
name: laydr-app
description: Create, bootstrap, edit, debug, review, or extend applications that use Laydr, the Kotlin Multiplatform filesystem-routed Compose navigation framework. Use when Codex needs to add Laydr to a KMP or Android-only app, work on route directories and route-local Route.kt files, use generated Laydr destinations or paths, configure Laydr Gradle generation, render Laydr routes with Compose, build Nav3 KMP or AndroidX Nav3 sections/stacks/payloads/results, host route-local workflows, inspect generated output, or run Laydr validation.
---

# Laydr App Development

Use this skill in downstream apps that use Laydr, or when adding Laydr to an
app. Do not assume the target app has the Laydr framework checkout or network
access unless the user says so.

Laydr is KMP-first, Compose-native, filesystem-routed, generated where that
removes real duplication, explicit, and low magic. Android-only Compose apps
are supported through the separate AndroidX Navigation 3 adapter.

## Fast Model

- Route tree: visible filesystem map of app locations.
- `Route.kt`: route-local declaration for screen/layout behavior.
- `LaydrRoutes`: generated route refs, `destination(...)`, `path(...)`,
  `appGraph`, and `routeMap`.
- `LaydrComposeRoutes`: generated Compose definitions for `LaydrRouteHost`
  and Nav3 entry providers.
- `LaydrNavRoutes`: generated helpers for Nav3 sections and stacks when one
  Nav3 adapter is enabled.
- App code owns UI, dependencies, DI, ViewModels, repositories, state,
  labels, icons, auth, analytics, deep links, platform policy, and shell
  chrome.

## First Reads

Read only the references needed for the task:

- Setup, version catalogs, local artifacts, composite builds, route roots,
  adapter flags, or first route: `references/project-setup.md`.
- Route tree edits, route kinds, dynamic segments, metadata, or scanner
  validation: `references/routes.md`.
- Generated objects, destinations, paths, route maps, Compose definitions, or
  generated Nav3 helpers: `references/generated-api.md`.
- `LaydrRouteHost`, route-local screens/layouts, inherited layouts, or layout
  values: `references/compose.md`.
- Route dependencies, DI, `CompositionLocal`, shell capabilities, payloads,
  results, previews, or workflow dependency lookup:
  `references/dependencies.md`.
- KMP Nav3 sections, stacks, Back, payloads, results, adaptive scenes,
  external targets, or parent-stack capabilities:
  `references/nav3-kmp.md`.
- Android-only AndroidX Nav3 setup, sections, stacks, payloads, results, Back,
  or validation: `references/nav3-androidx.md`.
- Route-local `Workflow.kt`, nodes, renderer registration, output handling, or
  workflow tests: `references/workflow.md`.
- Failures involving route validation, stale generated APIs, missing runtime
  dependencies, Nav3 rejected targets, payloads/results, or workflow setup:
  `references/troubleshooting.md`.
- Final checks for non-trivial app changes: `references/validation.md`.

## Default Workflow

1. Identify app shape: KMP route module, Android-only route module, existing
   Laydr app, or new setup.
2. Inspect the route root before editing:
   - KMP default: `src/commonMain/kotlin/routes`
   - Android-only default: `src/main/kotlin/routes`
3. Inspect the module's `laydr { ... }` block, generated package, adapter
   flags, and current validation tasks.
4. Prefer app-owned source edits: route-local declarations, screen/layout UI,
   dependency access, shell navigation, or workflow nodes/renderers.
5. Use generated destinations for in-app navigation. Use generated paths only
   when the app deliberately stores or handles path strings.
6. Never edit generated files. Fix route source and rerun validation or
   generation.
7. Validate narrowly first, then broaden when source sets, adapters, or UI
   behavior changed.

Common fallback commands when the app has no better script:

```bash
./gradlew :shared:checkLaydrRoutes
./gradlew :shared:generateLaydrRoutes
./gradlew :shared:compileKotlinDesktop
```

For Android-only apps, start with:

```bash
./gradlew :app:checkLaydrRoutes
./gradlew :app:compileDebugKotlin
```

## Package Map

- `dev.goquick.laydr:laydr-core`: framework-neutral route contracts, maps,
  keys, destinations, matching, and path building.
- `dev.goquick.laydr:laydr-compose`: Compose Multiplatform route host and
  route definition contracts.
- `dev.goquick.laydr:laydr-nav-runtime`: shared Nav3 adapter-neutral stack,
  section, launch, payload/result, and lookup semantics.
- `dev.goquick.laydr:laydr-nav3-kmp`: JetBrains Navigation3 KMP adapter.
- `dev.goquick.laydr:laydr-nav3-kmp-adaptive`: optional Material adaptive
  scene helpers for KMP Nav3.
- `dev.goquick.laydr:laydr-nav3-androidx`: Google AndroidX Navigation 3
  adapter for Android-only Compose apps.
- `dev.goquick.laydr:laydr-workflow`: route-local workflow runtime and Compose
  host APIs.
- `dev.goquick.laydr` Gradle plugin: route generation and route validation.

## Non-Negotiables

- Keep route ownership visible in filesystem directories.
- Keep declarations in route-local `Route.kt` files.
- Keep `Screen.kt`, `Layout.kt`, `Workflow.kt`, and helpers app-owned unless
  generated output owns them.
- Keep labels, icons, visual navigation, breakpoints, auth, persistence,
  repositories, DI, ViewModels, reducers, platform policy, and shell chrome
  app-owned.
- Do not create generated app contexts, root service registries, umbrella
  providers, hidden runtime registration, annotation-first routing, or a
  second navigation system.
- Use `laydr-nav3-kmp` for KMP Nav3 apps and `laydr-nav3-androidx` for
  Android-only AndroidX Nav3 apps. Do not enable both for one route tree.
- Do not document, scaffold, or use AndroidX adaptive scene support as current
  behavior.
- Use route-local workflow only for private feature flow inside an already
  matched route.

## Generated Files

Treat these as build output:

```text
build/generated/laydr/commonMain/kotlin/
build/generated/laydr/main/kotlin/
```

If generated APIs are stale or missing, fix authored route files and rerun
`checkLaydrRoutes` or `generateLaydrRoutes`.

## Stop Conditions

Stop and inspect before continuing when the route root is unclear, source-set
behavior is ambiguous, generated output disagrees with route source, the
requested change needs new Laydr framework behavior, or validation failures are
unrelated to the edit.
