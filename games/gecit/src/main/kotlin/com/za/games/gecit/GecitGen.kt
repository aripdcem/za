package com.za.games.gecit

import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Şerit üreteci: tohumdan deterministik, sıralı. Tehlike kümeleri (yol, ray,
 * nehir) arasına çim girer; çimdeki ağaçlar her zaman geçilebilir bırakılır:
 * yeni çim satırının boş sütunları, bir önceki satırdan ulaşılabilen sütunlara
 * bağlanmıyorsa merkeze en yakın uygun sütundaki ağaç kaldırılır.
 */
class GecitGen(private val seed: Long) {

    private var batchKind = LaneKind.GRASS
    private var batchLeft = 0

    fun next(prev: List<Lane>): Lane {
        val row = prev.size
        val rng = Random(mix(seed, row, 1))
        val d = difficulty(row)
        val kind = if (row < 3) {
            LaneKind.GRASS
        } else {
            if (batchLeft == 0) chooseBatch(rng, row, d)
            batchLeft--
            batchKind
        }
        return when (kind) {
            LaneKind.GRASS -> grass(row, rng, d, prev.lastOrNull())
            LaneKind.ROAD -> road(row, rng, d)
            LaneKind.RIVER -> river(row, rng, d)
            LaneKind.RAIL -> rail(row, rng, d)
        }
    }

    private fun chooseBatch(rng: Random, row: Int, d: Float) {
        val afterHazard = batchKind != LaneKind.GRASS
        if (afterHazard && rng.nextFloat() < 0.85f - 0.35f * d) {
            batchKind = LaneKind.GRASS
            batchLeft = if (rng.nextFloat() < 0.35f) 2 else 1
            return
        }
        val pRail = if (row >= RAIL_FROM) 0.2f + 0.05f * d else 0f
        val pRiver = if (row >= RIVER_FROM) 0.28f else 0f
        val roll = rng.nextFloat()
        batchKind = when {
            roll < pRail -> LaneKind.RAIL
            roll < pRail + pRiver -> LaneKind.RIVER
            else -> LaneKind.ROAD
        }
        batchLeft = when (batchKind) {
            LaneKind.ROAD -> 1 + rng.nextInt(3 + (2 * d).toInt())
            LaneKind.RIVER -> 1 + rng.nextInt(2 + (2 * d).toInt())
            LaneKind.RAIL -> 1 + rng.nextInt(2)
            LaneKind.GRASS -> 1
        }
    }

    private fun grass(row: Int, rng: Random, d: Float, prev: Lane?): Lane {
        val trees = BooleanArray(WIDTH)
        if (row >= 1) {
            val p = if (row < 3) 0.12f else 0.18f + 0.14f * d
            for (c in 0 until WIDTH) if (rng.nextFloat() < p) trees[c] = true
            if (row < 3) trees[START_COL] = false
        }
        val prevReach = prev?.reach ?: BooleanArray(WIDTH) { true }
        var reach = reachable(trees, prevReach)
        if (reach.none { it }) {
            val c = (0 until WIDTH).filter { prevReach[it] }.minByOrNull { abs(it - START_COL) } ?: START_COL
            trees[c] = false
            reach = reachable(trees, prevReach)
        }
        return Lane(row, LaneKind.GRASS, trees = trees).also { it.reach = reach }
    }

    /** Boş sütunların yatay bileşenlerinden, önceki satırdan ulaşılabilene bağlı olanlar. */
    private fun reachable(trees: BooleanArray, prevReach: BooleanArray): BooleanArray {
        val reach = BooleanArray(WIDTH)
        var c = 0
        while (c < WIDTH) {
            if (trees[c]) {
                c++
                continue
            }
            var e = c
            while (e + 1 < WIDTH && !trees[e + 1]) e++
            if ((c..e).any { prevReach[it] }) for (k in c..e) reach[k] = true
            c = e + 1
        }
        return reach
    }

    private fun road(row: Int, rng: Random, d: Float): Lane {
        val dir = if (rng.nextBoolean()) 1 else -1
        val speed = 2.2f + 3.2f * d + rng.nextFloat()
        val count = 2 + rng.nextInt(2 + (2 * d).toInt())
        val minGap = if (d < 0.5f) 3 else 2
        val movers = layout(rng, count, minGap, styles = 6) { if (rng.nextFloat() < 0.3f) 2 else 1 }
        return Lane(row, LaneKind.ROAD, dir, speed, movers)
    }

    private fun river(row: Int, rng: Random, d: Float): Lane {
        val dir = if (rng.nextBoolean()) 1 else -1
        val speed = 1.3f + 1.9f * d + rng.nextFloat() * 0.6f
        return Lane(row, LaneKind.RIVER, dir, speed, riverLayout(rng, d))
    }

    /**
     * Kütükler parkura eşit aralıkla (±0,3 hücre) dağıtılır; toplam uzunluk
     * parkurun en az üçte biri, aralıklar en az 2 hücre kalır.
     */
    private fun riverLayout(rng: Random, d: Float): List<Mover> {
        val base = if (d < 0.45f) 3 else 2
        val count = if (base == 3) 3 else 4
        val lens = IntArray(count) { base + rng.nextInt(2) }
        val gap = (Lane.TRACK - lens.sum()) / count
        var pos = rng.nextFloat() * gap
        val result = ArrayList<Mover>()
        for (i in 0 until count) {
            result += Mover(pos, lens[i], rng.nextInt(3))
            pos += lens[i] + gap + (rng.nextFloat() - 0.5f) * 0.6f
        }
        return result
    }

    private fun rail(row: Int, rng: Random, d: Float): Lane {
        val dir = if (rng.nextBoolean()) 1 else -1
        val period = 5.5f - 2.3f * d + rng.nextFloat() * 1.5f
        val initial = rng.nextFloat() * (period - Lane.TRAIN_TIME - Lane.WARNING_TIME)
        return Lane(row, LaneKind.RAIL, dir, 0f, period = period, initialTimer = initial)
    }

    /** Parkuru dolaşarak yerleştirir; nesneler arasında (sarmal dahil) en az [minGap] boşluk. */
    private fun layout(rng: Random, count: Int, minGap: Int, styles: Int, len: () -> Int): List<Mover> {
        val result = ArrayList<Mover>()
        var pos = rng.nextFloat() * 4f
        for (i in 0 until count) {
            val l = len()
            if (pos + l > Lane.TRACK - minGap) break
            result += Mover(pos, l, rng.nextInt(styles))
            pos += l + minGap + rng.nextInt(3)
        }
        return result
    }

    companion object {
        const val WIDTH = 9
        const val START_COL = 4
        const val RIVER_FROM = 6
        const val RAIL_FROM = 14

        /** 0 → 1 arası zorluk; 160. şeritte tavan. */
        fun difficulty(row: Int): Float = min(1f, row / 160f)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }
}
