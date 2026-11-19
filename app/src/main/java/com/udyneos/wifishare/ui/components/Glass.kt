package com.udyneos.wifishare.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.udyneos.wifishare.ui.theme.Ios
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.sin

// Efek RenderEffect baru jalan di Android 12+, lens (RuntimeShader) butuh Android 13+
internal val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
internal val supportsLens = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** Kepekatan permukaan kaca (0..1), diatur dari Pengaturan. Makin kecil makin transparan. */
val LocalGlassOpacity = compositionLocalOf { 0.35f }

/**
 * Backdrop milik kartu kaca terdekat (hasil exportedBackdrop). Tombol dan slider di dalam kartu
 * memakainya supaya membiaskan kartunya, bukan hanya wallpaper (kaca di atas kaca).
 */
val LocalGroupBackdrop = compositionLocalOf<Backdrop?> { null }

/** Warna permukaan kaca. [boost] menambah kepekatan untuk elemen yang perlu lebih terbaca. */
@Composable
fun glassTint(boost: Float = 0f): Color {
    val ios = Ios.colors
    val a = (LocalGlassOpacity.current + boost).coerceIn(0.05f, 0.95f)
    // di bawah Android 12 nggak ada blur, jadi dibuat lebih pekat supaya teks tetap kebaca
    return ios.cardTint.copy(alpha = if (supportsBlur) a else max(a, 0.92f))
}

/**
 * Garis tepi tipis di pinggir kaca. Karena kaca sekarang lebih transparan, tepi ini yang
 * menjaga bentuk kartu tetap terbaca. Terang di sudut kiri atas, memudar ke kanan bawah.
 */
internal fun DrawScope.glassEdge(radiusPx: Float, isDark: Boolean) {
    val stroke = 1.dp.toPx()
    val base = if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.85f)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(base, base.copy(alpha = base.alpha * 0.2f)),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radiusPx - stroke / 2f),
        style = Stroke(stroke),
    )
}

/**
 * Permukaan kaca. Backdrop yang dipakai harus berasal dari layer yang BUKAN
 * parent langsung dari composable ini, kalau tidak bakal menggambar dirinya sendiri.
 */
@Composable
fun Modifier.glassSurface(
    backdrop: Backdrop,
    corner: Dp = 22.dp,
    refract: Boolean = false,
): Modifier {
    val ios = Ios.colors
    val tint = glassTint()

    return drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedCornerShape(corner) },
        effects = {
            vibrancy()
            blur(14.dp.toPx())
            if (refract && supportsLens) {
                lens(refractionHeight = 14.dp.toPx(), refractionAmount = 28.dp.toPx())
            }
        },
        onDrawSurface = {
            drawRect(tint)
            glassEdge(corner.toPx(), ios.isDark)
        },
    )
}

/**
 * Wallpaper berwarna di belakang semua konten. Bagian ini yang bikin kaca terlihat "hidup",
 * karena blur di atas warna polos tidak kelihatan apa-apa. Bila [animate] aktif, titik warna
 * melayang pelan sehingga kaca di atasnya ikut berubah warna.
 */
@Composable
fun GlassWallpaper(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ios = Ios.colors
    val base = if (ios.isDark) Color(0xFF0B0B12) else Color(0xFFEFF2FB)

    // State dibaca di dalam drawBehind, jadi animasi hanya memicu gambar ulang, bukan recomposition
    val phase: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "wallpaper").animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(32_000, easing = LinearEasing)),
            label = "wallpaperPhase",
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                val w = size.width
                val h = size.height
                val p = phase.value
                // semua kelipatan bulat dari p, jadi putarannya menyambung mulus
                blob(
                    Offset(w * (0.12f + 0.08f * sin(p)), h * (0.10f + 0.05f * cos(p))),
                    w * 0.70f, ios.blue, if (ios.isDark) 0.55f else 0.38f,
                )
                blob(
                    Offset(w * (0.95f + 0.07f * cos(p + 1.5f)), h * (0.34f + 0.06f * sin(p + 1.5f))),
                    w * 0.60f, Color(0xFFBF5AF2), if (ios.isDark) 0.45f else 0.28f,
                )
                blob(
                    Offset(w * (0.20f + 0.09f * sin(p + 3f)), h * (0.72f + 0.05f * cos(p + 3f))),
                    w * 0.65f, Color(0xFF30D5C8), if (ios.isDark) 0.40f else 0.30f,
                )
                blob(
                    Offset(w * (0.90f + 0.06f * cos(p + 4.5f)), h * (0.95f + 0.04f * sin(p + 4.5f))),
                    w * 0.55f, Color(0xFFFF9F0A), if (ios.isDark) 0.30f else 0.22f,
                )
            },
    ) {
        content()
    }
}

private fun DrawScope.blob(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * Dua layer backdrop:
 * - wallpaper: hanya wallpaper berwarna (dipakai kartu).
 * - screen: wallpaper + seluruh konten layar, TANPA overlay (dipakai tab bar).
 *
 * Overlay HARUS berada di luar Box yang ber-layerBackdrop(screen). Kalau overlay menjadi
 * anak dari layer yang ia gambar sendiri, RenderNode-nya merekam dirinya sendiri tanpa
 * henti dan RenderThread crash stack overflow di SkiaDisplayList::prepareListAndChildren.
 */
@Composable
fun GlassHost(
    animateBackground: Boolean,
    overlay: @Composable BoxScope.(screen: Backdrop) -> Unit,
    content: @Composable (wallpaper: Backdrop) -> Unit,
) {
    val screen = rememberLayerBackdrop()
    val wallpaper = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(screen)) {
            GlassWallpaper(Modifier.layerBackdrop(wallpaper), animate = animateBackground) {}
            content(wallpaper)
        }
        overlay(screen)
    }
}
