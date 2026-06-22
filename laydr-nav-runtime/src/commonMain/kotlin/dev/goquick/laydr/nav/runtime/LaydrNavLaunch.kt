// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

import dev.goquick.laydr.core.LaydrScreenDestination

/**
 * App-owned launch data for one Laydr navigation entry.
 *
 * [destination] is the generated screen destination to launch. [payload] is a
 * transient app-owned value scoped to the created entry. [entryMetadata] is
 * copied to the resolved adapter entry metadata after adapter-level metadata
 * has been applied.
 */
public class LaydrNavLaunch public constructor(
    /**
     * Generated screen destination to launch.
     */
    public val destination: LaydrScreenDestination,
    /**
     * Optional transient entry payload.
     */
    public val payload: Any? = null,
    /**
     * App-owned metadata for the created entry.
     */
    public val entryMetadata: LaydrNavEntryMetadata = LaydrNavEntryMetadata.Empty,
)
