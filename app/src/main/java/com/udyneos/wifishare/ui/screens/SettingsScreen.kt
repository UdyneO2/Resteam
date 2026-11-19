package com.udyneos.wifishare.ui.screens

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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.udyneos.wifishare.data.AppSettings
import com.udyneos.wifishare.ui.components.GlassGroup
import com.udyneos.wifishare.ui.components.GlassScrollScreen
import com.udyneos.wifishare.ui.components.GlassSlider
import com.udyneos.wifishare.ui.components.IosSwitch
import com.udyneos.wifishare.ui.components.RowDivider
import com.udyneos.wifishare.ui.components.SectionFooter
import com.udyneos.wifishare.ui.components.SectionHeader
import com.udyneos.wifishare.ui.components.TextEditRequest
import com.udyneos.wifishare.ui.components.ValueRow
import com.udyneos.wifishare.ui.theme.Ios
import kotlin.math.roundToInt

private val SettingsBg = Color(0xFF0B1222)
private val SettingsCard = Color(0xFF151F36)
private val SettingsElevated = Color(0xFF283552)
private val SettingsBorder = Color(0xFF23304D)
private val SettingsText = Color(0xFFF8FAFC)
private val SettingsMuted = Color(0xFF94A3B8)
private val SettingsBlue = Color(0xFF3B82F6)
private val SettingsTeal = Color(0xFF14B8A6)

