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
- `dev.goquick.laydr:laydr-nav3-kmp`
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
