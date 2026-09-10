package com.za.games.sincap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SincapWorldTest {

    private fun run(w: SincapWorld, seconds: Float, ev: MutableList<SincapEvent>? = null) {
        repeat((seconds / SincapWorld.STEP + 0.5f).toInt()) {
            val e = w.step()
            ev?.addAll(e)
        }
    }

    /** Karga doğmaz, kedi tırmanmaz: kurallar tek tek ölçülür. */
    private fun quiet(seed: Long = 1L): SincapWorld = SincapWorld(seed).apply {
        freezeCrowsForTest()
        freezeCatForTest()
    }

    /** Pilot kararıyla [target] basamağa dek tırmanır. */
    private fun climb(w: SincapWorld, target: Int) {
        var guard = 0
        while (w.height < target && w.status == SincapStatus.RUNNING && guard++ < 60 * 600) {
            if (!w.jumping && !w.falling) {
                val s = SincapBots.decide(w)
                if (s != null) w.tap(s)
            }
            w.step()
        }
    }

    @Test
    fun sameSeedAndSameTapsReplayIdentically() {
        val a = SincapWorld(42L)
        val b = SincapWorld(42L)
        for (f in 0 until 60 * 25) {
            if (f % 30 == 0 && !a.jumping && !a.falling) {
                val want = if ((f / 30) % 2 == 0) Side.LEFT else Side.RIGHT
                val s = if (a.target(want) >= 0) want else if (want == Side.LEFT) Side.RIGHT else Side.LEFT
                a.tap(s)
                b.tap(s)
            }
            a.step()
            b.step()
            assertEquals(a.level, b.level)
            assertEquals(a.y, b.y, 0f)
            assertEquals(a.score, b.score)
            assertEquals(a.crows.size, b.crows.size)
            assertEquals(a.status, b.status)
            if (a.status != SincapStatus.RUNNING) break
        }
        assertEquals(a.cause, b.cause)
    }

    @Test
    fun everyLevelHasABranchAndASafeEscape() {
        for (seed in 1L..30L) {
            val w = SincapWorld(seed)
            w.levelAt(400)
            for (i in 0..SincapWorld.EASY_LEVELS) {
                assertEquals(BranchKind.NORMAL, w.levels[i].left)
                assertEquals(BranchKind.NORMAL, w.levels[i].right)
                assertFalse(w.levels[i].crow)
            }
            for (k in 0 until 398) {
                val lv = w.levels[k]
                assertTrue("tohum $seed basamak $k dalsız", lv.left.present || lv.right.present)
                assertTrue("tohum $seed basamak $k çıkmaz", w.escapable(k))
                if (lv.crow) {
                    assertTrue(k > SincapWorld.EASY_LEVELS + 2)
                    val prev = w.levels[k - 1]
                    assertNotEquals("kargalı basamağın altında kuru dal", BranchKind.DRY, prev.left)
                    assertNotEquals(BranchKind.DRY, prev.right)
                }
            }
            // Güvenli dallarla yukarı yol: her yönün erişilen ilk dalıyla genişletilen erişim.
            val reach = BooleanArray(400)
            reach[0] = true
            for (k in 0 until 398) {
                if (!reach[k]) continue
                for (s in Side.entries) {
                    for (j in k + 1..k + SincapWorld.REACH) {
                        val b = w.levels[j].at(s)
                        if (!b.present) continue
                        if (b.safe) reach[j] = true
                        break
                    }
                }
            }
            assertTrue("tohum $seed: 399 erişilemez", reach[398] || reach[399])
        }
    }

    @Test
    fun tapJumpsToNearestBranchOnThatSideWithinReach() {
        val w = quiet()
        w.setLevelForTest(1, BranchKind.NONE, BranchKind.NORMAL)
        w.setLevelForTest(2, BranchKind.NORMAL, BranchKind.NORMAL)
        assertEquals(2, w.target(Side.LEFT))
        assertEquals(1, w.target(Side.RIGHT))
        val ev = ArrayList<SincapEvent>()
        assertTrue(w.tap(Side.LEFT))
        assertTrue(w.jumping)
        run(w, 1f, ev)
        assertEquals(2, w.level)
        assertEquals(Side.LEFT, w.side)
        assertEquals(2, w.height)
        assertEquals(2 * SincapWorld.HEIGHT_POINTS, w.score)
        assertTrue(ev.contains(SincapEvent.Landed(2, Side.LEFT, BranchKind.NORMAL)))
        assertTrue("iki basamak daha uzun sürer", ev.filterIsInstance<SincapEvent.Jumped>().first().to == 2)
        w.tap(Side.RIGHT)
        run(w, 1f)
        assertEquals(3, w.level)
        assertEquals(Side.RIGHT, w.side)
        assertFalse(w.jumping)
        assertEquals(SincapWorld.sideX(Side.RIGHT), w.x, 1e-5f)
    }

    @Test
    fun tapTowardsNothingWithinReachFalls() {
        val w = quiet()
        w.setLevelForTest(1, BranchKind.NONE, BranchKind.NORMAL)
        w.setLevelForTest(2, BranchKind.NONE, BranchKind.NORMAL)
        val ev = ArrayList<SincapEvent>()
        w.tap(Side.LEFT)
        assertTrue(w.falling)
        run(w, SincapWorld.FALL_TIME + 0.1f, ev)
        assertEquals(SincapStatus.OVER, w.status)
        assertEquals(DeathCause.FALL, w.cause)
        assertTrue(ev.contains(SincapEvent.Jumped(0, -1, Side.LEFT)))
        assertEquals(0, w.height)
        assertFalse("bitince dokunuş işlemez", w.tap(Side.RIGHT))
    }

    @Test
    fun dryBranchBreaksUnlessLeftInTime() {
        val w = quiet()
        w.setLevelForTest(1, BranchKind.DRY, BranchKind.NORMAL)
        val ev = ArrayList<SincapEvent>()
        w.tap(Side.LEFT)
        run(w, 0.5f, ev)
        assertEquals(1, w.level)
        assertTrue(w.onDry)
        assertTrue(ev.contains(SincapEvent.Cracking(1, Side.LEFT)))
        run(w, SincapWorld.DRY_HOLD, ev)
        assertEquals(SincapStatus.OVER, w.status)
        assertEquals(DeathCause.BROKE, w.cause)
        assertTrue(ev.contains(SincapEvent.Broke(1, Side.LEFT)))
        assertEquals(BranchKind.NONE, w.levels[1].left)

        val w2 = quiet()
        w2.setLevelForTest(1, BranchKind.DRY, BranchKind.NORMAL)
        w2.tap(Side.LEFT)
        run(w2, 0.6f)
        w2.tap(Side.RIGHT)
        run(w2, 1f)
        assertEquals(SincapStatus.RUNNING, w2.status)
        assertEquals(2, w2.level)
        assertFalse(w2.onDry)
    }

    @Test
    fun snakeBranchKillsOnLanding() {
        val w = quiet()
        w.setLevelForTest(1, BranchKind.SNAKE, BranchKind.NORMAL)
        val ev = ArrayList<SincapEvent>()
        w.tap(Side.LEFT)
        run(w, 0.5f, ev)
        assertEquals(SincapStatus.OVER, w.status)
        assertEquals(DeathCause.SNAKE, w.cause)
        assertTrue(ev.contains(SincapEvent.Landed(1, Side.LEFT, BranchKind.SNAKE)))
        assertEquals("puan yazılır ama ölür", 1, w.height)
    }

    @Test
    fun crowKnocksOffOnlyOnItsOwnBranch() {
        val w = quiet()
        assertEquals(Side.RIGHT, w.side)
        w.spawnCrowForTest(0, 1, -SincapWorld.CROW_EDGE, 1.2f)
        run(w, 1.1f)
        assertEquals("henüz uzakta", SincapStatus.RUNNING, w.status)
        run(w, 0.3f)
        assertEquals(SincapStatus.OVER, w.status)
        assertEquals(DeathCause.CROW, w.cause)

        val w2 = quiet()
        w2.spawnCrowForTest(1, 1, -SincapWorld.CROW_EDGE, 1.2f)
        run(w2, 3f)
        assertEquals("başka basamak", SincapStatus.RUNNING, w2.status)
        assertTrue("kenardan çıkan karga silinir", w2.crows.isEmpty())

        // Zıplarken karga geçer, konunca dal boş: sağ kalır.
        val w3 = quiet()
        w3.spawnCrowForTest(1, -1, 0.2f, 1.2f)
        w3.tap(Side.RIGHT)
        run(w3, 1f)
        assertEquals(SincapStatus.RUNNING, w3.status)
        assertEquals(1, w3.level)
    }

    @Test
    fun catCatchesIdleSquirrelAndSpeedsUpWithHeight() {
        val w = SincapWorld(1L)
        w.freezeCrowsForTest()
        assertEquals(SincapWorld.CAT_GAP0, w.catGap, 1e-4f)
        val ev = ArrayList<SincapEvent>()
        run(w, 5f, ev)
        assertEquals(SincapWorld.CAT_GAP0 - 5f * SincapWorld.CAT_SPEED0, w.catGap, 0.05f)
        run(w, 15f, ev)
        assertEquals(SincapStatus.OVER, w.status)
        assertEquals(DeathCause.CAT, w.cause)
        val closeAt = ev.indexOf(SincapEvent.CatClose)
        val overAt = ev.indexOfFirst { it is SincapEvent.Over }
        assertTrue("uyarı yakalamadan önce", closeAt in 0 until overAt)

        val w2 = SincapWorld(3L)
        w2.freezeCrowsForTest()
        climb(w2, 120)
        assertTrue(w2.height >= 120)
        assertTrue(w2.catSpeed() > SincapWorld.CAT_SPEED0 + 0.3f)
        assertTrue(w2.catSpeed() <= SincapWorld.CAT_SPEED_CAP)
        assertTrue("kedi en çok ${SincapWorld.CAT_GAP_MAX} geride kalır", w2.catGap <= SincapWorld.CAT_GAP_MAX + 1e-3f)
    }

    @Test
    fun nutsScoreOnceAndGoldenScoresMore() {
        val w = quiet()
        w.setLevelForTest(1, BranchKind.NUT, BranchKind.NORMAL)
        w.setLevelForTest(2, BranchKind.NORMAL, BranchKind.GOLD)
        val ev = ArrayList<SincapEvent>()
        w.tap(Side.LEFT)
        run(w, 1f, ev)
        assertEquals(1, w.nuts)
        assertEquals(SincapWorld.HEIGHT_POINTS + SincapWorld.NUT_POINTS, w.score)
        assertEquals("fındık bir kez", BranchKind.NORMAL, w.levels[1].left)
        assertTrue(ev.contains(SincapEvent.Nut(1, Side.LEFT, SincapWorld.NUT_POINTS, false)))
        w.tap(Side.RIGHT)
        run(w, 1f, ev)
        assertEquals(1, w.golds)
        assertEquals(2 * SincapWorld.HEIGHT_POINTS + SincapWorld.NUT_POINTS + SincapWorld.GOLD_POINTS, w.score)
        assertTrue(ev.contains(SincapEvent.Nut(2, Side.RIGHT, SincapWorld.GOLD_POINTS, true)))
        assertEquals(w.score, w.hud().score)
    }

    @Test
    fun tapDuringJumpIsBufferedAndAppliedOnLanding() {
        val w = quiet()
        val ev = ArrayList<SincapEvent>()
        w.tap(Side.LEFT)
        run(w, 0.05f, ev)
        assertTrue(w.jumping)
        assertTrue(w.tap(Side.RIGHT))
        run(w, 1.5f, ev)
        assertEquals(2, w.level)
        assertEquals(Side.RIGHT, w.side)
        val jumps = ev.filterIsInstance<SincapEvent.Jumped>()
        assertEquals(listOf(SincapEvent.Jumped(0, 1, Side.LEFT), SincapEvent.Jumped(1, 2, Side.RIGHT)), jumps)
    }

    @Test
    fun milestonesFireAndPilotClimbsWithoutFalling() {
        val w = quiet(5L)
        val ev = ArrayList<SincapEvent>()
        var guard = 0
        while (w.height < 60 && guard++ < 60 * 300) {
            if (!w.jumping && !w.falling) SincapBots.decide(w)?.let { w.tap(it) }
            ev.addAll(w.step())
        }
        // İki basamaklık sıçrama eşiği aşabilir (25 ya da 26).
        assertEquals(listOf(1, 2), ev.filterIsInstance<SincapEvent.Milestone>().map { it.height / SincapWorld.MILESTONE })

        val runs = (1L..10L).map { SincapBots.play(it, SincapBots.Pilot(0.2f)) }
        assertTrue("pilot boşluğa atlamaz", runs.none { it.cause == DeathCause.FALL })
        assertTrue("ortalama yükseklik: ${runs.map { it.height }}", runs.map { it.height }.average() >= 20.0)
    }

    @Test
    fun dailySeedVariesByDay() {
        assertNotEquals(SincapWorld.dailySeed(20_000), SincapWorld.dailySeed(20_001))
        assertEquals(SincapWorld.dailySeed(20_000), SincapWorld.dailySeed(20_000))
        assertNull(SincapWorld(9L).cause)
    }
}