@Composable
fun SettingsScreen(
    wallpaper: Backdrop,
    settings: AppSettings,
    sharing: Boolean,
    bottomInset: Dp,
    onEdit: (TextEditRequest) -> Unit,
) {
    val ios = Ios.colors

    // state lokal yang dicerminkan ke SharedPreferences
    var name by remember { mutableStateOf(settings.networkName) }
    var passphrase by remember { mutableStateOf(settings.passphrase) }
    var port by remember { mutableStateOf(settings.proxyPort) }
    var fiveGhz by remember { mutableStateOf(settings.useFiveGhz) }
    var keepScreenOn by remember { mutableStateOf(settings.keepScreenOn) }
    var autoStart by remember { mutableStateOf(settings.autoStart) }
    var animateBg by remember { mutableStateOf(settings.animateBackground) }
    var searchQuery by remember { mutableStateOf("") }

    val opacity = settings.glassOpacity
    val span = AppSettings.GLASS_MAX - AppSettings.GLASS_MIN
    val q = searchQuery.trim()

    fun matches(vararg keywords: String): Boolean {
        if (q.isEmpty()) return true
        return keywords.any { it.contains(q, ignoreCase = true) }
    }

    GlassScrollScreen(title = "Pengaturan", wallpaper = wallpaper, bottomInset = bottomInset) {
        // Header & Search Bar ala gambar referensi
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = "Konfigurasi aplikasi,",
                    color = SettingsMuted,
                    fontSize = 13.sp,
                )
                Text(
                    text = "Pengaturan",
                    color = SettingsText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = {
                    Text("Cari pengaturan...", color = SettingsMuted, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = SettingsMuted,
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SettingsCard,
                    unfocusedContainerColor = SettingsCard,
                    focusedBorderColor = SettingsBlue.copy(alpha = 0.65f),
                    unfocusedBorderColor = SettingsBorder,
                    focusedTextColor = SettingsText,
                    unfocusedTextColor = SettingsText,
                    cursorColor = SettingsBlue,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Ringkasan profil aktif (setara blok preview utama di gambar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SettingsCard)
                    .border(1.dp, SettingsBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SettingsElevated)
                        .padding(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name,
                                color = SettingsText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                repeat(4) { idx ->
                                    val lit = if (fiveGhz) idx < 4 else idx < 2
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (lit) Color(0xFF22C55E) else SettingsBg),
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Proxy :$port • ${if (fiveGhz) "5 GHz" else "2.4 GHz"} • Kaca ${(opacity * 100).roundToInt()}%",
                            color = SettingsMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            // Grid 2 Kolom Cepat ala bagian "Popular" pada gambar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsShortcutCard(
                    title = "Profil WiFi",
                    subtitle = if (fiveGhz) "Pita 5 GHz aktif" else "Pita standar",
                    icon = Icons.Rounded.Wifi,
                    badgeColor = SettingsBlue,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onEdit(
                            TextEditRequest(
                                title = "Nama jaringan",
                                initial = name,
                                validate = { input ->
                                    val value = input.trim()
                                    when {
                                        !value.startsWith(AppSettings.NAME_PREFIX) ->
                                            "Harus diawali \"${AppSettings.NAME_PREFIX}\""
                                        value.length < AppSettings.NAME_PREFIX.length + 2 ->
                                            "Tambahkan minimal 2 karakter setelah \"${AppSettings.NAME_PREFIX}\""
                                        value.length > 32 -> "Maksimal 32 karakter"
                                        else -> null
                                    }
                                },
                                onSave = { name = it; settings.networkName = it },
                            ),
                        )
                    },
                )
                SettingsShortcutCard(
                    title = "Port Proxy",
                    subtitle = "Port saat ini: $port",
                    icon = Icons.Rounded.Tune,
                    badgeColor = SettingsTeal,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onEdit(
                            TextEditRequest(
                                title = "Port proxy",
                                initial = port.toString(),
                                keyboardType = KeyboardType.Number,
                                validate = { input ->
                                    val value = input.trim().toIntOrNull()
                                    if (value == null || value !in AppSettings.PORT_MIN..AppSettings.PORT_MAX) {
                                        "Isi angka ${AppSettings.PORT_MIN} sampai ${AppSettings.PORT_MAX}"
                                    } else null
                                },
                                onSave = { port = it.toInt(); settings.proxyPort = port },
                            ),
                        )
                    },
                )
            }

            // Baris metrik ringkas ala bagian bawah gambar
            SettingsStatusRow(
                dotColor = SettingsBlue,
                title = "Kepekatan Kaca",
                subtitle = if (animateBg) "Animasi latar aktif" else "Animasi latar nonaktif",
                metric = "${(opacity * 100).roundToInt()}%",
            )
            SettingsStatusRow(
                dotColor = if (autoStart) Color(0xFF22C55E) else Color(0xFF0EA5E9),
                title = "Mulai Otomatis",
                subtitle = if (keepScreenOn) "Layar tetap menyala saat berbagi" else "Layar mengikuti sistem",
                metric = if (autoStart) "ON" else "OFF",
            )
        }

        if (matches("Jaringan", "Nama jaringan", "Kata sandi", "Port proxy", "Pita 5 GHz")) {
            SectionHeader("Jaringan")
            GlassGroup(wallpaper) {
                ValueRow(
                    "Nama jaringan",
                    value = name,
                    onClick = {
                        onEdit(
                            TextEditRequest(
                                title = "Nama jaringan",
                                initial = name,
                                validate = { input ->
                                    val value = input.trim()
                                    when {
                                        !value.startsWith(AppSettings.NAME_PREFIX) ->
                                            "Harus diawali \"${AppSettings.NAME_PREFIX}\""
                                        value.length < AppSettings.NAME_PREFIX.length + 2 ->
                                            "Tambahkan minimal 2 karakter setelah \"${AppSettings.NAME_PREFIX}\""
                                        value.length > 32 -> "Maksimal 32 karakter"
                                        else -> null
                                    }
                                },
                                onSave = { name = it; settings.networkName = it },
                            ),
                        )
                    },
                )
                RowDivider()
                ValueRow(
                    "Kata sandi",
                    value = passphrase,
                    onClick = {
                        onEdit(
                            TextEditRequest(
                                title = "Kata sandi",
                                initial = passphrase,
                                validate = { input ->
                                    if (input.trim().length !in 8..63) "Panjang 8 sampai 63 karakter" else null
                                },
                                onSave = { passphrase = it; settings.passphrase = it },
                            ),
                        )
                    },
                )
                RowDivider()
                ValueRow(
                    "Port proxy",
                    value = port.toString(),
                    onClick = {
                        onEdit(
                            TextEditRequest(
                                title = "Port proxy",
                                initial = port.toString(),
                                keyboardType = KeyboardType.Number,
                                validate = { input ->
                                    val value = input.trim().toIntOrNull()
                                    if (value == null || value !in AppSettings.PORT_MIN..AppSettings.PORT_MAX) {
                                        "Isi angka ${AppSettings.PORT_MIN} sampai ${AppSettings.PORT_MAX}"
                                    } else null
                                },
                                onSave = { port = it.toInt(); settings.proxyPort = port },
                            ),
                        )
                    },
                )
                RowDivider()
                ValueRow(
                    title = "Pita 5 GHz",
                    trailing = {
                        IosSwitch(fiveGhz, { fiveGhz = it; settings.useFiveGhz = it })
                    },
                )
            }
            SectionFooter(
                if (sharing) {
                    "Perubahan jaringan baru dipakai saat berbagi dimulai lagi."
                } else {
                    "5 GHz lebih cepat tapi tidak semua perangkat dan HP mendukungnya."
                }
            )
        }

        if (matches("Tampilan", "Kepekatan kaca", "Animasi latar")) {
            SectionHeader("Tampilan")
            GlassGroup(wallpaper, delayMillis = 70) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Kepekatan kaca", color = ios.label, fontSize = 17.sp)
                        Text("${(opacity * 100).roundToInt()}%", color = ios.secondaryLabel, fontSize = 17.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    GlassSlider(
                        value = (opacity - AppSettings.GLASS_MIN) / span,
                        onValueChange = { settings.glassOpacity = AppSettings.GLASS_MIN + it * span },
                        backdrop = wallpaper,
                    )
                }
                RowDivider()
                ValueRow(
                    title = "Animasi latar",
                    subtitle = "Warna kaca bergerak pelan, matikan untuk hemat baterai",
                    trailing = {
                        IosSwitch(animateBg, { animateBg = it; settings.animateBackground = it })
                    },
                )
            }
            SectionFooter("Makin kecil kepekatan, makin transparan kaca dan makin kelihatan wallpaper di baliknya.")
        }

        if (matches("Umum", "Layar tetap menyala", "Mulai otomatis")) {
            SectionHeader("Umum")
            GlassGroup(wallpaper, delayMillis = 140) {
                ValueRow(
                    title = "Layar tetap menyala",
                    subtitle = "Saat sedang berbagi",
                    trailing = {
                        IosSwitch(keepScreenOn, { keepScreenOn = it; settings.keepScreenOn = it })
                    },
                )
                RowDivider()
                ValueRow(
                    title = "Mulai otomatis",
                    subtitle = "Langsung berbagi saat aplikasi dibuka",
                    trailing = {
                        IosSwitch(autoStart, { autoStart = it; settings.autoStart = it })
                    },
                )
            }
        }

        if (matches("Tentang", "Versi")) {
            SectionHeader("Tentang")
            GlassGroup(wallpaper, delayMillis = 210) {
                ValueRow("Versi", value = "1.2.0")
            }
        }
    }
}

@Composable
private fun SettingsShortcutCard(
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
            .background(SettingsCard)
            .border(1.dp, SettingsBorder, RoundedCornerShape(18.dp))
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
            color = SettingsText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = SettingsMuted,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun SettingsStatusRow(
    dotColor: Color,
    title: String,
    subtitle: String,
    metric: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SettingsCard)
            .border(1.dp, SettingsBorder, RoundedCornerShape(16.dp))
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
                color = SettingsText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = SettingsMuted,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Text(
            text = metric,
            color = SettingsText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
