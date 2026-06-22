package dev.goquick.laydr.examples.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.goquick.laydr.compose.LaydrRouteHost
import dev.goquick.laydr.examples.basic.generated.LaydrComposeRoutes
import dev.goquick.laydr.examples.basic.generated.LaydrRoutes

internal class ComposeBasicContext(
    val repository: InMemoryAddressBookRepository,
    val navigate: (String) -> Unit,
)

internal val LocalComposeBasicContext = staticCompositionLocalOf<ComposeBasicContext> {
    error("ComposeBasicContext was not provided")
}

@Composable
fun ExampleApp() {
    val repository = remember { InMemoryAddressBookRepository(seedContacts()) }
    var currentPath by remember { mutableStateOf(LaydrRoutes.Contacts.path()) }
    val navigate: (String) -> Unit = { path -> currentPath = path }
    val appContext = remember(repository, navigate) {
        ComposeBasicContext(
            repository = repository,
            navigate = navigate,
        )
    }

    CompositionLocalProvider(LocalComposeBasicContext provides appContext) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground),
        ) {
            AppHeader(currentPath = currentPath)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                LaydrRouteHost(
                    currentPath = currentPath,
                    routeDefinitions = LaydrComposeRoutes.definitions,
                    notFoundContent = { path ->
                        NotFoundContent(path = path)
                    },
                )
            }
        }
    }
}

@Composable
private fun AppHeader(currentPath: String) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
            .border(width = 1.dp, color = border)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (maxWidth < 520.dp) {
            Column {
                AppTitle()
                Spacer(modifier = Modifier.height(8.dp))
                PathText(currentPath = currentPath)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTitle(modifier = Modifier.weight(1f))
                PathText(currentPath = currentPath)
            }
        }
    }
}

@Composable
private fun AppTitle(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        BasicText(
            text = "Laydr Address Book",
            style = TextStyle(
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = "Generated routes, inherited layouts, app-owned navigation",
            style = TextStyle(color = textMuted, fontSize = 13.sp),
        )
    }
}

@Composable
private fun PathText(currentPath: String) {
    BasicText(
        text = currentPath,
        style = TextStyle(color = accent, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
private fun NotFoundContent(path: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(24.dp),
    ) {
        BasicText(
            text = "Route not found",
            style = TextStyle(color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = path,
            style = TextStyle(color = textMuted, fontSize = 14.sp),
        )
    }
}
