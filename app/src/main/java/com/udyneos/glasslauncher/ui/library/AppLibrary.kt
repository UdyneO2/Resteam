package com.udyneos.glasslauncher.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.udyneos.glasslauncher.data.AppCategory
import com.udyneos.glasslauncher.data.AppInfo
import com.udyneos.glasslauncher.data.AppRepository
import com.udyneos.glasslauncher.ui.glass.frosted
import com.udyneos.glasslauncher.ui.home.AppIcon

/**
 * Last page of the pager. Apps are grouped by the category the app itself
 * declares in its manifest; anything undeclared lands in Utilities.
 */
@Composable
fun AppLibrary(
    apps: List<AppInfo>,
    showLabels: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var openCategory by remember { mutableStateOf<AppCategory?>(null) }

    val grouped = remember(apps) {
        apps.groupBy { AppRepository.categoryOf(it) }
    }

    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        SearchField(
            value = query,
            onValueChange = { query = it; openCategory = null },
            modifier = Modifier.padding(top = 12.dp, bottom = 14.dp),
        )

        val category = openCategory
        when {
            query.isNotBlank() -> {
                val hits = apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
                AppList(hits, onLaunch, onLongPress)
            }

            category != null -> {
                Text(
                    text = "‹  ${category.title}",
                    modifier = Modifier
                        .clickable { openCategory = null }
                        .padding(vertical = 8.dp),
                    style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                )
                AppList(grouped[category].orEmpty(), onLaunch, onLongPress)
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(AppCategory.entries.filter { grouped[it]?.isNotEmpty() == true }) { cat ->
                        CategoryTile(
                            category = cat,
                            apps = grouped[cat].orEmpty(),
                            showLabels = showLabels,
                            onLaunch = onLaunch,
                            onOpen = { openCategory = cat },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(Color.White),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .frosted(RoundedCornerShape(16.dp), alpha = 0.22f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (value.isEmpty()) {
                    Text("App Library", style = TextStyle(color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp))
                }
                inner()
            }
        },
    )
}

@Composable
private fun CategoryTile(
    category: AppCategory,
    apps: List<AppInfo>,
    showLabels: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onOpen: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .frosted(RoundedCornerShape(26.dp))
                .padding(14.dp)
        ) {
            val preview = apps.take(4)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                preview.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { app ->
                            AppIcon(
                                app = app,
                                showLabel = false,
                                onClick = { onLaunch(app) },
                                iconSize = 46.dp,
                            )
                        }
                    }
                }
            }
            // More than four apps: the corner becomes an "open folder" hit area.
            if (apps.size > 4) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(46.dp)
                        .clickable(onClick = onOpen)
                )
            }
        }
        if (showLabels) {
            Text(
                text = category.title,
                modifier = Modifier
                    .clickable(onClick = onOpen)
                    .padding(top = 6.dp),
                style = TextStyle(color = Color.White, fontSize = 12.sp),
            )
        }
    }
}

@Composable
private fun AppList(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(apps, key = { it.key }) { app ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onLaunch(app) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                )
                Text(app.label, style = TextStyle(color = Color.White, fontSize = 16.sp))
            }
        }
    }
}
