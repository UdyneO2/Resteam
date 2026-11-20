package com.example.launcher

import com.example.launcher.domain.model.ManagedApp
import com.example.launcher.domain.repository.GameRepository
import com.example.launcher.ui.manage.ManageGamesViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManageGamesViewModelTest {

    private val repository = mockk<GameRepository>()
    private val chess = ManagedApp("com.chess.app", "Chess", isGame = true, detectedAsGame = true)
    private val notes = ManagedApp("com.notes.app", "Notes", isGame = false, detectedAsGame = false)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { repository.getInstalledApps() } returns listOf(chess, notes)
        coEvery { repository.setGameIncluded(any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `memuat semua aplikasi saat dibuat`() = runTest {
        val viewModel = ManageGamesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(listOf(chess, notes), viewModel.uiState.value.apps)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `pencarian memfilter berdasarkan judul atau package`() = runTest {
        val viewModel = ManageGamesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onQueryChange("NOTES")

        assertEquals(listOf(notes), viewModel.uiState.value.apps)
    }

    @Test
    fun `toggle menyimpan pilihan dan memperbarui baris`() = runTest {
        val viewModel = ManageGamesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onToggle(notes, included = true)

        coVerify { repository.setGameIncluded("com.notes.app", true) }
        assertTrue(viewModel.uiState.value.apps.first { it.packageName == notes.packageName }.isGame)
    }

    @Test
    fun `gagal memuat menampilkan error`() = runTest {
        coEvery { repository.getInstalledApps() } throws IllegalStateException("gagal")
        val viewModel = ManageGamesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals("gagal", viewModel.uiState.value.error)
    }
}
