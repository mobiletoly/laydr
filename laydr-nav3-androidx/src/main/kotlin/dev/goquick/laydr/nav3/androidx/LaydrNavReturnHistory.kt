// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.androidx

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList

internal typealias LaydrNavReturnEntry = dev.goquick.laydr.nav.runtime.LaydrNavReturnEntry

internal fun laydrNavReturnHistorySaver(): Saver<SnapshotStateList<LaydrNavReturnEntry>, Any> =
    listSaver(
        save = { history -> saveLaydrNavReturnHistory(history) },
        restore = { saved -> restoreLaydrNavReturnHistory(saved) },
    )

internal fun saveLaydrNavReturnHistory(
    history: List<LaydrNavReturnEntry>,
): List<String> =
    dev.goquick.laydr.nav.runtime.saveLaydrNavReturnHistory(history)

internal fun restoreLaydrNavReturnHistory(saved: List<String>): SnapshotStateList<LaydrNavReturnEntry>? {
    val restored = dev.goquick.laydr.nav.runtime.restoreLaydrNavReturnHistory(saved)
        ?: return null
    return mutableStateListOf<LaydrNavReturnEntry>().apply {
        addAll(restored)
    }
}
