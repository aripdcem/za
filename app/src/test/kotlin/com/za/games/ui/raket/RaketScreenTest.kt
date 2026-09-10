package com.za.games.ui.raket

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.game
import com.za.games.setZaContent
import com.za.games.str
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Raket ekranı: menü çipleri ayrıntıyı değiştirir; duvar koşusu başlar, kort
 * erişilebilirlik açıklamasıyla ağaçtadır, duraklatılır ve başa dönülür.
 */
@RunWith(AndroidJUnit4::class)
class RaketScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_raket", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun modeChipsSwitchTheMenuDetails() {
        rule.setZaContent { game("raket").screen(0L, {}, {}) }
        rule.onNodeWithText(str(R.string.raket_mode_duo)).performClick()
        rule.onNodeWithText(str(R.string.raket_duo_hint)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.raket_mode_wall)).performClick()
        rule.onNodeWithText(str(R.string.raket_daily_desc)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.raket_wall_hint)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.raket_mode_solo)).performClick()
        rule.onNodeWithText(str(R.string.difficulty_hard)).performClick()
        rule.onNodeWithText(str(R.string.raket_solo_hint)).assertIsDisplayed()
    }

    @Test
    fun wallRunStartsPausesAndReturnsToTheMenu() {
        rule.setZaContent { game("raket").screen(0L, {}, {}) }
        rule.onNodeWithText(str(R.string.raket_mode_wall)).performClick()
        // Koşu başlayınca kare döngüsü sonsuzdur; saat elle ilerletilir (bkz. Yılan duman testi).
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.raket_start)).performClick()
        rule.mainClock.advanceTimeBy(2_000L)
        val courtPrefix = str(R.string.raket_wall_desc_fmt, 0).substringBeforeLast(' ')
        rule.onNode(hasContentDescription(courtPrefix, substring = true)).assertExists()
        rule.onNodeWithText(str(R.string.raket_start)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.pause)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.paused)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.raket_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.raket_start)).assertIsDisplayed()
    }
}
