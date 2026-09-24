package com.dragonpi.timegem

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dragonpi.timegem.data.notes.Note
import com.dragonpi.timegem.feature.notes.NoteEditorScreen
import com.dragonpi.timegem.ui.theme.TimeGemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NoteEditorScreenTest {
    @get:Rule val compose = createComposeRule()
    private val saved = mutableListOf<Note>()
    private var exits = 0
    private lateinit var back: OnBackPressedDispatcher
    private val saving = mutableStateOf(false)
    private val error = mutableStateOf<String?>(null)

    private fun editor(note: Note = Note("draft"), prompt: Boolean = false) {
        compose.setContent {
            back = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            TimeGemTheme {
                NoteEditorScreen(note, note.updatedAt == 0L, saving.value, error.value,
                    onSave = { saved.add(it) }, onDelete = {}, onBack = { exits++ }, askBeforeSaving = prompt)
            }
        }
    }

    @Test fun leavingNonemptyDraftSavesOnceAndKeepsSaveLabelStable() {
        editor()
        compose.onNodeWithText("Note", substring = false).performTextInput("A useful idea")
        val before = compose.onNodeWithText("Save").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled().performClick()
        val after = compose.onNodeWithText("Save").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            assertEquals(1, saved.size)
            assertEquals("A useful idea", saved.single().body)
            assertEquals(before, after)
        }
    }

    @Test fun backAutomaticallySavesNonemptyDraft() {
        editor()
        compose.onNodeWithText("Title").performTextInput("Remember this")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertEquals("Remember this", saved.single().title) }
    }

    @Test fun whitespaceDraftIsNotSaved() {
        editor()
        compose.onNodeWithText("Note", substring = false).performTextInput("   ")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertEquals(0, saved.size); assertEquals(1, exits) }
    }

    @Test fun clearingExistingNoteDoesNotOverwriteOriginal() {
        editor(Note("existing", body = "Keep this", updatedAt = 1L))
        compose.onNodeWithText("Note", substring = false).performTextClearance()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertEquals(0, saved.size); assertEquals(1, exits) }
    }

    @Test fun optionalPromptCanKeepEditing() {
        editor(prompt = true)
        compose.onNodeWithText("Title").performTextInput("Draft")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Save your changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("Draft").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, saved.size); assertEquals(0, exits) }
    }

    @Test fun cancelledPredictiveBackKeepsDraftAndCommittedBackSaves() {
        editor()
        compose.onNodeWithText("Title").performTextInput("Gesture draft")
        compose.runOnIdle { back.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { back.dispatchOnBackProgressed(BackEventCompat(80f, 200f, 0.4f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { back.dispatchOnBackCancelled() }
        compose.runOnIdle { assertEquals(0, saved.size); assertEquals(0, exits) }
        compose.onNodeWithText("Gesture draft").assertIsDisplayed()
        compose.runOnIdle { back.onBackPressed() }
        compose.runOnIdle { assertEquals(1, saved.size) }
    }

    @Test fun failedSaveCanBeRetriedWithoutLosingText() {
        editor()
        compose.onNodeWithText("Title").performTextInput("Retry me")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { saving.value = true }
        compose.runOnIdle { saving.value = false; error.value = "Could not save" }
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, saved.size); assertEquals("Retry me", saved.last().title) }
    }
}
