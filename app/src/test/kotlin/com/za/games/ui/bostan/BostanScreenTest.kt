package com.za.games.ui.bostan

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.down
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.bostan.BostanDifficulty
import com.za.games.bostan.BostanLevel
import com.za.games.bostan.BostanState
import com.za.games.bostan.DefenderKind
import com.za.games.bostan.EnemyKind
import com.za.games.bostan.Spawn
import com.za.games.bostan.Wave
import com.za.games.setZaContent
import com.za.games.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bostan ekranı: menüde mod ve zorluk çipleri; koşu başlayınca kart seçip
 * hücreye dokunmak savunma koyar ve su düşer, duraklatılıp başa dönülür.
 */
@RunWith(AndroidJUnit4::class)
class BostanScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_bostan", Context.MODE_PRIVATE).edit().clear().commit()
    }

    /** Üreticiyi atlar: uzman doğrulaması testin işi değil; tek karga 30 s sonra gelir. */
    private val quickLevel: (Long, BostanDifficulty) -> BostanLevel = { seed, d ->
        BostanLevel(seed, d, listOf(Wave(listOf(Spawn(30f, 2, EnemyKind.KARGA)), false)), 1f, 3, 0)
    }

    @Test
    fun menuShowsRulesModesAndDifficulties() {
        rule.setZaContent { BostanScreen(highScore = 0L, onScore = {}, onExit = {}) }
        rule.onNodeWithText(str(R.string.bostan_rules)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.bostan_daily_desc)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.difficulty_hard)).performClick()
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.bostan_daily_desc)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.bostan_start)).assertIsDisplayed()
    }

    /** Serbest koşuyu başlatır ve arka plandaki üretimi bekler; saat elle ilerletilir. */
    private fun startFreeRun(vm: BostanViewModel) {
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.bostan_start)).performClick()
        val deadline = System.currentTimeMillis() + 10_000
        while (vm.phase.value != BostanPhase.PLAYING && System.currentTimeMillis() < deadline) {
            rule.waitForIdle()
            Thread.sleep(20)
        }
        assertEquals(BostanPhase.PLAYING, vm.phase.value)
        rule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun aNearbyDropIsCollectedBeforePlantingEvenWithACardSelected() {
        val vm = BostanViewModel(ApplicationProvider.getApplicationContext(), quickLevel)
        rule.setZaContent { BostanScreen(highScore = 0L, onScore = {}, onExit = {}, viewModel = vm) }
        startFreeRun(vm)
        vm.state.dropForTest(2, 5)
        val water0 = vm.state.water
        rule.onNode(hasContentDescription(str(R.string.bostan_card_desc_fmt, str(R.string.bostan_def_kuyu), DefenderKind.KUYU.cost))).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(DefenderKind.KUYU, vm.selected.value)
        val prefix = str(R.string.bostan_board_desc_fmt, 0, 0, 0, 0).substringBefore(':')
        val field = rule.onNode(hasContentDescription(prefix, substring = true))
        // Damlanın altındaki hücreye (2,6), üst kenara yakın dokunuş: hücre farklı ama damlaya 0,6 hücre yakın.
        field.performTouchInput {
            val c = bostanCellCenter(width.toFloat(), height.toFloat(), 2, 6)
            val above = bostanCellCenter(width.toFloat(), height.toFloat(), 2, 5)
            val p = Offset(c.x, c.y + (above.y - c.y) * 0.4f)
            down(p)
            up()
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("damla toplandı, ekim olmadı", water0 + BostanState.DROP_WATER, vm.state.water)
        assertNull(vm.state.defenderAt(2, 6))
        assertNull(vm.state.defenderAt(2, 5))
        assertTrue(vm.state.drops.isEmpty())
        assertEquals("kart seçimi kalır", DefenderKind.KUYU, vm.selected.value)
    }

    @Test
    fun selectingACardAndTappingACellPlacesAWellThenPauseReturnsToTheMenu() {
        val vm = BostanViewModel(ApplicationProvider.getApplicationContext(), quickLevel)
        rule.setZaContent { BostanScreen(highScore = 0L, onScore = {}, onExit = {}, viewModel = vm) }
        // Kare döngüsü sonsuzdur; saat elle ilerletilir. Seviye arka planda üretilir,
        // sonuç ana iş parçacığına gönderilir: kuyruk boşaltılmadan (waitForIdle) evre değişmez.
        startFreeRun(vm)
        val water0 = vm.state.water
        assertNull(vm.state.defenderAt(2, 6))
        val cardDesc = str(R.string.bostan_card_desc_fmt, str(R.string.bostan_def_kuyu), DefenderKind.KUYU.cost)
        rule.onNode(hasContentDescription(cardDesc)).performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(DefenderKind.KUYU, vm.selected.value)
        val prefix = str(R.string.bostan_board_desc_fmt, 0, 0, 0, 0).substringBefore(':')
        val field = rule.onNode(hasContentDescription(prefix, substring = true))
        field.performTouchInput {
            val c = bostanCellCenter(width.toFloat(), height.toFloat(), 2, 6)
            down(c)
            up()
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(DefenderKind.KUYU, vm.state.defenderAt(2, 6)?.kind)
        assertEquals(water0 - DefenderKind.KUYU.cost, vm.state.water)
        assertNull("yerleştirince seçim kalkar", vm.selected.value)
        rule.mainClock.advanceTimeBy(1_000L)
        rule.onNodeWithText(str(R.string.pause)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.paused)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.bostan_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.bostan_start)).assertIsDisplayed()
    }
}
