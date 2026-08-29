package com.za.games.snake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakeStateTest {

    private val farFood = Cell(19, 0)

    @Test
    fun `new game starts with a three segment snake and food off the body`() {
        val state = SnakeState.newGame(seed = 1L)
        assertEquals(3, state.body.size)
        assertEquals(Cell(10, 7), state.body.first())
        assertEquals(SnakeDir.RIGHT, state.dir)
        assertFalse(state.food in state.body)
        assertEquals(SnakeStatus.RUNNING, state.status)
    }

    @Test
    fun `same seed produces identical games`() {
        assertEquals(SnakeState.newGame(42L), SnakeState.newGame(42L))
        assertEquals(
            SnakeState.newGame(42L).turn(SnakeDir.UP).tick().tick(),
            SnakeState.newGame(42L).turn(SnakeDir.UP).tick().tick(),
        )
    }

    @Test
    fun `tick moves the head one cell and drags the tail`() {
        val state = SnakeState.newGame(1L).copy(food = farFood)
        val ticked = state.tick()
        assertEquals(Cell(10, 8), ticked.body.first())
        assertEquals(3, ticked.body.size)
        assertFalse(state.body.last() in ticked.body)
    }

    @Test
    fun `turn changes direction but reversing is ignored`() {
        val state = SnakeState.newGame(2L).copy(food = farFood)
        val up = state.turn(SnakeDir.UP).tick()
        assertEquals(Cell(9, 7), up.body.first())
        assertEquals(state, state.turn(SnakeDir.LEFT)) // RIGHT giderken LEFT yok sayılır
    }

    @Test
    fun `eating grows the snake scores ten and respawns food`() {
        val state = SnakeState.newGame(3L).copy(food = Cell(10, 8))
        val fed = state.tick()
        assertEquals(4, fed.body.size)
        assertEquals(10L, fed.score)
        assertEquals(1, fed.foods)
        assertEquals(SnakeStatus.RUNNING, fed.status)
        assertFalse(fed.food in fed.body)
    }

    @Test
    fun `hitting the wall ends the game`() {
        val state = SnakeState.newGame(4L).copy(
            body = listOf(Cell(0, 14), Cell(0, 13), Cell(0, 12)),
            dir = SnakeDir.RIGHT,
            pending = SnakeDir.RIGHT,
            food = farFood,
        )
        assertEquals(SnakeStatus.OVER, state.tick().status)
    }

    @Test
    fun `hitting the body ends the game`() {
        val state = SnakeState.newGame(5L).copy(
            body = listOf(
                Cell(5, 5), Cell(5, 6), Cell(6, 6), Cell(6, 5),
                Cell(6, 4), Cell(5, 4), Cell(4, 4),
            ),
            dir = SnakeDir.LEFT,
            pending = SnakeDir.LEFT,
            food = farFood,
        )
        assertEquals(SnakeStatus.OVER, state.tick().status)
    }

    @Test
    fun `moving into the cell the tail is vacating is safe`() {
        val state = SnakeState.newGame(6L).copy(
            body = listOf(
                Cell(5, 5), Cell(5, 6), Cell(6, 6), Cell(6, 5),
                Cell(6, 4), Cell(5, 4),
            ),
            dir = SnakeDir.LEFT,
            pending = SnakeDir.LEFT,
            food = farFood,
        )
        val ticked = state.tick()
        assertEquals(SnakeStatus.RUNNING, ticked.status)
        assertEquals(Cell(5, 4), ticked.body.first())
        assertEquals(6, ticked.body.size)
    }

    @Test
    fun `speed rises with each food and has a floor`() {
        assertEquals(160L, snakeSpeedMillis(0))
        assertTrue(snakeSpeedMillis(10) < snakeSpeedMillis(1))
        assertEquals(70L, snakeSpeedMillis(40))
        assertEquals(70L, snakeSpeedMillis(100))
    }

    @Test
    fun `paused and over states ignore ticks`() {
        val paused = SnakeState.newGame(7L).pause()
        assertEquals(paused, paused.tick())
        assertEquals(paused, paused.turn(SnakeDir.UP))
        assertEquals(SnakeStatus.RUNNING, paused.togglePause().status)

        val over = SnakeState.newGame(8L).copy(status = SnakeStatus.OVER)
        assertEquals(over, over.tick())
        assertEquals(over, over.togglePause())
    }
}
