package dev.goquick.laydr.nav3.kmp

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrParameterlessScreenRouteRef
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.core.LaydrScreenRouteRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LaydrNavSectionsTest {
    @Test
    fun declaresSectionsInOrderAndFindsOwningSection() {
        val graph = testGraph()
        val sections = testSections(graph)

        assertEquals(listOf("Contacts", "Profile"), sections.items.map { section -> section.route.route.id })
        assertEquals(LaydrRouteKey(routeId = "Contacts"), sections.items.first().rootKey)
        assertSame(
            sections.items.first(),
            sections.sectionFor(TestDestination(graph.detail.key(mapOf("id" to "alpha")))),
        )
        assertSame(
            sections.items.first(),
            sections.sectionFor(graph.detail.key(mapOf("id" to "alpha"))),
        )
        assertSame(sections.items.first(), sections.sectionForPath("/contacts/alpha"))
        assertSame(sections.items.last(), sections.sectionFor(TestDestination(graph.profile.key())))
        assertSame(sections.items.last(), sections.sectionForPath("/profile"))
        assertNull(sections.sectionFor(TestDestination(graph.about.key())))
        assertNull(sections.sectionForPath("/about"))
        assertNull(sections.sectionFor(TestDestination(graph.settings.key())))
        assertNull(sections.sectionForPath("/settings"))
        assertNull(sections.sectionFor(TestDestination(LaydrRouteKey(routeId = "Missing"))))
        assertNull(sections.sectionFor(LaydrRouteKey(routeId = "Contacts.ById")))
        assertNull(sections.sectionForPath("/contacts/alpha/"))
        assertNull(sections.sectionForPath("/missing"))
    }

    @Test
    fun describesSectionPlacementForDestinationsAndKeys() {
        val graph = testGraph()
        val sections = testSections(graph)

        val rootPlacement = assertNotNull(sections.placementFor(TestDestination(graph.contacts.key())))
        assertSame(sections.items.first(), rootPlacement.section)
        assertTrue(rootPlacement.isSectionRoot)
        assertEquals(0, rootPlacement.depthFromSectionRoot)

        val detailPlacement = sections.placementFor(graph.detail.key(mapOf("id" to "alpha")))
        assertSame(sections.items.first(), detailPlacement?.section)
        assertFalse(detailPlacement?.isSectionRoot == true)
        assertEquals(1, detailPlacement?.depthFromSectionRoot)

        val editPlacement = sections.placementFor(TestDestination(graph.edit.key(mapOf("id" to "alpha"))))
        assertSame(sections.items.first(), editPlacement?.section)
        assertFalse(editPlacement?.isSectionRoot == true)
        assertEquals(2, editPlacement?.depthFromSectionRoot)

        assertNull(sections.placementFor(TestDestination(graph.about.key())))
        assertNull(sections.placementFor(LaydrRouteKey(routeId = "Missing")))
    }

    @Test
    fun buildsTypedSectionSpecsWithDefaultDestinations() {
        val graph = testGraph()
        val sections = typedSections(graph, contactsData = "Contacts", profileData = "Profile")

        assertEquals(listOf("Contacts", "Profile"), sections.items.map { section -> section.data })
        assertEquals(graph.contacts.key(), sections.items.first().rootDestination.routeKey)
        assertEquals(graph.profile.key(), sections.items.last().rootDestination.routeKey)
    }

    @Test
    fun selectedSectionStateKeyIgnoresAppOwnedSectionDataChanges() {
        val graph = testGraph()
        val first = typedSections(graph, contactsData = "Contacts", profileData = "Profile")
        val second = typedSections(
            graph,
            contactsData = "Contacts updated",
            profileData = "Profile updated",
        )

        assertEquals(first.selectedSectionStateKey, second.selectedSectionStateKey)
        assertEquals(listOf("Contacts", "Profile"), first.items.map { section -> section.data })
        assertEquals(listOf("Contacts updated", "Profile updated"), second.items.map { section -> section.data })
    }

    @Test
    fun rejectsInvalidSectionDeclarations() {
        val graph = testGraph()

        assertFailsWith<IllegalArgumentException> {
            LaydrNavSectionSetBuilder(graph.appGraph).build()
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrNavSectionSetBuilder(graph.appGraph)
                .apply {
                    section(screenRef(graph.contacts), TestDestination(graph.contacts.key()))
                    section(screenRef(graph.contacts), TestDestination(graph.contacts.key()))
                }
                .build()
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrNavSectionSetBuilder(graph.appGraph)
                .apply {
                    section(
                        screenRef(graph.detail),
                        TestDestination(LaydrRouteKey(routeId = "Contacts.ById")),
                    )
                }
                .build()
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrNavSectionSetBuilder(graph.appGraph)
                .apply {
                    section(screenRef(graph.settings), TestDestination(graph.settings.key()))
                }
                .build()
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrNavSectionSetBuilder(graph.appGraph)
                .apply {
                    section(
                        screenRef(route(id = "Missing", segments = listOf(static("missing")))),
                        TestDestination(graph.contacts.key()),
                    )
                }
                .build()
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrNavSectionSetBuilder(graph.appGraph)
                .apply {
                    section(screenRef(graph.contacts), TestDestination(graph.profile.key()))
                }
                .build()
        }
        assertFailsWith<IllegalArgumentException> {
            LaydrNavSectionSetBuilder(graph.appGraph)
                .apply {
                    section(
                        screenRef(graph.contacts),
                        TestDestination(
                            LaydrRouteKey(
                                routeId = "Contacts",
                                parameters = mapOf("unused" to "value"),
                            ),
                        ),
                    )
                }
                .build()
        }
    }

    @Test
    fun selectsInitialSectionAndRejectsForeignInitialSection() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)

        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )

        assertSame(sections.items.first(), controller.selectedSection)
        assertSame(controllers.getValue(sections.items.first()), controller.selectedController)
        assertSame(controllers.getValue(sections.items.first()).backStack, controller.selectedBackStack)
        assertEquals("/contacts", controller.currentPath)

        val foreignSection = testSections(graph).items.first()
        assertFailsWith<IllegalArgumentException> {
            LaydrNavSectionsCoordinator(
                sections = sections,
                initialSection = foreignSection,
                sectionControllers = controllers,
            )
        }
    }

    @Test
    fun selectsSectionsWithoutMutatingStacks() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )

        val contactsStack = controllers.getValue(sections.items.first()).backStack.toList()
        val profileStack = controllers.getValue(sections.items.last()).backStack.toList()

        controller.select(sections.items.last())

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(contactsStack, controllers.getValue(sections.items.first()).backStack.toList())
        assertEquals(profileStack, controllers.getValue(sections.items.last()).backStack.toList())

        val foreignSection = testSections(graph).items.first()
        assertFailsWith<IllegalArgumentException> {
            controller.select(foreignSection)
        }
        assertSame(sections.items.last(), controller.selectedSection)
    }

    @Test
    fun selectsTypedDestinationsAndRoutesWithoutMutatingStacks() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )

        val contactsStack = controllers.getValue(sections.items.first()).backStack.toList()
        val profileStack = controllers.getValue(sections.items.last()).backStack.toList()

        controller.select(TestDestination(graph.profile.key()))

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(contactsStack, controllers.getValue(sections.items.first()).backStack.toList())
        assertEquals(profileStack, controllers.getValue(sections.items.last()).backStack.toList())

        controller.select(parameterlessScreenRef(graph.contacts))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(contactsStack, controllers.getValue(sections.items.first()).backStack.toList())
        assertEquals(profileStack, controllers.getValue(sections.items.last()).backStack.toList())

        assertFailsWith<IllegalArgumentException> {
            controller.select(TestDestination(graph.about.key()))
        }
        assertFailsWith<IllegalArgumentException> {
            controller.select(parameterlessScreenRef(graph.about))
        }
        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(contactsStack, controllers.getValue(sections.items.first()).backStack.toList())
        assertEquals(profileStack, controllers.getValue(sections.items.last()).backStack.toList())
    }

    @Test
    fun pushesAndReplacesOnTheOwningSectionStack() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )
        val contactsController = controllers.getValue(sections.items.first())
        val profileController = controllers.getValue(sections.items.last())
        val originalProfileStack = profileController.backStack.toList()

        controller.push(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            contactsController.backStack.toList(),
        )
        assertEquals(originalProfileStack, profileController.backStack.toList())
        assertEquals("/contacts/alpha", controller.currentPath)

        controller.push(TestDestination(graph.detail.key(mapOf("id" to "bravo"))))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            contactsController.backStack.toList(),
        )
        assertEquals(originalProfileStack, profileController.backStack.toList())
        assertEquals("/contacts/bravo", controller.currentPath)

        controller.select(sections.items.last())
        controller.replace(TestDestination(graph.detail.key(mapOf("id" to "bravo"))))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            contactsController.backStack.toList(),
        )
        assertEquals(originalProfileStack, profileController.backStack.toList())
        assertEquals("/contacts/bravo", controller.currentPath)
    }

    @Test
    fun selectsPushesAndReplacesPathsOnTheOwningSectionStack() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )
        val contactsController = controllers.getValue(sections.items.first())
        val profileController = controllers.getValue(sections.items.last())
        val originalContactsStack = contactsController.backStack.toList()
        val originalProfileStack = profileController.backStack.toList()

        assertAccepted(controller.selectPath("/profile"))

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(originalContactsStack, contactsController.backStack.toList())
        assertEquals(originalProfileStack, profileController.backStack.toList())

        assertAccepted(controller.pushPath("/contacts/alpha"))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            contactsController.backStack.toList(),
        )
        assertEquals(originalProfileStack, profileController.backStack.toList())
        assertEquals("/contacts/alpha", controller.currentPath)

        assertAccepted(controller.pushPath("/contacts/bravo"))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            contactsController.backStack.toList(),
        )
        assertEquals(originalProfileStack, profileController.backStack.toList())
        assertEquals("/contacts/bravo", controller.currentPath)

        controller.select(sections.items.last())

        assertAccepted(controller.replacePath("/contacts/bravo"))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            contactsController.backStack.toList(),
        )
        assertEquals(originalProfileStack, profileController.backStack.toList())
        assertEquals("/contacts/bravo", controller.currentPath)
    }

    @Test
    fun scopesDynamicSectionsByRootDestinationParameters() {
        val graph = testGraph()
        val sections = laydrNavSectionSet(
            graph.appGraph,
            laydrNavSection(
                route = screenRef(graph.detail),
                rootDestination = TestDestination(graph.detail.key(mapOf("id" to "alpha"))),
            ),
            laydrNavSection(
                route = screenRef(graph.detail),
                rootDestination = TestDestination(graph.detail.key(mapOf("id" to "bravo"))),
            ),
        )
        val controllers = controllersForDynamicSections(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )
        val alphaEditKey = graph.edit.key(mapOf("id" to "alpha"))
        val bravoEditKey = graph.edit.key(mapOf("id" to "bravo"))

        assertSame(sections.items.first(), sections.sectionForRootKey(graph.detail.key(mapOf("id" to "alpha"))))
        assertSame(sections.items.last(), sections.sectionForRootKey(graph.detail.key(mapOf("id" to "bravo"))))
        assertSame(sections.items.first(), sections.sectionFor(alphaEditKey))
        assertSame(sections.items.last(), sections.sectionFor(bravoEditKey))
        assertSame(sections.items.first(), sections.sectionForRouteId("Contacts.ById"))

        assertAccepted(controller.pushPath("/contacts/bravo/edit"))

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(
            listOf(
                graph.detail.key(mapOf("id" to "bravo")).navKey(),
                bravoEditKey.navKey(),
            ),
            controllers.getValue(sections.items.last()).backStack.toList(),
        )
        assertEquals(
            listOf(graph.detail.key(mapOf("id" to "alpha")).navKey()),
            controllers.getValue(sections.items.first()).backStack.toList(),
        )
    }

    @Test
    fun selectsPushesAndReplacesExternalTargetsOnOwningSectionStack() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )
        val contactsController = controllers.getValue(sections.items.first())
        val profileController = controllers.getValue(sections.items.last())
        val originalContactsStack = contactsController.backStack.toList()
        val originalProfileStack = profileController.backStack.toList()

        assertExternalAccepted(controller.selectExternalTarget("https://example.test/profile?tab=main#top"))

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(originalContactsStack, contactsController.backStack.toList())
        assertEquals(originalProfileStack, profileController.backStack.toList())

        val accepted = assertExternalAccepted(controller.pushExternalTarget("/contacts/alpha?mode=preview#notes"))

        assertEquals("/contacts/alpha", accepted.path)
        assertEquals("mode=preview", accepted.query)
        assertEquals("notes", accepted.fragment)
        assertSame(sections.items.first(), controller.selectedSection)
        assertTrue(controller.canPopSelectedStack)
        assertEquals(LaydrNavListDetailStackShape.ListAndDetail, controller.listDetailStackState?.shape)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            contactsController.backStack.toList(),
        )

        val contactsStackBeforeRejectedTarget = contactsController.backStack.toList()
        val profileStackBeforeRejectedTarget = profileController.backStack.toList()

        assertExternalRejected(
            controller.replaceExternalTarget("/about?x=1"),
            LaydrNavExternalTargetRejectionReason.OutsideDeclaredSection,
        )
        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(contactsStackBeforeRejectedTarget, contactsController.backStack.toList())
        assertEquals(profileStackBeforeRejectedTarget, profileController.backStack.toList())

        assertExternalAccepted(controller.replaceExternalTarget("/contacts/bravo"))
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            contactsController.backStack.toList(),
        )
    }

    @Test
    fun navigatorInterfaceExcludesOwnerOnlyHelpers() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )
        val navSections = testNavSections(sections, controller)
        val navigator: LaydrNavSectionsNavigator = navSections.navigator
        val contactsStack = controllers.getValue(sections.items.first()).backStack.toList()
        val profileStack = controllers.getValue(sections.items.last()).backStack.toList()

        navigator.select(TestDestination(graph.contacts.key()))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(contactsStack, controllers.getValue(sections.items.first()).backStack.toList())
        assertEquals(profileStack, controllers.getValue(sections.items.last()).backStack.toList())

        navigator.select(parameterlessScreenRef(graph.profile))

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(contactsStack, controllers.getValue(sections.items.first()).backStack.toList())
        assertEquals(profileStack, controllers.getValue(sections.items.last()).backStack.toList())

        navigator.push(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            controllers.getValue(sections.items.first()).backStack.toList(),
        )
    }

    @Test
    fun sectionLaunchStoresPayloadForCreatedEntry() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )

        controller.push(
            LaydrNavLaunch(
                destination = TestDestination(graph.detail.key(mapOf("id" to "alpha"))),
                payload = "section-payload",
            ),
        )

        val key = controllers.getValue(sections.items.first()).backStack.last() as LaydrNavKey
        val lookup = controller.entryStore.lookupPayload(key)

        assertTrue(lookup is LaydrNavPayloadLookup.Present)
        assertEquals("section-payload", lookup.payload)
    }

    @Test
    fun sectionNavigatorCompletesTypedResultOnce() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )
        val navSections = testNavSections(sections, controller)
        val results = mutableListOf<TestRouteResult>()
        var cancelCount = 0

        navSections.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(
                destination = TestDestination(graph.detail.key(mapOf("id" to "alpha"))),
            ),
            onCancel = { cancelCount += 1 },
        ) { result ->
            results += result
        }
        val key = controllers.getValue(sections.items.first()).backStack.last() as LaydrNavKey
        val lookup = controller.entryStore.lookupResult(key)

        assertTrue(lookup is LaydrNavResultLookup.Present)
        @Suppress("UNCHECKED_CAST")
        val sink = lookup.sink as LaydrNavResultSink<TestRouteResult>
        sink.complete(TestRouteResult("alpha"))
        sink.complete(TestRouteResult("bravo"))

        assertEquals(listOf(TestRouteResult("alpha")), results)
        assertEquals(0, cancelCount)
        assertEquals(0, controller.entryStore.resultCount)
    }

    @Test
    fun sectionResultCancelsWhenEntryLeavesStack() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )
        var cancelCount = 0

        controller.pushForResult(
            launch = LaydrNavLaunch(
                destination = TestDestination(graph.detail.key(mapOf("id" to "alpha"))),
            ),
            onCancel = { cancelCount += 1 },
            resultType = TestRouteResult::class,
        ) { _ -> }

        assertEquals(1, controller.entryStore.resultCount)

        controller.replace(TestDestination(graph.contacts.key()))

        assertEquals(1, cancelCount)
        assertEquals(0, controller.entryStore.resultCount)
    }

    @Test
    fun crossSectionPushForResultRestoresCallerAfterCompletionAndBack() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )
        val contactsController = controllers.getValue(sections.items.first())
        val profileController = controllers.getValue(sections.items.last())
        val sourceProfileStack = profileController.backStack.toList()
        val previousContactsStack = contactsController.backStack.toList()
        val results = mutableListOf<TestRouteResult>()
        var cancelCount = 0

        controller.pushForResult(
            launch = LaydrNavLaunch(
                destination = TestDestination(graph.detail.key(mapOf("id" to "alpha"))),
            ),
            onCancel = { cancelCount += 1 },
            resultType = TestRouteResult::class,
        ) { result ->
            results += result
        }

        assertSame(sections.items.first(), controller.selectedSection)
        assertTrue(controller.canReturn)
        assertEquals(
            graph.detail.key(mapOf("id" to "alpha")),
            (contactsController.backStack.last() as LaydrNavKey).toLaydrRouteKey(),
        )

        val key = contactsController.backStack.last() as LaydrNavKey
        val lookup = controller.entryStore.lookupResult(key)

        assertTrue(lookup is LaydrNavResultLookup.Present)
        @Suppress("UNCHECKED_CAST")
        val sink = lookup.sink as LaydrNavResultSink<TestRouteResult>
        sink.complete(TestRouteResult("alpha"))

        assertEquals(listOf(TestRouteResult("alpha")), results)
        assertEquals(0, cancelCount)
        assertEquals(0, controller.entryStore.resultCount)

        assertTrue(controller.back())

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(sourceProfileStack, profileController.backStack.toList())
        assertEquals(previousContactsStack, contactsController.backStack.toList())
    }

    @Test
    fun crossSectionPushForResultCancelsAndRestoresCallerOnBack() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )
        val contactsController = controllers.getValue(sections.items.first())
        val profileController = controllers.getValue(sections.items.last())
        val sourceProfileStack = profileController.backStack.toList()
        val previousContactsStack = contactsController.backStack.toList()
        val results = mutableListOf<TestRouteResult>()
        var cancelCount = 0

        controller.pushForResult(
            launch = LaydrNavLaunch(
                destination = TestDestination(graph.detail.key(mapOf("id" to "alpha"))),
            ),
            onCancel = { cancelCount += 1 },
            resultType = TestRouteResult::class,
        ) { result ->
            results += result
        }

        assertSame(sections.items.first(), controller.selectedSection)
        assertTrue(controller.canReturn)

        assertTrue(controller.back())

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(sourceProfileStack, profileController.backStack.toList())
        assertEquals(previousContactsStack, contactsController.backStack.toList())
        assertEquals(emptyList(), results)
        assertEquals(1, cancelCount)
        assertEquals(0, controller.entryStore.resultCount)
    }

    @Test
    fun sameSectionPushForResultUsesOrdinaryStackBack() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )
        val contactsController = controllers.getValue(sections.items.first())
        var cancelCount = 0

        controller.pushForResult(
            launch = LaydrNavLaunch(
                destination = TestDestination(graph.detail.key(mapOf("id" to "alpha"))),
            ),
            onCancel = { cancelCount += 1 },
            resultType = TestRouteResult::class,
        ) { _ -> }

        assertSame(sections.items.first(), controller.selectedSection)
        assertTrue(controller.canGoBack)
        assertFalse(controller.canReturn)
        assertEquals(
            graph.detail.key(mapOf("id" to "alpha")),
            (contactsController.backStack.last() as LaydrNavKey).toLaydrRouteKey(),
        )

        assertTrue(controller.back())

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(listOf(graph.contacts.key().navKey()), contactsController.backStack.toList())
        assertEquals(1, cancelCount)
        assertEquals(0, controller.entryStore.resultCount)
    }

    @Test
    fun rejectsUnownedOrInvalidDestinationsBeforeMutation() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )
        val contactsStack = controllers.getValue(sections.items.first()).backStack.toList()
        val profileStack = controllers.getValue(sections.items.last()).backStack.toList()

        assertFailsWith<IllegalArgumentException> {
            controller.push(TestDestination(graph.about.key()))
        }
        assertFailsWith<IllegalArgumentException> {
            controller.replace(TestDestination(LaydrRouteKey(routeId = "Contacts.ById")))
        }

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(contactsStack, controllers.getValue(sections.items.first()).backStack.toList())
        assertEquals(profileStack, controllers.getValue(sections.items.last()).backStack.toList())
    }

    @Test
    fun rejectsInvalidOrUnownedPathsBeforeMutation() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )
        val contactsStack = controllers.getValue(sections.items.first()).backStack.toList()
        val profileStack = controllers.getValue(sections.items.last()).backStack.toList()

        assertRejected(controller.selectPath("/about"), LaydrNavPathRejectionReason.OutsideDeclaredSection)
        assertRejected(controller.pushPath("/about"), LaydrNavPathRejectionReason.OutsideDeclaredSection)
        assertRejected(controller.replacePath("/settings"), LaydrNavPathRejectionReason.LayoutOnlyRoute)
        assertRejected(
            controller.pushPath("/contacts/alpha/"),
            LaydrNavPathRejectionReason.UnsupportedPath,
        )
        assertRejected(controller.replacePath("/missing"), LaydrNavPathRejectionReason.UnknownRoute)

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(contactsStack, controllers.getValue(sections.items.first()).backStack.toList())
        assertEquals(profileStack, controllers.getValue(sections.items.last()).backStack.toList())
    }

    @Test
    fun popsTheSelectedSectionWithoutRemovingTheRoot() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )

        controller.push(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))

        assertTrue(controller.popSelectedStack())
        assertEquals(
            listOf(graph.contacts.key().navKey()),
            controllers.getValue(sections.items.first()).backStack.toList(),
        )
        assertFalse(controller.popSelectedStack())
        assertEquals(
            listOf(graph.contacts.key().navKey()),
            controllers.getValue(sections.items.first()).backStack.toList(),
        )
    }

    @Test
    fun appBackFallsBackToSelectedSectionPop() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )

        assertFalse(controller.canGoBack)

        controller.push(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))

        assertTrue(controller.canGoBack)
        assertFalse(controller.canReturn)
        assertTrue(controller.back())
        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(graph.contacts.key().navKey()),
            controllers.getValue(sections.items.first()).backStack.toList(),
        )
        assertFalse(controller.back())
    }

    @Test
    fun pushWithReturnRestoresSourceAndTargetStacks() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )
        val contactsController = controllers.getValue(sections.items.first())
        val profileController = controllers.getValue(sections.items.last())

        controller.push(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))
        controller.select(sections.items.last())
        val sourceProfileStack = profileController.backStack.toList()
        val previousContactsStack = contactsController.backStack.toList()

        controller.pushWithReturn(TestDestination(graph.detail.key(mapOf("id" to "bravo"))))

        assertSame(sections.items.first(), controller.selectedSection)
        assertTrue(controller.canGoBack)
        assertTrue(controller.canReturn)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            contactsController.backStack.toList(),
        )

        assertTrue(controller.back())

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(sourceProfileStack, profileController.backStack.toList())
        assertEquals(previousContactsStack, contactsController.backStack.toList())
    }

    @Test
    fun savedReturnHistoryRestoresReturnAwareBack() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val returnHistory = mutableStateListOf<LaydrNavReturnEntry>()
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
            returnHistory = returnHistory,
        )
        val contactsController = controllers.getValue(sections.items.first())
        val profileController = controllers.getValue(sections.items.last())
        val sourceProfileStack = profileController.backStack.toList()
        val previousContactsStack = contactsController.backStack.toList()

        controller.pushWithReturn(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))

        val savedHistory = saveLaydrNavReturnHistory(returnHistory)
        val restoredHistory = restoreLaydrNavReturnHistory(savedHistory)
            ?: error("Return history should restore from saved values")
        val restoredController = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            selectedSectionState = mutableStateOf(sections.items.first().stateId),
            sectionControllers = controllers,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
            returnHistory = restoredHistory,
        )

        assertSame(sections.items.first(), restoredController.selectedSection)
        assertTrue(restoredController.canReturn)
        assertTrue(restoredController.canGoBack)

        assertTrue(restoredController.back())

        assertSame(sections.items.last(), restoredController.selectedSection)
        assertEquals(sourceProfileStack, profileController.backStack.toList())
        assertEquals(previousContactsStack, contactsController.backStack.toList())
        assertFalse(restoredController.canReturn)
    }

    @Test
    fun replaceWithReturnRestoresSameSectionStack() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )
        val contactsController = controllers.getValue(sections.items.first())

        controller.push(TestDestination(graph.edit.key(mapOf("id" to "alpha"))))
        val previousContactsStack = contactsController.backStack.toList()

        controller.replaceWithReturn(TestDestination(graph.detail.key(mapOf("id" to "bravo"))))

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            contactsController.backStack.toList(),
        )

        assertTrue(controller.back())

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(previousContactsStack, contactsController.backStack.toList())
    }

    @Test
    fun explicitSectionSelectionClearsReturnHistory() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )
        val contactsController = controllers.getValue(sections.items.first())

        controller.pushWithReturn(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))
        controller.select(sections.items.first())

        assertTrue(controller.back())

        assertSame(sections.items.first(), controller.selectedSection)
        assertEquals(
            listOf(graph.contacts.key().navKey()),
            contactsController.backStack.toList(),
        )
    }

    @Test
    fun ordinaryCrossSectionNavigationClearsReturnHistory() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(
            graph = graph,
            sections = sections,
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )

        controller.pushWithReturn(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))
        controller.replace(TestDestination(graph.profile.key()))

        assertSame(sections.items.last(), controller.selectedSection)
        assertFalse(controller.canGoBack)
        assertFalse(controller.back())
    }

    @Test
    fun invalidReturnAwareDestinationDoesNotMutateState() {
        val graph = testGraph()
        val sections = testSections(graph)
        val controllers = controllersFor(graph, sections)
        val controller = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.last(),
            sectionControllers = controllers,
        )
        val contactsStack = controllers.getValue(sections.items.first()).backStack.toList()
        val profileStack = controllers.getValue(sections.items.last()).backStack.toList()

        assertFailsWith<IllegalArgumentException> {
            controller.pushWithReturn(TestDestination(graph.about.key()))
        }
        assertFailsWith<IllegalArgumentException> {
            controller.replaceWithReturn(TestDestination(LaydrRouteKey(routeId = "Contacts.ById")))
        }

        assertSame(sections.items.last(), controller.selectedSection)
        assertEquals(contactsStack, controllers.getValue(sections.items.first()).backStack.toList())
        assertEquals(profileStack, controllers.getValue(sections.items.last()).backStack.toList())
        assertFalse(controller.canGoBack)
    }

    private fun testSections(graph: TestGraph): LaydrNavSectionSet<Unit> =
        LaydrNavSectionSetBuilder(graph.appGraph)
            .apply {
                section(screenRef(graph.contacts), TestDestination(graph.contacts.key()))
                section(screenRef(graph.profile), TestDestination(graph.profile.key()))
            }
            .build()

    private fun typedSections(
        graph: TestGraph,
        contactsData: String,
        profileData: String,
    ): LaydrNavSectionSet<String> =
        laydrNavSectionSet(
            graph.appGraph,
            laydrNavSection(parameterlessScreenRef(graph.contacts), contactsData),
            laydrNavSection(parameterlessScreenRef(graph.profile), profileData),
        )

    private fun controllersFor(
        graph: TestGraph,
        sections: LaydrNavSectionSet<Unit>,
        sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    ): Map<LaydrNavSection<Unit>, LaydrNavStackCoordinator> =
        mapOf(
            sections.items[0] to LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(graph.contacts.key().navKey()),
                sceneSupport = sceneSupport,
            ),
            sections.items[1] to LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(graph.profile.key().navKey()),
                sceneSupport = sceneSupport,
            ),
        )

    private fun controllersForDynamicSections(
        graph: TestGraph,
        sections: LaydrNavSectionSet<Unit>,
    ): Map<LaydrNavSection<Unit>, LaydrNavStackCoordinator> =
        mapOf(
            sections.items[0] to LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(graph.detail.key(mapOf("id" to "alpha")).navKey()),
            ),
            sections.items[1] to LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(graph.detail.key(mapOf("id" to "bravo")).navKey()),
            ),
        )

    private fun testNavSections(
        sections: LaydrNavSectionSet<Unit>,
        controller: LaydrNavSectionsCoordinator<Unit>,
    ): LaydrNavSections<Unit> =
        LaydrNavSections(
            sectionSet = sections,
            coordinator = controller,
            entryProvider = { key -> NavEntry(key) {} },
        )

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
        val about = route(id = "About", segments = listOf(static("about")))
        val settings = route(id = "Settings", segments = listOf(static("settings")))
        val routeMap = LaydrRouteMap(
            routes = listOf(contacts, profile, about, settings),
            screenRoutes = listOf(contacts, detail, edit, profile, about),
            layoutRoutes = listOf(settings),
        )
        return TestGraph(
            appGraph = LaydrAppGraph(routeMap),
            contacts = contacts,
            detail = detail,
            edit = edit,
            profile = profile,
            about = about,
            settings = settings,
        )
    }

    private data class TestGraph(
        val appGraph: LaydrAppGraph,
        val contacts: LaydrRoute,
        val detail: LaydrRoute,
        val edit: LaydrRoute,
        val profile: LaydrRoute,
        val about: LaydrRoute,
        val settings: LaydrRoute,
    )

    private fun screenRef(route: LaydrRoute): LaydrScreenRouteRef =
        object : LaydrScreenRouteRef {
            override val route: LaydrRoute = route
        }

    private fun parameterlessScreenRef(route: LaydrRoute): LaydrParameterlessScreenRouteRef =
        object : LaydrParameterlessScreenRouteRef {
            override val route: LaydrRoute = route
            override val defaultDestination: LaydrScreenDestination = TestDestination(route.key())
        }

    private data class TestDestination(
        override val routeKey: LaydrRouteKey,
    ) : LaydrScreenDestination

    private data class TestRouteResult(val value: String)
}
