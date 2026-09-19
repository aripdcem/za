package com.za.games.ui.hub

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.za.games.R
import com.za.games.platform.GameRegistry
import com.za.games.platform.ZaLocale
import com.za.games.setZaContent
import com.za.games.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(selected: String = ZaLocale.SYSTEM, onPick: (String) -> Unit = {}) {
        rule.setZaContent {
            LanguageScreen(selected = selected, effective = "en", onPick = onPick, onExit = {})
        }
    }

    @Test
    fun listsEverySupportedLanguageByItsOwnName() {
        // Açık bir seçimle: "telefonun dili" satırı o zaman alt satırda dil adı
        // yazmaz, her ad listede tek geçer.
        show(selected = "en")
        val list = rule.onNodeWithTag(LANGUAGE_LIST_TAG)
        for (tag in ZaLocale.TAGS) {
            val name = ZaLocale.endonym(tag)
            list.performScrollToNode(hasText(name))
            rule.onNodeWithText(name).assertIsDisplayed()
        }
    }

    @Test
    fun offersThePhoneLanguageAndNamesWhatItResolvedTo() {
        // Seçim "telefonun dili"ndeyken satır hangi dile düşüldüğünü alt satırda
        // yazar: "English" o zaman iki kez geçer (bu satır + listedeki İngilizce).
        show(selected = ZaLocale.SYSTEM)
        rule.onNodeWithText(str(R.string.language_system)).assertIsDisplayed()
        assertEquals(2, rule.onAllNodesWithText(ZaLocale.endonym("en")).fetchSemanticsNodes().size)
    }

    @Test
    fun anExplicitChoiceDropsTheResolvedLanguageLine() {
        // Türkçe seçiliyken "telefonun dili" satırı alt satır yazmaz: "English"
        // yalnız listedeki İngilizce satırında geçer.
        show(selected = "tr")
        assertEquals(1, rule.onAllNodesWithText(ZaLocale.endonym("en")).fetchSemanticsNodes().size)
    }

    @Test
    fun pickingALanguageReportsItsTag() {
        var picked: String? = null
        show(selected = ZaLocale.SYSTEM, onPick = { picked = it })
        val list = rule.onNodeWithTag(LANGUAGE_LIST_TAG)
        list.performScrollToNode(hasText(ZaLocale.endonym("de")))
        rule.onNodeWithText(ZaLocale.endonym("de")).performClick()
        assertEquals("de", picked)
    }

    @Test
    fun theWordGameNoteIsShown() {
        show()
        val note = str(R.string.language_words_note)
        rule.onNodeWithTag(LANGUAGE_LIST_TAG).performScrollToNode(hasText(note))
        rule.onNodeWithText(note).assertIsDisplayed()
        // Not, TR/EN ile sınırlı dört oyunu adıyla saymalı.
        for (id in listOf(R.string.game_besharf, R.string.game_kiskac, R.string.game_turetme, R.string.game_dizgi)) {
            assertTrue("notta ${str(id)} geçmeli", note.contains(str(id)))
        }
    }

    @Test
    fun theHubHeaderShowsTheCurrentLanguageCode() {
        rule.setZaContent {
            HubScreen(
                games = GameRegistry.games,
                highScores = emptyMap(),
                onPlay = {},
                languageCode = "de",
            )
        }
        rule.onNodeWithText("DE").assertIsDisplayed()
    }
}
