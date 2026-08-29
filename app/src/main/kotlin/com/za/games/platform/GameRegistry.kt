package com.za.games.platform

import androidx.compose.ui.graphics.Color
import com.za.games.R
import com.za.games.ui.hub.TetrominoArt
import com.za.games.ui.tetris.TetrisScreen

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
    )
}
