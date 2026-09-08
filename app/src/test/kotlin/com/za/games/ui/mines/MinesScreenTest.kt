package com.za.games.ui.mines

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.game
import com.za.games.mines.MinesDifficulty
import com.za.games.setZaContent
import com.za.games.str
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MinesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun board(flags: Int): String {
        val d = MinesDifficulty.EASY
        return str(R.string.mines_board_desc, d.width, d.height, d.mineCount, flags)
    }

    @Test
    fun longPressPlacesAndRemovesAFlag() {
        rule.setZaContent { game("mines").screen(0L, {}, {}) }
        rule.onNodeWithText(str(R.string.difficulty_easy)).performClick()
        rule.onNodeWithContentDescription(board(0)).assertIsDisplayed()
        rule.onNodeWithContentDescription(board(0)).performTouchInput { longClick(center) }
        rule.onNodeWithContentDescription(board(1)).assertIsDisplayed()
        rule.onNodeWithContentDescription(board(1)).performTouchInput { longClick(center) }
        rule.onNodeWithContentDescription(board(0)).assertIsDisplayed()
    }
}
