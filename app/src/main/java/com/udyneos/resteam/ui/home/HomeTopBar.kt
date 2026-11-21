package com.udyneos.resteam.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.udyneos.resteam.domain.model.LauncherTab
import com.udyneos.resteam.domain.model.ShizukuState
import com.udyneos.resteam.ui.components.FilterPill
import com.udyneos.resteam.ui.components.indicatorColor
import com.udyneos.resteam.ui.theme.LauncherColors

@Composable
fun HomeTopBar(
    userName: String,
    selectedTab: LauncherTab,
    onTabSelected: (LauncherTab) -> Unit,
    shizukuState: ShizukuState,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val greeting: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            Text("Hi, $userName!", style = MaterialTheme.typography.titleMedium, color = LauncherColors.TextPrimary)
            Text("Here's your last progress", style = MaterialTheme.typography.bodySmall, color = LauncherColors.TextSecondary)
        }
    }
    val tabs: @Composable (Modifier) -> Unit = { m ->
        LazyRow(m, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LauncherTab.entries) { tab ->
                FilterPill(tab.label, selected = tab == selectedTab, onClick = { onTabSelected(tab) })
            }
        }
    }

    if (isCompact) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                greeting(Modifier.weight(1f))
                ProfileAvatar(userName, shizukuState)
            }
            tabs(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            greeting(Modifier)
            tabs(Modifier.weight(1f))
            ProfileAvatar(userName, shizukuState)
        }
    }
}

@Composable
private fun ProfileAvatar(userName: String, shizukuState: ShizukuState) {
    Box(Modifier.size(44.dp)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(LauncherColors.Accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(userName.take(1).uppercase(), color = LauncherColors.Accent, style = MaterialTheme.typography.titleMedium)
        }
        // Badge status Shizuku
        Box(
            Modifier
                .size(14.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(shizukuState.indicatorColor())
                .border(2.dp, LauncherColors.Canvas, CircleShape),
        )
    }
}
