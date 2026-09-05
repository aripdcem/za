package com.za.games.kakuro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class KakuroTest {

    /** Küçük el yapımı bulmaca: 4×4, iç 3×3'te sol üst köşe kara. */
    private fun sample(): KakuroState {
        val n = 4
        val w = BooleanArray(n * n) { it / n > 0 && it % n > 0 }
        w[1 * n + 1] = false
        // Çözüm (satır 1: _ 1 2, satır 2: 3 4 5, satır 3: 6 7 8) → koşu içi rakamlar farklı.
        val sol = IntArray(n * n)
        sol[1 * n + 2] = 1; sol[1 * n + 3] = 2
        sol[2 * n + 1] = 3; sol[2 * n + 2] = 4; sol[2 * n + 3] = 5
        sol[3 * n + 1] = 6; sol[3 * n + 2] = 7; sol[3 * n + 3] = 8
        val cells = KakuroLogic.withClues(n, w, sol)
        return KakuroState.restore(n, cells, sol.toList(), List(n * n) { 0 }, List(n * n) { emptySet() }, KakuroDifficulty.EASY, 1L)
    }

    @Test
    fun combosAreDistinctDigitSets() {
        assertEquals(listOf(0b11), KakuroLogic.combos(3, 2).toList())
        assertEquals(1, KakuroLogic.combos(45, 9).size)
        assertEquals(0x1FF, KakuroLogic.combos(45, 9)[0])
        assertEquals(4, KakuroLogic.combos(10, 2).size) // 1+9, 2+8, 3+7, 4+6
        assertEquals(0, KakuroLogic.combos(4, 3).size)
        assertEquals(1, KakuroLogic.combos(6, 3).size) // 1+2+3
    }

    @Test
    fun runsAndCluesFromCells() {
        val s = sample()
        val runs = s.runs
        assertEquals(6, runs.size)
        val across = runs.filter { it.horizontal }
        assertEquals(listOf(3, 12, 21), across.map { it.sum })
        val down = runs.filter { !it.horizontal }
        assertEquals(listOf(9, 12, 15), down.map { it.sum })
        assertEquals(listOf(2, 3, 3), across.map { it.cells.size })
        // Her beyaz hücre bir yatay ve bir dikey koşuda.
        for (i in 0 until 16) {
            if (s.cells[i].white) {
                assertTrue(s.acrossOf[i] >= 0)
                assertTrue(s.downOf[i] >= 0)
            }
        }
    }

    @Test
    fun solverCountsSolutions() {
        val s = sample()
        val count = KakuroLogic.countSolutions(s.size, s.cells, limit = 5)
        assertTrue(count >= 1)
        // Çözümün kendisi geçerli: değerleri doldurunca çözülmüş sayılır.
        var st = s
        for (i in 0 until 16) if (s.cells[i].white) st = st.setValue(i, s.solution[i])
        assertEquals(KakuroStatus.SOLVED, st.status)
        assertTrue(st.conflicts.isEmpty())
        assertEquals(6, st.solvedRuns.size)
    }

    @Test
    fun conflictsDetectDuplicatesAndSums() {
        val s = sample()
        val n = 4
        // Aynı yatay koşuya iki kez 1: yineleme.
        var st = s.setValue(1 * n + 2, 1).setValue(1 * n + 3, 1)
        assertTrue(st.conflicts.containsAll(listOf(1 * n + 2, 1 * n + 3)))
        // Toplamı aşan kısmi doldurma (yatay 3'e 9): dolu hücre işaretli.
        st = s.setValue(1 * n + 2, 9)
        assertTrue(st.conflicts.contains(1 * n + 2))
        // Doğru rakam: temiz.
        st = s.setValue(1 * n + 2, 1)
        assertTrue(st.conflicts.isEmpty())
        // Aynı rakama tekrar basmak siler; not eklenip silinir.
        assertEquals(0, st.setValue(1 * n + 2, 1).values[1 * n + 2])
        val noted = s.toggleNote(2 * n + 1, 3).toggleNote(2 * n + 1, 7)
        assertEquals(setOf(3, 7), noted.notes[2 * n + 1])
        // Koşuya 3 yazılınca komşu hücrelerdeki 3 notu düşer.
        val after = noted.setValue(2 * n + 2, 3)
        assertEquals(setOf(7), after.notes[2 * n + 1])
        // Kara hücreye yazılamaz.
        assertEquals(s, s.setValue(0, 5))
    }

    @Test
    fun layoutInvariants() {
        val rng = Random(5)
        var made = 0
        var tries = 0
        while (made < 5 && tries++ < 50) {
            val n = 10
            val white = KakuroLogic.layout(n, 0.25f, rng) ?: continue
            made++
            assertTrue(KakuroLogic.violations(n, white).isEmpty())
            for (i in 0 until n) {
                assertFalse(white[i])
                assertFalse(white[i * n])
            }
            // 180° simetri.
            for (r in 1 until n) for (c in 1 until n) {
                assertEquals(white[r * n + c], white[(n - r) * n + (n - c)])
            }
        }
        assertEquals(5, made)
    }

    @Test
    fun generatedPuzzlesAreUniqueAndConsistent() {
        for (difficulty in KakuroDifficulty.entries) {
            val t0 = System.nanoTime()
            val s = KakuroState.newGame(difficulty, seed = 42L + difficulty.ordinal)
            val ms = (System.nanoTime() - t0) / 1_000_000
            println("${difficulty.name}: ${s.size}x${s.size}, ${s.whiteCount} beyaz, ${s.runs.size} koşu, $ms ms, düzen ${KakuroLogic.lastLayouts}, doldurma ${KakuroLogic.lastFills}")
            assertEquals(1, KakuroLogic.countSolutions(s.size, s.cells, 2))
            for (run in s.runs) {
                assertTrue(run.cells.size in KakuroLogic.MIN_RUN..KakuroLogic.MAX_RUN)
                assertEquals(run.sum, run.cells.sumOf { s.solution[it] })
                assertEquals(run.cells.size, run.cells.map { s.solution[it] }.toSet().size)
            }
            for (i in 0 until s.size * s.size) {
                if (s.cells[i].white) {
                    assertTrue(s.solution[i] in 1..9)
                    assertTrue(s.acrossOf[i] >= 0 && s.downOf[i] >= 0)
                } else {
                    assertEquals(0, s.solution[i])
                }
            }
            assertTrue(s.whiteCount >= (s.size - 1) * (s.size - 1) * 0.40)
        }
    }

    @Test
    fun sameSeedSamePuzzle() {
        val a = KakuroState.newGame(KakuroDifficulty.MEDIUM, 7L)
        val b = KakuroState.newGame(KakuroDifficulty.MEDIUM, 7L)
        assertEquals(a.cells, b.cells)
        assertEquals(a.solution, b.solution)
        val c = KakuroState.newGame(KakuroDifficulty.MEDIUM, 8L)
        assertFalse(a.solution == c.solution)
    }

    @Test
    fun generationIsFastEnough() {
        for (difficulty in KakuroDifficulty.entries) {
            var worst = 0L
            var total = 0L
            repeat(8) {
                val t0 = System.nanoTime()
                KakuroState.newGame(difficulty, 200L + it)
                val ms = (System.nanoTime() - t0) / 1_000_000
                total += ms
                worst = maxOf(worst, ms)
            }
            println("${difficulty.name}: ortalama ${total / 8} ms, en kötü $worst ms")
            assertTrue("${difficulty.name} üretimi çok yavaş: en kötü $worst ms", worst < 3000)
        }
    }
}
