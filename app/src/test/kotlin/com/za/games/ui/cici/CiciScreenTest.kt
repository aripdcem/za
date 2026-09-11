package com.za.games.ui.cici

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
 * Cici ekranı: menüde bölüm etiketi ve mod çipleri; uçuş başlayınca tuvalde
 * sürüklemek Cici'yi hedefe götürür, duraklatılıp başa dönülür.
 */
@RunWith(AndroidJUnit4::class)
class CiciScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_cici", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun menuShowsChapterRulesAndModes() {
        rule.setZaContent { CiciScreen(highScore = 0L, onScore = {}, onExit = {}) }
        rule.onNodeWithText(str(R.string.cici_chapter_space)).assertExists()
        rule.onNodeWithText(str(R.string.cici_rules)).assertExists()
        rule.onNodeWithText(str(R.string.cici_daily_desc)).assertExists()
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.cici_daily_desc)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.cici_start)).assertIsDisplayed()
    }

    @Test
    fun draggingSpaceMovesCiciAndPauseReturnsToTheMenu() {
        val vm = CiciViewModel(ApplicationProvider.getApplicationContext())
        rule.setZaContent { CiciScreen(highScore = 0L, onScore = {}, onExit = {}, viewModel = vm) }
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.cici_start)).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(CiciPhase.PLAYING, vm.phase.value)
        val prefix = str(R.string.cici_board_desc_fmt, 0, 3, 0).substringBefore(':')
        val space = rule.onNode(hasContentDescription(prefix, substring = true))
        val x0 = vm.world.ciciX
        val y0 = vm.world.ciciY
        space.performTouchInput {
            down(center)
            moveBy(Offset(width * 0.25f, -height * 0.2f))
            up()
        }
        rule.mainClock.advanceTimeBy(1_000L)
        assertTrue("sürükleyince sağa ve yukarı gider: ${vm.world.ciciX},${vm.world.ciciY} / $x0,$y0", vm.world.ciciX > x0 + 0.1f && vm.world.ciciY < y0 - 0.1f)
        assertEquals(1, vm.world.facing)
        assertEquals("hareket sayacı sıfırlandı", 0f, vm.world.idleSeconds, 1.2f)
        rule.onNodeWithText(str(R.string.pause)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.paused)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.cici_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.cici_start)).assertIsDisplayed()
    }
}
