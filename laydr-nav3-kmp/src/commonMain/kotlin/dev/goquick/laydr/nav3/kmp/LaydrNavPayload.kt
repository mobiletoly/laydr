// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.nav.runtime.LaydrNavEntryStore as RuntimeLaydrNavEntryStore
import dev.goquick.laydr.nav.runtime.LaydrNavPayloadLookup as RuntimeLaydrNavPayloadLookup
import dev.goquick.laydr.nav.runtime.LaydrNavResultLookup as RuntimeLaydrNavResultLookup
import kotlin.reflect.KClass

@PublishedApi
internal sealed interface LaydrNavPayloadLookup {
    data object Missing : LaydrNavPayloadLookup

    class Present(
        val payload: Any,
    ) : LaydrNavPayloadLookup
}

@PublishedApi
internal val LocalLaydrNavPayloadLookup: ProvidableCompositionLocal<LaydrNavPayloadLookup> =
    staticCompositionLocalOf<LaydrNavPayloadLookup> {
        LaydrNavPayloadLookup.Missing
    }

@PublishedApi
internal sealed interface LaydrNavResultLookup {
    data object Missing : LaydrNavResultLookup

    class Present(
        val resultType: KClass<*>,
        val sink: LaydrNavResultSink<*>,
    ) : LaydrNavResultLookup
}

@PublishedApi
internal val LocalLaydrNavResultLookup: ProvidableCompositionLocal<LaydrNavResultLookup> =
    staticCompositionLocalOf<LaydrNavResultLookup> {
        LaydrNavResultLookup.Missing
    }

/**
 * Returns the current Laydr Nav route launch payload when it exists and has
 * type [T].
 *
 * Payloads are transient app-owned values scoped to one Nav3 stack entry. They
 * are not route identity, route metadata, dependencies, saved state, or app
 * policy.
 */
@Composable
public inline fun <reified T : Any> laydrNavPayloadOrNull(): T? =
    when (val lookup = LocalLaydrNavPayloadLookup.current) {
        LaydrNavPayloadLookup.Missing -> null
        is LaydrNavPayloadLookup.Present -> lookup.payload as? T
    }

/**
 * Returns the current Laydr Nav route launch payload with type [T].
 *
 * Throws [IllegalStateException] when the current entry has no payload or when
 * the payload has a different type.
 */
@Composable
public inline fun <reified T : Any> requireLaydrNavPayload(): T {
    return when (val lookup = LocalLaydrNavPayloadLookup.current) {
        LaydrNavPayloadLookup.Missing -> {
            error("No Laydr Nav payload is available for the current entry")
        }
        is LaydrNavPayloadLookup.Present -> {
            lookup.payload as? T
                ?: error(
                    "Laydr Nav payload type mismatch. " +
                        "Expected ${T::class}, found ${lookup.payload::class}.",
                )
        }
    }
}

/**
 * One-shot route result sink for the current Laydr Nav stack entry.
 *
 * Completing or canceling a result only delivers the registered callback. It
 * does not mutate the Nav3 stack or own app policy.
 */
public class LaydrNavResultSink<Result : Any> internal constructor(
    @PublishedApi
    internal val resultType: KClass<*>,
    private val entryToken: String,
    private val entryStore: LaydrNavEntryStore,
) {
    /**
     * Completes the pending route result with [result].
     */
    public fun complete(result: Result) {
        entryStore.completeResult(
            entryToken = entryToken,
            resultType = resultType,
            result = result,
        )
    }

    /**
     * Cancels the pending route result.
     */
    public fun cancel() {
        entryStore.cancelResult(
            entryToken = entryToken,
            resultType = resultType,
        )
    }
}

/**
 * Returns the current Laydr Nav route result sink when one exists and has type
 * [Result].
 */
@Composable
public inline fun <reified Result : Any> laydrNavResultSinkOrNull(): LaydrNavResultSink<Result>? =
    when (val lookup = LocalLaydrNavResultLookup.current) {
        LaydrNavResultLookup.Missing -> null
        is LaydrNavResultLookup.Present -> {
            if (lookup.resultType == Result::class) {
                @Suppress("UNCHECKED_CAST")
                (lookup.sink as LaydrNavResultSink<Result>)
            } else {
                null
            }
        }
    }

/**
 * Returns the current Laydr Nav route result sink with type [Result].
 *
 * Throws [IllegalStateException] when the current entry has no pending result
 * or when the pending result has a different type.
 */
