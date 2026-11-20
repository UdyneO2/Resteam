package com.example.launcher.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.BuildConfig
import com.example.launcher.ui.components.LauncherCard
import com.example.launcher.ui.components.launcherTextFieldColors
import com.example.launcher.ui.theme.LauncherColors

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshUsageAccess() }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = LauncherColors.TextPrimary)

        LauncherCard(Modifier.fillMaxWidth()) {
            SectionTitle("Profil")
            Spacer(Modifier.height(12.dp))
            UserNameField(current = state.settings.userName, onSave = viewModel::onSaveUserName)
        }

        LauncherCard(Modifier.fillMaxWidth()) {
            SectionTitle("Preset tweak")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Konfirmasi sebelum menerapkan", style = MaterialTheme.typography.titleMedium, color = LauncherColors.TextPrimary)
                    Text(
                        "Tampilkan dialog konfirmasi sebelum preset dijalankan lewat Shizuku.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LauncherColors.TextSecondary,
                    )
                }
                Switch(
                    checked = state.settings.confirmBeforeApply,
                    onCheckedChange = viewModel::onConfirmBeforeApplyChange,
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

        LauncherCard(Modifier.fillMaxWidth()) {
            SectionTitle("Statistik bermain")
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.hasUsageAccess) "Akses penggunaan aktif. Jam main dan riwayat aktivitas ditampilkan."
                else "Akses penggunaan belum aktif. Jam main dan riwayat aktivitas tidak tersedia.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.hasUsageAccess) LauncherColors.Success else LauncherColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { openUsageAccessSettings(context) }) { Text("Buka pengaturan") }
        }

        LauncherCard(Modifier.fillMaxWidth()) {
            SectionTitle("Tentang")
            Spacer(Modifier.height(8.dp))
            Text("Versi ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = LauncherColors.TextSecondary)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = LauncherColors.Accent)
}

@Composable
private fun UserNameField(current: String, onSave: (String) -> Unit) {
    var text by remember(current) { mutableStateOf(current) }
    val trimmed = text.trim()

    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(SettingsViewModel.MAX_NAME_LENGTH) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Nama tampilan") },
        colors = launcherTextFieldColors(),
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onSave(trimmed) },
        enabled = trimmed.isNotEmpty() && trimmed != current,
        colors = ButtonDefaults.buttonColors(containerColor = LauncherColors.Accent, contentColor = LauncherColors.OnAccent),
    ) { Text("Simpan") }
}

private fun openUsageAccessSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
