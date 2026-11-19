package com.udyneos.wifishare.service

import android.content.Context
import com.udyneos.wifishare.data.AppSettings
import com.udyneos.wifishare.wifi.WifiClientController
import com.udyneos.wifishare.wifi.WifiDirectController

/**
 * Pegangan satu-satunya ke controller WiFi Direct. Service dan UI sama-sama memakainya,
 * jadi tampilan tetap benar walau activity dibuat ulang selama berbagi jalan.
 */
object ShareHub {

    @Volatile
    private var controller: WifiDirectController? = null

    fun controller(context: Context): WifiDirectController {
        controller?.let { return it }
        return synchronized(this) {
            controller ?: WifiDirectController(
                context.applicationContext,
                AppSettings(context.applicationContext),
            ).also { controller = it }
        }
    }

    @Volatile
    private var client: WifiClientController? = null

    /** Controller klien, dipisah dari host supaya sambungan tidak ikut mati saat activity dibuat ulang. */
    fun client(context: Context): WifiClientController {
        client?.let { return it }
        return synchronized(this) {
            client ?: WifiClientController(context.applicationContext).also { client = it }
        }
    }
}
