// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

import kotlin.reflect.KClass

/**
 * Adapter-neutral lookup result for an entry payload.
 */
public sealed interface LaydrNavPayloadLookup {
    /**
     * No payload exists for the current entry.
     */
    public data object Missing : LaydrNavPayloadLookup

    /**
     * A payload exists for the current entry.
     */
    public class Present public constructor(
        /**
         * App-owned payload value.
         */
        public val payload: Any,
    ) : LaydrNavPayloadLookup
}

/**
 * Adapter-neutral lookup result for an entry result sink.
 */
public sealed interface LaydrNavResultLookup {
    /**
     * No result sink exists for the current entry.
     */
    public data object Missing : LaydrNavResultLookup

    /**
     * A result sink exists for the current entry.
     */
    public class Present public constructor(
        /**
         * Runtime result type expected by the sink.
         */
        public val resultType: KClass<*>,
        /**
         * Sink that completes or cancels the pending result.
         */
        public val sink: LaydrNavResultSink<*>,
    ) : LaydrNavResultLookup
}

/**
 * One-shot route result sink for an adapter-neutral Laydr navigation entry.
 */
public class LaydrNavResultSink<Result : Any> internal constructor(
    private val resultType: KClass<*>,
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
 * Stores transient entry payloads and result registrations for active Laydr
 * navigation entries.
 */
public class LaydrNavEntryStore public constructor(
    private val currentEntries: () -> List<LaydrNavEntryKey>,
) {
    private var nextEntryToken = 0
    private val payloadsByEntryToken = mutableMapOf<String, Any>()
    private val resultsByEntryToken = mutableMapOf<String, LaydrNavResultRegistration>()

    /**
     * Returns [key] with a new entry token and stores [payload] under it.
     */
    public fun keyWithPayload(
        key: LaydrNavEntryKey,
        payload: Any,
    ): LaydrNavEntryKey {
        val entryToken = nextEntryToken()
        payloadsByEntryToken[entryToken] = payload
        return key.withEntryToken(entryToken)
    }

    /**
     * Returns the entry key for [launch], storing payload data when present.
     */
    public fun keyForLaunch(
        key: LaydrNavEntryKey,
        launch: LaydrNavLaunch,
    ): LaydrNavEntryKey {
        val metadataKey = key.withEntryMetadata(launch.entryMetadata)
        val payload = launch.payload ?: return metadataKey
        return keyWithPayload(key = metadataKey, payload = payload)
    }

    /**
     * Returns the entry key for [launch] and registers a result callback.
     */
    public fun keyForLaunchAndResult(
        key: LaydrNavEntryKey,
        launch: LaydrNavLaunch,
        resultType: KClass<*>,
        onCancel: () -> Unit,
        onResult: (Any) -> Unit,
    ): LaydrNavEntryKey {
        val metadataKey = key.withEntryMetadata(launch.entryMetadata)
        return if (launch.payload == null) {
            keyWithResult(
                key = metadataKey,
                resultType = resultType,
                onCancel = onCancel,
                onResult = onResult,
            )
        } else {
            keyWithPayloadAndResult(
                key = metadataKey,
                payload = launch.payload,
                resultType = resultType,
                onCancel = onCancel,
                onResult = onResult,
            )
        }
    }

    /**
     * Returns [key] with a new entry token and registers a result callback.
     */
    public fun keyWithResult(
        key: LaydrNavEntryKey,
        resultType: KClass<*>,
        onCancel: () -> Unit,
        onResult: (Any) -> Unit,
    ): LaydrNavEntryKey {
        val entryToken = nextEntryToken()
        resultsByEntryToken[entryToken] = LaydrNavResultRegistration(
            resultType = resultType,
            onCancel = onCancel,
            onResult = onResult,
        )
        return key.withEntryToken(entryToken)
    }

    /**
     * Returns [key] with a new entry token and stores both payload and result
     * registration.
     */
    public fun keyWithPayloadAndResult(
        key: LaydrNavEntryKey,
        payload: Any,
        resultType: KClass<*>,
        onCancel: () -> Unit,
        onResult: (Any) -> Unit,
    ): LaydrNavEntryKey {
        val entryToken = nextEntryToken()
        payloadsByEntryToken[entryToken] = payload
        resultsByEntryToken[entryToken] = LaydrNavResultRegistration(
            resultType = resultType,
            onCancel = onCancel,
            onResult = onResult,
        )
        return key.withEntryToken(entryToken)
    }

    /**
     * Looks up the payload for [key].
     */
    public fun lookupPayload(key: LaydrNavEntryKey): LaydrNavPayloadLookup {
        prune()
        val entryToken = key.entryToken ?: return LaydrNavPayloadLookup.Missing
        val payload = payloadsByEntryToken[entryToken] ?: return LaydrNavPayloadLookup.Missing
        return LaydrNavPayloadLookup.Present(payload)
    }

    /**
     * Looks up the result sink for [key].
     */
    public fun lookupResult(key: LaydrNavEntryKey): LaydrNavResultLookup {
        prune()
        val entryToken = key.entryToken ?: return LaydrNavResultLookup.Missing
        val registration = resultsByEntryToken[entryToken] ?: return LaydrNavResultLookup.Missing
        return LaydrNavResultLookup.Present(
            resultType = registration.resultType,
            sink = LaydrNavResultSink<Any>(
                resultType = registration.resultType,
                entryToken = entryToken,
                entryStore = this,
            ),
        )
    }

    /**
     * Completes a registered result.
     */
    public fun completeResult(
        entryToken: String,
        resultType: KClass<*>,
        result: Any,
    ) {
        val registration = resultsByEntryToken[entryToken] ?: return
        check(registration.resultType == resultType) {
            "Laydr Nav result sink type mismatch. Expected $resultType, found ${registration.resultType}."
        }
        resultsByEntryToken.remove(entryToken)
        registration.onResult(result)
    }

    /**
     * Cancels a registered result.
     */
    public fun cancelResult(
        entryToken: String,
        resultType: KClass<*>,
    ) {
        val registration = resultsByEntryToken[entryToken] ?: return
        check(registration.resultType == resultType) {
            "Laydr Nav result sink type mismatch. Expected $resultType, found ${registration.resultType}."
        }
        resultsByEntryToken.remove(entryToken)
        registration.onCancel()
    }

    /**
     * Removes payload/result state whose entry tokens are no longer active.
     */
    public fun prune() {
        val activeTokens = currentEntries()
            .mapNotNull { key -> key.entryToken }
            .toSet()
        val removedResults = resultsByEntryToken
            .filterKeys { entryToken -> entryToken !in activeTokens }
            .values
            .toList()
        payloadsByEntryToken.keys.retainAll(activeTokens)
        resultsByEntryToken.keys.retainAll(activeTokens)
        removedResults.forEach { registration ->
            registration.onCancel()
        }
    }

    /**
     * Number of currently stored payloads.
     */
    public val payloadCount: Int
        get() = payloadsByEntryToken.size

    /**
     * Number of currently registered results.
     */
    public val resultCount: Int
        get() = resultsByEntryToken.size

    private fun nextEntryToken(): String =
        nextEntryToken++.toString()
}

private class LaydrNavResultRegistration(
    val resultType: KClass<*>,
    val onCancel: () -> Unit,
    val onResult: (Any) -> Unit,
)
