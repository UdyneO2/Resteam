package com.udyneos.wifishare.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import java.security.SecureRandom

/** Pengaturan sederhana di SharedPreferences. Cukup buat saat ini, nanti bisa pindah ke DataStore. */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Harus diawali "DIRECT-" plus dua karakter, aturan WifiP2pConfig. */
    var networkName: String
        get() = prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
        set(value) = prefs.edit { putString(KEY_NAME, value) }

    /** Dibuat acak waktu pertama dibaca supaya tiap pemasangan punya password sendiri. */
    var passphrase: String
        get() = prefs.getString(KEY_PASS, null) ?: generatePassphrase().also { passphrase = it }
        set(value) = prefs.edit { putString(KEY_PASS, value) }

    var proxyPort: Int
        get() = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = prefs.edit { putInt(KEY_PORT, value) }

    var useFiveGhz: Boolean
        get() = prefs.getBoolean(KEY_5GHZ, false)
        set(value) = prefs.edit { putBoolean(KEY_5GHZ, value) }

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_SCREEN, true)
        set(value) = prefs.edit { putBoolean(KEY_SCREEN, value) }

    var autoStart: Boolean
        get() = prefs.getBoolean(KEY_AUTOSTART, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTOSTART, value) }

    // pakai state Compose supaya GlassHost ikut berubah begitu saklar di Pengaturan digeser
    private val animateBackgroundState = mutableStateOf(prefs.getBoolean(KEY_ANIM, true))

    var animateBackground: Boolean
        get() = animateBackgroundState.value
        set(value) {
            animateBackgroundState.value = value
            prefs.edit { putBoolean(KEY_ANIM, value) }
        }

    // juga state Compose: slider di Pengaturan langsung mengubah semua kaca
    private val glassOpacityState = mutableStateOf(prefs.getFloat(KEY_GLASS, DEFAULT_GLASS))

    var glassOpacity: Float
        get() = glassOpacityState.value
        set(value) {
            glassOpacityState.value = value
            prefs.edit { putFloat(KEY_GLASS, value) }
        }

    /** Jaringan terakhir yang dipakai di tab Klien, supaya tidak perlu mengetik ulang. */
    var clientSsid: String
        get() = prefs.getString(KEY_C_SSID, "") ?: ""
        set(value) = prefs.edit { putString(KEY_C_SSID, value) }

    var clientPassphrase: String
        get() = prefs.getString(KEY_C_PASS, "") ?: ""
        set(value) = prefs.edit { putString(KEY_C_PASS, value) }

    /** Port proxy di HP host, dipakai untuk menampilkan alamat proxy di tab Klien. */
    var clientProxyPort: Int
        get() = prefs.getInt(KEY_C_PORT, DEFAULT_PORT)
        set(value) = prefs.edit { putInt(KEY_C_PORT, value) }

    private fun generatePassphrase(): String {
        // tanpa karakter yang gampang tertukar (0/O, 1/l/I)
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        val random = SecureRandom()
        return (1..10).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    companion object {
        private const val KEY_NAME = "network_name"
        private const val KEY_PASS = "passphrase"
        private const val KEY_PORT = "proxy_port"
        private const val KEY_5GHZ = "five_ghz"
        private const val KEY_SCREEN = "keep_screen_on"
        private const val KEY_AUTOSTART = "auto_start"
        private const val KEY_ANIM = "animate_background"
        private const val KEY_GLASS = "glass_opacity"
        private const val KEY_C_SSID = "client_ssid"
        private const val KEY_C_PASS = "client_pass"
        private const val KEY_C_PORT = "client_port"

        private const val DEFAULT_NAME = "DIRECT-WifiShare"
        private const val DEFAULT_PORT = 8000
        private const val DEFAULT_GLASS = 0.35f

        const val NAME_PREFIX = "DIRECT-"
        const val GLASS_MIN = 0.10f
        const val GLASS_MAX = 0.80f
        const val PORT_MIN = 1024
        const val PORT_MAX = 65535
    }
}
