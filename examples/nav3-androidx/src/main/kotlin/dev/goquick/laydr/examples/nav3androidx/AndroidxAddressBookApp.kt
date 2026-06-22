package dev.goquick.laydr.examples.nav3androidx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.ui.NavDisplay
import dev.goquick.laydr.examples.nav3androidx.generated.LaydrNavRoutes
import dev.goquick.laydr.nav3.androidx.LaydrNavNotFound
import dev.goquick.laydr.nav3.androidx.LaydrNavSection
import dev.goquick.laydr.nav3.androidx.LaydrNavSectionsNavigator

internal class AddressBookContext(
    val repository: InMemoryAddressBookRepository,
    val navigator: LaydrNavSectionsNavigator,
)

internal val LocalAddressBookContext = staticCompositionLocalOf<AddressBookContext> {
    error("AddressBookContext was not provided")
}

private data class AddressBookTab(
    val label: String,
)

/**
 * Renders the Android-only AndroidX Nav3 Address Book using generated Laydr
 * route contracts.
 */
@Composable
fun AndroidxAddressBookApp() {
    val repository = remember { InMemoryAddressBookRepository(seedContacts()) }
    val wiring = LaydrNavRoutes.rememberSections(
        sectionSpecs = listOf(
            LaydrNavRoutes.Contacts.section(AddressBookTab(label = "Contacts")),
            LaydrNavRoutes.Profile.section(AddressBookTab(label = "Profile")),
        ),
        notFoundContent = { notFound ->
            NotFoundContent(notFound = notFound)
        },
    )

    val appContext = AddressBookContext(
        repository = repository,
        navigator = wiring.navigator,
    )

    CompositionLocalProvider(LocalAddressBookContext provides appContext) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            AppHeader(
                canGoBack = wiring.canShowBack(showingWideListDetail = false),
                onBack = {
                    wiring.back()
                },
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                NavDisplay(
                    backStack = wiring.selectedBackStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = {
                        wiring.back()
                    },
                    entryProvider = wiring.entryProvider,
                )
            }

            BottomTabBar(
                sections = wiring.sectionSet.items,
                selectedSection = wiring.selectedSection,
                onSelect = wiring::select,
            )
        }
    }
}

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
private fun AppTitle(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        BasicText(
            text = "Laydr AndroidX Nav3",
            style = TextStyle(
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = "Android-only routes, generated helpers, sectioned stacks",
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
        AppButton(
            label = "Back",
            onClick = onBack,
            enabled = visible,
        )
    }
}

@Composable
private fun BottomTabBar(
    sections: List<LaydrNavSection<AddressBookTab>>,
    selectedSection: LaydrNavSection<AddressBookTab>,
    onSelect: (LaydrNavSection<AddressBookTab>) -> Unit,
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
private fun NotFoundContent(notFound: LaydrNavNotFound) {
    val display = notFound.displayPath
        ?: notFound.laydrKey?.toString()
        ?: notFound.key.toString()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        BasicText(
            text = "Route not found",
            style = TextStyle(color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = "${notFound.reason}: $display",
            style = TextStyle(color = textMuted, fontSize = 14.sp),
        )
    }
}
