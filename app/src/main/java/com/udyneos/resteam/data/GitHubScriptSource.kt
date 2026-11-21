package com.udyneos.resteam.data

import com.udyneos.resteam.domain.repository.RemoteScriptFile
import com.udyneos.resteam.domain.repository.RemoteScriptSource
import com.udyneos.resteam.domain.usecase.isValidBranch
import com.udyneos.resteam.domain.usecase.isValidRepo
import com.udyneos.resteam.domain.usecase.isValidScriptFileName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengambil script lewat GitHub REST API tanpa token (batas 60 permintaan/jam per IP).
 * Tanpa dependency tambahan: HttpURLConnection + org.json bawaan Android.
 */
@Singleton
class GitHubScriptSource @Inject constructor() : RemoteScriptSource {

    override suspend fun listScripts(repo: String, branch: String): Result<List<RemoteScriptFile>> = io {
        require(isValidRepo(repo)) { "Nama repo tidak valid: $repo" }
        require(isValidBranch(branch)) { "Nama branch tidak valid: $branch" }
        val url = "$API/repos/$repo/contents/scripts?ref=${URLEncoder.encode(branch, "UTF-8")}"
        val body = try {
            get(url, MAX_LIST_BYTES)
        } catch (e: HttpStatusException) {
            if (e.code == 404) throw IOException("Folder scripts/ tidak ditemukan di branch \"$branch\"")
            throw e
        }
        val array = try {
            JSONArray(body)
        } catch (e: JSONException) {
            throw IOException("Respons GitHub bukan daftar file (scripts bukan folder?)")
        }
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val name = item.optString("name")
                val downloadUrl = item.optString("download_url")
                if (item.optString("type") != "file" || !isValidScriptFileName(name) || downloadUrl.isEmpty()) continue
                add(RemoteScriptFile(name, item.optString("sha"), downloadUrl, item.optLong("size")))
            }
        }
    }

    override suspend fun download(file: RemoteScriptFile): Result<String> = io {
        require(file.downloadUrl.startsWith(RAW_PREFIX)) { "URL unduhan tidak dipercaya" }
        if (file.size > MAX_SCRIPT_BYTES) throw IOException("${file.fileName} terlalu besar (${file.size} byte)")
        get(file.downloadUrl, MAX_SCRIPT_BYTES.toInt())
    }

    override suspend fun listBranches(repo: String): Result<List<String>> = io {
        require(isValidRepo(repo)) { "Nama repo tidak valid: $repo" }
        val array = JSONArray(get("$API/repos/$repo/branches?per_page=100", MAX_LIST_BYTES))
        buildList {
            for (i in 0 until array.length()) {
                val name = array.getJSONObject(i).optString("name")
                if (isValidBranch(name)) add(name)
            }
        }
    }

    private suspend fun <T> io(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun get(url: String, maxBytes: Int): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "ReSteam")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val rateLimited = (code == 403 || code == 429) && conn.getHeaderField("X-RateLimit-Remaining") == "0"
                if (rateLimited) throw IOException("Batas permintaan GitHub habis, coba lagi nanti")
                throw HttpStatusException(code)
            }
            val bytes = conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    if (out.size() > maxBytes) throw IOException("Respons terlalu besar")
                }
                out.toByteArray()
            }
            return String(bytes, Charsets.UTF_8)
        } finally {
            conn.disconnect()
        }
    }

    private class HttpStatusException(val code: Int) : IOException("GitHub membalas HTTP $code")

    private companion object {
        const val API = "https://api.github.com"
        const val RAW_PREFIX = "https://raw.githubusercontent.com/"
        const val TIMEOUT_MS = 15_000
        const val MAX_LIST_BYTES = 512 * 1024
        const val MAX_SCRIPT_BYTES = 256L * 1024
    }
}
