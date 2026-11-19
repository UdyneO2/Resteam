package com.udyneos.glasslauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.udyneos.glasslauncher.data.AppInfo
import com.udyneos.glasslauncher.ui.glass.glass

private val Danger = Color(0xFFFF453A)

@Composable
fun AppContextMenu(
    app: AppInfo,
    backdrop: Backdrop,
    blurDp: Float,
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(250.dp)
                // swallow taps so touching the card doesn't dismiss it
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .glass(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(26.dp),
                    blur = blurDp.dp,
                    tint = Color.Black.copy(alpha = 0.22f),
                    lensHeight = 10.dp,
                    lensAmount = 20.dp,
                ),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)),
                )
                Text(
                    app.label,
                    style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
            MenuRow("App info", Color.White, onAppInfo)
            HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
            MenuRow("Uninstall", Danger, onUninstall)
        }
    }
}

@Composable
private fun MenuRow(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        style = TextStyle(color = color, fontSize = 16.sp),
    )
}
