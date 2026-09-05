package com.za.games.kakuro

import kotlin.random.Random

/**
 * Kakuro kuralları: koşu çıkarımı, rakam kombinasyonları, yayılımlı çözücü
 * (çözüm sayma) ve üretici. Üretici, 180° simetrik bir düzen kurar, koşuları
 * rastgele farklı rakamlarla doldurur, toplamları ipucu yapar ve tek çözüm
 * bulunana dek doldurmayı (gerekirse düzeni) yeniler.
 */
object KakuroLogic {
    const val MIN_RUN = 2
    const val MAX_RUN = 9
    private const val FILL_TRIES = 6
    private const val FILL_NODE_BUDGET = 200_000
    private const val REPAIR_TRIES = 160
    private const val REPAIR_SAMPLES = 6

    /** Son üretimin sayaçları (ölçüm ve testler için). */
    @Volatile
    var lastLayouts = 0
        private set

    @Volatile
    var lastFills = 0
        private set

    /** Uç rakamlar (1, 2, 8, 9) az kombinasyonlu toplamlar verir; doldurma bunlara eğilimlidir. */
    private val DIGIT_WEIGHTS = doubleArrayOf(3.0, 2.0, 1.2, 1.0, 1.0, 1.0, 1.2, 2.0, 3.0)

    /** Hücrelerden koşuları çıkarır; ipucu koşunun başındaki kara hücreden okunur. */
    fun runs(size: Int, cells: List<Cell>): List<Run> {
        val out = ArrayList<Run>()
        for (r in 0 until size) {
            var c = 0
            while (c < size) {
                if (!cells[r * size + c].white) {
                    c++
                    continue
                }
                val start = c
                val list = ArrayList<Int>()
                while (c < size && cells[r * size + c].white) {
                    list += r * size + c
                    c++
                }
                val clue = if (start > 0) cells[r * size + start - 1].across else 0
                out += Run(clue, list, horizontal = true)
            }
        }
        for (c in 0 until size) {
            var r = 0
            while (r < size) {
                if (!cells[r * size + c].white) {
                    r++
                    continue
                }
                val start = r
                val list = ArrayList<Int>()
                while (r < size && cells[r * size + c].white) {
                    list += r * size + c
                    r++
                }
                val clue = if (start > 0) cells[(start - 1) * size + c].down else 0
                out += Run(clue, list, horizontal = false)
            }
        }
        return out
    }

    private val comboCache = HashMap<Int, IntArray>()

    /** [sum] toplamlı, [len] uzunluklu farklı rakam kümeleri; bit d-1 = rakam d. */
    fun combos(sum: Int, len: Int): IntArray = synchronized(comboCache) {
        comboCache.getOrPut(sum * 16 + len) {
            val out = ArrayList<Int>()
            for (mask in 1 until 512) {
                if (Integer.bitCount(mask) != len) continue
                var s = 0
                for (d in 1..9) if (mask and (1 shl (d - 1)) != 0) s += d
                if (s == sum) out += mask
            }
            out.toIntArray()
        }
    }

    /** Çözüm sayısını [limit]'e kadar sayar (2 = tek çözüm denetimi için yeter). */
    fun countSolutions(size: Int, cells: List<Cell>, limit: Int = 2): Int =
        solutions(size, cells, limit).size

    /** En çok [limit] çözüm; her çözüm hücre başına rakam (kara hücrede 0). */
    fun solutions(size: Int, cells: List<Cell>, limit: Int = 2): List<IntArray> {
        val runs = runs(size, cells)
        val search = Search(runs, runs.map { combos(it.sum, it.cells.size) }, cells)
        val cand = IntArray(size * size) { if (cells[it].white) 0x1FF else 0 }
        val found = ArrayList<IntArray>()
        search.collect(cand, limit, found)
        return found
    }

