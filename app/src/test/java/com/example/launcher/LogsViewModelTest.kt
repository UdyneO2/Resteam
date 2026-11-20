package com.example.launcher

import com.example.launcher.domain.model.LogEntry
import com.example.launcher.domain.repository.ActivityLog
import com.example.launcher.ui.logs.LogsViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogsViewModelTest {

    private val activityLog = mockk<ActivityLog>(relaxed = true)
    private val older = LogEntry(1L, "lama", "", false)
    private val newer = LogEntry(2L, "baru", "", true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { activityLog.entries } returns MutableStateFlow(listOf(older, newer))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `entri terbaru ditampilkan paling atas`() = runTest {
        val viewModel = LogsViewModel(activityLog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.entries.collect {} }

        assertEquals(listOf(newer, older), viewModel.entries.value)
    }

    @Test
    fun `bersihkan meneruskan ke ActivityLog`() = runTest {
        val viewModel = LogsViewModel(activityLog)

        viewModel.onClear()

        verify(exactly = 1) { activityLog.clear() }
    }
}
