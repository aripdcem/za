package com.za.games.mines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinesStateTest {

    /** 5x5, sol üst köşede tek mayınlı hazır tahta. */
    private fun tinyBoard(): MinesState = MinesState.newGame(MinesDifficulty.EASY, seed = 1L).copy(
        width = 5, height = 5, mineCount = 1,
        mines = setOf(0),
        status = MinesStatus.RUNNING,
    )

    @Test
    fun `first reveal is always safe and places all mines`() {
        val start = MinesState.newGame(MinesDifficulty.EASY, seed = 3L)
        assertEquals(MinesStatus.READY, start.status)
        assertTrue(start.mines.isEmpty())

        val center = (start.height / 2) * start.width + start.width / 2
        val opened = start.reveal(center)
        assertEquals(MinesDifficulty.EASY.mineCount, opened.mines.size)
        assertTrue(center in opened.revealed)
        assertTrue(opened.status == MinesStatus.RUNNING || opened.status == MinesStatus.WON)
        val safeZone = (opened.neighbors(center) + center).toSet()
        assertTrue(opened.mines.none { it in safeZone })
    }

    @Test
    fun `same seed and first tap produce the same minefield`() {
        val a = MinesState.newGame(MinesDifficulty.MEDIUM, seed = 5L).reveal(30)
        val b = MinesState.newGame(MinesDifficulty.MEDIUM, seed = 5L).reveal(30)
        assertEquals(a, b)
    }

    @Test
    fun `adjacent mine counts are correct`() {
        val board = tinyBoard()
        assertEquals(1, board.adjacentMines(1))
        assertEquals(1, board.adjacentMines(5))
        assertEquals(1, board.adjacentMines(6))
        assertEquals(0, board.adjacentMines(2))
        assertEquals(0, board.adjacentMines(24))
    }

    @Test
    fun `flood fill opens the whole safe region and wins`() {
        val board = tinyBoard()
        val opened = board.reveal(24)
        assertEquals(24, opened.revealed.size) // mayın dışındaki her hücre
        assertTrue(0 !in opened.revealed)
        assertEquals(MinesStatus.WON, opened.status)
    }

    @Test
    fun `revealing a mine loses the game`() {
        val board = tinyBoard()
        val lost = board.reveal(0)
        assertEquals(MinesStatus.LOST, lost.status)
        assertEquals(0, lost.exploded)
        assertEquals(lost, lost.reveal(24)) // oyun bitince hamle işlemez
    }

    @Test
    fun `flags block reveal and toggle off`() {
        val board = tinyBoard().toggleFlag(0)
        assertTrue(0 in board.flagged)
        assertEquals(board, board.reveal(0)) // bayraklı hücre açılmaz
        assertTrue(0 !in board.toggleFlag(0).flagged)
        // Açık hücreye bayrak konmaz.
        val opened = tinyBoard().reveal(24)
        assertEquals(opened, opened.toggleFlag(24))
    }

    @Test
    fun `chord opens neighbors when flags match the number`() {
        val board = MinesState.newGame(MinesDifficulty.EASY, seed = 7L).copy(
            width = 3, height = 3, mineCount = 1,
            mines = setOf(0),
            revealed = setOf(4),
            flagged = setOf(0),
            status = MinesStatus.RUNNING,
        )
        val chorded = board.chord(4)
        assertEquals(MinesStatus.WON, chorded.status)
        assertEquals(8, chorded.revealed.size)
    }

    @Test
    fun `chord with a wrong flag can lose the game`() {
        val board = MinesState.newGame(MinesDifficulty.EASY, seed = 7L).copy(
            width = 3, height = 3, mineCount = 1,
            mines = setOf(0),
            revealed = setOf(4),
            flagged = setOf(1), // yanlış bayrak
            status = MinesStatus.RUNNING,
        )
        val chorded = board.chord(4)
        assertEquals(MinesStatus.LOST, chorded.status)
        assertEquals(0, chorded.exploded)
    }

    @Test
    fun `chord does nothing when flags do not match`() {
        val board = tinyBoard().reveal(24)
        // Kazanılmadan önceki durum üzerinde denemek için bayraksız chord:
        val running = tinyBoard().copy(revealed = setOf(6))
        assertEquals(running, running.chord(6)) // 1 sayısı, 0 bayrak
    }
}
