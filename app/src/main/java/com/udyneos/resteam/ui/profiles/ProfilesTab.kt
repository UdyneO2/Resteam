package com.udyneos.resteam.ui.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udyneos.resteam.ui.components.FilterPill
import com.udyneos.resteam.ui.components.GameIcon
import com.udyneos.resteam.ui.components.LauncherCard
import com.udyneos.resteam.ui.scripts.ScriptsMasterToggleCard
import com.udyneos.resteam.ui.theme.LauncherColors

/** Konten tab "Profiles": preset default per game dan auto-apply saat Launch Game. */
@Composable
fun ProfilesTab(
    modifier: Modifier = Modifier,
    viewModel: ProfilesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScriptsMasterToggleCard()
        Text(
            "Pilih preset default untuk tiap game. Jika auto-apply aktif, preset diterapkan lewat Shizuku tepat sebelum game dibuka dari tombol Launch Game.",
            style = MaterialTheme.typography.bodySmall,
            color = LauncherColors.TextSecondary,
        )
        when {
            state.isLoading -> CircularProgressIndicator(color = LauncherColors.Accent)
            state.rows.isEmpty() -> LauncherCard(Modifier.fillMaxWidth()) {
                Text("Belum ada game. Tambahkan lewat menu Manage.", color = LauncherColors.TextSecondary)
            }
            else -> state.rows.forEach { row ->
                ProfileCard(
                    row = row,
                    onPresetSelected = { viewModel.onPresetSelected(row.game, it) },
                    onAutoApplyChange = { viewModel.onAutoApplyChange(row.game, it) },
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    row: ProfileRow,
    onPresetSelected: (Long?) -> Unit,
    onAutoApplyChange: (Boolean) -> Unit,
) {
    LauncherCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameIcon(row.game.packageName, Modifier.size(44.dp))
            Text(
                row.game.title,
                style = MaterialTheme.typography.titleMedium,
                color = LauncherColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("Preset default", style = MaterialTheme.typography.bodySmall, color = LauncherColors.TextSecondary)
        Spacer(Modifier.height(8.dp))
        if (row.presets.isEmpty()) {
            Text(
                "Belum ada preset untuk game ini.",
                style = MaterialTheme.typography.bodySmall,
                color = LauncherColors.Warning,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "none") {
                    FilterPill("Tidak ada", selected = row.selectedPresetId == null, onClick = { onPresetSelected(null) })
                }
                items(row.presets, key = { it.id }) { preset ->
                    FilterPill(preset.name, selected = row.selectedPresetId == preset.id, onClick = { onPresetSelected(preset.id) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Terapkan otomatis saat Launch",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.selectedPresetId != null) LauncherColors.TextPrimary else LauncherColors.TextSecondary,
            )
            Switch(
                checked = row.autoApply,
                onCheckedChange = onAutoApplyChange,
                enabled = row.selectedPresetId != null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LauncherColors.OnAccent,
                    checkedTrackColor = LauncherColors.Accent,
                    uncheckedThumbColor = LauncherColors.TextSecondary,
                    uncheckedTrackColor = LauncherColors.Border,
                    uncheckedBorderColor = LauncherColors.Border,
                ),
            )
        }
    }
}
