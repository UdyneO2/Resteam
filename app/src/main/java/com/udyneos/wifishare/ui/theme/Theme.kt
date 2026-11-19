package com.udyneos.wifishare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Warna sistem iOS, nilainya diambil dari HIG. */
data class IosColors(
    val blue: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val label: Color,
    val secondaryLabel: Color,
    val separator: Color,
    val groupedBackground: Color,
    val cardTint: Color,
    val isDark: Boolean,
)

private val LightIos = IosColors(
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    label = Color(0xFF000000),
    secondaryLabel = Color(0x993C3C43),
    separator = Color(0x4A3C3C43),
    groupedBackground = Color(0xFFF2F2F7),
    cardTint = Color(0x59FFFFFF),
    isDark = false,
)

private val DarkIos = IosColors(
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0x99EBEBF5),
    separator = Color(0x99545458),
    groupedBackground = Color(0xFF000000),
    cardTint = Color(0x661C1C1E),
    isDark = true,
)

val LocalIos = staticCompositionLocalOf { LightIos }

object Ios {
    val colors: IosColors
        @Composable get() = LocalIos.current
}

@Composable
fun WifiShareTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ios = if (dark) DarkIos else LightIos

    val scheme = if (dark) {
        darkColorScheme(primary = ios.blue, background = ios.groupedBackground)
    } else {
        lightColorScheme(primary = ios.blue, background = ios.groupedBackground)
    }

    CompositionLocalProvider(LocalIos provides ios) {
        MaterialTheme(colorScheme = scheme, typography = IosTypography, content = content)
    }
}
