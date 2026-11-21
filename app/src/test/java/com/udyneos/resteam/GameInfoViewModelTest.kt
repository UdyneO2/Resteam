package com.udyneos.resteam

import com.udyneos.resteam.domain.model.CommandResult
import com.udyneos.resteam.domain.model.InfoRow
import com.udyneos.resteam.domain.model.ShizukuMode
import com.udyneos.resteam.domain.model.ShizukuState
import com.udyneos.resteam.domain.repository.GameInfoRepository
import com.udyneos.resteam.domain.repository.ShellExecutor
import com.udyneos.resteam.shizuku.ShizukuManager
import com.udyneos.resteam.ui.info.GameInfoViewModel
import com.udyneos.resteam.ui.info.GameModeInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameInfoViewModelTest {

    private val infoRepository = mockk<GameInfoRepository>()
    private val shell = mockk<ShellExecutor>()
    private val shizukuManager = mockk<ShizukuManager>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { infoRepository.getDeviceInfo() } returns listOf(InfoRow("Android", "14"))
        every { infoRepository.getAppInfo(any()) } returns listOf(InfoRow("Package", "com.test.game"))
        every { shizukuManager.state } returns flowOf(ShizukuState.Ready(ShizukuMode.Adb))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = GameInfoViewModel(infoRepository, shell, shizukuManager)

    @Test
    fun `info perangkat dan app dimuat`() = runTest {
        coEvery { shell.run(any()) } returns Result.success(CommandResult("x", 0, "ok"))
        val viewModel = createViewModel()

        viewModel.load("com.test.game")

        assertEquals(listOf(InfoRow("Android", "14")), viewModel.uiState.value.device)
        assertEquals(listOf(InfoRow("Package", "com.test.game")), viewModel.uiState.value.app)
    }

    @Test
    fun `game mode ditampilkan saat perintah sukses`() = runTest {
        coEvery { shell.run(listOf("cmd", "game", "list-modes", "com.test.game")) } returns
            Result.success(CommandResult("cmd game list-modes", 0, "current mode: performance"))
        val viewModel = createViewModel()

        viewModel.load("com.test.game")

        assertEquals(GameModeInfo.Loaded("current mode: performance"), viewModel.uiState.value.gameMode)
    }

    @Test
    fun `tanpa Shizuku siap perintah tidak dijalankan`() = runTest {
        every { shizukuManager.state } returns flowOf(ShizukuState.Unavailable)
        val viewModel = createViewModel()

        viewModel.load("com.test.game")

        assertTrue(viewModel.uiState.value.gameMode is GameModeInfo.Unavailable)
        coVerify(exactly = 0) { shell.run(any()) }
    }

    @Test
    fun `package tidak valid ditolak`() = runTest {
        val viewModel = createViewModel()

        viewModel.load("com.test; reboot")

        assertTrue(viewModel.uiState.value.gameMode is GameModeInfo.Unavailable)
        coVerify(exactly = 0) { shell.run(any()) }
    }

    @Test
    fun `exit code bukan nol dianggap tidak tersedia`() = runTest {
        coEvery { shell.run(any()) } returns Result.success(CommandResult("x", 1, ""))
        val viewModel = createViewModel()

        viewModel.load("com.test.game")

        assertTrue(viewModel.uiState.value.gameMode is GameModeInfo.Unavailable)
    }
}
