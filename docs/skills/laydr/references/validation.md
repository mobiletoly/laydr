# Validation

Use this reference before finalizing non-trivial Laydr app changes.

## Start With Current Shape

Inspect:

- route root location
- generated package
- `compose` flag
- enabled Nav3 helper target, if any
- app-owned scripts
- current example or platform modules

Prefer project-specific validation commands over generic commands.

## Route Validation

Run route validation after route tree or `Route.kt` changes:

```bash
./gradlew :shared:checkLaydrRoutes
```

Run generation only when generated source needs to be inspected or compiled:

```bash
./gradlew :shared:generateLaydrRoutes
```

Do not hand-edit generated output.

## Compile Checks

For common route or generated API changes, compile the affected source set:

```bash
./gradlew :shared:compileKotlinDesktop
```

For Android or iOS source-set changes, run the app's existing Android or iOS
compile task. Do not invent a broad platform matrix when the app already has a
narrow validation script.

Android-only Laydr modules commonly use:

```bash
./gradlew :app:checkLaydrRoutes
./gradlew :app:compileDebugKotlin
```

## Runtime Checks

Use a UI smoke check when visual navigation, app Back, adaptive scenes, or
workflow rendering changed and a runnable target is available.

For source-only route changes, route validation plus compile checks are often
enough.

## Hygiene

Before finalizing, check the worktree and avoid reverting unrelated user work:

```bash
git status --short
```

For docs or config edits, also run whitespace and ASCII scans when the project
expects plain ASCII.

When a validation failure names generated APIs, route declarations, or
Nav3 rejected targets, use `generated-api.md` and `troubleshooting.md`
before changing app architecture.
