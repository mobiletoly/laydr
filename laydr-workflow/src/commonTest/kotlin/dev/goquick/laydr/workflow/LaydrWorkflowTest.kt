package dev.goquick.laydr.workflow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LaydrWorkflowTest {
    @Test
    fun nodeStateAndOutputsAreAvailableHeadlessly() = runTest {
        val node = CounterNode(this)
        val workflow = LaydrWorkflow(this, node)
        val scenario = LaydrWorkflowScenario(workflow, this).start()

        scenario.updateTopNode<CounterNode> {
            onEvent(CounterEvent.Increment)
            onEvent(CounterEvent.Done)
        }

        assertEquals(CounterState(value = 1), node.state.value)
        scenario.assertNextOutput(TestOutput.Done)
        scenario.finish()
    }

    @Test
    fun workflowPushReplaceAndBackMutatePrivateStack() = runTest {
        val workflow = TestWorkflow(this)
        val scenario = LaydrWorkflowScenario(workflow, this).start()

        scenario
            .assertStackSize(1)
            .assertCanBack(false)
            .updateTopNode<CounterNode> {
                onEvent(CounterEvent.OpenDetail)
            }
            .awaitTopNodeIs<DetailNode>()
            .assertStackSize(2)
            .assertCanBack(true)

        workflow.replaceTop(ConfirmNode(this))
        scenario.assertTopNodeIs<ConfirmNode>()
        scenario.back()
            .assertTopNodeIs<CounterNode>()
            .assertCanBack(false)

        scenario.finish()
    }

    @Test
    fun detachedNodesStopEmittingOutputs()= runTest {
        val root = CounterNode(this)
        val workflow = LaydrWorkflow(this, root)
        val scenario = LaydrWorkflowScenario(workflow, this).start()
        val detail = DetailNode(this)

        workflow.push(detail)
        workflow.back()
        detail.onEvent(DetailEvent.Done)

        scenario.assertNoMoreOutputs()
        scenario.finish()
    }

    @Test
    fun lifecycleHooksRunForStackMembership() = runTest {
        val root = LifecycleNode(this)
        val workflow = LaydrWorkflow(this, root)

        workflow.start()
        assertTrue(root.attached)

        workflow.dispose()
        assertTrue(root.detached)
    }

    @Test
    fun outputsEmittedDuringAttachAreObserved() = runTest {
        val workflow = AttachOutputWorkflow(this)
        val scenario = LaydrWorkflowScenario(workflow, this).start()

        scenario.assertNextOutput(TestOutput.Done)
        assertEquals(TestOutput.Done, workflow.lastObservedOutput)
        scenario.finish()
    }
}

private data class CounterState(val value: Int = 0)

private sealed interface CounterEvent {
    data object Increment : CounterEvent
    data object OpenDetail : CounterEvent
    data object Done : CounterEvent
}

private sealed interface DetailEvent {
    data object Done : DetailEvent
}

private sealed interface TestOutput {
    data object OpenDetail : TestOutput
    data object Done : TestOutput
}

private class CounterNode(scope: CoroutineScope) :
    LaydrStatefulWorkflowNode<CounterState, CounterEvent, TestOutput>(
        parentScope = scope,
        initialState = CounterState(),
    ) {
    override fun onEvent(event: CounterEvent) {
        when (event) {
            CounterEvent.Increment -> updateState { state -> state.copy(value = state.value + 1) }
            CounterEvent.OpenDetail -> tryEmitOutput(TestOutput.OpenDetail)
            CounterEvent.Done -> tryEmitOutput(TestOutput.Done)
        }
    }
}

private class DetailNode(scope: CoroutineScope) :
    LaydrStatefulWorkflowNode<Unit, DetailEvent, TestOutput>(
        parentScope = scope,
        initialState = Unit,
    ) {
    override fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.Done -> tryEmitOutput(TestOutput.Done)
        }
    }
}

private class ConfirmNode(scope: CoroutineScope) :
    LaydrStatefulWorkflowNode<Unit, DetailEvent, TestOutput>(
        parentScope = scope,
        initialState = Unit,
    ) {
    override fun onEvent(event: DetailEvent): Unit = Unit
}

private class LifecycleNode(scope: CoroutineScope) :
    LaydrStatefulWorkflowNode<Unit, DetailEvent, TestOutput>(
        parentScope = scope,
        initialState = Unit,
    ) {
    var attached: Boolean = false
        private set
    var detached: Boolean = false
        private set

    override fun onAttach() {
        attached = true
    }

    override fun onCleared() {
        detached = true
    }

    override fun onEvent(event: DetailEvent): Unit = Unit
}

private class AttachOutputNode(scope: CoroutineScope) :
    LaydrStatefulWorkflowNode<Unit, DetailEvent, TestOutput>(
        parentScope = scope,
        initialState = Unit,
    ) {
    override fun onAttach() {
        tryEmitOutput(TestOutput.Done)
    }

    override fun onEvent(event: DetailEvent): Unit = Unit
}

private class AttachOutputWorkflow(
    workflowScope: CoroutineScope,
) : LaydrWorkflow<TestOutput>(
    scope = workflowScope,
    rootNode = AttachOutputNode(workflowScope),
) {
    var lastObservedOutput: TestOutput? = null
        private set

    override fun onNodeOutput(
        node: LaydrWorkflowNode<*, *, TestOutput>,
        output: TestOutput,
    ) {
        lastObservedOutput = output
    }
}

private class TestWorkflow(
    private val workflowScope: CoroutineScope,
) : LaydrWorkflow<TestOutput>(
    scope = workflowScope,
    rootNode = CounterNode(workflowScope),
) {
    override fun onNodeOutput(
        node: LaydrWorkflowNode<*, *, TestOutput>,
        output: TestOutput,
    ) {
        when (output) {
            TestOutput.OpenDetail -> push(DetailNode(workflowScope))
            TestOutput.Done -> Unit
        }
    }
}
