package com.example.launcher.ui.tweaks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.domain.model.CompileFilter
import com.example.launcher.domain.model.GameMode
import com.example.launcher.domain.model.TweakPreset
import com.example.launcher.ui.components.FilterPill
import com.example.launcher.ui.components.LauncherCard
import com.example.launcher.ui.components.launcherTextFieldColors
import com.example.launcher.ui.theme.LauncherColors

/** Konten tab "Shizuku Tweaks": kelola preset per game. Dipakai di dalam layar Home yang sudah scrollable. */
@Composable
fun TweaksTab(
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TweaksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<TweakPreset?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TweaksEvent.ShowMessage -> onMessage(event.text)
            }
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when {
            state.isLoading -> CircularProgressIndicator(color = LauncherColors.Accent)
            state.games.isEmpty() -> LauncherCard(Modifier.fillMaxWidth()) {
                Text(
                    "Belum ada game. Tambahkan lewat menu Manage agar preset bisa dibuat.",
                    color = LauncherColors.TextSecondary,
                )
            }
            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.games, key = { it.id }) { game ->
                        FilterPill(game.title, selected = game.id == state.selectedGameId, onClick = { viewModel.onGameSelected(game) })
                    }
                }
                if (!state.canApply) {
                    Text(
                        "Shizuku belum siap. Preset tetap bisa diedit, tetapi belum bisa diterapkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LauncherColors.Warning,
                    )
                }
                if (state.presets.isEmpty()) {
                    Text("Belum ada preset untuk game ini.", color = LauncherColors.TextSecondary)
                }
                state.presets.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        canApply = state.canApply && !state.isApplying,
                        onApply = { viewModel.onApplyPreset(preset) },
                        onEdit = { viewModel.onEditPreset(preset) },
                        onDelete = { pendingDelete = preset },
                    )
                }
                Button(
                    onClick = viewModel::onNewPreset,
                    colors = ButtonDefaults.buttonColors(containerColor = LauncherColors.Accent, contentColor = LauncherColors.OnAccent),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Preset baru")
                }
            }
        }
    }

    state.draft?.let { draft ->
        PresetEditorDialog(
            draft = draft,
            onChange = viewModel::onDraftChange,
            onSave = viewModel::onSaveDraft,
            onDismiss = viewModel::onDismissDraft,
        )
    }

    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = LauncherColors.Surface,
            title = { Text("Hapus preset?", color = LauncherColors.TextPrimary) },
            text = { Text("\"${preset.name}\" akan dihapus permanen.", color = LauncherColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.onDeletePreset(preset); pendingDelete = null }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun PresetCard(
    preset: TweakPreset,
    canApply: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = LauncherColors.Surface,
        border = BorderStroke(1.dp, LauncherColors.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(preset.name, style = MaterialTheme.typography.titleMedium, color = LauncherColors.TextPrimary)
            Text(preset.describe(), style = MaterialTheme.typography.bodySmall, color = LauncherColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onApply,
                    enabled = canApply,
                    colors = ButtonDefaults.buttonColors(containerColor = LauncherColors.Accent, contentColor = LauncherColors.OnAccent),
                ) { Text("Apply") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit ${preset.name}", tint = LauncherColors.TextPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Hapus ${preset.name}", tint = LauncherColors.Error)
                }
            }
        }
    }
}

@Composable
private fun PresetEditorDialog(
    draft: PresetDraft,
    onChange: (PresetDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LauncherColors.Surface,
        title = {
            Text(if (draft.id == 0L) "Preset baru" else "Edit preset", color = LauncherColors.TextPrimary)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onChange(draft.copy(name = it)) },
                    singleLine = true,
                    label = { Text("Nama preset") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = launcherTextFieldColors(),
                )
                SectionLabel("Game mode")
                GameMode.entries.forEach { mode ->
                    RadioRow(mode.label, selected = draft.gameMode == mode) { onChange(draft.copy(gameMode = mode)) }
                }
                SectionLabel("Compile (opsional)")
                RadioRow("Tidak ada", selected = draft.compileFilter == null) { onChange(draft.copy(compileFilter = null)) }
                CompileFilter.entries.forEach { filter ->
                    RadioRow(filter.arg, selected = draft.compileFilter == filter) { onChange(draft.copy(compileFilter = filter)) }
                }
                Text(
                    "Proses compile bisa memakan waktu saat preset diterapkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherColors.TextSecondary,
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = draft.isValid) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = LauncherColors.Accent, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = LauncherColors.Accent,
                unselectedColor = LauncherColors.TextSecondary,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = LauncherColors.TextPrimary)
    }
}
