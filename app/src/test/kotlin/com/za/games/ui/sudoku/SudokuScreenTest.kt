package com.za.games.ui.sudoku

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.game
import com.za.games.setZaContent
import com.za.games.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SudokuScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun digitKey(digit: Int): SemanticsMatcher {
        val labels = (0..9).map { str(R.string.sudoku_digit_desc_fmt, digit, it) }
        return SemanticsMatcher("rakam tuşu $digit") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it in labels } == true
        }
    }

    @Test
    fun pickDifficultyThenEnterDigitFillsTheSelectedCell() {
        rule.setZaContent { game("sudoku").screen(0L, {}, {}) }
        rule.onNodeWithText(str(R.string.difficulty_easy)).performClick()

        val empty = str(R.string.sudoku_cell_empty)
        fun emptyCells() = rule.onAllNodes(hasContentDescription(empty, substring = true))
        val before = emptyCells().fetchSemanticsNodes().size
        assertTrue("boş hücre yok", before > 0)

        emptyCells()[0].performClick()
        for (digit in 1..9) {
            rule.onNode(digitKey(digit)).performClick()
            if (emptyCells().fetchSemanticsNodes().size < before) break
        }
        assertEquals(before - 1, emptyCells().fetchSemanticsNodes().size)
    }
}
