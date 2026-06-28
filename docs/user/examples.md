# Examples

Laydr examples are consumer-style apps under `examples/`.

Read [Getting Started](getting-started.md) first, then use the example that
matches the app shape you are building.

## Choose An Example

| Need | Example |
| --- | --- |
| Small path-state Compose app | `examples/compose-basic/` |
| KMP Nav3 sections and `NavDisplay` | `examples/nav3-kmp/` |
| Larger KMP app with shopping flows | `examples/nav3-kmp-shopping/` |
| Android-only AndroidX Navigation 3 app | `examples/nav3-androidx/` |

## Compose Basic

Start here when you do not need Nav3.

It demonstrates:

- generated route descriptors
- typed path builders and destinations
- dynamic path parameters
- inherited layouts
- screen-owned layout data
- app-owned path state rendered through `LaydrRouteHost`

Validation checks:

```sh
./gradlew :examples:compose-basic:shared:checkLaydrRoutes
./gradlew :examples:compose-basic:desktopApp:compileKotlin
./gradlew :examples:compose-basic:desktopApp:run --dry-run
```

## Nav3 KMP

Use this when you want the recommended KMP Nav3 shape.
Read [Nav3 KMP](nav3-kmp.md) before using this example as a template.

It demonstrates:

- generated `destination(...)` values
- route-local `Route.kt` and `Screen.kt` files
- app-owned dependency access from route entrypoints
- generated `LaydrNavRoutes.rememberSections(...)`
- `LaydrNavSections` and `LaydrNavSectionsNavigator`
- section stacks
- app Back
- optional adaptive list/detail scenes
- structured path and external target helpers
- app-owned `NavDisplay`

Validation checks:

```sh
./gradlew :examples:nav3-kmp:shared:checkLaydrRoutes
./gradlew :examples:nav3-kmp:desktopApp:compileKotlin
./gradlew :examples:nav3-kmp:desktopApp:run --dry-run
```

## Nav3 KMP Shopping

Read this after `nav3-kmp` when you need larger app patterns.
Use it with [Advanced Navigation Topics](navigation/advanced.md) and
[Payloads And Results](navigation/payloads-results.md).

It demonstrates:

- Shop, Search, Cart, Orders, and Profile sections
- compact bottom navigation and wide side rail
- checkout layout wrapping
- optional adaptive product and order detail scenes
- a mixed root `LaydrNavStack` for sectioned shopping plus account sign-in
- typed route results for address, payment-method, and sign-in flows
- return-aware Back flows
- route-local checkout review workflow
- local Koin dependency access in route-owned screen files
- accepted and rejected external targets
- Android, desktop, iOS, and WasmJS launchers

Validation checks:

```sh
./gradlew :examples:nav3-kmp-shopping:shared:checkLaydrRoutes
./gradlew :examples:nav3-kmp-shopping:shared:compileKotlinDesktop
./gradlew :examples:nav3-kmp-shopping:shared:compileKotlinWasmJs
./gradlew :examples:nav3-kmp-shopping:desktopApp:run --dry-run
./gradlew :examples:nav3-kmp-shopping:webApp:wasmJsBrowserDevelopmentWebpack
```

Manual launch commands:

```sh
./gradlew :examples:nav3-kmp-shopping:webApp:wasmJsBrowserDevelopmentRun
```

## AndroidX Nav3

Use this when the app is Android-only and should not have a KMP shared module.
Read [AndroidX Nav3](nav3-androidx.md) before using this example as a
template.

It demonstrates:

- Android-only route root `src/main/kotlin/routes`
- generated `LaydrNavRoutes.rememberSections(...)`
- AndroidX Navigation 3 `NavDisplay`
- Contacts and Profile sections matching the smaller KMP Address Book flow
- app-owned dependency access from route entrypoints
- no AndroidX adaptive scene support

Validation checks:

```sh
./gradlew :examples:nav3-androidx:checkLaydrRoutes
./gradlew :examples:nav3-androidx:compileDebugKotlin
./gradlew :examples:nav3-androidx:assembleDebug
```