    private class Search(
        private val runs: List<Run>,
        private val runCombos: List<IntArray>,
        private val cells: List<Cell>,
    ) {
        /** Aday kümelerini koşu kombinasyonlarıyla daraltır; çelişkide false. */
        fun propagate(cand: IntArray): Boolean {
            var changed = true
            while (changed) {
                changed = false
                for (ri in runs.indices) {
                    val run = runs[ri]
                    var fixedMask = 0
                    var fixedCount = 0
                    var unfixedUnion = 0
                    for (c in run.cells) {
                        val m = cand[c]
                        if (m == 0) return false
                        if (Integer.bitCount(m) == 1) {
                            if ((fixedMask and m) != 0) return false
                            fixedMask = fixedMask or m
                            fixedCount++
                        } else {
                            unfixedUnion = unfixedUnion or m
                        }
                    }
                    if (fixedCount == run.cells.size) {
                        var ok = false
                        for (combo in runCombos[ri]) {
                            if (combo == fixedMask) {
                                ok = true
                                break
                            }
                        }
                        if (!ok) return false
                        continue
                    }
                    var allowed = 0
                    for (combo in runCombos[ri]) {
                        if ((combo and fixedMask) != fixedMask) continue
                        val rest = combo and fixedMask.inv()
                        if ((rest and unfixedUnion.inv()) != 0) continue
                        allowed = allowed or rest
                    }
                    if (allowed == 0) return false
                    for (c in run.cells) {
                        val m = cand[c]
                        if (Integer.bitCount(m) == 1) continue
                        val nm = m and allowed
                        if (nm != m) {
                            if (nm == 0) return false
                            cand[c] = nm
                            changed = true
                        }
                    }
                }
            }
            return true
        }

        fun collect(cand: IntArray, limit: Int, found: MutableList<IntArray>) {
            if (found.size >= limit) return
            if (!propagate(cand)) return
            var best = -1
            var bestBits = 10
            for (i in cand.indices) {
                if (!cells[i].white) continue
                val b = Integer.bitCount(cand[i])
                if (b > 1 && b < bestBits) {
                    bestBits = b
                    best = i
                }
            }
            if (best < 0) {
                found += IntArray(cand.size) { if (cells[it].white) Integer.numberOfTrailingZeros(cand[it]) + 1 else 0 }
                return
            }
            var m = cand[best]
            while (m != 0 && found.size < limit) {
                val bit = m and (-m)
                m = m xor bit
                val copy = cand.copyOf()
                copy[best] = bit
                collect(copy, limit, found)
            }
        }
    }

    fun generate(difficulty: KakuroDifficulty, seed: Long): KakuroState {
        val rng = Random(seed)
        val n = difficulty.size
        var layouts = 0
        var fills = 0
        var misses = 0
        while (true) {
            val white = layout(n, difficulty.blackRatio, rng)
            if (white == null) {
                check(++misses < 500) { "Kakuro düzeni üretilemedi: ${difficulty.name}" }
                continue
            }
            layouts++
            for (attempt in 0 until FILL_TRIES) {
                val solution = fill(n, white, rng) ?: continue
                fills++
                if (!makeUnique(n, white, solution, rng)) continue
                lastLayouts = layouts
                lastFills = fills
                return KakuroState(
                    size = n,
                    cells = withClues(n, white, solution),
                    solution = solution.toList(),
                    values = List(n * n) { 0 },
                    notes = List(n * n) { emptySet() },
                    difficulty = difficulty,
                    status = KakuroStatus.RUNNING,
                    seed = seed,
                )
            }
        }
    }

    /** Son üretimde tek çözüme ulaşmak için yapılan onarım turu sayısı (ölçüm için). */
    @Volatile
    var lastRepairs = 0
        private set

