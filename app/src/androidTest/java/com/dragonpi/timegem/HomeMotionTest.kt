package com.dragonpi.timegem

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dragonpi.timegem.data.notes.Note
import com.dragonpi.timegem.data.preferences.TimeGemPreferences
import com.dragonpi.timegem.feature.home.HomeScreen
import com.dragonpi.timegem.feature.notes.NotesState
import com.dragonpi.timegem.ui.theme.TimeGemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeMotionTest {
    @get:Rule val compose = createComposeRule()
    private var creations = 0

    private fun home() {
        val preferences = mutableStateOf(TimeGemPreferences(setupCompleted = true))
        compose.setContent {
            TimeGemTheme {
                HomeScreen(preferences.value, NotesState.Ready(listOf(Note("one", body = "Test idea"))),
                    {}, { creations++ }, { _, _, _ -> }, {}, { preferences.value = preferences.value.copy(homeOptions = it) }, {}, {})
            }
        }
        compose.mainClock.advanceTimeBy(3200)
    }

    @Test fun rapidCreateTapsReverseWithoutLosingTheButton() {
        home()
        compose.mainClock.autoAdvance = false
        repeat(12) { index ->
            compose.onNodeWithContentDescription(if (index % 2 == 0) "Create" else "Close create menu").performClick()
            compose.mainClock.advanceTimeBy(48)
        }
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithContentDescription("Create").assertIsDisplayed().performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Text note").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle { assertEquals(1, creations) }
        compose.onNodeWithContentDescription("Create").assertIsDisplayed()
    }

    @Test fun workspaceRoundTripKeepsNotesAndSearchCanClose() {
        home()
        compose.onNodeWithText("Week").performClick()
        compose.onNodeWithText("Your week").assertIsDisplayed()
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithText("A place for your reminders and daily routines. These features are coming next.").assertIsDisplayed()
        compose.onAllNodesWithText("Notes").onLast().performClick()
        compose.onNodeWithText("Test idea").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search Time Gem").performClick()
        compose.onNodeWithText("Images").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close search").performClick()
        compose.onNodeWithText("Images").assertDoesNotExist()
    }
}
