package com.za.games.gecit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GecitWorldTest {

    private val w = GecitGen.WIDTH

    /** Satır sırasıyla verilen şeritler; ötesi ağaçsız çim. */
    private fun factory(vararg lanes: (Int) -> Lane): (List<Lane>) -> Lane = { prev ->
        val r = prev.size
        if (r < lanes.size) lanes[r](r) else Lane(r, LaneKind.GRASS)
    }

    private fun grass(vararg treeCols: Int): (Int) -> Lane =
        { r -> Lane(r, LaneKind.GRASS, trees = BooleanArray(w) { it in treeCols }) }

    /** Araçlar ekran hücresi (sol kenar) ve uzunlukla verilir. */
    private fun road(dir: Int, speed: Float, vararg cars: Pair<Float, Int>): (Int) -> Lane =
        { r -> Lane(r, LaneKind.ROAD, dir, speed, cars.map { Mover(it.first + Lane.PAD, it.second, 0) }) }

    private fun river(dir: Int, speed: Float, vararg logs: Pair<Float, Int>): (Int) -> Lane =
        { r -> Lane(r, LaneKind.RIVER, dir, speed, logs.map { Mover(it.first + Lane.PAD, it.second, 0) }) }

    private fun rail(period: Float, initialTimer: Float, dir: Int = 1): (Int) -> Lane =
        { r -> Lane(r, LaneKind.RAIL, dir, 0f, period = period, initialTimer = initialTimer) }

    private fun world(vararg lanes: (Int) -> Lane) = GecitWorld(1L, factory(*lanes))

    /** Hamleyi verir ve zıplamanın bitmesi için bekler. */
    private fun hop(world: GecitWorld, move: Move): List<GecitEvent> {
        val out = ArrayList<GecitEvent>(world.step(move))
        repeat(8) { out += world.step(null) }
        return out
    }

    private fun run(world: GecitWorld, frames: Int): List<GecitEvent> {
        val out = ArrayList<GecitEvent>()
        repeat(frames) { out += world.step(null) }
        return out
    }

    @Test
    fun startsAtCenterOfRowZero() {
        val world = GecitWorld(7L)
        assertEquals(0, world.player.row)
        assertEquals(GecitGen.START_COL.toFloat(), world.player.x, 0f)
        assertEquals(GecitStatus.RUNNING, world.status)
        assertEquals(0L, world.score)
        assertEquals(-4f, world.camera, 0f)
        assertFalse(world.started)
        // İlk hamleden önce ne kamera ilerler ne kartal gelir.
        run(world, 600)
        assertEquals(GecitStatus.RUNNING, world.status)
        assertEquals(-4f, world.camera, 0f)
    }

    @Test
    fun forwardHopScoresAndBuffersNextHop() {
        val world = world()
        val events = world.step(Move.FORWARD)
        assertTrue(events.contains(GecitEvent.Hop))
        assertEquals(1, world.player.row)
        assertTrue(world.player.hopT < 1f)
        assertTrue(world.started)
        world.step(Move.FORWARD) // tamponlanır
        run(world, 2)
        assertEquals(1, world.player.row)
        run(world, 6)
        assertEquals(2, world.player.row)
        assertEquals(2L, world.score)
        // Geri gitmek skoru düşürmez.
        hop(world, Move.BACK)
        assertEquals(1, world.player.row)
        assertEquals(2L, world.score)
    }

    @Test
    fun bumpsOnTreesEdgesAndStart() {
        val world = world(grass(), grass(4))
        assertTrue(world.step(Move.BACK).contains(GecitEvent.Bump))
        assertTrue(world.step(Move.FORWARD).contains(GecitEvent.Bump))
        assertEquals(0, world.player.row)
        repeat(4) { hop(world, Move.LEFT) }
        assertEquals(0f, world.player.x, 0f)
        assertTrue(world.step(Move.LEFT).contains(GecitEvent.Bump))
        repeat(8) { hop(world, Move.RIGHT) }
        assertEquals((w - 1).toFloat(), world.player.x, 0f)
        assertTrue(world.step(Move.RIGHT).contains(GecitEvent.Bump))
        assertEquals(GecitStatus.RUNNING, world.status)
    }

    @Test
    fun carHitsPlayer() {
        val world = world(grass(), road(1, 4f, 2f to 1))
        hop(world, Move.FORWARD)
        val events = run(world, 40)
        assertEquals(DeathCause.CAR, world.cause)
        assertTrue(events.contains(GecitEvent.Over(DeathCause.CAR)))
        assertTrue(world.step(Move.FORWARD).isEmpty())
    }

    @Test
    fun carMovingAwayIsSafe() {
        val world = world(grass(), road(-1, 4f, 2f to 1))
        hop(world, Move.FORWARD)
        run(world, 120)
        assertEquals(GecitStatus.RUNNING, world.status)
    }

    @Test
    fun logCarriesPlayerThenOffTheEdge() {
        val world = world(grass(), river(1, 2f, 3.5f to 2))
        hop(world, Move.FORWARD)
        assertEquals(GecitStatus.RUNNING, world.status)
        run(world, 60)
        assertTrue("kütük taşımalı: x=${world.player.x}", world.player.x > 4.5f)
        assertEquals(GecitStatus.RUNNING, world.status)
        run(world, 200)
        assertEquals(DeathCause.CARRIED, world.cause)
    }

    @Test
    fun landingOnWaterDrowns() {
        val world = world(grass(), river(1, 1f, 0f to 1))
        val events = hop(world, Move.FORWARD)
        assertEquals(DeathCause.WATER, world.cause)
        assertTrue(events.contains(GecitEvent.Over(DeathCause.WATER)))
    }

    @Test
    fun hoppingOffALogSnapsToColumn() {
        val world = world(grass(), river(1, 2f, 3.5f to 2), grass())
        hop(world, Move.FORWARD)
        run(world, 30)
        assertTrue(world.player.x != 4f)
        hop(world, Move.FORWARD)
        assertEquals(2, world.player.row)
        assertEquals(world.player.x, world.player.x.toInt().toFloat(), 0f)
    }

    @Test
    fun trainWarnsThenHits() {
        val start = 4f - Lane.TRAIN_TIME - Lane.WARNING_TIME - 0.03f
        val world = world(grass(), rail(4f, start))
        val events = run(world, 5)
        assertTrue(events.contains(GecitEvent.Warning(1)))
        val later = run(world, 80)
        assertTrue(later.contains(GecitEvent.Train(1)))
        val hit = world(grass(), rail(4f, 4f - Lane.TRAIN_TIME))
        hop(hit, Move.FORWARD)
        run(hit, 60)
        assertEquals(DeathCause.TRAIN, hit.cause)
    }

    @Test
    fun eagleTakesIdlePlayer() {
        val world = world()
        hop(world, Move.FORWARD)
        run(world, 150)
        assertEquals(GecitStatus.RUNNING, world.status)
        assertTrue(world.idle > GecitWorld.IDLE_SIDE_RESET)
        // Yana zıplamak sayacı yarıya indirir ama sıfırlamaz.
        hop(world, Move.LEFT)
        assertTrue(world.idle <= GecitWorld.IDLE_SIDE_RESET + 0.2f)
        assertTrue(world.idle > 1f)
        run(world, 60)
        assertEquals(GecitStatus.RUNNING, world.status)
        run(world, 80)
        assertEquals(DeathCause.EAGLE, world.cause)
    }

    @Test
    fun forwardHopResetsEagleAndSideHopsCannotStallForever() {
        val world = world()
        hop(world, Move.FORWARD)
        run(world, 150)
        hop(world, Move.FORWARD)
        assertTrue(world.idle < 0.2f)
        var frames = 0
        while (world.status == GecitStatus.RUNNING && frames < 1200) {
            world.step(if (frames % 60 == 0) (if ((frames / 60) % 2 == 0) Move.RIGHT else Move.LEFT) else null)
            frames++
        }
        // Yana zıplayarak oyalanan ya kamerada kalır ya kartala gider; ikisi de ölümdür.
        assertEquals(GecitStatus.OVER, world.status)
        assertTrue(world.cause == DeathCause.CAMERA || world.cause == DeathCause.EAGLE)
        assertTrue("$frames", frames in 600..1000)
    }

    @Test
    fun eagleWarnsOncePerWait() {
        val world = world()
        hop(world, Move.FORWARD)
        val events = run(world, 200)
        assertEquals(1, events.count { it == GecitEvent.EagleNear })
        assertEquals(GecitStatus.RUNNING, world.status)
        hop(world, Move.FORWARD)
        assertEquals(1, run(world, 200).count { it == GecitEvent.EagleNear })
    }

    @Test
    fun gemsAreCollectedOnLandingAndCountTowardScore() {
        val world = world({ r -> Lane(r, LaneKind.GRASS) }, { r -> Lane(r, LaneKind.GRASS, gemCol = 5) })
        hop(world, Move.FORWARD)
        assertEquals(0, world.gems)
        val events = hop(world, Move.RIGHT)
        assertEquals(1, world.gems)
        assertTrue(events.any { it is GecitEvent.Gem && it.col == 5 && it.total == 1 })
        assertEquals(-1, world.lane(1)!!.gemCol)
        assertEquals(2L, world.score)
        hop(world, Move.LEFT)
        hop(world, Move.RIGHT)
        assertEquals(1, world.gems)
    }

    @Test
    fun cameraCatchesPlayerWhoFallsBehind() {
        val world = world()
        repeat(30) { hop(world, Move.FORWARD) }
        assertEquals(30, world.maxRow)
        assertTrue(world.camera >= 26f)
        repeat(4) { hop(world, Move.BACK) }
        assertEquals(26, world.player.row)
        assertTrue(world.step(Move.BACK).contains(GecitEvent.Bump))
        run(world, 200)
        assertEquals(DeathCause.CAMERA, world.cause)
    }

    @Test
    fun milestonesEveryTwentyFiveLanes() {
        val world = world()
        val events = ArrayList<GecitEvent>()
        repeat(50) { events += hop(world, Move.FORWARD) }
        assertEquals(listOf(25, 50), events.filterIsInstance<GecitEvent.Milestone>().map { it.row })
        assertEquals(50L, world.score)
    }

    @Test
    fun dailySeedIsStable() {
        assertEquals(GecitWorld.dailySeed(20_000L), GecitWorld.dailySeed(20_000L))
        assertTrue(GecitWorld.dailySeed(20_000L) != GecitWorld.dailySeed(20_001L))
    }

    private fun scripted(frame: Int): Move? = when {
        frame % 9 == 0 -> Move.FORWARD
        frame % 40 == 5 -> Move.LEFT
        frame % 70 == 6 -> Move.RIGHT
        else -> null
    }

    @Test
    fun sameSeedSameMovesSameRun() {
        val a = GecitWorld(GecitWorld.dailySeed(20_300L))
        val b = GecitWorld(GecitWorld.dailySeed(20_300L))
        var countA = 0
        var countB = 0
        for (frame in 0 until 3000) {
            countA += a.step(scripted(frame)).size
            countB += b.step(scripted(frame)).size
        }
        assertEquals(a.hud(), b.hud())
        assertEquals(a.player.x, b.player.x, 0f)
        assertEquals(a.camera, b.camera, 0f)
        assertEquals(countA, countB)
        assertTrue(a.maxRow > 0)
    }

    @Test
    fun randomRunsSurvive() {
        for (seed in 1L..5L) {
            val world = GecitWorld(seed)
            val rng = Random(seed * 17)
            var frames = 0
            var next = 0
            while (world.status == GecitStatus.RUNNING && frames < 6000) {
                var move: Move? = null
                if (frames >= next) {
                    move = when (rng.nextInt(10)) {
                        in 0..5 -> Move.FORWARD
                        6, 7 -> Move.LEFT
                        8 -> Move.RIGHT
                        else -> Move.BACK
                    }
                    next = frames + 6 + rng.nextInt(10)
                }
                world.step(move)
                frames++
            }
            assertTrue(world.maxRow >= 1)
            if (world.status == GecitStatus.OVER) assertTrue(world.cause != null) else assertNull(world.cause)
            assertTrue(world.player.row >= world.camera - 1f)
            assertEquals(world.score, world.maxRow.toLong())
        }
    }
}
