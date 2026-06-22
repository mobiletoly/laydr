package routes.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.goquick.laydr.examples.nav3androidx.DetailRow
import dev.goquick.laydr.examples.nav3androidx.accent
import dev.goquick.laydr.examples.nav3androidx.border
import dev.goquick.laydr.examples.nav3androidx.generated.LaydrRoutes
import dev.goquick.laydr.examples.nav3androidx.selectedBackground
import dev.goquick.laydr.examples.nav3androidx.surface
import dev.goquick.laydr.examples.nav3androidx.surfaceMuted
import dev.goquick.laydr.examples.nav3androidx.textMuted
import dev.goquick.laydr.examples.nav3androidx.textPrimary

@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.Destination,
) {
    ProfileContent(routePath = route.path)
}

@Composable
private fun ProfileContent(routePath: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar()
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = "Morgan Lee",
                    style = TextStyle(color = textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicText(
                    text = "Product Operations Lead",
                    style = TextStyle(color = textMuted, fontSize = 14.sp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                BasicText(
                    text = routePath,
                    style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
            }
        }

        ProfileSection(title = "Workspace") {
            DetailRow(label = "Team", value = "Customer Success")
            DetailRow(label = "Location", value = "Remote")
            DetailRow(label = "Timezone", value = "Atlantic/Canary")
        }

        ProfileSection(title = "Navigation Preferences") {
            DetailRow(label = "Default tab", value = "Contacts")
            DetailRow(label = "Saved view", value = "AndroidX address book")
            DetailRow(label = "Route source", value = "src/main/kotlin/routes/profile")
        }
    }
}

@Composable
private fun ProfileAvatar() {
    Row(
        modifier = Modifier
            .background(selectedBackground, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = accent, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        BasicText(
            text = "ML",
            style = TextStyle(color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceMuted, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            text = title,
            style = TextStyle(color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        )
        content()
    }
}
