package com.za.games.gecit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GecitGenTest {

    private val w = GecitGen.WIDTH

    private fun lanes(seed: Long, rows: Int): List<Lane> {
        val gen = GecitGen(seed)
        val list = ArrayList<Lane>()
        repeat(rows) { list.add(gen.next(list)) }
        return list
    }

    @Test
    fun firstRowsAreOpenGrass() {
        for (seed in 1L..6L) {
            val first = lanes(seed, 3)
            assertTrue(first.all { it.kind == LaneKind.GRASS })
            assertTrue(first[0].trees.none { it })
            assertTrue(!first[1].trees[GecitGen.START_COL] && !first[2].trees[GecitGen.START_COL])
        }
    }

    @Test
    fun hazardsAppearAfterTheirRows() {
        for (seed in 1L..6L) {
            val all = lanes(seed, 400)
            for (lane in all) {
                if (lane.kind == LaneKind.RIVER) assertTrue(lane.row >= GecitGen.RIVER_FROM)
                if (lane.kind == LaneKind.RAIL) assertTrue(lane.row >= GecitGen.RAIL_FROM)
            }
            assertEquals(LaneKind.entries.toSet(), all.map { it.kind }.toSet())
        }
    }

    /** Çim ağaçları geçişi asla kapatmaz: bağımsız bir erişilebilirlik hesabı üreteçle uyuşur. */
    @Test
    fun grassStaysReachableAllTheWay() {
        for (seed in 1L..8L) {
            var prev = BooleanArray(w) { true }
            for (lane in lanes(seed, 600)) {
                val reach = BooleanArray(w)
                if (lane.kind == LaneKind.GRASS) {
                    var c = 0
                    while (c < w) {
                        if (lane.trees[c]) {
                            c++
                            continue
                        }
                        var e = c
                        while (e + 1 < w && !lane.trees[e + 1]) e++
                        if ((c..e).any { prev[it] }) for (k in c..e) reach[k] = true
                        c = e + 1
                    }
                } else {
                    reach.fill(true)
                }
                assertTrue("tohum $seed satır ${lane.row} kapalı", reach.any { it })
                assertTrue(reach.contentEquals(lane.reach))
                prev = reach
            }
        }
    }

    @Test
    fun moversKeepGapsAndCoverage() {
        for (seed in 1L..6L) {
            for (lane in lanes(seed, 500)) {
                val where = "tohum $seed satır ${lane.row} ${lane.kind}"
                when (lane.kind) {
                    LaneKind.ROAD, LaneKind.RIVER -> {
                        val ms = lane.movers.sortedBy { it.start }
                        val dump = ms.joinToString { "${it.start}+${it.len}" }
                        assertTrue("$where az nesne: $dump", ms.size >= 2)
                        for (i in 0 until ms.size - 1) {
                            val gap = ms[i + 1].start - (ms[i].start + ms[i].len)
                            assertTrue("$where dar aralık $gap: $dump", gap >= 1.99f) // kayan nokta toplamı
                        }
                        val last = ms.last()
                        val wrap = Lane.TRACK - (last.start + last.len) + ms.first().start
                        assertTrue("$where dar sarmal aralık $wrap: $dump", wrap >= 1.99f)
                        if (lane.kind == LaneKind.ROAD) {
                            assertTrue("$where araç boyu: $dump", ms.all { it.len in 1..2 })
                        } else {
                            assertTrue("$where kütük boyu: $dump", ms.all { it.len in 2..4 })
                            val coverage = ms.sumOf { it.len } / Lane.TRACK
                            assertTrue("$where örtü $coverage: $dump", coverage >= 0.3f)
                        }
                        assertTrue("$where hız", lane.speed > 0f)
                    }
                    LaneKind.RAIL -> {
                        assertTrue("$where dönem ${lane.period}", lane.period >= Lane.WARNING_TIME + Lane.TRAIN_TIME + 1f)
                        assertTrue("$where sayaç ${lane.timer}/${lane.period}", lane.timer < lane.period - Lane.TRAIN_TIME - Lane.WARNING_TIME)
                        assertEquals(where, RailPhase.IDLE, lane.railPhase)
                    }
                    LaneKind.GRASS -> assertTrue(where, lane.movers.isEmpty())
                }
            }
        }
    }

    @Test
    fun difficultyRamps() {
        val all = (1L..6L).flatMap { lanes(it, 400) }
        val early = all.filter { it.kind == LaneKind.ROAD && it.row < 60 }.map { it.speed }.average()
        val late = all.filter { it.kind == LaneKind.ROAD && it.row >= 250 }.map { it.speed }.average()
        assertTrue("erken $early geç $late", late > early + 1.5)
        val earlyHazard = all.filter { it.row in 3..60 }.count { it.kind != LaneKind.GRASS } / all.count { it.row in 3..60 }.toFloat()
        val lateHazard = all.filter { it.row >= 250 }.count { it.kind != LaneKind.GRASS } / all.count { it.row >= 250 }.toFloat()
        assertTrue(lateHazard > earlyHazard)
    }

    @Test
    fun sameSeedSameLanes() {
        val a = lanes(42L, 300)
        val b = lanes(42L, 300)
        for (i in a.indices) {
            assertEquals(a[i].kind, b[i].kind)
            assertEquals(a[i].dir, b[i].dir)
            assertEquals(a[i].speed, b[i].speed, 0f)
            assertEquals(a[i].period, b[i].period, 0f)
            assertTrue(a[i].trees.contentEquals(b[i].trees))
            assertEquals(a[i].movers.map { Triple(it.start, it.len, it.style) }, b[i].movers.map { Triple(it.start, it.len, it.style) })
        }
    }
}
