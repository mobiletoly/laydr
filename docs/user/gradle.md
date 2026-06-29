# Gradle

Use this page when wiring the Laydr plugin, runtime dependencies, adapter
flags, generated source locations, or validation tasks.

The key boundary: the Gradle plugin is build-time wiring. It scans `routes/`,
validates the tree, and adds generated source directories. Runtime artifacts
are explicit app dependencies, so the module only pays for the host or adapter
it actually uses.

## Before You Copy

The snippets assume your app already has Kotlin, Compose, Android, or KMP
plugins configured. Laydr adds route scanning, generated source wiring, and
runtime artifacts. It does not configure Compose or Android for the app.

Replace `LAYDR_VERSION` with the Laydr version your app should consume. For a
published release, use that release number. For local checkout dogfooding, use
the locally published version from [Local Checkout](#local-checkout).

## Version Catalog

Define Laydr once:

```toml
[versions]
laydr = "LAYDR_VERSION"

[plugins]
laydr = { id = "dev.goquick.laydr", version.ref = "laydr" }

[libraries]
laydr-compose = { module = "dev.goquick.laydr:laydr-compose", version.ref = "laydr" }
laydr-workflow = { module = "dev.goquick.laydr:laydr-workflow", version.ref = "laydr" }
laydr-nav3-kmp = { module = "dev.goquick.laydr:laydr-nav3-kmp", version.ref = "laydr" }
laydr-nav3-kmp-adaptive = { module = "dev.goquick.laydr:laydr-nav3-kmp-adaptive", version.ref = "laydr" }
laydr-nav3-androidx = { module = "dev.goquick.laydr:laydr-nav3-androidx", version.ref = "laydr" }
```

## Repository Setup

Make sure the app can resolve Gradle plugins and dependencies:

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

## Local Checkout

When testing an unpublished Laydr checkout from another app, publish Laydr to
Maven local from this repository:

```sh
./gradlew publishToMavenLocal
```

Then add `mavenLocal()` to both plugin and dependency repositories in the
consumer app:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}
```

Use `0.2.0` as `LAYDR_VERSION` for the default local publish. If the
checkout was published with `-Playdr.version=...`, use that exact version
instead.

## KMP Module Template

Use this in the KMP module that owns `src/commonMain/kotlin/routes`:

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
    generatedPackage.set("example.app.generated")
    compose.set(true)
}
```

Default route root:

```text
src/commonMain/kotlin/routes
```

Default generated source:

```text
build/generated/laydr/commonMain/kotlin
```

Add Nav3 KMP only when this shared module uses Nav3 stacks or sections:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.nav3.kmp)
        }
    }
}

laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

## Android-Only Module Template

Use this in an Android application or library module that owns
`src/main/kotlin/routes`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.laydr)
}

dependencies {
    implementation(libs.laydr.compose)
}

laydr {
    generatedPackage.set("example.app.generated")
    compose.set(true)
}
```

Default route root:

```text
src/main/kotlin/routes
```

Default generated source:

```text
build/generated/laydr/main/kotlin
```

Add AndroidX Nav3 only when the Android module uses Google AndroidX
Navigation 3:

```kotlin
dependencies {
    implementation(libs.laydr.nav3.androidx)
}

laydr {
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
```

Use AndroidX Nav3 only for Android-only Compose modules. Use Nav3 KMP in a
shared KMP route module.

## Runtime Dependency Choices

The Laydr plugin does not add runtime dependencies automatically.

| If the app uses | Add | Enable |
| --- | --- | --- |
| Plain `LaydrRouteHost` | `laydr-compose` | `compose.set(true)` |
| Nav3 KMP stacks or sections | `laydr-compose`, `laydr-nav3-kmp` | `compose.set(true)`, `adapters.nav3Kmp.set(true)` |
| Android-only AndroidX Nav3 | `laydr-compose`, `laydr-nav3-androidx` | `compose.set(true)`, `adapters.nav3Androidx.set(true)` |
| Optional Material adaptive scenes for Nav3 KMP | `laydr-nav3-kmp-adaptive` | `adapters.nav3Kmp.set(true)` |
| Route-local workflow | `laydr-workflow` | no Laydr adapter flag |

`nav3Kmp` and `nav3Androidx` are mutually exclusive. Both require
`compose.set(true)`.

Use `laydr-nav3-kmp-adaptive` only when the app uses optional Material
adaptive list/detail scenes. On Android, that artifact inherits the Material
adaptive `compileSdk 37` requirement.

Most apps do not declare `laydr-core` directly. The Compose, Nav3, and
workflow artifacts bring in the lower-level runtime they need.

For KMP route-local workflow:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.workflow)
        }
    }
}
```

For Android-only route-local workflow:

```kotlin
dependencies {
    implementation(libs.laydr.workflow)
}
```

## Extension Options

```kotlin
laydr {
    routesDirectory.set(layout.projectDirectory.dir("src/commonMain/kotlin/routes"))
    generatedPackage.set("example.app.generated")
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

Options:

- `routesDirectory`: route root to scan.
- `generatedPackage`: package for generated app-level source.
- `compose`: generate Compose definitions and route-local `LaydrRouteDef`
  helpers.
- `adapters.nav3Kmp`: generate KMP Nav3 `LaydrNavRoutes` helpers.
- `adapters.nav3Androidx`: generate AndroidX Nav3 `LaydrNavRoutes` helpers.

## Tasks

Validate without writing generated source:

```sh
./gradlew :shared:checkLaydrRoutes
```

Generate source:

```sh
./gradlew :shared:generateLaydrRoutes
```

Android-only module examples:

```sh
./gradlew :app:checkLaydrRoutes
./gradlew :app:compileDebugKotlin
```

The normal Gradle `check` task depends on `checkLaydrRoutes`.

## Runtime Artifact Coordinates

Runtime artifacts:

- `dev.goquick.laydr:laydr-core:LAYDR_VERSION`
- `dev.goquick.laydr:laydr-compose:LAYDR_VERSION`
- `dev.goquick.laydr:laydr-workflow:LAYDR_VERSION`
- `dev.goquick.laydr:laydr-nav3-kmp:LAYDR_VERSION`
- `dev.goquick.laydr:laydr-nav3-kmp-adaptive:LAYDR_VERSION`
- `dev.goquick.laydr:laydr-nav3-androidx:LAYDR_VERSION`

Laydr KMP runtime artifacts publish JVM, iOS, and WasmJS variants. The
AndroidX Nav3 artifact is Android-only. Laydr JVM artifacts target JVM 17
bytecode, so JVM and desktop KMP consumers may compile with
`jvmTarget = JVM_17` when using Laydr runtime artifacts.

## When Generated APIs Are Missing

Check these in order:

1. Is the Laydr plugin applied to the module that owns `routes/`?
2. Does the route root exist in the expected source set?
3. Is `compose.set(true)` enabled when `Route.kt` uses `LaydrRouteDef`?
4. Is the correct Nav3 adapter flag enabled when you import
   `LaydrNavRoutes`?
5. Did `checkLaydrRoutes` or `generateLaydrRoutes` run after route changes?

See [Troubleshooting](troubleshooting.md) for symptom-specific fixes.
