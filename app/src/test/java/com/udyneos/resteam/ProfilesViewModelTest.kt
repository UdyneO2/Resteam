package com.udyneos.resteam

import com.udyneos.resteam.domain.model.Game
import com.udyneos.resteam.domain.model.GameMode
import com.udyneos.resteam.domain.model.GameProfile
import com.udyneos.resteam.domain.model.TweakPreset
import com.udyneos.resteam.domain.repository.GameRepository
import com.udyneos.resteam.domain.repository.PresetRepository
import com.udyneos.resteam.domain.repository.ProfileRepository
import com.udyneos.resteam.ui.profiles.ProfilesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesViewModelTest {

    private val gameRepository = mockk<GameRepository>()
    private val presetRepository = mockk<PresetRepository>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository>(relaxed = true)

    private val game = Game(id = "com.test.game", title = "Test Game", packageName = "com.test.game")
    private val preset = TweakPreset(1, game.id, "Performance", GameMode.Performance)
    private val otherPreset = TweakPreset(2, "com.other", "Other", GameMode.Battery)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { gameRepository.getGames() } returns listOf(game)
        every { presetRepository.observeAllPresets() } returns flowOf(listOf(preset, otherPreset))
        every { profileRepository.observeProfiles() } returns flowOf(emptyMap())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(scope: kotlinx.coroutines.CoroutineScope, dispatcher: kotlinx.coroutines.CoroutineDispatcher): ProfilesViewModel {
        val viewModel = ProfilesViewModel(gameRepository, presetRepository, profileRepository)
        scope.launch(dispatcher) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `baris hanya berisi preset milik game tersebut`() = runTest {
        val viewModel = createViewModel(backgroundScope, UnconfinedTestDispatcher(testScheduler))

        val row = viewModel.uiState.value.rows.single()
        assertEquals(listOf(preset), row.presets)
        assertNull(row.selectedPresetId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `memilih preset menyimpan profil tanpa auto apply`() = runTest {
        val viewModel = createViewModel(backgroundScope, UnconfinedTestDispatcher(testScheduler))

        viewModel.onPresetSelected(game, preset.id)

        coVerify { profileRepository.save(GameProfile(game.id, preset.id, autoApply = false)) }
    }

    @Test
    fun `auto apply tidak bisa diaktifkan tanpa preset terpilih`() = runTest {
        val viewModel = createViewModel(backgroundScope, UnconfinedTestDispatcher(testScheduler))

        viewModel.onAutoApplyChange(game, true)

        coVerify(exactly = 0) { profileRepository.save(any()) }
    }

    @Test
    fun `auto apply menyimpan preset yang sedang dipilih`() = runTest {
        every { profileRepository.observeProfiles() } returns flowOf(mapOf(game.id to GameProfile(game.id, preset.id, false)))
        val viewModel = createViewModel(backgroundScope, UnconfinedTestDispatcher(testScheduler))

        viewModel.onAutoApplyChange(game, true)

        coVerify { profileRepository.save(GameProfile(game.id, preset.id, autoApply = true)) }
    }

    @Test
    fun `preset yang sudah dihapus dianggap tidak dipilih`() = runTest {
        every { profileRepository.observeProfiles() } returns flowOf(mapOf(game.id to GameProfile(game.id, 99, true)))
        val viewModel = createViewModel(backgroundScope, UnconfinedTestDispatcher(testScheduler))

        val row = viewModel.uiState.value.rows.single()
        assertNull(row.selectedPresetId)
        assertTrue(!row.autoApply)
    }
}
