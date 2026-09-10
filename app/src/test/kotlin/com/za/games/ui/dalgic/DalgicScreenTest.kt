package com.za.games.ui.dalgic

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
import com.za.games.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Dalgıç ekranı: menüde mod çipleri; dalış başlayınca tuvalde sürüklemek
 * denizaltıyı hedefe götürür, duraklatılıp başa dönülür.
 */
@RunWith(AndroidJUnit4::class)
class DalgicScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_dalgic", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun menuShowsRulesAndModes() {
        rule.setZaContent { DalgicScreen(highScore = 0L, onScore = {}, onExit = {}) }
        rule.onNodeWithText(str(R.string.dalgic_rules)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.dalgic_daily_desc)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.dalgic_daily_desc)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.dalgic_start)).assertIsDisplayed()
    }

    @Test
    fun draggingTheSeaMovesTheSubAndPauseReturnsToTheMenu() {
        val vm = DalgicViewModel(ApplicationProvider.getApplicationContext())
        rule.setZaContent { DalgicScreen(highScore = 0L, onScore = {}, onExit = {}, viewModel = vm) }
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.dalgic_start)).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(DalgicPhase.PLAYING, vm.phase.value)
        val prefix = str(R.string.dalgic_board_desc_fmt, 0, 0, 100).substringBefore(':')
        val sea = rule.onNode(hasContentDescription(prefix, substring = true))
        val x0 = vm.world.subX
        val y0 = vm.world.subY
        sea.performTouchInput {
            down(center)
            moveBy(Offset(width * 0.25f, height * 0.25f))
            up()
        }
        rule.mainClock.advanceTimeBy(1_500L)
        assertTrue("sürükleyince sağa ve aşağı gider: ${vm.world.subX},${vm.world.subY} / $x0,$y0", vm.world.subX > x0 + 0.1f && vm.world.subY > y0 + 0.1f)
        assertEquals(1, vm.world.facing)
        rule.onNodeWithText(str(R.string.pause)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.paused)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.dalgic_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.dalgic_start)).assertIsDisplayed()
    }
}
