# Laydr Engineering Guide

This document is the top-level maintainer and agent entry point for Laydr.

Read this before implementing new Laydr behavior. For implementation slices,
also read `docs/spec/README.md` and the active spec that owns the work.

## Project Thesis

Laydr is a KMP-first library for filesystem-routed, code-generated Compose
navigation and layout structure.

The goal is to make a Compose app's route tree visible from the project
filesystem, then generate boring, typed Kotlin wiring around that tree. Laydr
should remove duplicated navigation strings, graph wiring, argument extraction,
layout wrapping, and route metadata plumbing without hiding Compose or turning
the app into a large architecture framework.

Laydr should feel:

- KMP-first
- Compose-native
- filesystem-visible
- generated where generation removes real duplication
- explicit
- inspectable
- low magic
- easy to explain to a new maintainer

Laydr is not:

- an Android-only navigation helper
- a replacement for Compose
- a ViewModel, DI, MVI, reducer, or repository framework
- an annotation-first routing framework
- a runtime scanner or hidden registration system
- a client architecture framework

## Philosophy

Filesystem structure is a product feature.

Laydr should let a maintainer understand the app's navigation and layout shape
by looking at directories and a small set of local route files. The filesystem
should remain the primary map of the app.

Generation is for explicit wiring, not hidden behavior.

Generated code should be stable, readable Kotlin. It should be easy to inspect
and debug. Generation should own route graph wiring, typed screen destinations,
argument helpers, layout composition hooks, metadata access, and stale-output
checks when those contracts exist. It should not generate app state machines,
repositories, DI modules, ViewModels, or UI components.

Compose must remain visible.

Laydr should improve Compose navigation and layout ergonomics while keeping
Compose APIs and app-owned composables visible. Do not wrap Compose in a
private DSL unless an active spec proves that the wrapper removes real
duplication and stays simpler than direct Compose.

KMP is the default.

`laydr-compose` means Compose Multiplatform. Platform-specific behavior should
be added in modules named for the platform API or behavior, such as
`laydr-androidx-navigation`, not by making the core Compose module
Android-shaped.

Prefer checks over runtime magic.

Invalid route trees, duplicate names, unsupported source-set layouts, stale
generated files, and malformed route declarations should fail through clear
build-time checks where practical.

## Repository Layout

Current top-level modules:

```text
laydr-core/
laydr-nav-runtime/
laydr-compose/
laydr-nav3-kmp/
laydr-nav3-kmp-adaptive/
laydr-nav3-androidx/
laydr-workflow/
laydr-codegen/
laydr-gradle-plugin/
build-logic/conventions/
examples/compose-basic/
examples/nav3-kmp/
examples/nav3-kmp-shopping/
docs/spec/
docs/engineering/
```

Module ownership:

- `laydr-core` owns KMP runtime contracts. It must not depend on Compose,
  Gradle, Android, or code generation.
- `laydr-nav-runtime` owns shared Nav3 adapter-neutral navigation semantics:
  keys, launches, route lookup results, stack mutation, sections, return
  history, payload/result storage, not-found classification, and route
  placement. It may depend on `laydr-core`, but must not depend on Compose,
  Android, Gradle, code generation, JetBrains Navigation3 KMP, AndroidX
  Navigation 3, or Material adaptive APIs.
- `laydr-compose` owns Compose Multiplatform integration. It depends on
  `laydr-core` and should stay common-first.
- `laydr-nav3-kmp` owns JetBrains Navigation3 KMP integration. It
  depends on generated Laydr route contracts and Navigation3 KMP. It may own
  platform key conversion, platform `NavEntry` creation, saved-state wiring,
  and KMP-specific scene strategy plumbing, but shared stack and section
  semantics live in `laydr-nav-runtime`. It does not own app rendering policy,
  breakpoint policy, platform deep links, retained entry policy, Material
  adaptive scene implementations, or AndroidX Navigation 3.
- `laydr-nav3-kmp-adaptive` owns optional Material adaptive Navigation3 scene
  helpers over `laydr-nav3-kmp`. It may depend on Material adaptive APIs.
- `laydr-nav3-androidx` owns Google AndroidX Navigation 3 integration for
  Android-only Compose apps. It depends on `laydr-nav-runtime` and
  `laydr-compose`, converts generated destinations into AndroidX `NavKey`
  entries, and builds AndroidX `NavDisplay` entry providers. It must not
  depend on JetBrains Navigation3 KMP or expose AndroidX adaptive scene support
  unless an active spec adds and validates that artifact.
- `laydr-workflow` owns route-local workflow runtime contracts and Compose
  hosting APIs. Workflow nodes remain headless, but the artifact exports
  Compose runtime because it exposes public composable host APIs. It must not
  depend on Nav3, Gradle, Android, or code generation.
- `laydr-codegen` owns filesystem scanning, validation, and Kotlin emission. It
  is JVM-only and must not depend on Gradle APIs.
- `laydr-gradle-plugin` owns Gradle task and source-set wiring. It should stay
  a thin integration layer over `laydr-codegen`.
