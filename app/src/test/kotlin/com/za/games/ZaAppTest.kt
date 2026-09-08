package com.za.games

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.ui.theme.ZaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Uygulama kökü: ana menü, oyun açma/kapama, Hakkında ve ayar düğmeleri. */
@RunWith(AndroidJUnit4::class)
class ZaAppTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun launch() {
        rule.setContent { ZaTheme { ZaApp() } }
    }

    @Test
    fun opensGameAndReturnsToHubWithRecentRow() {
        launch()
        rule.onAllNodesWithText(str(R.string.play))[0].performClick()
        rule.onNodeWithText(titleOf(R.string.game_tetris)).assertIsDisplayed()
        rule.onNodeWithContentDescription(str(R.string.back)).performClick()
        rule.onNodeWithText(str(R.string.hub_tagline)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.hub_recent).uppercase(Locale.getDefault())).assertIsDisplayed()
    }

    @Test
    fun aboutOpensFromHubAndBackReturns() {
        launch()
        rule.onNodeWithContentDescription(str(R.string.about_title)).performClick()
        rule.onNodeWithText(str(R.string.about_version_fmt, "").trim(), substring = true).assertIsDisplayed()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText(str(R.string.hub_tagline)).assertIsDisplayed()
    }

    @Test
    fun soundToggleFlipsItsDescription() {
        launch()
        rule.onNodeWithContentDescription(str(R.string.sound_off)).performClick()
        rule.onNodeWithContentDescription(str(R.string.sound_on)).assertIsDisplayed()
    }

    @Test
    fun firstLaunchShowsNoWhatsNewCard() {
        launch()
        rule.onNodeWithText(str(R.string.whats_new_title)).assertDoesNotExist()
    }
}
