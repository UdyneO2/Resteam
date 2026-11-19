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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

private val DashBg = Color(0xFF0B1222)
private val DashCard = Color(0xFF151F36)
private val DashElevated = Color(0xFF283552)
private val DashBorder = Color(0xFF23304D)
private val DashText = Color(0xFFF8FAFC)
private val DashMuted = Color(0xFF94A3B8)
private val DashBlue = Color(0xFF3B82F6)
private val DashTeal = Color(0xFF14B8A6)

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
    var filterQuery by remember { mutableStateOf("") }
    var pasteHint by remember { mutableStateOf<String?>(null) }

    val busy = state.connecting || state.connected
    val canConnect = !sharing && !state.connecting && ssid.isNotBlank() && pass.length >= 8

    val accent = when {
        state.connected -> ios.green
        state.connecting -> ios.orange
        else -> ios.blue
    }

    GlassScrollScreen(title = "Klien", wallpaper = wallpaper, bottomInset = bottomInset) {
        // Header & Search / Filter bar ala gambar referensi
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = "WiFi Direct Client",
                    color = DashMuted,
                    fontSize = 13.sp,
                )
                Text(
                    text = "Klien",
                    color = DashText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            OutlinedTextField(
                value = filterQuery,
                onValueChange = { value ->
                    filterQuery = value
                    if (value.startsWith("WIFI:", ignoreCase = true)) {
                        parseWifiQr(value)?.let { parsed ->
                            ssid = parsed.first
                            pass = parsed.second
                            settings.clientSsid = ssid
                            settings.clientPassphrase = pass
                            pasteHint = "Nama dan kata sandi terisi dari teks WIFI:…"
                        }
                    }
                },
                singleLine = true,
                placeholder = {
                    Text("Cari atau tempel WIFI:...", color = DashMuted, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = DashMuted,
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DashCard,
                    unfocusedContainerColor = DashCard,
                    focusedBorderColor = DashBlue.copy(alpha = 0.65f),
                    unfocusedBorderColor = DashBorder,
                    focusedTextColor = DashText,
                    unfocusedTextColor = DashText,
                    cursorColor = DashBlue,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Kartu Status Utama + Tombol Aksi
        GlassGroup(wallpaper, Modifier.padding(top = 8.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DashCard)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.connected) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = when {
                        state.connected -> "Tersambung"
                        state.connecting -> "Menyambung…"
                        else -> "Sambung ke HP lain"
                    },
                    color = DashText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when {
                        state.connected || state.connecting -> state.ssid
                        else -> "Pakai jaringan WiFi Direct dari HP yang sedang berbagi"
                    },
                    color = DashMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )

                // Panel preview ringkas bergaya blok platform di gambar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DashElevated)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (ssid.isNotEmpty()) ssid else "Belum ada SSID target",
                                color = DashText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Proxy Port: $port",
                                color = DashMuted,
                                fontSize = 12.sp,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(3) { idx ->
                                val activeDot = when {
                                    state.connected -> true
                                    state.connecting -> idx <= 1
                                    else -> idx == 0
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (activeDot) Color(0xFF22C55E) else DashBg),
                                )
                            }
                        }
                    }
                }
            }
            RowDivider()
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(DashCard)
                    .padding(12.dp),
            ) {
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

        // Grid 2 Kolom Cepat ala bagian "Popular" pada gambar
        SectionHeader("Pintasan koneksi")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickShortcutCard(
                title = "Nama SSID",
                subtitle = ssid.ifEmpty { "Atur SSID" },
                icon = Icons.Rounded.Router,
                badgeColor = DashBlue,
                modifier = Modifier.weight(1f),
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
            QuickShortcutCard(
                title = "Kata sandi",
                subtitle = if (pass.isEmpty()) "8–63 kar." else "•".repeat(pass.length.coerceAtMost(10)),
                icon = Icons.Rounded.VpnKey,
                badgeColor = DashTeal,
                modifier = Modifier.weight(1f),
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
        }

        // Baris Metrik Status ala bagian bawah gambar
        SectionHeader("Ringkasan status")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusMetricRow(
                dotColor = DashBlue,
                title = "Port Proxy Host",
                subtitle = "Port lokal tujuan di jaringan host",
                metric = port.toString(),
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
            StatusMetricRow(
                dotColor = if (state.connected) Color(0xFF22C55E) else Color(0xFF0EA5E9),
                title = "Status Koneksi",
                subtitle = if (sharing) "Mode berbagi aktif di HP ini" else state.ssid.ifEmpty { "Siap menyambung" },
                metric = when {
                    state.connected -> "Aktif"
                    state.connecting -> "Proses"
                    else -> "Siap"
                },
            )
        }

        // Detail Jaringan & Clipboard
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(DashCard)
                    .padding(12.dp),
            ) {
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

@Composable
private fun QuickShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DashCard)
            .border(1.dp, DashBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = DashText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = DashMuted,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusMetricRow(
    dotColor: Color,
    title: String,
    subtitle: String,
    metric: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DashCard)
            .border(1.dp, DashBorder, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DashText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = DashMuted,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Text(
            text = metric,
            color = DashText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
