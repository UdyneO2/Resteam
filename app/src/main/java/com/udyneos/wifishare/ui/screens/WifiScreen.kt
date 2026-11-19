package com.udyneos.wifishare.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
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
import androidx.compose.ui.unit.Dp
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

private val WifiBg = Color(0xFF0B1222)
private val WifiCard = Color(0xFF151F36)
private val WifiElevated = Color(0xFF283552)
private val WifiBorder = Color(0xFF23304D)
private val WifiText = Color(0xFFF8FAFC)
private val WifiMuted = Color(0xFF94A3B8)
private val WifiBlue = Color(0xFF3B82F6)
private val WifiTeal = Color(0xFF14B8A6)

@Composable
fun WifiScreen(
    wallpaper: Backdrop,
    state: ShareState,
    onToggle: (Boolean) -> Unit,
    bottomInset: Dp,
) {
    val ios = Ios.colors
    val clipboard = LocalClipboardManager.current
    var filterQuery by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val filteredClients = remember(state.clients, filterQuery) {
        val q = filterQuery.trim()
        if (q.isEmpty()) state.clients
        else state.clients.filter {
            it.name.contains(q, ignoreCase = true) || it.address.contains(q, ignoreCase = true)
        }
    }

    GlassScrollScreen(title = "WiFi", wallpaper = wallpaper, bottomInset = bottomInset) {
        // Header & Search Bar ala gambar referensi
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = "WiFi Direct Host,",
                    color = WifiMuted,
                    fontSize = 13.sp,
                )
                Text(
                    text = "WiFi",
                    color = WifiText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                singleLine = true,
                placeholder = {
                    Text("Cari perangkat tersambung...", color = WifiMuted, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = WifiMuted,
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = WifiCard,
                    unfocusedContainerColor = WifiCard,
                    focusedBorderColor = WifiBlue.copy(alpha = 0.65f),
                    unfocusedBorderColor = WifiBorder,
                    focusedTextColor = WifiText,
                    unfocusedTextColor = WifiText,
                    cursorColor = WifiBlue,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Kartu utama: ikon, status, blok preview node, dan saklar bagikan
        GlassGroup(wallpaper, Modifier.padding(top = 8.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WifiCard)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(if (state.active) ios.green else ios.blue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.active) Icons.Rounded.WifiTethering else Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (state.active) "Sedang berbagi" else "Berbagi internet",
                    color = WifiText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (state.active) {
                        "${state.clients.size} perangkat tersambung"
                    } else {
                        "Pakai WiFi Direct untuk membagi koneksi data HP ini"
                    },
                    color = WifiMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )

                // Blok preview status ala bagian "Cloud Platform" pada gambar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(WifiElevated)
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.networkName.ifEmpty { "WiFi Direct Node" },
                                color = WifiText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                repeat(2) { rowIdx ->
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        repeat(4) { colIdx ->
                                            val activeCell = state.active && (rowIdx * 4 + colIdx) < (state.clients.size + 3).coerceAtMost(8)
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(
                                                        when {
                                                            activeCell -> Color(0xFF22C55E)
                                                            state.active -> WifiBlue.copy(alpha = 0.45f)
                                                            else -> WifiBg
                                                        },
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = if (state.proxyAddress.isNotEmpty()) {
                                "Proxy: ${state.proxyAddress}"
                            } else {
                                "Alamat proxy akan muncul saat berbagi aktif"
                            },
                            color = WifiMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            RowDivider()
            Box(Modifier.background(WifiCard)) {
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
        }

        if (state.error != null) {
            Text(
                text = state.error,
                color = ios.red,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
            )
        }

        // Grid 2 kolom pintasan salin cepat ala bagian "Popular" pada gambar
        SectionHeader("Pintasan cepat")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WifiQuickCard(
                title = "Kata Sandi",
                subtitle = if (state.passphrase.isNotEmpty()) "Ketuk untuk salin" else "Belum aktif",
                icon = Icons.Rounded.QrCode2,
                badgeColor = WifiBlue,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (state.passphrase.isNotEmpty()) {
                        clipboard.setText(AnnotatedString(state.passphrase))
                        copiedNotice = "Kata sandi disalin ke clipboard"
                    }
                },
            )
            WifiQuickCard(
                title = "Alamat Proxy",
                subtitle = state.proxyAddress.ifEmpty { "Menunggu host" },
                icon = Icons.Rounded.Devices,
                badgeColor = WifiTeal,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (state.proxyAddress.isNotEmpty()) {
                        clipboard.setText(AnnotatedString(state.proxyAddress))
                        copiedNotice = "Alamat proxy disalin ke clipboard"
                    }
                },
            )
        }

        // Baris metrik ringkas ala bagian bawah gambar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WifiMetricRow(
                dotColor = if (state.active) Color(0xFF22C55E) else WifiBlue,
                title = "Klien Terhubung",
                subtitle = copiedNotice ?: if (state.active) "Host WiFi Direct aktif" else "Host belum berjalan",
                metric = state.clients.size.toString(),
            )
            WifiMetricRow(
                dotColor = Color(0xFF0EA5E9),
                title = "Status Proxy",
                subtitle = state.proxyAddress.ifEmpty { "Port belum terbuka" },
                metric = if (state.active) "ON" else "OFF",
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
                            .background(WifiCard)
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
            Box(Modifier.background(WifiCard)) {
                ValueRow("Nama", value = state.networkName.ifEmpty { "—" })
            }
            RowDivider()
            Box(Modifier.background(WifiCard)) {
                ValueRow(
                    title = "Kata sandi",
                    value = state.passphrase.ifEmpty { "—" },
                    onClick = if (state.passphrase.isNotEmpty()) {
                        {
                            clipboard.setText(AnnotatedString(state.passphrase))
                            copiedNotice = "Kata sandi disalin ke clipboard"
                        }
                    } else null,
                )
            }
            RowDivider()
            Box(Modifier.background(WifiCard)) {
                ValueRow(
                    title = "Proxy",
                    value = state.proxyAddress.ifEmpty { "—" },
                    onClick = if (state.proxyAddress.isNotEmpty()) {
                        {
                            clipboard.setText(AnnotatedString(state.proxyAddress))
                            copiedNotice = "Alamat proxy disalin ke clipboard"
                        }
                    } else null,
                )
            }
            RowDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WifiCard)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LiquidButton(
                    label = "Salin sandi",
                    onClick = {
                        clipboard.setText(AnnotatedString(state.passphrase))
                        copiedNotice = "Kata sandi disalin ke clipboard"
                    },
                    backdrop = wallpaper,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.ContentCopy,
                    enabled = state.passphrase.isNotEmpty(),
                )
                LiquidButton(
                    label = "Salin proxy",
                    onClick = {
                        clipboard.setText(AnnotatedString(state.proxyAddress))
                        copiedNotice = "Alamat proxy disalin ke clipboard"
                    },
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
            if (filteredClients.isEmpty()) {
                Box(Modifier.background(WifiCard)) {
                    ValueRow(
                        title = if (filterQuery.isNotBlank()) "Tidak ada hasil cocok" else "Belum ada perangkat",
                        subtitle = if (filterQuery.isNotBlank()) {
                            "Coba kata kunci lain untuk \"$filterQuery\""
                        } else {
                            "Perangkat yang tersambung akan muncul di sini"
                        },
                    )
                }
            } else {
                filteredClients.forEachIndexed { index, device ->
                    if (index > 0) RowDivider()
                    Box(Modifier.background(WifiCard)) {
                        ValueRow(title = device.name, value = device.address)
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiQuickCard(
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
            .background(WifiCard)
            .border(1.dp, WifiBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ){
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
            color = WifiText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = WifiMuted,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun WifiMetricRow(
    dotColor: Color,
    title: String,
    subtitle: String,
    metric: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WifiCard)
            .border(1.dp, WifiBorder, RoundedCornerShape(16.dp))
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
                color = WifiText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = WifiMuted,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Text(
            text = metric,
            color = WifiText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
