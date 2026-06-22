// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.workflow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Headless test driver for [LaydrWorkflow].
 *
 * The scenario starts a workflow, collects emitted outputs, and exposes
 * focused assertions without rendering UI.
 */
public class LaydrWorkflowScenario<Output : Any> public constructor(
    /**
     * Workflow under test.
     */
    public val workflow: LaydrWorkflow<Output>,
    private val scope: CoroutineScope,
) {
    private val collectedOutputs: ArrayDeque<Output> = ArrayDeque()
    private var outputsJob: Job? = null
    private var started: Boolean = false

    /**
     * Starts output collection and starts [workflow].
     */
    public fun start(): LaydrWorkflowScenario<Output> = apply {
        if (started) {
            return@apply
        }
        started = true
        outputsJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            workflow.outputs.collect { output -> collectedOutputs.addLast(output) }
        }
        workflow.start()
    }

    /**
     * Runs [block] when the top node has type [Node].
     */
    public inline fun <reified Node : LaydrWorkflowNode<*, *, *>> updateTopNode(
        block: Node.() -> Unit,
    ): LaydrWorkflowScenario<Output> = apply {
        requireStarted()
        check(workflow.updateTopNode(block)) {
            "Expected top node ${Node::class.simpleName} but was " +
                "${workflow.currentTopNode()::class.simpleName}"
        }
    }

    /**
     * Asserts whether workflow-local back is available.
     */
    public fun assertCanBack(expected: Boolean): LaydrWorkflowScenario<Output> = apply {
        val actual = workflow.canBack()
        check(actual == expected) {
            "Expected canBack=$expected but was $actual"
        }
    }

    /**
     * Runs workflow-local back and fails when nothing can be removed.
     */
    public fun back(): LaydrWorkflowScenario<Output> = apply {
        check(workflow.back()) {
            "Expected workflow back() to remove a node."
        }
    }

    /**
     * Asserts the current stack size.
     */
    public fun assertStackSize(expected: Int): LaydrWorkflowScenario<Output> = apply {
        val actual = workflow.stackState.value.size
        check(actual == expected) {
            "Expected stack size $expected but was $actual"
        }
    }

    /**
     * Asserts the current top node type.
     */
    public inline fun <reified Node : LaydrWorkflowNode<*, *, *>> assertTopNodeIs():
        LaydrWorkflowScenario<Output> = apply {
        val actual = workflow.stackState.value.top
        check(actual is Node) {
            "Expected top node ${Node::class.simpleName} but was ${actual::class.simpleName}"
        }
    }

    /**
     * Waits until the stack reaches [expected].
     */
    public suspend fun awaitStackSize(
        expected: Int,
        timeoutMillis: Long = 1_000,
    ): LaydrWorkflowScenario<Output> = apply {
        check(started) {
            "Call start() before awaiting workflow stack changes."
        }
        if (workflow.stackState.value.size == expected) {
            return@apply
        }
        withTimeout(timeoutMillis.milliseconds) {
            workflow.stackState.first { state -> state.size == expected }
        }
    }

    /**
     * Waits until the top node has type [Node].
     */
    public suspend inline fun <reified Node : LaydrWorkflowNode<*, *, *>> awaitTopNodeIs(
        timeoutMillis: Long = 1_000,
    ): LaydrWorkflowScenario<Output> = apply {
        requireStarted()
        if (workflow.stackState.value.top is Node) {
            return@apply
        }
        withTimeout(timeoutMillis.milliseconds) {
            workflow.stackState.first { state -> state.top is Node }
        }
    }

    /**
     * Waits for the next emitted output.
     */
    public suspend fun awaitNextOutput(timeoutMillis: Long = 1_000): Output =
        withTimeout(timeoutMillis.milliseconds) {
            while (collectedOutputs.isEmpty()) {
                yield()
            }
            collectedOutputs.removeFirst()
        }

    /**
     * Asserts the next emitted output.
     */
    public suspend fun assertNextOutput(expected: Output): LaydrWorkflowScenario<Output> = apply {
        val actual = awaitNextOutput()
        check(actual == expected) {
            "Expected next output $expected but was $actual"
        }
    }

    /**
     * Asserts that no outputs are currently buffered.
     */
    public fun assertNoMoreOutputs(): LaydrWorkflowScenario<Output> = apply {
        check(collectedOutputs.isEmpty()) {
            "Expected no more outputs but found $collectedOutputs"
        }
    }

    /**
     * Stops output collection and disposes the workflow.
     */
    public suspend fun finish() {
        try {
            outputsJob?.cancelAndJoin()
        } catch (_: CancellationException) {
        }
        workflow.dispose()
        started = false
    }

    @PublishedApi
    internal fun requireStarted() {
        check(started) {
            "Call start() before interacting with a Laydr workflow scenario."
        }
    }
}
