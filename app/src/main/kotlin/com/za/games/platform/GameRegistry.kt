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
import com.za.games.ui.hub.KakuroArt
import com.za.games.ui.hub.VergiciArt
import com.za.games.ui.hub.ToplamArt
import com.za.games.ui.hub.FiloArt
import com.za.games.ui.hub.ReyonArt
import com.za.games.ui.hub.SincapArt
import com.za.games.ui.hub.RaketArt
import com.za.games.ui.hub.TuseArt
import com.za.games.ui.hub.UcurtmaArt
import com.za.games.ui.hub.BostanArt
import com.za.games.ui.hub.CekirgeArt
import com.za.games.ui.hub.CiciArt
import com.za.games.ui.hub.DalgicArt
import com.za.games.ui.hub.VirajArt
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
import com.za.games.ui.kakuro.KakuroScreen
import com.za.games.ui.vergici.VergiciScreen
import com.za.games.ui.toplam.ToplamScreen
import com.za.games.ui.filo.FiloScreen
import com.za.games.ui.sincap.SincapScreen
import com.za.games.ui.reyon.ReyonScreen
import com.za.games.ui.raket.RaketScreen
import com.za.games.ui.tuse.TuseScreen
import com.za.games.ui.ucurtma.UcurtmaScreen
import com.za.games.ui.bostan.BostanScreen
import com.za.games.ui.cekirge.CekirgeScreen
import com.za.games.ui.cici.CiciScreen
import com.za.games.ui.dalgic.DalgicScreen
import com.za.games.ui.viraj.VirajScreen
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
            since = "0.1.0",
            titleRes = R.string.game_tetris,
            taglineRes = R.string.game_tetris_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFF22D3EE),
            art = { modifier -> TetrominoArt(modifier) },
            screen = { highScore, onScore, onExit ->
                TetrisScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "2048",
            since = "0.2.0",
            titleRes = R.string.game_2048,
            taglineRes = R.string.game_2048_tagline,
            category = GameCategory.PUZZLE,
            accent = Color(0xFFFACC15),
            art = { modifier -> Art2048(modifier) },
            screen = { highScore, onScore, onExit ->
                G2048Screen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "snake",
            since = "0.2.0",
            titleRes = R.string.game_snake,
            taglineRes = R.string.game_snake_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFF4ADE80),
            art = { modifier -> SnakeArt(modifier) },
            screen = { highScore, onScore, onExit ->
                SnakeScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "sudoku",
            since = "0.3.0",
            titleRes = R.string.game_sudoku,
            taglineRes = R.string.game_sudoku_tagline,
            category = GameCategory.PUZZLE,
            accent = Color(0xFF60A5FA),
            art = { modifier -> SudokuArt(modifier) },
            screen = { highScore, onScore, onExit ->
                SudokuScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "mines",
            since = "0.3.0",
            titleRes = R.string.game_mines,
            taglineRes = R.string.game_mines_tagline,
            category = GameCategory.PUZZLE,
            accent = Color(0xFFF87171),
            art = { modifier -> MinesArt(modifier) },
            screen = { highScore, onScore, onExit ->
                MinesScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "kiskac",
            since = "0.9.0",
            titleRes = R.string.game_kiskac,
            taglineRes = R.string.game_kiskac_tagline,
            category = GameCategory.WORD,
            accent = Color(0xFFF472B6),
            art = { modifier -> KiskacArt(modifier) },
            screen = { highScore, onScore, onExit ->
                KiskacScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "besharf",
            since = "0.4.0",
            titleRes = R.string.game_besharf,
            taglineRes = R.string.game_besharf_tagline,
            category = GameCategory.WORD,
            accent = Color(0xFFFACC15),
            art = { modifier -> BesHarfArt(modifier) },
            screen = { highScore, onScore, onExit ->
                BesHarfScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "turetme",
            since = "0.10.0",
            titleRes = R.string.game_turetme,
            taglineRes = R.string.game_turetme_tagline,
            category = GameCategory.WORD,
            accent = Color(0xFFA78BFA),
            art = { modifier -> TuretmeArt(modifier) },
            screen = { highScore, onScore, onExit ->
                TuretmeScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "dizgi",
            since = "0.12.0",
            titleRes = R.string.game_dizgi,
            taglineRes = R.string.game_dizgi_tagline,
            category = GameCategory.WORD,
            accent = Color(0xFFFB923C),
            art = { modifier -> DizgiArt(modifier) },
            screen = { highScore, onScore, onExit ->
                DizgiScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "kuyu",
            since = "0.13.0",
            titleRes = R.string.game_kuyu,
            taglineRes = R.string.game_kuyu_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFFF1F5F9),
            art = { modifier -> KuyuArt(modifier) },
            screen = { highScore, onScore, onExit ->
                KuyuScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "gecit",
            since = "0.14.0",
            titleRes = R.string.game_gecit,
            taglineRes = R.string.game_gecit_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFFA3E635),
            art = { modifier -> GecitArt(modifier) },
            screen = { highScore, onScore, onExit ->
                GecitScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "tavla",
            since = "0.16.0",
            titleRes = R.string.game_tavla,
            taglineRes = R.string.game_tavla_tagline,
            category = GameCategory.BOARD,
            accent = Color(0xFFD97706),
            art = { modifier -> TavlaArt(modifier) },
            screen = { highScore, onScore, onExit ->
                TavlaScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "balkon",
            since = "0.17.0",
            titleRes = R.string.game_balkon,
            taglineRes = R.string.game_balkon_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFF38BDF8),
            art = { modifier -> BalkonArt(modifier) },
            screen = { highScore, onScore, onExit ->
                BalkonScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "kakuro",
            since = "0.18.0",
            titleRes = R.string.game_kakuro,
            taglineRes = R.string.game_kakuro_tagline,
            category = GameCategory.PUZZLE,
            accent = Color(0xFF14B8A6),
            art = { modifier -> KakuroArt(modifier) },
            screen = { highScore, onScore, onExit ->
                KakuroScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "vergici",
            since = "0.19.0",
            titleRes = R.string.game_vergici,
            taglineRes = R.string.game_vergici_tagline,
            category = GameCategory.BOARD,
            accent = Color(0xFFA3E635),
            art = { modifier -> VergiciArt(modifier) },
            screen = { highScore, onScore, onExit ->
                VergiciScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "toplam",
            since = "0.19.0",
            titleRes = R.string.game_toplam,
            taglineRes = R.string.game_toplam_tagline,
            category = GameCategory.BOARD,
            accent = Color(0xFFC084FC),
            art = { modifier -> ToplamArt(modifier) },
            screen = { highScore, onScore, onExit ->
                ToplamScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "viraj",
            since = "0.23.0",
            titleRes = R.string.game_viraj,
            taglineRes = R.string.game_viraj_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFF4DE1FF),
            art = { modifier -> VirajArt(modifier) },
            screen = { highScore, onScore, onExit ->
                VirajScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "filo",
            since = "0.24.0",
            titleRes = R.string.game_filo,
            taglineRes = R.string.game_filo_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFFFB7185),
            art = { modifier -> FiloArt(modifier) },
            screen = { highScore, onScore, onExit ->
                FiloScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "reyon",
            since = "0.25.0",
            titleRes = R.string.game_reyon,
            taglineRes = R.string.game_reyon_tagline,
            category = GameCategory.PUZZLE,
            accent = Color(0xFF60A5FA),
            art = { modifier -> ReyonArt(modifier) },
            screen = { highScore, onScore, onExit ->
                ReyonScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "raket",
            since = "0.29.0",
            titleRes = R.string.game_raket,
            taglineRes = R.string.game_raket_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFF5EEAD4),
            art = { modifier -> RaketArt(modifier) },
            screen = { highScore, onScore, onExit ->
                RaketScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "tuse",
            since = "0.30.0",
            titleRes = R.string.game_tuse,
            taglineRes = R.string.game_tuse_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFFE879F9),
            art = { modifier -> TuseArt(modifier) },
            screen = { highScore, onScore, onExit ->
                TuseScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "ucurtma",
            since = "0.31.0",
            titleRes = R.string.game_ucurtma,
            taglineRes = R.string.game_ucurtma_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFF60B8F5),
            art = { modifier -> UcurtmaArt(modifier) },
            screen = { highScore, onScore, onExit ->
                UcurtmaScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "dalgic",
            since = "0.32.0",
            titleRes = R.string.game_dalgic,
            taglineRes = R.string.game_dalgic_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFFFACC15),
            art = { modifier -> DalgicArt(modifier) },
            screen = { highScore, onScore, onExit ->
                DalgicScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "bostan",
            since = "0.33.0",
            titleRes = R.string.game_bostan,
            taglineRes = R.string.game_bostan_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFF84CC16),
            art = { modifier -> BostanArt(modifier) },
            screen = { highScore, onScore, onExit ->
                BostanScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "sincap",
            since = "0.34.0",
            titleRes = R.string.game_sincap,
            taglineRes = R.string.game_sincap_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFFD97706),
            art = { modifier -> SincapArt(modifier) },
            screen = { highScore, onScore, onExit ->
                SincapScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "cekirge",
            since = "0.35.0",
            titleRes = R.string.game_cekirge,
            taglineRes = R.string.game_cekirge_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFF65A30D),
            art = { modifier -> CekirgeArt(modifier) },
            screen = { highScore, onScore, onExit ->
                CekirgeScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
        GameEntry(
            id = "cici",
            since = "0.37.0",
            titleRes = R.string.game_cici,
            taglineRes = R.string.game_cici_tagline,
            category = GameCategory.ARCADE,
            accent = Color(0xFF818CF8),
            art = { modifier -> CiciArt(modifier) },
            screen = { highScore, onScore, onExit ->
                CiciScreen(highScore = highScore, onScore = onScore, onExit = onExit)
            },
        ),
    )
}
