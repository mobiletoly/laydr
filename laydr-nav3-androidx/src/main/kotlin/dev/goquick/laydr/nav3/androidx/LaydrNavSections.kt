// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.androidx

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

/**
 * Complete sectioned AndroidX Nav3 runtime object returned by Laydr section
 * helpers.
 */
public class LaydrNavSections<Data : Any> internal constructor(
    /**
     * Static section set used for destination validation and membership.
     */
    public val sectionSet: LaydrNavSectionSet<Data>,
    internal val coordinator: LaydrNavSectionsCoordinator<Data>,
    /**
     * AndroidX Nav3 entry provider backed by generated route-owned definitions.
     */
    public val entryProvider: (NavKey) -> NavEntry<NavKey>,
) {
    /**
     * Route-facing navigator for declared sections.
     */
    public val navigator: LaydrNavSectionsNavigator = coordinator

    internal val entryStore: LaydrNavEntryStore
        get() = coordinator.entryStore

    /**
     * Ordered section entries in app declaration order.
     */
    public val items: List<LaydrNavSection<Data>>
        get() = sectionSet.items

    /**
     * Currently selected section.
     */
    public val selectedSection: LaydrNavSection<Data>
        get() = coordinator.selectedSection

    /**
     * Back stack for the currently selected section.
     */
    public val selectedBackStack: NavBackStack<NavKey>
        get() = coordinator.selectedBackStack

    /**
     * Current generated path for the logical top of the selected stack, or
     * `null`.
     */
    public val currentPath: String?
        get() = coordinator.currentPath

    /**
     * True when the selected section stack contains more than one entry.
     */
    public val canPopSelectedStack: Boolean
        get() = coordinator.canPopSelectedStack

    /**
     * True when app Back can restore a return-aware navigation point or pop the
     * selected section stack.
     */
    public val canGoBack: Boolean
        get() = coordinator.canGoBack

    /**
     * True when app Back can restore a return-aware navigation point before
     * falling back to selected-section stack popping.
     */
    public val canReturn: Boolean
        get() = coordinator.canReturn

    /**
     * Scene support used by this section runtime.
     */
    public val sceneSupport: LaydrNavSceneSupport
        get() = coordinator.sceneSupport

    /**
     * Returns whether app Back should be shown for the current stack.
     */
    public fun canShowBack(showingWideListDetail: Boolean): Boolean =
        canReturn || (canPopSelectedStack && !showingWideListDetail)

    /**
     * Selects [section] without mutating section stacks.
     */
    public fun select(section: LaydrNavSection<Data>) {
        coordinator.select(section)
    }

    /**
     * Selects the section that owns [path].
     */
    public fun selectPath(path: String): LaydrNavPathResult =
        coordinator.selectPath(path)

    /**
     * Selects the section that owns URL-like [input].
     */
    public fun selectExternalTarget(input: String): LaydrNavExternalTargetResult =
        coordinator.selectExternalTarget(input)

    /**
     * Pushes the screen route represented by [path].
     */
    public fun pushPath(path: String): LaydrNavPathResult =
        coordinator.pushPath(path)

    /**
     * Pushes the screen route represented by URL-like [input].
     */
    public fun pushExternalTarget(input: String): LaydrNavExternalTargetResult =
        coordinator.pushExternalTarget(input)

    /**
     * Replaces the current entry with the screen route represented by [path].
     */
    public fun replacePath(path: String): LaydrNavPathResult =
        coordinator.replacePath(path)

    /**
     * Replaces the current entry with the screen route represented by URL-like
     * [input].
     */
    public fun replaceExternalTarget(input: String): LaydrNavExternalTargetResult =
        coordinator.replaceExternalTarget(input)

    /**
     * Performs user-facing app Back.
     */
    public fun back(): Boolean =
        coordinator.back()

    /**
     * Pops the selected section stack without restoring return-aware navigation
     * points or removing the section root.
     */
    public fun popSelectedStack(): Boolean =
        coordinator.popSelectedStack()
}
