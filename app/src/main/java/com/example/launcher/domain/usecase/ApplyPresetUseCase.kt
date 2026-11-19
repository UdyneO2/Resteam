package com.example.launcher.domain.usecase

import com.example.launcher.domain.model.CommandFailedException
import com.example.launcher.domain.model.CommandResult
import com.example.launcher.domain.model.TweakPreset
import com.example.launcher.domain.repository.ShellExecutor
import javax.inject.Inject

private val PACKAGE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

/** Hanya perintah dari whitelist ini yang boleh dijalankan lewat Shizuku. */
internal fun buildCommands(preset: TweakPreset, packageName: String): List<List<String>> = buildList {
    add(listOf("cmd", "game", "set", "--mode", preset.gameMode.arg, packageName))
    preset.compileFilter?.let {
        add(listOf("cmd", "package", "compile", "-m", it.arg, "-f", packageName))
    }
}

class ApplyPresetUseCase @Inject constructor(
    private val shell: ShellExecutor,
) {
    suspend operator fun invoke(
        preset: TweakPreset,
        packageName: String,
    ): Result<List<CommandResult>> {
        if (!PACKAGE_REGEX.matches(packageName)) {
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
}
