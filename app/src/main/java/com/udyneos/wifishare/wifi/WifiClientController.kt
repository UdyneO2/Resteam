package com.udyneos.wifishare.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.udyneos.wifishare.data.ClientState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Menyambung HP ini ke jaringan DIRECT-... milik HP lain sebagai klien biasa (WPA2),
 * memakai WifiNetworkSpecifier (Android 10+). Jaringan ini tanpa internet, jadi
 * NET_CAPABILITY_INTERNET dicabut dari permintaan. Sambungan hidup selama callback terdaftar.
 */
class WifiClientController(context: Context) {

    private val cm = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _state = MutableStateFlow(ClientState())
    val state: StateFlow<ClientState> = _state.asStateFlow()

    private var callback: ConnectivityManager.NetworkCallback? = null

    fun connect(ssid: String, passphrase: String) {
        disconnect()
        _state.value = ClientState(connecting = true, ssid = ssid)

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(passphrase)
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // proses ini diarahkan ke jaringan itu supaya socket aplikasi lewat sana
                cm.bindProcessToNetwork(network)
                _state.value = ClientState(
                    connected = true,
                    ssid = ssid,
                    gateway = gatewayOf(cm.getLinkProperties(network)),
                )
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                if (_state.value.connected) {
                    _state.value = _state.value.copy(gateway = gatewayOf(linkProperties))
                }
            }

            override fun onUnavailable() {
                _state.value = ClientState(error = "Gagal tersambung. Periksa nama jaringan dan kata sandi.")
                release()
            }

            override fun onLost(network: Network) {
                _state.value = ClientState(error = "Sambungan terputus")
                release()
            }
        }
        callback = cb

        try {
            // batas 30 dtk supaya onUnavailable terpanggil bila user mengabaikan dialog sistem
            cm.requestNetwork(request, cb, 30_000)
        } catch (e: SecurityException) {
            callback = null
            _state.value = ClientState(error = "Izin jaringan belum diberikan")
        } catch (e: RuntimeException) {
            callback = null
            _state.value = ClientState(error = "Tidak bisa memulai sambungan")
        }
    }

    fun disconnect() {
        release()
        _state.value = ClientState()
    }

    private fun release() {
        callback?.let {
            try {
                cm.unregisterNetworkCallback(it)
            } catch (_: IllegalArgumentException) {
            }
        }
        callback = null
        cm.bindProcessToNetwork(null)
    }

    private fun gatewayOf(props: LinkProperties?): String {
        if (props == null) return DEFAULT_GATEWAY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            props.dhcpServerAddress?.hostAddress?.let { return it }
        }
        val route = props.routes.firstOrNull { it.isDefaultRoute && it.gateway != null }
        return route?.gateway?.hostAddress ?: DEFAULT_GATEWAY
    }

    private companion object {
        // alamat group owner WiFi Direct pada umumnya
        const val DEFAULT_GATEWAY = "192.168.49.1"
    }
}
