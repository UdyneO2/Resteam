package com.udyneos.wifishare.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import com.udyneos.wifishare.data.AppSettings
import com.udyneos.wifishare.data.ClientDevice
import com.udyneos.wifishare.data.ShareState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Membuat dan menjaga grup WiFi Direct dengan HP ini sebagai group owner.
 * Semua callback WifiP2pManager jalan di main looper, jadi state aman diubah langsung.
 */
@SuppressLint("MissingPermission") // izin dicek di MainActivity sebelum start()
class WifiDirectController(
    context: Context,
    private val settings: AppSettings,
) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private var receiverRegistered = false

    private val _state = MutableStateFlow(ShareState())
    val state: StateFlow<ShareState> = _state.asStateFlow()

    val isSupported: Boolean get() = manager != null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED && _state.value.active) {
                        // user mematikan WiFi saat sedang berbagi
                        _state.value = ShareState(error = "WiFi dimatikan, berbagi berhenti")
                        unregister()
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION,
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    if (_state.value.active) refreshGroup()
                }
            }
        }
    }

    fun start() {
        val m = manager ?: return fail("Perangkat ini tidak mendukung WiFi Direct")
        if (_state.value.busy || _state.value.active) return

        val ch = channel ?: m.initialize(appContext, Looper.getMainLooper(), null).also { channel = it }
        register()
        _state.value = ShareState(busy = true)

        // sisa grup dari sesi sebelumnya bikin createGroup gagal BUSY, jadi dibersihkan dulu
        try {
            m.removeGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() = createGroup(m, ch)
                override fun onFailure(reason: Int) = createGroup(m, ch)
            })
        } catch (e: SecurityException) {
            fail("Izin WiFi belum diberikan")
        }
    }

    fun stop() {
        val m = manager
        val ch = channel
        if (m != null && ch != null) {
            try {
                m.removeGroup(ch, null)
            } catch (_: SecurityException) {
                // sudah tidak ada yang bisa dilakukan
            }
        }
        unregister()
        _state.value = ShareState()
    }

    /** Hentikan grup dan tampilkan alasan, dipakai service kalau proxy gagal jalan. */
    fun abort(message: String) {
        val m = manager
        val ch = channel
        if (m != null && ch != null) {
            try {
                m.removeGroup(ch, null)
            } catch (_: SecurityException) {
            }
        }
        fail(message)
    }

    private fun createGroup(m: WifiP2pManager, ch: WifiP2pManager.Channel) {
        val config = try {
            WifiP2pConfig.Builder()
                .setNetworkName(settings.networkName)
                .setPassphrase(settings.passphrase)
                .enablePersistentMode(false)
                .setGroupOperatingBand(
                    if (settings.useFiveGhz) WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
                    else WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
                )
                .build()
        } catch (e: IllegalArgumentException) {
            return fail("Nama atau kata sandi tidak valid")
        }

        try {
            m.createGroup(ch, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    _state.update { it.copy(active = true, busy = false) }
                    refreshGroup()
                }

                override fun onFailure(reason: Int) = fail(reasonText(reason))
            })
        } catch (e: SecurityException) {
            fail("Izin WiFi belum diberikan")
        }
    }

    /** Ambil nama, password, alamat owner, dan klien terbaru dari sistem. */
    private fun refreshGroup() {
        val m = manager ?: return
        val ch = channel ?: return

        m.requestConnectionInfo(ch) { info ->
            val ownerIp = info?.groupOwnerAddress?.hostAddress ?: DEFAULT_OWNER_IP

            m.requestGroupInfo(ch) { group ->
                if (group == null || !group.isGroupOwner) return@requestGroupInfo
                _state.update {
                    it.copy(
                        active = true,
                        busy = false,
                        networkName = group.networkName.orEmpty(),
                        passphrase = group.passphrase.orEmpty(),
                        proxyAddress = "$ownerIp:${settings.proxyPort}",
                        clients = group.clientList.map { device ->
                            ClientDevice(
                                name = device.deviceName.ifBlank { "Perangkat" },
                                address = device.deviceAddress.orEmpty(),
                            )
                        },
                    )
                }
            }
        }
    }

    private fun fail(message: String) {
        unregister()
        _state.value = ShareState(error = message)
    }

    private fun reasonText(reason: Int) = when (reason) {
        WifiP2pManager.BUSY -> "WiFi sedang sibuk, coba lagi sebentar lagi"
        WifiP2pManager.P2P_UNSUPPORTED -> "WiFi Direct tidak didukung"
        else -> "Gagal membuat grup. Pastikan WiFi dan lokasi aktif"
    }

    private fun register() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        }
        appContext.registerReceiver(receiver, filter)
        receiverRegistered = true
    }

    private fun unregister() {
        if (!receiverRegistered) return
        appContext.unregisterReceiver(receiver)
        receiverRegistered = false
    }

    private companion object {
        // alamat bawaan group owner WiFi Direct di Android
        const val DEFAULT_OWNER_IP = "192.168.49.1"
    }
}
