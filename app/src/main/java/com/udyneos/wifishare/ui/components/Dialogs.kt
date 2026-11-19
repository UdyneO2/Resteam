package com.udyneos.wifishare.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.udyneos.wifishare.ui.theme.Ios

/** Permintaan membuka dialog edit satu baris teks. [validate] mengembalikan pesan error atau null. */
class TextEditRequest(
    val title: String,
    val initial: String,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val validate: (String) -> String? = { null },
    val onSave: (String) -> Unit,
)

/**
 * Dialog kaca. Ini overlay biasa di GlassHost, BUKAN Dialog/AlertDialog bawaan: kaca hanya bisa
 * membiaskan layar kalau digambar di window yang sama, dan harus berada di luar layer `screen`.
 *
 * Panel mengekspor backdrop-nya (exportedBackdrop) supaya kolom teks dan tombol di dalamnya
 * membiaskan panel itu sendiri (kaca di atas kaca, seperti tutorial Glass Bottom Sheet).
 */
@Composable
fun BoxScope.GlassTextDialog(
    backdrop: Backdrop,
    request: TextEditRequest?,
    onDismiss: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (request != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "dialogProgress",
    )
    // simpan permintaan terakhir supaya panel tetap tampil selama animasi keluar
    var last by remember { mutableStateOf<TextEditRequest?>(null) }
    LaunchedEffect(request) { if (request != null) last = request }

    BackHandler(enabled = request != null) { onDismiss() }

    val current = request ?: last
    if (current == null || (request == null && progress < 0.01f)) return

    key(current) {
        DialogPanel(backdrop, current, progress, onDismiss)
    }
}

@Composable
private fun BoxScope.DialogPanel(
    backdrop: Backdrop,
    request: TextEditRequest,
    progress: Float,
    onDismiss: () -> Unit,
) {
    val ios = Ios.colors
    val panelBackdrop = rememberLayerBackdrop()
    var text by remember { mutableStateOf(request.initial) }
    val error = request.validate(text)
    val focus = remember { FocusRequester() }
    val surface = ios.cardTint.copy(alpha = if (ios.isDark) 0.62f else 0.55f)
    val fieldTint = ios.cardTint.copy(alpha = 0.18f)

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    // scrim peredam latar; ketuk untuk menutup
    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = 0.30f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    )

    CompositionLocalProvider(LocalGroupBackdrop provides panelBackdrop) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .imePadding()
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(36.dp) },
                    effects = {
                        colorControls(brightness = if (ios.isDark) -0.04f else 0.04f, saturation = 1.4f)
                        blur(8.dp.toPx())
                        if (supportsLens) lens(20.dp.toPx(), 40.dp.toPx(), true)
                    },
                    exportedBackdrop = panelBackdrop,
                    layerBlock = {
                        val p = progress.coerceIn(0f, 1f)
                        val scale = 0.88f + 0.12f * p
                        scaleX = scale
                        scaleY = scale
                        alpha = p
                    },
                    onDrawSurface = {
                        drawRect(surface)
                        glassEdge(36.dp.toPx(), ios.isDark)
                    },
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = request.title,
                color = ios.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))

            // kolom teks juga kaca, membiaskan panel
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = panelBackdrop,
                        shape = { RoundedCornerShape(16.dp) },
                        effects = { blur(2.dp.toPx()) },
                        onDrawSurface = { drawRect(fieldTint) },
                    )
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(color = ios.label, fontSize = 17.sp),
                    cursorBrush = SolidColor(ios.blue),
                    keyboardOptions = KeyboardOptions(keyboardType = request.keyboardType),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }

            if (error != null) {
                Text(
                    text = error,
                    color = ios.red,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 8.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LiquidButton(
                    label = "Batal",
                    onClick = onDismiss,
                    backdrop = panelBackdrop,
                    modifier = Modifier.weight(1f),
                )
                LiquidButton(
                    label = "Simpan",
                    onClick = {
                        request.onSave(text.trim())
                        onDismiss()
                    },
                    backdrop = panelBackdrop,
                    modifier = Modifier.weight(1f),
                    tint = ios.blue,
                    enabled = error == null,
                )
            }
        }
    }
}
