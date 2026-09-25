package com.dragonpi.timegem

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dragonpi.timegem.data.preferences.TimeGemPreferences
import com.dragonpi.timegem.feature.notes.NotesViewModel
import com.dragonpi.timegem.navigation.TimeGemNavigation
import com.dragonpi.timegem.ui.theme.TimeGemTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun editorRemainsVisibleAndShrinksDuringBackTransition() {
        compose.setContent {
            val app = LocalContext.current.applicationContext as android.app.Application
            val notes = remember { NotesViewModel(app) }
            TimeGemTheme {
                TimeGemNavigation(TimeGemPreferences(setupCompleted = true), {}, {}, { _, _, _ -> }, notes, {})
            }
        }
        compose.onNodeWithContentDescription("Create").performClick()
        compose.onNodeWithText("Text note").performClick()
        val before = compose.onNodeWithContentDescription("Title").fetchSemanticsNode().boundsInRoot
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Back").performClick()
        compose.mainClock.advanceTimeBy(160)
        val during = compose.onNodeWithContentDescription("Title").fetchSemanticsNode().boundsInRoot
        assertTrue("Editor should visibly shrink before it is removed", during.width < before.width - 1f)
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithContentDescription("Title").assertDoesNotExist()
        compose.onNodeWithContentDescription("Create").assertIsDisplayed()
    }
}
