package com.udyneos.wifishare.proxy

import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Pagar supaya proxy tidak terbuka untuk jaringan lain (WiFi rumah, data seluler).
 * Koneksi hanya diterima kalau alamat asalnya satu subnet dengan antarmuka WiFi Direct.
 */
object P2pGuard {

    // subnet bawaan group owner Android, dipakai kalau daftar antarmuka tidak bisa dibaca
    private val FALLBACK = byteArrayOf(192.toByte(), 168.toByte(), 49)

    fun isAllowed(remote: InetAddress): Boolean {
        val bytes = remote.address
        if (bytes.size == 4 && bytes[0] == FALLBACK[0] && bytes[1] == FALLBACK[1] && bytes[2] == FALLBACK[2]) {
            return true
        }

        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        } catch (e: Exception) {
            emptyList()
        }

        return interfaces
            .filter { it.name.startsWith("p2p") }
            .flatMap { it.interfaceAddresses }
            .any { sameSubnet(it.address.address, bytes, it.networkPrefixLength.toInt()) }
    }

    private fun sameSubnet(a: ByteArray, b: ByteArray, prefixLength: Int): Boolean {
        if (a.size != b.size) return false
        var bits = prefixLength
        for (i in a.indices) {
            if (bits <= 0) break
            val mask = if (bits >= 8) 0xff else (0xff shl (8 - bits)) and 0xff
            if ((a[i].toInt() and mask) != (b[i].toInt() and mask)) return false
            bits -= 8
        }
        return true
    }
}
