package com.udyneos.wifishare.proxy

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Proxy kecil untuk klien WiFi Direct. Satu port melayani dua protokol:
 * SOCKS5 (tanpa auth, hanya CONNECT) dan HTTP (CONNECT untuk HTTPS, forward untuk HTTP biasa).
 * Koneksi keluar memakai jaringan default HP, yaitu data seluler.
 */
class ProxyServer(private val port: Int) {

    private var server: ServerSocket? = null
    private val pool = Executors.newCachedThreadPool()
    private val open = ConcurrentHashMap.newKeySet<Socket>()

    /** Mengembalikan false kalau port tidak bisa dipakai. */
    fun start(): Boolean {
        val socket = try {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(port))
            }
        } catch (e: IOException) {
            return false
        }
        server = socket

        pool.execute {
            while (!socket.isClosed) {
                val client = try {
                    socket.accept()
                } catch (e: IOException) {
                    break
                }
                pool.execute { handle(client) }
            }
        }
        return true
    }

    fun stop() {
        closeQuietly(server)
        server = null
        open.toList().forEach { closeQuietly(it) }
        open.clear()
        pool.shutdownNow()
    }

    private fun handle(client: Socket) {
        open += client
        try {
            // jangan jadi open proxy: hanya terima koneksi dari jaringan WiFi Direct
            if (!P2pGuard.isAllowed(client.inetAddress)) return

            client.soTimeout = HANDSHAKE_TIMEOUT_MS
            val input = BufferedInputStream(client.getInputStream())
            input.mark(1)
            val first = input.read()
            if (first < 0) return
            input.reset()

            if (first == SOCKS_VERSION) handleSocks(client, input) else handleHttp(client, input)
        } catch (e: IOException) {
            // klien putus di tengah jalan, tidak perlu dilaporkan
        } finally {
            closeQuietly(client)
            open -= client
        }
    }

    // ---- HTTP ----

    private fun handleHttp(client: Socket, input: InputStream) {
        val out = client.getOutputStream()

        val requestLine = readLine(input) ?: return
        val parts = requestLine.split(" ")
        if (parts.size < 3) return respond(out, "400 Bad Request")

        val method = parts[0]
        val target = parts[1]
        val version = parts[2]

        val headers = mutableListOf<String>()
        while (true) {
            val line = readLine(input) ?: return
            if (line.isEmpty()) break
            headers += line
            if (headers.size > MAX_HEADERS) return respond(out, "431 Request Header Fields Too Large")
        }

        if (method.equals("CONNECT", ignoreCase = true)) {
            val (host, port) = splitHostPort(target, 443) ?: return respond(out, "400 Bad Request")
            val remote = connectRemote(host, port) ?: return respond(out, "502 Bad Gateway")
            out.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
            out.flush()
            relay(client, input, remote)
            return
        }

        val uri = try {
            URI(target)
        } catch (e: URISyntaxException) {
            null
        }
        if (uri == null || uri.scheme?.lowercase() != "http" || uri.host == null) {
            return respond(out, "400 Bad Request")
        }

        val remotePort = if (uri.port > 0) uri.port else 80
        val path = (uri.rawPath?.takeIf { it.isNotEmpty() } ?: "/") + (uri.rawQuery?.let { "?$it" } ?: "")
        val remote = connectRemote(uri.host, remotePort) ?: return respond(out, "502 Bad Gateway")

        val request = StringBuilder("$method $path $version\r\n")
        for (header in headers) {
            val name = header.substringBefore(':').trim().lowercase()
            if (name in DROPPED_HEADERS) continue
            request.append(header).append("\r\n")
        }
        // satu koneksi satu request, jauh lebih sederhana daripada mengurus keep-alive
        request.append("Connection: close\r\n\r\n")

        remote.getOutputStream().apply {
            write(request.toString().toByteArray(Charsets.ISO_8859_1))
            flush()
        }
        relay(client, input, remote)
    }

    private fun respond(out: OutputStream, status: String) {
        out.write("HTTP/1.1 $status\r\nConnection: close\r\nContent-Length: 0\r\n\r\n".toByteArray())
        out.flush()
    }

    // ---- SOCKS5 ----

    private fun handleSocks(client: Socket, input: InputStream) {
        val out = client.getOutputStream()

        input.read() // versi, sudah dicek di handle()
        val methodCount = input.read()
        if (methodCount < 0) return
        val methods = readFully(input, methodCount) ?: return
        if (methods.none { it.toInt() == 0 }) {
            out.write(byteArrayOf(SOCKS_VERSION.toByte(), -1))
            out.flush()
            return
        }
        out.write(byteArrayOf(SOCKS_VERSION.toByte(), 0))
        out.flush()

        val head = readFully(input, 4) ?: return // versi, perintah, reserved, tipe alamat
        if (head[1].toInt() != 1) return socksReply(out, 7) // hanya CONNECT

        val host = when (head[3].toInt()) {
            1 -> readFully(input, 4)?.let { InetAddress.getByAddress(it).hostAddress }
            3 -> input.read().takeIf { it >= 0 }?.let { readFully(input, it)?.toString(Charsets.US_ASCII) }
            4 -> readFully(input, 16)?.let { InetAddress.getByAddress(it).hostAddress }
            else -> null
        } ?: return socksReply(out, 8)

        val portBytes = readFully(input, 2) ?: return
        val port = ((portBytes[0].toInt() and 0xff) shl 8) or (portBytes[1].toInt() and 0xff)

        val remote = connectRemote(host, port) ?: return socksReply(out, 5)
        socksReply(out, 0)
        relay(client, input, remote)
    }

    private fun socksReply(out: OutputStream, code: Int) {
        out.write(byteArrayOf(SOCKS_VERSION.toByte(), code.toByte(), 0, 1, 0, 0, 0, 0, 0, 0))
        out.flush()
    }

    // ---- util ----

    private fun connectRemote(host: String, port: Int): Socket? {
        return try {
            val address = InetAddress.getByName(host)
            // klien tidak boleh menjangkau layanan di HP ini lewat proxy
            if (address.isLoopbackAddress || address.isAnyLocalAddress) return null
            Socket().apply {
                connect(InetSocketAddress(address, port), CONNECT_TIMEOUT_MS)
                tcpNoDelay = true
                open += this
            }
        } catch (e: IOException) {
            null
        }
    }

    /** Salin data dua arah sampai salah satu sisi selesai, lalu tutup keduanya. */
    private fun relay(client: Socket, clientIn: InputStream, remote: Socket) {
        client.soTimeout = IDLE_TIMEOUT_MS
        remote.soTimeout = IDLE_TIMEOUT_MS
        try {
            val upstream = pool.submit {
                copy(clientIn, remote.getOutputStream(), client, remote)
                try { remote.shutdownOutput() } catch (_: IOException) {}
            }
            copy(remote.getInputStream(), client.getOutputStream(), client, remote)
            try { client.shutdownOutput() } catch (_: IOException) {}
            upstream.get()
        } catch (e: Exception) {
            // diputus saat stop() atau salah satu sisi error
        } finally {
            closeQuietly(remote)
            open -= remote
        }
    }

    private fun copy(src: InputStream, dst: OutputStream, a: Socket, b: Socket) {
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            while (true) {
                val n = src.read(buffer)
                if (n < 0) break
                dst.write(buffer, 0, n)
                dst.flush()
            }
        } catch (e: IOException) {
            // salah satu sisi error atau timeout, tutup keduanya supaya sisi lain tidak menggantung
            closeQuietly(a)
            closeQuietly(b)
        }
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
            if (sb.length > MAX_LINE) return null
        }
        return sb.toString()
    }

    private fun readFully(input: InputStream, count: Int): ByteArray? {
        val buffer = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buffer, offset, count - offset)
            if (read < 0) return null
            offset += read
        }
        return buffer
    }

    private fun splitHostPort(target: String, defaultPort: Int): Pair<String, Int>? {
        if (target.startsWith("[")) {
            val end = target.indexOf(']')
            if (end < 0) return null
            val port = target.substring(end + 1).removePrefix(":").toIntOrNull() ?: defaultPort
            return target.substring(1, end) to port
        }
        val colon = target.lastIndexOf(':')
        if (colon < 0) return target to defaultPort
        val port = target.substring(colon + 1).toIntOrNull() ?: return null
        return target.substring(0, colon) to port
    }

    private fun closeQuietly(closeable: java.io.Closeable?) {
        try {
            closeable?.close()
        } catch (_: IOException) {
        }
    }

    private companion object {
        const val SOCKS_VERSION = 5
        const val HANDSHAKE_TIMEOUT_MS = 15_000
        const val CONNECT_TIMEOUT_MS = 10_000
        const val IDLE_TIMEOUT_MS = 300_000
        const val BUFFER_SIZE = 16 * 1024
        const val MAX_LINE = 8192
        const val MAX_HEADERS = 100

        val DROPPED_HEADERS = setOf("proxy-connection", "proxy-authorization", "connection", "keep-alive")
    }
}
