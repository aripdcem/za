package com.za.games.platform

import androidx.compose.ui.graphics.Color
import com.za.games.R
import com.za.games.ui.g2048.G2048Screen
import com.za.games.ui.hub.Art2048
import com.za.games.ui.hub.SnakeArt
import com.za.games.ui.hub.TetrominoArt
import com.za.games.ui.snake.SnakeScreen
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
    )
}
