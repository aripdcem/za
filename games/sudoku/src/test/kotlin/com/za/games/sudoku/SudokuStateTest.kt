package com.za.games.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuStateTest {

    private fun assertValidSolvedGrid(grid: List<Int>) {
        val full = (1..9).toSet()
        for (i in 0 until 9) {
            val row = (0 until 9).map { grid[i * 9 + it] }.toSet()
            val col = (0 until 9).map { grid[it * 9 + i] }.toSet()
            assertEquals(full, row)
            assertEquals(full, col)
        }
        for (br in 0 until 3) {
            for (bc in 0 until 3) {
                val box = buildSet {
                    for (r in 0 until 3) for (c in 0 until 3) {
                        add(grid[(br * 3 + r) * 9 + (bc * 3 + c)])
                    }
                }
                assertEquals(full, box)
            }
        }
    }

    @Test
    fun `generator produces a valid solved grid`() {
        assertValidSolvedGrid(SudokuState.generateSolved(Random(7L)).toList())
    }

    @Test
    fun `puzzles have exactly one solution and enough clues`() {
        for (difficulty in SudokuDifficulty.entries) {
            val state = SudokuState.newGame(difficulty, seed = 11L)
            val clues = state.given.count { it }
            assertTrue("ipuçları hedefin altında olamaz", clues >= difficulty.targetClues)
            assertTrue(clues < 81)
            assertEquals(1, SudokuState.countSolutions(state.values.toIntArray(), limit = 2))
            assertValidSolvedGrid(state.solution)
        }
    }

    @Test
    fun `same seed produces identical puzzles`() {
        assertEquals(
            SudokuState.newGame(SudokuDifficulty.MEDIUM, 42L),
            SudokuState.newGame(SudokuDifficulty.MEDIUM, 42L),
        )
        assertNotEquals(
            SudokuState.newGame(SudokuDifficulty.MEDIUM, 42L).values,
            SudokuState.newGame(SudokuDifficulty.MEDIUM, 43L).values,
        )
    }

    @Test
    fun `filling every empty cell with the solution solves the puzzle`() {
        var state = SudokuState.newGame(SudokuDifficulty.EASY, seed = 5L)
        for (index in 0 until 81) {
            if (!state.given[index]) state = state.setValue(index, state.solution[index])
        }
        assertEquals(SudokuStatus.SOLVED, state.status)
        assertEquals(state.solution, state.values)
        // Çözüldükten sonra hamleler işlemez.
        val idx = state.given.indexOfFirst { !it }
        assertEquals(state, state.setValue(idx, 1))
    }

    @Test
    fun `given cells cannot be changed`() {
        val state = SudokuState.newGame(SudokuDifficulty.EASY, seed = 9L)
        val givenIndex = state.given.indexOfFirst { it }
        assertEquals(state, state.setValue(givenIndex, 5))
        assertEquals(state, state.clearCell(givenIndex))
        assertEquals(state, state.toggleNote(givenIndex, 5))
    }

    @Test
    fun `setting the same value again clears the cell`() {
        val state = SudokuState.newGame(SudokuDifficulty.EASY, seed = 9L)
        val index = state.given.indexOfFirst { !it }
        val value = state.solution[index]
        val filled = state.setValue(index, value)
        assertEquals(value, filled.values[index])
        val cleared = filled.setValue(index, value)
        assertEquals(0, cleared.values[index])
    }

    @Test
    fun `conflicts are reported for duplicates in a row`() {
        val state = SudokuState.newGame(SudokuDifficulty.EASY, seed = 13L)
        // Boş bir hücreye, satırındaki mevcut bir değeri kopyala.
        val index = (0 until 81).first { !state.given[it] }
        val row = index / 9
        val existing = (0 until 9).map { row * 9 + it }
            .first { it != index && state.values[it] != 0 }
        val wrong = state.setValue(index, state.values[existing])
        assertTrue(index in wrong.conflicts)
        assertTrue(existing in wrong.conflicts)
        assertTrue(state.conflicts.isEmpty())
    }

    @Test
    fun `notes toggle and are cleared from peers when a value is placed`() {
        val state = SudokuState.newGame(SudokuDifficulty.EASY, seed = 17L)
        val index = (0 until 81).first { !state.given[it] }
        val peer = SudokuState.peers(index).first { !state.given[it] && state.values[it] == 0 }
        val value = state.solution[index]

        val noted = state.toggleNote(peer, value).toggleNote(peer, 9).toggleNote(peer, 9)
        assertEquals(setOf(value), noted.notes[peer])

        val placed = noted.setValue(index, value)
        assertTrue(value !in placed.notes[peer]) // komşu notu otomatik silindi

        // Dolu hücreye not yazılamaz.
        assertEquals(placed, placed.toggleNote(index, 3))
    }
}
