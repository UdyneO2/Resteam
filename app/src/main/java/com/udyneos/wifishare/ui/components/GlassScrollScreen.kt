package com.udyneos.wifishare.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.udyneos.wifishare.ui.theme.Ios

/**
 * Layar scroll dengan bar judul kaca. Judul besar ikut tergulir; begitu digulir, bar kaca di atas
 * perlahan muncul dan mengaburkan isi yang lewat di bawahnya, dengan judul kecil di tengah.
 *
 * Isi yang tergulir direkam ke layer sendiri (contentBackdrop), lalu digabung dengan wallpaper
 * lewat rememberCombinedBackdrop. Bar berada di luar layer itu, jadi tidak menggambar dirinya sendiri.
 */
@Composable
fun GlassScrollScreen(
    title: String,
    wallpaper: Backdrop,
    bottomInset: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ios = Ios.colors
    val contentBackdrop = rememberLayerBackdrop()
    val barBackdrop = rememberCombinedBackdrop(wallpaper, contentBackdrop)
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val barTint = glassTint(0.12f)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 0 saat di paling atas, 1 setelah digulir sekitar 60dp
    val fadeDistance = with(density) { 60.dp.toPx() }
    val barProgress by remember { derivedStateOf { (scroll.value / fadeDistance).coerceIn(0f, 1f) } }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(contentBackdrop)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .statusBarsPadding()
                    .padding(bottom = bottomInset),
            ) {
                LargeTitle(title)
                content()
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topInset + 52.dp)
                .drawBackdrop(
                    backdrop = barBackdrop,
                    shape = { RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp) },
                    effects = {
                        colorControls(saturation = 1.4f)
                        blur(12.dp.toPx())
                    },
                    layerBlock = { alpha = barProgress },
                    onDrawSurface = {
                        drawRect(barTint)
                        glassEdge(28.dp.toPx(), ios.isDark)
                    },
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                text = title,
                color = ios.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 15.dp),
            )
        }
    }
}
