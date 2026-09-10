package com.za.games.ui.sincap

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.setZaContent
import com.za.games.sincap.Side
import com.za.games.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sincap ekranı: menüde mod çipleri; tırmanış başlayınca tuvalin sağ
 * yarısına dokunmak sincabı sağdaki üst dala atlatır, duraklatılıp başa dönülür.
 */
@RunWith(AndroidJUnit4::class)
class SincapScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_sincap", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun menuShowsRulesAndModes() {
        rule.setZaContent { SincapScreen(highScore = 0L, onScore = {}, onExit = {}) }
        rule.onNodeWithText(str(R.string.sincap_rules)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.sincap_daily_desc)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.sincap_daily_desc)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.sincap_start)).assertIsDisplayed()
    }

    @Test
    fun tappingTheRightHalfJumpsToTheRightBranchAndPauseReturnsToTheMenu() {
        val vm = SincapViewModel(ApplicationProvider.getApplicationContext())
        rule.setZaContent { SincapScreen(highScore = 0L, onScore = {}, onExit = {}, viewModel = vm) }
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.sincap_start)).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(SincapPhase.PLAYING, vm.phase.value)
        assertEquals(0, vm.world.level)
        val prefix = str(R.string.sincap_board_desc_fmt, 0, 0).substringBefore(':')
        val tree = rule.onNode(hasContentDescription(prefix, substring = true))
        tree.performTouchInput {
            down(Offset(width * 0.8f, height * 0.5f))
            up()
        }
        assertTrue("ilk temasta zıplar", vm.world.jumping)
        rule.mainClock.advanceTimeBy(1_000L)
        assertEquals(1, vm.world.level)
        assertEquals(Side.RIGHT, vm.world.side)
        assertEquals(1, vm.hud.value.height)
        rule.onNodeWithText(str(R.string.pause)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.paused)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.sincap_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.sincap_start)).assertIsDisplayed()
    }
}
