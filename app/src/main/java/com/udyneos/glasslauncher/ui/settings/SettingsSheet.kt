package com.udyneos.glasslauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.udyneos.glasslauncher.BuildConfig
import com.udyneos.glasslauncher.ui.glass.glass

/**
 * The hidden settings. Nothing on the home screen points here, the clock
 * easter egg is the only way in.
 */
@Composable
fun SettingsSheet(
    backdrop: Backdrop,
    showLabels: Boolean,
    blurDp: Float,
    tintAlpha: Float,
    onShowLabels: (Boolean) -> Unit,
    onBlur: (Float) -> Unit,
    onTint: (Float) -> Unit,
    onReloadApps: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .glass(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(34.dp),
                    blur = blurDp.dp,
                    tint = Color.Black.copy(alpha = 0.28f),
                    lensHeight = 14.dp,
                    lensAmount = 28.dp,
                )
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "Settings",
                style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold),
            )

            ToggleRow("Show app names", showLabels, onShowLabels)

            SliderRow("Glass blur", blurDp, 0f..24f, onBlur)
            SliderRow("Glass tint", tintAlpha, 0f..0.4f, onTint)

            Text(
                "Reload app list",
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedButton()
                    .clickable(onClick = onReloadApps)
                    .padding(14.dp),
                style = TextStyle(color = Color.White, fontSize = 16.sp),
            )

            Text(
                "Glass Launcher ${BuildConfig.VERSION_NAME}",
                style = TextStyle(color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp),
            )
        }
    }
}

private fun Modifier.frostedButton(): Modifier =
    this.background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = TextStyle(color = Color.White, fontSize = 16.sp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF30D158)),
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = TextStyle(color = Color.White, fontSize = 16.sp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
    }
}
