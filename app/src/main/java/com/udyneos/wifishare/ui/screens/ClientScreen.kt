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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.udyneos.wifishare.data.AppSettings
import com.udyneos.wifishare.data.ClientState
import com.udyneos.wifishare.ui.components.GlassGroup
import com.udyneos.wifishare.ui.components.GlassScrollScreen
import com.udyneos.wifishare.ui.components.LiquidButton
import com.udyneos.wifishare.ui.components.RowDivider
import com.udyneos.wifishare.ui.components.SectionFooter
import com.udyneos.wifishare.ui.components.SectionHeader
import com.udyneos.wifishare.ui.components.TextEditRequest
import com.udyneos.wifishare.ui.components.ValueRow
import com.udyneos.wifishare.ui.components.parseWifiQr
import com.udyneos.wifishare.ui.theme.Ios

/**
 * Tab Klien: menyambung HP ini ke jaringan WiFi Direct milik HP lain (yang sedang berbagi).
 * Isi nama dan kata sandi dari tab WiFi di HP host, atau tempel teks QR berformat WIFI:...
 */
@Composable
fun ClientScreen(
    wallpaper: Backdrop,
    settings: AppSettings,
    state: ClientState,
    sharing: Boolean,
    bottomInset: Dp,
    onConnect: (ssid: String, passphrase: String) -> Unit,
    onDisconnect: () -> Unit,
    onEdit: (TextEditRequest) -> Unit,
) {
    val ios = Ios.colors
    val clipboard = LocalClipboardManager.current

    var ssid by remember { mutableStateOf(settings.clientSsid) }
    var pass by remember { mutableStateOf(settings.clientPassphrase) }
    var port by remember { mutableStateOf(settings.clientProxyPort) }
    var pasteHint by remember { mutableStateOf<String?>(null) }

    val busy = state.connecting || state.connected
    val canConnect = !sharing && !state.connecting && ssid.isNotBlank() && pass.length >= 8

    val accent = when {
        state.connected -> ios.green
        state.connecting -> ios.orange
        else -> ios.blue
    }

    GlassScrollScreen(title = "Klien", wallpaper = wallpaper, bottomInset = bottomInset) {
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
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.connected) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = when {
                        state.connected -> "Tersambung"
                        state.connecting -> "Menyambung…"
                        else -> "Sambung ke HP lain"
                    },
                    color = ios.label,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when {
                        state.connected || state.connecting -> state.ssid
                        else -> "Pakai jaringan WiFi Direct dari HP yang sedang berbagi"
                    },
                    color = ios.secondaryLabel,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            RowDivider()
            Box(Modifier.fillMaxWidth().padding(12.dp)) {
                if (busy) {
                    LiquidButton(
                        label = "Putuskan",
                        onClick = onDisconnect,
                        backdrop = wallpaper,
                        modifier = Modifier.fillMaxWidth(),
                        tint = ios.red,
                    )
                } else {
                    LiquidButton(
                        label = "Sambungkan",
                        onClick = {
                            settings.clientSsid = ssid
                            settings.clientPassphrase = pass
                            onConnect(ssid, pass)
                        },
                        backdrop = wallpaper,
                        modifier = Modifier.fillMaxWidth(),
                        tint = ios.blue,
                        enabled = canConnect,
                    )
                }
            }
        }

        if (state.error != null) {
            Text(
                text = state.error,
                color = ios.red,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
            )
        }

        SectionHeader("Jaringan tujuan")
        GlassGroup(wallpaper, delayMillis = 70) {
            ValueRow(
                title = "Nama",
                value = ssid.ifEmpty { "—" },
                onClick = {
                    onEdit(
                        TextEditRequest(
                            title = "Nama jaringan",
                            initial = ssid,
                            validate = { if (it.isBlank()) "Tidak boleh kosong" else null },
                            onSave = { ssid = it; settings.clientSsid = it },
                        ),
                    )
                },
            )
            RowDivider()
            ValueRow(
                title = "Kata sandi",
                value = if (pass.isEmpty()) "—" else "•".repeat(pass.length.coerceAtMost(12)),
                onClick = {
                    onEdit(
                        TextEditRequest(
                            title = "Kata sandi",
                            initial = pass,
                            validate = { if (it.trim().length !in 8..63) "Panjang 8 sampai 63 karakter" else null },
                            onSave = { pass = it; settings.clientPassphrase = it },
                        ),
                    )
                },
            )
            RowDivider()
            ValueRow(
                title = "Port proxy host",
                value = port.toString(),
                onClick = {
                    onEdit(
                        TextEditRequest(
                            title = "Port proxy host",
                            initial = port.toString(),
                            keyboardType = KeyboardType.Number,
                            validate = { input ->
                                val v = input.trim().toIntOrNull()
                                if (v == null || v !in AppSettings.PORT_MIN..AppSettings.PORT_MAX) {
                                    "Isi angka ${AppSettings.PORT_MIN} sampai ${AppSettings.PORT_MAX}"
                                } else null
                            },
                            onSave = { port = it.toInt(); settings.clientProxyPort = port },
                        ),
                    )
                },
            )
            RowDivider()
            Box(Modifier.fillMaxWidth().padding(12.dp)) {
                LiquidButton(
                    label = "Tempel dari clipboard",
                    onClick = {
                        val parsed = clipboard.getText()?.text?.let { parseWifiQr(it) }
                        if (parsed != null) {
                            ssid = parsed.first
                            pass = parsed.second
                            settings.clientSsid = ssid
                            settings.clientPassphrase = pass
                            pasteHint = "Nama dan kata sandi terisi dari clipboard"
                        } else {
                            pasteHint = "Clipboard tidak berisi teks WIFI:… yang valid"
                        }
                    },
                    backdrop = wallpaper,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.ContentPaste,
                )
            }
        }
        SectionFooter(
            when {
                sharing -> "HP ini sedang berbagi. Hentikan berbagi di tab WiFi dulu sebelum menyambung sebagai klien."
                pasteHint != null -> pasteHint!!
                else -> "Isi nama dan kata sandi dari tab WiFi di HP host. Android akan menampilkan dialog konfirmasi sebelum menyambung."
            },
        )

        AnimatedVisibility(
            visible = state.connected,
            enter = fadeIn(tween(350)) + expandVertically(spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut(tween(200)) + shrinkVertically(),
        ) {
            Column {
                SectionHeader("Proxy")
                GlassGroup(wallpaper) {
                    ValueRow("Gateway", value = state.gateway.ifEmpty { "—" })
                    RowDivider()
                    ValueRow(
                        title = "Alamat proxy",
                        value = "${state.gateway}:$port",
                        onClick = { clipboard.setText(AnnotatedString("${state.gateway}:$port")) },
                    )
                }
                SectionFooter("Jaringan ini tidak punya internet sendiri. Di pengaturan WiFi HP ini, atur proxy manual ke alamat di atas. Ketuk alamat untuk menyalin.")
            }
        }
    }
}
