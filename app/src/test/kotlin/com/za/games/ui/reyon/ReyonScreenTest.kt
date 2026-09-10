package com.za.games.ui.reyon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.game
import com.za.games.setZaContent
import com.za.games.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReyonScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun clearSavedState() = ReyonTestSupport.clearPrefs()

    @After
    fun leaveRoundAndClear() {
        rule.reyonLeaveRound()
        ReyonTestSupport.clearPrefs()
    }

    private fun openMenu() = rule.reyonOpenMenu()

    @Test
    fun hintsPlaceProductsUndoReturnsThemAndThePuzzleGetsSolved() {
        rule.setZaContent { game("reyon").screen(0L, {}, {}) }
        openMenu()
        rule.reyonPickKind(str(R.string.reyon_kind_puzzle))
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.difficulty_easy)).performClick()
        rule.onNodeWithText(str(R.string.reyon_start)).performClick()

        val trayPrefix = str(R.string.reyon_tray_label) + ":"
        fun trayItems() = rule.onAllNodes(hasContentDescription(trayPrefix, substring = true))
        // Bulmaca arka planda üretilir; tepsi gelene dek bekle.
        rule.waitUntil(timeoutMillis = 30_000) { trayItems().fetchSemanticsNodes().isNotEmpty() }
        val before = trayItems().fetchSemanticsNodes().size
        assertTrue("tepside ürün olmalı", before > 0)

        rule.onNodeWithText(str(R.string.reyon_hint)).performClick()
        assertEquals(before - 1, trayItems().fetchSemanticsNodes().size)
        rule.onNodeWithText(str(R.string.undo)).performClick()
        assertEquals(before, trayItems().fetchSemanticsNodes().size)

        var guard = 0
        while (trayItems().fetchSemanticsNodes().isNotEmpty() && guard++ < 20) {
            rule.onNodeWithText(str(R.string.reyon_hint)).performClick()
        }
        rule.onNodeWithText(str(R.string.congrats)).assertIsDisplayed()
    }

    @Test
    fun auditHintsRevealEveryDeviation() {
        rule.setZaContent { game("reyon").screen(0L, {}, {}) }
        openMenu()
        rule.reyonPickKind(str(R.string.reyon_kind_audit))
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.difficulty_easy)).performClick()
        rule.onNodeWithText(str(R.string.reyon_audit_start)).performClick()

        val shelfDesc = str(R.string.reyon_audit_board_desc_fmt, 0, 0, 0, 0).substringBefore(' ')
        fun shelf() = rule.onAllNodes(hasContentDescription(shelfDesc, substring = true))
        rule.waitUntil(timeoutMillis = 30_000) { shelf().fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(str(R.string.reyon_audit_plan_label)).assertIsDisplayed()

        // Her ipucu bir sapma açar; Kolay'da iki sapma var.
        var guard = 0
        while (rule.onAllNodesWithText(str(R.string.reyon_audit_done_title)).fetchSemanticsNodes().isEmpty() && guard++ < 8) {
            rule.onNodeWithText(str(R.string.reyon_hint)).performClick()
        }
        rule.onNodeWithText(str(R.string.reyon_audit_done_title)).assertIsDisplayed()
    }

    @Test
    fun salesPlacesAProductByTappingTheShelfAndUndoReturnsIt() {
        rule.setZaContent { game("reyon").screen(0L, {}, {}) }
        openMenu()
        rule.reyonPickKind(str(R.string.reyon_kind_sales))
        rule.onNodeWithText(str(R.string.mode_free)).performClick()
        rule.onNodeWithText(str(R.string.difficulty_easy)).performClick()
        rule.onNodeWithText(str(R.string.reyon_sales_start)).performClick()

        val trayPrefix = str(R.string.reyon_tray_label) + ":"
        fun trayItems() = rule.onAllNodes(hasContentDescription(trayPrefix, substring = true))
        rule.waitUntil(timeoutMillis = 30_000) { trayItems().fetchSemanticsNodes().isNotEmpty() }
        val before = trayItems().fetchSemanticsNodes().size
        assertTrue("tepside ürün olmalı", before > 0)

        // İlk ürünü seç, rafın ortasına dokun: boş raf, yerleşmeli.
        trayItems()[0].performClick()
        val shelfDesc = str(R.string.reyon_sales_board_desc_fmt, 0, 0, 0, 0).substringBefore(' ')
        rule.onNode(hasContentDescription(shelfDesc, substring = true)).performClick()
        assertEquals(before - 1, trayItems().fetchSemanticsNodes().size)
        rule.onNodeWithText(str(R.string.undo)).performClick()
        assertEquals(before, trayItems().fetchSemanticsNodes().size)
    }
}
