package com.dragonpi.timegem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dragonpi.timegem.data.preferences.Appearance
import com.dragonpi.timegem.feature.setup.SetupScreen
import com.dragonpi.timegem.ui.theme.TimeGemTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun headingAndOptionsDoNotOverlapAndAppearanceCanChange() {
        val appearance = mutableStateOf(Appearance())
        compose.setContent {
            TimeGemTheme(appearance = appearance.value) {
                Box(Modifier.width(360.dp)) {
                    SetupScreen(appearance.value, { appearance.value = it }, false, {})
                }
            }
        }
        val heading = compose.onNodeWithText("Make it yours").fetchSemanticsNode().boundsInRoot
        val calendar = compose.onNodeWithText("Calendar", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("The feature rows must start below the heading", calendar.top >= heading.bottom)
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Find your colors").assertIsDisplayed()
        compose.onNodeWithText("Light", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Find your colors").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Make it yours").assertIsDisplayed()
    }
}
