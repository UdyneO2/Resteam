package com.udyneos.resteam.ui.shizuku

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udyneos.resteam.domain.model.ShizukuState
import com.udyneos.resteam.ui.components.LauncherCard
import com.udyneos.resteam.ui.components.StatusDot
import com.udyneos.resteam.ui.components.description
import com.udyneos.resteam.ui.components.indicatorColor
import com.udyneos.resteam.ui.components.pillText
import com.udyneos.resteam.ui.theme.LauncherColors

@Composable
fun ShizukuScreen(
    modifier: Modifier = Modifier,
    viewModel: ShizukuViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Shizuku", style = MaterialTheme.typography.headlineSmall, color = LauncherColors.TextPrimary)

        LauncherCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusDot(state.shizuku.indicatorColor(), size = 14)
                Text(state.shizuku.pillText(), style = MaterialTheme.typography.titleMedium, color = LauncherColors.TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(state.shizuku.description(), style = MaterialTheme.typography.bodyMedium, color = LauncherColors.TextSecondary)
            Spacer(Modifier.height(16.dp))

            val buttonColors = ButtonDefaults.buttonColors(
                containerColor = LauncherColors.Accent,
                contentColor = LauncherColors.OnAccent,
            )
            when (state.shizuku) {
                ShizukuState.PermissionRequired ->
                    Button(onClick = viewModel::onRequestPermission, colors = buttonColors) { Text("Berikan izin") }
                is ShizukuState.Ready ->
                    Button(
                        onClick = viewModel::onRunDiagnostic,
                        enabled = state.diagnostic !is DiagnosticState.Running,
                        colors = buttonColors,
                    ) { Text(if (state.diagnostic is DiagnosticState.Running) "Menjalankan…" else "Jalankan diagnostik") }
                else ->
                    Button(onClick = { openShizukuApp(context) }, colors = buttonColors) { Text("Buka Shizuku") }
            }
        }

        (state.diagnostic as? DiagnosticState.Finished)?.let { result ->
            LauncherCard(Modifier.fillMaxWidth()) {
                Text(
                    if (result.success) "Diagnostik berhasil" else "Diagnostik gagal",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (result.success) LauncherColors.Success else LauncherColors.Error,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    result.output,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherColors.TextPrimary,
                )
            }
        }

        LauncherCard(Modifier.fillMaxWidth()) {
            Text("Cara mengaktifkan", style = MaterialTheme.typography.titleMedium, color = LauncherColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            listOf(
                "1. Pasang aplikasi Shizuku (shizuku.rikka.app).",
                "2. Jalankan layanannya: aktifkan Wireless debugging di Developer options lalu tekan Start di aplikasi Shizuku, atau lewat ADB: adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                "3. Kembali ke sini lalu tekan \"Berikan izin\".",
            ).forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = LauncherColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
