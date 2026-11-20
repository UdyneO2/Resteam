package com.example.launcher

import com.example.launcher.domain.model.AppSettings
import com.example.launcher.domain.repository.GameRepository
import com.example.launcher.domain.repository.SettingsRepository
import com.example.launcher.ui.settings.SettingsViewModel
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val gameRepository = mockk<GameRepository>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { settingsRepository.settings } returns flowOf(AppSettings(userName = "Aldi"))
        every { gameRepository.hasUsageAccess() } returns false
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(settingsRepository, gameRepository)

    @Test
    fun `state menggabungkan settings dan status akses penggunaan`() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("Aldi", viewModel.uiState.value.settings.userName)
        assertEquals(false, viewModel.uiState.value.hasUsageAccess)
    }

    @Test
    fun `nama dipangkas dan dibatasi panjangnya`() = runTest {
        val viewModel = createViewModel()

        viewModel.onSaveUserName("  " + "A".repeat(40) + "  ")

        coVerify { settingsRepository.setUserName("A".repeat(SettingsViewModel.MAX_NAME_LENGTH)) }
    }

    @Test
    fun `nama kosong tidak disimpan`() = runTest {
        val viewModel = createViewModel()

        viewModel.onSaveUserName("   ")

        coVerify(exactly = 0) { settingsRepository.setUserName(any()) }
    }

    @Test
    fun `toggle konfirmasi diteruskan ke repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.onConfirmBeforeApplyChange(false)

        coVerify { settingsRepository.setConfirmBeforeApply(false) }
    }

    @Test
    fun `refresh membaca ulang akses penggunaan`() = runTest {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        every { gameRepository.hasUsageAccess() } returns true

        viewModel.refreshUsageAccess()

        assertTrue(viewModel.uiState.value.hasUsageAccess)
    }
}
