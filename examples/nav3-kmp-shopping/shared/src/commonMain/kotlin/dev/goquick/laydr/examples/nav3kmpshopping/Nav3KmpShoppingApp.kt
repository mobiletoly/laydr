package dev.goquick.laydr.examples.nav3kmpshopping

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrNavRoutes
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import dev.goquick.laydr.nav3.kmp.LaydrNavListDetailStackShape
import dev.goquick.laydr.nav3.kmp.LaydrNavSectionsNavigator
import dev.goquick.laydr.nav3.kmp.LaydrNavNotFound
import dev.goquick.laydr.nav3.kmp.LaydrNavSection
import dev.goquick.laydr.nav3.kmp.LaydrNavSections
import dev.goquick.laydr.nav3.kmp.ProvideLaydrNavStackNavigator
import dev.goquick.laydr.nav3.kmp.get
import dev.goquick.laydr.nav3.kmp.laydrNavListDetailScene
import dev.goquick.laydr.nav3.kmp.rememberLaydrNavAdaptiveScenes
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

internal class ShoppingContext(
    val store: ShoppingStore,
    val navigator: LaydrNavSectionsNavigator,
)

private object ShoppingSectionsRootKey : NavKey

private data class ShoppingSectionData(
    val label: String,
)

private val WideShellBreakpoint = 960.dp
private val WideListDetailBreakpoint = 840.dp

/**
 * Renders the Laydr Nav3 KMP shopping example from shared KMP Compose code.
 */
@Composable
fun Nav3KmpShoppingApp() {
    val store = remember { seedShoppingStore() }
    val sceneSupport = rememberLaydrNavAdaptiveScenes(
        LaydrRoutes.appGraph,
        laydrNavListDetailScene(
            list = LaydrRoutes.Shop,
            detail = LaydrRoutes.Shop.Products.ByProductId,
            detailPlaceholder = {
                EmptyAdaptiveDetail(
                    title = "Select a product",
                    message = "Product details will appear here on wide screens.",
                )
            },
        ),
        laydrNavListDetailScene(
            list = LaydrRoutes.Orders,
            detail = LaydrRoutes.Orders.ByOrderId,
            detailPlaceholder = {
                EmptyAdaptiveDetail(
                    title = "Select an order",
                    message = "Order details will appear here on wide screens.",
                )
            },
        ),
    )
    val wiring = LaydrNavRoutes.rememberSections(
        sectionSpecs = listOf(
            LaydrNavRoutes.Shop.section(ShoppingSectionData(label = "Shop")),
            LaydrNavRoutes.Search.section(ShoppingSectionData(label = "Search")),
            LaydrNavRoutes.Cart.section(ShoppingSectionData(label = "Cart")),
            LaydrNavRoutes.Orders.section(ShoppingSectionData(label = "Orders")),
            LaydrNavRoutes.Profile.section(ShoppingSectionData(label = "Profile")),
        ),
        sceneSupport = sceneSupport,
        notFoundContent = { notFound ->
            NotFoundContent(notFound = notFound)
        },
    )
    val rootBackStack = remember {
        NavBackStack<NavKey>(ShoppingSectionsRootKey)
    }
    val rootStack = LaydrNavRoutes.rememberStack(
        backStack = rootBackStack,
        notFoundContent = { notFound ->
            NotFoundContent(notFound = notFound)
        },
    )
    val shoppingModule = remember(store, wiring) {
        module {
            single {
                ShoppingContext(
                    store = store,
                    navigator = wiring.navigator,
                )
            }
        }
    }

    KoinApplication(
        configuration = koinConfiguration {
            modules(shoppingModule)
        },
    ) {
        NavDisplay(
            backStack = rootStack.backStack,
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground),
            onBack = {
                if (!rootStack.navigator.back()) {
                    wiring.back()
                }
            },
            sceneStrategies = rootStack.sceneStrategies,
            transitionSpec = { instantNavigationTransition() },
            popTransitionSpec = { instantNavigationTransition() },
            predictivePopTransitionSpec = { _ -> instantNavigationTransition() },
            entryProvider = { key ->
                when (key) {
                    ShoppingSectionsRootKey ->
                        NavEntry(key = key) {
                            ProvideLaydrNavStackNavigator(rootStack.navigator) {
                                ShoppingSectionsSurface(wiring = wiring)
                            }
                        }
                    else -> presentRootEntry(rootStack.entryProvider(key))
                }
            },
        )
    }
}

