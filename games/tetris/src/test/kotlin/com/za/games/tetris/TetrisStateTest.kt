package com.za.games.tetris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TetrisStateTest {

    private fun filledRowExcept(vararg gaps: Int): List<Tetromino?> =
        List(TetrisState.WIDTH) { col -> if (col in gaps) null else Tetromino.Z }

    // --- Doğuş ve torba (7-bag) ---

    @Test
    fun `new game deals every tetromino exactly once in the first bag`() {
        val state = TetrisState.newGame(seed = 1L)
        val firstBag = (listOf(state.active.type) + state.next.take(6)).toSet()
        assertEquals(Tetromino.entries.toSet(), firstBag)
        assertEquals(13, state.next.size) // iki torba - aktif taş
    }

    @Test
    fun `same seed produces identical games`() {
        assertEquals(TetrisState.newGame(42L), TetrisState.newGame(42L))
        val a = TetrisState.newGame(42L).hardDrop().moveLeft().rotate()
        val b = TetrisState.newGame(42L).hardDrop().moveLeft().rotate()
        assertEquals(a, b)
    }

    @Test
    fun `pieces spawn centered with bottom row visible`() {
        val state = TetrisState.newGame(seed = 3L)
        assertEquals(-1, state.active.row)
        val cols = state.active.cells.map { it.col }
        assertTrue(cols.all { it in 3..6 })
    }

    // --- Hareket ---

    @Test
    fun `piece stops at the walls`() {
        var state = TetrisState.newGame(seed = 5L)
        repeat(15) { state = state.moveLeft() }
        assertEquals(0, state.active.cells.minOf { it.col })
        assertEquals(state, state.moveLeft())

        repeat(15) { state = state.moveRight() }
        assertEquals(TetrisState.WIDTH - 1, state.active.cells.maxOf { it.col })
        assertEquals(state, state.moveRight())
    }

    @Test
    fun `tick moves the active piece down one row`() {
        val state = TetrisState.newGame(seed = 9L)
        val ticked = state.tick()
        assertEquals(state.active.row + 1, ticked.active.row)
        assertEquals(state.score, ticked.score)
    }

    @Test
    fun `soft drop scores one point per cell`() {
        val state = TetrisState.newGame(seed = 9L)
        val dropped = state.softDrop()
        assertEquals(state.active.row + 1, dropped.active.row)
        assertEquals(state.score + 1, dropped.score)
    }

    // --- Rotasyon ve SRS ---

    @Test
    fun `kick tables convert guideline offsets to board coordinates`() {
        assertEquals(
            listOf(0 to 0, 0 to -1, -1 to -1, 2 to 0, 2 to -1),
            Srs.kicks(Tetromino.T, from = 0, to = 1),
        )
        assertEquals(
            listOf(0 to 0, 0 to 2, 0 to -1, -1 to 2, 2 to -1),
            Srs.kicks(Tetromino.I, from = 1, to = 0),
        )
        assertEquals(listOf(0 to 0), Srs.kicks(Tetromino.O, from = 0, to = 1))
    }

    @Test
    fun `T rotations follow SRS states`() {
        val expectedR = setOf(Cell(0, 1), Cell(1, 1), Cell(1, 2), Cell(2, 1))
        assertEquals(expectedR, Tetromino.T.rotations[1].toSet())
        val expected180 = setOf(Cell(1, 0), Cell(1, 1), Cell(1, 2), Cell(2, 1))
        assertEquals(expected180, Tetromino.T.rotations[2].toSet())
    }

    @Test
    fun `wall kick rescues a rotation against the left wall`() {
        val base = TetrisState.newGame(seed = 3L)
        // Sol duvara yaslanmış dikey T: kutu col=-1'de ama tüm hücreler tahtada.
        val state = base.copy(active = ActivePiece(Tetromino.T, rotation = 1, row = 5, col = -1))
        val rotated = state.rotate(clockwise = true)
        assertEquals(2, rotated.active.rotation)
        assertEquals(0, rotated.active.col) // (0,+1) tekmesi uygulandı
        assertTrue(rotated.active.cells.all { it.col >= 0 })
    }

    // --- Kilitlenme ve satır temizleme ---

    @Test
    fun `hard drop locks the piece and spawns the next one`() {
        val state = TetrisState.newGame(seed = 21L)
        val expectedNext = state.next.first()
        val dropped = state.hardDrop()
        assertEquals(4, dropped.board.sumOf { row -> row.count { it != null } })
        assertEquals(expectedNext, dropped.active.type)
        assertEquals(-1, dropped.active.row)
        assertEquals(1, dropped.locks)
        assertEquals(0, dropped.clearEvents) // temizlik yoksa olay artmaz
    }

    @Test
    fun `hard drop lands exactly on the ghost position`() {
        val state = TetrisState.newGame(seed = 21L)
        val ghost = state.ghost
        assertTrue(ghost.row >= state.active.row)
        val dropped = state.hardDrop()
        ghost.cells.filter { it.row >= 0 }.forEach { (r, c) ->
            assertEquals(state.active.type, dropped.board[r][c])
        }
    }

    @Test
    fun `soft drop on the floor locks the piece`() {
        val base = TetrisState.newGame(seed = 11L)
        val state = base.copy(active = ActivePiece(Tetromino.O, rotation = 0, row = 18, col = 4))
        val locked = state.softDrop()
        assertEquals(Tetromino.O, locked.board[19][4])
        assertEquals(Tetromino.O, locked.board[18][5])
        assertEquals(-1, locked.active.row)
    }

    @Test
    fun `single line clear scores 100 times level plus drop bonus`() {
        val base = TetrisState.newGame(seed = 13L)
        val board = base.board.toMutableList()
        board[19] = filledRowExcept(4, 5)
        val state = base.copy(board = board, active = ActivePiece(Tetromino.O, 0, -1, 4))

        val result = state.hardDrop()

        assertEquals(2L * 19 + 100L, result.score)
        assertEquals(1, result.lines)
        assertEquals(1, result.lastClear)
        assertEquals(listOf(19), result.lastClearedRows)
        assertEquals(1, result.clearEvents)
        assertEquals(1, result.locks)
        // Üstteki O kalıntısı bir satır aşağı kaydı.
        assertEquals(Tetromino.O, result.board[19][4])
        assertEquals(Tetromino.O, result.board[19][5])
        assertNull(result.board[19][0])
    }

    @Test
    fun `four line clear scores 800`() {
        val base = TetrisState.newGame(seed = 13L)
        val board = base.board.toMutableList()
        for (r in 16..19) board[r] = filledRowExcept(9)
        val state = base.copy(board = board, active = ActivePiece(Tetromino.I, rotation = 1, row = 0, col = 7))

        val result = state.hardDrop()

        assertEquals(2L * 16 + 800L, result.score)
        assertEquals(4, result.lines)
        assertEquals(4, result.lastClear)
        assertEquals(listOf(16, 17, 18, 19), result.lastClearedRows)
        assertEquals(1, result.clearEvents)
        assertTrue(result.board.all { row -> row.all { it == null } })
    }

    @Test
    fun `next queue never runs dry`() {
        var state = TetrisState.newGame(seed = 31L)
        repeat(10) {
            if (state.status == TetrisStatus.RUNNING) state = state.hardDrop()
            assertTrue(state.next.size >= TetrisState.VISIBLE_NEXT)
        }
    }

    // --- Hold ---

    @Test
    fun `hold swaps once per piece`() {
        val state = TetrisState.newGame(seed = 7L)
        val first = state.active.type
        val second = state.next.first()

        val held = state.holdPiece()
        assertEquals(first, held.hold)
        assertEquals(second, held.active.type)
        assertTrue(held.holdUsed)
        assertEquals(held, held.holdPiece()) // aynı taş için ikinci hold yok

        val afterLock = held.hardDrop()
        assertTrue(!afterLock.holdUsed)
        val swappedBack = afterLock.holdPiece()
        assertEquals(first, swappedBack.active.type)
    }

    // --- Seviye ve yerçekimi ---

    @Test
    fun `level rises every ten lines and gravity speeds up`() {
        val base = TetrisState.newGame(seed = 17L)
        assertEquals(1, base.level)
        assertEquals(2, base.copy(lines = 10).level)
        assertEquals(4, base.copy(lines = 35).level)

        assertEquals(1000L, gravityMillis(1))
        assertTrue(gravityMillis(2) < gravityMillis(1))
        assertTrue(gravityMillis(10) < gravityMillis(5))
        assertTrue(gravityMillis(30) >= 50L)
    }

    // --- Duraklatma ve oyun sonu ---

    @Test
    fun `paused game ignores moves and resumes cleanly`() {
        val paused = TetrisState.newGame(seed = 23L).pause()
        assertEquals(TetrisStatus.PAUSED, paused.status)
        assertEquals(paused, paused.tick())
        assertEquals(paused, paused.moveLeft())
        assertEquals(paused, paused.hardDrop())
        assertEquals(TetrisStatus.RUNNING, paused.togglePause().status)
    }

    @Test
    fun `stacking to the top ends the game`() {
        var state = TetrisState.newGame(seed = 99L)
        var safety = 0
        while (state.status != TetrisStatus.OVER && safety < 500) {
            state = state.hardDrop()
            safety++
        }
        assertEquals(TetrisStatus.OVER, state.status)
        assertTrue(state.score > 0)
        assertEquals(state, state.hardDrop()) // oyun bitince hamle işlemez
        assertEquals(state, state.togglePause())
        assertNotEquals(TetrisStatus.OVER, TetrisState.newGame(seed = 1L).status)
    }
}
