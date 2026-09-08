package com.za.games.ui.besharf

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
class BesHarfScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun submittingAShortWordShowsTheHint() {
        rule.setZaContent { game("besharf").screen(0L, {}, {}) }
        for (letter in listOf("K", "A", "L")) {
            rule.onNode(hasText(letter) and hasClickAction()).performClick()
        }
        rule.onNode(hasText(str(R.string.key_enter)) and hasClickAction()).performClick()
        rule.onNodeWithText(str(R.string.too_short)).assertIsDisplayed()
    }
}
