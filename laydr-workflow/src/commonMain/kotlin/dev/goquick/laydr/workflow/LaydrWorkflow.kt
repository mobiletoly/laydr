// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.workflow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One entry in a [LaydrWorkflow] stack.
 *
 * @param Output workflow output type.
 * @param node node represented by this stack entry.
 * @param tag optional app-owned debug tag for tests and diagnostics.
 */
public class LaydrWorkflowStackEntry<Output : Any> public constructor(
    public val node: LaydrWorkflowNode<*, *, Output>,
    public val tag: String? = null,
)

/**
 * Immutable snapshot of a non-empty workflow stack.
 *
 * @param Output workflow output type.
 * @param stack entries from root to top.
 */
public class LaydrWorkflowStackState<Output : Any> public constructor(
    stack: List<LaydrWorkflowStackEntry<Output>>,
) {
    /**
     * Entries from root to top.
     */
    public val stack: List<LaydrWorkflowStackEntry<Output>> = stack.toList()

    /**
     * Root entry.
     */
    public val rootEntry: LaydrWorkflowStackEntry<Output> = this.stack.first()

    /**
     * Top entry.
     */
    public val topEntry: LaydrWorkflowStackEntry<Output> = this.stack.last()

    /**
     * Root node.
     */
    public val root: LaydrWorkflowNode<*, *, Output> = rootEntry.node

    /**
     * Top node.
     */
    public val top: LaydrWorkflowNode<*, *, Output> = topEntry.node

    /**
     * Stack size.
     */
    public val size: Int = this.stack.size

    init {
        require(this.stack.isNotEmpty()) {
            "Laydr workflow stack must not be empty"
        }
    }
}

/**
 * Headless route-local feature workflow.
 *
 * A workflow owns a private stack of [LaydrWorkflowNode] instances. It is not
 * an app router: use app-owned Laydr destinations for app navigation and emit
 * workflow outputs when a feature needs to communicate with its route shell.
 *
 * @param Output app-facing output type emitted by this workflow.
 * @param scope coroutine scope used to observe node outputs.
 * @param rootNode root node for this workflow.
 */
