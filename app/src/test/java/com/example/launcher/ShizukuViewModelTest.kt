package com.example.launcher

import com.example.launcher.domain.model.CommandResult
import com.example.launcher.domain.model.ShizukuMode
import com.example.launcher.domain.model.ShizukuState
import com.example.launcher.domain.repository.ShellExecutor
import com.example.launcher.shizuku.ShizukuManager
import com.example.launcher.ui.shizuku.DiagnosticState
import com.example.launcher.ui.shizuku.ShizukuViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShizukuViewModelTest {

    private val manager = mockk<ShizukuManager>(relaxed = true)
    private val shell = mockk<ShellExecutor>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun collectState(viewModel: ShizukuViewModel, scope: kotlinx.coroutines.CoroutineScope, dispatcher: kotlinx.coroutines.CoroutineDispatcher) {
        scope.launch(dispatcher) { viewModel.uiState.collect {} }
    }

    @Test
    fun `diagnostik diabaikan saat Shizuku belum siap`() = runTest {
        every { manager.state } returns flowOf(ShizukuState.Unavailable)
        val viewModel = ShizukuViewModel(manager, shell)

        viewModel.onRunDiagnostic()

        coVerify(exactly = 0) { shell.run(any()) }
    }

    @Test
    fun `diagnostik sukses menampilkan output`() = runTest {
        every { manager.state } returns flowOf(ShizukuState.Ready(ShizukuMode.Adb))
        coEvery { shell.run(any()) } returns Result.success(CommandResult("id", 0, "uid=2000(shell)"))
        val viewModel = ShizukuViewModel(manager, shell)
        collectState(viewModel, backgroundScope, UnconfinedTestDispatcher(testScheduler))

        viewModel.onRunDiagnostic()

        val diagnostic = viewModel.uiState.value.diagnostic as DiagnosticState.Finished
        assertTrue(diagnostic.success)
        assertTrue(diagnostic.output.contains("uid=2000"))
    }

    @Test
    fun `exit code bukan nol menandai diagnostik gagal`() = runTest {
        every { manager.state } returns flowOf(ShizukuState.Ready(ShizukuMode.Adb))
        coEvery { shell.run(any()) } returns Result.success(CommandResult("id", 1, "denied"))
        val viewModel = ShizukuViewModel(manager, shell)
        collectState(viewModel, backgroundScope, UnconfinedTestDispatcher(testScheduler))

        viewModel.onRunDiagnostic()

        assertFalse((viewModel.uiState.value.diagnostic as DiagnosticState.Finished).success)
    }

    @Test
    fun `meminta izin diteruskan ke manager`() = runTest {
        every { manager.state } returns flowOf(ShizukuState.PermissionRequired)
        val viewModel = ShizukuViewModel(manager, shell)

        viewModel.onRequestPermission()

        verify(exactly = 1) { manager.requestPermission() }
    }
}
