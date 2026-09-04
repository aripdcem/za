package com.za.games.balkon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

class BalkonWorldTest {

    /** Rüzgârsız ve kendiliğinden hedef doğurmayan dünya: testler kendi hedeflerini koyar. */
    private fun world(seed: Long = 1L): BalkonWorld = BalkonWorld(seed, windEnabled = false, spawning = false)

    private fun run(w: BalkonWorld, seconds: Float): List<BalkonEvent> {
        val out = ArrayList<BalkonEvent>()
        repeat((seconds / BalkonWorld.STEP).roundToInt()) { out += w.step() }
        return out
    }

    private fun still(w: BalkonWorld, kind: TargetKind, x: Float, lane: Lane = Lane.NEAR_WALK): Target =
        w.spawnTarget(kind, lane, x, 1, 0f)

    @Test
    fun missLandsAndResetsCombo() {
        val w = world()
        assertTrue(w.throwAt(0.5f, 0.3f, mega = false))
        val events = run(w, 1.0f)
        assertTrue(events.any { it is BalkonEvent.Throw })
        val land = events.filterIsInstance<BalkonEvent.Land>().single()
        assertEquals(0, land.hits)
        assertTrue(events.contains(BalkonEvent.Miss))
        assertEquals(0, w.combo)
        assertTrue(w.shots.isEmpty())
    }

    @Test
    fun hitScoresAndCountsCombo() {
        val w = world()
        still(w, TargetKind.PIGEON, 0.5f)
        w.throwAt(0.5f, Lane.NEAR_WALK.depth, mega = false)
        val events = run(w, 0.8f)
        val hit = events.filterIsInstance<BalkonEvent.Hit>().single()
        assertEquals(TargetKind.PIGEON, hit.kind)
        assertEquals(15, hit.points)
        assertEquals(15L, w.score)
        assertEquals(1, w.hits)
        assertEquals(1, w.combo)
        assertFalse(events.contains(BalkonEvent.Miss))
        // Vurulan hedef tepki süresi sonunda silinir.
        run(w, BalkonWorld.HIT_ANIM + 0.1f)
        assertTrue(w.targets.none { it.kind == TargetKind.PIGEON })
    }

    @Test
    fun throwRulesRespected() {
        val w = world()
        assertFalse(w.throwAt(0.5f, 0.1f, mega = false)) // duvar dibi
        assertFalse(w.throwAt(0.5f, 0.5f, mega = true)) // şarj yok
        repeat(BalkonWorld.MAX_SHOTS) { assertTrue(w.throwAt(0.5f, 0.5f, mega = false)) }
        assertFalse(w.throwAt(0.5f, 0.5f, mega = false)) // havada en çok 3
        run(w, 1.0f)
        assertTrue(w.throwAt(0.5f, 0.5f, mega = false))
        // Nişan x'i balkondaki oyuncuyu kaydırır.
        w.throwAt(0.9f, 0.5f, mega = false)
        run(w, 0.5f)
        assertTrue(w.avatarX > 0.8f)
    }

    @Test
    fun forbiddenTargetPenalizesAndStuns() {
        val w = world()
        still(w, TargetKind.PIGEON, 0.3f)
        still(w, TargetKind.JANITOR, 0.7f)
        w.throwAt(0.3f, Lane.NEAR_WALK.depth, mega = false)
        run(w, 0.8f)
        assertEquals(15L, w.score)
        w.throwAt(0.7f, Lane.NEAR_WALK.depth, mega = false)
        val events = run(w, 0.8f)
        val bad = events.filterIsInstance<BalkonEvent.Forbidden>().single()
        assertEquals(TargetKind.JANITOR, bad.kind)
        assertEquals(0L, w.score) // 15 - 50, sıfırın altına inmez
        assertEquals(0, w.combo)
        assertEquals(1, w.hits) // yasak hedef isabet sayılmaz
        assertTrue(w.hud().stunned)
        assertFalse(w.throwAt(0.5f, 0.5f, mega = false))
        run(w, BalkonWorld.STUN_TIME + 0.1f)
        assertFalse(w.hud().stunned)
        assertTrue(w.throwAt(0.5f, 0.5f, mega = false))
    }

    @Test
    fun comboFillsMegaAndMegaHitsWide() {
        val w = world()
        repeat(BalkonWorld.COMBO_FOR_MEGA) { i ->
            still(w, TargetKind.BALL, 0.2f + i * 0.15f, Lane.FAR_WALK)
        }
        var events = ArrayList<BalkonEvent>()
        repeat(BalkonWorld.COMBO_FOR_MEGA) { i ->
            assertTrue(w.throwAt(0.2f + i * 0.15f, Lane.FAR_WALK.depth, mega = false))
            events += run(w, 1.1f)
        }
        assertTrue(events.contains(BalkonEvent.MegaReady))
        assertEquals(1, w.charges)
        assertEquals(0, w.combo)
        // Mega: geniş alan, iki kat puan (7 isabet: seviye henüz bitmez).
        still(w, TargetKind.PIGEON, 0.44f)
        still(w, TargetKind.PIGEON, 0.56f)
        val before = w.score
        assertTrue(w.throwAt(0.5f, Lane.NEAR_WALK.depth, mega = true))
        assertEquals(0, w.charges)
        events = ArrayList(run(w, 0.8f))
        val hits = events.filterIsInstance<BalkonEvent.Hit>()
        assertEquals(2, hits.size)
        assertTrue(hits.all { it.points == 30 })
        assertEquals(before + 60, w.score)
        assertEquals(2, events.filterIsInstance<BalkonEvent.Land>().single().hits)
        assertEquals(BalkonStatus.RUNNING, w.status)
    }

