// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

import kotlin.reflect.KClass
import kotlin.uuid.Uuid

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
    private val entryInstanceId: String,
    private val owner: LaydrNavEntryStateOwner,
) {
    /**
     * Completes the pending route result with [result].
     */
    public fun complete(result: Result) {
        owner.completeResult(
            entryInstanceId = entryInstanceId,
            resultType = resultType,
            result = result,
        )
    }

    /**
     * Cancels the pending route result.
     */
    public fun cancel() {
        owner.cancelResult(
            entryInstanceId = entryInstanceId,
            resultType = resultType,
        )
    }
}

/**
 * App-retainable owner of transient Laydr navigation entry state.
 *
 * The owner contains payload objects and one-shot result registrations only in
 * memory. Applications may retain it through a ViewModel or another
 * configuration-retained boundary, then pass it back to a recreated adapter.
 * Calls must stay on one application navigation thread. Call [clear] when the
 * application-owned lifetime ends.
 */
public class LaydrNavEntryStateOwner public constructor() {
    private var nextBindingGeneration: Long = 0
    private var activeBindingGeneration: Long? = null
    private var cleared: Boolean = false
    private val payloadsByEntryInstanceId = mutableMapOf<String, Any>()
    private val resultsByEntryInstanceId = mutableMapOf<String, LaydrNavResultRegistration>()

    /**
     * Opens the current adapter binding and invalidates any older binding.
     */
    public fun bind(): LaydrNavEntryStateBinding {
        check(!cleared) {
            "Cannot bind a cleared Laydr Nav entry state owner. Create a new owner for the new lifetime."
        }
        val generation = nextBindingGeneration++
        activeBindingGeneration = generation
        return LaydrNavEntryStateBinding(owner = this, generation = generation)
    }

    /**
     * Terminates this owner, clears payloads, and cancels pending results once.
     * Repeated calls do nothing.
     */
    public fun clear() {
        if (cleared) {
            return
        }
        cleared = true
        activeBindingGeneration = null
        payloadsByEntryInstanceId.clear()
        val registrations = resultsByEntryInstanceId.values.toList()
        resultsByEntryInstanceId.clear()
        cancelRegistrations(registrations)
    }

    /**
     * Number of payloads retained by this owner.
     */
    public val payloadCount: Int
        get() = payloadsByEntryInstanceId.size

    /**
     * Number of pending result registrations retained by this owner.
     */
    public val resultCount: Int
        get() = resultsByEntryInstanceId.size

    internal fun requireActive(generation: Long) {
        check(!cleared && activeBindingGeneration == generation) {
            "This Laydr Nav binding is stale. Reacquire the stack, sections, or navigator from the current composition."
        }
    }

    internal fun isActive(generation: Long): Boolean =
        !cleared && activeBindingGeneration == generation

    internal fun closeBinding(generation: Long): Boolean {
        if (activeBindingGeneration != generation) {
            return false
        }
        activeBindingGeneration = null
        return true
    }

    internal fun keyForLaunch(
        generation: Long,
        key: LaydrNavEntryKey,
        launch: LaydrNavLaunch,
    ): LaydrNavEntryKey {
        requireActive(generation)
        val entryInstanceId = Uuid.random().toString()
        launch.payload?.let { payload ->
            payloadsByEntryInstanceId[entryInstanceId] = payload
        }
        return key
            .withEntryMetadata(launch.entryMetadata)
            .withEntryInstanceId(entryInstanceId)
    }

    internal fun keyForLaunchAndResult(
        generation: Long,
        key: LaydrNavEntryKey,
        launch: LaydrNavLaunch,
        resultType: KClass<*>,
        onCancel: () -> Unit,
        onResult: (Any) -> Unit,
    ): LaydrNavEntryKey {
        requireActive(generation)
        val entryInstanceId = Uuid.random().toString()
        launch.payload?.let { payload ->
            payloadsByEntryInstanceId[entryInstanceId] = payload
        }
        resultsByEntryInstanceId[entryInstanceId] = LaydrNavResultRegistration(
            resultType = resultType,
            onCancel = onCancel,
            onResult = onResult,
        )
        return key
            .withEntryMetadata(launch.entryMetadata)
            .withEntryInstanceId(entryInstanceId)
    }

    internal fun lookupPayload(
        generation: Long,
        key: LaydrNavEntryKey,
    ): LaydrNavPayloadLookup {
        requireActive(generation)
        val entryInstanceId = key.entryInstanceId ?: return LaydrNavPayloadLookup.Missing
        val payload = payloadsByEntryInstanceId[entryInstanceId]
            ?: return LaydrNavPayloadLookup.Missing
        return LaydrNavPayloadLookup.Present(payload)
    }

    internal fun lookupResult(
        generation: Long,
        key: LaydrNavEntryKey,
    ): LaydrNavResultLookup {
        requireActive(generation)
        val entryInstanceId = key.entryInstanceId ?: return LaydrNavResultLookup.Missing
        val registration = resultsByEntryInstanceId[entryInstanceId]
            ?: return LaydrNavResultLookup.Missing
        return LaydrNavResultLookup.Present(
            resultType = registration.resultType,
            sink = LaydrNavResultSink<Any>(
                resultType = registration.resultType,
                entryInstanceId = entryInstanceId,
                owner = this,
            ),
        )
    }

