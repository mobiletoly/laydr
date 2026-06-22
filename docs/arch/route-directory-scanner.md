# Route Directory Scanner

`laydr-codegen` owns filesystem scanning for Laydr route trees. The scanner
contract is intentionally internal and exists to validate route-tree
semantics before Kotlin source is generated.

## Current Contract

The scanner accepts a JVM `Path` pointing at a routes root. Each directory
under the root is a segment directory. A segment directory declares route
behavior only when it contains `Route.kt`; without `Route.kt`, it is a
namespace-only segment directory when it contains declared descendants.

For declared routes, the scanner strips comments and string literals from
`Route.kt`, then reads one unaliased route-kind declaration:

- `LaydrRouteDef.screen` or `LaydrRouteDeclaration.screen` for a screen
  endpoint.
- `LaydrRouteDef.screenWithLayoutValues` for a Compose screen endpoint whose
  content returns layout values.
- `LaydrRouteDef.screenAndLayout` or
  `LaydrRouteDeclaration.screenAndLayout` for a screen endpoint that also
  wraps descendants.
- `LaydrRouteDef.layout` or `LaydrRouteDeclaration.layout` for a layout-only
  parent route with at least one child route.

Nested segment directories become child nodes, and child ordering is
deterministic by directory name. Generated object nesting mirrors the full
segment tree. Declared route descriptor `children` skip namespace-only segment
directories and attach the nearest declared descendants.

Static directory names map to URL path segments by replacing underscores with
hyphens. Dynamic directory names use `by_<name>` and map to `{name}` path
segments. The same validation applies to declared routes and namespace-only
segment directories. For example, `routes/user_profile` maps to `/user-profile`,
while `routes/users/by_id` maps to `/users/{id}` and, when `by_id` contains
`Route.kt`, produces the deterministic route name `UsersByIdRoute`.

Directory names must be lowercase snake_case. Route names for declared routes
are built from the segment source names in PascalCase plus `Route`, so generated
Kotlin identifiers stay source-name based even when URL paths are hyphenated.

## Validation

The scanner fails early for missing roots, non-directory roots, `Route.kt`
without exactly one route-kind declaration, direct Kotlin files in a segment
directory without `Route.kt`, layout-only routes without declared descendants,
invalid segment directory names, invalid dynamic directory names, duplicate
generated object names, and duplicate generated route ids among declared routes.

`Route.kt` is the route-owned declaration file. For Compose-enabled generation,
the scanner parses its package declaration so generated route-local Compose
helpers can be emitted into the same package and the generated aggregate can
reference the route-owned `Route` value. Namespace-only segment directories do
not receive generated `LaydrRouteDef.kt`, Compose definitions, route metadata,
destinations, or helper files. `Screen.kt`, `Layout.kt`, workflow files, and
other files in a declared route package are ordinary app-owned implementation
files with no scanner behavior.

Generated route metadata is derived from the existing route tree, not from
app implementation file contents. App-authored metadata, query strings,
fragments, redirects, and platform-specific navigation are outside the current
scanner contract.
