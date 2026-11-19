package com.udyneos.wifishare.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    val opacity = settings.glassOpacity
    val span = AppSettings.GLASS_MAX - AppSettings.GLASS_MIN

    GlassScrollScreen(title = "Pengaturan", wallpaper = wallpaper, bottomInset = bottomInset) {
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

        SectionHeader("Tentang")
        GlassGroup(wallpaper, delayMillis = 210) {
            ValueRow("Versi", value = "1.2.0")
        }
    }
}
