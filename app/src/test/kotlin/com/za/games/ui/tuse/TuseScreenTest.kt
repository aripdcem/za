package com.za.games.ui.tuse

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.game
import com.za.games.setZaContent
import com.za.games.str
import com.za.games.tuse.TuseWorld
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tuşe ekranı: menüde mod çipleri ve parça seçici ayrıntıyı değiştirir; Klasik
 * koşuda doğru şeride dokunmak karoyu vurur, yanlış şerit koşuyu bitirir.
 */
@RunWith(AndroidJUnit4::class)
class TuseScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("za_tuse", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun menuChipsAndSongPickerChangeTheDetails() {
        rule.setZaContent { game("tuse").screen(0L, {}, {}) }
        rule.onNodeWithText(str(R.string.tuse_song_ode)).assertIsDisplayed()
        rule.onNodeWithContentDescription(str(R.string.tuse_next_song)).performClick()
        rule.onNodeWithText(str(R.string.tuse_song_elise)).assertIsDisplayed()
        rule.onNodeWithContentDescription(str(R.string.tuse_prev_song)).performClick()
        rule.onNodeWithText(str(R.string.tuse_song_ode)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.mode_daily)).performClick()
        rule.onNodeWithText(str(R.string.tuse_daily_desc)).assertIsDisplayed()
        rule.onNodeWithContentDescription(str(R.string.tuse_next_song)).assertDoesNotExist()
        rule.onNodeWithText(str(R.string.tuse_mode_arcade)).performClick()
        rule.onNodeWithContentDescription(str(R.string.tuse_next_song)).assertIsDisplayed()
    }

    @Test
    fun classicRunHitsTheNextLaneAndAWrongLaneEndsIt() {
        rule.setZaContent { game("tuse").screen(0L, {}, {}) }
        // Koşu başlayınca kare döngüsü sonsuzdur; saat elle ilerletilir.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText(str(R.string.tuse_start)).performClick()
        rule.mainClock.advanceTimeByFrame()
        val prefix = str(R.string.tuse_board_desc_fmt, 0, 1).substringBefore(':')
        val board = rule.onNode(hasContentDescription(prefix, substring = true))

        fun nextLane(): Int = board.description().substringAfterLast(' ').toInt() - 1

        fun tapLane(lane: Int) {
            board.performTouchInput { click(Offset(width * (lane + 0.5f) / TuseWorld.LANES, height * 0.9f)) }
            rule.mainClock.advanceTimeByFrame()
        }

        tapLane(nextLane())
        rule.onNodeWithText("1/${TuseWorld.CLASSIC_TILES}").assertExists()
        assertEquals(1, board.description().substringAfter(": ").substringBefore(' ').toInt())
        tapLane((nextLane() + 1) % TuseWorld.LANES)
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.tuse_wrong_key)).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.tuse_to_menu)).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(str(R.string.tuse_start)).assertIsDisplayed()
    }

    private fun SemanticsNodeInteraction.description(): String =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() ?: ""
}
