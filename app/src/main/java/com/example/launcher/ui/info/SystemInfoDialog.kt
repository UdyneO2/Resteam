package com.example.launcher.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.domain.model.InfoRow
import com.example.launcher.ui.theme.LauncherColors

@Composable
fun SystemInfoDialog(
    packageName: String,
    gameTitle: String,
    onDismiss: () -> Unit,
    viewModel: GameInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(packageName) { viewModel.load(packageName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LauncherColors.Surface,
        title = { Text("System properties", color = LauncherColors.TextPrimary) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoSection("Perangkat", state.device)
                InfoSection(gameTitle, state.app)
                SectionHeader("Game mode (Shizuku)")
                when (val mode = state.gameMode) {
                    GameModeInfo.Loading -> Text("Memuat…", color = LauncherColors.TextSecondary)
                    is GameModeInfo.Loaded -> Text(
                        mode.output,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = LauncherColors.TextPrimary,
                    )
                    is GameModeInfo.Unavailable -> Text(
                        mode.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = LauncherColors.TextSecondary,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = LauncherColors.Accent, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun InfoSection(title: String, rows: List<InfoRow>) {
    SectionHeader(title)
    rows.forEach { row ->
        Column {
            Text(row.label, style = MaterialTheme.typography.bodySmall, color = LauncherColors.TextSecondary)
            Text(row.value, style = MaterialTheme.typography.bodyMedium, color = LauncherColors.TextPrimary)
        }
    }
}
