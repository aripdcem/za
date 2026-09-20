package com.aripd.zagames.ui.hub

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aripd.zagames.R
import com.aripd.zagames.platform.Changelog
import com.aripd.zagames.platform.GameCategory
import com.aripd.zagames.platform.GameEntry
import com.aripd.zagames.platform.GameRegistry
import com.aripd.zagames.setZaContent
import com.aripd.zagames.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HubScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val games = GameRegistry.games

    @Test
    fun registryHoldsTheKnownGames() {
        val expected = listOf(
            "blok", "2048", "snake", "sudoku", "mines", "kiskac", "besharf", "turetme",
            "dizgi", "kuyu", "gecit", "tavla", "balkon", "kakuro", "vergici", "toplam", "viraj", "filo", "reyon", "raket", "tuse", "ucurtma", "dalgic", "bostan", "sincap", "cekirge", "cici",
        )
        assertEquals(expected, games.map { it.id })
    }

    @Test
    fun listsEveryGame() {
        rule.setZaContent { HubScreen(games = games, highScores = emptyMap(), onPlay = {}) }
        for (game in games) {
            val title = str(game.titleRes)
            rule.onNodeWithTag(HUB_LIST_TAG).performScrollToNode(hasText(title))
            rule.onNodeWithText(title).assertIsDisplayed()
        }
    }

    @Test
    fun categoryChipReportsSelection() {
        var picked: GameCategory? = null
        rule.setZaContent {
            HubScreen(games = games, highScores = emptyMap(), onPlay = {}, onCategory = { picked = it })
        }
        rule.onNodeWithText(str(R.string.hub_cat_word)).performClick()
        assertEquals(GameCategory.WORD, picked)
    }

    @Test
    fun categoryFilterShowsOnlyThatGroup() {
        rule.setZaContent {
            HubScreen(games = games, highScores = emptyMap(), onPlay = {}, category = GameCategory.WORD)
        }
        val list = rule.onNodeWithTag(HUB_LIST_TAG)
        list.performScrollToNode(hasText(str(R.string.game_dizgi)))
        rule.onNodeWithText(str(R.string.game_dizgi)).assertIsDisplayed()
        val blok = runCatching { list.performScrollToNode(hasText(str(R.string.game_blok))) }
        assertTrue("Blok kelime grubunda listelenmemeli", blok.isFailure)
    }

    @Test
    fun playButtonReportsFirstGame() {
        var played: GameEntry? = null
        rule.setZaContent { HubScreen(games = games, highScores = emptyMap(), onPlay = { played = it }) }
        rule.onAllNodesWithText(str(R.string.play))[0].performClick()
        assertEquals(games.first().id, played?.id)
    }

    @Test
    fun whatsNewCardShowsNotesAndDismisses() {
        var dismissed = false
        val note = Changelog.latest
        rule.setZaContent {
            HubScreen(
                games = games,
                highScores = emptyMap(),
                onPlay = {},
                whatsNew = note,
                onDismissWhatsNew = { dismissed = true },
            )
        }
        rule.onNodeWithText(str(R.string.whats_new_title)).assertIsDisplayed()
        rule.onNodeWithText("• " + note.notes().first()).assertIsDisplayed()
        rule.onNodeWithText(str(R.string.whats_new_dismiss)).performClick()
        assertTrue(dismissed)
    }

    @Test
    fun newBadgeMarksGamesAddedAfterSeenVersion() {
        rule.setZaContent {
            HubScreen(games = games, highScores = emptyMap(), onPlay = {}, newSinceCode = Changelog.versionCode("0.18.1"))
        }
        rule.onNodeWithTag(HUB_LIST_TAG).performScrollToNode(hasText(str(R.string.game_vergici)))
        assertTrue(rule.onAllNodesWithText(str(R.string.badge_new)).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun noBadgeWithoutAnUpdate() {
        rule.setZaContent { HubScreen(games = games, highScores = emptyMap(), onPlay = {}) }
        rule.onNodeWithTag(HUB_LIST_TAG).performScrollToNode(hasText(str(R.string.game_vergici)))
        assertTrue(rule.onAllNodesWithText(str(R.string.badge_new)).fetchSemanticsNodes().isEmpty())
    }
}
