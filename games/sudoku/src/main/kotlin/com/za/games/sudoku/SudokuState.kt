package com.za.games.sudoku

import kotlin.random.Random

enum class SudokuDifficulty(val targetClues: Int) {
    EASY(40), MEDIUM(32), HARD(26)
}

enum class SudokuStatus { RUNNING, SOLVED }

/**
 * Sudoku motoru: değişmez durum makinesi.
 *
 * Bulmacalar geri izlemeli (backtracking) üreteçle kurulur: önce dolu ve
 * geçerli bir çözüm üretilir, sonra hücreler **tek çözüm** garantisi
 * bozulmayacak şekilde silinir. Rastgelelik [seed] üzerinden deterministiktir.
 * İpucu hücreleri değiştirilemez; bulmaca, çözümle birebir eşleşince biter.
 */
data class SudokuState(
    /** 81 hücrelik tam çözüm (1..9). */
    val solution: List<Int>,
    /** 81 hücre: ipucu mu? İpucu hücreleri değiştirilemez. */
    val given: List<Boolean>,
    /** 81 hücrelik mevcut tablo; 0 = boş. */
    val values: List<Int>,
    /** Hücre başına kalem notları (1..9). */
    val notes: List<Set<Int>>,
    val difficulty: SudokuDifficulty,
    val status: SudokuStatus,
    val seed: Long,
) {

    /** Satır/sütun/kutu içinde yinelenen değere sahip hücre indeksleri. */
    val conflicts: Set<Int>
        get() {
            val bad = mutableSetOf<Int>()
            for (index in 0 until 81) {
                val v = values[index]
                if (v == 0) continue
                if (peers(index).any { values[it] == v }) bad += index
            }
            return bad
        }

    fun setValue(index: Int, value: Int): SudokuState {
        if (status != SudokuStatus.RUNNING || given[index] || value !in 1..9) return this
        if (values[index] == value) return clearCell(index) // aynı rakama tekrar basmak siler

        val newValues = values.toMutableList().also { it[index] = value }
        // Hücrenin notlarını ve komşulardaki aynı rakam notlarını temizle.
        val newNotes = notes.toMutableList()
        newNotes[index] = emptySet()
        for (peer in peers(index)) {
            if (value in newNotes[peer]) newNotes[peer] = newNotes[peer] - value
        }
        return copy(
            values = newValues,
            notes = newNotes,
            status = if (newValues == solution) SudokuStatus.SOLVED else SudokuStatus.RUNNING,
        )
    }

    fun toggleNote(index: Int, value: Int): SudokuState {
        if (status != SudokuStatus.RUNNING || given[index] || value !in 1..9) return this
        if (values[index] != 0) return this // dolu hücreye not yazılmaz
        val newNotes = notes.toMutableList()
        newNotes[index] =
            if (value in newNotes[index]) newNotes[index] - value else newNotes[index] + value
        return copy(notes = newNotes)
    }

    fun clearCell(index: Int): SudokuState {
        if (status != SudokuStatus.RUNNING || given[index]) return this
        if (values[index] == 0 && notes[index].isEmpty()) return this
        val newValues = values.toMutableList().also { it[index] = 0 }
        val newNotes = notes.toMutableList().also { it[index] = emptySet() }
        return copy(values = newValues, notes = newNotes)
    }

    companion object {

        fun newGame(
            difficulty: SudokuDifficulty,
            seed: Long = Random.nextLong(),
        ): SudokuState {
            val rng = Random(seed)
            val solution = generateSolved(rng)
            val values = solution.copyOf()

            var clues = 81
            for (index in (0 until 81).shuffled(rng)) {
                if (clues <= difficulty.targetClues) break
                val backup = values[index]
                values[index] = 0
                if (countSolutions(values, limit = 2) == 1) clues-- else values[index] = backup
            }

            return SudokuState(
                solution = solution.toList(),
                given = values.map { it != 0 },
                values = values.toList(),
                notes = List(81) { emptySet() },
                difficulty = difficulty,
                status = SudokuStatus.RUNNING,
                seed = seed,
            )
        }

        /** index hücresiyle aynı satır, sütun ya da kutudaki diğer hücreler. */
        fun peers(index: Int): List<Int> {
            val row = index / 9
            val col = index % 9
            val boxRow = (row / 3) * 3
            val boxCol = (col / 3) * 3
            val result = mutableListOf<Int>()
            for (i in 0 until 9) {
                val rowPeer = row * 9 + i
                val colPeer = i * 9 + col
                if (rowPeer != index) result += rowPeer
                if (colPeer != index) result += colPeer
            }
            for (r in boxRow until boxRow + 3) {
                for (c in boxCol until boxCol + 3) {
                    val boxPeer = r * 9 + c
                    if (boxPeer != index && r != row && c != col) result += boxPeer
                }
            }
            return result
        }

        private fun canPlace(grid: IntArray, index: Int, value: Int): Boolean =
            peers(index).none { grid[it] == value }

        /** Geri izlemeyle dolu ve geçerli bir tahta üretir. */
        internal fun generateSolved(rng: Random): IntArray {
            val grid = IntArray(81)
            fun fill(pos: Int): Boolean {
                if (pos == 81) return true
                for (value in (1..9).shuffled(rng)) {
                    if (canPlace(grid, pos, value)) {
                        grid[pos] = value
                        if (fill(pos + 1)) return true
                        grid[pos] = 0
                    }
                }
                return false
            }
            check(fill(0)) { "Sudoku üretimi başarısız" }
            return grid
        }

        /**
         * Çözüm sayısını [limit] değerine kadar sayar. En az adaylı boş
         * hücreyi seçerek (MRV) hızlı kalır; benzersizlik testi için
         * limit=2 yeterlidir.
         */
        internal fun countSolutions(grid: IntArray, limit: Int): Int {
            var bestIndex = -1
            var bestCandidates: List<Int> = emptyList()
            for (index in 0 until 81) {
                if (grid[index] != 0) continue
                val candidates = (1..9).filter { canPlace(grid, index, it) }
                if (candidates.isEmpty()) return 0
                if (bestIndex == -1 || candidates.size < bestCandidates.size) {
                    bestIndex = index
                    bestCandidates = candidates
                    if (candidates.size == 1) break
                }
            }
            if (bestIndex == -1) return 1 // boş hücre yok: tek çözüm bulundu

            var count = 0
            for (value in bestCandidates) {
                grid[bestIndex] = value
                count += countSolutions(grid, limit - count)
                grid[bestIndex] = 0
                if (count >= limit) break
            }
            return count
        }
    }
}