    @Test
    fun windDriftsLanding() {
        val w = world()
        w.setWind(0.1f)
        val depth = 0.5f
        val duration = BalkonWorld.flightTime(depth)
        still(w, TargetKind.BALL, 0.5f + 0.1f * duration, Lane.ROAD_A)
        w.throwAt(0.5f, depth, mega = false)
        val events = run(w, 1.0f)
        val land = events.filterIsInstance<BalkonEvent.Land>().single()
        assertTrue(abs(land.x - (0.5f + 0.1f * duration)) < 1e-4f)
        assertEquals(1, events.filterIsInstance<BalkonEvent.Hit>().size)
    }

    @Test
    fun levelClearsWithBonusThenAdvances() {
        val w = world()
        val need = BalkonWorld.required(1)
        assertEquals(8, need)
        var events = ArrayList<BalkonEvent>()
        for (i in 0 until need) {
            still(w, TargetKind.CAT, 0.1f + i * 0.1f, Lane.FAR_WALK)
            assertTrue(w.throwAt(0.1f + i * 0.1f, Lane.FAR_WALK.depth, mega = false))
            events += run(w, 1.0f)
        }
        val clear = events.filterIsInstance<BalkonEvent.LevelClear>().single()
        assertEquals(1, clear.level)
        assertTrue(clear.bonus > 0)
        assertEquals(BalkonStatus.CLEARED, w.status)
        assertEquals(need * 20L + clear.bonus, w.score)
        assertFalse(w.throwAt(0.5f, 0.5f, mega = false)) // seviye arası atış yok
        events = ArrayList(run(w, BalkonWorld.CLEAR_PAUSE + 0.1f))
        assertEquals(BalkonEvent.LevelStart(2), events.filterIsInstance<BalkonEvent.LevelStart>().single())
        assertEquals(2, w.level)
        assertEquals(0, w.hits)
        assertTrue(w.timeLeft > BalkonWorld.LEVEL_TIME - 0.5f) // yeni seviyenin süresi yeni başladı
        assertEquals(10, w.required)
    }

    @Test
    fun timeRunsOut() {
        val w = world()
        val events = run(w, BalkonWorld.LEVEL_TIME + 0.5f)
        assertEquals(BalkonStatus.OVER, w.status)
        assertEquals(1, events.count { it == BalkonEvent.Over })
        assertEquals(0, w.hud().seconds)
        assertFalse(w.throwAt(0.5f, 0.5f, mega = false))
    }

    @Test
    fun simitAddsTime() {
        val w = world()
        run(w, 10f)
        val before = w.timeLeft
        still(w, TargetKind.SIMIT, 0.5f)
        w.throwAt(0.5f, Lane.NEAR_WALK.depth, mega = false)
        val events = run(w, 0.8f)
        val bonus = events.filterIsInstance<BalkonEvent.Bonus>().single()
        assertEquals(5, bonus.seconds)
        assertTrue(w.timeLeft > before + 4f)
        assertEquals(100L, w.score)
    }

    @Test
    fun spawnsFlowAndStayBounded() {
        val w = BalkonWorld(3L)
        var maxAlive = 0
        var seenKinds = HashSet<TargetKind>()
        repeat((40f / BalkonWorld.STEP).toInt()) {
            w.step()
            maxAlive = maxOf(maxAlive, w.targets.count { it.alive })
            w.targets.forEach { seenKinds += it.kind }
        }
        assertTrue(maxAlive in 3..8)
        assertTrue(seenKinds.size >= 4)
        // Birinci seviyede yasak hedef çıkmaz.
        assertFalse(seenKinds.contains(TargetKind.JANITOR))
        assertFalse(seenKinds.contains(TargetKind.NEIGHBOR))
        assertTrue(w.targets.all { it.x > -0.25f && it.x < 1.25f })
    }

    @Test
    fun deterministicForSameSeedAndThrows() {
        val a = BalkonWorld(11L)
        val b = BalkonWorld(11L)
        repeat(20 * 60) { frame ->
            if (frame % 90 == 0) {
                val y = 0.3f + (frame % 3) * 0.2f
                a.throwAt(0.4f, y, mega = false)
                b.throwAt(0.4f, y, mega = false)
            }
            a.step()
            b.step()
        }
        assertEquals(a.hud(), b.hud())
        assertEquals(a.targets.map { Triple(it.kind, it.x, it.y) }, b.targets.map { Triple(it.kind, it.x, it.y) })
        assertEquals(a.wind, b.wind, 0f)
    }
}
