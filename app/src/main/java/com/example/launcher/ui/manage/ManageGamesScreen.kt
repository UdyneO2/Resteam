package com.example.launcher.ui.manage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.domain.model.ManagedApp
import com.example.launcher.ui.components.GameIcon
import com.example.launcher.ui.theme.LauncherColors
import com.example.launcher.ui.theme.LauncherShapes

@Composable
fun ManageGamesScreen(
    modifier: Modifier = Modifier,
    viewModel: ManageGamesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Manage games", style = MaterialTheme.typography.headlineSmall, color = LauncherColors.TextPrimary)
        Text(
            "Aktifkan aplikasi yang ingin muncul di Library. Game yang terdeteksi otomatis sudah aktif.",
            style = MaterialTheme.typography.bodySmall,
            color = LauncherColors.TextSecondary,
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = LauncherShapes.Pill,
            placeholder = { Text("Cari aplikasi") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LauncherColors.Accent,
                unfocusedBorderColor = LauncherColors.Border,
                focusedTextColor = LauncherColors.TextPrimary,
                unfocusedTextColor = LauncherColors.TextPrimary,
                cursorColor = LauncherColors.Accent,
                focusedLeadingIconColor = LauncherColors.Accent,
                unfocusedLeadingIconColor = LauncherColors.TextSecondary,
                focusedPlaceholderColor = LauncherColors.TextSecondary,
                unfocusedPlaceholderColor = LauncherColors.TextSecondary,
            ),
        )

        when {
            state.isLoading && state.apps.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LauncherColors.Accent)
            }
            state.error != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.error.orEmpty(), color = LauncherColors.Error)
                Button(onClick = viewModel::refresh) { Text("Coba lagi") }
            }
            state.apps.isEmpty() -> Text("Tidak ada aplikasi yang cocok.", color = LauncherColors.TextSecondary)
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(state.apps, key = { it.packageName }) { app ->
                    AppRow(app, onToggle = { viewModel.onToggle(app, it) })
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: ManagedApp, onToggle: (Boolean) -> Unit) {
    Surface(
        shape = LauncherShapes.Card,
        color = LauncherColors.Surface,
        border = BorderStroke(1.dp, LauncherColors.Border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GameIcon(app.packageName, Modifier.size(44.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LauncherColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(app.packageName)
                        if (app.detectedAsGame) append(" • auto")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = app.isGame,
                onCheckedChange = onToggle,
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
