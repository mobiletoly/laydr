// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

import dev.goquick.laydr.core.LaydrParameterlessScreenRouteRef
import dev.goquick.laydr.core.LaydrScreenDestination
import kotlin.reflect.KClass

/**
 * Adapter-neutral selected-section state holder.
 */
public interface LaydrNavSelectedSectionState {
    /**
     * Current selected section state id.
     */
    public var value: String
}

/**
 * Adapter-neutral section routing engine.
 */
public class LaydrNavSectionsEngine<Section : Any, Key : Any> public constructor(
    private val sections: List<Section>,
    private val initialSection: Section,
    private val selectedSectionState: LaydrNavSelectedSectionState,
    private val sectionStateId: (Section) -> String,
    private val sectionLabel: (Section) -> String,
    private val sectionForStateId: (String) -> Section?,
    private val sectionForDestination: (LaydrScreenDestination) -> Section?,
    private val sectionForEntryKey: (LaydrNavEntryKey) -> Section?,
    private val controllerFor: (Section) -> LaydrNavStackEngine<Key>,
    private val returnHistory: MutableList<LaydrNavReturnEntry> = mutableListOf(),
    /**
     * Entry store shared by all section stacks.
     */
    public val entryStore: LaydrNavEntryStore,
) {
    init {
        requireKnownSection(initialSection)
        require(sections.isNotEmpty()) {
            "Laydr Nav sections must declare at least one section"
        }
        selectedSectionState.value = sectionStateId(selectedSection)
    }

    /**
     * Currently selected section.
     */
    public val selectedSection: Section
        get() = sectionForStateId(selectedSectionState.value)
            ?: sections.first()

    /**
     * Engine that owns the currently selected stack.
     */
    public val selectedController: LaydrNavStackEngine<Key>
        get() = controllerFor(selectedSection)

    /**
     * Current generated path for the selected stack.
     */
    public val currentPath: String?
        get() = selectedController.currentPath

    /**
     * True when the selected stack can pop a Laydr entry.
     */
    public val canPopSelectedStack: Boolean
        get() = selectedController.canPopLaydrEntry

    /**
     * True when app Back can return or pop.
     */
    public val canGoBack: Boolean
        get() = matchingReturnEntry() != null || canPopSelectedStack

    /**
     * True when app Back can restore return-aware navigation.
     */
    public val canReturn: Boolean
        get() = matchingReturnEntry() != null

    /**
     * Recognized list-detail facts for the selected stack.
     */
    public val listDetailStackState: LaydrNavListDetailStackState?
        get() = selectedController.listDetailStackState

    /**
     * Selects [section] without mutating section stacks.
     */
    public fun select(section: Section) {
        returnHistory.clear()
        selectedSectionState.value = sectionStateId(requireKnownSection(section))
    }

    /**
     * Selects the section that owns [destination].
     */
    public fun select(destination: LaydrScreenDestination) {
        val section = requireOwningSection(destination)
        returnHistory.clear()
        selectedSectionState.value = sectionStateId(section)
    }

    /**
     * Selects the section for a parameterless route.
     */
    public fun select(route: LaydrParameterlessScreenRouteRef) {
        select(route.defaultDestination)
    }

    /**
     * Selects the section that owns [path].
     */
    public fun selectPath(path: String): LaydrNavPathResult =
        when (val target = pathTarget(path)) {
            is AcceptedPathTarget -> {
                returnHistory.clear()
                selectedSectionState.value = sectionStateId(target.section)
                target.result
            }
            is RejectedPathTarget -> target.result
        }

    /**
     * Selects the section that owns URL-like [input].
     */
    public fun selectExternalTarget(input: String): LaydrNavExternalTargetResult =
        when (val target = externalTarget(input)) {
            is AcceptedExternalTarget -> {
                returnHistory.clear()
                selectedSectionState.value = sectionStateId(target.section)
                target.result
            }
            is RejectedExternalTarget -> target.result
        }

    /**
     * Pushes [destination] on the owning section stack using route identity.
     */
    public fun push(destination: LaydrScreenDestination) {
        val section = requireOwningSection(destination)
        val controller = controllerFor(section)
        val key = controller.validatedKey(destination)
        val clearReturnHistory = section != selectedSection
        controller.pushKey(key)
        if (clearReturnHistory) {
            returnHistory.clear()
        }
        selectedSectionState.value = sectionStateId(section)
        entryStore.prune()
    }

    /**
     * Pushes [launch] on the owning section stack.
     */
    public fun push(launch: LaydrNavLaunch) {
        val section = requireOwningSection(launch.destination)
        val controller = controllerFor(section)
        val validatedKey = controller.validatedKey(launch.destination)
        val clearReturnHistory = section != selectedSection
        val key = entryStore.keyForLaunch(
            key = validatedKey,
            launch = launch,
        )
        try {
            controller.pushNewEntryKey(key)
            if (clearReturnHistory) {
                returnHistory.clear()
            }
            selectedSectionState.value = sectionStateId(section)
        } finally {
            entryStore.prune()
        }
    }

    /**
     * Pushes [destination] and records return history.
     */
    public fun pushWithReturn(destination: LaydrScreenDestination) {
        navigateWithReturn(destination) { key -> pushKey(key) }
    }

    /**
     * Pushes [launch] and records return history.
     */
    public fun pushWithReturn(launch: LaydrNavLaunch) {
        navigateWithReturn(launch) { key -> pushNewEntryKey(key) }
    }

    /**
     * Pushes the section-owned route represented by [path].
     */
    public fun pushPath(path: String): LaydrNavPathResult =
        when (val target = pathTarget(path)) {
            is AcceptedPathTarget -> {
                clearReturnHistoryForOrdinaryNavigation(target.section)
                selectedSectionState.value = sectionStateId(target.section)
                controllerFor(target.section).pushKey(target.result.key)
                entryStore.prune()
                target.result
            }
            is RejectedPathTarget -> target.result
        }

    /**
     * Pushes the section-owned route represented by URL-like [input].
     */
    public fun pushExternalTarget(input: String): LaydrNavExternalTargetResult =
        when (val target = externalTarget(input)) {
            is AcceptedExternalTarget -> {
                clearReturnHistoryForOrdinaryNavigation(target.section)
                selectedSectionState.value = sectionStateId(target.section)
                controllerFor(target.section).pushKey(target.result.key)
                entryStore.prune()
                target.result
            }
            is RejectedExternalTarget -> target.result
        }

    /**
     * Replaces the current entry with [launch] on the owning section stack.
     */
    public fun replace(launch: LaydrNavLaunch) {
        val section = requireOwningSection(launch.destination)
        val controller = controllerFor(section)
        val validatedKey = controller.validatedKey(launch.destination)
        controller.requireCanReplaceCurrentEntry()
        val key = entryStore.keyForLaunch(
            key = validatedKey,
            launch = launch,
        )
        try {
            controller.replaceKey(key)
            clearReturnHistoryForOrdinaryNavigation(section)
            selectedSectionState.value = sectionStateId(section)
        } finally {
            entryStore.prune()
        }
    }

    /**
     * Replaces with [launch] and records return history.
     */
    public fun replaceWithReturn(launch: LaydrNavLaunch) {
        navigateWithReturn(
            launch = launch,
            preflight = { requireCanReplaceCurrentEntry() },
        ) { key -> replaceKey(key) }
    }

    /**
     * Replaces the current entry with the route represented by [path].
     */
    public fun replacePath(path: String): LaydrNavPathResult =
        when (val target = pathTarget(path)) {
            is AcceptedPathTarget -> {
                val controller = controllerFor(target.section)
                controller.requireCanReplaceCurrentEntry()
                try {
                    controller.replaceKey(target.result.key)
                    clearReturnHistoryForOrdinaryNavigation(target.section)
                    selectedSectionState.value = sectionStateId(target.section)
                } finally {
                    entryStore.prune()
                }
                target.result
            }
            is RejectedPathTarget -> target.result
        }

    /**
     * Replaces the current entry with the route represented by URL-like
     * [input].
     */
    public fun replaceExternalTarget(input: String): LaydrNavExternalTargetResult =
        when (val target = externalTarget(input)) {
            is AcceptedExternalTarget -> {
                val controller = controllerFor(target.section)
                controller.requireCanReplaceCurrentEntry()
                try {
                    controller.replaceKey(target.result.key)
                    clearReturnHistoryForOrdinaryNavigation(target.section)
                    selectedSectionState.value = sectionStateId(target.section)
                } finally {
                    entryStore.prune()
                }
                target.result
            }
            is RejectedExternalTarget -> target.result
        }

    /**
     * Pushes [launch] and registers a one-shot typed result callback.
     */
    public fun <Result : Any> pushForResult(
        launch: LaydrNavLaunch,
        onCancel: () -> Unit,
        resultType: KClass<Result>,
        onResult: (Result) -> Unit,
    ) {
        val section = requireOwningSection(launch.destination)
        if (section == selectedSection) {
            val controller = controllerFor(section)
            val key = keyForResultLaunch(
                controller = controller,
                launch = launch,
                resultType = resultType,
                onCancel = onCancel,
                onResult = onResult,
            )
            controller.pushNewEntryKey(key)
            entryStore.prune()
            return
        }

        navigateWithReturn(
            launch = launch,
            keyForLaunch = { controller ->
                keyForResultLaunch(
                    controller = controller,
                    launch = launch,
                    resultType = resultType,
                    onCancel = onCancel,
                    onResult = onResult,
                )
            },
        ) { key -> pushNewEntryKey(key) }
    }

    /**
     * Performs app Back.
     */
    public fun back(): Boolean {
        val entry = matchingReturnEntry()
        if (entry != null) {
            returnHistory.removeAt(returnHistory.lastIndex)
            if (restoreReturnEntry(entry)) {
                entryStore.prune()
                return true
            }
        }
        return popSelectedStack().also {
            entryStore.prune()
        }
    }

    /**
     * Pops the selected stack.
     */
    public fun popSelectedStack(): Boolean =
        selectedController.popLaydrEntry().also {
            entryStore.prune()
        }

    private fun clearReturnHistoryForOrdinaryNavigation(section: Section) {
        if (section != selectedSection) {
            returnHistory.clear()
        }
    }

    private fun navigateWithReturn(
        destination: LaydrScreenDestination,
        preflight: LaydrNavStackEngine<Key>.() -> Unit = {},
        operation: LaydrNavStackEngine<Key>.(LaydrNavEntryKey) -> Unit,
    ) {
        navigateWithReturn(
            destination = destination,
            preflight = preflight,
            keyForNavigation = { controller ->
                controller.validatedKey(destination)
            },
            operation = operation,
        )
    }

    private fun navigateWithReturn(
        launch: LaydrNavLaunch,
        preflight: LaydrNavStackEngine<Key>.() -> Unit = {},
        operation: LaydrNavStackEngine<Key>.(LaydrNavEntryKey) -> Unit,
    ) {
        navigateWithReturn(
            destination = launch.destination,
            preflight = preflight,
            keyForNavigation = { controller ->
                entryStore.keyForLaunch(
                    key = controller.validatedKey(launch.destination),
                    launch = launch,
                )
            },
            operation = operation,
        )
    }

    private fun navigateWithReturn(
        launch: LaydrNavLaunch,
        preflight: LaydrNavStackEngine<Key>.() -> Unit = {},
        keyForLaunch: (LaydrNavStackEngine<Key>) -> LaydrNavEntryKey,
        operation: LaydrNavStackEngine<Key>.(LaydrNavEntryKey) -> Unit,
    ) {
        navigateWithReturn(
            destination = launch.destination,
            preflight = preflight,
            keyForNavigation = keyForLaunch,
            operation = operation,
        )
    }

    private fun navigateWithReturn(
        destination: LaydrScreenDestination,
        preflight: LaydrNavStackEngine<Key>.() -> Unit = {},
        keyForNavigation: (LaydrNavStackEngine<Key>) -> LaydrNavEntryKey,
        operation: LaydrNavStackEngine<Key>.(LaydrNavEntryKey) -> Unit,
    ) {
        val targetSection = requireOwningSection(destination)
        val sourceSection = selectedSection
        val sourceController = selectedController
        val targetController = controllerFor(targetSection)
        val sourceStack = sourceController.allEntryKeys()
        val targetStackBefore = targetController.allEntryKeys()
        targetController.preflight()
        val key = keyForNavigation(targetController)

        try {
            targetController.operation(key)
        } finally {
            entryStore.prune()
        }
        selectedSectionState.value = sectionStateId(targetSection)

        val targetStackAfter = targetController.allEntryKeys() ?: return
        val targetKey = targetStackAfter.lastOrNull() ?: return
        if (sourceStack == null || targetStackBefore == null) {
            return
        }
        if (sourceSection == targetSection && sourceStack == targetStackAfter) {
            return
        }
        returnHistory += LaydrNavReturnEntry(
            sourceSectionStateId = sectionStateId(sourceSection),
            sourceBackStack = sourceStack,
            targetSectionStateId = sectionStateId(targetSection),
            targetBackStackBeforeNavigation = targetStackBefore,
            targetKey = targetKey,
        )
    }

    private fun <Result : Any> keyForResultLaunch(
        controller: LaydrNavStackEngine<Key>,
        launch: LaydrNavLaunch,
        resultType: KClass<Result>,
        onCancel: () -> Unit,
        onResult: (Result) -> Unit,
    ): LaydrNavEntryKey =
        entryStore.keyForLaunchAndResult(
            key = controller.validatedKey(launch.destination),
            launch = launch,
            resultType = resultType,
            onCancel = onCancel,
            onResult = { result ->
                @Suppress("UNCHECKED_CAST")
                onResult(result as Result)
            },
        )

    private fun matchingReturnEntry(): LaydrNavReturnEntry? {
        val entry = returnHistory.lastOrNull() ?: return null
        if (entry.targetSectionStateId != sectionStateId(selectedSection)) {
            return null
        }
        if (entry.targetKey != selectedController.currentEntryKey) {
            return null
        }
        if (sectionForStateId(entry.sourceSectionStateId) == null) {
            return null
        }
        return entry
    }

    private fun restoreReturnEntry(entry: LaydrNavReturnEntry): Boolean {
        val sourceSection = sectionForStateId(entry.sourceSectionStateId) ?: return false
        val targetSection = sectionForStateId(entry.targetSectionStateId) ?: return false
        if (sourceSection == targetSection) {
            controllerFor(sourceSection).replaceWithEntryKeys(entry.sourceBackStack)
        } else {
            controllerFor(sourceSection).replaceWithEntryKeys(entry.sourceBackStack)
            controllerFor(targetSection).replaceWithEntryKeys(entry.targetBackStackBeforeNavigation)
        }
        selectedSectionState.value = sectionStateId(sourceSection)
        return true
    }

    private fun requireKnownSection(section: Section): Section {
        require(section in sections) {
            "Laydr Nav section is not part of this section set: ${sectionLabel(section)}"
        }
        return section
    }

    private fun requireOwningSection(destination: LaydrScreenDestination): Section =
        sectionForDestination(destination)
            ?: throw IllegalArgumentException(
                "Destination is not owned by any declared Laydr Nav section: " +
                    "${destination.routeKey}",
            )

    private fun pathTarget(path: String): PathTargetResult<Section> {
        val result = laydrNavPathResult(path = path, routeMap = selectedController.appGraph.routeMap)
        val accepted = result as? LaydrNavPathAccepted
            ?: return RejectedPathTarget(result as LaydrNavPathRejected)
        val section = sectionForEntryKey(accepted.key)
            ?: return RejectedPathTarget(
                LaydrNavPathRejected(
                    path = path,
                    reason = LaydrNavPathRejectionReason.OutsideDeclaredSection,
                ),
            )
        return AcceptedPathTarget(section = section, result = accepted)
    }

    private fun externalTarget(input: String): ExternalTargetResult<Section> {
        val result = laydrNavExternalTargetResult(input = input, routeMap = selectedController.appGraph.routeMap)
        val accepted = result as? LaydrNavExternalTargetAccepted
            ?: return RejectedExternalTarget(result as LaydrNavExternalTargetRejected)
        val section = sectionForEntryKey(accepted.key)
            ?: return RejectedExternalTarget(
                LaydrNavExternalTargetRejected(
                    input = input,
                    path = accepted.path,
                    query = accepted.query,
                    fragment = accepted.fragment,
                    reason = LaydrNavExternalTargetRejectionReason.OutsideDeclaredSection,
                ),
            )
        return AcceptedExternalTarget(section = section, result = accepted)
    }
}

private sealed interface PathTargetResult<Section : Any>

private data class AcceptedPathTarget<Section : Any>(
    val section: Section,
    val result: LaydrNavPathAccepted,
) : PathTargetResult<Section>

private data class RejectedPathTarget<Section : Any>(
    val result: LaydrNavPathRejected,
) : PathTargetResult<Section>

private sealed interface ExternalTargetResult<Section : Any>

private data class AcceptedExternalTarget<Section : Any>(
    val section: Section,
    val result: LaydrNavExternalTargetAccepted,
) : ExternalTargetResult<Section>

private data class RejectedExternalTarget<Section : Any>(
    val result: LaydrNavExternalTargetRejected,
) : ExternalTargetResult<Section>
