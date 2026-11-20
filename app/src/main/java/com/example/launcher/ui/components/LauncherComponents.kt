package com.example.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.launcher.ui.theme.LauncherColors
import com.example.launcher.ui.theme.LauncherShapes

@Composable
fun LauncherCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = LauncherShapes.Card,
        colors = CardDefaults.cardColors(containerColor = LauncherColors.Surface),
        border = BorderStroke(1.dp, LauncherColors.Border),
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Int = 8) {
    Box(modifier.size(size.dp).clip(CircleShape).background(color))
}

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leading?.invoke()
            Text(text, style = MaterialTheme.typography.labelLarge, color = LauncherColors.TextPrimary)
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = LauncherShapes.Pill, color = LauncherColors.PillOverlay, content = content)
    } else {
        Surface(modifier = modifier, shape = LauncherShapes.Pill, color = LauncherColors.PillOverlay, content = content)
    }
}

@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = LauncherShapes.Pill,
        color = if (selected) LauncherColors.Border else LauncherColors.Canvas,
        border = BorderStroke(1.dp, LauncherColors.Border),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) LauncherColors.TextPrimary else LauncherColors.TextSecondary,
        )
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = LauncherColors.ControlSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = LauncherColors.TextPrimary)
        }
    }
}

@Composable
fun launcherTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LauncherColors.Accent,
    unfocusedBorderColor = LauncherColors.Border,
    focusedTextColor = LauncherColors.TextPrimary,
    unfocusedTextColor = LauncherColors.TextPrimary,
    cursorColor = LauncherColors.Accent,
    focusedLabelColor = LauncherColors.Accent,
    unfocusedLabelColor = LauncherColors.TextSecondary,
)
