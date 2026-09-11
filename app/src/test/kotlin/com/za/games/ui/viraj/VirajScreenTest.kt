package com.za.games.ui.viraj

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Viraj ekranı: tuş yok, tuvale dokunulur. Sol yarı sola, sağ yarı sağa kırar;
 * orta şerit ya da ikinci parmak fren; parmak kalkınca direksiyon düz, fren
 * bırakılır.
 */
@RunWith(AndroidJUnit4::class)
class VirajScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_viraj", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun startFreeRun(vm: VirajViewModel) {
        rule.setZaContent { VirajScreen(highScore = 0L, onScore = {}, onExit = {}, viewModel = vm) }
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.viraj_start)).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(VirajPhase.PLAYING, vm.phase.value)
    }

    private fun road() = rule.onNode(hasContentDescription(str(R.string.viraj_board_desc, 0, 0, 0).substringBefore(':'), substring = true))

    @Test
    fun menuShowsTouchHintInsteadOfButtons() {
        rule.setZaContent { VirajScreen(highScore = 0L, onScore = {}, onExit = {}) }
        rule.onNodeWithText(str(R.string.viraj_intro)).assertExists()
        rule.onNodeWithText(str(R.string.viraj_touch_hint)).assertExists()
        rule.onNodeWithText(str(R.string.viraj_ctrl_brake)).assertDoesNotExist()
    }

    @Test
    fun halvesSteerMiddleAndSecondFingerBrake() {
        val vm = VirajViewModel(ApplicationProvider.getApplicationContext())
        startFreeRun(vm)
        val road = road()
        road.performTouchInput { down(0, Offset(width * 0.1f, centerY)) }
        assertEquals("sol yarı sola kırar", -1, vm.world.steer)
        assertFalse(vm.world.brake)

        road.performTouchInput { down(1, Offset(width * 0.9f, centerY)) }
        assertEquals("ikinci parmak direksiyonu değiştirmez", -1, vm.world.steer)
        assertTrue("ikinci parmak fren", vm.world.brake)

        road.performTouchInput { up(1) }
        assertEquals(-1, vm.world.steer)
        assertFalse("ikinci parmak kalkınca fren bırakılır", vm.world.brake)

        road.performTouchInput { moveTo(0, Offset(width * 0.5f, centerY)) }
        assertEquals("orta şerit düz", 0, vm.world.steer)
        assertTrue("orta şerit fren", vm.world.brake)

        road.performTouchInput { moveTo(0, Offset(width * 0.9f, centerY)) }
        assertEquals("sağ yarı sağa kırar", 1, vm.world.steer)
        assertFalse(vm.world.brake)

        road.performTouchInput { up(0) }
        assertEquals("parmak kalkınca düz", 0, vm.world.steer)
        assertFalse(vm.world.brake)
    }

    @Test
    fun steeringSurvivesTheSimulationAndPauseWorks() {
        val vm = VirajViewModel(ApplicationProvider.getApplicationContext())
        startFreeRun(vm)
        val road = road()
        road.performTouchInput { down(0, Offset(width * 0.9f, centerY)) }
        val x0 = vm.world.playerX
        rule.mainClock.advanceTimeBy(500L)
        assertEquals(1, vm.world.steer)
        assertTrue("sağa kırılınca araç sağa kayar (${vm.world.playerX} / $x0)", vm.world.playerX > x0)
        road.performTouchInput { up(0) }
        rule.onNodeWithText(str(R.string.pause)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.paused)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.viraj_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.viraj_start)).assertIsDisplayed()
    }
}
