package com.furka.music

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.furka.music.ui.screens.PlayerScreen
import com.furka.music.ui.viewmodel.PlayerViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry

@RunWith(AndroidJUnit4::class)
class PlayerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playerScreen_displaysTrackInfoAndControls() {
        // Set up the test content
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = PlayerViewModel(context.applicationContext as Application)
        composeTestRule.setContent {
            PlayerScreen(
                onDismiss = { },
                viewModel = viewModel
            )
        }

        // Verify that the track title is displayed
        composeTestRule.onNodeWithText("No Track").assertExists()

        // Verify that the artist name is displayed
        composeTestRule.onNodeWithText("Select a song").assertExists()

        // Verify that the play button is displayed
        composeTestRule.onNodeWithContentDescription("Play").assertExists()

        // Verify that the next button is displayed
        composeTestRule.onNodeWithContentDescription("Next").assertExists()

        // Verify that the previous button is displayed
        composeTestRule.onNodeWithContentDescription("Previous").assertExists()
    }
}
