package com.za.games.ui.about

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.setZaContent
import com.za.games.str
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsVersionAndRevealsApacheText() {
        rule.setZaContent { AboutScreen(onExit = {}) }
        rule.onNodeWithText(str(R.string.about_version_fmt, "").trim(), substring = true).assertIsDisplayed()
        val show = str(R.string.about_license_show)
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(show))
        rule.onNodeWithText(show).performClick()
        rule.onNodeWithText("Apache License", substring = true).assertExists()
    }
}
