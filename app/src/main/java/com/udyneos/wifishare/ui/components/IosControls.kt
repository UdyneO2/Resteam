package com.udyneos.wifishare.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.udyneos.wifishare.ui.theme.Ios
import kotlinx.coroutines.delay

/** Switch ala iOS: 51x31, knob putih, hijau saat aktif. */
@Composable
fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val ios = Ios.colors
    val track by animateColorAsState(
        if (checked) ios.green else ios.separator.copy(alpha = 0.35f),
        label = "switchTrack",
    )
    val knobX by animateDpAsState(if (checked) 20.dp else 0.dp, label = "switchKnob")

    Box(
        modifier = Modifier
            .width(51.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(50))
            .background(track.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(
                enabled = enabled,
                interactionSource = MutableInteractionSource(),
                indication = null,
            ) { onCheckedChange(!checked) }
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = knobX)
                .size(27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/** Judul kecil huruf besar di atas grup, seperti Settings iOS. */
@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = Ios.colors.secondaryLabel,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 6.dp),
    )
}

@Composable
fun SectionFooter(text: String) {
    Text(
        text = text,
        color = Ios.colors.secondaryLabel,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 6.dp),
    )
}

/**
 * Grup baris berbentuk kartu kaca. Antar baris dipisah garis tipis. Kartu masuk dengan
 * geser + membesar + muncul pelan, [delayMillis] dipakai untuk berurutan (stagger).
 *
 * Kartu mengekspor backdrop-nya (exportedBackdrop) lewat [LocalGroupBackdrop], jadi tombol dan
 * slider di dalamnya membiaskan kartu itu sendiri. Memakai layerBackdrop biasa di sini akan loop.
 */
@Composable
fun GlassGroup(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val ios = Ios.colors
    val tint = glassTint()
    val inner = rememberLayerBackdrop()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow))
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .graphicsLayer {
                val p = progress.value
                alpha = p.coerceIn(0f, 1f)
                translationY = (1f - p) * 32.dp.toPx()
                val scale = 0.96f + 0.04f * p
                scaleX = scale
                scaleY = scale
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(22.dp) },
                effects = {
                    vibrancy()
                    blur(14.dp.toPx())
                },
                exportedBackdrop = inner,
                onDrawSurface = {
                    drawRect(tint)
                    glassEdge(22.dp.toPx(), ios.isDark)
                },
            ),
    ) {
        CompositionLocalProvider(LocalGroupBackdrop provides inner) {
            content()
        }
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = Ios.colors.separator,
    )
}

@Composable
fun ValueRow(
    title: String,
    value: String? = null,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val ios = Ios.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ios.label, fontSize = 17.sp)
            if (subtitle != null) {
                Text(subtitle, color = ios.secondaryLabel, fontSize = 13.sp)
            }
        }
        if (value != null) {
            Text(value, color = ios.secondaryLabel, fontSize = 17.sp)
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun LargeTitle(text: String) {
    Text(
        text = text,
        color = Ios.colors.label,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
    )
}
