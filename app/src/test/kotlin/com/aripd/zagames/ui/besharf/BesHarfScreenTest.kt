package com.aripd.zagames.ui.besharf

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aripd.zagames.R
import com.aripd.zagames.game
import com.aripd.zagames.setZaContent
import com.aripd.zagames.str
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
