#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "Native consumer qualification requires macOS." >&2
    exit 1
fi
mkdir -p "$repository_root/build/nav3-validation"
run_root="$(mktemp -d "$repository_root/build/nav3-consumers.XXXXXX")"
artifact_repository="$run_root/maven"
artifact_version="0.3.0-nav3-binding-check-SNAPSHOT"
printf '%s\n' "$run_root" > "$repository_root/build/nav3-validation/latest-consumer-run.txt"

"$repository_root/gradlew" -p "$repository_root" writeNav3ConsumerVersions \
    --no-configuration-cache | tee "$run_root/versions.log"
version_matrix="$run_root/consumer-versions.tsv"
cp "$repository_root/build/nav3-validation/consumer-versions.tsv" "$version_matrix"
if [[ $# -eq 0 ]]; then
    while IFS=$'\t' read -r compiler compose nav agp activity; do
        set -- "$@" "$compiler"
    done < "$version_matrix"
fi
for consumer_version in "$@"; do
    found=false
    while IFS=$'\t' read -r compiler compose nav agp activity; do
        if [[ "$compiler" == "$consumer_version" ]]; then found=true; break; fi
    done < "$version_matrix"
    if [[ "$found" != true ]]; then
        echo "Unsupported qualification compiler: $consumer_version" >&2
        exit 1
    fi
done

"$repository_root/gradlew" -p "$repository_root" \
    publishAllPublicationsToNav3VerificationRepository \
    :laydr-gradle-plugin:publishAllPublicationsToNav3VerificationRepository \
    -Playdr.version="$artifact_version" \
    -Pnav3VerificationRepository="$artifact_repository" \
    --no-configuration-cache | tee "$run_root/publication.log"

for consumer_version in "$@"; do
    while IFS=$'\t' read -r compiler compose_version nav_version agp_version activity_version; do
        if [[ "$compiler" == "$consumer_version" ]]; then break; fi
    done < "$version_matrix"
    consumer_root="$run_root/$consumer_version"
    mkdir -p "$consumer_root"
    cp -R "$repository_root/verification/nav3-consumer/." "$consumer_root/"
    "$repository_root/gradlew" -p "$consumer_root" \
        :kmp:verifyConsumerCompiler :androidApp:verifyConsumerCompiler \
        :kmp:checkLaydrRoutes :androidApp:checkLaydrRoutes \
        :kmp:jvmTest :androidApp:assembleDebug \
        :kmp:linkDebugFrameworkIosArm64 :kmp:linkDebugFrameworkIosSimulatorArm64 \
        :kmp:compileDevelopmentExecutableKotlinWasmJs \
        -PconsumerKotlinVersion="$consumer_version" \
        -Pkotlin.native.version="$consumer_version" \
        -PconsumerComposeVersion="$compose_version" \
        -PconsumerNav3Version="$nav_version" \
        -PconsumerAgpVersion="$agp_version" \
        -PconsumerActivityVersion="$activity_version" \
        -PconsumerLaydrVersion="$artifact_version" \
        -PconsumerRepository="$artifact_repository" \
        --no-configuration-cache | tee "$run_root/$consumer_version.log"
    android_only_root="$run_root/android-only-$consumer_version"
    mkdir -p "$android_only_root"
    cp -R "$repository_root/verification/nav3-consumer/." "$android_only_root/"
    cp "$android_only_root/android-only.gradle.kts" "$android_only_root/build.gradle.kts"
    "$repository_root/gradlew" -p "$android_only_root" \
        :androidApp:verifyConsumerCompiler :androidApp:checkLaydrRoutes :androidApp:assembleDebug \
        -PconsumerAndroidOnly=true \
        -PconsumerKotlinVersion="$consumer_version" \
        -PconsumerComposeVersion="$compose_version" \
        -PconsumerNav3Version="$nav_version" \
        -PconsumerAgpVersion="$agp_version" \
        -PconsumerActivityVersion="$activity_version" \
        -PconsumerLaydrVersion="$artifact_version" \
        -PconsumerRepository="$artifact_repository" \
        --no-configuration-cache | tee "$run_root/android-only-$consumer_version.log"
done
echo "Consumer qualification passed. Evidence: $run_root"
