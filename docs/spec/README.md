# Spec Authoring Policy

`docs/spec/` contains short-lived planning and implementation specs.

Specs are execution documents, not permanent documentation. They guide focused
work, keep scope explicit, and define validation and cleanup before work is
done.

Permanent Laydr behavior belongs in durable docs:

- `docs/user/`
- `docs/arch/`
- `README.md` when relevant
- package documentation
- examples
- tests

Do not treat `docs/spec/` as a long-term design archive.

## When A Spec Is Required

Create a spec before implementation when a change affects architecture, public
behavior, generated output, or more than one subsystem.

This includes:

- route or layout filesystem conventions
- KMP source-set behavior
- Compose Multiplatform rendering behavior
- Android, iOS, desktop, or web platform adapters
- route metadata or navigation model behavior
- Gradle plugin behavior
- code generation
- generated Kotlin APIs
- generated project structure
- public APIs
- toolchain requirements
- dependency additions
- breaking changes
- examples or durable documentation

Small local fixes may skip a spec when they are isolated, obvious, low risk,
and covered by existing tests.

When unsure about architecture or public behavior, write a short spec.

## Public API Documentation

Public Kotlin library interfaces, classes, functions, properties, Gradle
extensions, and Gradle tasks must have KDoc that describes the supported
contract. Specs that add or change public API must name the required KDoc
updates in the implementation plan or documentation section.

## Lifecycle

Specs that drive implementation must declare:

```md
status: draft | active | implemented | abandoned
created: YYYY-MM-DD
updated: YYYY-MM-DD
```

Optional metadata such as `owner:` is allowed when it helps coordination.

Only `active` specs should drive implementation.

Status meanings:

- `draft`: still shaping the work; not approved for implementation.
- `active`: approved execution document for implementation.
- `implemented`: work is complete, validated, and folded into durable docs
  where needed.
- `abandoned`: no longer relevant.

Implemented specs should be deleted during cleanup once they no longer provide
useful context.

## File Naming

Most specs should be standalone files named by creation date:

```text
YYYY-MM-DD-short-slug.md
```

Use standalone specs when the work is one focused implementation slice and does
not need a multi-track roadmap.

Example:

```text
2026-06-11-route-directory-scanner.md
```

Use grouped specs only when the work explicitly needs an umbrella plus multiple
child tracks. Grouped specs use this shape:

```text
<group><sequence>-<short-slug>.md
```

Where:

- `<group>` is a single lowercase ASCII letter such as `a`, `b`, or `c`.
- `<sequence>` is a four-digit number.
- `<short-slug>` is lowercase ASCII words separated by hyphens.

The `0000` sequence is reserved for the umbrella spec in that group.

Child specs under the same umbrella must use the same group letter and the next
available sequence numbers.

Example:

```text
a0000-routing-umbrella.md
a0001-route-directory-scanner.md
a0002-generated-route-api.md
a0003-compose-route-host.md

b0000-gradle-plugin-umbrella.md
b0001-generate-task.md
b0002-source-set-integration.md
b0003-stale-output-check.md
```

Do not use one global numeric sequence for unrelated initiatives.

Do not mix child specs from different umbrellas under the same group letter.

Do not reuse a group letter for a new initiative while specs from the previous
initiative remain in `docs/spec/`.

`README.md` is the only non-spec file in this directory and does not follow a
spec filename pattern.

## Required Structure

Use this structure for active implementation specs unless there is a strong
reason not to:

```md
# Spec: <short title>

status: draft | active | implemented | abandoned
created: YYYY-MM-DD
updated: YYYY-MM-DD

## 1. Goal

## 2. Non-Goals

## 3. Background

## 4. Desired Behavior

## 5. Locked Decisions And Invariants

## 6. Rules And Failure Modes

## 7. Existing Patterns To Reuse

## 8. Agent Containment

## 9. Proposed Design

## 10. Implementation Plan

## 11. Acceptance Criteria

## 12. Validation Commands

## 13. Documentation Updates

## 14. Cleanup / Legacy Removal

## 15. Open Questions
```

Keep specs as small as possible while still executable. Split large specs
instead of creating one document that invites scope drift.

## Handoff-Ready Specs

