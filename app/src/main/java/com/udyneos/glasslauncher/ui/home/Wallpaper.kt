package com.udyneos.glasslauncher.ui.home

import android.app.WallpaperManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

/**
 * Draws the system wallpaper when we're allowed to read it, otherwise a
 * soft gradient. The gradient matters: glass needs something colourful
 * behind it or the blur just looks like grey smoke.
 */
@Composable
fun Wallpaper(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember {
        runCatching {
            WallpaperManager.getInstance(context).peekDrawable()?.toBitmap()?.asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Canvas(modifier.fillMaxSize()) {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B)),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                )
            )
        }
    }
}