@Composable
private fun ShoppingSectionsSurface(
    wiring: LaydrNavSections<ShoppingSectionData>,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val wideShell = maxWidth >= WideShellBreakpoint
        val selectedListDetailState = wiring.listDetailStackState
        val showingWideDetail =
            maxWidth >= WideListDetailBreakpoint &&
                selectedListDetailState?.shape == LaydrNavListDetailStackShape.ListAndDetail
        val canGoBack = wiring.canShowBack(showingWideListDetail = showingWideDetail)

        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                canGoBack = canGoBack,
                onBack = { wiring.back() },
            )

            if (wideShell) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    WideSectionRail(
                        sections = wiring.sectionSet.items,
                        selectedSection = wiring.selectedSection,
                        onSelect = wiring::select,
                    )
                    NavDisplay(
                        backStack = wiring.selectedBackStack,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onBack = { wiring.back() },
                        sceneStrategies = wiring.sceneSupport.sceneStrategies,
                        transitionSpec = { instantNavigationTransition() },
                        popTransitionSpec = { instantNavigationTransition() },
                        predictivePopTransitionSpec = { _ -> instantNavigationTransition() },
                        entryProvider = wiring.entryProvider,
                    )
                }
            } else {
                NavDisplay(
                    backStack = wiring.selectedBackStack,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(18.dp),
                    onBack = { wiring.back() },
                    sceneStrategies = wiring.sceneSupport.sceneStrategies,
                    transitionSpec = { instantNavigationTransition() },
                    popTransitionSpec = { instantNavigationTransition() },
                    predictivePopTransitionSpec = { _ -> instantNavigationTransition() },
                    entryProvider = wiring.entryProvider,
                )

                BottomSectionBar(
                    sections = wiring.sectionSet.items,
                    selectedSection = wiring.selectedSection,
                    onSelect = wiring::select,
                )
            }
        }
    }
}

private fun presentRootEntry(entry: NavEntry<NavKey>): NavEntry<NavKey> {
    val presentation = entry.metadata[ShoppingEntryPresentationKey]
    return when (presentation) {
        ShoppingEntryPresentation.Overlay ->
            NavEntry(navEntry = entry) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBackground)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                    ) {
                        entry.Content()
                    }
                }
            }
        else -> entry
    }
}

private fun instantNavigationTransition() =
    EnterTransition.None togetherWith ExitTransition.None

@Composable
private fun AppHeader(
    canGoBack: Boolean,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
            .border(width = 1.dp, color = border)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTitle(modifier = Modifier.weight(1f))
            HeaderBackAction(onBack = onBack, visible = canGoBack)
        }
    }
}

@Composable
private fun AppTitle(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BasicText(
            text = "Laydr Shop",
            style = TextStyle(
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = "Generated destinations, section stacks, checkout flow, adaptive detail",
            style = TextStyle(color = textMuted, fontSize = 13.sp),
        )
    }
}

@Composable
private fun HeaderBackAction(
    onBack: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val visibilityModifier =
        if (visible) {
            Modifier
        } else {
            Modifier
                .alpha(0f)
                .clearAndSetSemantics {}
        }
    Row(
        modifier = modifier.then(visibilityModifier),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShoppingButton(
            label = "Back",
            onClick = onBack,
            enabled = visible,
        )
    }
}

@Composable
private fun WideSectionRail(
    sections: List<LaydrNavSection<ShoppingSectionData>>,
    selectedSection: LaydrNavSection<ShoppingSectionData>,
    onSelect: (LaydrNavSection<ShoppingSectionData>) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(172.dp)
            .fillMaxHeight()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sections.forEach { section ->
            SectionRailItem(
                label = section.data.label,
                selected = section == selectedSection,
                onClick = { onSelect(section) },
            )
        }
    }
}

@Composable
private fun SectionRailItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) selectedBackground else surface
    val foreground = if (selected) accent else textMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabDot(selected = selected)
        BasicText(
            text = label,
            style = TextStyle(color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun BottomSectionBar(
    sections: List<LaydrNavSection<ShoppingSectionData>>,
    selectedSection: LaydrNavSection<ShoppingSectionData>,
    onSelect: (LaydrNavSection<ShoppingSectionData>) -> Unit,
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = border),
        containerColor = surface,
    ) {
        sections.forEach { section ->
            val selected = section == selectedSection
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(section) },
                icon = {
                    TabDot(selected = selected)
                },
                label = {
                    Text(text = section.data.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    selectedTextColor = accent,
                    indicatorColor = selectedBackground,
                    unselectedIconColor = textMuted,
                    unselectedTextColor = textMuted,
                ),
            )
        }
    }
}

@Composable
private fun TabDot(
    selected: Boolean,
) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                color = if (selected) accent else textMuted,
                shape = CircleShape,
            ),
    )
}

@Composable
private fun NotFoundContent(
    notFound: LaydrNavNotFound,
) {
    val display = notFound.displayPath
        ?: notFound.laydrKey?.toString()
        ?: notFound.key.toString()
    RoutePage(
        title = "Route not found",
        subtitle = "${notFound.reason}: $display",
        routePath = display,
    ) {
        ItemCard(
            title = "Nav3 entry rejected",
            subtitle = "Laydr did not redirect this unknown, stale, invalid, or layout-only key.",
            meta = "Strict not-found content",
        )
    }
}
