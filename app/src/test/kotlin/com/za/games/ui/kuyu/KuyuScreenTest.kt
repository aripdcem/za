package com.za.games.ui.kuyu

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.kuyu.KuyuWorld
import com.za.games.setZaContent
import com.za.games.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kuyu ekranı: tuş yok, tuvale dokunulur. İlk parmak oyuncuyu parmağın
 * sütununa yürütür, kalkınca durur; ikinci parmak yerdeyken zıplatır; tek
 * parmağın kısa dokunuşu ya da yukarı kaydırması zıplatır, kısa yürüme
 * dürtmesi zıplatmaz.
 */
@RunWith(AndroidJUnit4::class)
class KuyuScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_kuyu", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun startFreeRun(vm: KuyuViewModel) {
        rule.setZaContent { KuyuScreen(highScore = 0L, onScore = {}, onExit = {}, viewModel = vm) }
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.kuyu_start)).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(KuyuPhase.PLAYING, vm.phase.value)
    }

    private fun field() = rule.onNode(hasContentDescription(str(R.string.kuyu_board_desc, 0, 0, 0).substringBefore(':'), substring = true))

    @Test
    fun menuShowsTouchHintInsteadOfButtons() {
        rule.setZaContent { KuyuScreen(highScore = 0L, onScore = {}, onExit = {}) }
        rule.onNodeWithText(str(R.string.kuyu_intro)).assertExists()
        rule.onNodeWithText(str(R.string.kuyu_hint)).assertExists()
        rule.onNodeWithText(str(R.string.kuyu_hand_label)).assertDoesNotExist()
    }

    @Test
    fun heldFingerWalksTowardsItAndReleaseStops() {
        val vm = KuyuViewModel(ApplicationProvider.getApplicationContext())
        startFreeRun(vm)
        // Başlangıç platformu sol duvara yapışıktır: önce sağa, sonra geri sola yürünür.
        val field = field()
        field.performTouchInput { down(0, Offset(width - 1f, centerY)) }
        rule.mainClock.advanceTimeBy(200L)
        assertTrue("parmak sağda: sağa yürümeli (vx=${vm.world.player.vx})", vm.world.player.vx > 0f)
        field.performTouchInput {
            moveTo(0, Offset(1f, centerY))
            advanceEventTime(300L)
        }
        rule.mainClock.advanceTimeBy(100L)
        assertTrue("parmak sola kaydı: sola yürümeli (vx=${vm.world.player.vx})", vm.world.player.vx < 0f)
        field.performTouchInput { up(0) }
        rule.mainClock.advanceTimeBy(100L)
        assertEquals("parmak kalkınca durur", 0f, vm.world.player.vx, 1e-6f)
    }

    @Test
    fun secondFingerJumpsWhileGrounded() {
        val vm = KuyuViewModel(ApplicationProvider.getApplicationContext())
        startFreeRun(vm)
        assertTrue("koşu yerde başlar", vm.world.player.grounded)
        val field = field()
        val x = vm.world.player.centerX
        field.performTouchInput {
            down(0, Offset(width * x / KuyuWorld.WIDTH, centerY))
            down(1, Offset(width * 0.85f, centerY))
        }
        rule.mainClock.advanceTimeBy(60L)
        assertTrue("ikinci parmak zıplatır (vy=${vm.world.player.vy})", vm.world.player.vy < 0f)
        field.performTouchInput {
            up(1)
            up(0)
        }
    }

    @Test
    fun quickTapJumps() {
        val vm = KuyuViewModel(ApplicationProvider.getApplicationContext())
        startFreeRun(vm)
        assertTrue(vm.world.player.grounded)
        field().performTouchInput {
            down(0, center)
            up(0)
        }
        rule.mainClock.advanceTimeBy(60L)
        assertTrue("kısa dokunuş zıplatır (vy=${vm.world.player.vy})", vm.world.player.vy < 0f)
    }

    /** Cihaz bulgusu: 150–200 ms'lik yürüme dürtmesi 220 ms eşiğinde zıplatıyordu; eşik 130 ms. */
    @Test
    fun shortNudgeWalksWithoutJumping() {
        val vm = KuyuViewModel(ApplicationProvider.getApplicationContext())
        startFreeRun(vm)
        assertTrue(vm.world.player.grounded)
        field().performTouchInput {
            down(0, Offset(width - 1f, centerY))
            advanceEventTime(160L)
            up(0)
        }
        rule.mainClock.advanceTimeBy(60L)
        assertTrue("160 ms'lik dürtme zıplatmaz (vy=${vm.world.player.vy})", vm.world.player.vy >= 0f)
    }

    @Test
    fun flickUpJumpsWhileWalking() {
        val vm = KuyuViewModel(ApplicationProvider.getApplicationContext())
        startFreeRun(vm)
        val field = field()
        field.performTouchInput { down(0, Offset(width - 1f, height * 0.9f)) }
        rule.mainClock.advanceTimeBy(120L)
        assertTrue("yürüyor (vx=${vm.world.player.vx})", vm.world.player.vx > 0f)
        field.performTouchInput {
            moveBy(0, Offset(0f, -4f))
            moveBy(0, Offset(0f, -200f))
        }
        rule.mainClock.advanceTimeBy(60L)
        assertTrue("yukarı kaydırma zıplatır (vy=${vm.world.player.vy})", vm.world.player.vy < 0f)
        assertTrue("yürüme sürer (vx=${vm.world.player.vx})", vm.world.player.vx > 0f)
        field.performTouchInput { up(0) }
    }
}
