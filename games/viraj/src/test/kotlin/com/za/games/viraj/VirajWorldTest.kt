package com.za.games.viraj

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VirajWorldTest {

    private val seg = VirajWorld.SEGMENT_LENGTH
    private val max = VirajWorld.MAX_SPEED

    private fun run(world: VirajWorld, frames: Int, steer: Int = 0, brake: Boolean = false): List<VirajEvent> {
        world.steer = steer
        world.brake = brake
        val out = ArrayList<VirajEvent>()
        repeat(frames) { out += world.step() }
        return out
    }

    /** Ortaya doğru direksiyon kıran basit otomatik pilot. */
    private fun autopilot(world: VirajWorld, frames: Int): List<VirajEvent> {
        val out = ArrayList<VirajEvent>()
        repeat(frames) {
            world.steer = when {
                world.playerX > 0.1f -> -1
                world.playerX < -0.1f -> 1
                else -> 0
            }
            out += world.step()
        }
        return out
    }

    private fun clean(seed: Long = 1L): VirajWorld = VirajWorld(seed).also { it.clearCarsForTest() }

    @Test
    fun sameSeedSameInputsSameRun() {
        val a = VirajWorld(11L)
        val b = VirajWorld(11L)
        val rng = Random(5)
        repeat(900) {
            val steer = when (rng.nextInt(6)) {
                0 -> -1
                1 -> 1
                else -> 0
            }
            val brake = rng.nextInt(25) == 0
            a.steer = steer
            b.steer = steer
            a.brake = brake
            b.brake = brake
            assertEquals(a.step(), b.step())
        }
        assertEquals(a.position, b.position, 0f)
        assertEquals(a.playerX, b.playerX, 0f)
        assertEquals(a.score, b.score)
        assertEquals(a.cars.map { Triple(it.z, it.x, it.speed) }, b.cars.map { Triple(it.z, it.x, it.speed) })
    }

    @Test
    fun acceleratesToMaxThenBrakes() {
        val w = clean()
        run(w, 300)
        assertEquals(max, w.speed, max * 0.01f)
        assertEquals(VirajWorld.KMH_AT_MAX, w.kmh)
        assertTrue("düz başlangıçta yolda kalmalı: ${w.playerX}", kotlin.math.abs(w.playerX) < 1f)
        run(w, 60, brake = true)
        assertEquals(0f, w.speed, 0f)
    }

    @Test
    fun offRoadSlowsDown() {
        val w = clean()
        run(w, 300)
        run(w, 130, steer = -1)
        assertTrue("yol dışında olmalı: ${w.playerX}", w.playerX < -1f)
        assertTrue("yavaşlamalı: ${w.speed}", w.speed <= max * 0.3f)
    }

    @Test
    fun checkpointExtendsTimeAndScores() {
        val w = clean()
        w.setSpeedForTest(max)
        w.jumpToSegmentForTest(VirajWorld.CHECKPOINT_EVERY - 1)
        val timeBefore = w.timeLeft
        val scoreBefore = w.score
        val events = run(w, 3)
        val checkpoint = events.filterIsInstance<VirajEvent.Checkpoint>()
        assertEquals(1, checkpoint.size)
        assertEquals(1, w.checkpoints)
        assertTrue(w.timeLeft > timeBefore + 10f)
        assertTrue(w.score >= scoreBefore + VirajWorld.CHECKPOINT_POINTS)
        assertTrue(w.timeLeft <= VirajWorld.MAX_TIME)
        assertTrue("rakipler eklenmeli", w.cars.size >= VirajWorld.MIN_CARS)
    }

    @Test
    fun timeRunsOutEndsGame() {
        val w = clean()
        val events = run(w, 60 * 41, brake = true)
        assertEquals(VirajStatus.OVER, w.status)
        assertEquals(1, events.count { it == VirajEvent.Over })
        assertEquals(0f, w.timeLeft, 0f)
        val frames = w.frames
        assertTrue(w.step().isEmpty())
        assertEquals(frames, w.frames)
    }

    @Test
    fun overtakingScoresOnce() {
        val w = clean()
        w.setSpeedForTest(max)
        w.addCarForTest(aheadSegments = 3f, x = 0.8f, speed = max * 0.3f)
        val events = run(w, 30)
        assertEquals(listOf(VirajEvent.Overtake(1)), events.filterIsInstance<VirajEvent.Overtake>())
        assertEquals(1, w.overtakes)
        assertTrue(w.score >= VirajWorld.OVERTAKE_POINTS)
        assertTrue(run(w, 60).none { it is VirajEvent.Overtake })
        assertTrue(events.none { it is VirajEvent.Crash })
    }

    @Test
    fun hittingASlowerCarSlowsAndPushesBehind() {
        val w = clean()
        w.setSpeedForTest(max)
        val car = w.addCarForTest(aheadSegments = 2f, x = 0f, speed = max * 0.3f)
        val events = run(w, 6)
        assertTrue(events.any { it is VirajEvent.Crash })
        assertTrue("hız düşmeli: ${w.speed}", w.speed <= car.speed)
        assertTrue("aracın gerisinde kalmalı", w.playerZ < car.z)
        assertEquals(0, w.overtakes)
    }

    @Test
    fun shieldAbsorbsOneCrash() {
        val w = clean()
        w.setSpeedForTest(max)
        w.grantShieldForTest()
        val car = w.addCarForTest(aheadSegments = 2f, x = 0f, speed = max * 0.3f)
        val events = run(w, 6)
        assertTrue(events.any { it == VirajEvent.ShieldUsed })
        assertTrue(events.none { it is VirajEvent.Crash })
        assertFalse(w.shield)
        assertTrue("kalkanla hız korunmalı: ${w.speed}", w.speed >= car.speed)
    }

    @Test
    fun turboStripBoostsSpeed() {
        val w = clean()
        w.setSpeedForTest(max)
        w.track.segment(w.playerSegmentIndex + 1).item = Item(ItemKind.TURBO, 0f)
        val events = run(w, 3)
        assertTrue(events.any { it == VirajEvent.Pickup(ItemKind.TURBO, null) })
        assertTrue(w.turboT > 0f)
        assertEquals(max * VirajWorld.TURBO_FACTOR, w.speed, 1f)
        run(w, (VirajWorld.TURBO_TIME * 60).toInt() + 5)
        assertEquals(0f, w.turboT, 0f)
        assertTrue(w.speed <= max)
    }

    @Test
    fun coneAndOilPunish() {
        val cone = clean()
        cone.setSpeedForTest(max)
        cone.track.segment(cone.playerSegmentIndex + 1).item = Item(ItemKind.CONE, 0f)
        val coneEvents = run(cone, 2)
        assertTrue(coneEvents.any { it == VirajEvent.Cone })
        assertTrue("koni yavaşlatmalı: ${cone.speed}", cone.speed < max * 0.75f)

        val oil = clean()
        oil.setSpeedForTest(max)
        oil.track.segment(oil.playerSegmentIndex + 1).item = Item(ItemKind.OIL, 0f)
        val oilEvents = run(oil, 2)
        assertTrue(oilEvents.any { it == VirajEvent.Slip })
        assertTrue(oil.slipT > 0f)
        run(oil, 12, steer = 1)
        assertTrue("yağda direksiyon ters: ${oil.playerX}", oil.playerX < 0f)
    }

    @Test
    fun boxGivesAGift() {
        val w = clean(3L)
        w.setSpeedForTest(max)
        val timeBefore = w.timeLeft
        w.track.segment(w.playerSegmentIndex + 1).item = Item(ItemKind.BOX, 0f)
        val events = run(w, 2)
        val pickup = events.filterIsInstance<VirajEvent.Pickup>().single()
        assertEquals(ItemKind.BOX, pickup.kind)
        assertNotNull(pickup.gift)
        when (pickup.gift) {
            BoxGift.TURBO -> assertTrue(w.turboT > 0f)
            BoxGift.SHIELD -> assertTrue(w.shield)
            BoxGift.TIME -> assertTrue(w.timeLeft > timeBefore + 4f)
            null -> Unit
        }
    }

    @Test
    fun trackIsDeterministicAndBounded() {
        val a = VirajTrack(7L)
        val b = VirajTrack(7L)
        for (i in 0 until 3000) {
            val s = a.segment(i)
            val t = b.segment(i)
            assertEquals(s.curve, t.curve, 0f)
            assertEquals(s.y, t.y, 0f)
            assertEquals(s.sprites.map { it.kind to it.x }, t.sprites.map { it.kind to it.x })
            assertEquals(s.item?.kind, t.item?.kind)
            assertTrue("viraj sınırı ${s.curve}", kotlin.math.abs(s.curve) <= 6.6f)
            assertTrue(s.y.isFinite())
            assertEquals(i > 0 && i % VirajWorld.CHECKPOINT_EVERY == 0, s.checkpoint)
            if (s.checkpoint) assertTrue(s.item == null)
            if (i < 60) assertEquals(0f, s.curve, 0f)
            for (sprite in s.sprites) assertTrue(kotlin.math.abs(sprite.x) > 1.1f)
        }
    }

    @Test
    fun carsRespawnAheadAndStayCounted() {
        val w = VirajWorld(9L)
        autopilot(w, 60 * 25)
        assertTrue(w.cars.size in VirajWorld.MIN_CARS..VirajWorld.MAX_CARS)
        for (car in w.cars) {
            assertTrue("araç çok geride", car.z >= w.playerZ - (VirajWorld.BEHIND_SEGMENTS + 2) * seg)
            assertTrue(car.x in -0.9f..0.9f)
        }
        assertTrue("puan birikmeli", w.score > 100)
    }
}
