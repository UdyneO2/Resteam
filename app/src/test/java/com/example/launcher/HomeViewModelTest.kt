package com.example.launcher

import com.example.launcher.data.GameLauncher
import com.example.launcher.domain.model.AppSettings
import com.example.launcher.domain.model.CommandResult
import com.example.launcher.domain.model.Game
import com.example.launcher.domain.model.GameMode
import com.example.launcher.domain.model.GameProfile
import com.example.launcher.domain.model.LogEntry
import com.example.launcher.domain.model.ShizukuMode
import com.example.launcher.domain.model.ShizukuState
import com.example.launcher.domain.model.TweakPreset
import com.example.launcher.domain.repository.GameRepository
import com.example.launcher.domain.repository.ActivityLog
import com.example.launcher.domain.repository.PresetRepository
import com.example.launcher.domain.repository.ProfileRepository
import com.example.launcher.domain.repository.SettingsRepository
import com.example.launcher.domain.usecase.ApplyPresetUseCase
import com.example.launcher.shizuku.ShizukuManager
import com.example.launcher.ui.home.ContentState
import com.example.launcher.ui.home.HomeEvent
import com.example.launcher.ui.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class HomeViewModelTest {

    private val repository = mockk<GameRepository>()
    private val presetRepository = mockk<PresetRepository>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository>()
    private val applyPreset = mockk<ApplyPresetUseCase>()
    private val activityLog = mockk<ActivityLog>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()
    private val shizukuManager = mockk<ShizukuManager>(relaxed = true)
    private val gameLauncher = mockk<GameLauncher>()

    private val game = Game(id = "com.test.game", title = "Test Game", packageName = "com.test.game", hoursPlayed = 10)
    private val otherGame = Game(id = "com.test.other", title = "Other Game", packageName = "com.test.other")
    private val preset = TweakPreset(1, "com.test.game", "Performance", GameMode.Performance)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { shizukuManager.state } returns flowOf(ShizukuState.Ready(ShizukuMode.Adb))
        every { presetRepository.observePresets(any()) } returns flowOf(listOf(preset))
        every { settingsRepository.settings } returns flowOf(AppSettings())
        coEvery { profileRepository.getProfile(any()) } returns null
        every { activityLog.entries } returns MutableStateFlow(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stubRepositorySuccess() {
        coEvery { repository.getGames() } returns listOf(game, otherGame)
        coEvery { repository.getMonthlyActivity(any()) } returns listOf(null, 3f)
        every { repository.hasUsageAccess() } returns true
    }

    private fun createViewModel() =
        HomeViewModel(repository, presetRepository, profileRepository, applyPreset, activityLog, settingsRepository, shizukuManager, gameLauncher)

    @Test
    fun `load sukses menghasilkan ContentState Success dan memuat preset`() = runTest {
        stubRepositorySuccess()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertTrue(viewModel.uiState.value.content is ContentState.Success)
        assertEquals(ShizukuState.Ready(ShizukuMode.Adb), viewModel.uiState.value.shizuku)
        assertEquals(listOf(preset), viewModel.uiState.value.tweaks.presets)
        assertEquals(AppSettings(), viewModel.uiState.value.settings)
    }

    @Test
    fun `load gagal menghasilkan ContentState Error`() = runTest {
        coEvery { repository.getGames() } throws IllegalStateException("offline")
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val content = viewModel.uiState.value.content
        assertTrue(content is ContentState.Error)
        assertEquals("offline", (content as ContentState.Error).message)
    }

    @Test
    fun `launch game gagal mengirim pesan ke UI`() = runTest {
        stubRepositorySuccess()
        every { gameLauncher.launch(game.packageName) } returns Result.failure(IllegalStateException("Game belum terpasang"))
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onLaunchGame()

        assertEquals(HomeEvent.ShowMessage("Game belum terpasang"), viewModel.events.first())
    }

    @Test
    fun `klik pill meminta izin hanya saat status PermissionRequired`() = runTest {
        stubRepositorySuccess()
        every { shizukuManager.state } returns flowOf(ShizukuState.PermissionRequired)
        val viewModel = createViewModel()

        viewModel.onShizukuPillClick()

        verify(exactly = 1) { shizukuManager.requestPermission() }
    }

    @Test
    fun `apply preset sukses menutup sheet`() = runTest {
        stubRepositorySuccess()
        coEvery { applyPreset(preset, game.packageName) } returns
            Result.success(listOf(CommandResult("cmd game set", 0, "")))
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.onTweakPresetClick()

        viewModel.onApplyPreset(preset)

        val tweaks = viewModel.uiState.value.tweaks
        assertTrue(!tweaks.showPresetSheet)
        assertTrue(!tweaks.isApplying)
    }

    @Test
    fun `apply preset ditolak saat Shizuku belum siap`() = runTest {
        stubRepositorySuccess()
        every { shizukuManager.state } returns flowOf(ShizukuState.Unavailable)
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onApplyPreset(preset)

        assertEquals(HomeEvent.ShowMessage("Shizuku belum siap"), viewModel.events.first())
        coVerify(exactly = 0) { applyPreset(any(), any()) }
    }

    @Test
    fun `tanpa game terdeteksi menghasilkan NoGames`() = runTest {
        coEvery { repository.getGames() } returns emptyList()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(ContentState.NoGames, viewModel.uiState.value.content)
    }

    @Test
    fun `game pertama dipilih default dan bisa diganti`() = runTest {
        stubRepositorySuccess()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        fun selectedId() = (viewModel.uiState.value.content as ContentState.Success).dashboard.selected.id
        assertEquals(game.id, selectedId())

        viewModel.onGameSelected(otherGame)

        assertEquals(otherGame.id, selectedId())
        coVerify { repository.getMonthlyActivity(otherGame.packageName) }
    }

    @Test
    fun `auto apply menerapkan preset sebelum membuka game`() = runTest {
        stubRepositorySuccess()
        coEvery { profileRepository.getProfile(game.id) } returns GameProfile(game.id, preset.id, autoApply = true)
        coEvery { applyPreset(preset, game.packageName) } returns Result.success(emptyList())
        every { gameLauncher.launch(game.packageName) } returns Result.success(Unit)
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onLaunchGame()

        coVerifyOrder {
            applyPreset(preset, game.packageName)
            gameLauncher.launch(game.packageName)
        }
    }

    @Test
    fun `auto apply dilewati saat Shizuku belum siap tetapi game tetap dibuka`() = runTest {
        stubRepositorySuccess()
        every { shizukuManager.state } returns flowOf(ShizukuState.Unavailable)
        coEvery { profileRepository.getProfile(game.id) } returns GameProfile(game.id, preset.id, autoApply = true)
        every { gameLauncher.launch(game.packageName) } returns Result.success(Unit)
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onLaunchGame()

        coVerify(exactly = 0) { applyPreset(any(), any()) }
        verify(exactly = 1) { gameLauncher.launch(game.packageName) }
        assertEquals(HomeEvent.ShowMessage("Shizuku belum siap, game dibuka tanpa preset"), viewModel.events.first())
    }

    @Test
    fun `kegagalan preset tidak menghalangi game dibuka`() = runTest {
        stubRepositorySuccess()
        coEvery { profileRepository.getProfile(game.id) } returns GameProfile(game.id, preset.id, autoApply = true)
        coEvery { applyPreset(preset, game.packageName) } returns Result.failure(IllegalStateException("gagal"))
        every { gameLauncher.launch(game.packageName) } returns Result.success(Unit)
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onLaunchGame()

        verify(exactly = 1) { gameLauncher.launch(game.packageName) }
    }

    @Test
    fun `profil tanpa auto apply langsung membuka game`() = runTest {
        stubRepositorySuccess()
        coEvery { profileRepository.getProfile(game.id) } returns GameProfile(game.id, preset.id, autoApply = false)
        every { gameLauncher.launch(game.packageName) } returns Result.success(Unit)
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onLaunchGame()

        coVerify(exactly = 0) { applyPreset(any(), any()) }
        verify(exactly = 1) { gameLauncher.launch(game.packageName) }
    }

    @Test
    fun `log aktivitas bersama diteruskan ke state`() = runTest {
        stubRepositorySuccess()
        val entry = LogEntry(1L, "judul", "detail", isError = false)
        every { activityLog.entries } returns MutableStateFlow(listOf(entry))
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(listOf(entry), viewModel.uiState.value.tweaks.log)
    }

    @Test
    fun `bersihkan log memanggil ActivityLog`() = runTest {
        stubRepositorySuccess()
        val viewModel = createViewModel()

        viewModel.onClearLog()

        verify(exactly = 1) { activityLog.clear() }
    }

    @Test
    fun `tombol info membuka dan menutup dialog`() = runTest {
        stubRepositorySuccess()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onInfoClick()
        assertTrue(viewModel.uiState.value.tweaks.showInfo)

        viewModel.onDismissInfo()
        assertTrue(!viewModel.uiState.value.tweaks.showInfo)
    }
}
