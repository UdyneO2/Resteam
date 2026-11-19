package com.udyneos.wifishare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Format standar WiFi QR: WIFI:T:WPA;S:nama;P:sandi;; dengan karakter khusus di-escape. */
fun wifiQrPayload(ssid: String, passphrase: String): String {
    fun esc(v: String) = buildString {
        v.forEach { c ->
            if (c == '\\' || c == ';' || c == ',' || c == ':' || c == '"') append('\\')
            append(c)
        }
    }
    return "WIFI:T:WPA;S:${esc(ssid)};P:${esc(passphrase)};;"
}

/** Kebalikan [wifiQrPayload]: ambil (nama, sandi) dari teks WIFI:..., atau null bila bukan format itu. */
fun parseWifiQr(raw: String): Pair<String, String>? {
    val text = raw.trim()
    if (!text.startsWith("WIFI:", ignoreCase = true)) return null

    val fields = mutableListOf<String>()
    val cur = StringBuilder()
    var escaped = false
    for (c in text.substring(5)) {
        when {
            escaped -> { cur.append(c); escaped = false }
            c == '\\' -> escaped = true
            c == ';' -> { fields.add(cur.toString()); cur.clear() }
            else -> cur.append(c)
        }
    }
    if (cur.isNotEmpty()) fields.add(cur.toString())

    var ssid = ""
    var pass = ""
    for (f in fields) {
        if (f.startsWith("S:")) ssid = f.substring(2)
        if (f.startsWith("P:")) pass = f.substring(2)
    }
    return if (ssid.isNotEmpty() && pass.isNotEmpty()) Pair(ssid, pass) else null
}

/** QR hitam di atas latar putih, selalu kontras tinggi supaya mudah discan walau UI transparan. */
@Composable
fun QrCode(content: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) {
        runCatching {
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(
                    EncodeHintType.MARGIN to 0,
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                ),
            )
        }.getOrNull()
    }

    Box(modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(14.dp)) {
        if (matrix != null) {
            Canvas(Modifier.fillMaxSize()) {
                val n = matrix.width
                // ukuran modul dibulatkan ke piksel utuh supaya tepinya tajam
                val cell = (size.minDimension / n).toInt().coerceAtLeast(1).toFloat()
                val origin = (size.minDimension - cell * n) / 2f
                for (y in 0 until n) {
                    for (x in 0 until n) {
                        if (matrix.get(x, y)) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(origin + x * cell, origin + y * cell),
                                size = Size(cell, cell),
                            )
                        }
                    }
                }
            }
        }
    }
}
