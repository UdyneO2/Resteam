package com.udyneos.resteam.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.udyneos.resteam.domain.model.LogEntry
import com.udyneos.resteam.domain.model.TweakPreset
import com.udyneos.resteam.ui.logs.toDisplayText
import com.udyneos.resteam.ui.theme.LauncherColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSheet(
    presets: List<TweakPreset>,
    isApplying: Boolean,
    canApply: Boolean,
    confirmApply: Boolean,
    onApply: (TweakPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    var pending by remember { mutableStateOf<TweakPreset?>(null) }

    pending?.let { preset ->
        AlertDialog(
            onDismissRequest = { pending = null },
            containerColor = LauncherColors.Surface,
            title = { Text("Terapkan preset?", color = LauncherColors.TextPrimary) },
            text = { Text("${preset.name} • ${preset.describe()}", color = LauncherColors.TextSecondary) },
            confirmButton = { TextButton(onClick = { pending = null; onApply(preset) }) { Text("Terapkan") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Batal") } },
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = LauncherColors.Surface) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tweak preset", style = MaterialTheme.typography.headlineSmall, color = LauncherColors.TextPrimary)
            if (!canApply) {
                Text(
                    "Aktifkan Shizuku dan berikan izin agar preset bisa diterapkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherColors.Warning,
                )
            }
            if (presets.isEmpty()) {
                Text("Belum ada preset.", color = LauncherColors.TextSecondary)
            }
            presets.forEach { preset ->
                PresetRow(
                    preset,
                    enabled = canApply && !isApplying,
                    onApply = { if (confirmApply) pending = preset else onApply(preset) },
                )
            }
        }
    }
}

@Composable
private fun PresetRow(preset: TweakPreset, enabled: Boolean, onApply: () -> Unit) {
    Surface(
        color = LauncherColors.Canvas,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, LauncherColors.Border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.titleMedium, color = LauncherColors.TextPrimary)
                Text(preset.describe(), style = MaterialTheme.typography.bodySmall, color = LauncherColors.TextSecondary)
            }
            Button(
                onClick = onApply,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LauncherColors.Accent,
                    contentColor = LauncherColors.OnAccent,
                ),
            ) { Text("Apply") }
        }
    }
}

@Composable
fun LogDialog(log: List<LogEntry>, onClear: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LauncherColors.Surface,
        title = { Text("Log", color = LauncherColors.TextPrimary) },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                if (log.isEmpty()) {
                    Text("Belum ada aktivitas.", color = LauncherColors.TextSecondary)
                } else {
                    Text(
                        text = log.asReversed().joinToString("\n\n") { it.toDisplayText() },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = LauncherColors.TextPrimary,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
        dismissButton = { TextButton(onClick = onClear, enabled = log.isNotEmpty()) { Text("Bersihkan") } },
    )
}
