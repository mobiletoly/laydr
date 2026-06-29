// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Remembers a [LaydrWorkflow].
 *
 * @param key value that recreates the workflow when it changes.
 * @param factory creates the workflow with the composition coroutine scope.
 */
@Composable
public fun <Output : Any> rememberLaydrWorkflow(
    key: Any? = Unit,
    factory: (CoroutineScope) -> LaydrWorkflow<Output>,
): LaydrWorkflow<Output> {
    val scope = rememberCoroutineScope()
    return remember(key) {
        factory(scope)
    }
}

/**
 * Renders workflow nodes by runtime node type.
 */
public class LaydrWorkflowRenderer<Output : Any> internal constructor(
    private val delegates: List<LaydrWorkflowRenderDelegate<Output>>,
) {
    /**
     * Renders [node], or fails when no delegate was registered for its type.
     */
    @Composable
    public fun Render(node: LaydrWorkflowNode<*, *, Output>) {
        for (delegate in delegates) {
            if (delegate.render(node)) {
                return
            }
        }
        error("Missing Laydr workflow renderer for node ${node::class.simpleName}")
    }
}

/**
 * Builder for [LaydrWorkflowRenderer].
 */
public class LaydrWorkflowRendererBuilder<Output : Any> public constructor() {
    @PublishedApi
    internal val delegates: MutableList<LaydrWorkflowRenderDelegate<Output>> = mutableListOf()

    /**
     * Registers [content] for workflow nodes of type [Node].
     */
    public inline fun <reified Node : LaydrWorkflowNode<*, *, Output>> register(
        noinline content: @Composable (Node) -> Unit,
    ) {
        delegates += LaydrWorkflowRenderDelegate { node ->
            if (node is Node) {
                content(node)
                true
            } else {
                false
            }
        }
    }

    /**
     * Builds the renderer.
     */
    public fun build(): LaydrWorkflowRenderer<Output> =
        LaydrWorkflowRenderer(delegates.toList())
}

@PublishedApi
internal class LaydrWorkflowRenderDelegate<Output : Any> @PublishedApi internal constructor(
    val render: @Composable (LaydrWorkflowNode<*, *, Output>) -> Boolean,
)

/**
 * Creates a [LaydrWorkflowRenderer].
 */
public fun <Output : Any> laydrWorkflowRenderer(
    builder: LaydrWorkflowRendererBuilder<Output>.() -> Unit,
): LaydrWorkflowRenderer<Output> =
    LaydrWorkflowRendererBuilder<Output>().apply(builder).build()

/**
 * Renders the current top workflow node with [renderer].
 *
 * This overload starts [workflow] when it enters composition and disposes it
 * when it leaves composition.
 *
 * This host does not own app Back or app navigation. App or route-local UI can
 * call [LaydrWorkflow.back] explicitly when it wants workflow-local Back.
 */
@Composable
public fun <Output : Any> LaydrWorkflowHost(
    workflow: LaydrWorkflow<Output>,
    renderer: LaydrWorkflowRenderer<Output>,
) {
    LaunchedEffect(workflow) {
        try {
            workflow.start()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                workflow.dispose()
            }
        }
    }
    RenderLaydrWorkflowTop(workflow = workflow, renderer = renderer)
}

/**
 * Starts [workflow], collects app-facing outputs, and renders its current top
 * node with [renderer].
 *
 * Output collection is installed before [LaydrWorkflow.start], so outputs
 * emitted from node attach hooks are delivered to [onOutput].
 */
@Composable
public fun <Output : Any> LaydrWorkflowHost(
    workflow: LaydrWorkflow<Output>,
    renderer: LaydrWorkflowRenderer<Output>,
    onOutput: suspend (Output) -> Unit,
) {
    val currentOnOutput by rememberUpdatedState(onOutput)

    LaunchedEffect(workflow) {
        val outputsJob = launch(start = CoroutineStart.UNDISPATCHED) {
            workflow.outputs.collect { output ->
                currentOnOutput(output)
            }
        }
        try {
            workflow.start()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                outputsJob.cancelAndJoin()
                workflow.dispose()
            }
        }
    }
    RenderLaydrWorkflowTop(workflow = workflow, renderer = renderer)
}

@Composable
private fun <Output : Any> RenderLaydrWorkflowTop(
    workflow: LaydrWorkflow<Output>,
    renderer: LaydrWorkflowRenderer<Output>,
) {
    val stackState by workflow.stackState.collectAsState()
    renderer.Render(stackState.top)
}

/**
 * Collects app-facing workflow outputs while [workflow] is in composition.
 */
@Deprecated(
    message = "Use LaydrWorkflowHost(workflow, renderer, onOutput) so output collection starts before workflow.start().",
)
@Composable
public fun <Output : Any> CollectLaydrWorkflowOutputs(
    workflow: LaydrWorkflow<Output>,
    onOutput: suspend (Output) -> Unit,
) {
    val currentOnOutput by rememberUpdatedState(onOutput)

    LaunchedEffect(workflow) {
        workflow.outputs.collect { output ->
            currentOnOutput(output)
        }
    }
}
