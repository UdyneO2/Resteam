package com.udyneos.resteam.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udyneos.resteam.domain.model.LogEntry
import com.udyneos.resteam.ui.components.LauncherCard
import com.udyneos.resteam.ui.components.StatusDot
import com.udyneos.resteam.ui.theme.LauncherColors

/** Konten tab "Logs": riwayat aktivitas sesi ini. */
@Composable
fun LogsTab(
    modifier: Modifier = Modifier,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Activity log",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                color = LauncherColors.TextPrimary,
            )
            TextButton(onClick = viewModel::onClear, enabled = entries.isNotEmpty()) { Text("Bersihkan") }
        }
        if (entries.isEmpty()) {
            LauncherCard(Modifier.fillMaxWidth()) {
                Text(
                    "Belum ada aktivitas. Hasil penerapan preset akan muncul di sini selama aplikasi berjalan.",
                    color = LauncherColors.TextSecondary,
                )
            }
        } else {
            entries.forEach { LogCard(it) }
        }
    }
}

@Composable
private fun LogCard(entry: LogEntry) {
    LauncherCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusDot(if (entry.isError) LauncherColors.Error else LauncherColors.Success)
            Text(
                entry.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = LauncherColors.TextPrimary,
            )
            Text(entry.formattedTime(), style = MaterialTheme.typography.bodySmall, color = LauncherColors.TextSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            entry.detail,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = LauncherColors.TextSecondary,
        )
    }
}