Active standalone specs and active child specs must be handoff-ready.

Handoff-ready means another engineer or agent can implement the spec without
reading prior chat, guessing missing decisions, or inventing scope.

An active implementation spec must include:

- the exact goal and observable outcome
- explicit non-goals
- the supported behavior and any rejected behavior
- concrete interfaces, commands, paths, data shapes, or module boundaries
  affected by the work
- locked decisions and invariants that implementation must preserve
- rules and failure modes that matter for the slice
- existing patterns, helpers, modules, examples, tests, or docs that must be
  reused
- containment rules for allowed files, forbidden files, API and dependency
  constraints, refactor constraints, and stop conditions
- phased implementation checklist items
- targeted tests or an explicit reason tests are not needed
- validation commands
- durable docs and examples impact
- cleanup and legacy removal requirements
- open questions that are answered, deferred, or converted into follow-up
  specs before implementation

Do not mark a spec `active` if a reader still needs to decide what to build.

If implementation exposes a missing decision, stop and update the spec before
continuing.

For very small active specs, sections may be brief, but do not omit locked
decisions, failure modes, reuse expectations, or containment when the work
affects routing, layouts, code generation, Gradle behavior, public APIs,
examples, generated Kotlin, or multiple modules.

## Umbrella Specs

Umbrella specs may be lighter than child implementation specs.

They may use compact tracker checklists and do not need to duplicate the full
implementation structure for every future track.

Umbrella specs must still make progress trackable:

- each track should have status
- each track should name its child spec when it exists
- each track should record docs impact
- each track should record examples impact
- each track should record validation state or next validation action

The next planned or active implementation track must have a child spec.

Future tracks may remain placeholders until they become the next slice of work.

Do not implement directly from an umbrella spec unless the change is only
umbrella maintenance.

## Goals And Non-Goals

Goals must describe observable outcomes:

- what user-visible behavior changes
- what developer workflow changes
- what files, modules, packages, or commands are affected
- what should not change

Non-goals are mandatory for active specs.

If implementation needs to cross a non-goal, stop and update the spec before
continuing.

Do not silently drift.

## Locked Decisions, Reuse, And Containment

Locked decisions and invariants name behavior, boundaries, and architectural
constraints that must not change during implementation. They should be concrete
enough for a reviewer to tell whether the implementation preserved them.

Rules and failure modes should cover behavior that can be reached through
current supported scanners, parsers, APIs, generated code, Gradle tasks,
filesystem shapes, route declarations, or user input. Do not require code or
tests for unreachable defensive states. If a spec keeps defensive handling for
a state, it must name the current entry point that can produce that state and
explain why the handling belongs in this slice.

Existing patterns to reuse should name the local command, module, helper, test
pattern, example, or documentation style that should guide the work. Specs
should prefer existing Laydr patterns over parallel new systems.

Agent containment should state:

- allowed files or directories
- forbidden files or directories
- public API constraints
- dependency constraints
- refactor constraints
- stop conditions

If a phase, validation command, documentation update, cleanup item, or
acceptance criterion names a source path, durable doc, generated artifact,
config, or script, that path should also appear in the implementation
touchpoints or containment rules.

## Architecture Quality Gates

Architecture or module-boundary specs must name the durable owner of the core
behavior and the contracts it owns.

Default ownership boundaries:

- `laydr-core` owns KMP runtime contracts and must not depend on Compose,
  Gradle, Android, or code generation.
- `laydr-compose` owns Compose Multiplatform integration and depends on
  `laydr-core`.
- `laydr-codegen` owns filesystem scanning, validation, and Kotlin emission.
  It must not depend on Gradle APIs.
- `laydr-gradle-plugin` owns Gradle task and source-set wiring. It should stay
  a thin integration layer over `laydr-codegen`.
- `examples/compose-*` are consumer-style KMP Compose examples. Platform-only
  examples should be named `android-*`, `ios-*`, or by the platform behavior
  they demonstrate.

Do not introduce packages, facades, wrappers, shims, mirrored types, aliases,
or forwarding helpers mainly to hide dependency direction, avoid touching the
real owner, or preserve a weak old structure. A new package or module must
name the durable responsibility it owns, who should depend on it, who must not
depend on it, and why that responsibility does not belong in an existing
module.

