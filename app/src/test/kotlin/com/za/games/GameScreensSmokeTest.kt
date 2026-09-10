package com.za.games

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/**
 * Her oyun ekranı çökmeden açılır ve üst çubukta adını gösterir. Liste elle
 * tutulur; kayıt defteriyle eşleşmesi HubScreenTest'te doğrulanır.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class GameScreensSmokeTest(private val gameId: String) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun games(): List<Array<Any>> = listOf(
            "tetris", "2048", "snake", "sudoku", "mines", "kiskac", "besharf", "turetme",
            "dizgi", "kuyu", "gecit", "tavla", "balkon", "kakuro", "vergici", "toplam", "viraj", "filo", "reyon", "raket", "tuse", "ucurtma",
        ).map { arrayOf<Any>(it) }
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun screenOpensAndShowsItsTitle() {
        val entry = game(gameId)
        // Yılan sonsuz bir animasyon kullanır; saat elle ilerletilmezse beklemede kalınır.
        val manualClock = gameId == "snake"
        if (manualClock) rule.mainClock.autoAdvance = false
        rule.setZaContent { entry.screen(0L, {}, {}) }
        if (manualClock) {
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()
        }
        // Başlık başka yerde de geçebilir (Vergici'de skor kartı etiketi); en az bir düğüm yeter.
        val title = titleOf(entry.titleRes)
        assertTrue("$gameId başlığı yok: $title", rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty())
    }
}