- `build-logic/conventions` owns internal build conventions only. It is not a
  published product module.
- `examples/compose-*` are consumer-style Compose Multiplatform examples.
  Platform-specific examples should be named explicitly, such as `android-*`
  or `ios-*`.
- `docs/spec/` owns short-lived execution specs.
- `docs/engineering/` owns durable maintainer guidance.

Publishing and local artifact maintenance are covered in
[Publishing](publishing.md).

The current scaffold is intentionally minimal. Do not treat placeholder
classes as product API.

## Expected Route Shape

The exact route contract must be introduced by an active spec before
implementation. The intended direction is a segment-directory-owned shape like:

```text
routes/
  home/
    Route.kt

  settings/
    Route.kt
    profile/
      Route.kt

  catalog/
    Route.kt
    bundle/
      by_activity_bundle_id/
        Route.kt

  users/
    Route.kt
    by_id/
      Route.kt
```

In this tree, `catalog/bundle` is a namespace-only segment directory: it
contributes a path segment and generated object nesting, but it is not a route
because it has no `Route.kt`.

General expectations:

- segment directories own path and generated object shape
- local `Route.kt` files opt a segment directory into route behavior
- `Route.kt` declares whether a route is a screen, a layout, or both
- namespace-only segment directories may group declared descendants without
  placeholder route declarations
- namespace-only segment directories should not contain direct Kotlin source
  files
- parent route directories may own layout or shell behavior for descendants
- dynamic segments should use Kotlin-friendly filesystem names
- static segment directory underscores should become hyphens in browser URL
  paths while generated Kotlin APIs stay source-name based
- generated APIs should be typed and deterministic
- source sets must be explicit and compatible with KMP builds
- platform adapters should be optional and layered outside core contracts

Do not implement route scanning, generated APIs, layout inheritance, or dynamic
segment rules directly from this document. Create or use an active spec first.

## Core Route Model

`laydr-core` owns the framework-neutral route descriptor model. It describes
URI path templates, path segments, dynamic parameters, route hierarchy, path
building, path matching, route-map lookup, and UI-neutral route sections. It
must stay independent of Compose, Gradle, Android, Ktor, code generation, and
JVM-only URI APIs.

The core model treats route paths as URI path components:

- static route segments are unreserved URI path text
- dynamic parameter names are lowercase snake case
- dynamic values are percent-encoded per URI path segment when building paths
- matching splits the raw path on `/` before decoding dynamic values
- query strings and fragments are outside the core path match contract

This keeps the same core contract usable by future Compose, HTTP, and HTMX
adapters. Framework-specific adapters may add navigation, rendering, method,
request, response, or template behavior outside `laydr-core`.

Route declaration metadata is part of the framework-neutral descriptor. It is
static app-owned data for diagnostics, inspection, and app rendering
decisions. It is not page response metadata, layout state, navigation policy,
auth policy, or route matching behavior.

`LaydrRouteMap` is the shared structural app map generated from route
descriptors. Adapters should use it for path/key conversion, route lookup,
layout-chain lookup, top-level classification, and section membership instead
of requiring apps to pass parallel route lists or rediscover the tree.
`LaydrAppGraph` is the generated app-level facade over that route map. It gives
adapters one typed object for destination validation and route-list access
without moving navigation state, rendering, section policy, labels, icons, or
platform behavior into core.
Generated screen destinations are the app-facing values for navigation. Keys
remain the small framework-neutral payload used by route maps, route sections,
and adapter internals.
`laydrRouteSections` is intentionally UI-neutral: it exposes section roots,
root keys, and `sectionFor(key)` membership lookup only. Labels, icons,
resources, ordering, visibility, badges, and navigation-bar rendering stay in
app code.

## Compose Route Hosting

`laydr-compose` hosts generated descriptors without owning app navigation
state. The low-level host takes `LaydrRouteMap` plus screen or layout lambdas,
but ordinary generated integrations should use route-owned Compose
definitions.

Declared route directories declare rendering in local `Route.kt` files through
generated `LaydrRouteDef` helpers. The generator assembles those declarations
into `LaydrComposeRoutes.definitions`, and `LaydrRouteHost` can render them
without a generated app context. `Route.kt` is also the route-kind source of
truth: screen routes use `LaydrRouteDef.screen`, layout routes use
`LaydrRouteDef.layout`, and routes that are both use
`LaydrRouteDef.screenAndLayout`. Core-only route graphs use
`LaydrRouteDeclaration` from `laydr-core` for the same screen/layout/both
classification without depending on Compose. `Screen.kt`, `Layout.kt`, and
other declared route-package files are ordinary app-owned implementation files.
Namespace-only segment directories do not receive route descriptors, Compose
definitions, destinations, metadata, or route-local helpers.

Generated Compose route definitions are opt-in. Core-only generation emits
`LaydrRoutes.kt` without Compose or `laydr-compose` references; Compose apps
must set `laydr { compose.set(true) }` and provide the corresponding
dependencies themselves.

