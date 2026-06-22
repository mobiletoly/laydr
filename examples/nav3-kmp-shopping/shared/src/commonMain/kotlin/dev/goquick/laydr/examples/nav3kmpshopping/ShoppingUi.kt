package dev.goquick.laydr.examples.nav3kmpshopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val pageBackground = Color(0xFFF4F7F6)
internal val surface = Color(0xFFFFFFFF)
internal val surfaceMuted = Color(0xFFF7FAF9)
internal val selectedBackground = Color(0xFFE7F4EE)
internal val border = Color(0xFFD8E1DD)
internal val accent = Color(0xFF0B6B58)
internal val warning = Color(0xFF9A5A00)
internal val textPrimary = Color(0xFF1F2A29)
internal val textMuted = Color(0xFF62706B)

@Composable
internal fun ShoppingButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val background = when {
        !enabled -> surfaceMuted
        primary -> accent
        else -> surface
    }
    val foreground = when {
        !enabled -> textMuted
        primary -> Color.White
        else -> accent
    }
    val outline = if (primary && enabled) accent else border

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(width = 1.dp, color = outline, shape = RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
internal fun RoutePage(
    title: String,
    subtitle: String,
    routePath: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            text = title,
            style = TextStyle(color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        )
        BasicText(
            text = subtitle,
            style = TextStyle(color = textMuted, fontSize = 13.sp),
        )
        BasicText(
            text = routePath,
            style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = Modifier.height(2.dp))
        content()
    }
}

@Composable
internal fun SectionTitle(
    label: String,
) {
    BasicText(
        text = label,
        style = TextStyle(color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
internal fun ItemCard(
    title: String,
    subtitle: String,
    meta: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val background = if (selected) selectedBackground else surfaceMuted
    val outline = if (selected) accent else border
    var cardModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(background)
        .border(width = 1.dp, color = outline, shape = RoundedCornerShape(8.dp))
    if (onClick != null) {
        cardModifier = cardModifier.clickable(onClick = onClick)
    }

    Column(
        modifier = cardModifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        BasicText(
            text = title,
            style = TextStyle(color = if (selected) accent else textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        )
        BasicText(
            text = subtitle,
            style = TextStyle(color = textMuted, fontSize = 13.sp),
        )
        BasicText(
            text = meta,
            style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
internal fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BasicText(
            text = label,
            style = TextStyle(color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = value,
            style = TextStyle(color = textPrimary, fontSize = 15.sp),
        )
    }
}

@Composable
internal fun ActionRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
internal fun MissingRouteState(
    title: String,
    message: String,
    routePath: String,
) {
    RoutePage(
        title = title,
        subtitle = message,
        routePath = routePath,
    ) {
        ItemCard(
            title = "No matching example data",
            subtitle = "The generated route matched, but this fake store has no item for the id.",
            meta = "Route-local fallback",
        )
    }
}

@Composable
internal fun EmptyAdaptiveDetail(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceMuted, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = title,
            style = TextStyle(color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = message,
            style = TextStyle(color = textMuted, fontSize = 14.sp),
        )
    }
}
