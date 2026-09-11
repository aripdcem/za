package com.za.games.ui.cekirge

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Çekirge ekranı: menüde mod çipleri; koşu başlayınca tuvale dokunmak
 * fıskırtır, yatay sürüklemek çiftçiyi yürütür, duraklatılıp başa dönülür.
 */
@RunWith(AndroidJUnit4::class)
class CekirgeScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_cekirge", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun menuShowsRulesAndModes() {
        rule.setZaContent { CekirgeScreen(highScore = 0L, onScore = {}, onExit = {}) }
        rule.onNodeWithText(str(R.string.cekirge_rules)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cekirge_daily_desc)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.cekirge_daily_desc)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.cekirge_start)).assertIsDisplayed()
    }

    @Test
    fun tapSpraysDragWalksAndPauseReturnsToTheMenu() {
        val vm = CekirgeViewModel(ApplicationProvider.getApplicationContext())
        rule.setZaContent { CekirgeScreen(highScore = 0L, onScore = {}, onExit = {}, viewModel = vm) }
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.cekirge_start)).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(CekirgePhase.PLAYING, vm.phase.value)
        val prefix = str(R.string.cekirge_board_desc_fmt, 0, 0, 0).substringBefore(':')
        val field = rule.onNode(hasContentDescription(prefix, substring = true))
        field.performTouchInput {
            down(center)
            up()
        }
        assertNotNull("dokunuş fıskırtır", vm.world.shot)
        val x0 = vm.world.farmerX
        field.performTouchInput {
            down(center)
            moveBy(Offset(width * 0.25f, 0f))
            up()
        }
        assertTrue("sürükleme yürütür: ${vm.world.farmerX} / $x0", vm.world.farmerX > x0 + 0.1f)
        rule.mainClock.advanceTimeBy(500L)
        rule.onNodeWithText(str(R.string.pause)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.paused)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cekirge_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.cekirge_start)).assertIsDisplayed()
    }
}
