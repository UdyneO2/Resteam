package com.udyneos.wifishare.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.udyneos.wifishare.ui.theme.Ios
import kotlinx.coroutines.launch

enum class Tab(val label: String) {
    WIFI("WiFi"),
    CLIENT("Klien"),
    SETTINGS("Pengaturan"),
}

private val TabGap = 4.dp

/**
 * Bottom bar kaca berbentuk pil (tutorial Glass Bottom Bar dari Backdrop). Indikator tab aktif
 * adalah satu kaca bertint yang meluncur dengan pegas antar tab, bukan muncul-hilang per tab.
 *
 * [backdrop] harus layer yang TIDAK memuat bar ini (lihat GlassHost).
 */
@Composable
fun GlassBottomBar(
    backdrop: Backdrop,
    selected: Tab,
    icons: Map<Tab, ImageVector>,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ios = Ios.colors
    val surface = glassTint(0.12f)
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    val accent = ios.blue

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 10.dp)
            .fillMaxWidth()
            .height(64.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                    if (supportsLens) lens(16.dp.toPx(), 32.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(surface)
                    glassEdge(size.minDimension / 2f, ios.isDark)
                },
            )
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val count = Tab.entries.size
            val tabWidth = (maxWidth - TabGap * (count - 1)) / count
            val indicatorX by animateDpAsState(
                targetValue = (tabWidth + TabGap) * selected.ordinal,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
                label = "indicatorX",
            )

            val onPressChange: (Boolean) -> Unit = { pressed ->
                scope.launch { press.animateTo(if (pressed) 1f else 0f, spring(0.5f, 300f, 0.001f)) }
            }

            // kaca bertint yang meluncur; BlendMode.Hue supaya warna backdrop menyesuaikan tint
            Box(
                Modifier
                    .offset { IntOffset(indicatorX.roundToPx(), 0) }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                            if (supportsLens) lens(8.dp.toPx(), 16.dp.toPx())
                        },
                        // press-to-scale: layerBlock, bukan graphicsLayer, supaya backdrop tidak ikut membesar
                        layerBlock = {
                            val maxScale = (size.width + 16.dp.toPx()) / size.width
                            val scale = lerp(1f, maxScale, press.value)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = {
                            drawRect(accent, blendMode = BlendMode.Hue)
                            drawRect(accent.copy(alpha = 0.75f))
                        },
                    ),
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(TabGap),
            ) {
                Tab.entries.forEach { tab ->
                    TabItem(
                        tab = tab,
                        icon = icons.getValue(tab),
                        active = tab == selected,
                        onClick = { onSelect(tab) },
                        onPressChange = onPressChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    tab: Tab,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    onPressChange: (Boolean) -> Unit,
) {
    val ios = Ios.colors
    val content by animateColorAsState(
        targetValue = if (active) Color.White else ios.secondaryLabel,
        label = "tabTint",
    )
    // ikon sedikit membesar dan memantul saat tab dipilih
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "iconScale",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onClick,
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onPressChange(true)
                    waitForUpOrCancellation()
                    onPressChange(false)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tab.label,
            tint = content,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        Text(tab.label, color = content, fontSize = 10.sp)
    }
}
