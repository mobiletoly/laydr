// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.androidx

import dev.goquick.laydr.nav.runtime.LaydrNavSceneSupport as RuntimeLaydrNavSceneSupport

/**
 * Non-adaptive AndroidX Nav3 scene support boundary for Laydr.
 *
 * AndroidX adaptive scene support is intentionally deferred. This type keeps
 * generated helper APIs parallel with the KMP adapter while exposing only the
 * no-adaptive behavior supported by this module.
 */
public class LaydrNavSceneSupport internal constructor(
    internal val runtimeSupport: RuntimeLaydrNavSceneSupport,
) {
    public companion object {
        /**
         * No AndroidX adaptive scene support.
         */
        public val None: LaydrNavSceneSupport =
            LaydrNavSceneSupport(RuntimeLaydrNavSceneSupport.None)
    }

    internal fun metadataFor(route: dev.goquick.laydr.core.LaydrRoute): Map<String, Any> =
        runtimeSupport.metadataFor(route)
}
