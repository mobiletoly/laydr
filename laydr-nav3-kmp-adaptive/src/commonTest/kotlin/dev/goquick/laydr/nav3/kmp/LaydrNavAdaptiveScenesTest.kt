package dev.goquick.laydr.nav3.kmp

import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteSegment
import dev.goquick.laydr.core.LaydrScreenRouteRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LaydrNavAdaptiveScenesTest {
    @Test
    fun buildsMaterialMetadataForListDetailEntries() {
        val graph = testGraph()
        val sceneSupport = laydrNavAdaptiveScenes(
            graph.appGraph,
            laydrNavListDetailScene(
                list = screenRef(graph.contacts),
                detail = screenRef(graph.detail),
                detailPlaceholder = {},
            ),
        )
        val provider = laydrNavEntryProvider(
            routeMap = graph.appGraph.routeMap,
            notFoundContent = {},
            sceneSupport = sceneSupport,
        ) {
            LaydrScreenContent {
            }
        }

        val listEntry = provider(graph.contacts.key().navKey())
        val detailEntry = provider(graph.detail.key(mapOf("id" to "ada")).navKey())
        val editEntry = provider(graph.edit.key(mapOf("id" to "ada")).navKey())

        assertEquals(emptyList(), sceneSupport.sceneStrategies)
        assertEquals(
            listOf(
                LaydrNavSceneStateKey(
                    listRouteId = "Contacts",
                    detailRouteId = "Contacts.ById",
                ),
            ),
            sceneSupport.stateKey,
        )
        assertTrue(listEntry.metadata.isNotEmpty())
        assertTrue(detailEntry.metadata.isNotEmpty())
        assertTrue(editEntry.metadata.isEmpty())
        assertEquals(
            LaydrNavListDetailStackState(
                listKey = graph.contacts.key().navKey(),
                detailKey = graph.detail.key(mapOf("id" to "ada")).navKey(),
                shape = LaydrNavListDetailStackShape.ListAndDetail,
            ),
            sceneSupport.listDetailStackState(
                listOf(
                    graph.contacts.key().navKey(),
                    graph.detail.key(mapOf("id" to "ada")).navKey(),
                ),
            ),
        )
    }

    @Test
    fun acceptsDynamicListRoutes() {
        val graph = testGraph()
        val sceneSupport = laydrNavAdaptiveScenes(
            graph.appGraph,
            laydrNavListDetailScene(
                list = screenRef(graph.detail),
                detail = screenRef(graph.edit),
                detailPlaceholder = {},
            ),
        )

        val listKey = graph.detail.key(mapOf("id" to "ada")).navKey()
        val detailKey = graph.edit.key(mapOf("id" to "ada")).navKey()

        assertEquals(
            listOf(
                LaydrNavSceneStateKey(
                    listRouteId = "Contacts.ById",
                    detailRouteId = "Contacts.ById.Edit",
                ),
            ),
            sceneSupport.stateKey,
        )
        assertEquals(
            LaydrNavListDetailStackState(
                listKey = listKey,
                detailKey = detailKey,
                shape = LaydrNavListDetailStackShape.ListAndDetail,
            ),
            sceneSupport.listDetailStackState(listOf(listKey, detailKey)),
        )
    }

    @Test
    fun rejectsInvalidListDetailDeclarations() {
        val graph = testGraph()

        assertFailsWith<IllegalArgumentException> {
            laydrNavAdaptiveScenes(
                graph.appGraph,
                laydrNavListDetailScene(
                    list = screenRef(graph.contacts),
                    detail = screenRef(graph.detail),
                    detailPlaceholder = {},
                ),
                laydrNavListDetailScene(
                    list = screenRef(graph.contacts),
                    detail = screenRef(graph.edit),
                    detailPlaceholder = {},
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            laydrNavAdaptiveScenes(
                graph.appGraph,
                laydrNavListDetailScene(
                    list = screenRef(graph.contacts),
                    detail = screenRef(graph.profile),
                    detailPlaceholder = {},
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            laydrNavAdaptiveScenes(
                graph.appGraph,
                laydrNavListDetailScene(
                    list = screenRef(graph.contacts),
                    detail = screenRef(graph.settings),
                    detailPlaceholder = {},
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            laydrNavAdaptiveScenes(
                graph.appGraph,
                laydrNavListDetailScene(
                    list = screenRef(
                        route(id = "Missing", segments = listOf(static("missing"))),
                    ),
                    detail = screenRef(graph.detail),
                    detailPlaceholder = {},
                ),
            )
        }
    }

    @Test
    fun rejectsDuplicateDetailRouteIdsBeforeSubtreeValidation() {
        val duplicatedDetailUnderTasks = route(
            id = "Contacts.ById",
            segments = listOf(static("tasks"), dynamic("id")),
        )
        val tasks = route(
            id = "Tasks",
            segments = listOf(static("tasks")),
            children = listOf(duplicatedDetailUnderTasks),
        )
        val graph = testGraph(
            extraTopLevelRoutes = listOf(tasks),
            extraScreenRoutes = listOf(tasks, duplicatedDetailUnderTasks),
        )

        assertFailsWith<IllegalArgumentException> {
            laydrNavAdaptiveScenes(
                graph.appGraph,
                laydrNavListDetailScene(
                    list = screenRef(graph.contacts),
                    detail = screenRef(graph.detail),
                    detailPlaceholder = {},
                ),
                laydrNavListDetailScene(
                    list = screenRef(tasks),
                    detail = screenRef(duplicatedDetailUnderTasks),
                    detailPlaceholder = {},
                ),
            )
        }
    }

    private fun testGraph(
        extraTopLevelRoutes: List<LaydrRoute> = emptyList(),
        extraScreenRoutes: List<LaydrRoute> = emptyList(),
    ): TestGraph {
        val edit = route(
            id = "Contacts.ById.Edit",
            segments = listOf(static("contacts"), dynamic("id"), static("edit")),
        )
        val detail = route(
            id = "Contacts.ById",
            segments = listOf(static("contacts"), dynamic("id")),
            children = listOf(edit),
        )
        val contacts = route(
            id = "Contacts",
            segments = listOf(static("contacts")),
            children = listOf(detail),
        )
        val profile = route(id = "Profile", segments = listOf(static("profile")))
        val settings = route(id = "Settings", segments = listOf(static("settings")))
        val routeMap = LaydrRouteMap(
            routes = listOf(contacts, profile, settings) + extraTopLevelRoutes,
            screenRoutes = listOf(contacts, detail, edit, profile) + extraScreenRoutes,
            layoutRoutes = listOf(settings),
        )
        return TestGraph(
            appGraph = LaydrAppGraph(routeMap),
            contacts = contacts,
            detail = detail,
            edit = edit,
            profile = profile,
            settings = settings,
        )
    }

    private data class TestGraph(
        val appGraph: LaydrAppGraph,
        val contacts: LaydrRoute,
        val detail: LaydrRoute,
        val edit: LaydrRoute,
        val profile: LaydrRoute,
        val settings: LaydrRoute,
    )

    private fun screenRef(route: LaydrRoute): LaydrScreenRouteRef =
        object : LaydrScreenRouteRef {
            override val route: LaydrRoute = route
        }
}

private fun route(
    id: String,
    segments: List<LaydrRouteSegment>,
    children: List<LaydrRoute> = emptyList(),
): LaydrRoute =
    LaydrRoute(
        id = id,
        segments = segments,
        children = children,
    )

private fun static(value: String): LaydrRouteSegment.Static =
    LaydrRouteSegment.Static(value)

private fun dynamic(parameterName: String): LaydrRouteSegment.Dynamic =
    LaydrRouteSegment.Dynamic(parameterName)
