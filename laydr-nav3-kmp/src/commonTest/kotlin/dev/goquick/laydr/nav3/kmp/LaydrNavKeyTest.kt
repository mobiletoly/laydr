package dev.goquick.laydr.nav3.kmp

import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteSegment
import dev.goquick.laydr.core.LaydrScreenRouteRef
import dev.goquick.laydr.core.LaydrScreenDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LaydrNavKeyTest {
    @Test
    fun createsStaticRouteKey() {
        val home = route(id = "Home", segments = listOf(static("home")))

        assertEquals(LaydrNavKey(routeId = "Home"), home.key().navKey())
    }

    @Test
    fun createsDynamicRouteKeyWithRequiredParams() {
        val byId = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertEquals(
            LaydrNavKey(
                routeId = "Users.ById",
                parameters = mapOf("id" to "alpha"),
            ),
            byId.key(mapOf("id" to "alpha")).navKey(),
        )
    }

    @Test
    fun navKeyCopiesParameters() {
        val parameters = mutableMapOf("id" to "alpha")

        val key = LaydrNavKey(routeId = "Users.ById", parameters = parameters)
        parameters["id"] = "changed"

        assertEquals(mapOf("id" to "alpha"), key.parameters)
    }

    @Test
    fun navKeyValidatesRouteId() {
        assertFailsWith<IllegalArgumentException> {
            LaydrNavKey(routeId = "bad-id")
        }
    }

    @Test
    fun convertsScreenDestinationToNav3Key() {
        val destination = TestDestination(
            routeKey = LaydrRouteKey(
                routeId = "Users.ById",
                parameters = mapOf("id" to "alpha"),
            ),
        )

        assertEquals(
            LaydrNavKey(
                routeId = "Users.ById",
                parameters = mapOf("id" to "alpha"),
            ),
            destination.navKey(),
        )
    }

    @Test
    fun entryTokenDistinguishesRuntimeEntriesWithoutChangingRouteIdentity() {
        val base = LaydrNavKey(
            routeId = "Users.ById",
            parameters = mapOf("id" to "alpha"),
        )
        val first = base.withEntryToken("first")
        val second = base.withEntryToken("second")

        assertNotEquals(base, first)
        assertNotEquals(first, second)
        assertEquals(base.toLaydrRouteKey(), first.toLaydrRouteKey())
        assertEquals(base.toLaydrRouteKey(), second.toLaydrRouteKey())
    }

    @Test
    fun rejectsMissingDynamicParams() {
        val byId = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            byId.key().navKey()
        }

        assertEquals("Route parameters missing for Users.ById: [id]", error.message)
    }

    @Test
    fun rejectsExtraParams() {
        val home = route(id = "Home", segments = listOf(static("home")))

        val error = assertFailsWith<IllegalArgumentException> {
            home.key(mapOf("unused" to "value"))
        }

        assertEquals("Route parameters not used by Home: [unused]", error.message)
    }

    @Test
    fun convertsPathToKeyWithDecodedParams() {
        val byId = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertEquals(
            LaydrNavKey(
                routeId = "Users.ById",
                parameters = mapOf("id" to "a/b"),
            ),
            laydrNavKeyForPath(
                "/users/a%2Fb",
                routeMap(routes = listOf(byId), screenRoutes = listOf(byId), layoutRoutes = emptyList()),
            ),
        )
    }

    @Test
    fun returnsNullForInvalidPaths() {
        val home = route(id = "Home", segments = listOf(static("home")))

        val routeMap = routeMap(routes = listOf(home), screenRoutes = listOf(home), layoutRoutes = emptyList())

        assertNull(laydrNavKeyForPath("/home?tab=profile", routeMap))
        assertNull(laydrNavKeyForPath("/home#section", routeMap))
        assertNull(laydrNavKeyForPath("/home/", routeMap))
        assertNull(laydrNavKeyForPath("/missing", routeMap))
    }

    @Test
    fun resolvesPathResultReasons() {
        val home = route(id = "Home", segments = listOf(static("home")))
        val settings = route(id = "Settings", segments = listOf(static("settings")))
        val routeMap = routeMap(routes = listOf(home, settings), screenRoutes = listOf(home), layoutRoutes = listOf(settings))

        assertAccepted(laydrNavPathResult("/home", routeMap))
        assertRejected(
            laydrNavPathResult("/home?tab=profile", routeMap),
            LaydrNavPathRejectionReason.UnsupportedPath,
        )
        assertRejected(
            laydrNavPathResult("/missing", routeMap),
            LaydrNavPathRejectionReason.UnknownRoute,
        )
        assertRejected(
            laydrNavPathResult("/settings", routeMap),
            LaydrNavPathRejectionReason.LayoutOnlyRoute,
        )
    }

    @Test
    fun resolvesExternalTargetResultReasonsAndPreservesRawParts() {
        val detail = route(
            id = "Contacts.ById",
            segments = listOf(static("contacts"), dynamic("id")),
        )
        val settings = route(id = "Settings", segments = listOf(static("settings")))
        val routeMap = routeMap(
            routes = listOf(detail, settings),
            screenRoutes = listOf(detail),
            layoutRoutes = listOf(settings),
        )

        val appPath = assertExternalAccepted(
            laydrNavExternalTargetResult(
                "/contacts/ada?mode=preview#notes",
                routeMap,
            ),
        )

        assertEquals("/contacts/ada", appPath.path)
        assertEquals("mode=preview", appPath.query)
        assertEquals("notes", appPath.fragment)
        assertEquals(detail.key(mapOf("id" to "ada")).navKey(), appPath.key)

        val absoluteUrl = assertExternalAccepted(
            laydrNavExternalTargetResult(
                "https://example.test/contacts/a%2Fb?x=1#details",
                routeMap,
            ),
        )

        assertEquals("/contacts/a%2Fb", absoluteUrl.path)
        assertEquals("x=1", absoluteUrl.query)
        assertEquals("details", absoluteUrl.fragment)
        assertEquals(detail.key(mapOf("id" to "a/b")).navKey(), absoluteUrl.key)
        assertExternalRejected(
            laydrNavExternalTargetResult("https://example.test", routeMap),
            LaydrNavExternalTargetRejectionReason.UnsupportedTarget,
        )
        val unsupportedWithRawParts = assertExternalRejected(
            laydrNavExternalTargetResult(
                "https://example.test?next=/contacts#top",
                routeMap,
            ),
            LaydrNavExternalTargetRejectionReason.UnsupportedTarget,
        )
        assertNull(unsupportedWithRawParts.path)
        assertEquals("next=/contacts", unsupportedWithRawParts.query)
        assertEquals("top", unsupportedWithRawParts.fragment)
        assertExternalRejected(
            laydrNavExternalTargetResult("/contacts/ada/", routeMap),
            LaydrNavExternalTargetRejectionReason.UnsupportedPath,
        )
        assertExternalRejected(
            laydrNavExternalTargetResult("/missing?next=/contacts/ada", routeMap),
            LaydrNavExternalTargetRejectionReason.UnknownRoute,
        )
        assertExternalRejected(
            laydrNavExternalTargetResult("/settings#main", routeMap),
            LaydrNavExternalTargetRejectionReason.LayoutOnlyRoute,
        )
    }

    @Test
    fun convertsPathsBySearchingRouteTrees() {
        val layout = route(id = "Items", segments = listOf(static("items")))
        val screen = route(
            id = "Items.ById",
            segments = listOf(static("items"), dynamic("id")),
        )

        assertNull(
            laydrNavKeyForPath(
                "/items",
                routeMap(routes = listOf(screen), screenRoutes = listOf(screen), layoutRoutes = emptyList()),
            ),
        )
        assertNull(
            laydrNavKeyForPath(
                "/items",
                routeMap(routes = listOf(layout), screenRoutes = emptyList(), layoutRoutes = listOf(layout)),
            ),
        )

        val detailLayout = route(
            id = "Items.Detail",
            segments = listOf(static("items"), static("detail")),
        )
        val itemsScreen = route(
            id = "Items",
            segments = listOf(static("items")),
            children = listOf(detailLayout),
        )

        val itemsRouteMap = routeMap(
            routes = listOf(itemsScreen),
            screenRoutes = listOf(itemsScreen),
            layoutRoutes = listOf(detailLayout),
        )

        assertNull(laydrNavKeyForPath("/items/detail", itemsRouteMap))
        assertEquals("/items/detail", detailLayout.key().navKey().path(itemsRouteMap))
    }

    @Test
    fun buildsPathFromKey() {
        val byId = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertEquals(
            "/users/a%2Fb",
            LaydrNavKey(
                routeId = "Users.ById",
                parameters = mapOf("id" to "a/b"),
            ).path(routeMap(routes = listOf(byId), screenRoutes = listOf(byId), layoutRoutes = emptyList())),
        )
    }

    @Test
    fun returnsNullWhenKeyRouteIdIsUnknown() {
        val home = route(id = "Home", segments = listOf(static("home")))

        assertNull(
            LaydrNavKey(routeId = "Missing").path(
                routeMap(routes = listOf(home), screenRoutes = listOf(home), layoutRoutes = emptyList()),
            ),
        )
    }

    @Test
    fun returnsNullWhenKeyParamsAreInvalid() {
        val byId = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertNull(
            LaydrNavKey(routeId = "Users.ById").path(
                routeMap(routes = listOf(byId), screenRoutes = listOf(byId), layoutRoutes = emptyList()),
            ),
        )
    }

    @Test
    fun navKeyPredicatesValidateRoutesAndSubtrees() {
        val detail = route(
            id = "Contacts.ById",
            segments = listOf(static("contacts"), dynamic("id")),
        )
        val contacts = route(
            id = "Contacts",
            segments = listOf(static("contacts")),
            children = listOf(detail),
        )
        val profile = route(id = "Profile", segments = listOf(static("profile")))
        val routeMap = routeMap(
            routes = listOf(contacts, profile),
            screenRoutes = listOf(contacts, detail, profile),
            layoutRoutes = emptyList(),
        )
        val contactsRef = screenRef(contacts)
        val detailRef = screenRef(detail)
        val profileRef = screenRef(profile)
        val detailKey = detail.key(mapOf("id" to "alpha")).navKey()
        val invalidDetailKey = LaydrNavKey(routeId = "Contacts.ById")
        val foreignKey = ForeignNavKey

        assertTrue(detailKey.isLaydrRoute(detailRef))
        assertFalse(detailKey.isLaydrRoute(contactsRef))
        assertFalse(invalidDetailKey.isLaydrRoute(detailRef))
        assertFalse(foreignKey.isLaydrRoute(detailRef))

        assertTrue(detailKey.isInLaydrRouteTree(routeMap, contactsRef))
        assertFalse(detailKey.isInLaydrRouteTree(routeMap, profileRef))
        assertFalse(invalidDetailKey.isInLaydrRouteTree(routeMap, contactsRef))
        assertFalse(foreignKey.isInLaydrRouteTree(routeMap, contactsRef))

        val stack = listOf<NavKey>(profile.key().navKey(), detailKey)
        assertTrue(stack.topIsLaydrRoute(detailRef))
        assertTrue(stack.topIsInLaydrRouteTree(routeMap, contactsRef))
        assertFalse(stack.topIsLaydrRoute(profileRef))
    }

    @Test
    fun appOwnedParentStackHelpersMutateLaydrKeysAfterValidation() {
        val editor = route(
            id = "Editor.ById",
            segments = listOf(static("editor"), dynamic("id")),
        )
        val profile = route(id = "Profile", segments = listOf(static("profile")))
        val routeMap = routeMap(
            routes = listOf(editor, profile),
            screenRoutes = listOf(editor, profile),
            layoutRoutes = emptyList(),
        )
        val appGraph = LaydrAppGraph(routeMap)
        val editorRef = screenRef(editor)
        val profileRef = screenRef(profile)
        val stack = mutableListOf<NavKey>(profile.key().navKey())

        val editorAlpha = TestDestination(editor.key(mapOf("id" to "alpha")))
        val editorBravo = TestDestination(editor.key(mapOf("id" to "bravo")))
        val profileDestination = TestDestination(profile.key())

        assertEquals(editor.key(mapOf("id" to "alpha")).navKey(), stack.pushLaydr(appGraph, editorAlpha))
        assertEquals(listOf<NavKey>(profile.key().navKey(), editor.key(mapOf("id" to "alpha")).navKey()), stack)

        assertTrue(stack.replaceTopLaydrIf(appGraph, editorRef, editorBravo))
        assertEquals(listOf<NavKey>(profile.key().navKey(), editor.key(mapOf("id" to "bravo")).navKey()), stack)

        assertFalse(stack.replaceTopLaydrIf(appGraph, profileRef, profileDestination))
        assertEquals(listOf<NavKey>(profile.key().navKey(), editor.key(mapOf("id" to "bravo")).navKey()), stack)

        assertEquals(profile.key().navKey(), stack.replaceTopWithLaydr(appGraph, profileDestination))
        assertEquals(listOf<NavKey>(profile.key().navKey(), profile.key().navKey()), stack)

        assertFailsWith<IllegalArgumentException> {
            stack.pushLaydr(appGraph, TestDestination(LaydrRouteKey(routeId = "Missing")))
        }
        assertFailsWith<IllegalStateException> {
            mutableListOf<NavKey>().replaceTopWithLaydr(appGraph, profileDestination)
        }
    }

    @Test
    fun parentStackPruningRemovesEntriesAboveLastMarker() {
        val shell = ForeignNavKey
        val first = LaydrNavKey(routeId = "First")
        val second = LaydrNavKey(routeId = "Second")
        val third = LaydrNavKey(routeId = "Third")
        val stack = mutableListOf<NavKey>(shell, first, second, shell, third)

        assertEquals(1, stack.removeEntriesAboveLast(shell))
        assertEquals(listOf(shell, first, second, shell), stack)
        assertEquals(2, stack.removeEntriesAboveLast { key -> key == first })
        assertEquals(listOf(shell, first), stack)
        assertEquals(0, stack.removeEntriesAboveLast(LaydrNavKey(routeId = "Missing")))
        assertEquals(listOf(shell, first), stack)
        assertEquals(0, stack.removeEntriesAboveLast(first))
        assertEquals(listOf(shell, first), stack)
    }
}

