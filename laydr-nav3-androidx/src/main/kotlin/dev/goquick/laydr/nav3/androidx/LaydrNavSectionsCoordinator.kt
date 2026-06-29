// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.androidx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.core.LaydrParameterlessScreenRouteRef
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.nav.runtime.LaydrNavSectionsEngine
import dev.goquick.laydr.nav.runtime.LaydrNavSelectedSectionState

/**
 * Remembers internal AndroidX Nav3 stack coordination for declared sections.
 */
@Composable
internal fun <Data : Any> rememberLaydrNavSectionsCoordinator(
    sections: LaydrNavSectionSet<Data>,
    initialSection: LaydrNavSection<Data> = sections.items.first(),
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
): LaydrNavSectionsCoordinator<Data> {
    val sectionControllers = remember(sections, sceneSupport) {
        mutableMapOf<LaydrNavSection<Data>, LaydrNavStackCoordinator>()
    }
    val selectedSectionState = rememberSaveable(
        sections.selectedSectionStateKey,
        initialSection.stateId,
    ) {
        mutableStateOf(initialSection.stateId)
    }
    val returnHistory = rememberSaveable(
        sections.selectedSectionStateKey,
        initialSection.stateId,
        saver = laydrNavReturnHistorySaver(),
    ) {
        mutableStateListOf<LaydrNavReturnEntry>()
    }

    if (sections.sectionForStateId(selectedSectionState.value) == null) {
        selectedSectionState.value = initialSection.stateId
        returnHistory.clear()
    }

    sectionControllers.keys.retainAll(sections.items.toSet())
    for (section in sections.items) {
        key(section.stateId) {
            sectionControllers[section] = rememberLaydrNavStackCoordinator(
                appGraph = sections.appGraph,
                initialDestination = section.rootDestination,
                sceneSupport = sceneSupport,
            )
        }
    }
    return remember(sections, initialSection, selectedSectionState, sceneSupport, returnHistory) {
        LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = initialSection,
            selectedSectionState = selectedSectionState,
            sectionControllers = sectionControllers,
            sceneSupport = sceneSupport,
            returnHistory = returnHistory,
        )
    }
}

