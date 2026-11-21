package com.udyneos.resteam.ui.scripts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udyneos.resteam.domain.model.ShizukuScript
import com.udyneos.resteam.ui.components.LauncherCard
import com.udyneos.resteam.ui.theme.LauncherColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun launcherSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = LauncherColors.OnAccent,
    checkedTrackColor = LauncherColors.Accent,
    uncheckedThumbColor = LauncherColors.TextSecondary,
    uncheckedTrackColor = LauncherColors.Border,
    uncheckedBorderColor = LauncherColors.Border,
)

/** Tab "Shizuku Tweaks": script global dari GitHub, bukan lagi per game. */
@Composable
fun ScriptsTab(
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScriptsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.loadBranches()
        viewModel.events.collect { event ->
            when (event) {
                is ScriptsEvent.ShowMessage -> onMessage(event.text)
            }
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LauncherCard(Modifier.fillMaxWidth()) {
            Text("Script dari ${state.repo}", style = MaterialTheme.typography.titleMedium, color = LauncherColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Script yang diaktifkan dijalankan lewat Shizuku saat Launch Game, jika toggle \"Jalankan Shizuku Tweaks\" di tab Profiles menyala.",
                style = MaterialTheme.typography.bodySmall,
                color = LauncherColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BranchPicker(state.branch, state.branches, onSelect = viewModel::onBranchSelected, enabled = !state.isUpdating)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = viewModel::onUpdate,
                    enabled = !state.isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LauncherColors.Accent,
                        contentColor = LauncherColors.OnAccent,
                    ),
                ) {
                    if (state.isUpdating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LauncherColors.OnAccent)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.isUpdating) "Memperbarui…" else "Update")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                lastUpdatedLabel(state.lastUpdatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = LauncherColors.TextSecondary,
            )
        }

        if (state.scripts.isEmpty()) {
            LauncherCard(Modifier.fillMaxWidth()) {
                Text(
                    "Belum ada script. Tekan Update untuk mengambil dari folder scripts/ di branch ${state.branch}.",
                    color = LauncherColors.TextSecondary,
                )
            }
        } else {
            state.scripts.forEach { script ->
                ScriptCard(script, onToggle = { viewModel.onToggle(script, it) })
            }
        }
    }
}

@Composable
private fun BranchPicker(
    branch: String,
    branches: List<String>,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
            Text("Branch: $branch", maxLines = 1, overflow = TextOverflow.Ellipsis, color = LauncherColors.TextPrimary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            branches.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        expanded = false
                        onSelect(name)
                    },
                )
            }
        }
    }
}

@Composable
private fun ScriptCard(script: ShizukuScript, onToggle: (Boolean) -> Unit) {
    LauncherCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    script.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = LauncherColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (script.description.isNotBlank()) {
                    Text(script.description, style = MaterialTheme.typography.bodySmall, color = LauncherColors.TextSecondary)
                }
                Text(
                    "${script.fileName} · v${script.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LauncherColors.TextSecondary,
                )
            }
            Switch(checked = script.enabled, onCheckedChange = onToggle, colors = launcherSwitchColors())
        }
    }
}

/** Kartu toggle global di tab Profiles. */
@Composable
fun ScriptsMasterToggleCard(
    modifier: Modifier = Modifier,
    viewModel: ScriptsGateViewModel = hiltViewModel(),
) {
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    LauncherCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Jalankan Shizuku Tweaks", style = MaterialTheme.typography.titleMedium, color = LauncherColors.TextPrimary)
                Text(
                    "Jalankan script aktif (tab Shizuku Tweaks) saat game dibuka, lalu kembalikan nilainya setelah game ditutup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherColors.TextSecondary,
                )
            }
            Switch(checked = enabled, onCheckedChange = viewModel::onChange, colors = launcherSwitchColors())
        }
    }
}

private fun lastUpdatedLabel(epochMs: Long): String =
    if (epochMs <= 0L) {
        "Belum pernah diperbarui"
    } else {
        val text = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault()))
        "Terakhir diperbarui $text"
    }
