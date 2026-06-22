// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

/**
 * Runtime module marker used by tests and dependency inspections.
 *
 * This module is intentionally adapter-neutral. It must not depend on Compose,
 * AndroidX Navigation 3, JetBrains Navigation3 KMP, Material adaptive APIs,
 * Gradle APIs, Android APIs, or Laydr code generation.
 */
public object LaydrNavRuntime
