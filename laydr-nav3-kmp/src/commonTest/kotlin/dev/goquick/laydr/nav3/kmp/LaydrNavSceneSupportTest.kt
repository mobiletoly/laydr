package dev.goquick.laydr.nav3.kmp

import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LaydrNavSceneSupportTest {
    @Test
    fun buildsListDetailMetadataAndDetailStackExpansion() {
        val graph = testGraph()
        val sceneSupport = testSceneSupport(graph.contacts, graph.detail)
        val detailKey = graph.detail.key(mapOf("id" to "ada")).navKey()

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
        assertEquals(mapOf("test:list" to "Contacts"), sceneSupport.metadataFor(graph.contacts))
        assertEquals(mapOf("test:detail" to "Contacts.ById"), sceneSupport.metadataFor(graph.detail))
        assertTrue(sceneSupport.metadataFor(graph.edit).isEmpty())
        assertEquals(
            listOf(graph.contacts.key().navKey(), detailKey),
            sceneSupport.expandedStackFor(detailKey),
        )
        assertTrue(sceneSupport.registeredDetailRouteId("Contacts.ById"))
    }

    @Test
    fun exposesListDetailStackStateWithoutChoosingUiPolicy() {
        val graph = testGraph()
        val sceneSupport = testSceneSupport(graph.contacts, graph.detail)
        val listKey = graph.contacts.key().navKey()
        val detailKey = graph.detail.key(mapOf("id" to "ada")).navKey()
        val editKey = graph.edit.key(mapOf("id" to "ada")).navKey()

        assertEquals(
            LaydrNavListDetailStackState(
                listKey = listKey,
                detailKey = null,
                shape = LaydrNavListDetailStackShape.ListOnly,
            ),
            sceneSupport.listDetailStackState(listOf(listKey)),
        )
        assertEquals(
            LaydrNavListDetailStackState(
                listKey = listKey,
                detailKey = detailKey,
                shape = LaydrNavListDetailStackShape.ListAndDetail,
            ),
            sceneSupport.listDetailStackState(listOf(listKey, detailKey)),
        )
        assertEquals(
            LaydrNavListDetailStackState(
                listKey = listKey,
                detailKey = detailKey,
                shape = LaydrNavListDetailStackShape.Nested,
            ),
            sceneSupport.listDetailStackState(listOf(listKey, detailKey, editKey)),
        )
        assertNull(sceneSupport.listDetailStackState(listOf(graph.profile.key().navKey())))
        assertNull(sceneSupport.listDetailStackState(listOf(listKey, ForeignKey)))
        assertNull(sceneSupport.listDetailStackState(listOf(listKey, LaydrNavKey(routeId = "Contacts.ById"))))
        assertNull(
            sceneSupport.listDetailStackState(
                listOf(
                    listKey,
                    LaydrNavKey(
                        routeId = "Contacts.ById",
                        parameters = mapOf("id" to " "),
                    ),
                ),
            ),
        )
    }

    @Test
    fun derivesDynamicListKeysFromDetailParameters() {
        val graph = testGraph()
        val sceneSupport = testSceneSupport(graph.workspaceItems, graph.workspaceItemDetail)
        val listKey = graph.workspaceItems.key(mapOf("workspace_id" to "acme")).navKey()
        val detailKey = graph.workspaceItemDetail
            .key(mapOf("workspace_id" to "acme", "item_id" to "roadmap"))
            .navKey()
        val otherWorkspaceDetailKey = graph.workspaceItemDetail
            .key(mapOf("workspace_id" to "other", "item_id" to "roadmap"))
            .navKey()

        assertEquals(
            listOf(listKey, detailKey),
            sceneSupport.expandedStackFor(detailKey),
        )
        assertEquals(
            1,
            sceneSupport.detailSelectionIndex(
                backStack = listOf(listKey, detailKey),
                key = graph.workspaceItemDetail
                    .key(mapOf("workspace_id" to "acme", "item_id" to "shipping"))
                    .navKey(),
            ),
        )
        assertNull(
            sceneSupport.detailSelectionIndex(
                backStack = listOf(listKey, detailKey),
                key = otherWorkspaceDetailKey,
            ),
        )
        assertEquals(
            LaydrNavListDetailStackState(
                listKey = listKey,
                detailKey = detailKey,
                shape = LaydrNavListDetailStackShape.ListAndDetail,
            ),
            sceneSupport.listDetailStackState(listOf(listKey, detailKey)),
        )
        assertNull(sceneSupport.listDetailStackState(listOf(listKey, otherWorkspaceDetailKey)))
        assertNull(
            sceneSupport.expandedStackFor(
                LaydrNavKey(
                    routeId = "Workspaces.ByWorkspaceId.Items.ByItemId",
                    parameters = mapOf("item_id" to "roadmap"),
                ),
            ),
        )
    }

    @Test
    fun notFoundEntriesDoNotReceiveSceneMetadata() {
        val graph = testGraph()
        val provider = laydrNavEntryProvider(
            routeMap = graph.appGraph.routeMap,
            notFoundContent = {},
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        ) {
            error("Screen content should not be evaluated while creating entries")
        }

        val missingEntry = provider(LaydrNavKey(routeId = "Missing"))

        assertTrue(missingEntry.metadata.isEmpty())
    }

    private fun testGraph(): TestGraph {
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
        val workspaceItemDetail = route(
            id = "Workspaces.ByWorkspaceId.Items.ByItemId",
            segments = listOf(
                static("workspaces"),
                dynamic("workspace_id"),
                static("items"),
                dynamic("item_id"),
            ),
        )
        val workspaceItems = route(
            id = "Workspaces.ByWorkspaceId.Items",
            segments = listOf(static("workspaces"), dynamic("workspace_id"), static("items")),
            children = listOf(workspaceItemDetail),
        )
        val workspace = route(
            id = "Workspaces.ByWorkspaceId",
            segments = listOf(static("workspaces"), dynamic("workspace_id")),
            children = listOf(workspaceItems),
        )
        val workspaces = route(
            id = "Workspaces",
            segments = listOf(static("workspaces")),
            children = listOf(workspace),
        )
        val routeMap = LaydrRouteMap(
            routes = listOf(contacts, profile, workspaces),
            screenRoutes = listOf(
                contacts,
                detail,
                edit,
                profile,
                workspaceItems,
                workspaceItemDetail,
            ),
            layoutRoutes = emptyList(),
        )
        return TestGraph(
            appGraph = LaydrAppGraph(routeMap),
            contacts = contacts,
            detail = detail,
            edit = edit,
            profile = profile,
            workspaceItems = workspaceItems,
            workspaceItemDetail = workspaceItemDetail,
        )
    }

    private data class TestGraph(
        val appGraph: LaydrAppGraph,
        val contacts: LaydrRoute,
        val detail: LaydrRoute,
        val edit: LaydrRoute,
        val profile: LaydrRoute,
        val workspaceItems: LaydrRoute,
        val workspaceItemDetail: LaydrRoute,
    )

    private object ForeignKey : NavKey
}
