# Publishing

This page is for Laydr maintainers publishing framework artifacts. Application
developers should normally read `docs/user/gradle.md` for consumption setup.

## Coordinates

Published coordinates are controlled by `gradle.properties`:

```properties
laydr.group=dev.goquick.laydr
laydr.version=0.1.0-SNAPSHOT
```

The root build applies these values to product subprojects. The
`laydr.publishing` convention also uses them as a fallback when
`laydr-gradle-plugin` is evaluated as an included build.

Remote publishing requires an explicit `laydr.version` from the current build
or parent `../gradle.properties`. If no version is configured,
`laydr.publishing` fails remote publish tasks instead of silently publishing a
snapshot. The local `0.1.0-SNAPSHOT` fallback is only a development convenience
for non-remote tasks and `publishToMavenLocal`.

The standalone plugin build includes `laydr-codegen` for plugin implementation
but intentionally does not include `laydr-core`; codegen emits core symbol
references but does not compile against the KMP runtime module.

## JVM Compatibility

Published Laydr JVM artifacts target JVM 17 bytecode. Build conventions use a
JDK 17 toolchain for product Kotlin JVM compilations so downstream JVM 17 KMP
apps can consume runtime artifacts, including inline APIs.

## Local Publish

Publish all Laydr product modules and the Gradle plugin marker to Maven local:

```sh
./gradlew publishToMavenLocal
```

Focused checks:

```sh
./gradlew :laydr-core:publishToMavenLocal
./gradlew :laydr-gradle-plugin:publishToMavenLocal
```

Local publishing must not require signing credentials.

## Published Modules

- `dev.goquick.laydr:laydr-core`
- `dev.goquick.laydr:laydr-compose`
- `dev.goquick.laydr:laydr-workflow`
- `dev.goquick.laydr:laydr-nav-runtime`
- `dev.goquick.laydr:laydr-nav3-kmp`
- `dev.goquick.laydr:laydr-nav3-androidx`
- `dev.goquick.laydr:laydr-nav3-kmp-adaptive`
- `dev.goquick.laydr:laydr-codegen`
- `dev.goquick.laydr:laydr-gradle-plugin`
- plugin marker `dev.goquick.laydr:dev.goquick.laydr.gradle.plugin`

Examples are not published.

## Maven Central

The build exposes Maven Central publish tasks through Vanniktech Maven Publish:

```sh
./gradlew publishToMavenCentral
```

Remote publishing requires external Maven Central and signing credentials.
Signing is enabled only when credentials are present and the task is not
`publishToMavenLocal`.

## GitHub Actions Release Workflow

`.github/workflows/publish.yml` publishes Maven/KMP artifacts from version
tags that match `vX.Y.Z` or `vX.Y.Z-*`.

The workflow pins `macos-26` rather than `macos-latest` so release validation
uses the current iOS simulator SDK needed by Compose UIKit dependencies.

Before publishing, the workflow verifies that:

- the release version is not a snapshot
- `gradle.properties` contains `laydr.version=<tag version without v>`
- the focused release validation build tests every published module with a
  configured JVM or iOS simulator test suite
- `publishToMavenLocal` can assemble every configured publication

Manual `workflow_dispatch` runs are validation-only and must keep
`dry_run=true`. This keeps Maven Central credentials out of dry runs because
Vanniktech resolves remote credentials during Gradle configuration for
`publishToMavenCentral`.

The tag publish step expects these GitHub secrets:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_KEY_ID`, the last 8 hexadecimal characters of the GPG signing key
  id, optionally prefixed with `0x`
- `SIGNING_PASSWORD`
- `GPG_KEY_CONTENTS`

Tag publishes validate that all five secrets are present and that
`SIGNING_KEY_ID` matches Gradle signing's expected key-id shape before running
the release build.

The workflow uploads and validates with Maven Central through Vanniktech. Final
Central Portal release remains a maintainer action.

Android library artifacts publish an empty javadoc jar. This avoids running
AGP javadoc generation during release while still satisfying Maven Central's
javadoc artifact requirement.
