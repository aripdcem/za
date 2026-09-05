package com.za.games.sayi

/**
 * Vergici (Taxman): tahtada 1..[n] sayıları. Oyuncu, tahtada en az bir tam
 * böleni kalmış bir sayıyı alır ve puanına ekler; vergici o sayının tahtada
 * kalan tüm tam bölenlerini alır. Alınabilecek sayı kalmayınca vergici
 * kalan her şeyi alır. Puanı yüksek olan kazanır. Değişmez durum.
 */
data class VergiciState(
    val n: Int,
    /** Dizin 1..n: tahtada mı? Dizin 0 kullanılmaz. */
    val remaining: List<Boolean>,
    val player: Int = 0,
    val taxman: Int = 0,
    /** Son hamle: alınan sayı ve vergicinin aldığı bölenler (animasyon için). */
    val lastTaken: Int = 0,
    val lastTaxed: List<Int> = emptyList(),
    /** Oyun bitince vergiciye kalan sayılar. */
    val leftovers: List<Int> = emptyList(),
    val over: Boolean = false,
    val moves: List<Int> = emptyList(),
) {
    /** [x] sayısının tahtada kalan tam bölenleri. */
    fun divisorsOf(x: Int): List<Int> {
        if (x !in 1..n) return emptyList()
        val out = ArrayList<Int>()
        for (d in 1 until x) if (x % d == 0 && remaining[d]) out += d
        return out
    }

    fun canTake(x: Int): Boolean = x in 1..n && remaining[x] && divisorsOf(x).isNotEmpty()

    val takeable: List<Int> get() = (1..n).filter { canTake(it) }

    val boardNumbers: List<Int> get() = (1..n).filter { remaining[it] }

    fun take(x: Int): VergiciState {
        if (over || !canTake(x)) return this
        val taxed = divisorsOf(x)
        val rem = remaining.toMutableList()
        rem[x] = false
        for (d in taxed) rem[d] = false
        val next = copy(
            remaining = rem,
            player = player + x,
            taxman = taxman + taxed.sum(),
            lastTaken = x,
            lastTaxed = taxed,
            moves = moves + x,
        )
        if (next.takeable.isNotEmpty()) return next
        val left = (1..n).filter { rem[it] }
        return next.copy(
            taxman = next.taxman + left.sum(),
            remaining = List(n + 1) { false },
            leftovers = left,
            over = true,
        )
    }

    /** Oyuncunun kazanması için gereken puan: toplamın yarısından fazlası. */
    val total: Int get() = n * (n + 1) / 2

    companion object {
        val SIZES = listOf(12, 20, 30, 40)

        fun start(n: Int): VergiciState = VergiciState(n, List(n + 1) { it >= 1 })
    }
}

/** Vergici çözücüleri: hedef puan için tam arama (küçük n) ve açgözlü sezgisel. */
object VergiciSolver {
    /** Son tam aramada gezilen durum sayısı (ölçüm için). */
    @Volatile
    var lastNodes = 0
        private set

    /**
     * Oyuncunun ulaşabileceği en yüksek puan: kalan sayılar kümesi üzerinde
     * bellekli tam arama. [budget] durumdan fazlası gerekirse null.
     */
    fun optimal(n: Int, budget: Int = 400_000): Int? {
        require(n in 1..62)
        val memo = HashMap<Long, Int>()
        var nodes = 0
        val divMasks = LongArray(n + 1)
        for (x in 1..n) {
            var m = 0L
            for (d in 1 until x) if (x % d == 0) m = m or (1L shl d)
            divMasks[x] = m
        }

        fun best(mask: Long): Int? {
            memo[mask]?.let { return it }
            if (++nodes > budget) return null
            var bestValue = 0
            var x = n
            while (x >= 1) {
                if ((mask and (1L shl x)) != 0L) {
                    val divs = divMasks[x] and mask
                    if (divs != 0L) {
                        val sub = best(mask and (1L shl x).inv() and divs.inv()) ?: return null
                        val v = x + sub
                        if (v > bestValue) bestValue = v
                    }
                }
                x--
            }
            memo[mask] = bestValue
            return bestValue
        }

        val full = (1L shl (n + 1)) - 2L
        val result = best(full)
        lastNodes = nodes
        return result
    }

    /** Açgözlü: alınabilir sayılar içinde (sayı − bölen toplamı) en büyük olanı, eşitlikte büyüğü. */
    fun greedyScore(n: Int): Int {
        var s = VergiciState.start(n)
        while (!s.over) {
            val pick = s.takeable.maxWithOrNull(compareBy({ it - s.divisorsOf(it).sum() }, { it })) ?: break
            s = s.take(pick)
        }
        return s.player
    }
}
