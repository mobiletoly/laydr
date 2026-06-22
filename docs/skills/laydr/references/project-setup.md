# Project Setup

Use this reference when adding Laydr to a Kotlin Multiplatform app,
Android-only Compose app, or checking an existing app setup.

## Local Artifact Or Source Consumption

When the app should consume artifact-style local builds, ensure the consuming
app has `mavenLocal()` in both plugin and dependency repositories:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
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

Use this only when Laydr artifacts are already installed in Maven local.
When actively editing Laydr and the app together, prefer composite build
wiring:

```kotlin
pluginManagement {
    includeBuild("../laydr")
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

includeBuild("../laydr")
```

Use the Laydr root checkout in both places. Do not also include
`../laydr/laydr-gradle-plugin`, because that can give Gradle and IDE import two
build identities for the same source checkout.

If the app already has wrapper conventions or version catalogs, follow the app
pattern instead of forcing this exact snippet.

## Version Catalog

Use `LAYDR_VERSION` as a placeholder for the Laydr artifact version. Replace it
with the version selected by the app or the local artifact set, and define it
once:

```toml
[versions]
laydr = "LAYDR_VERSION"

[plugins]
laydr = { id = "dev.goquick.laydr", version.ref = "laydr" }

[libraries]
laydr-compose = { module = "dev.goquick.laydr:laydr-compose", version.ref = "laydr" }
laydr-nav3-kmp = { module = "dev.goquick.laydr:laydr-nav3-kmp", version.ref = "laydr" }
laydr-nav3-kmp-adaptive = { module = "dev.goquick.laydr:laydr-nav3-kmp-adaptive", version.ref = "laydr" }
laydr-nav3-androidx = { module = "dev.goquick.laydr:laydr-nav3-androidx", version.ref = "laydr" }
laydr-workflow = { module = "dev.goquick.laydr:laydr-workflow", version.ref = "laydr" }
```

## Gradle Module

Apply Laydr in the KMP or Android-only module that owns the route tree.

KMP module:

```kotlin
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.laydr)
}

laydr {
    generatedPackage.set("example.app.generated")
    compose.set(true)
}
```

Android-only module:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.laydr)
}

laydr {
    generatedPackage.set("example.app.generated")
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
```

Default route roots:

```text
src/commonMain/kotlin/routes  # KMP
src/main/kotlin/routes        # Android-only
```

Default generated package:

```text
dev.goquick.laydr.generated
```

## Dependencies

The Gradle plugin does not add runtime dependencies automatically. Add the
modules required by the app surface:

```kotlin
commonMain.dependencies {
    implementation(libs.laydr.compose)
    implementation(libs.laydr.nav3.kmp)
    implementation(libs.laydr.workflow)
}
```

Use `laydr-compose` for `LaydrRouteHost`, `laydr-nav3-kmp` for
JetBrains Navigation3 KMP integration, and `laydr-workflow` when route-local
workflows are hosted.
Add `laydr-nav3-kmp-adaptive` only when the app uses optional Material
adaptive list/detail scenes.
Use `laydr-nav3-androidx` for Android-only apps using Google AndroidX
Navigation 3. AndroidX adaptive scene support is not implemented.

If plugin resolution uses a composite build or app convention that supplies
the plugin version, follow that app pattern instead of forcing the literal
version into the module.

## First Route Checklist

- Add one route directory with `Route.kt`.
- Bind screen UI through generated `LaydrRouteDef`.
- Run `checkLaydrRoutes`.
- Compile the affected source set.

Use `generated-api.md` before naming generated route objects and
`troubleshooting.md` when validation fails.
