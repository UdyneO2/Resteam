package com.udyneos.wifishare.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.udyneos.wifishare.data.ShareState
import com.udyneos.wifishare.ui.components.GlassGroup
import com.udyneos.wifishare.ui.components.GlassScrollScreen
import com.udyneos.wifishare.ui.components.IosSwitch
import com.udyneos.wifishare.ui.components.LiquidButton
import com.udyneos.wifishare.ui.components.QrCode
import com.udyneos.wifishare.ui.components.RowDivider
import com.udyneos.wifishare.ui.components.SectionFooter
import com.udyneos.wifishare.ui.components.SectionHeader
import com.udyneos.wifishare.ui.components.ValueRow
import com.udyneos.wifishare.ui.components.wifiQrPayload
import com.udyneos.wifishare.ui.theme.Ios

@Composable
fun WifiScreen(
    wallpaper: Backdrop,
    state: ShareState,
    onToggle: (Boolean) -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    val ios = Ios.colors
    val clipboard = LocalClipboardManager.current

    GlassScrollScreen(title = "WiFi", wallpaper = wallpaper, bottomInset = bottomInset) {
        // kartu utama: ikon, status, dan saklar bagikan
        GlassGroup(wallpaper, Modifier.padding(top = 8.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(if (state.active) ios.green else ios.blue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.active) Icons.Rounded.WifiTethering else Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (state.active) "Sedang berbagi" else "Berbagi internet",
                    color = ios.label,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (state.active) {
                        "${state.clients.size} perangkat tersambung"
                    } else {
                        "Pakai WiFi Direct untuk membagi koneksi data HP ini"
                    },
                    color = ios.secondaryLabel,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            RowDivider()
            ValueRow(
                title = "Bagikan internet",
                trailing = {
                    IosSwitch(
                        checked = state.active || state.busy,
                        onCheckedChange = onToggle,
                        enabled = !state.busy,
                    )
                },
            )
        }

        if (state.error != null) {
            Text(
                text = state.error,
                color = ios.red,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
            )
        }

        // QR hanya muncul saat berbagi aktif dan nama + sandi sudah ada
        AnimatedVisibility(
            visible = state.active && state.networkName.isNotEmpty() && state.passphrase.isNotEmpty(),
            enter = fadeIn(tween(350)) + expandVertically(spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut(tween(200)) + shrinkVertically(),
        ) {
            Column {
                SectionHeader("Sambung via QR")
                GlassGroup(wallpaper) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QrCode(
                            content = wifiQrPayload(state.networkName, state.passphrase),
                            modifier = Modifier.size(208.dp),
                        )
                    }
                }
                SectionFooter("Scan dengan kamera perangkat lain untuk menyambung ke jaringan. Proxy tetap harus diatur manual ke alamat di bawah.")
            }
        }

        SectionHeader("Jaringan")
        GlassGroup(wallpaper, delayMillis = 70) {
            ValueRow("Nama", value = state.networkName.ifEmpty { "—" })
            RowDivider()
            ValueRow(
                title = "Kata sandi",
                value = state.passphrase.ifEmpty { "—" },
                onClick = if (state.passphrase.isNotEmpty()) {
                    { clipboard.setText(AnnotatedString(state.passphrase)) }
                } else null,
            )
            RowDivider()
            ValueRow(
                title = "Proxy",
                value = state.proxyAddress.ifEmpty { "—" },
                onClick = if (state.proxyAddress.isNotEmpty()) {
                    { clipboard.setText(AnnotatedString(state.proxyAddress)) }
                } else null,
            )
            RowDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LiquidButton(
                    label = "Salin sandi",
                    onClick = { clipboard.setText(AnnotatedString(state.passphrase)) },
                    backdrop = wallpaper,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.ContentCopy,
                    enabled = state.passphrase.isNotEmpty(),
                )
                LiquidButton(
                    label = "Salin proxy",
                    onClick = { clipboard.setText(AnnotatedString(state.proxyAddress)) },
                    backdrop = wallpaper,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.ContentCopy,
                    enabled = state.proxyAddress.isNotEmpty(),
                )
            }
        }
        SectionFooter("Ketuk kata sandi atau proxy untuk menyalin. Di perangkat tujuan, sambung ke jaringan di atas lalu atur proxy manual ke alamat proxy.")

        SectionHeader("Perangkat tersambung")
        GlassGroup(wallpaper, delayMillis = 140) {
            if (state.clients.isEmpty()) {
                ValueRow(title = "Belum ada perangkat", subtitle = "Perangkat yang tersambung akan muncul di sini")
            } else {
                state.clients.forEachIndexed { index, device ->
                    if (index > 0) RowDivider()
                    ValueRow(title = device.name, value = device.address)
                }
            }
        }
    }
}
