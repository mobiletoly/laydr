# Spec: Route Graph Generator Split

status: implemented
created: 2026-06-22
updated: 2026-06-22

## 1. Goal

Reduce the size and responsibility concentration of
`laydr-codegen/src/main/kotlin/dev/goquick/laydr/codegen/RouteGraphGenerator.kt`
without changing Laydr code generation behavior.

The observable outcome is a smaller route graph generator facade with separate
source files for core route graph emission, Compose route definition emission,
and Nav3 helper emission.

## 2. Non-Goals

- Do not change generated Kotlin source text intentionally.
- Do not change public generated APIs.
- Do not change route scanning, validation, Gradle plugin wiring, examples, or
  durable user docs.
- Do not add dependencies.
- Do not refactor tests except to update imports or names if required by the
  internal split.

## 3. Background

Drymint repo review identified `RouteGraphGenerator.kt` as the clearest
production maintainability smell. The file currently owns orchestration,
framework-neutral route object emission, Compose route-local helper emission,
Nav3 helper emission, naming helpers, KotlinPoet symbols, and collection code
helpers.

## 4. Desired Behavior

`RouteGraphGenerator` remains the internal entry point used by `LaydrCodegen`
and existing tests. Its `generateRouteGraph` and `generateSources` contracts
must keep returning the same generated source paths and source text for the same
inputs.

The implementation should move cohesive helper groups into separate files in
the same package:

- core route graph and destination emission
- Compose route definitions and route-local helpers
- Nav3 route helpers
- shared names, symbols, and KotlinPoet utility helpers as needed

## 5. Locked Decisions And Invariants

- Generated output is the compatibility boundary.
- `RouteGraphGenerator` stays `internal`.
- `RouteGraphGeneratorException` stays available to existing code and tests.
- New helper types and functions stay `internal` or `private`.
- KotlinPoet remains the emission mechanism.
- Files must keep the Apache header.

## 6. Rules And Failure Modes

- If generated output changes, stop and decide whether the change is accidental.
- If a helper needs public visibility outside `laydr-codegen`, stop and revise
  the spec before continuing.
- If the split creates circular helper dependencies or unclear ownership, keep
  the helper with the source family that emits the affected generated file.

## 7. Existing Patterns To Reuse

- Keep the existing KotlinPoet builder style.
- Keep existing generated source paths through `generatedSourcePath`.
- Keep existing generated object names and function names.
- Keep the existing `LaydrCodegenTest` coverage as the primary behavior lock.

## 8. Agent Containment

Allowed files:

- `docs/spec/2026-06-22-route-graph-generator-split.md`
- `laydr-codegen/src/main/kotlin/dev/goquick/laydr/codegen/LaydrCodegen.kt`
- `laydr-codegen/src/main/kotlin/dev/goquick/laydr/codegen/RouteGraphGenerator.kt`
- new Kotlin files under
  `laydr-codegen/src/main/kotlin/dev/goquick/laydr/codegen/`

Forbidden without spec update:

- `laydr-core/`
- `laydr-compose/`
- `laydr-nav-runtime/`
- `laydr-nav3-kmp/`
- `laydr-nav3-androidx/`
- `laydr-gradle-plugin/`
- examples
- durable docs

## 9. Proposed Design

Keep `RouteGraphGenerator` as a small orchestrator. Extract helper groups into
package-local extension functions or internal collaborators in the same package
so the refactor does not alter call sites outside codegen.

Prefer cohesive files over new abstractions:

- one file for core route graph source emission
- one file for Compose source emission
- one file for Nav3 source emission
- one file for shared model naming and KotlinPoet helpers only if needed

## 10. Implementation Plan

1. Move Compose generation helpers out of `RouteGraphGenerator.kt`.
2. Move Nav3 generation helpers out of `RouteGraphGenerator.kt`.
3. Move shared symbols, names, and utility helpers only as needed to keep the
   source ownership clear.
4. Compile and run focused codegen tests.
5. Run Drymint change review.

## 11. Acceptance Criteria

- `RouteGraphGenerator.kt` is materially smaller.
- Codegen tests pass.
- Drymint review does not report an uninspected behavior-risk gate for the
  refactor.
- `git diff --check -- .` passes.

## 12. Validation Commands

```shell
./gradlew :laydr-codegen:test
git diff --check -- .
```

## 13. Documentation Updates

No durable user documentation update is required because this is an internal
codegen maintainability refactor.

## 14. Cleanup / Legacy Removal

Mark this spec `implemented` after validation passes. Delete it in a later spec
cleanup pass if it no longer provides useful context.

## 15. Open Questions

None.
