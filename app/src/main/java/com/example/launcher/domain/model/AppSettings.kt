package com.example.launcher.domain.model

data class AppSettings(
    val userName: String = DEFAULT_NAME,
    val confirmBeforeApply: Boolean = true,
) {
    companion object {
        const val DEFAULT_NAME = "Player"
    }
}
