# Validation

Use this before finalizing non-trivial Laydr app changes.

## Inspect First

Before choosing commands, identify:

- route root and source set
- generated package
- `compose` flag
- enabled Nav3 helper target, if any
- app-owned scripts or validation tasks
- affected platform modules

Prefer project-specific commands over generic commands.

## Route And Generated Source

Run route validation after route tree or `Route.kt` changes:

```bash
./gradlew :shared:checkLaydrRoutes
```

Android-only modules commonly use:

```bash
./gradlew :app:checkLaydrRoutes
```

Run generation only when generated APIs need to be inspected, compiled, or
indexed:

```bash
./gradlew :shared:generateLaydrRoutes
```

Never hand-edit generated output.

## Compile Checks

For KMP route or generated API changes, compile the affected source set:

```bash
./gradlew :shared:compileKotlinDesktop
```

For Android-only Laydr modules:

```bash
./gradlew :app:compileDebugKotlin
```

Use the app's broader build or platform task when changes touch resources,
manifests, platform wiring, packaging, or launchers.

## Runtime And UI Checks

Use a UI smoke check when visual navigation, app Back, adaptive scenes,
payload/result flows, or workflow rendering changed and a runnable target is
available.

For source-only route changes, route validation plus compile checks are often
enough.

## Hygiene

Check the worktree and avoid reverting unrelated user work:

```bash
git status --short
```

For docs or config edits, also run whitespace and ASCII scans when the project
expects plain ASCII.

If validation failures name generated APIs, route declarations, rejected Nav3
targets, payloads/results, or workflow setup, read `generated-api.md` and
`troubleshooting.md` before changing app architecture.