@Composable
public inline fun <reified Result : Any> requireLaydrNavResultSink(): LaydrNavResultSink<Result> {
    return when (val lookup = LocalLaydrNavResultLookup.current) {
        LaydrNavResultLookup.Missing -> {
            error("No Laydr Nav result sink is available for the current entry")
        }
        is LaydrNavResultLookup.Present -> {
            if (lookup.resultType == Result::class) {
                @Suppress("UNCHECKED_CAST")
                (lookup.sink as LaydrNavResultSink<Result>)
            } else {
                error(
                    "Laydr Nav result sink type mismatch. " +
                        "Expected ${Result::class}, found ${lookup.resultType}.",
                )
            }
        }
    }
}

internal class LaydrNavEntryStore(
    private val currentStack: () -> List<NavKey>,
) {
    internal val runtimeStore = RuntimeLaydrNavEntryStore {
        currentStack().mapNotNull { key -> (key as? LaydrNavKey)?.entryKey }
    }

    fun keyWithPayload(
        key: LaydrNavKey,
        payload: Any,
    ): LaydrNavKey =
        LaydrNavKey(runtimeStore.keyWithPayload(key = key.entryKey, payload = payload))

    fun keyForLaunch(
        key: LaydrNavKey,
        launch: LaydrNavLaunch,
    ): LaydrNavKey =
        LaydrNavKey(runtimeStore.keyForLaunch(key = key.entryKey, launch = launch))

    fun keyForLaunchAndResult(
        key: LaydrNavKey,
        launch: LaydrNavLaunch,
        resultType: KClass<*>,
        onCancel: () -> Unit,
        onResult: (Any) -> Unit,
    ): LaydrNavKey =
        LaydrNavKey(
            runtimeStore.keyForLaunchAndResult(
                key = key.entryKey,
                launch = launch,
                resultType = resultType,
                onCancel = onCancel,
                onResult = onResult,
            ),
        )

    fun keyWithResult(
        key: LaydrNavKey,
        resultType: KClass<*>,
        onCancel: () -> Unit,
        onResult: (Any) -> Unit,
    ): LaydrNavKey =
        LaydrNavKey(
            runtimeStore.keyWithResult(
                key = key.entryKey,
                resultType = resultType,
                onCancel = onCancel,
                onResult = onResult,
            ),
        )

    fun keyWithPayloadAndResult(
        key: LaydrNavKey,
        payload: Any,
        resultType: KClass<*>,
        onCancel: () -> Unit,
        onResult: (Any) -> Unit,
    ): LaydrNavKey =
        LaydrNavKey(
            runtimeStore.keyWithPayloadAndResult(
                key = key.entryKey,
                payload = payload,
                resultType = resultType,
                onCancel = onCancel,
                onResult = onResult,
            ),
        )

    fun lookupPayload(key: LaydrNavKey): LaydrNavPayloadLookup {
        return when (val lookup = runtimeStore.lookupPayload(key.entryKey)) {
            RuntimeLaydrNavPayloadLookup.Missing -> LaydrNavPayloadLookup.Missing
            is RuntimeLaydrNavPayloadLookup.Present -> LaydrNavPayloadLookup.Present(lookup.payload)
        }
    }

    fun lookupResult(key: LaydrNavKey): LaydrNavResultLookup {
        return when (val lookup = runtimeStore.lookupResult(key.entryKey)) {
            RuntimeLaydrNavResultLookup.Missing -> LaydrNavResultLookup.Missing
            is RuntimeLaydrNavResultLookup.Present -> LaydrNavResultLookup.Present(
                resultType = lookup.resultType,
                sink = LaydrNavResultSink<Any>(
                    resultType = lookup.resultType,
                    entryToken = key.entryToken ?: return LaydrNavResultLookup.Missing,
                    entryStore = this,
                ),
            )
        }
    }

    fun completeResult(
        entryToken: String,
        resultType: KClass<*>,
        result: Any,
    ) {
        runtimeStore.completeResult(entryToken = entryToken, resultType = resultType, result = result)
    }

    fun cancelResult(
        entryToken: String,
        resultType: KClass<*>,
    ) {
        runtimeStore.cancelResult(entryToken = entryToken, resultType = resultType)
    }

    fun prune() {
        runtimeStore.prune()
    }

    internal val payloadCount: Int
        get() = runtimeStore.payloadCount

    internal val resultCount: Int
        get() = runtimeStore.resultCount
}