If a temporary bridge or compatibility path is intentionally retained, the spec
must name the owner, reason, guardrail, and removal condition before
implementation proceeds.

Prefer explicit v0 contract cleanup over legacy compatibility when no concrete
current compatibility requirement is recorded in the spec.

If clean ownership is blocked by dependency cycles or unclear responsibility,
stop and update the spec with the available owner options before continuing.

## Anti-Pressure Rules

A checklist is not permission to force a pass. If validation evidence fails,
mark the spec blocked or update the spec with the next concrete design step
before changing downstream behavior.

Do not make a test, example, or review comment pass by adding route-specific
strings, path-substring checks, fixture-specific branches, command-specific
special cases, or one-off generated output tweaks unless those strings or
branches are already product contracts or are introduced as product contracts
in the spec before code changes.

Tests, examples, and review comments are validation inputs, not implementation
selectors. If implementation needs a string, path, or branch condition, the
spec must explain the Laydr contract that owns it.

## Implementation Plan

Non-trivial specs must be phased.

Checklist items must be actionable and verifiable.

An actionable checklist item names a concrete code, test, docs, validation, or
cleanup result that a reviewer can confirm.

Checklist items should not describe intentions, hopes, or broad quality goals.

When phases are helpful, keep phases as headings and put checkbox items under
each phase. Each phase should include the tests and documentation updates tied
to the behavior changed by that phase when practical. Use the final phase for
full verification and cleanup, not as the only place where tests or docs
appear.

Each source phase that changes behavior should include representative
happy-path tests and reachable edge-case tests, or explain why tests are not
needed. Name the important edge categories for the phase, such as validation
failures, missing files, stale generated output, unsupported route shapes, bad
Gradle task inputs, empty results, malformed route declarations, or platform
source-set mismatch.

Good:

```md
- [ ] `routes/users/by_id/Route.kt` maps to `/users/{id}`.
- [ ] `routes/settings/Route.kt` declares a layout route.
- [ ] Duplicate generated route names fail with an actionable error.
- [ ] `laydr-codegen` scanner tests cover missing route-kind declarations.
- [ ] `examples/compose-basic` demonstrates the generated route graph.
```

Bad:

```md
- [ ] Improve routing.
- [ ] Make Compose easier.
- [ ] Add tests.
- [ ] Update docs.
```

Update checklist items only when the code, tests, docs, or validation evidence
exists.

Once a checklist item is verifiably complete, tick it before continuing to the
next phase or reporting the implementation as complete. Do not leave
completed, verifiable work unchecked as a progress note for later cleanup.

## Drift Control

You must not expand scope silently.

During implementation:

- do not add features not listed in the spec
- do not introduce new architectural concepts without updating the spec
- do not rewrite unrelated code
- do not rename public APIs unless the spec requires it
- do not create a second way to do something
- do not leave obsolete behavior half-supported unless compatibility is
  explicit

If new information changes scope, architecture, public behavior, validation, or
non-goals, update the spec before continuing.

## Pre-v0 Breaking Changes

Laydr is pre-v0.

Specs may intentionally introduce breaking changes when they improve
architecture, simplicity, KMP-first behavior, Compose-native behavior,
filesystem clarity, inspectability, public API clarity, generated output, or
long-term maintainability.

Before v0, do not preserve compatibility with weak early conventions unless a
spec explicitly requires it.

If a spec introduces a breaking change, it must state:

- what breaks
- why the new design is better
- what obsolete code, docs, examples, generated output, or Gradle behavior must
  be removed
- what validation proves the new behavior

Pre-v0 freedom is not permission for churn. It is permission to improve the
foundation.

## Project Rules Still Apply

All specs must follow `AGENTS.md` if available.

Do not duplicate root architectural rules here once they exist. In particular,
Laydr should stay KMP-first, Compose-native, filesystem-visible,
code-generation-friendly, explicit, and low magic. Runtime scanning,
annotation-first routing, hidden registration, broad dependency additions, and
large architecture frameworks need an active spec before implementation.

Specs must use plain ASCII text unless there is a concrete product reason not
to.

