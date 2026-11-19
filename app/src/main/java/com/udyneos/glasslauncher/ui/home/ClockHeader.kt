package com.udyneos.glasslauncher.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TAPS_NEEDED = 5
private const val TAP_WINDOW_MS = 2_500L

/**
 * Big lock-screen style clock. There is no visible hint, but tapping it
 * five times in a row (within a couple of seconds) opens the hidden settings.
 */
@Composable
fun ClockHeader(
    onSecretUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000L - (System.currentTimeMillis() % 1_000L))
        }
    }

    var taps by remember { mutableIntStateOf(0) }
    var firstTapAt by remember { mutableLongStateOf(0L) }
    val haptics = LocalHapticFeedback.current

    val shadow = Shadow(Color.Black.copy(alpha = 0.25f), blurRadius = 12f)

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                val t = System.currentTimeMillis()
                if (taps == 0 || t - firstTapAt > TAP_WINDOW_MS) {
                    taps = 1
                    firstTapAt = t
                } else {
                    taps += 1
                }
                if (taps >= TAPS_NEEDED) {
                    taps = 0
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSecretUnlocked()
                }
            }
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            style = TextStyle(
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                shadow = shadow,
            ),
        )
        Text(
            text = now.format(DateTimeFormatter.ofPattern("H:mm")),
            style = TextStyle(
                color = Color.White,
                fontSize = 76.sp,
                fontWeight = FontWeight.Bold,
                shadow = shadow,
            ),
        )
    }
}
