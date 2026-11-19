package com.example.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.launcher.domain.model.ActivityLevel
import com.example.launcher.ui.components.LauncherCard
import com.example.launcher.ui.theme.LauncherColors
import com.example.launcher.ui.theme.LauncherShapes

private val DayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun ActivityLevel.color(): Color = when (this) {
    ActivityLevel.None -> LauncherColors.HeatNone
    ActivityLevel.Low -> LauncherColors.HeatLow
    ActivityLevel.Medium -> LauncherColors.HeatMedium
    ActivityLevel.High -> LauncherColors.Accent
}

@Composable
fun ActivityCard(
    title: String,
    activity: List<Float?>,
    modifier: Modifier = Modifier,
) {
    LauncherCard(modifier) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = LauncherColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        Legend()
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            activity.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { hours -> HeatCell(hours, Modifier.weight(1f)) }
                    // Lengkapi baris terakhir agar lebar sel konsisten
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            DayLabels.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun HeatCell(hours: Float?, modifier: Modifier = Modifier) {
    val base = modifier.aspectRatio(1f).clip(LauncherShapes.Cell)
    if (hours == null) {
        Spacer(base)
    } else {
        Box(base.background(ActivityLevel.fromHours(hours).color()))
    }
}

@Composable
private fun Legend() {
    val items = listOf("0h" to ActivityLevel.None, ">2h" to ActivityLevel.Low, ">4h" to ActivityLevel.Medium, ">8h" to ActivityLevel.High)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (label, level) ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(LauncherShapes.Pill).background(level.color()))
                Text(label, style = MaterialTheme.typography.bodySmall, color = LauncherColors.TextSecondary)
            }
        }
    }
}
