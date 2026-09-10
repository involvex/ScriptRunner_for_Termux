package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptTemplate
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptTemplateRepository
import io.github.swiftstagrime.termuxrunner.ui.features.templates.TemplatesUiState
import io.github.swiftstagrime.termuxrunner.ui.features.templates.TemplatesViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TemplatesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScriptTemplateRepository
    private lateinit var viewModel: TemplatesViewModel

    private val sampleTemplates =
        listOf(
            ScriptTemplate(
                id = "bash_hello",
                name = "Hello World",
                description = "A simple bash script",
                content = "#!/bin/bash\necho 'Hello'",
                category = "bash",
            ),
            ScriptTemplate(
                id = "python_data",
                name = "Data Processing",
                description = "Python data script",
                content = "print('data')",
                category = "python",
            ),
            ScriptTemplate(
                id = "node_server",
                name = "HTTP Server",
                description = "Node.js server",
                content = "const http = require('http')",
                category = "node",
            ),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        viewModel = TemplatesViewModel(repository, testDispatcher)
        val initialState = viewModel.uiState.value
        assertTrue("Initial state should be Loading", initialState is TemplatesUiState.Loading)
    }

    @Test
    fun `loadTemplates sets state to Success with all templates`() =
        runTest(testDispatcher) {
            coEvery { repository.getTemplates() } returns sampleTemplates

            viewModel = TemplatesViewModel(repository, testDispatcher)

            viewModel.loadTemplates()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is TemplatesUiState.Success)
            val success = state as TemplatesUiState.Success
            assertEquals(3, success.templates.size)
            assertEquals("bash_hello", success.templates[0].id)
        }

    @Test
    fun `search with blank query returns all templates`() =
        runTest(testDispatcher) {
            coEvery { repository.getTemplates() } returns sampleTemplates

            viewModel = TemplatesViewModel(repository, testDispatcher)

            viewModel.search("")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is TemplatesUiState.Success)
            val success = state as TemplatesUiState.Success
            assertEquals(3, success.templates.size)
        }

    @Test
    fun `search with non-blank query returns filtered templates`() =
        runTest(testDispatcher) {
            val filteredResults = listOf(sampleTemplates[0])
            coEvery { repository.searchTemplates("bash") } returns filteredResults

            viewModel = TemplatesViewModel(repository, testDispatcher)

            viewModel.search("bash")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is TemplatesUiState.Success)
            val success = state as TemplatesUiState.Success
            assertEquals(1, success.templates.size)
            assertEquals("bash_hello", success.templates[0].id)
        }

    @Test
    fun `search with whitespace-only query returns all templates`() =
        runTest(testDispatcher) {
            coEvery { repository.getTemplates() } returns sampleTemplates

            viewModel = TemplatesViewModel(repository, testDispatcher)

            viewModel.search("   ")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is TemplatesUiState.Success)
            val success = state as TemplatesUiState.Success
            assertEquals(3, success.templates.size)
        }

    @Test
    fun `search returns empty list when no templates match`() =
        runTest(testDispatcher) {
            coEvery { repository.searchTemplates("nonexistent") } returns emptyList()

            viewModel = TemplatesViewModel(repository, testDispatcher)

            viewModel.search("nonexistent")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is TemplatesUiState.Success)
            val success = state as TemplatesUiState.Success
            assertTrue(success.templates.isEmpty())
        }
}
