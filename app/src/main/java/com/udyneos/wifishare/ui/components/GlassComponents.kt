package com.udyneos.wifishare.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.udyneos.wifishare.ui.theme.Ios
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Tombol liquid glass transparan (kapsul). Permukaannya hampir bening, bentuknya dibiaskan
 * lens, dan membesar mengikuti jari saat ditekan (layerBlock, jadi backdrop tidak ikut membesar).
 *
 * Bila ada kartu kaca di atasnya, tombol membiaskan kartu itu ([LocalGroupBackdrop]);
 * kalau tidak, memakai [backdrop]. [tint] diisi untuk tombol aksi utama (biru) atau bahaya (merah).
 */
@Composable
fun LiquidButton(
    label: String,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
) {
    val ios = Ios.colors
    val source = LocalGroupBackdrop.current ?: backdrop
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    val tinted = tint != Color.Unspecified
    val clear = ios.cardTint.copy(alpha = 0.10f)
    val contentColor = if (tinted) Color.White else ios.label

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = source,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    if (supportsLens) lens(12.dp.toPx(), 24.dp.toPx())
                },
                layerBlock = {
                    val maxScale = (size.width + 14.dp.toPx()) / size.width
                    val scale = lerp(1f, maxScale, press.value)
                    scaleX = scale
                    scaleY = scale
                    alpha = if (enabled) 1f else 0.45f
                },
                onDrawSurface = {
                    if (tinted) {
                        // Hue supaya warna backdrop menyesuaikan tint, lalu lapisan tint tipis
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.65f))
                    } else {
                        drawRect(clear)
                        glassEdge(size.minDimension / 2f, ios.isDark)
                    }
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .pointerInput(scope) {
                val animationSpec = spring<Float>(0.5f, 300f, 0.001f)
                awaitEachGesture {
                    awaitFirstDown()
                    scope.launch { press.animateTo(1f, animationSpec) }
                    waitForUpOrCancellation()
                    scope.launch { press.animateTo(0f, animationSpec) }
                }
            }
            .height(48.dp)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Slider kaca. Jempolnya membiaskan latar DAN trek sekaligus lewat rememberCombinedBackdrop,
 * jadi trek terlihat melengkung di bawah lensa. [value] dalam 0..1.
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val ios = Ios.colors
    val source = LocalGroupBackdrop.current ?: backdrop
    val trackBackdrop = rememberLayerBackdrop()
    val combined = rememberCombinedBackdrop(source, trackBackdrop)
    val currentValue by rememberUpdatedState(value)
    val currentChange by rememberUpdatedState(onValueChange)
    val thumbWidth = 56.dp
    val thumbHeight = 32.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val travelPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            (maxWidth - thumbWidth).toPx()
        }

        // trek: abu-abu penuh + biru sampai posisi jempol
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .fillMaxWidth()
                .height(6.dp)
                .background(ios.separator.copy(alpha = 0.45f), CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0.02f, 1f))
                    .background(ios.blue, CircleShape),
            )
        }

        // jempol
        Box(
            Modifier
                .offset { IntOffset((travelPx * value.coerceIn(0f, 1f)).roundToInt(), 0) }
                .drawBackdrop(
                    backdrop = combined,
                    shape = { CircleShape },
                    effects = {
                        lens(
                            refractionHeight = 12.dp.toPx(),
                            refractionAmount = 16.dp.toPx(),
                            chromaticAberration = true,
                        )
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.14f)) },
                )
                .pointerInput(travelPx) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (travelPx > 0f) {
                            currentChange((currentValue + dragAmount / travelPx).coerceIn(0f, 1f))
                        }
                    }
                }
                .size(thumbWidth, thumbHeight),
        )
    }
}