private fun routeMap(
    routes: List<LaydrRoute>,
    screenRoutes: List<LaydrRoute>,
    layoutRoutes: List<LaydrRoute>,
): LaydrRouteMap =
    LaydrRouteMap(
        routes = routes,
        screenRoutes = screenRoutes,
        layoutRoutes = layoutRoutes,
    )

internal fun route(
    id: String,
    segments: List<LaydrRouteSegment>,
    children: List<LaydrRoute> = emptyList(),
): LaydrRoute =
    LaydrRoute(
        id = id,
        segments = segments,
        children = children,
    )

internal fun static(value: String): LaydrRouteSegment.Static =
    LaydrRouteSegment.Static(value)

internal fun dynamic(parameterName: String): LaydrRouteSegment.Dynamic =
    LaydrRouteSegment.Dynamic(parameterName)

private data class TestDestination(
    override val routeKey: LaydrRouteKey,
) : LaydrScreenDestination

private object ForeignNavKey : NavKey

private fun screenRef(route: LaydrRoute): LaydrScreenRouteRef =
    object : LaydrScreenRouteRef {
        override val route: LaydrRoute = route
    }

internal fun assertExternalAccepted(
    result: LaydrNavExternalTargetResult,
): LaydrNavExternalTargetAccepted {
    assertTrue(result.accepted)
    assertTrue(result is LaydrNavExternalTargetAccepted)
    return result
}

internal fun assertExternalRejected(
    result: LaydrNavExternalTargetResult,
    reason: LaydrNavExternalTargetRejectionReason,
): LaydrNavExternalTargetRejected {
    assertFalse(result.accepted)
    assertTrue(result is LaydrNavExternalTargetRejected)
    assertEquals(reason, result.reason)
    return result
}
