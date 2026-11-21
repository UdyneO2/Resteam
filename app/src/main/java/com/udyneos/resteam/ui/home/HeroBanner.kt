package com.udyneos.resteam.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.udyneos.resteam.ui.components.GameIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.udyneos.resteam.domain.model.Game
import com.udyneos.resteam.domain.model.ShizukuState
import com.udyneos.resteam.ui.components.CircleIconButton
import com.udyneos.resteam.ui.components.StatusDot
import com.udyneos.resteam.ui.components.StatusPill
import com.udyneos.resteam.ui.components.indicatorColor
import com.udyneos.resteam.ui.components.pillText
import com.udyneos.resteam.ui.theme.LauncherColors
import com.udyneos.resteam.ui.theme.LauncherShapes

@Composable
fun HeroBanner(
    game: Game,
    shizukuState: ShizukuState,
    isCompact: Boolean,
    onLaunch: () -> Unit,
    onShizukuClick: () -> Unit,
    onTweakClick: () -> Unit,
    onInfoClick: () -> Unit,
    onLogClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** File banner pilihan pengguna; menimpa cover bawaan. */
    bannerFile: java.io.File? = null,
    onBannerClick: (() -> Unit)? = null,
    onBannerLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(LauncherShapes.Card)
            .background(LauncherColors.Surface),
    ) {
        // Latar: cover game jika ada, jika tidak gradient aksen
        val bannerModel: Any? = bannerFile ?: game.coverUrl
        if (bannerModel != null) {
            AsyncImage(
                model = bannerModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(
                Modifier.matchParentSize().background(
                    Brush.linearGradient(listOf(LauncherColors.Accent.copy(alpha = 0.18f), LauncherColors.Surface)),
                ),
            )
        }
        // Scrim agar teks tetap terbaca
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, LauncherColors.Canvas.copy(alpha = 0.92f))),
            ),
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    lastPlayedLabel(game.lastPlayedAt),
                    style = MaterialTheme.typography.titleMedium,
                    color = LauncherColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                GameIcon(game.packageName, Modifier.size(56.dp))
            }
            Spacer(Modifier.height(if (isCompact) 72.dp else 120.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                game.hoursPlayed?.let { StatusPill("$it hrs played") }
                StatusPill(
                    text = shizukuState.pillText(),
                    leading = { StatusDot(shizukuState.indicatorColor()) },
                    onClick = if (shizukuState == ShizukuState.PermissionRequired) onShizukuClick else null,
                )
            }
            Text(game.title, style = MaterialTheme.typography.headlineLarge, color = LauncherColors.TextPrimary)

            val utilities: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleIconButton(Icons.Rounded.Tune, "Tweak preset", onTweakClick)
                    CircleIconButton(Icons.Rounded.Info, "System properties", onInfoClick)
                    CircleIconButton(Icons.Rounded.Terminal, "Terminal / log viewer", onLogClick)
                    if (onBannerClick != null) {
                        CircleIconButton(Icons.Rounded.Image, "Ubah banner", onBannerClick)
                    }
                    if (bannerFile != null && onBannerLongClick != null) {
                        CircleIconButton(Icons.Rounded.HideImage, "Hapus banner", onBannerLongClick)
                    }
                }
            }
            val cta: @Composable (Modifier) -> Unit = { m ->
                Button(
                    onClick = onLaunch,
                    modifier = m.height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LauncherColors.Accent,
                        contentColor = LauncherColors.OnAccent,
                    ),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Launch Game", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (isCompact) {
                utilities()
                cta(Modifier.fillMaxWidth())
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    utilities()
                    Spacer(Modifier.weight(1f))
                    cta(Modifier)
                }
            }
        }
    }
}

private fun lastPlayedLabel(epochMs: Long?): String =
    epochMs?.let {
        val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
        "Last played $date"
    } ?: "Not played yet"
