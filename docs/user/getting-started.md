# Getting Started

This tutorial builds one tiny Compose Multiplatform route tree and renders it
with `LaydrRouteHost`.

Start here for Compose Multiplatform apps, even if the app will later use
Nav3 KMP. Android-only apps should start with
[AndroidX Nav3](navigation/androidx.md); the route concepts are the same, but
the source set is different.

You will:

1. configure the KMP module that owns routes
2. create two route directories
3. declare route-local screens
4. render the current path with plain Compose
5. run Laydr validation

The snippets use `LAYDR_VERSION` for the Laydr artifact version. Replace it
with the version your app should consume.

## What You Are Building

The tutorial route tree has two screens:

| Route directory | Path | Generated object |
| --- | --- | --- |
| `routes/contacts` | `/contacts` | `LaydrRoutes.Contacts` |
| `routes/contacts/by_id` | `/contacts/{id}` | `LaydrRoutes.Contacts.ById` |

The app owns contacts data, UI components, and current path state. Laydr owns
route validation, typed path builders, generated route definitions, and route
content wiring.

## 1. Pick The Route Module

For a Compose Multiplatform app, put routes in the shared KMP module:

```text
shared/
  src/commonMain/kotlin/routes/
```

Laydr generates source for that source set:

```text
shared/build/generated/laydr/commonMain/kotlin/
```

Android-only apps use a different route root. Read
[AndroidX Nav3](navigation/androidx.md) for the Android-only shape.

## 2. Add Laydr To The Build

Make sure your project can resolve Gradle plugins and dependencies:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}
```

Define Laydr in the version catalog:

```toml
[versions]
laydr = "LAYDR_VERSION"

[plugins]
laydr = { id = "dev.goquick.laydr", version.ref = "laydr" }

[libraries]
laydr-compose = { module = "dev.goquick.laydr:laydr-compose", version.ref = "laydr" }
```

Apply the plugin in the KMP module that owns `routes/`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.laydr)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.compose)
        }
    }
}

laydr {
    generatedPackage.set("example.contacts.generated")
    compose.set(true)
}
```

This assumes Kotlin Multiplatform and Compose are already configured in the
module. Read [Gradle](gradle.md) when you need full setup templates or Nav3
runtime dependencies.

## 3. Create The Route Tree

Create this source tree in the shared module:

```text
src/commonMain/kotlin/
  example/contacts/
    ContactsApp.kt
    ContactsRepository.kt
    ContactsUi.kt
  routes/
    contacts/
      Route.kt
      Screen.kt
      by_id/
        Route.kt
        Screen.kt
```

Files under `example/contacts/` are normal app code. Laydr does not generate
repositories, UI components, ViewModels, dependency containers, or test
skeletons.

`by_id` is a dynamic route segment. Laydr generates a route-scoped parameter
factory named `id(...)` and exposes the typed value as `route.id`.

## 4. Declare The Contacts Screen

Create `routes/contacts/Route.kt`:

```kotlin
package routes.contacts

internal val Route = LaydrRouteDef.screen { route ->
    Screen(route = route)
}
```

Create `routes/contacts/Screen.kt`:

```kotlin
package routes.contacts

import androidx.compose.runtime.Composable
import example.contacts.ContactsRouteDependencies
import example.contacts.ContactsScreen
import example.contacts.LocalContactsRouteDependencies
import example.contacts.generated.LaydrRoutes

@Composable
internal fun Screen(
    route: LaydrRoutes.Contacts.Destination,
    dependencies: ContactsRouteDependencies = LocalContactsRouteDependencies.current,
) {
    ContactsScreen(
        contacts = dependencies.repository.listContacts(),
        selectedPath = route.path,
        onContactClick = dependencies.openContact,
    )
}
```

`LaydrRouteDef` is generated into the route package, so `Route.kt` usually
uses it without an import. The `Screen` composable is yours. It can receive
plain parameters for previews, use a narrow `CompositionLocal`, or read from
app-owned DI.

## 5. Declare The Detail Screen

Create `routes/contacts/by_id/Route.kt`:

```kotlin
package routes.contacts.by_id

internal val Route = LaydrRouteDef.screen { route ->
    Screen(route = route)
}
```

Create `routes/contacts/by_id/Screen.kt`:

