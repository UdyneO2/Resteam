package com.udyneos.resteam.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.udyneos.resteam.ui.theme.LauncherColors
import com.udyneos.resteam.ui.theme.LauncherShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Memuat ikon aplikasi dari PackageManager di background thread. */
@Composable
fun GameIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
            }.getOrNull()
        }
    }
    val shape = modifier.clip(LauncherShapes.Cell)
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = shape)
    } else {
        Box(shape.background(LauncherColors.ControlSurface))
    }
}