    /**
     * Tek çözüme dek hedefli onarım: iki çözüm bulunursa farklı hücrelerin rakamları
     * yeniden seçilir (koşu içinde yinelenmeden, geri izlemeyle). Birkaç aday
     * arasından etkilenen koşuların kombinasyon sayısını en aza indiren (en sıkı)
     * seçilir; ipuçları yenilenir. Başarısızsa false (doldurma yenilenir).
     */
    internal fun makeUnique(n: Int, white: BooleanArray, solution: IntArray, rng: Random): Boolean {
        val plain = List(n * n) { Cell(white[it]) }
        val runs = runs(n, plain)
        val acrossOf = IntArray(n * n) { -1 }
        val downOf = IntArray(n * n) { -1 }
        runs.forEachIndexed { i, run -> for (c in run.cells) if (run.horizontal) acrossOf[c] = i else downOf[c] = i }

        fun tightness(cells: List<Int>): Double {
            val affected = HashSet<Int>()
            for (c in cells) {
                affected += acrossOf[c]
                affected += downOf[c]
            }
            var score = 0.0
            for (r in affected) {
                val run = runs[r]
                val sum = run.cells.sumOf { solution[it] }
                score += Math.log(combos(sum, run.cells.size).size.toDouble() + 1.0)
            }
            return score
        }

        /** [targets] hücrelerini rastgele sırayla geri izlemeyle yeniden doldurur. */
        fun refill(targets: List<Int>): Boolean {
            val order = targets.shuffled(rng)
            for (c in order) solution[c] = 0
            var nodes = 0
            fun rec(k: Int): Boolean {
                if (k == order.size) return true
                if (++nodes > 20_000) return false
                val c = order[k]
                var used = 0
                for (r in intArrayOf(acrossOf[c], downOf[c])) {
                    for (o in runs[r].cells) if (solution[o] != 0) used = used or (1 shl (solution[o] - 1))
                }
                for (d in weightedOrder(rng)) {
                    if ((used and (1 shl (d - 1))) != 0) continue
                    solution[c] = d
                    if (rec(k + 1)) return true
                    solution[c] = 0
                }
                return false
            }
            return rec(0)
        }

        for (iter in 0 until REPAIR_TRIES) {
            val sols = solutions(n, withClues(n, white, solution), 2)
            if (sols.size == 1) {
                lastRepairs = iter
                return true
            }
            if (sols.isEmpty()) return false
            val diff = (0 until n * n).filter { sols[0][it] != sols[1][it] }
            if (diff.isEmpty()) return false
            var best: IntArray? = null
            var bestScore = Double.MAX_VALUE
            val backup = solution.copyOf()
            repeat(REPAIR_SAMPLES) {
                System.arraycopy(backup, 0, solution, 0, solution.size)
                if (!refill(diff)) return@repeat
                val score = tightness(diff)
                if (score < bestScore) {
                    bestScore = score
                    best = solution.copyOf()
                }
            }
            val chosen = best ?: return false
            System.arraycopy(chosen, 0, solution, 0, solution.size)
        }
        return false
    }

    /**
     * Düzen: ilk satır ve sütun kara (ipucu kenarı); iç hücrelere 180° simetrik
     * rastgele kara hücreler. Onarım: tek hücrelik koşunun hücresi ve 9'dan
     * uzun koşunun ortası (aynasıyla) karartılır. Çok seyrekleşirse null.
     * 2×2 beyaz bloklara izin verilir; belirsizlikleri doldurma aşamasında
     * ([breakBlocks]) rakam seçimiyle kırılır.
     */
    internal fun layout(n: Int, ratio: Float, rng: Random): BooleanArray? {
        val white = BooleanArray(n * n) { it / n > 0 && it % n > 0 }
        val interior = (n - 1) * (n - 1)
        val target = (interior * ratio).toInt()
        var placed = 0
        var guard = 0
        while (placed < target && guard++ < 1000) {
            val r = 1 + rng.nextInt(n - 1)
            val c = 1 + rng.nextInt(n - 1)
            val i = r * n + c
            if (!white[i]) continue
            white[i] = false
            placed++
            val j = (n - r) * n + (n - c)
            if (white[j]) {
                white[j] = false
                placed++
            }
        }
        repeat(80) {
            val bad = violations(n, white)
            if (bad.isEmpty()) {
                return if (white.count { it } >= interior * 0.40f) white else null
            }
            for (i in bad) {
                white[i] = false
                val r = i / n
                val c = i % n
                white[(n - r) * n + (n - c)] = false
            }
        }
        return null
    }

