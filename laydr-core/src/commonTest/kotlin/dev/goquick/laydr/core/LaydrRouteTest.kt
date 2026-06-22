package dev.goquick.laydr.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LaydrRouteTest {
    @Test
    fun derivesStaticRouteTemplate() {
        val route = route(
            id = "Home",
            segments = listOf(static("home")),
        )

        assertEquals("/home", route.pathTemplate)
        assertEquals(emptyList(), route.parameterNames)
        assertEquals("/home", route.buildPath())
    }

    @Test
    fun derivesDynamicRouteTemplate() {
        val route = route(
            id = "UsersById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertEquals("/users/{id}", route.pathTemplate)
        assertEquals(listOf("id"), route.parameterNames)
    }

    @Test
    fun derivesNestedRouteTemplate() {
        val profile = route(
            id = "Settings.Profile",
            segments = listOf(static("settings"), static("profile")),
        )
        val settings = route(
            id = "Settings",
            segments = listOf(static("settings")),
            children = listOf(profile),
        )

        assertEquals("/settings", settings.pathTemplate)
        assertEquals("/settings/profile", settings.children.single().pathTemplate)
    }

    @Test
    fun defaultsMetadataNameToRouteId() {
        val route = route(
            id = "Home",
            segments = listOf(static("home")),
        )

        assertEquals(LaydrRouteMetadata(name = "Home"), route.metadata)
        assertEquals("Home", route.metadata.name)
        assertEquals(emptyMap(), route.metadata.labels)
    }

    @Test
    fun retainsExplicitMetadata() {
        val route = route(
            id = "Home",
            segments = listOf(static("home")),
            metadata = LaydrRouteMetadata(
                name = "Home Page",
                labels = mapOf("area" to "main"),
            ),
        )

        assertEquals("Home Page", route.metadata.name)
        assertEquals(mapOf("area" to "main"), route.metadata.labels)
    }

    @Test
    fun findMatchDeliversRouteMetadata() {
        val profile = route(
            id = "Settings.Profile",
            segments = listOf(static("settings"), static("profile")),
            metadata = LaydrRouteMetadata(name = "Settings Profile"),
        )
        val settings = route(
            id = "Settings",
            segments = listOf(static("settings")),
            children = listOf(profile),
            metadata = LaydrRouteMetadata(name = "Settings"),
        )

        val match = settings.findMatch("/settings/profile")

        assertSame(profile, match?.route)
        assertEquals("Settings Profile", match?.route?.metadata?.name)
    }

    @Test
    fun copiesMetadataLabels() {
        val labels = mutableMapOf("area" to "main")

        val metadata = LaydrRouteMetadata(name = "Home", labels = labels)
        labels["area"] = "changed"

        assertEquals(mapOf("area" to "main"), metadata.labels)
    }

    @Test
    fun validatesMetadataConstruction() {
        assertFailsWith<IllegalArgumentException> {
            LaydrRouteMetadata(name = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrRouteMetadata(name = "Home", labels = mapOf(" " to "main"))
        }
    }

    @Test
    fun buildPathEncodesDynamicSegmentValues() {
        val route = route(
            id = "UsersById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertEquals("/users/abc%20123", route.buildPath(mapOf("id" to "abc 123")))
        assertEquals("/users/a%2Fb", route.buildPath(mapOf("id" to "a/b")))
        assertEquals("/users/m%C3%BCller", route.buildPath(mapOf("id" to "m\u00FCller")))
        assertEquals("/users/100%25", route.buildPath(mapOf("id" to "100%")))
        assertEquals("/users/a%2Bb", route.buildPath(mapOf("id" to "a+b")))
    }

    @Test
    fun buildPathRequiresExactDynamicParameters() {
        val route = route(
            id = "UsersById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertFailsWith<IllegalArgumentException> {
            route.buildPath()
        }
        assertFailsWith<IllegalArgumentException> {
            route.buildPath(mapOf("id" to "1", "extra" to "2"))
        }
        assertFailsWith<IllegalArgumentException> {
            route.buildPath(mapOf("id" to " "))
        }
    }

    @Test
    fun createsRouteKeyAfterValidatingParameters() {
        val route = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertEquals(
            LaydrRouteKey(routeId = "Users.ById", parameters = mapOf("id" to "alpha")),
            route.key(mapOf("id" to "alpha")),
        )

        assertFailsWith<IllegalArgumentException> {
            route.key()
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrRouteKey(routeId = "bad-id")
        }
    }

    @Test
    fun routeKeyCopiesParameters() {
        val parameters = mutableMapOf("id" to "alpha")

        val key = LaydrRouteKey(routeId = "Users.ById", parameters = parameters)
        parameters["id"] = "changed"

        assertEquals(mapOf("id" to "alpha"), key.parameters)
    }

    @Test
    fun routeMatchCopiesParameters() {
        val route = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )
        val parameters = mutableMapOf("id" to "alpha")

        val match = LaydrRouteMatch(route = route, parameters = parameters)
        parameters["id"] = "changed"

        assertEquals(mapOf("id" to "alpha"), match.parameters)
    }

    @Test
    fun parameterlessScreenRouteRefExposesDefaultDestination() {
        val home = route(id = "Home", segments = listOf(static("home")))
        val destination = object : LaydrScreenDestination {
            override val routeKey: LaydrRouteKey = home.key()
        }
        val routeRef = object : LaydrParameterlessScreenRouteRef {
            override val route: LaydrRoute = home
            override val defaultDestination: LaydrScreenDestination = destination
        }

        assertSame(home, routeRef.route)
        assertSame(destination, routeRef.defaultDestination)
    }

    @Test
    fun resolvesRouteKeyForNestedPath() {
        val detail = route(
            id = "Items.ById",
            segments = listOf(static("items"), dynamic("id")),
        )
        val items = route(
            id = "Items",
            segments = listOf(static("items")),
            children = listOf(detail),
        )
        val routeMap = LaydrRouteMap(
            routes = listOf(items),
            screenRoutes = listOf(items, detail),
            layoutRoutes = emptyList(),
        )

        assertEquals(
            LaydrRouteKey(routeId = "Items.ById", parameters = mapOf("id" to "alpha")),
            routeMap.keyForPath("/items/alpha"),
        )
        assertEquals(
            LaydrRouteKey(routeId = "Items.ById", parameters = mapOf("id" to "a/b")),
            routeMap.keyForPath("/items/a%2Fb"),
        )
        assertSame(detail, routeMap.routeFor(LaydrRouteKey(routeId = "Items.ById", mapOf("id" to "alpha"))))
        assertSame(detail, routeMap.screenRouteFor(LaydrRouteKey(routeId = "Items.ById", mapOf("id" to "alpha"))))
        assertSame(items, routeMap.topLevelRouteFor(LaydrRouteKey(routeId = "Items.ById", mapOf("id" to "alpha"))))
        assertNull(routeMap.keyForPath("/items/alpha?tab=details"))
        assertNull(routeMap.keyForPath("/items/alpha/"))
    }

    @Test
    fun buildsPathFromRouteKeyBySearchingNestedRoutes() {
        val detail = route(
            id = "Items.ById",
            segments = listOf(static("items"), dynamic("id")),
        )
        val items = route(
            id = "Items",
            segments = listOf(static("items")),
            children = listOf(detail),
        )
        val routeMap = LaydrRouteMap(
            routes = listOf(items),
            screenRoutes = listOf(detail),
            layoutRoutes = listOf(items),
        )

        assertEquals(
            "/items/a%2Fb",
            routeMap.pathFor(LaydrRouteKey(routeId = "Items.ById", parameters = mapOf("id" to "a/b"))),
        )
        assertEquals(listOf(items, detail), routeMap.routeChainFor(detail))
        assertEquals(listOf(items), routeMap.layoutChainFor(detail))
        assertEquals(emptyList(), routeMap.routeChainFor(route(id = "Missing", segments = listOf(static("missing")))))
        assertNull(routeMap.pathFor(LaydrRouteKey(routeId = "Missing")))
        assertNull(routeMap.pathFor(LaydrRouteKey(routeId = "Items.ById")))
        assertNull(routeMap.screenRouteFor(LaydrRouteKey(routeId = "Items")))
    }

    @Test
    fun routePredicatesValidateIdentityAndParameters() {
        val detail = route(
            id = "Items.ById",
            segments = listOf(static("items"), dynamic("id")),
        )
        val items = route(
            id = "Items",
            segments = listOf(static("items")),
            children = listOf(detail),
        )
        val routeMap = LaydrRouteMap(
            routes = listOf(items),
            screenRoutes = listOf(items, detail),
            layoutRoutes = emptyList(),
        )
        val itemsRef = screenRef(items)
        val detailRef = screenRef(detail)
        val validDetailKey = LaydrRouteKey(
            routeId = "Items.ById",
            parameters = mapOf("id" to "alpha"),
        )
        val invalidDetailKey = LaydrRouteKey(routeId = "Items.ById")
        val unknownKey = LaydrRouteKey(routeId = "Missing")
        val foreignParentRef = screenRef(
            route(
                id = "Foreign",
                segments = listOf(static("items")),
                children = listOf(detail),
            ),
        )

        assertTrue(detailRef.matches(validDetailKey))
        assertTrue(validDetailKey.isRoute(detailRef))
        assertFalse(itemsRef.matches(validDetailKey))
        assertFalse(detailRef.matches(invalidDetailKey))
        assertFalse(detailRef.matches(unknownKey))

        assertTrue(routeMap.contains(itemsRef, validDetailKey))
        assertTrue(validDetailKey.isInRouteTree(routeMap, itemsRef))
        assertFalse(routeMap.contains(detailRef, items.key()))
        assertFalse(routeMap.contains(foreignParentRef, validDetailKey))
        assertFalse(routeMap.contains(itemsRef, invalidDetailKey))
        assertFalse(routeMap.contains(itemsRef, unknownKey))
    }

    @Test
    fun appGraphDelegatesRouteListsAndResolvesScreenDestinations() {
        val detail = route(
            id = "Items.ById",
            segments = listOf(static("items"), dynamic("id")),
        )
        val items = route(
            id = "Items",
            segments = listOf(static("items")),
            children = listOf(detail),
        )
        val routeMap = LaydrRouteMap(
            routes = listOf(items),
            screenRoutes = listOf(detail),
            layoutRoutes = listOf(items),
        )
        val appGraph = LaydrAppGraph(routeMap)
        val destination = TestDestination(detail.key(mapOf("id" to "a/b")))

        assertEquals(listOf(items), appGraph.routes)
        assertEquals(listOf(detail), appGraph.screenRoutes)
        assertEquals(listOf(items), appGraph.layoutRoutes)
        assertSame(detail, appGraph.screenRouteFor(destination))
        assertSame(detail, appGraph.requireScreenRoute(destination))
        assertEquals("/items/a%2Fb", appGraph.pathFor(destination))
    }

    @Test
    fun appGraphRejectsNonScreenDestinations() {
        val detail = route(
            id = "Items.ById",
            segments = listOf(static("items"), dynamic("id")),
        )
        val items = route(
            id = "Items",
            segments = listOf(static("items")),
            children = listOf(detail),
        )
        val appGraph = LaydrAppGraph(
            LaydrRouteMap(
                routes = listOf(items),
                screenRoutes = listOf(detail),
                layoutRoutes = listOf(items),
            ),
        )

        assertNull(appGraph.screenRouteFor(TestDestination(LaydrRouteKey(routeId = "Missing"))))
        assertNull(appGraph.screenRouteFor(TestDestination(items.key())))
        assertNull(appGraph.screenRouteFor(TestDestination(LaydrRouteKey(routeId = "Items.ById"))))
        assertNull(
            appGraph.screenRouteFor(
                TestDestination(
                    LaydrRouteKey(
                        routeId = "Items.ById",
                        parameters = mapOf("id" to "alpha", "extra" to "unused"),
                    ),
                ),
            ),
        )
        assertNull(
            appGraph.screenRouteFor(
                TestDestination(
                    LaydrRouteKey(routeId = "Items.ById", parameters = mapOf("id" to " ")),
                ),
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            appGraph.requireScreenRoute(TestDestination(items.key()))
        }
    }

    @Test
    fun validatesRouteMapRouteMembership() {
        val home = route(id = "Home", segments = listOf(static("home")))
        val missing = route(id = "Missing", segments = listOf(static("missing")))

        assertFailsWith<IllegalArgumentException> {
            LaydrRouteMap(routes = listOf(home), screenRoutes = listOf(missing), layoutRoutes = emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrRouteMap(routes = listOf(home), screenRoutes = emptyList(), layoutRoutes = listOf(missing))
        }
    }

    @Test
    fun declaresUiNeutralRouteSections() {
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
        val routeMap = LaydrRouteMap(
            routes = listOf(contacts, profile),
            screenRoutes = listOf(contacts, detail, profile),
            layoutRoutes = emptyList(),
        )
        val contactsRef = screenRef(contacts)
        val profileRef = screenRef(profile)

        val sections = laydrRouteSections(routeMap) {
            section(contactsRef)
            section(profileRef)
        }

        assertEquals(listOf(contactsRef, profileRef), sections.items.map { section -> section.route })
        assertEquals(LaydrRouteKey(routeId = "Contacts"), sections.items.first().rootKey)
        assertTrue(sections.items.first().matches(LaydrRouteKey(routeId = "Contacts.ById", mapOf("id" to "a"))))
        assertFalse(sections.items.first().matches(LaydrRouteKey(routeId = "Profile")))
        assertSame(
            sections.items.first(),
            sections.sectionFor(LaydrRouteKey(routeId = "Contacts.ById", mapOf("id" to "a"))),
        )
        assertSame(sections.items.last(), sections.sectionFor(LaydrRouteKey(routeId = "Profile")))
        assertNull(sections.sectionFor(LaydrRouteKey(routeId = "Missing")))
    }

    @Test
    fun scopesDynamicRouteSectionsByRootKeyParameters() {
        val workspaceContacts = route(
            id = "Workspaces.ById.Contacts",
            segments = listOf(static("workspaces"), dynamic("workspace_id"), static("contacts")),
        )
        val workspace = route(
            id = "Workspaces.ById",
            segments = listOf(static("workspaces"), dynamic("workspace_id")),
            children = listOf(workspaceContacts),
        )
        val routeMap = LaydrRouteMap(
            routes = listOf(workspace),
            screenRoutes = listOf(workspace, workspaceContacts),
            layoutRoutes = emptyList(),
        )
        val workspaceRef = screenRef(workspace)
        val alphaRootKey = workspace.key(mapOf("workspace_id" to "alpha"))
        val bravoRootKey = workspace.key(mapOf("workspace_id" to "bravo"))
        val alphaContactsKey = workspaceContacts.key(mapOf("workspace_id" to "alpha"))
        val bravoContactsKey = workspaceContacts.key(mapOf("workspace_id" to "bravo"))

        val sections = laydrRouteSections(routeMap) {
            section(workspaceRef, alphaRootKey)
            section(workspaceRef, bravoRootKey)
        }

        assertEquals(listOf(alphaRootKey, bravoRootKey), sections.items.map { section -> section.rootKey })
        assertTrue(sections.items[0].matches(alphaContactsKey))
        assertFalse(sections.items[0].matches(bravoContactsKey))
        assertSame(sections.items[0], sections.sectionFor(alphaContactsKey))
        assertSame(sections.items[1], sections.sectionFor(bravoContactsKey))
    }

    @Test
    fun validatesRouteSectionRoots() {
        val detail = route(
            id = "Contacts.ById",
            segments = listOf(static("contacts"), dynamic("id")),
        )
        val contacts = route(
            id = "Contacts",
            segments = listOf(static("contacts")),
            children = listOf(detail),
        )
        val routeMap = LaydrRouteMap(
            routes = listOf(contacts),
            screenRoutes = listOf(detail),
            layoutRoutes = listOf(contacts),
        )

        assertFailsWith<IllegalArgumentException> {
            laydrRouteSections(routeMap) {
                section(screenRef(detail))
            }
        }
        assertFailsWith<IllegalArgumentException> {
            laydrRouteSections(routeMap) {
                section(screenRef(contacts))
            }
        }
        assertFailsWith<IllegalArgumentException> {
            laydrRouteSections(routeMap) {
                section(screenRef(detail), LaydrRouteKey(routeId = "Missing"))
            }
        }
        assertFailsWith<IllegalArgumentException> {
            laydrRouteSections(routeMap) {
                section(screenRef(detail), LaydrRouteKey(routeId = "Contacts.ById"))
            }
        }
    }

    @Test
    fun matchDecodesDynamicSegmentValues() {
        val route = route(
            id = "UsersById",
            segments = listOf(static("users"), dynamic("id")),
        )

        val match = route.match("/users/a%2Fb")

        assertSame(route, match?.route)
        assertEquals(mapOf("id" to "a/b"), match?.parameters)
    }

    @Test
    fun matchRejectsUnsupportedPaths() {
        val route = route(
            id = "UsersById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertNull(route.match("users/1"))
        assertNull(route.match("/users"))
        assertNull(route.match("/accounts/1"))
        assertNull(route.match("/users/1/extra"))
        assertNull(route.match("/users/1?tab=profile"))
        assertNull(route.match("/users/1#profile"))
        assertNull(route.match("/users/"))
        assertNull(route.match("/users/%"))
        assertNull(route.match("/users/%ZZ"))
        assertNull(route.match("/users/%FF"))
        assertNull(route.match("/users/%C3%28"))
        assertNull(route.match("/users/%20"))
    }

    @Test
    fun findMatchReturnsDescendantInDeclarationOrder() {
        val profile = route(
            id = "SettingsProfile",
            segments = listOf(static("settings"), static("profile")),
        )
        val security = route(
            id = "SettingsSecurity",
            segments = listOf(static("settings"), static("security")),
        )
        val settings = route(
            id = "Settings",
            segments = listOf(static("settings")),
            children = listOf(profile, security),
        )

        assertSame(settings, settings.findMatch("/settings")?.route)
        assertSame(profile, settings.findMatch("/settings/profile")?.route)
        assertSame(security, settings.findMatch("/settings/security")?.route)
        assertNull(settings.findMatch("/settings/billing"))
    }

    @Test
    fun findMatchPrefersStaticChildRoutesOverDynamicSiblings() {
        val dynamic = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )
        val settings = route(
            id = "Users.Settings",
            segments = listOf(static("users"), static("settings")),
        )
        val users = route(
            id = "Users",
            segments = listOf(static("users")),
            children = listOf(dynamic, settings),
        )

        val settingsMatch = users.findMatch("/users/settings")
        val dynamicMatch = users.findMatch("/users/alpha")

        assertSame(settings, settingsMatch?.route)
        assertEquals(emptyMap(), settingsMatch?.parameters)
        assertSame(dynamic, dynamicMatch?.route)
        assertEquals(mapOf("id" to "alpha"), dynamicMatch?.parameters)
    }

    @Test
    fun routeMapPrefersStaticTopLevelRoutesOverDynamicSiblings() {
        val dynamic = route(
            id = "Dynamic",
            segments = listOf(dynamic("id")),
        )
        val home = route(
            id = "Home",
            segments = listOf(static("home")),
        )
        val routeMap = LaydrRouteMap(
            routes = listOf(dynamic, home),
            screenRoutes = listOf(dynamic, home),
            layoutRoutes = emptyList(),
        )

        assertEquals(LaydrRouteKey(routeId = "Home"), routeMap.keyForPath("/home"))
        assertEquals(
            LaydrRouteKey(routeId = "Dynamic", parameters = mapOf("id" to "profile")),
            routeMap.keyForPath("/profile"),
        )
    }

    @Test
    fun copiesRouteListsBeforeDerivingState() {
        val segments = mutableListOf<LaydrRouteSegment>(static("users"), dynamic("id"))
        val details = route(
            id = "UsersDetails",
            segments = listOf(static("users"), dynamic("id"), static("details")),
        )
        val children = mutableListOf(details)

        val users = route(
            id = "Users",
            segments = segments,
            children = children,
        )

        segments.clear()
        segments += static("accounts")
        children.clear()

        assertEquals(listOf(static("users"), dynamic("id")), users.segments)
        assertEquals(listOf(details), users.children)
        assertEquals("/users/{id}", users.pathTemplate)
        assertEquals(listOf("id"), users.parameterNames)
        assertEquals("/users/42", users.buildPath(mapOf("id" to "42")))
        assertSame(details, users.findMatch("/users/42/details")?.route)
        assertNull(users.match("/accounts/42"))
    }

    @Test
    fun validatesRouteConstruction() {
        assertEquals(
            "Users.ById",
            route(id = "Users.ById", segments = listOf(static("users"), dynamic("id"))).id,
        )
        assertFailsWith<IllegalArgumentException> {
            route(id = "bad-id", segments = listOf(static("home")))
        }
        assertFailsWith<IllegalArgumentException> {
            route(id = "Users..ById", segments = listOf(static("users"), dynamic("id")))
        }
        assertFailsWith<IllegalArgumentException> {
            route(id = "Home", segments = emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            route(id = "Users", segments = listOf(dynamic("id"), dynamic("id")))
        }
        assertFailsWith<IllegalArgumentException> {
            route(
                id = "Users",
                segments = listOf(static("users")),
                children = listOf(
                    route(id = "UsersIndex", segments = listOf(static("users"), static("index"))),
                    route(id = "UsersIndex", segments = listOf(static("users"), static("profile"))),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            route(
                id = "Users",
                segments = listOf(static("users")),
                children = listOf(
                    route(id = "Profile", segments = listOf(static("profile"))),
                ),
            )
        }
    }

    @Test
    fun validatesRouteSegments() {
        assertFailsWith<IllegalArgumentException> {
            static("user profile")
        }
        assertFailsWith<IllegalArgumentException> {
            static("")
        }
        assertFailsWith<IllegalArgumentException> {
            dynamic("UserId")
        }
        assertFailsWith<IllegalArgumentException> {
            dynamic("id-1")
        }
    }

    @Test
    fun declaresCoreOnlyRouteKinds() {
        assertEquals(LaydrRouteKind.SCREEN, LaydrRouteDeclaration.screen().kind)
        assertEquals(LaydrRouteKind.LAYOUT, LaydrRouteDeclaration.layout().kind)
        assertEquals(
            LaydrRouteKind.SCREEN_AND_LAYOUT,
            LaydrRouteDeclaration.screenAndLayout().kind,
        )
    }

    @Test
    fun declaresCoreOnlyRouteMetadata() {
        val labels = mutableMapOf("area" to "shop")
        val declaration = LaydrRouteDeclaration.screen(
            name = "Product detail",
            labels = labels,
        )
        labels["area"] = "changed"

        assertEquals("Product detail", declaration.name)
        assertEquals(mapOf("area" to "shop"), declaration.labels)
    }

    @Test
    fun validatesCoreOnlyRouteMetadata() {
        assertFailsWith<IllegalArgumentException> {
            LaydrRouteDeclaration.screen(name = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrRouteDeclaration.layout(labels = mapOf(" " to "icon"))
        }
    }

    private fun route(
        id: String,
        segments: List<LaydrRouteSegment>,
        children: List<LaydrRoute> = emptyList(),
        metadata: LaydrRouteMetadata = LaydrRouteMetadata(name = id),
    ): LaydrRoute =
        LaydrRoute(
            id = id,
            segments = segments,
            children = children,
            metadata = metadata,
        )

    private fun static(value: String): LaydrRouteSegment.Static =
        LaydrRouteSegment.Static(value)

    private fun dynamic(parameterName: String): LaydrRouteSegment.Dynamic =
        LaydrRouteSegment.Dynamic(parameterName)

    private fun screenRef(route: LaydrRoute): LaydrScreenRouteRef =
        object : LaydrScreenRouteRef {
            override val route: LaydrRoute = route
        }

    private data class TestDestination(
        override val routeKey: LaydrRouteKey,
    ) : LaydrScreenDestination
}
