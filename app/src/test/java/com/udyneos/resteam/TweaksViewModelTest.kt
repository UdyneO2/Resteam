package com.udyneos.resteam

import com.udyneos.resteam.domain.model.CompileFilter
import com.udyneos.resteam.domain.model.Game
import com.udyneos.resteam.domain.model.GameMode
import com.udyneos.resteam.domain.model.ShizukuMode
import com.udyneos.resteam.domain.model.ShizukuState
import com.udyneos.resteam.domain.model.TweakPreset
import com.udyneos.resteam.domain.repository.GameRepository
import com.udyneos.resteam.domain.repository.PresetRepository
import com.udyneos.resteam.domain.usecase.ApplyPresetUseCase
import com.udyneos.resteam.shizuku.ShizukuManager
import com.udyneos.resteam.ui.tweaks.PresetDraft
import com.udyneos.resteam.ui.tweaks.TweaksEvent
import com.udyneos.resteam.ui.tweaks.TweaksViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TweaksViewModelTest {

    private val gameRepository = mockk<GameRepository>()
    private val presetRepository = mockk<PresetRepository>(relaxed = true)
    private val applyPreset = mockk<ApplyPresetUseCase>()
    private val shizukuManager = mockk<ShizukuManager>(relaxed = true)

    private val game = Game(id = "com.test.game", title = "Test Game", packageName = "com.test.game")
    private val preset = TweakPreset(7, game.id, "Performance", GameMode.Performance)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { gameRepository.getGames() } returns listOf(game)
        every { presetRepository.observePresets(game.id) } returns flowOf(listOf(preset))
        every { shizukuManager.state } returns flowOf(ShizukuState.Ready(ShizukuMode.Adb))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TweaksViewModel(gameRepository, presetRepository, applyPreset, shizukuManager)

    @Test
    fun `memilih game pertama dan memuat presetnya`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(game.id, state.selectedGameId)
        assertEquals(listOf(preset), state.presets)
        assertEquals(true, state.canApply)
    }

    @Test
    fun `simpan draft baru memangkas nama dan memakai game terpilih`() = runTest {
        val viewModel = createViewModel()
        viewModel.onNewPreset()
        viewModel.onDraftChange(PresetDraft(name = "  Turbo  ", gameMode = GameMode.Performance, compileFilter = CompileFilter.Speed))

        viewModel.onSaveDraft()

        coVerify { presetRepository.save(TweakPreset(0, game.id, "Turbo", GameMode.Performance, CompileFilter.Speed)) }
        assertNull(viewModel.uiState.value.draft)
    }

    @Test
    fun `draft dengan nama kosong tidak disimpan`() = runTest {
        val viewModel = createViewModel()
        viewModel.onNewPreset()
        viewModel.onDraftChange(PresetDraft(name = "   "))

        viewModel.onSaveDraft()

        coVerify(exactly = 0) { presetRepository.save(any()) }
    }

    @Test
    fun `edit preset mengisi draft dari preset lama dan mempertahankan id`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEditPreset(preset)

        assertEquals(PresetDraft(7, "Performance", GameMode.Performance, null), viewModel.uiState.value.draft)
    }

    @Test
    fun `hapus preset meneruskan id ke repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.onDeletePreset(preset)

        coVerify { presetRepository.delete(7) }
    }

    @Test
    fun `apply ditolak saat Shizuku belum siap`() = runTest {
        every { shizukuManager.state } returns flowOf(ShizukuState.Unavailable)
        val viewModel = createViewModel()

        viewModel.onApplyPreset(preset)

        assertEquals(TweaksEvent.ShowMessage("Shizuku belum siap"), viewModel.events.first())
        coVerify(exactly = 0) { applyPreset(any(), any()) }
    }

    @Test
    fun `apply sukses mengirim pesan dengan nama game`() = runTest {
        coEvery { applyPreset(preset, game.packageName) } returns Result.success(emptyList())
        val viewModel = createViewModel()

        viewModel.onApplyPreset(preset)

        assertEquals(
            TweaksEvent.ShowMessage("Preset \"Performance\" diterapkan ke Test Game"),
            viewModel.events.first(),
        )
    }
}
