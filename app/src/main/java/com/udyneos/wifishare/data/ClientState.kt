package com.udyneos.wifishare.data

/** Status sambungan HP ini sebagai klien ke jaringan WiFi Direct HP lain. */
data class ClientState(
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val ssid: String = "",
    val gateway: String = "",
    val error: String? = null,
)
