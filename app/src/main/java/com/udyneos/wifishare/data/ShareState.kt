package com.udyneos.wifishare.data

/** Satu perangkat yang tersambung ke grup. */
data class ClientDevice(
    val name: String,
    val address: String,
)

/**
 * State layar WiFi. Diisi WifiDirectController. Alamat proxy baru benar-benar
 * melayani koneksi setelah Phase 3, sampai itu cuma jadi info.
 */
data class ShareState(
    val active: Boolean = false,
    val busy: Boolean = false,
    val networkName: String = "",
    val passphrase: String = "",
    val proxyAddress: String = "",
    val clients: List<ClientDevice> = emptyList(),
    val error: String? = null,
)
