package com.udyneos.glasslauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlassColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    surface = Color(0xFF111418),
    onSurface = Color.White,
    background = Color.Black,
    onBackground = Color.White,
)

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GlassColors, content = content)
}
