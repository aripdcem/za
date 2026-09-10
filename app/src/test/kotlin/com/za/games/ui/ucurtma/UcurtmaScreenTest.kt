package com.za.games.ui.ucurtma

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.str
import com.za.games.setZaContent
import com.za.games.ucurtma.Missions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uçurtma ekranı: menüde görevler ve kilitli ekipman görünür; koşu başlayınca
 * tuvale basmak ipi çeker (uçurtma yükselir), duraklatılıp başa dönülür.
 */
@RunWith(AndroidJUnit4::class)
class UcurtmaScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_ucurtma", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun menuListsMissionsAndLockedGear() {
        rule.setZaContent { UcurtmaScreen(highScore = 0L, onScore = {}, onExit = {}) }
        rule.onNodeWithText("• " + str(R.string.ucurtma_m_distance_fmt, Missions.at(0).target)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.ucurtma_completed_fmt, 0), substring = true).assertIsDisplayed()
        rule.onNodeWithText("🔒 " + str(R.string.ucurtma_gadget_tail)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.ucurtma_daily_desc)).assertDoesNotExist()
    }

    @Test
    fun holdingTheSkyPullsTheKiteUpAndPauseReturnsToTheMenu() {
        val vm = UcurtmaViewModel(ApplicationProvider.getApplicationContext())
        rule.setZaContent { UcurtmaScreen(highScore = 0L, onScore = {}, onExit = {}, viewModel = vm) }
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        // Koşu başlayınca kare döngüsü sonsuzdur; saat elle ilerletilir.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.ucurtma_start)).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(UcurtmaPhase.PLAYING, vm.phase.value)
        val prefix = str(R.string.ucurtma_board_desc_fmt, 0, 0).substringBefore(':')
        val sky = rule.onNode(hasContentDescription(prefix, substring = true))
        val before = vm.world.kiteY
        sky.performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(500L)
        assertTrue("basılıyken yükselir: ${vm.world.kiteY} / $before", vm.world.holding && vm.world.kiteY < before)
        sky.performTouchInput { up() }
        rule.mainClock.advanceTimeByFrame()
        assertTrue(!vm.world.holding)
        rule.onNodeWithText(str(R.string.pause)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.paused)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.ucurtma_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.ucurtma_start)).assertIsDisplayed()
    }
}
