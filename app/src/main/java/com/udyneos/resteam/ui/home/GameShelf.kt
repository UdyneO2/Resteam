package com.udyneos.resteam.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.udyneos.resteam.domain.model.Game
import com.udyneos.resteam.ui.components.GameIcon
import com.udyneos.resteam.ui.theme.LauncherColors
import com.udyneos.resteam.ui.theme.LauncherShapes

@Composable
fun GameShelf(
    games: List<Game>,
    selectedId: String,
    onSelect: (Game) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Your games (${games.size})",
            style = MaterialTheme.typography.headlineSmall,
            color = LauncherColors.TextPrimary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(games, key = { it.id }) { game ->
                ShelfItem(game, game.id == selectedId, onClick = { onSelect(game) })
            }
        }
    }
}

@Composable
private fun ShelfItem(game: Game, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = LauncherShapes.Card,
        color = LauncherColors.Surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) LauncherColors.Accent else LauncherColors.Border),
    ) {
        Column(
            modifier = Modifier.width(96.dp).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GameIcon(game.packageName, Modifier.size(56.dp))
            Text(
                game.title,
                style = MaterialTheme.typography.bodySmall,
                color = LauncherColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
