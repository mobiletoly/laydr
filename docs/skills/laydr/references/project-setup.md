# Project Setup

Use this when adding Laydr to an app or checking build wiring.

## Contents

- decide the app shape
- consume Laydr artifacts or source
- configure KMP modules
- configure Android-only modules
- add route-local workflow
- create the first route

## Decide The App Shape

| App shape | Route root | Generated source | Runtime |
| --- | --- | --- | --- |
| KMP with plain Compose host | `src/commonMain/kotlin/routes` | `build/generated/laydr/commonMain/kotlin` | `laydr-compose` |
| KMP with Nav3 sections/stacks | `src/commonMain/kotlin/routes` | `build/generated/laydr/commonMain/kotlin` | `laydr-nav3-kmp` |
| Android-only with AndroidX Nav3 | `src/main/kotlin/routes` | `build/generated/laydr/main/kotlin` | `laydr-nav3-androidx` |

The Laydr Gradle plugin scans routes and wires generated source. It does not
add runtime dependencies automatically.

## Consume Laydr

For published or locally published artifacts, define one version:

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

If artifacts are in Maven local, add `mavenLocal()` to both plugin and
dependency repositories. Use this only when the artifacts were already
published locally:

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

When actively editing Laydr and the app together, prefer a composite build:

```kotlin
pluginManagement {
    includeBuild("../laydr")
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

includeBuild("../laydr")
```

Use the Laydr root checkout. Do not also include
`../laydr/laydr-gradle-plugin`; that creates two Gradle identities for the
same source checkout.

## KMP Module

Apply Laydr in the KMP module that owns `src/commonMain/kotlin/routes`:

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

Add Nav3 KMP only when the shared module uses Nav3 sections or stacks:

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

Add `laydr-nav3-kmp-adaptive` only when using optional Material adaptive
list/detail scenes.

## Android-Only Module

Apply Laydr in the Android module that owns `src/main/kotlin/routes`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.laydr)
}

dependencies {
    implementation(libs.laydr.compose)
    implementation(libs.laydr.nav3.androidx)
}

laydr {
    generatedPackage.set("example.app.generated")
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
```

Do not enable `nav3Kmp` in the same route tree. AndroidX adaptive Laydr APIs
are not current behavior.

## Route-Local Workflow

Add workflow only to modules that host private route-local workflow state:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.workflow)
        }
    }
}
```

Android-only modules use normal Android dependencies:

```kotlin
dependencies {
    implementation(libs.laydr.workflow)
}
```

## First Route Checklist

1. Create one route directory under the route root.
2. Add `Route.kt` with one `LaydrRouteDef` declaration.
3. Put screen UI in nearby app-owned files such as `Screen.kt`.
4. Run `checkLaydrRoutes`.
5. Compile the affected source set.

Read `routes.md` before creating a larger route tree and
`troubleshooting.md` when validation fails.
