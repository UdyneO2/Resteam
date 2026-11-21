package com.udyneos.resteam.domain.usecase

import com.udyneos.resteam.domain.model.CommandFailedException
import com.udyneos.resteam.domain.model.CommandResult
import com.udyneos.resteam.domain.model.TweakPreset
import com.udyneos.resteam.domain.repository.ActivityLog
import com.udyneos.resteam.domain.repository.ShellExecutor
import javax.inject.Inject

/** Hanya perintah dari whitelist ini yang boleh dijalankan lewat Shizuku. */
internal fun buildCommands(preset: TweakPreset, packageName: String): List<List<String>> = buildList {
    add(listOf("cmd", "game", "set", "--mode", preset.gameMode.arg, packageName))
    preset.compileFilter?.let {
        add(listOf("cmd", "package", "compile", "-m", it.arg, "-f", packageName))
    }
}

class ApplyPresetUseCase @Inject constructor(
    private val shell: ShellExecutor,
    private val log: ActivityLog,
) {
    suspend operator fun invoke(
        preset: TweakPreset,
        packageName: String,
    ): Result<List<CommandResult>> {
        val result = execute(preset, packageName)
        record(preset, packageName, result)
        return result
    }

    private suspend fun execute(preset: TweakPreset, packageName: String): Result<List<CommandResult>> {
        if (!isValidPackageName(packageName)) {
            return Result.failure(IllegalArgumentException("Nama package tidak valid: $packageName"))
        }
        val results = mutableListOf<CommandResult>()
        for (command in buildCommands(preset, packageName)) {
            val result = shell.run(command).getOrElse { return Result.failure(it) }
            if (!result.isSuccess) return Result.failure(CommandFailedException(result))
            results += result
        }
        return Result.success(results)
    }

    private fun record(preset: TweakPreset, packageName: String, result: Result<List<CommandResult>>) {
        result.fold(
            onSuccess = { list ->
                log.record(
                    title = "Preset ${preset.name} diterapkan ($packageName)",
                    detail = list.joinToString("\n\n") { "$ ${it.command}\n${it.output.ifBlank { "(ok)" }}" },
                )
            },
            onFailure = {
                log.record(
                    title = "Preset ${preset.name} gagal ($packageName)",
                    detail = it.message ?: "Kesalahan tidak diketahui",
                    isError = true,
                )
            },
        )
    }
}
