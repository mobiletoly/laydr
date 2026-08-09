package dev.goquick.laydr.nav.runtime

import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrScreenDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LaydrNavEntryStateOwnerTest {
    @Test
    fun rebindingRetainsPayloadAndCompletesOriginalResultExactlyOnce() {
        val owner = LaydrNavEntryStateOwner()
        val firstBinding = owner.bind()
        var completed: String? = null
        var completionCount = 0
        var cancellationCount = 0
        val key = firstBinding.keyForLaunchAndResult(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = "challenge"),
            resultType = String::class,
            onCancel = { cancellationCount++ },
            onResult = { result ->
                completionCount++
                completed = result as String
            },
        )

        val rebound = owner.bind()

        assertFailsWith<IllegalStateException> { firstBinding.lookupPayload(key) }
        assertEquals(
            "challenge",
            assertIs<LaydrNavPayloadLookup.Present>(rebound.lookupPayload(key)).payload,
        )
        val sink = assertIs<LaydrNavResultLookup.Present>(rebound.lookupResult(key)).stringSink()
        sink.complete("accepted")
        sink.complete("ignored")

        assertEquals("accepted", completed)
        assertEquals(1, completionCount)
        assertEquals(0, cancellationCount)
        assertIs<LaydrNavResultLookup.Missing>(rebound.lookupResult(key))
    }

    @Test
    fun lateCloseFromOldBindingDoesNotInvalidateNewBinding() {
        val owner = LaydrNavEntryStateOwner()
        val oldBinding = owner.bind()
        val currentBinding = owner.bind()

        assertEquals(false, oldBinding.close())
        currentBinding.requireActive()

        val key = currentBinding.keyForLaunch(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = "challenge"),
        )
        assertEquals(
            "challenge",
            assertIs<LaydrNavPayloadLookup.Present>(currentBinding.lookupPayload(key)).payload,
        )
    }

    @Test
    fun wrongRuntimeResultValueDoesNotConsumeRegistration() {
        val binding = LaydrNavEntryStateOwner().bind()
        var completed: String? = null
        val key = binding.keyForLaunchAndResult(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = null),
            resultType = String::class,
            onCancel = {},
            onResult = { completed = it as String },
        )
        val lookup = assertIs<LaydrNavResultLookup.Present>(binding.lookupResult(key))
        @Suppress("UNCHECKED_CAST")
        val erasedSink = lookup.sink as LaydrNavResultSink<Any>

        assertFailsWith<IllegalStateException> { erasedSink.complete(42) }
        assertEquals(1, binding.resultCount)
        assertNull(completed)

        lookup.stringSink().complete("accepted")
        assertEquals("accepted", completed)
        assertEquals(0, binding.resultCount)
    }

    @Test
    fun rebindingRetainsCancellationAndPruningCancelsOnlyOnce() {
        val owner = LaydrNavEntryStateOwner()
        val firstBinding = owner.bind()
        var cancellationCount = 0
        val key = firstBinding.keyForLaunchAndResult(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = "challenge"),
            resultType = String::class,
            onCancel = { cancellationCount++ },
            onResult = {},
        )
        val rebound = owner.bind()
        val sink = assertIs<LaydrNavResultLookup.Present>(rebound.lookupResult(key)).stringSink()

        sink.cancel()
        sink.cancel()
        rebound.prune(emptyList())

        assertEquals(1, cancellationCount)
        assertEquals(0, rebound.payloadCount)
        assertEquals(0, rebound.resultCount)
    }

    @Test
    fun pruningPoppedEntryClearsPayloadAndCancelsResultExactlyOnce() {
        val owner = LaydrNavEntryStateOwner()
        val binding = owner.bind()
        var cancellationCount = 0
        var completionCount = 0
        val key = binding.keyForLaunchAndResult(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = "challenge"),
            resultType = String::class,
            onCancel = { cancellationCount++ },
            onResult = { completionCount++ },
        )
        val sink = assertIs<LaydrNavResultLookup.Present>(binding.lookupResult(key)).stringSink()

        binding.prune(emptyList())
        binding.prune(emptyList())
        sink.complete("late")

        assertEquals(1, cancellationCount)
        assertEquals(0, completionCount)
        assertIs<LaydrNavPayloadLookup.Missing>(binding.lookupPayload(key))
        assertIs<LaydrNavResultLookup.Missing>(binding.lookupResult(key))
    }

    @Test
    fun newOwnerCannotResolveTransientStateForRestoredEntryId() {
        val retainedOwner = LaydrNavEntryStateOwner()
        val retainedBinding = retainedOwner.bind()
        val key = retainedBinding.keyForLaunchAndResult(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = "challenge"),
            resultType = String::class,
            onCancel = {},
            onResult = {},
        )

        val freshBinding = LaydrNavEntryStateOwner().bind()

        assertIs<LaydrNavPayloadLookup.Missing>(freshBinding.lookupPayload(key))
        assertIs<LaydrNavResultLookup.Missing>(freshBinding.lookupResult(key))
    }

    @Test
    fun repeatedLaunchesOfSameDestinationRemainIndependent() {
        val binding = LaydrNavEntryStateOwner().bind()
        var firstResult: String? = null
        var secondResult: String? = null
        val first = binding.keyForLaunchAndResult(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = "first"),
            resultType = String::class,
            onCancel = {},
            onResult = { firstResult = it as String },
        )
        val second = binding.keyForLaunchAndResult(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = "second"),
            resultType = String::class,
            onCancel = {},
            onResult = { secondResult = it as String },
        )

        assertNotEquals(first.entryInstanceId, second.entryInstanceId)
        assertEquals("first", assertIs<LaydrNavPayloadLookup.Present>(binding.lookupPayload(first)).payload)
        assertEquals("second", assertIs<LaydrNavPayloadLookup.Present>(binding.lookupPayload(second)).payload)
        assertIs<LaydrNavResultLookup.Present>(binding.lookupResult(second)).stringSink().complete("two")

        assertNull(firstResult)
        assertEquals("two", secondResult)
    }

    @Test
    fun payloadFreeLaunchesOfSameDestinationRemainIndependent() {
        val binding = LaydrNavEntryStateOwner().bind()

        val first = binding.keyForLaunch(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = null),
        )
        val second = binding.keyForLaunch(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = null),
        )

        assertNotNull(first.entryInstanceId)
        assertNotNull(second.entryInstanceId)
        assertNotEquals(first.entryInstanceId, second.entryInstanceId)
        assertEquals(0, binding.payloadCount)
        assertEquals(0, binding.resultCount)
    }

    @Test
    fun clearingOwnerCancelsPendingResultsAndPreventsRebinding() {
        val owner = LaydrNavEntryStateOwner()
        val binding = owner.bind()
        var cancellationCount = 0
        binding.keyForLaunchAndResult(
            key = LaydrNavEntryKey("Authentication"),
            launch = launch(payload = "challenge"),
            resultType = String::class,
            onCancel = { cancellationCount++ },
            onResult = {},
        )

        owner.clear()
        owner.clear()

        assertEquals(1, cancellationCount)
        assertEquals(0, owner.payloadCount)
        assertEquals(0, owner.resultCount)
        assertFailsWith<IllegalStateException> { owner.bind() }
        assertFailsWith<IllegalStateException> { binding.requireActive() }
    }

    @Test
    fun clearingOwnerAttemptsEveryCancellationWhenCallbacksThrow() {
        val owner = LaydrNavEntryStateOwner()
        val binding = owner.bind()
        val cancellations = mutableListOf<String>()
        listOf("first", "second").forEach { name ->
            binding.keyForLaunchAndResult(
                key = LaydrNavEntryKey("Authentication"),
                launch = launch(payload = name),
                resultType = String::class,
                onCancel = {
                    cancellations += name
                    error(name)
                },
                onResult = {},
            )
        }

        val failure = assertFailsWith<IllegalStateException> { owner.clear() }

        assertEquals(listOf("first", "second"), cancellations)
        assertEquals(1, failure.suppressedExceptions.size)
        assertEquals(0, owner.payloadCount)
        assertEquals(0, owner.resultCount)
    }

    @Test
    fun returnHistoryRoundTripPreservesEntryInstanceIds() {
        val source = LaydrNavEntryKey("Profile")
        val target = LaydrNavEntryKey(
            routeId = "Authentication",
            entryInstanceId = "instance-id",
        )
        val history = listOf(
            LaydrNavReturnEntry(
                sourceSectionStateId = "profile",
                sourceBackStack = listOf(source),
                targetSectionStateId = "auth",
                targetBackStackBeforeNavigation = emptyList(),
                targetKey = target,
            ),
        )

        assertEquals(history, restoreLaydrNavReturnHistory(saveLaydrNavReturnHistory(history)))
    }

    @Test
    fun returnHistoryStillReadsLegacyDataWithoutEntryInstanceIds() {
        val legacy = listOf(
            "1",
            "profile",
            "1",
            "Profile",
            "0",
            "auth",
            "0",
            "Authentication",
            "0",
        )

        val restored = restoreLaydrNavReturnHistory(legacy)

        assertEquals(1, restored?.size)
        assertNull(restored?.single()?.targetKey?.entryInstanceId)
    }

    private fun launch(payload: Any?): LaydrNavLaunch =
        LaydrNavLaunch(
            destination = TestDestination(LaydrRouteKey("Authentication")),
            payload = payload,
        )

    @Suppress("UNCHECKED_CAST")
    private fun LaydrNavResultLookup.Present.stringSink(): LaydrNavResultSink<String> =
        sink as LaydrNavResultSink<String>

    private data class TestDestination(
        override val routeKey: LaydrRouteKey,
    ) : LaydrScreenDestination
}
