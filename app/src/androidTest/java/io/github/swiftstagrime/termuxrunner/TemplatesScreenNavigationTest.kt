package io.github.swiftstagrime.termuxrunner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TemplatesScreenNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @Inject
    lateinit var settingsRepository: UserPreferencesRepository

    @Before
    fun init() {
        hiltRule.inject()

        runBlocking {
            settingsRepository.setOnboardingCompleted(true)
        }

        ActivityScenario.launch(MainActivity::class.java)

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            try {
                composeTestRule
                    .onNodeWithTag("fab_add_script")
                    .assertExists()
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    @Test
    fun navigateToTemplates_andDisplayBuiltinTemplates() {
        navigateToTemplatesScreen()

        composeTestRule
            .onNodeWithText("Hello World")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Simple Backup Script")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Data Processing")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Simple HTTP Server")
            .assertIsDisplayed()
    }

    @Test
    fun searchFiltersTemplatesByName() {
        navigateToTemplatesScreen()

        composeTestRule
            .onNodeWithTag("templates_search_field")
            .performTextReplacement("Hello")

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()
                true
            } catch (_: Throwable) {
                false
            }
        }

        composeTestRule
            .onNodeWithText("Hello World")
            .assertIsDisplayed()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeTestRule.onNodeWithText("Simple Backup Script").assertIsDisplayed()
                false
            } catch (_: Throwable) {
                true
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeTestRule.onNodeWithText("Data Processing").assertIsDisplayed()
                false
            } catch (_: Throwable) {
                true
            }
        }
    }

    @Test
    fun navigateBackReturnsToSettings() {
        navigateToTemplatesScreen()

        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
                true
            } catch (_: Throwable) {
                false
            }
        }

        composeTestRule
            .onNodeWithText("Settings")
            .assertIsDisplayed()
    }

    private fun navigateToTemplatesScreen() {
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeTestRule.onNodeWithText("Templates").assertIsDisplayed()
                true
            } catch (_: Throwable) {
                false
            }
        }

        composeTestRule
            .onNodeWithText("Templates")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeTestRule.onNodeWithText("Templates Library").assertIsDisplayed()
                true
            } catch (_: Throwable) {
                false
            }
        }
    }
}
