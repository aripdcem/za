package com.za.games.ui.kakuro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.game
import com.za.games.setZaContent
import com.za.games.str
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KakuroScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun easyPuzzleGeneratesAndNotesModeIsAnnounced() {
        rule.setZaContent { game("kakuro").screen(0L, {}, {}) }
        rule.onNodeWithText(str(R.string.difficulty_easy)).performClick()
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodes(hasContentDescription("Kakuro", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription(str(R.string.notes_toggle)).performClick()
        rule.onNodeWithText(str(R.string.kakuro_notes_on), substring = true).assertIsDisplayed()
    }
}