public open class LaydrWorkflow<Output : Any> public constructor(
    private val scope: CoroutineScope,
    rootNode: LaydrWorkflowNode<*, *, Output>,
) {
    private val mutableStackState: MutableStateFlow<LaydrWorkflowStackState<Output>> =
        MutableStateFlow(LaydrWorkflowStackState(listOf(createEntry(rootNode))))
    private val mutableOutputs: MutableSharedFlow<Output> =
        MutableSharedFlow(extraBufferCapacity = outputBufferSize)
    private val outputCollectors:
        MutableList<Pair<LaydrWorkflowNode<*, *, Output>, Job>> = mutableListOf()

    /**
     * Current stack snapshot.
     */
    public val stackState: StateFlow<LaydrWorkflowStackState<Output>> =
        mutableStackState.asStateFlow()

    /**
     * Hot stream of app-facing workflow outputs.
     */
    public val outputs: Flow<Output> = mutableOutputs.asSharedFlow()

    private var started: Boolean = false

    /**
     * Starts output collection and lifecycle attachment. Safe to call multiple
     * times.
     */
    public fun start() {
        if (started) {
            return
        }
        started = true
        stackState.value.stack.forEach { entry -> attachNode(entry.node) }
    }

    /**
     * True when this workflow has been started.
     */
    public fun isStarted(): Boolean = started

    /**
     * Current top node.
     */
    public fun currentTopNode(): LaydrWorkflowNode<*, *, Output> =
        stackState.value.top

    /**
     * True when [back] can remove a private workflow stack entry.
     */
    public fun canBack(): Boolean =
        stackState.value.size > 1

    /**
     * Pushes [node] onto the private workflow stack.
     */
    public fun push(node: LaydrWorkflowNode<*, *, Output>) {
        ensureStarted()
        val entry = createEntry(node)
        mutableStackState.value = LaydrWorkflowStackState(stackState.value.stack + entry)
        attachNode(node)
    }

    /**
     * Replaces the top stack entry with [node].
     */
    public fun replaceTop(node: LaydrWorkflowNode<*, *, Output>) {
        ensureStarted()
        val previous = stackState.value.top
        val nextStack = stackState.value.stack.dropLast(1) + createEntry(node)
        mutableStackState.value = LaydrWorkflowStackState(nextStack)
        detachNode(previous)
        attachNode(node)
    }

    /**
     * Replaces the entire private workflow stack with [node].
     */
    public fun replaceAll(node: LaydrWorkflowNode<*, *, Output>) {
        ensureStarted()
        val previous = stackState.value.stack.map { entry -> entry.node }
        mutableStackState.value = LaydrWorkflowStackState(listOf(createEntry(node)))
        previous.forEach(::detachNode)
        attachNode(node)
    }

    /**
     * Removes the top workflow node when possible.
     *
     * @return true when a node was removed.
     */
    public fun back(): Boolean {
        ensureStarted()
        if (!canBack()) {
            return false
        }
        val previous = stackState.value.top
        mutableStackState.value = LaydrWorkflowStackState(stackState.value.stack.dropLast(1))
        detachNode(previous)
        return true
    }

    /**
     * Disposes all active nodes and stops output collection. Safe to call
     * multiple times.
     */
    public fun dispose() {
        if (!started) {
            return
        }
        stackState.value.stack.forEach { entry -> detachNode(entry.node) }
        outputCollectors.toList().forEach { (_, job) -> job.cancel() }
        outputCollectors.clear()
        started = false
    }

    /**
     * Handles a node output. Subclasses can override this to perform private
     * workflow stack transitions before the same output is emitted from
     * [outputs].
     */
    protected open fun onNodeOutput(
        node: LaydrWorkflowNode<*, *, Output>,
        output: Output,
    ): Unit = Unit

    /**
     * Creates a stack entry for [node].
     */
    protected open fun createEntry(
        node: LaydrWorkflowNode<*, *, Output>,
    ): LaydrWorkflowStackEntry<Output> =
        LaydrWorkflowStackEntry(node = node)

    private fun attachNode(node: LaydrWorkflowNode<*, *, Output>) {
        observeNodeOutputs(node)
        (node as? LaydrWorkflowLifecycle)?.onAttach()
    }

    private fun detachNode(node: LaydrWorkflowNode<*, *, Output>) {
        stopObservingNode(node)
        (node as? LaydrWorkflowLifecycle)?.onDetach()
    }

    private fun observeNodeOutputs(node: LaydrWorkflowNode<*, *, Output>) {
        if (outputCollectors.any { (candidate, _) -> candidate === node }) {
            return
        }
        val job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            node.outputs.collect { output ->
                onNodeOutput(node = node, output = output)
                emitWorkflowOutput(output)
            }
        }
        outputCollectors += node to job
    }

    private fun emitWorkflowOutput(output: Output) {
        if (!mutableOutputs.tryEmit(output)) {
            scope.launch {
                mutableOutputs.emit(output)
            }
        }
    }

    private fun stopObservingNode(node: LaydrWorkflowNode<*, *, Output>) {
        val index = outputCollectors.indexOfFirst { (candidate, _) -> candidate === node }
        if (index >= 0) {
            outputCollectors.removeAt(index).second.cancel()
        }
    }

    private fun ensureStarted() {
        check(started) {
            "LaydrWorkflow must be started before use."
        }
    }

    private companion object {
        private const val outputBufferSize: Int = 16
    }
}

/**
 * Runs [block] when the current top node is [Node].
 *
 * @return true when the current top node matched.
 */
public inline fun <reified Node : LaydrWorkflowNode<*, *, *>> LaydrWorkflow<*>.updateTopNode(
    block: Node.() -> Unit,
): Boolean {
    val node = currentTopNode()
    if (node !is Node) {
        return false
    }
    node.block()
    return true
}
