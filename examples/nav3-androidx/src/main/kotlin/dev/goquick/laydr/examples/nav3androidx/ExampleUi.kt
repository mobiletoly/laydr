package dev.goquick.laydr.examples.nav3androidx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val pageBackground = Color(0xFFF4F6F8)
internal val surface = Color(0xFFFFFFFF)
internal val surfaceMuted = Color(0xFFF8FAFC)
internal val selectedBackground = Color(0xFFEAF2FF)
internal val border = Color(0xFFD9DEE7)
internal val accent = Color(0xFF2457A6)
internal val textPrimary = Color(0xFF1F2933)
internal val textMuted = Color(0xFF667085)

@Composable
internal fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val background = if (primary) accent else surface
    val foreground = if (primary) Color.White else accent
    val outline = if (primary) accent else border

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
internal fun ContactRow(
    contact: Contact,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) selectedBackground else surface
    val outline = if (selected) accent else border

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(width = 1.dp, color = outline, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        BasicText(
            text = contact.name,
            style = TextStyle(
                color = if (selected) accent else textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(3.dp))
        BasicText(
            text = contact.role,
            style = TextStyle(color = textMuted, fontSize = 12.sp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicText(
            text = contact.email,
            style = TextStyle(color = textMuted, fontSize = 12.sp),
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
internal fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BasicText(
            text = label,
            style = TextStyle(color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = textPrimary, fontSize = 15.sp),
            modifier = Modifier
                .fillMaxWidth()
                .background(surface, RoundedCornerShape(6.dp))
                .border(width = 1.dp, color = border, shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
