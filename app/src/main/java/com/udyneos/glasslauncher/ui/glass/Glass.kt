package com.udyneos.glasslauncher.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * Cheap stand-in for [glass] on things that are drawn *inside* the backdrop
 * layer itself (like the App Library tiles). A layer can't sample itself,
 * so those get a plain translucent fill with a thin highlight edge.
 */
fun Modifier.frosted(
    shape: Shape = RoundedCornerShape(24.dp),
    alpha: Float = 0.18f,
): Modifier = this
    .background(Color.White.copy(alpha = alpha), shape)
    .border(1.dp, Color.White.copy(alpha = 0.22f), shape)

/**
 * One place for the glass look so the dock, library tiles and settings
 * sheet all feel like the same material.
 *
 * Lens refraction needs Android 13+. Older devices still get the
 * blur + vibrancy + tint, which is close enough to frosted glass.
 */
fun Modifier.glass(
    backdrop: Backdrop,
    shape: CornerBasedShape = RoundedCornerShape(28.dp),
    blur: Dp = 10.dp,
    tint: Color = Color.White.copy(alpha = 0.16f),
    lensHeight: Dp = 10.dp,
    lensAmount: Dp = 20.dp,
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(blur.toPx())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lens(lensHeight.toPx(), lensAmount.toPx())
        }
    },
    onDrawSurface = { drawRect(tint) },
)