internal class LaydrNavSectionsCoordinator<Data : Any> internal constructor(
    val sections: LaydrNavSectionSet<Data>,
    initialSection: LaydrNavSection<Data>,
    selectedSectionState: MutableState<String> = mutableStateOf(initialSection.stateId),
    sectionControllers: Map<LaydrNavSection<Data>, LaydrNavStackCoordinator>,
    internal val sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    internal val returnHistory: SnapshotStateList<LaydrNavReturnEntry> = mutableStateListOf(),
) : LaydrNavSectionsNavigator {
    private val sectionControllers: Map<LaydrNavSection<Data>, LaydrNavStackCoordinator> =
        sectionControllers.toMap()

    private val selectedSectionState: MutableState<String> = selectedSectionState
    internal val entryStore: LaydrNavEntryStore = LaydrNavEntryStore {
        this.sectionControllers.values.flatMap { controller -> controller.backStack }
    }
    private val runtimeEngine = LaydrNavSectionsEngine(
        sections = sections.items,
        initialSection = initialSection,
        selectedSectionState = object : LaydrNavSelectedSectionState {
            override var value: String
                get() = selectedSectionState.value
                set(value) {
                    selectedSectionState.value = value
                }
        },
        sectionStateId = { section -> section.stateId },
        sectionLabel = { section -> section.route.route.id },
        sectionForStateId = { stateId -> sections.sectionForStateId(stateId) },
        sectionForDestination = { destination ->
            sections.appGraph.requireScreenRoute(destination)
            sections.sectionFor(destination)
        },
        sectionForEntryKey = { key -> sections.sectionFor(key.toRouteKey()) },
        controllerFor = { section -> controllerFor(section).engine },
        returnHistory = returnHistory,
        entryStore = entryStore.runtimeStore,
    )

    val selectedSection: LaydrNavSection<Data>
        get() = runtimeEngine.selectedSection

    internal val selectedController: LaydrNavStackCoordinator
        get() = controllerFor(selectedSection)

    val selectedBackStack: NavBackStack<NavKey>
        get() = selectedController.backStack

    val currentPath: String?
        get() = runtimeEngine.currentPath

    val canPopSelectedStack: Boolean
        get() = runtimeEngine.canPopSelectedStack

    val canGoBack: Boolean
        get() = runtimeEngine.canGoBack

    val canReturn: Boolean
        get() = runtimeEngine.canReturn

    init {
        requireKnownSection(initialSection)
        require(sectionControllers.keys.containsAll(sections.items)) {
            "Laydr Nav controller map must contain every declared section"
        }
        selectedSectionState.value = selectedSection.stateId
    }

    internal fun controllerFor(section: LaydrNavSection<Data>): LaydrNavStackCoordinator =
        sectionControllers[section]
            ?: throw IllegalArgumentException(
                "Laydr Nav section is not owned by this controller: ${section.route.route.id}",
            )

    fun select(section: LaydrNavSection<Data>) {
        runtimeEngine.select(section)
    }

    override fun select(destination: LaydrScreenDestination) {
        runtimeEngine.select(destination)
    }

    override fun select(route: LaydrParameterlessScreenRouteRef) {
        select(route.defaultDestination)
    }

    fun selectPath(path: String): LaydrNavPathResult =
        runtimeEngine.selectPath(path).toAndroidx()

    fun selectExternalTarget(input: String): LaydrNavExternalTargetResult =
        runtimeEngine.selectExternalTarget(input).toAndroidx()

    override fun push(destination: LaydrScreenDestination) {
        push(LaydrNavLaunch(destination = destination))
    }

    override fun push(launch: LaydrNavLaunch) {
        runtimeEngine.push(launch)
    }

    override fun pushWithReturn(destination: LaydrScreenDestination) {
        pushWithReturn(LaydrNavLaunch(destination = destination))
    }

    override fun pushWithReturn(launch: LaydrNavLaunch) {
        runtimeEngine.pushWithReturn(launch)
    }

    fun pushPath(path: String): LaydrNavPathResult =
        runtimeEngine.pushPath(path).toAndroidx()

    fun pushExternalTarget(input: String): LaydrNavExternalTargetResult =
        runtimeEngine.pushExternalTarget(input).toAndroidx()

    override fun replace(destination: LaydrScreenDestination) {
        replace(LaydrNavLaunch(destination = destination))
    }

    override fun replace(launch: LaydrNavLaunch) {
        runtimeEngine.replace(launch)
    }

    override fun replaceWithReturn(destination: LaydrScreenDestination) {
        replaceWithReturn(LaydrNavLaunch(destination = destination))
    }

    override fun replaceWithReturn(launch: LaydrNavLaunch) {
        runtimeEngine.replaceWithReturn(launch)
    }

    fun replacePath(path: String): LaydrNavPathResult =
        runtimeEngine.replacePath(path).toAndroidx()

    fun replaceExternalTarget(input: String): LaydrNavExternalTargetResult =
        runtimeEngine.replaceExternalTarget(input).toAndroidx()

    override fun <Result : Any> pushForResult(
        launch: LaydrNavLaunch,
        onCancel: () -> Unit,
        resultType: kotlin.reflect.KClass<Result>,
        onResult: (Result) -> Unit,
    ) {
        runtimeEngine.pushForResult(
            launch = launch,
            onCancel = onCancel,
            resultType = resultType,
            onResult = onResult,
        )
    }

    override fun back(): Boolean {
        return runtimeEngine.back()
    }

    fun popSelectedStack(): Boolean =
        runtimeEngine.popSelectedStack()

    private fun requireKnownSection(section: LaydrNavSection<Data>): LaydrNavSection<Data> {
        require(section in sections.items) {
            "Laydr Nav section is not part of this section set: ${section.route.route.id}"
        }
        return section
    }
}