Generated Compose route definitions are app-context-free. Route-local screen
and layout composables receive generated route destinations or layout context
only; feature dependencies should come from ordinary app-owned Compose code
such as parameters, `CompositionLocal`, `remember`, or Compose DI.

For a matched screen route, Laydr derives the inherited layout chain from the
generated route hierarchy and wraps screen content from outermost layout to
innermost layout. Matched screens may return typed layout values through
`LaydrScreenContent`, and inherited layouts may read those values from
`LaydrLayoutContext`. Those values are app-owned rendering state, not route
matching behavior or framework policy.

## Route-Local Workflow

`laydr-workflow` is for private feature state inside an already matched route.
It is not an app router and does not replace generated destinations,
Nav3 stacks, tabs, deep links, or app Back. Workflow nodes expose
headless state, accept typed events, and emit outputs; a workflow can mutate its
own private node stack or pass app-facing outputs back to the route shell.

`laydr-workflow` renders the current top workflow node through explicit
renderer registration. Apps can host route-local workflows directly from
app-owned composables with `rememberLaydrWorkflow`,
`CollectLaydrWorkflowOutputs`, and `LaydrWorkflowHost`.

Workflow hosting is app-owned. Laydr must not generate workflow host glue,
node classes, reducers, ViewModels, repositories, DI wiring, test skeletons, or
app navigation policy.

## Nav3 KMP Adapter

`laydr-nav3-kmp` is an adapter over the same generated route contracts.
Apps still render Navigation3 directly with `NavDisplay`; Laydr removes the
repetitive glue around generated destination validation, key conversion,
typed section membership, app-owned section data, multi-stack app state, app
state wiring, and structured external path and URL-like target results. It can also attach
optional adaptive scene metadata, expose recognized list/detail stack facts,
and normalize direct navigation to declared detail routes without creating
duplicate route flows.

Nav3 adapters should prefer `LaydrRoutes.appGraph` for validation and
generated destinations for app-owned navigation calls. `LaydrRouteMap`,
`LaydrRouteKey`, and `LaydrNavKey` stay available for lower-level
adapter code and apps that intentionally manage Nav3 stacks directly.
Path helpers are strict external-entry APIs with structured rejection reasons.
External target helpers may preserve raw query and fragment text, but Laydr
must not imply browser history, Android intent, iOS universal-link, host-based
routing, query parsing, or fragment routing.
Section labels, icons, ordering, visibility, badges, breakpoints, and visual
navigation chrome remain app code.

## Architecture Rules

Keep public API small.

Every public type, function, Gradle extension, task, or generated API is a
contract. Add public surface only when the active spec names the contract and
its validation.

Avoid annotations as the core model.

Laydr's primary value is filesystem-visible routing. Annotations may become
optional escape hatches later, but the main route model should not depend on an
annotation-first API unless a future active spec changes this decision.

Keep generation separate from Gradle.

The generator engine belongs in `laydr-codegen`. The Gradle plugin should wire
inputs, outputs, source sets, and checks, then delegate generation work.
`generateLaydrRoutes` writes generated source; `checkLaydrRoutes` validates
the configured route tree without writing generated source.

Keep app policy app-owned.

Laydr should not own authentication policy, dependency injection, state
management style, analytics, theming, repository access, or platform lifecycle
policy. It can expose route metadata or hooks when specified, but the app must
choose how to use them.

Do not hide platform differences.

KMP-first does not mean pretending platforms are identical. Shared behavior
belongs in common modules. Android, iOS, desktop, and future web behavior
should be separated when the platform contract is different.

## Spec Workflow

Use `docs/spec/README.md` for spec rules.

Create or update an active spec before implementing changes that affect:

- route or layout conventions
- generated Kotlin output
- Gradle plugin tasks or extensions
- KMP source-set behavior
- public APIs
- examples
- durable docs
- dependency or toolchain requirements
- platform adapters

Active specs must be handoff-ready. Another maintainer or agent should be able
to implement the slice without reading prior chat.

If implementation exposes a missing decision, stop and update the spec before
continuing.

## Validation

Use the narrowest validation that proves the slice.

Baseline repository checks:

```sh
./gradlew projects
./gradlew build
./gradlew :laydr-codegen:test
./gradlew :laydr-gradle-plugin:test
```

Current example checks:

```sh
./gradlew :examples:compose-basic:desktopApp:compileKotlin
./gradlew :examples:compose-basic:desktopApp:run --dry-run
```

Docs-only changes should at least check the touched Markdown for stale terms,
trailing whitespace, and non-ASCII text when plain ASCII is expected.

Do not claim validation passed unless the command was actually run.

## Starting A New Implementation Session

For a fresh Laydr-specific session:

1. Read this document.
2. Read `docs/spec/README.md`.
3. Inspect `README.md`, `settings.gradle.kts`, and `gradle/libs.versions.toml`.
4. Run `./gradlew projects` if build shape matters.
5. Find or create the active spec for the intended slice.
6. Keep edits within the active spec containment rules.
7. Run the validation commands named by the spec before reporting completion.

If there is no active spec for architecture, code generation, Gradle behavior,
public API, route conventions, examples, or platform integration work, write
the spec first.