    internal fun prune(
        generation: Long,
        activeEntries: List<LaydrNavEntryKey>,
    ) {
        requireActive(generation)
        val activeIds = activeEntries
            .mapNotNull { key -> key.entryInstanceId }
            .toSet()
        val removedRegistrations = resultsByEntryInstanceId
            .filterKeys { entryInstanceId -> entryInstanceId !in activeIds }
            .values
            .toList()
        payloadsByEntryInstanceId.keys.retainAll(activeIds)
        resultsByEntryInstanceId.keys.retainAll(activeIds)
        cancelRegistrations(removedRegistrations)
    }

    internal fun completeResult(
        entryInstanceId: String,
        resultType: KClass<*>,
        result: Any,
    ) {
        val registration = resultsByEntryInstanceId[entryInstanceId] ?: return
        check(registration.resultType == resultType) {
            "Laydr Nav result sink type mismatch. Expected $resultType, found ${registration.resultType}."
        }
        check(registration.resultType.isInstance(result)) {
            "Laydr Nav result value type mismatch. Expected ${registration.resultType}, found ${result::class}."
        }
        resultsByEntryInstanceId.remove(entryInstanceId)
        registration.onResult(result)
    }

    internal fun cancelResult(
        entryInstanceId: String,
        resultType: KClass<*>,
    ) {
        val registration = resultsByEntryInstanceId[entryInstanceId] ?: return
        check(registration.resultType == resultType) {
            "Laydr Nav result sink type mismatch. Expected $resultType, found ${registration.resultType}."
        }
        resultsByEntryInstanceId.remove(entryInstanceId)
        registration.onCancel()
    }
}

/**
 * Generation-scoped adapter access to one [LaydrNavEntryStateOwner].
 *
 * Adapter integrations retain this binding only with the current navigation
 * stack or section composition. A newer binding makes older bindings stale.
 */
public class LaydrNavEntryStateBinding internal constructor(
    private val owner: LaydrNavEntryStateOwner,
    private val generation: Long,
) {
    /**
     * True while this is the owner's current adapter binding.
     */
    public val isActive: Boolean
        get() = owner.isActive(generation)

    /**
     * Fails when this is no longer the owner's current binding.
     */
    public fun requireActive() {
        owner.requireActive(generation)
    }

    /**
     * Releases this binding without clearing owner-retained entry state.
     * Returns true when this binding was still current.
     */
    public fun close(): Boolean = owner.closeBinding(generation)

    /**
     * Creates a distinct entry key for [launch].
     */
    public fun keyForLaunch(
        key: LaydrNavEntryKey,
        launch: LaydrNavLaunch,
    ): LaydrNavEntryKey =
        owner.keyForLaunch(generation = generation, key = key, launch = launch)

    /**
     * Creates a distinct entry key and registers a one-shot result callback.
     */
    public fun keyForLaunchAndResult(
        key: LaydrNavEntryKey,
        launch: LaydrNavLaunch,
        resultType: KClass<*>,
        onCancel: () -> Unit,
        onResult: (Any) -> Unit,
    ): LaydrNavEntryKey =
        owner.keyForLaunchAndResult(
            generation = generation,
            key = key,
            launch = launch,
            resultType = resultType,
            onCancel = onCancel,
            onResult = onResult,
        )

    /**
     * Looks up the payload for [key] without pruning owner state.
     */
    public fun lookupPayload(key: LaydrNavEntryKey): LaydrNavPayloadLookup =
        owner.lookupPayload(generation = generation, key = key)

    /**
     * Looks up the result sink for [key] without pruning owner state.
     */
    public fun lookupResult(key: LaydrNavEntryKey): LaydrNavResultLookup =
        owner.lookupResult(generation = generation, key = key)

    /**
     * Removes state not represented by [activeEntries].
     */
    public fun prune(activeEntries: List<LaydrNavEntryKey>) {
        owner.prune(generation = generation, activeEntries = activeEntries)
    }

    /**
     * Number of payloads retained by the bound owner.
     */
    public val payloadCount: Int
        get() = owner.payloadCount

    /**
     * Number of pending results retained by the bound owner.
     */
    public val resultCount: Int
        get() = owner.resultCount
}

private fun cancelRegistrations(registrations: List<LaydrNavResultRegistration>) {
    var failure: Throwable? = null
    registrations.forEach { registration ->
        try {
            registration.onCancel()
        } catch (thrown: Throwable) {
            if (failure == null) {
                failure = thrown
            } else {
                failure.addSuppressed(thrown)
            }
        }
    }
    failure?.let { thrown -> throw thrown }
}

private class LaydrNavResultRegistration(
    val resultType: KClass<*>,
    val onCancel: () -> Unit,
    val onResult: (Any) -> Unit,
)
