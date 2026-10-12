# Publishing

This page is for Laydr maintainers publishing framework artifacts. Application
developers should normally read `docs/user/gradle.md` for consumption setup.

## Coordinates

Published coordinates are controlled by `gradle.properties`:

```properties
laydr.group=dev.goquick.laydr
laydr.version=0.3.0
```

The root build applies these values to product subprojects. The
`laydr.publishing` convention also uses them as a fallback when
`laydr-gradle-plugin` is evaluated as an included build.

Remote publishing requires an explicit `laydr.version` from the current build
or parent `../gradle.properties`. If no version is configured,
`laydr.publishing` fails remote publish tasks instead of silently publishing a
snapshot.

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

Local-only publishing must not require signing credentials. If the same
invocation also requests remote publication, remote signing policy takes
precedence for all publications. Task order and project-qualified task names
do not change that policy.

For consumer qualification, `./scripts/check-nav3-consumers.sh` publishes all
product artifacts and the plugin marker to a fresh directory under
`build/nav3-consumers.*`. It supplies `nav3VerificationRepository` and a
verification-only snapshot coordinate, then builds independent consumers.
`publishAllPublicationsToNav3VerificationRepository` tasks use file repositories
without signing or Maven Central credentials in local-only invocations. They
do not replace Maven local artifacts. Compiler/dependency evidence and build
logs remain in that directory;
`build/nav3-validation/latest-consumer-run.txt` points to the latest run.

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
Signing is enabled when credentials are present and the invocation is not
local-only. Adding `publishToMavenLocal` or a Nav3 verification publication task
to a remote request does not suppress signing.

## GitHub Actions Release Workflow

`.github/workflows/publish.yml` publishes Maven/KMP artifacts from version
tags that match `vX.Y.Z` or `vX.Y.Z-*`.

The workflow pins `macos-26` rather than `macos-latest` so release validation
uses the current iOS simulator SDK needed by Compose UIKit dependencies.
Checkout and Java setup use maintained Node.js 24 action releases shared with
the Nav3 validation workflow. Action updates preserve release triggers, version
guards, credential handling, and manual dry-run-only behavior.

Release validation qualifies both catalog Kotlin consumer versions through
`./scripts/check-nav3-consumers.sh` before the Maven Central step. This gate
uses fresh verification publications and receives no publishing credentials;
Maven local assembly alone does not prove compatibility. Consumer reports and
compiler evidence are uploaded even when qualification fails. Automatic Nav3
push/PR CI uses the older compiler in JVM/Android scope. Full Native/Wasm
qualification of both compilers is required separately for upgrades and
releases; the default local runner and manual workflow provide that gate.
Release jobs cache Gradle and Native dependencies using the
same bounded cache policy described in the engineering guide.

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
