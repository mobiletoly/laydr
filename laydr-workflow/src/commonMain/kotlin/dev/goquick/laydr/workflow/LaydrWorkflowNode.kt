// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.workflow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Headless state unit owned by a route-local [LaydrWorkflow].
 *
 * A node exposes immutable [state], accepts typed UI or workflow [onEvent]
 * calls, and emits [outputs] for the workflow to react to. Nodes are private
 * feature runtime objects; they are not app destinations or route keys.
 *
 * @param State immutable state observed by renderers and tests.
 * @param Event input event type accepted by [onEvent].
 * @param Output output type emitted to the owning workflow.
 */
public interface LaydrWorkflowNode<State : Any, Event : Any, Output : Any> {
    /**
     * Current node state.
     */
    public val state: StateFlow<State>

    /**
     * Handles a typed event for this node.
     */
    public fun onEvent(event: Event)

    /**
     * Outputs emitted by this node.
     */
    public val outputs: Flow<Output>
}

/**
 * Optional lifecycle hooks invoked as nodes enter or leave a workflow stack.
 */
public interface LaydrWorkflowLifecycle {
    /**
     * Called when the node is attached to an active workflow stack.
     */
    public fun onAttach(): Unit = Unit

    /**
     * Called before the node leaves an active workflow stack.
     */
    public fun onDetach(): Unit = Unit
}

/**
 * Convenience base class for workflow nodes with mutable state and outputs.
 *
 * The node owns a child coroutine [scope] that is cancelled when the node is
 * detached from a workflow stack. Subclasses should keep business behavior in
 * [onEvent] and emit outputs for workflow-level transitions.
 *
 * @param parentScope workflow or app scope used as the parent for node work.
 * @param initialState initial state exposed through [state].
 * @param outputBufferSize extra buffer capacity for [outputs].
 */
public abstract class LaydrStatefulWorkflowNode<State : Any, Event : Any, Output : Any> public constructor(
    parentScope: CoroutineScope,
    initialState: State,
    outputBufferSize: Int = 1,
) : LaydrWorkflowNode<State, Event, Output>, LaydrWorkflowLifecycle {
    private val nodeJob: Job = SupervisorJob(parentScope.coroutineContext[Job])

    /**
     * Child scope tied to the node lifecycle.
     */
    protected val scope: CoroutineScope = CoroutineScope(parentScope.coroutineContext + nodeJob)

    private val mutableState: MutableStateFlow<State> = MutableStateFlow(initialState)

    final override val state: StateFlow<State> = mutableState.asStateFlow()

    private val mutableOutputs: MutableSharedFlow<Output> =
        MutableSharedFlow(extraBufferCapacity = outputBufferSize)

    final override val outputs: Flow<Output> = mutableOutputs.asSharedFlow()

    /**
     * Replaces the current state.
     */
    protected fun setState(value: State) {
        mutableState.value = value
    }

    /**
     * Updates the current state with [reducer].
     */
    protected fun updateState(reducer: (State) -> State) {
        mutableState.update(reducer)
    }

    /**
     * Emits an output, suspending until the output is accepted.
     */
    protected suspend fun emitOutput(output: Output) {
        mutableOutputs.emit(output)
    }

    /**
     * Emits an output without suspending when buffer capacity is available.
     */
    protected fun tryEmitOutput(output: Output): Boolean =
        mutableOutputs.tryEmit(output)

    final override fun onDetach() {
        onCleared()
        nodeJob.cancel()
    }

    /**
     * Cleanup hook called immediately before the node scope is cancelled.
     */
    protected open fun onCleared(): Unit = Unit
}
