package com.za.games.sayi

import kotlin.random.Random

/**
 * Toplam Kapma (Number Scrabble / Fifteen): 1..9 sayıları ortada; oyuncular
 * sırayla bir sayı alır. Elindeki üç sayının toplamı 15 olan kazanır; dokuz
 * sayı bitince kimse kazanmadıysa berabere. Değişmez durum.
 */
data class ToplamState(
    val picks: List<List<Int>> = listOf(emptyList(), emptyList()),
    val turn: Int = 0,
    val winner: Int? = null,
    val over: Boolean = false,
    /** Kazanan üçlü (vurgu için). */
    val winningTriple: List<Int> = emptyList(),
) {
    val available: List<Int> get() = (1..9).filter { it !in picks[0] && it !in picks[1] }

    fun ownerOf(x: Int): Int? = when {
        x in picks[0] -> 0
        x in picks[1] -> 1
        else -> null
    }

    fun pick(x: Int): ToplamState {
        if (over || x !in available) return this
        val mine = picks[turn] + x
        val newPicks = picks.toMutableList().also { it[turn] = mine }
        val triple = triple(mine)
        return when {
            triple != null -> copy(picks = newPicks, winner = turn, over = true, winningTriple = triple)
            newPicks[0].size + newPicks[1].size == 9 -> copy(picks = newPicks, over = true, turn = 1 - turn)
            else -> copy(picks = newPicks, turn = 1 - turn)
        }
    }

    companion object {
        /** Toplamı 15 olan üçlü varsa onu döndürür. */
        fun triple(picks: List<Int>): List<Int>? {
            val s = picks.sorted()
            for (i in s.indices) for (j in i + 1 until s.size) for (k in j + 1 until s.size) {
                if (s[i] + s[j] + s[k] == 15) return listOf(s[i], s[j], s[k])
            }
            return null
        }

        /** 3×3 sihirli kare (Lo Shu): her satır, sütun ve köşegen 15; oyun aslında üç taştır. */
        val MAGIC_SQUARE = listOf(2, 7, 6, 9, 5, 1, 4, 3, 8)
    }
}

/** Toplam Kapma bilgisayarı: minimax (kusursuz) ya da ara sıra rastgele hata yapan kolay seviye. */
object ToplamAi {
    private val memo = HashMap<Int, Int>()

    /** Sıradaki oyuncunun bakış açısıyla değer: 1 kazanır, 0 berabere, −1 kaybeder. */
    private fun value(mine: Int, theirs: Int): Int {
        val key = mine or (theirs shl 9)
        memo[key]?.let { return it }
        var best = -2
        for (x in 1..9) {
            val bit = 1 shl (x - 1)
            if ((mine or theirs) and bit != 0) continue
            val next = mine or bit
            val v = when {
                wins(next) -> 1
                (next or theirs) == 0x1FF -> 0
                else -> -value(theirs, next)
            }
            if (v > best) best = v
            if (best == 1) break
        }
        if (best == -2) best = 0
        memo[key] = best
        return best
    }

    private fun wins(mask: Int): Boolean {
        val list = (1..9).filter { (mask and (1 shl (it - 1))) != 0 }
        return ToplamState.triple(list) != null
    }

    private fun mask(list: List<Int>): Int = list.fold(0) { acc, x -> acc or (1 shl (x - 1)) }

    /** Sıradaki oyuncu için hamle; [perfect] değilse yüzde 35 rastgele oynar. */
    fun choose(state: ToplamState, perfect: Boolean, rng: Random): Int {
        val available = state.available
        require(available.isNotEmpty())
        if (!perfect && rng.nextFloat() < 0.35f) return available[rng.nextInt(available.size)]
        val mine = mask(state.picks[state.turn])
        val theirs = mask(state.picks[1 - state.turn])
        var bestValue = -2
        val bestMoves = ArrayList<Int>()
        for (x in available) {
            val bit = 1 shl (x - 1)
            val next = mine or bit
            val v = when {
                wins(next) -> 1
                (next or theirs) == 0x1FF -> 0
                else -> -value(theirs, next)
            }
            if (v > bestValue) {
                bestValue = v
                bestMoves.clear()
            }
            if (v == bestValue) bestMoves += x
        }
        return bestMoves[rng.nextInt(bestMoves.size)]
    }
}