    /** Kurala aykırı beyaz hücreler: tek hücrelik koşu; 9'dan uzun koşunun ortası. */
    internal fun violations(n: Int, white: BooleanArray): List<Int> {
        val bad = LinkedHashSet<Int>()
        for (r in 0 until n) {
            var c = 0
            while (c < n) {
                if (!white[r * n + c]) {
                    c++
                    continue
                }
                val s = c
                while (c < n && white[r * n + c]) c++
                val len = c - s
                if (len < MIN_RUN) bad += r * n + s else if (len > MAX_RUN) bad += r * n + s + len / 2
            }
        }
        for (c in 0 until n) {
            var r = 0
            while (r < n) {
                if (!white[r * n + c]) {
                    r++
                    continue
                }
                val s = r
                while (r < n && white[r * n + c]) r++
                val len = r - s
                if (len < MIN_RUN) bad += s * n + c else if (len > MAX_RUN) bad += (s + len / 2) * n + c
            }
        }
        return bad.toList()
    }

    /** Beyaz hücreleri, koşu içinde yinelenmeyen rastgele rakamlarla doldurur. */
    internal fun fill(n: Int, white: BooleanArray, rng: Random): IntArray? {
        val plain = List(n * n) { Cell(white[it]) }
        val runs = runs(n, plain)
        val acrossOf = IntArray(n * n) { -1 }
        val downOf = IntArray(n * n) { -1 }
        runs.forEachIndexed { i, run ->
            for (c in run.cells) if (run.horizontal) acrossOf[c] = i else downOf[c] = i
        }
        val whites = (0 until n * n).filter { white[it] }
        val grid = IntArray(n * n)
        var nodes = 0

        fun used(i: Int): Int {
            var m = 0
            for (r in intArrayOf(acrossOf[i], downOf[i])) {
                if (r < 0) continue
                for (c in runs[r].cells) if (grid[c] != 0) m = m or (1 shl (grid[c] - 1))
            }
            return m
        }

        fun rec(k: Int): Boolean {
            if (k == whites.size) return true
            if (++nodes > FILL_NODE_BUDGET) return false
            val i = whites[k]
            val u = used(i)
            for (d in weightedOrder(rng)) {
                if ((u and (1 shl (d - 1))) != 0) continue
                grid[i] = d
                if (rec(k + 1)) return true
                grid[i] = 0
            }
            return false
        }
        return if (rec(0)) grid else null
    }

    /** Ağırlıklı, tekrarsız rastgele rakam sırası. */
    private fun weightedOrder(rng: Random): IntArray {
        val remaining = (1..9).toMutableList()
        val out = IntArray(9)
        for (k in 0 until 9) {
            var total = 0.0
            for (d in remaining) total += DIGIT_WEIGHTS[d - 1]
            var x = rng.nextDouble() * total
            var pick = remaining.last()
            for (d in remaining) {
                x -= DIGIT_WEIGHTS[d - 1]
                if (x <= 0.0) {
                    pick = d
                    break
                }
            }
            out[k] = pick
            remaining.remove(pick)
        }
        return out
    }

    /** Kara hücrelere sağındaki ve altındaki koşuların toplamını yazar. */
    internal fun withClues(n: Int, white: BooleanArray, solution: IntArray): List<Cell> {
        val across = IntArray(n * n)
        val down = IntArray(n * n)
        for (r in 0 until n) {
            for (c in 0 until n) {
                val i = r * n + c
                if (white[i]) continue
                var s = 0
                var cc = c + 1
                while (cc < n && white[r * n + cc]) {
                    s += solution[r * n + cc]
                    cc++
                }
                if (cc > c + 1) across[i] = s
                s = 0
                var rr = r + 1
                while (rr < n && white[rr * n + c]) {
                    s += solution[rr * n + c]
                    rr++
                }
                if (rr > r + 1) down[i] = s
            }
        }
        return List(n * n) { Cell(white[it], across[it], down[it]) }
    }
}
