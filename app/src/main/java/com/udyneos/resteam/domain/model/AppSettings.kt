package com.udyneos.resteam.domain.model

data class AppSettings(
    val userName: String = DEFAULT_NAME,
    val confirmBeforeApply: Boolean = true,
    /** Toggle global di halaman Profile: script Shizuku aktif hanya dijalankan jika true. */
    val shizukuTweaksEnabled: Boolean = false,
    /** Sumber script (owner/repo) dan branch yang dipilih di aplikasi. */
    val scriptRepo: String = DEFAULT_SCRIPT_REPO,
    val scriptBranch: String = DEFAULT_SCRIPT_BRANCH,
    /** Sumber script di GitHub; branch dipilih user di aplikasi. */
    val scriptRepo: String = DEFAULT_SCRIPT_REPO,
    val scriptBranch: String = DEFAULT_SCRIPT_BRANCH,
    /** Epoch millis terakhir kali daftar script berhasil diperbarui; 0 = belum pernah. */
    val scriptsLastUpdatedAt: Long = 0L,
) {
    companion object {
        const val DEFAULT_NAME = "Player"
        const val DEFAULT_SCRIPT_REPO = "Udyneo2/Resteam"
        const val DEFAULT_SCRIPT_BRANCH = "main"
        const val DEFAULT_SCRIPT_REPO = "Udyneo2/Resteam"
        const val DEFAULT_SCRIPT_BRANCH = "main"
    }
}
