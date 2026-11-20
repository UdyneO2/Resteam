package com.example.launcher.domain.model

enum class GameMode(val arg: String, val label: String) {
    Standard("standard", "Standard"),
    Performance("performance", "Performance"),
    Battery("battery", "Battery");

    companion object {
        fun fromArg(arg: String): GameMode = entries.firstOrNull { it.arg == arg } ?: Standard
    }
}

enum class CompileFilter(val arg: String) {
    SpeedProfile("speed-profile"),
    Speed("speed");

    companion object {
        fun fromArg(arg: String?): CompileFilter? = entries.firstOrNull { it.arg == arg }
    }
}

data class TweakPreset(
    val id: Long,
    val gameId: String,
    val name: String,
    val gameMode: GameMode,
    val compileFilter: CompileFilter? = null,
) {
    fun describe(): String = buildString {
        append("Game mode: ${gameMode.label}")
        compileFilter?.let { append(" • Compile: ${it.arg}") }
    }
}

data class CommandResult(
    val command: String,
    val exitCode: Int,
    val output: String,
) {
    val isSuccess: Boolean get() = exitCode == 0
}

class CommandFailedException(val result: CommandResult) :
    Exception("Perintah gagal (exit ${result.exitCode}): ${result.output.ifBlank { result.command }}")

/** Profil per game: preset default dan apakah otomatis diterapkan saat game diluncurkan dari launcher. */
data class GameProfile(
    val gameId: String,
    val presetId: Long?,
    val autoApply: Boolean,
)