```kotlin
package routes.contacts.by_id

import androidx.compose.runtime.Composable
import example.contacts.ContactDetailScreen
import example.contacts.ContactsRouteDependencies
import example.contacts.LocalContactsRouteDependencies
import example.contacts.generated.LaydrRoutes

@Composable
internal fun Screen(
    route: LaydrRoutes.Contacts.ById.Destination,
    dependencies: ContactsRouteDependencies = LocalContactsRouteDependencies.current,
) {
    ContactDetailScreen(
        contact = dependencies.repository.requireContact(route.id.value),
        onBack = dependencies.openContacts,
    )
}
```

Use `route.id.value` only when passing the generated parameter back into
app-owned APIs that expect a plain `String`. Inside Laydr navigation APIs, use
generated destinations and generated parameter factories.

## 6. Render The Current Path

Use `LaydrRouteHost` when your app owns the current path string:

```kotlin
package example.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.goquick.laydr.compose.LaydrRouteHost
import example.contacts.generated.LaydrComposeRoutes
import example.contacts.generated.LaydrRoutes

@Composable
internal fun ContactsApp(repository: ContactsRepository) {
    var currentPath by remember { mutableStateOf(LaydrRoutes.Contacts.path()) }

    val openContacts = {
        currentPath = LaydrRoutes.Contacts.path()
    }
    val openContact = { id: String ->
        currentPath = LaydrRoutes.Contacts.ById.path(
            id = LaydrRoutes.Contacts.ById.id(id),
        )
    }

    ProvideContactsRouteDependencies(
        repository = repository,
        openContacts = openContacts,
        openContact = openContact,
    ) {
        LaydrRouteHost(
            currentPath = currentPath,
            routeDefinitions = LaydrComposeRoutes.definitions,
            notFoundContent = { path -> NotFound(path) },
        )
    }
}
```

`ProvideContactsRouteDependencies` is app code. It can be a tiny
`CompositionLocal` provider, a DI boundary, or explicit parameters passed into
your route entry composables. Laydr does not require one dependency style.

For the smallest possible app, use a `CompositionLocal` provider around the
host:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

internal data class ContactsRouteDependencies(
    val repository: ContactsRepository,
    val openContacts: () -> Unit,
    val openContact: (id: String) -> Unit,
)

internal val LocalContactsRouteDependencies =
    staticCompositionLocalOf<ContactsRouteDependencies> {
        error("ContactsRouteDependencies was not provided")
    }

@Composable
internal fun ProvideContactsRouteDependencies(
    repository: ContactsRepository,
    openContacts: () -> Unit,
    openContact: (id: String) -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContactsRouteDependencies provides ContactsRouteDependencies(
            repository = repository,
            openContacts = openContacts,
            openContact = openContact,
        ),
        content = content,
    )
}
```

Read [Route Dependencies](route-dependencies.md) before turning this small
teaching provider into a broad root app context.

## 7. Run The Feedback Loop

After adding or renaming route files, run:

```sh
./gradlew :shared:checkLaydrRoutes
```

Generate source when you need generated APIs for compilation or IDE indexing:

```sh
./gradlew :shared:generateLaydrRoutes
```

If your IDE still cannot resolve `LaydrRouteDef`, `LaydrRoutes`, or
`LaydrComposeRoutes`, refresh the Gradle model after generation.

The normal Gradle `check` task depends on `checkLaydrRoutes`.

## 8. Choose The Next Runtime

Stay with `LaydrRouteHost` when the app only needs path-in/content-out
rendering and owns path state directly.

Use [Navigation](navigation/README.md) when the app needs Nav3 KMP stacks,
sections, Back behavior, payloads, results, adaptive scenes, or `NavDisplay`.

Use [AndroidX Nav3](navigation/androidx.md) when the app is Android-only and
uses Google AndroidX Navigation 3 instead of a shared KMP route module.

## 9. Next Pages

Read these next:

- [Concepts](concepts.md) if the model is still fuzzy.
- [Gradle](gradle.md) for complete setup templates and runtime dependencies.
- [Routes](routes.md) for route patterns, route kinds, layouts, and
  validation rules.
- [Generated API Tour](generated-api.md) when you need to know which generated
  object to call.
- [Route Dependencies](route-dependencies.md) before adding broad app context
  objects or providers.
- [Troubleshooting](troubleshooting.md) when validation or compilation fails.
