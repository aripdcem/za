package com.za.games.mines

import kotlin.random.Random

enum class MinesDifficulty(val width: Int, val height: Int, val mineCount: Int) {
    EASY(9, 12, 14), MEDIUM(10, 14, 25), HARD(12, 17, 40)
}

enum class MinesStatus { READY, RUNNING, WON, LOST }

/**
 * Mayın Tarlası motoru: değişmez durum makinesi.
 *
 * Mayınlar ilk açılışta, açılan hücre ve komşuları hariç tutularak
 * yerleştirilir (ilk dokunuş her zaman güvenlidir) ve [seed] üzerinden
 * deterministiktir. Sıfır komşulu hücreler akarak açılır (flood fill);
 * bayraklı hücreler açılmaz. Sayı hücresine tekrar dokunmak, bayrak
 * sayısı tutuyorsa komşuları toplu açar (chord).
 */
data class MinesState(
    val width: Int,
    val height: Int,
    val mineCount: Int,
    /** Mayın indeksleri; ilk açılışa kadar boştur. */
    val mines: Set<Int>,
    val revealed: Set<Int>,
    val flagged: Set<Int>,
    val status: MinesStatus,
    val seed: Long,
    /** Kaybettiren mayının indeksi. */
    val exploded: Int? = null,
) {

    val cellCount: Int get() = width * height

    fun neighbors(index: Int): List<Int> {
        val row = index / width
        val col = index % width
        val result = mutableListOf<Int>()
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = row + dr
                val c = col + dc
                if (r in 0 until height && c in 0 until width) result += r * width + c
            }
        }
        return result
    }

    fun adjacentMines(index: Int): Int = neighbors(index).count { it in mines }

    fun reveal(index: Int): MinesState {
        if (status == MinesStatus.READY) return placeMines(index).reveal(index)
        if (status != MinesStatus.RUNNING) return this
        if (index in flagged || index in revealed) return this

        if (index in mines) {
            return copy(status = MinesStatus.LOST, exploded = index)
        }

        val newRevealed = revealed.toMutableSet()
        val stack = ArrayDeque<Int>()
        stack.addLast(index)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (!newRevealed.add(current)) continue
            if (adjacentMines(current) == 0) {
                for (n in neighbors(current)) {
                    if (n !in newRevealed && n !in flagged && n !in mines) stack.addLast(n)
                }
            }
        }

        val won = newRevealed.size == cellCount - mineCount
        return copy(
            revealed = newRevealed,
            status = if (won) MinesStatus.WON else MinesStatus.RUNNING,
        )
    }

    fun toggleFlag(index: Int): MinesState {
        if (status != MinesStatus.RUNNING && status != MinesStatus.READY) return this
        if (index in revealed) return this
        return copy(flagged = if (index in flagged) flagged - index else flagged + index)
    }

    /**
     * Chord: açık bir sayı hücresinin çevresindeki bayrak sayısı sayıya
     * eşitse, bayraksız komşuları toplu açar. Yanlış bayrak mayına
     * bastırabilir — klasik davranış.
     */
    fun chord(index: Int): MinesState {
        if (status != MinesStatus.RUNNING || index !in revealed) return this
        val number = adjacentMines(index)
        if (number == 0) return this
        val around = neighbors(index)
        if (around.count { it in flagged } != number) return this

        var state = this
        for (n in around) {
            if (state.status != MinesStatus.RUNNING) break
            if (n !in state.flagged && n !in state.revealed) state = state.reveal(n)
        }
        return state
    }

    private fun placeMines(firstIndex: Int): MinesState {
        val rng = Random(seed)
        val safeZone = (neighbors(firstIndex) + firstIndex).toSet()
        val candidates = (0 until cellCount).filter { it !in safeZone }
        val mines = candidates.shuffled(rng).take(mineCount).toSet()
        return copy(mines = mines, status = MinesStatus.RUNNING)
    }

    companion object {
        fun newGame(
            difficulty: MinesDifficulty,
            seed: Long = Random.nextLong(),
        ): MinesState = MinesState(
            width = difficulty.width,
            height = difficulty.height,
            mineCount = difficulty.mineCount,
            mines = emptySet(),
            revealed = emptySet(),
            flagged = emptySet(),
            status = MinesStatus.READY,
            seed = seed,
        )
    }
}
