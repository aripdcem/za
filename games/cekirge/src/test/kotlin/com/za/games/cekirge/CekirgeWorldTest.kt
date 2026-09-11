package com.za.games.cekirge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CekirgeWorldTest {

    private fun run(w: CekirgeWorld, seconds: Float, ev: MutableList<CekirgeEvent>? = null) {
        repeat((seconds / CekirgeWorld.STEP + 0.5f).toInt()) {
            val e = w.step()
            ev?.addAll(e)
        }
    }

    /** Koşul sağlanana dek adımlar; sağlandıysa true. */
    private fun runUntil(w: CekirgeWorld, seconds: Float, ev: MutableList<CekirgeEvent>, until: (CekirgeEvent) -> Boolean): Boolean {
        repeat((seconds / CekirgeWorld.STEP + 0.5f).toInt()) {
            val e = w.step()
            ev.addAll(e)
            if (e.any(until)) return true
        }
        return false
    }

    /** Tükürük ve kraliçe yok: kurallar tek tek ölçülür. */
    private fun quiet(seed: Long = 1L): CekirgeWorld = CekirgeWorld(seed).apply {
        freezeSpitsForTest()
        setQueenTimerForTest(1e9f)
    }

    /** Sütunun alt çekirgesini fıskırtma varış anında vuracak x. */
    private fun aimAt(w: CekirgeWorld, col: Int): Float {
        val b = w.bugs.filter { it.alive && it.col == col }.maxBy { it.row }
        val t = (CekirgeWorld.FARMER_Y - w.bugY(b)) / CekirgeWorld.SHOT_SPEED
        return w.bugX(b) + w.swarmDir * w.swarmSpeed() * t
    }

    @Test
    fun sameSeedAndSameInputsReplayIdentically() {
        val a = CekirgeWorld(42L)
        val b = CekirgeWorld(42L)
        for (f in 0 until 60 * 40) {
            if (f % 20 == 0) {
                val dx = if ((f / 20) % 2 == 0) 0.05f else -0.05f
                a.moveBy(dx)
                b.moveBy(dx)
            }
            if (f % 30 == 0) {
                a.fire()
                b.fire()
            }
            a.step()
            b.step()
            assertEquals(a.score, b.score)
            assertEquals(a.farmerX, b.farmerX, 0f)
            assertEquals(a.swarmX, b.swarmX, 0f)
            assertEquals(a.spits.size, b.spits.size)
            assertEquals(a.status, b.status)
            if (a.status != CekirgeStatus.RUNNING) break
        }
    }

    @Test
    fun initialFormationKindsAndFirstWave() {
        val w = quiet()
        assertEquals(CekirgeWorld.TOTAL, w.bugs.size)
        for (b in w.bugs) {
            assertTrue(b.alive)
            assertEquals(CekirgeWorld.kindOf(b.row), b.kind)
            assertTrue(w.bugX(b) - CekirgeWorld.BUG_HALF_W >= CekirgeWorld.EDGE_MARGIN - 1e-4f)
            assertTrue(w.bugX(b) + CekirgeWorld.BUG_HALF_W <= CekirgeWorld.WIDTH - CekirgeWorld.EDGE_MARGIN + 1e-4f)
        }
        assertEquals(BugKind.KARA, w.bugs.first { it.row == 0 }.kind)
        assertEquals(BugKind.YESIL, w.bugs.first { it.row == 2 }.kind)
        assertEquals(BugKind.KAHVE, w.bugs.first { it.row == 4 }.kind)
        val hud = w.hud()
        assertEquals(CekirgeWorld.TOTAL, hud.alive)
        assertEquals(CekirgeWorld.TOTAL, hud.total)
        assertEquals(1, hud.wave)
        assertEquals(CekirgeWorld.LIVES, hud.lives)
        assertTrue(hud.shotReady)
        assertTrue(w.step().contains(CekirgeEvent.WaveStart(1)))
    }

    @Test
    fun onlyOneShotFliesAtATime() {
        val w = quiet()
        // Sol kenardan sık: sürü sağa yürüdüğünden fıskırtma kimseye değmeden çıkar.
        w.setFarmerForTest(CekirgeWorld.FARMER_MIN_X)
        val ev = ArrayList<CekirgeEvent>()
        assertTrue(w.fire())
        assertFalse("uçan fıskırtma varken ikinci sıkılmaz", w.fire())
        assertNotNull(w.shot)
        assertFalse(w.hud().shotReady)
        run(w, 0.05f, ev)
        assertTrue(ev.contains(CekirgeEvent.Fired))
        run(w, 1.2f, ev)
        assertNull("tarlayı terk etti", w.shot)
        assertTrue(ev.none { it is CekirgeEvent.BugHit })
        assertTrue(w.fire())
    }

    @Test
    fun shotHitsTheBottomBugOfItsColumnAndScoresByKind() {
        val w = quiet()
        // 3. sütun balyaların arasındaki boşluktan vurulur; balya önündeki sütuna atılan fıskırtma balyada kalır.
        w.setFarmerForTest(aimAt(w, 3))
        assertTrue(w.fire())
        val ev = ArrayList<CekirgeEvent>()
        assertTrue(runUntil(w, 1.5f, ev) { it is CekirgeEvent.BugHit })
        val hit = ev.filterIsInstance<CekirgeEvent.BugHit>().single()
        assertEquals(BugKind.KAHVE, hit.kind)
        assertEquals(BugKind.KAHVE.points, hit.points)
        assertEquals(BugKind.KAHVE.points, w.score)
        assertEquals(1, w.kills)
        assertEquals(1, w.hud().kills)
        assertEquals(CekirgeWorld.TOTAL - 1, w.aliveCount)
        assertFalse(w.bugs.first { it.col == 3 && it.row == 4 }.alive)
        assertTrue("üst sıralar sağ", w.bugs.filter { it.col == 3 && it.row < 4 }.all { it.alive })
        assertNull("vuran fıskırtma tükenir", w.shot)
    }

    @Test
    fun swarmReversesAtTheEdgeAndStepsDown() {
        val w = quiet()
        val y0 = 0.3f
        w.setSwarmForTest(CekirgeWorld.WIDTH - CekirgeWorld.EDGE_MARGIN - (CekirgeWorld.COLS - 1) * CekirgeWorld.SPACING_X - CekirgeWorld.BUG_HALF_W - 0.01f, y0, 1)
        run(w, 1f)
        assertEquals(-1, w.swarmDir)
        assertEquals(y0 + CekirgeWorld.STEP_DOWN, w.swarmY, 1e-4f)
        val right = w.bugs.filter { it.alive }.maxOf { w.bugX(it) } + CekirgeWorld.BUG_HALF_W
        assertTrue(right <= CekirgeWorld.WIDTH - CekirgeWorld.EDGE_MARGIN + 1e-4f)
    }

    @Test
    fun swarmSpeedsUpAsItThinsAndWithWaves() {
        val w = quiet()
        val s0 = w.swarmSpeed()
        assertEquals(CekirgeWorld.SWARM_SPEED0, s0, 1e-5f)
        for (b in w.bugs.take(CekirgeWorld.TOTAL - 5)) w.killForTest(b.col, b.row)
        val s1 = w.swarmSpeed()
        assertTrue("beş kalınca en az iki kat: $s1", s1 > s0 * 2f)
        for (b in w.bugs.drop(CekirgeWorld.TOTAL - 5).take(4)) w.killForTest(b.col, b.row)
        assertEquals(1, w.aliveCount)
        assertTrue("son çekirge ~4 kat", abs(w.swarmSpeed() / s0 - 4f) < 0.2f)
    }

    @Test
    fun spitHitsTheFarmerCostsALifeAndGrantsBriefInvulnerability() {
        val w = quiet()
        w.setFarmerForTest(0.5f)
        w.spawnSpitForTest(0.5f, CekirgeWorld.FARMER_Y - 0.1f)
        val ev = ArrayList<CekirgeEvent>()
        run(w, 0.4f, ev)
        assertEquals(2, w.lives)
        assertTrue(ev.contains(CekirgeEvent.FarmerHit(2)))
        assertTrue(w.invuln > 0f)
        assertTrue("sürü kısa süre durur", w.pause > 0f)
        assertTrue(w.spits.isEmpty())
        // Duraklama sürerken bırakılan tükürük duraklama bitince dokunulmaz çiftçiden geçer.
        w.spawnSpitForTest(0.5f, CekirgeWorld.FARMER_Y - 0.05f)
        run(w, CekirgeWorld.RESPAWN_PAUSE, ev)
        assertTrue(w.pause <= 0f)
        assertTrue("dokunulmazlık duraklamadan uzun", w.invuln > 0f)
        run(w, 0.25f, ev)
        assertEquals("dokunulmazken can gitmez", 2, w.lives)
        run(w, 2f)
        assertTrue(w.invuln <= 0f)
        w.spawnSpitForTest(0.5f, CekirgeWorld.FARMER_Y - 0.05f)
        run(w, 0.3f)
        assertEquals(1, w.lives)
        run(w, CekirgeWorld.RESPAWN_PAUSE + CekirgeWorld.INVULN + 0.5f)
        w.spawnSpitForTest(0.5f, CekirgeWorld.FARMER_Y - 0.05f)
        run(w, 0.3f, ev)
        assertEquals(0, w.lives)
        assertEquals(CekirgeStatus.OVER, w.status)
        assertTrue(ev.contains(CekirgeEvent.Over(invaded = false)))
        assertFalse(w.fire())
    }

    @Test
    fun balesErodeFromShotsSpitsAndTheSwarm() {
        val w = quiet()
        val bale = w.bales[0]
        val full = CekirgeWorld.BALE_CX * CekirgeWorld.BALE_CY
        assertEquals(full, bale.intact)
        w.setFarmerForTest(bale.x)
        assertTrue(w.fire())
        val ev = ArrayList<CekirgeEvent>()
        assertTrue(runUntil(w, 1f, ev) { it is CekirgeEvent.BaleHit })
        assertTrue(bale.intact < full)
        assertNull("balyaya çarpan fıskırtma tükenir", w.shot)
        val afterShot = bale.intact
        w.spawnSpitForTest(bale.x, CekirgeWorld.BALE_Y - 0.05f)
        assertTrue(runUntil(w, 1f, ev) { it is CekirgeEvent.BaleHit })
        assertTrue(bale.intact < afterShot)
        assertTrue(w.spits.isEmpty())

        val bale1 = w.bales[1]
        w.setSwarmForTest(bale1.x - 4 * CekirgeWorld.SPACING_X, CekirgeWorld.BALE_Y - 4 * CekirgeWorld.SPACING_Y, 1)
        run(w, 0.05f)
        assertTrue("sürü balyayı kemirir", bale1.intact < full)
    }

    @Test
    fun queenCrossesAndAHitPaysTheBonus() {
        val w = quiet()
        // Sürü yolda olmasın: hepsi ölü, dalga temizliği dondurulmuş.
        w.freezeWavesForTest()
        for (b in w.bugs) w.killForTest(b.col, b.row)
        w.setQueenTimerForTest(0.05f)
        val ev = ArrayList<CekirgeEvent>()
        assertTrue(runUntil(w, 0.5f, ev) { it is CekirgeEvent.QueenSpawned })
        val q = w.queen!!
        val t = (CekirgeWorld.FARMER_Y - CekirgeWorld.FARMER_HALF_H - CekirgeWorld.QUEEN_Y) / CekirgeWorld.SHOT_SPEED
        // Varış anındaki yeri balyaların arasındaki orta boşluğa (x ≈ 0,5) düşene dek bekle, sonra nişan al.
        var guard = 0
        while (abs(q.x + q.dir * CekirgeWorld.QUEEN_SPEED * t - 0.5f) > 0.03f && guard++ < 600) w.step()
        w.setFarmerForTest(q.x + q.dir * CekirgeWorld.QUEEN_SPEED * t)
        assertTrue(w.fire())
        assertTrue(runUntil(w, 1.5f, ev) { it is CekirgeEvent.QueenHit })
        val hit = ev.filterIsInstance<CekirgeEvent.QueenHit>().single()
        assertTrue(hit.points in CekirgeWorld.QUEEN_POINTS.toList())
        assertEquals(hit.points, w.score)
        assertNull(w.queen)

        val w2 = quiet()
        w2.freezeWavesForTest()
        for (b in w2.bugs) w2.killForTest(b.col, b.row)
        w2.setQueenTimerForTest(0.05f)
        run(w2, 0.5f)
        assertNotNull(w2.queen)
        run(w2, (CekirgeWorld.WIDTH + 0.3f) / CekirgeWorld.QUEEN_SPEED)
        assertNull("vurulmayan kraliçe çıkar", w2.queen)
    }

    @Test
    fun clearingTheSwarmStartsALowerFasterWaveWithBonus() {
        val w = quiet()
        for (b in w.bugs) if (!(b.col == 0 && b.row == 0)) w.killForTest(b.col, b.row)
        run(w, 0.1f)
        val ev = ArrayList<CekirgeEvent>()
        w.killForTest(0, 0)
        run(w, 0.05f, ev)
        assertTrue(ev.contains(CekirgeEvent.WaveCleared(1, CekirgeWorld.WAVE_BONUS)))
        assertEquals(CekirgeWorld.WAVE_BONUS, w.score)
        assertFalse("dalga arasında sıkılmaz", w.fire())
        run(w, CekirgeWorld.WAVE_GAP + 0.1f, ev)
        assertTrue(ev.contains(CekirgeEvent.WaveStart(2)))
        assertEquals(2, w.wave)
        assertEquals(CekirgeWorld.TOTAL, w.aliveCount)
        assertEquals(CekirgeWorld.SWARM_START_Y + CekirgeWorld.WAVE_DROP, w.swarmY, 1e-4f)
        assertTrue(w.swarmSpeed() > CekirgeWorld.SWARM_SPEED0)
        assertTrue(w.fire())
    }

    @Test
    fun swarmReachingTheFarmerRowIsAnInvasion() {
        val w = quiet()
        val bottomRow = 4
        val y = CekirgeWorld.INVASION_Y - bottomRow * CekirgeWorld.SPACING_Y - CekirgeWorld.BUG_HALF_H - 0.01f
        w.setSwarmForTest(CekirgeWorld.WIDTH - CekirgeWorld.EDGE_MARGIN - (CekirgeWorld.COLS - 1) * CekirgeWorld.SPACING_X - CekirgeWorld.BUG_HALF_W - 0.005f, y, 1)
        val ev = ArrayList<CekirgeEvent>()
        run(w, 0.5f, ev)
        assertEquals(CekirgeStatus.OVER, w.status)
        assertTrue(w.invaded)
        assertTrue(ev.contains(CekirgeEvent.Over(invaded = true)))
        assertEquals(3, w.lives)
    }

    @Test
    fun dailySeedVariesAndPilotClearsTheFirstWave() {
        assertNotEquals(CekirgeWorld.dailySeed(20_000), CekirgeWorld.dailySeed(20_001))
        assertEquals(CekirgeWorld.dailySeed(20_000), CekirgeWorld.dailySeed(20_000))
        val runs = (1L..6L).map { CekirgeBots.play(it, CekirgeBots.Pilot(0.2f, 0.9f, 0.02f)) }
        assertTrue("pilot çoğunlukla ilk dalgayı temizler: ${runs.map { it.wave }}", runs.count { it.wave >= 2 } >= 4)
        assertTrue(runs.all { it.kills >= 20 })
    }
}
