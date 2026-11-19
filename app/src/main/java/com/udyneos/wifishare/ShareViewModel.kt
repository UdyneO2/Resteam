package com.udyneos.wifishare

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.udyneos.wifishare.data.AppSettings
import com.udyneos.wifishare.service.ShareHub
import com.udyneos.wifishare.service.ShareService

class ShareViewModel(private val app: Application) : AndroidViewModel(app) {

    val settings = AppSettings(app)

    // state hidup di ShareHub supaya tetap sama walau activity dibuat ulang atau ditutup
    val state = ShareHub.controller(app).state

    private val clientController = ShareHub.client(app)
    val clientState = clientController.state

    fun connectClient(ssid: String, passphrase: String) = clientController.connect(ssid, passphrase)
    fun disconnectClient() = clientController.disconnect()

    fun start() {
        ContextCompat.startForegroundService(app, ShareService.startIntent(app))
    }

    fun stop() {
        app.startService(ShareService.stopIntent(app))
    }
}
