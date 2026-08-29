package com.za.games.g2048

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class G2048StateTest {

    /** Verilen hücrelerle çalışan durum kurar (4x4, skor 0). */
    private fun boardOf(vararg cells: Int, seed: Long = 1L): G2048State {
        require(cells.size == 16)
        return G2048State.newGame(seed).copy(
            cells = cells.toList(),
            score = 0L,
            status = G2048Status.RUNNING,
            reached2048 = false,
        )
    }

    @Test
    fun `new game starts with exactly two tiles of value 2 or 4`() {
        val state = G2048State.newGame(seed = 7L)
        val tiles = state.cells.filter { it != 0 }
        assertEquals(2, tiles.size)
        assertTrue(tiles.all { it == 2 || it == 4 })
        assertEquals(G2048Status.RUNNING, state.status)
    }

    @Test
    fun `same seed produces identical games`() {
        assertEquals(G2048State.newGame(42L), G2048State.newGame(42L))
        assertEquals(
            G2048State.newGame(42L).move(MoveDir.LEFT).move(MoveDir.UP),
            G2048State.newGame(42L).move(MoveDir.LEFT).move(MoveDir.UP),
        )
    }

    @Test
    fun `left move compacts and merges pairs`() {
        val state = boardOf(
            2, 2, 4, 4,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val moved = state.move(MoveDir.LEFT)
        assertEquals(4, moved.cells[0])
        assertEquals(8, moved.cells[1])
        assertEquals(12L, moved.score)
        assertEquals(listOf(0, 1), moved.lastMerged)
    }

    @Test
    fun `each tile merges only once per move`() {
        val state = boardOf(
            2, 2, 2, 2,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val moved = state.move(MoveDir.LEFT)
        assertEquals(4, moved.cells[0])
        assertEquals(4, moved.cells[1])
        assertEquals(8L, moved.score)
    }

    @Test
    fun `merged result does not chain into a second merge`() {
        val state = boardOf(
            4, 2, 2, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val moved = state.move(MoveDir.LEFT)
        assertEquals(4, moved.cells[0])
        assertEquals(4, moved.cells[1])
        assertEquals(4L, moved.score)
    }

    @Test
    fun `vertical moves merge along columns`() {
        val up = boardOf(
            2, 0, 0, 0,
            0, 0, 0, 0,
            2, 0, 0, 0,
            0, 0, 0, 0,
        ).move(MoveDir.UP)
        assertEquals(4, up.cells[0])

        val down = boardOf(
            2, 0, 0, 0,
            0, 0, 0, 0,
            2, 0, 0, 0,
            0, 0, 0, 0,
        ).move(MoveDir.DOWN)
        assertEquals(4, down.cells[12])
    }

    @Test
    fun `a move that changes nothing spawns nothing`() {
        val state = boardOf(
            2, 4, 8, 16,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        assertEquals(state, state.move(MoveDir.LEFT))
    }

    @Test
    fun `every effective move spawns exactly one new tile`() {
        val state = boardOf(
            2, 2, 4, 4,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val moved = state.move(MoveDir.LEFT)
        // 4 taş, 2 birleşme -> 2 taş kaldı + 1 doğan = 3
        assertEquals(3, moved.cells.count { it != 0 })
        assertTrue(moved.lastSpawn >= 0)
        assertTrue(moved.cells[moved.lastSpawn] == 2 || moved.cells[moved.lastSpawn] == 4)
    }

    @Test
    fun `game ends when the board is full with no merges left`() {
        val state = boardOf(
            2, 4, 2, 4,
            4, 2, 4, 2,
            8, 16, 32, 16,
            64, 8, 64, 0,
        )
        val moved = state.move(MoveDir.RIGHT)
        assertEquals(G2048Status.OVER, moved.status)
        assertEquals(16, moved.cells.count { it != 0 })
        assertEquals(moved, moved.move(MoveDir.LEFT)) // oyun bitince hamle işlemez
    }

    @Test
    fun `reaching 2048 sets the flag but the game continues`() {
        val state = boardOf(
            1024, 1024, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val moved = state.move(MoveDir.LEFT)
        assertEquals(2048, moved.cells[0])
        assertTrue(moved.reached2048)
        assertEquals(G2048Status.RUNNING, moved.status)
        assertFalse(state.reached2048)
    }
}
