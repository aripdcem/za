package com.za.games.kuyu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.floor

class KuyuGenTest {

    private fun longestGap(chunk: KuyuChunk, row: Int): Int {
        var best = 0
        var run = 0
        for (c in 0 until KuyuGen.WIDTH) {
            if (chunk.tile(row, c) == Tile.EMPTY) {
                run++
                best = maxOf(best, run)
            } else {
                run = 0
            }
        }
        return best
    }

    @Test
    fun sameSeedSameChunk() {
        val a = KuyuGen.chunk(42L, 5)
        val b = KuyuGen.chunk(42L, 5)
        for (r in 0 until KuyuGen.CHUNK_ROWS) {
            for (c in 0 until KuyuGen.WIDTH) assertEquals(a.tile(r, c), b.tile(r, c))
        }
        assertEquals(a.spawns, b.spawns)
    }

    @Test
    fun wallsGapsAndStartPlatform() {
        for (seed in 1L..6L) {
            for (index in 0 until 120) {
                val chunk = KuyuGen.chunk(seed, index)
                for (r in 0 until KuyuGen.CHUNK_ROWS) {
                    assertEquals(Tile.WALL, chunk.tile(r, 0))
                    assertEquals(Tile.WALL, chunk.tile(r, KuyuGen.WIDTH - 1))
                    assertTrue(
                        "tohum $seed parça $index satır $r dar",
                        longestGap(chunk, r) >= KuyuGen.MIN_GAP,
                    )
                }
            }
            val first = KuyuGen.chunk(seed, 0)
            val left = KuyuGen.leftWall(seed, KuyuGen.START_ROW)
            assertEquals(Tile.BLOCK, first.tile(KuyuGen.START_ROW, left))
            assertEquals(Tile.EMPTY, first.tile(KuyuGen.START_ROW - 1, left))
            assertTrue(longestGap(first, KuyuGen.START_ROW) >= KuyuGen.MIN_GAP)
        }
    }

    @Test
    fun spawnsSitInOpenCellsWithFloorWhereNeeded() {
        for (seed in 1L..6L) {
            for (index in 0 until 80) {
                val chunk = KuyuGen.chunk(seed, index)
                for (s in chunk.spawns) {
                    val r = s.row - index * KuyuGen.CHUNK_ROWS
                    assertTrue(r in 0 until KuyuGen.CHUNK_ROWS)
                    assertEquals(Tile.EMPTY, chunk.tile(r, s.col))
                    if (s.kind == EnemyKind.BLOB || s.kind == EnemyKind.SPIKY) {
                        assertTrue("zemin yok: $s", chunk.tile(r + 1, s.col).solid)
                    }
                    if (KuyuGen.area(index) == 0) {
                        assertTrue(s.kind == EnemyKind.BLOB || s.kind == EnemyKind.BAT)
                    }
                }
            }
        }
    }

    @Test
    fun enemyCountGrowsWithDepth() {
        assertEquals(1, KuyuGen.chunk(9L, 0).spawns.size)
        assertEquals(3, KuyuGen.chunk(9L, 2).spawns.size)
        assertEquals(7, KuyuGen.chunk(9L, 12).spawns.size)
        assertEquals(0, KuyuGen.area(7))
        assertEquals(1, KuyuGen.area(8))
        assertEquals(2, KuyuGen.area(40))
    }

    /**
     * Geçilebilirlik: başlangıçtan 40 parça aşağıya, yalnızca yürüme (yan),
     * düşme (aşağı) ve yerden 3 kare zıplama ile ulaşılabilmeli.
     */
    @Test
    fun wellIsPassableFromTop() {
        val chunks = 40
        val rows = chunks * KuyuGen.CHUNK_ROWS
        val w = KuyuGen.WIDTH
        for (seed in 1L..8L) {
            val world = KuyuWorld(seed)
            fun open(r: Int, c: Int) = r in 0 until rows && c in 0 until w && !world.tile(r, c).solid
            val startCol = floor(world.player.centerX).toInt()
            val startRow = floor(world.player.bottom - 0.01f).toInt()
            assertTrue(open(startRow, startCol))
            val seen = HashSet<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(startRow * w + startCol)
            seen.add(startRow * w + startCol)
            var reached = false
            while (queue.isNotEmpty() && !reached) {
                val code = queue.removeFirst()
                val r = code / w
                val c = code % w
                if (r == rows - 1) {
                    reached = true
                    break
                }
                val next = ArrayList<Pair<Int, Int>>()
                next += r + 1 to c
                next += r to c - 1
                next += r to c + 1
                if (!open(r + 1, c)) { // yerde: 3 kareye kadar zıplama
                    for (k in 1..3) {
                        if ((1..k).all { open(r - it, c) }) next += r - k to c
                    }
                }
                for ((nr, nc) in next) {
                    if (!open(nr, nc)) continue
                    val ncode = nr * w + nc
                    if (seen.add(ncode)) queue.add(ncode)
                }
            }
            assertTrue("tohum $seed: kuyunun dibine yol yok", reached)
        }
    }
}
