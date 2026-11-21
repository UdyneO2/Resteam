package com.udyneos.resteam.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Banner per game yang dipilih pengguna; disimpan sebagai JPEG kecil di filesDir/banners. */
@Singleton
class GameBannerStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir = File(context.filesDir, "banners")
    private val _banners = MutableStateFlow(scan())

    /** packageName -> file banner. */
    val banners: StateFlow<Map<String, File>> = _banners

    suspend fun save(packageName: String, source: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(PACKAGE.matches(packageName)) { "Nama package tidak valid" }
            val bitmap = decode(source) ?: throw IOException("Gambar tidak bisa dibaca")
            dir.mkdirs()
            val target = File(dir, "$packageName.jpg")
            val temp = File(dir, "$packageName.tmp")
            temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bitmap.recycle()
            if (!temp.renameTo(target)) throw IOException("Gagal menyimpan banner")
            _banners.value = scan()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun clear(packageName: String) = withContext(Dispatchers.IO) {
        if (PACKAGE.matches(packageName)) File(dir, "$packageName.jpg").delete()
        _banners.value = scan()
    }

    private fun decode(source: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_WIDTH) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun scan(): Map<String, File> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".jpg") }
            ?.associate { it.name.removeSuffix(".jpg") to it }
            .orEmpty()

    private companion object {
        const val MAX_WIDTH = 1280
        val PACKAGE = Regex("""[A-Za-z0-9_.]{1,150}""")
    }
}