Avoid em dashes, smart quotes, decorative bullets, non-ASCII punctuation, and
invisible Unicode whitespace.

## KMP, Dependencies, And Public API

Specs that add dependencies, public APIs, exported packages, generated APIs, or
toolchain requirements must call that out explicitly.

They must explain:

- why Kotlin, Gradle, Compose, KMP, or platform standard APIs are not enough
- why a small local implementation is not enough
- why the new API surface is necessary now
- what long-term maintenance burden is being accepted

Use current stable, mutually compatible Kotlin, Compose Multiplatform, Gradle,
and Android Gradle Plugin versions during v0 development. Verify version claims
against current upstream docs before committing them to an active spec.

Prefer current standard Kotlin, KMP, Compose, and Gradle APIs over custom
helpers.

Keep dependencies and public API surface minimal.

## Validation, Docs, And Cleanup

Every active spec must list validation commands. If a command is not available
yet, say so explicitly.

Do not claim validation passed unless it was actually run.

Tests should be targeted and minimal.

Add or update tests when they protect framework behavior, public APIs,
generated output, regressions, Gradle integration, or architectural invariants.

Prefer the smallest test that would fail for the behavior or bug the change is
meant to cover.

Avoid broad matrices, large fixture trees, brittle incidental-internal
assertions, and large golden files unless they protect a real framework
contract.

Each active spec should state whether it affects:

- `README.md`
- `docs/user/`
- `docs/arch/`
- examples
- generated Kotlin output
- Gradle plugin help or task behavior
- package docs

Specs that add or change public Kotlin packages, classes, interfaces,
functions, properties, constants, annotations, Gradle extensions, Gradle tasks,
or generated APIs must require public documentation for the interface. The only
exception is when the spec explicitly explains why a public identifier is
self-evident and already covered by package documentation.

If behavior changes, durable docs must change in the same implementation
slice.

If a spec changes supported user behavior, update `docs/user/` in the same
slice.

If a spec changes framework architecture, generated-code behavior, internal
invariants, or maintainer expectations, update `docs/arch/` in the same slice.

If framework usage changes, update the relevant example in the same slice or
explain why no example update is needed.

A child spec is not complete if it leaves durable docs or examples stale.

Gradle-visible changes should include Gradle dogfood in the phase that
implements the Gradle surface. Compose-visible example changes should include
desktop, Android, iOS, or screenshot validation when the behavior cannot be
proven by unit tests alone.

### Docs-Only Validation Exception

Use this exception only when every touched file is a Markdown file directly
under `docs/spec/`.

For those changes:

- run the handoff-quality pass from this README
- do not require Kotlin tests, Gradle builds, generated artifact checks,
  platform builds, browser checks, or example app launches
- optionally run whitespace and ASCII checks when useful

This exception does not apply when a change touches source files, tests,
generated files, durable docs outside `docs/spec/`, examples, configs, or any
non-Markdown file. Specs that plan future source work must still include the
correct source validation commands for that future implementation spec.

Cleanup is part of the work. The cleanup section should identify obsolete code,
docs, examples, compatibility paths, generated files, and tests.

During v0, cleanup should be aggressive.

Durable docs should describe the current intended design, not the history of
old decisions.

Remove stale docs, examples, generated output, and code instead of preserving
deprecated behavior or documenting long migration history, unless compatibility
is explicitly required by the spec.

No legacy leftovers unless explicitly justified.

## Handoff Quality Pass

After drafting or editing an active spec, run a separate review pass focused
only on handoff quality before marking the spec active or reporting the spec
edit complete.

The pass must verify:

1. The spec is self-contained and does not rely on chat context.
2. Rules are deterministic and unambiguous.
3. Locked decisions, invariants, and non-goals are clear.
4. Existing patterns to reuse are named where applicable.
5. Containment rules cover every path named by phase items, validation
   commands, cleanup items, and planned docs updates.
6. Acceptance criteria are concrete and testable.
7. Validation commands are explicit, or the docs-only validation exception is
   explicitly applicable.
8. Rollout, rollback, cleanup, or legacy-removal expectations are clear enough
   for the slice.

Avoid vague placeholder words unless the spec provides an explicit default.
