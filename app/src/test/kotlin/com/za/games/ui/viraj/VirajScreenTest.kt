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
 * Viraj ekranı: tuş ve bölge yok, tuval sürüklenir. Yatay sürükleme aracın
 * hedef çizgisini kaydırır (orantılı direksiyon), parmağı aşağı çekmek ya da
 * ikinci parmak fren yapar; parmak kalkınca fren bırakılır, çizgi korunur.
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
    fun draggingSteersProportionallyAndBothBrakesWork() {
        val vm = VirajViewModel(ApplicationProvider.getApplicationContext())
        startFreeRun(vm)
        val road = road()
        road.performTouchInput { down(0, center) }
        assertEquals("basış tek başına direksiyon kırmaz", 0f, vm.world.targetX, 1e-4f)
        assertFalse(vm.world.brake)

        road.performTouchInput { moveBy(0, Offset(width * 0.25f, 0f)) }
        assertTrue("sürükleme hedefi sağa taşır: ${vm.world.targetX}", vm.world.targetX > 0.2f)
        val x0 = vm.world.playerX
        // Yanal yetki hızla artar (2 × hız kesri); araç duruştan kalkarken yavaş kırar.
        rule.mainClock.advanceTimeBy(1_500L)
        assertTrue("araç hedefe gider: ${vm.world.playerX} / $x0", vm.world.playerX > x0 + 0.15f)
        assertTrue("orantılı direksiyon: ${vm.world.steer}", vm.world.steer > 0f && vm.world.steer <= 1f)

        road.performTouchInput { down(1, Offset(width * 0.2f, centerY)) }
        assertTrue("ikinci parmak fren", vm.world.brake)
        road.performTouchInput { up(1) }
        assertFalse("ikinci parmak kalkınca fren bırakılır", vm.world.brake)

        road.performTouchInput { moveBy(0, Offset(0f, 260f)) }
        assertTrue("parmağı aşağı çekmek fren", vm.world.brake)
        road.performTouchInput { up(0) }
        assertFalse("parmak kalkınca fren bırakılır", vm.world.brake)

        // Parmak kalkınca araç son çizgisini tutmayı sürdürür.
        val before = vm.world.playerX
        rule.mainClock.advanceTimeBy(400L)
        assertTrue("çizgiye gitmeyi sürdürür: $before → ${vm.world.playerX}", vm.world.playerX >= before)
    }

    @Test
    fun steeringSurvivesTheSimulationAndPauseWorks() {
        val vm = VirajViewModel(ApplicationProvider.getApplicationContext())
        startFreeRun(vm)
        val road = road()
        road.performTouchInput {
            down(0, center)
            moveBy(0, Offset(width * 0.3f, 0f))
        }
        val x0 = vm.world.playerX
        rule.mainClock.advanceTimeBy(1_500L)
        assertTrue("sağa sürüklenince araç sağa kayar (${vm.world.playerX} / $x0)", vm.world.playerX > x0)
        road.performTouchInput { up(0) }
        rule.onNodeWithText(str(R.string.pause)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.paused)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.viraj_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.viraj_start)).assertIsDisplayed()
    }
}
