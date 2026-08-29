package com.za.games.g2048

import kotlin.random.Random

enum class MoveDir { LEFT, RIGHT, UP, DOWN }

enum class G2048Status { RUNNING, OVER }

/**
 * 2048 motoru: değişmez durum makinesi.
 *
 * Klasik kurallar: kaydırmada taşlar yönde sıkışır, eşit komşular hamle
 * başına bir kez birleşir, değişen her hamleden sonra boş bir hücreye
 * %90 olasılıkla 2, %10 olasılıkla 4 doğar. Skor, birleşen taşların
 * toplamı kadar artar. Rastgelelik [seed] üzerinden deterministiktir.
 */
data class G2048State(
    val size: Int,
    /** size*size hücre; 0 boş, diğerleri taş değeri (2, 4, 8, ...). */
    val cells: List<Int>,
    val score: Long,
    val moves: Int,
    val status: G2048Status,
    /** 2048'e en az bir kez ulaşıldı mı? Oyun devam eder; arayüz kutlar. */
    val reached2048: Boolean,
    val seed: Long,
    /** Son hamlede doğan taşın indeksi (animasyon için); yoksa -1. */
    val lastSpawn: Int = -1,
    /** Son hamlede birleşme sonucu oluşan taşların indeksleri. */
    val lastMerged: List<Int> = emptyList(),
) {

    fun move(dir: MoveDir): G2048State {
        if (status != G2048Status.RUNNING) return this

        val next = cells.toMutableList()
        val merged = mutableListOf<Int>()
        var gained = 0L

        for (line in lineIndices(size, dir)) {
            val values = line.map { cells[it] }.filter { it != 0 }
            val out = IntArray(size)
            var outPos = 0
            var i = 0
            while (i < values.size) {
                if (i + 1 < values.size && values[i] == values[i + 1]) {
                    val m = values[i] * 2
                    out[outPos] = m
                    gained += m
                    merged += line[outPos]
                    i += 2
                } else {
                    out[outPos] = values[i]
                    i += 1
                }
                outPos++
            }
            line.forEachIndexed { pos, idx -> next[idx] = out[pos] }
        }

        if (next == cells) return this // hamle tahtayı değiştirmediyse taş doğmaz

        val rng = Random(seed)
        val empties = next.indices.filter { next[it] == 0 }
        val spawnIndex = empties[rng.nextInt(empties.size)]
        next[spawnIndex] = if (rng.nextInt(10) < 9) 2 else 4

        val state = copy(
            cells = next,
            score = score + gained,
            moves = moves + 1,
            reached2048 = reached2048 || next.any { it >= 2048 },
            seed = rng.nextLong(),
            lastSpawn = spawnIndex,
            lastMerged = merged,
        )
        return if (state.hasAnyMove()) state else state.copy(status = G2048Status.OVER)
    }

    private fun hasAnyMove(): Boolean {
        if (cells.any { it == 0 }) return true
        for (r in 0 until size) {
            for (c in 0 until size) {
                val v = cells[r * size + c]
                if (c + 1 < size && cells[r * size + c + 1] == v) return true
                if (r + 1 < size && cells[(r + 1) * size + c] == v) return true
            }
        }
        return false
    }

    companion object {
        const val SIZE = 4

        fun newGame(seed: Long = Random.nextLong()): G2048State {
            val rng = Random(seed)
            val cells = MutableList(SIZE * SIZE) { 0 }
            repeat(2) {
                val empties = cells.indices.filter { cells[it] == 0 }
                cells[empties[rng.nextInt(empties.size)]] = if (rng.nextInt(10) < 9) 2 else 4
            }
            return G2048State(
                size = SIZE,
                cells = cells,
                score = 0L,
                moves = 0,
                status = G2048Status.RUNNING,
                reached2048 = false,
                seed = rng.nextLong(),
            )
        }

        /** Her hat, kaydırma yönüne doğru sıralanmış hücre indeksleri. */
        private fun lineIndices(size: Int, dir: MoveDir): List<List<Int>> = when (dir) {
            MoveDir.LEFT -> (0 until size).map { r -> (0 until size).map { c -> r * size + c } }
            MoveDir.RIGHT -> (0 until size).map { r -> (size - 1 downTo 0).map { c -> r * size + c } }
            MoveDir.UP -> (0 until size).map { c -> (0 until size).map { r -> r * size + c } }
            MoveDir.DOWN -> (0 until size).map { c -> (size - 1 downTo 0).map { r -> r * size + c } }
        }
    }
}
