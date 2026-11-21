package com.udyneos.resteam.domain.usecase

import com.udyneos.resteam.domain.model.ShizukuScript
import com.udyneos.resteam.domain.model.parseScriptMetadata
import com.udyneos.resteam.domain.repository.ActivityLog
import com.udyneos.resteam.domain.repository.RemoteScriptSource
import com.udyneos.resteam.domain.repository.ScriptRepository
import com.udyneos.resteam.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateSummary(val added: Int, val updated: Int, val removed: Int, val unchanged: Int) {
    val hasChanges: Boolean get() = added + updated + removed > 0
}

/**
 * Menyinkronkan script lokal dengan folder scripts/ di GitHub.
 * Hanya file yang SHA-nya berubah yang diunduh. Jika ada yang gagal, daftar lokal tidak diubah sama sekali.
 */
@Singleton
class UpdateScriptsUseCase @Inject constructor(
    private val source: RemoteScriptSource,
    private val scripts: ScriptRepository,
    private val settings: SettingsRepository,
    private val log: ActivityLog,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): Result<UpdateSummary> = mutex.withLock {
        val result = try {
            sync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
        result.fold(
            onSuccess = {
                if (it.hasChanges) {
                    log.record(
                        title = "Script diperbarui",
                        detail = "Baru ${it.added}, diubah ${it.updated}, dihapus ${it.removed}, tetap ${it.unchanged}",
                    )
                }
            },
            onFailure = {
                log.record(
                    title = "Update script gagal",
                    detail = it.message ?: "Kesalahan tidak diketahui",
                    isError = true,
                )
            },
        )
        result
    }

    private suspend fun sync(): Result<UpdateSummary> {
        val config = settings.settings.first()
        val remote = source.listScripts(config.scriptRepo, config.scriptBranch).getOrElse { return Result.failure(it) }
        val local = scripts.observeScripts().first().associateBy { it.fileName }

        var added = 0
        var updated = 0
        var unchanged = 0
        val merged = ArrayList<ShizukuScript>(remote.size)
        for (file in remote) {
            val existing = local[file.fileName]
            if (existing != null && existing.sha == file.sha) {
                unchanged++
                merged += existing
                continue
            }
            val content = source.download(file).getOrElse { return Result.failure(it) }
            val meta = parseScriptMetadata(file.fileName, content)
            if (existing == null) added++ else updated++
            merged += ShizukuScript(
                fileName = file.fileName,
                name = meta.name,
                description = meta.description,
                version = meta.version,
                content = content,
                sha = file.sha,
                enabled = existing?.enabled ?: false,
            )
        }
        val removed = local.keys.count { name -> remote.none { it.fileName == name } }

        scripts.replaceAll(merged)
        settings.setScriptsLastUpdatedAt(System.currentTimeMillis())
        return Result.success(UpdateSummary(added, updated, removed, unchanged))
    }
}
