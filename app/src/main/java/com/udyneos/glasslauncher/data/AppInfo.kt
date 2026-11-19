package com.udyneos.glasslauncher.data

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val category: Int,
    val icon: ImageBitmap,
) {
    val key: String get() = "$packageName/$activityName"
}
