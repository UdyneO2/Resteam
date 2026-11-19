package com.udyneos.wifishare.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.roundToInt

enum class Tab(val label: String) {
    WIFI("Hotspot"),
    CLIENT("Klien"),
    SETTINGS("Pengaturan"),
}

private val TabGap = 4.dp

@Composable
fun GlassBottomBar(
    backdrop: Backdrop,
    selected: Tab,
    icons: Map<Tab, ImageVector>,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ios = Ios.colors
    val surface = glassTint(0.14f)
    val thumbSurface = glassTint(0.18f)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val press = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp)
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
            val tabStridePx = with(density) { (tabWidth + TabGap).toPx() }
            val maxOffsetPx = tabStridePx * (count - 1).coerceAtLeast(0)

            val indicatorPx = remember { Animatable(tabStridePx * selected.ordinal) }
            var isDragging by remember { mutableStateOf(false) }

            val hoveredIndex = if (tabStridePx > 0f) {
                (indicatorPx.value / tabStridePx).roundToInt().coerceIn(0, count - 1)
            } else {
                selected.ordinal
            }
            val highlightedTab = if (isDragging) Tab.entries[hoveredIndex] else selected

            LaunchedEffect(selected, tabStridePx, isDragging) {
                if (!isDragging) {
                    indicatorPx.animateTo(
                        targetValue = (tabStridePx * selected.ordinal).coerceIn(0f, maxOffsetPx),
                        animationSpec = spring(
                            dampingRatio = 0.68f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }

            val onPressChange: (Boolean) -> Unit = { pressed ->
                scope.launch {
                    press.animateTo(
                        targetValue = if (pressed) 1f else 0f,
                        animationSpec = spring(0.5f, 300f, 0.001f),
                    )
                }
            }

            fun snapToNearestTab() {
                if (tabStridePx <= 0f) return
                val targetIndex = (indicatorPx.value / tabStridePx)
                    .roundToInt()
                    .coerceIn(0, count - 1)
                val targetTab = Tab.entries[targetIndex]
                val targetPx = tabStridePx * targetIndex

                scope.launch {
                    indicatorPx.animateTo(
                        targetValue = targetPx,
                        animationSpec = spring(
                            dampingRatio = 0.68f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
                if (targetTab != selected) {
                    onSelect(targetTab)
                }
            }

            // 1. Indikator kaca bening tanpa tint di lapisan bawah Row agar ikon & label tidak tertutup/terdistorsi
            Box(
                Modifier
                    .offset { IntOffset(indicatorPx.value.roundToInt(), 0) }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(3.dp.toPx())
                            if (supportsLens) lens(8.dp.toPx(), 16.dp.toPx())
                        },
                        layerBlock = {
                            val maxScale = ((size.height + 8.dp.toPx()) / size.height)
                                .coerceAtLeast((size.width + 14.dp.toPx()) / size.width)
                            val scale = lerp(1f, maxScale, press.value)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = {
                            drawRect(thumbSurface)
                            glassEdge(size.minDimension / 2f, ios.isDark)
                        },
                    ),
            )

            // 2. Konten tab di atas indikator kaca dengan translationZ agar ikon & teks selalu terlihat jelas
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationZ = 1f }
                    .pointerInput(tabStridePx, maxOffsetPx, selected) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging = true
                                onPressChange(true)
                            },
                            onDragEnd = {
                                isDragging = false
                                onPressChange(false)
                                snapToNearestTab()
                            },
                            onDragCancel = {
                                isDragging = false
                                onPressChange(false)
                                snapToNearestTab()
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val nextValue = (indicatorPx.value + dragAmount).coerceIn(0f, maxOffsetPx)
                                scope.launch {
                                    indicatorPx.snapTo(nextValue)
                                }
                            },
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(TabGap),
            ) {
                Tab.entries.forEach { tab ->
                    val iconVector = icons[tab]
                    if (iconVector != null) {
                        TabItem(
                            tab = tab,
                            icon = iconVector,
                            active = tab == highlightedTab,
                            onClick = { onSelect(tab) },
                            onPressChange = onPressChange,
                        )
                    }
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
        targetValue = if (active) ios.blue else ios.secondaryLabel,
        label = "tabTint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "iconScale",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
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
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            color = content,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}
