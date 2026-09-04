package com.za.games.platform

import androidx.compose.ui.graphics.Color
import com.za.games.R
import com.za.games.ui.besharf.BesHarfScreen
import com.za.games.ui.dizgi.DizgiScreen
import com.za.games.ui.g2048.G2048Screen
import com.za.games.ui.hub.Art2048
import com.za.games.ui.hub.BesHarfArt
import com.za.games.ui.hub.DizgiArt
import com.za.games.ui.hub.KiskacArt
import com.za.games.ui.hub.GecitArt
import com.za.games.ui.hub.KuyuArt
import com.za.games.ui.hub.TavlaArt
import com.za.games.ui.hub.BalkonArt
import com.za.games.ui.hub.MinesArt
import com.za.games.ui.hub.SnakeArt
import com.za.games.ui.hub.SudokuArt
import com.za.games.ui.hub.TetrominoArt
import com.za.games.ui.hub.TuretmeArt
import com.za.games.ui.mines.MinesScreen
import com.za.games.ui.kiskac.KiskacScreen
import com.za.games.ui.gecit.GecitScreen
import com.za.games.ui.kuyu.KuyuScreen
import com.za.games.ui.tavla.TavlaScreen
import com.za.games.ui.balkon.BalkonScreen
import com.za.games.ui.snake.SnakeScreen
import com.za.games.ui.sudoku.SudokuScreen
import com.za.games.ui.tetris.TetrisScreen
import com.za.games.ui.turetme.TuretmeScreen

/**
 * Platformdaki oyunların tek listesi.
 * Yeni oyun = yeni bir [GameEntry] + bu listeye bir satır.
 */
object GameRegistry {

    val games: List<GameEntry> = listOf(
        GameEntry(
            id = "tetris",
            titleRes = R.string.game_tetris,
            taglineRes = R.string.game_tetris_tagline,
            accent = Color(0xFF22D3EE),
            art = { modifier -> TetrominoArt(modifier) },
            screen = { highScore, onScore, onExit ->
                TetrisScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "2048",
            titleRes = R.string.game_2048,
            taglineRes = R.string.game_2048_tagline,
            accent = Color(0xFFFACC15),
            art = { modifier -> Art2048(modifier) },
            screen = { highScore, onScore, onExit ->
                G2048Screen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "snake",
            titleRes = R.string.game_snake,
            taglineRes = R.string.game_snake_tagline,
            accent = Color(0xFF4ADE80),
            art = { modifier -> SnakeArt(modifier) },
            screen = { highScore, onScore, onExit ->
                SnakeScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "sudoku",
            titleRes = R.string.game_sudoku,
            taglineRes = R.string.game_sudoku_tagline,
            accent = Color(0xFF60A5FA),
            art = { modifier -> SudokuArt(modifier) },
            screen = { highScore, onScore, onExit ->
                SudokuScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "mines",
            titleRes = R.string.game_mines,
            taglineRes = R.string.game_mines_tagline,
            accent = Color(0xFFF87171),
            art = { modifier -> MinesArt(modifier) },
            screen = { highScore, onScore, onExit ->
                MinesScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "kiskac",
            titleRes = R.string.game_kiskac,
            taglineRes = R.string.game_kiskac_tagline,
            accent = Color(0xFFF472B6),
            art = { modifier -> KiskacArt(modifier) },
            screen = { highScore, onScore, onExit ->
                KiskacScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "besharf",
            titleRes = R.string.game_besharf,
            taglineRes = R.string.game_besharf_tagline,
            accent = Color(0xFFFACC15),
            art = { modifier -> BesHarfArt(modifier) },
            screen = { highScore, onScore, onExit ->
                BesHarfScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "turetme",
            titleRes = R.string.game_turetme,
            taglineRes = R.string.game_turetme_tagline,
            accent = Color(0xFFA78BFA),
            art = { modifier -> TuretmeArt(modifier) },
            screen = { highScore, onScore, onExit ->
                TuretmeScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "dizgi",
            titleRes = R.string.game_dizgi,
            taglineRes = R.string.game_dizgi_tagline,
            accent = Color(0xFFFB923C),
            art = { modifier -> DizgiArt(modifier) },
            screen = { highScore, onScore, onExit ->
                DizgiScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "kuyu",
            titleRes = R.string.game_kuyu,
            taglineRes = R.string.game_kuyu_tagline,
            accent = Color(0xFFF1F5F9),
            art = { modifier -> KuyuArt(modifier) },
            screen = { highScore, onScore, onExit ->
                KuyuScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "gecit",
            titleRes = R.string.game_gecit,
            taglineRes = R.string.game_gecit_tagline,
            accent = Color(0xFFA3E635),
            art = { modifier -> GecitArt(modifier) },
            screen = { highScore, onScore, onExit ->
                GecitScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "tavla",
            titleRes = R.string.game_tavla,
            taglineRes = R.string.game_tavla_tagline,
            accent = Color(0xFFD97706),
            art = { modifier -> TavlaArt(modifier) },
            screen = { highScore, onScore, onExit ->
                TavlaScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "balkon",
            titleRes = R.string.game_balkon,
            taglineRes = R.string.game_balkon_tagline,
            accent = Color(0xFF38BDF8),
            art = { modifier -> BalkonArt(modifier) },
            screen = { highScore, onScore, onExit ->
                BalkonScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
    )
}
