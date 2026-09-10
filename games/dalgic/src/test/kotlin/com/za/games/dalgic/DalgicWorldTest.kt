package com.za.games.dalgic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class DalgicWorldTest {

    /** Boş deniz: doğum kapalı, akıntı yok. */
    private fun sea(seed: Long = 1L): DalgicWorld = DalgicWorld(seed).also { it.freezeSpawnForTest() }

    private fun run(w: DalgicWorld, frames: Int): List<DalgicEvent> {
        val out = ArrayList<DalgicEvent>()
        repeat(frames) { out += w.step() }
        return out
    }

    private fun runUntil(w: DalgicWorld, max: Int, pred: (List<DalgicEvent>) -> Boolean): List<DalgicEvent> {
        val out = ArrayList<DalgicEvent>()
        repeat(max) {
            out += w.step()
            if (pred(out)) return out
        }
        throw AssertionError("beklenen durum $max karede oluşmadı; olaylar: ${out.filter { it !is DalgicEvent.Shot }}")
    }

    @Test
    fun sameSeedSameInputsSameRun() {
        val a = DalgicWorld(11L)
        val b = DalgicWorld(11L)
        val rng = Random(5)
        var frames = 0
        while (a.status == DalgicStatus.RUNNING && frames < 3600) {
            val dx = (rng.nextFloat() - 0.5f) * 0.05f
            val dy = (rng.nextFloat() - 0.4f) * 0.05f
            a.steerBy(dx, dy)
            b.steerBy(dx, dy)
            assertEquals(a.step(), b.step())
            frames++
        }
        assertEquals(a.subX, b.subX, 0f)
        assertEquals(a.subY, b.subY, 0f)
        assertEquals(a.score, b.score)
        assertEquals(a.lives, b.lives)
        assertEquals(a.foes.map { it.id }, b.foes.map { it.id })
        assertEquals(DalgicWorld.dailySeed(3L), DalgicWorld.dailySeed(3L))
        assertNotEquals(DalgicWorld.dailySeed(3L), DalgicWorld.dailySeed(4L))
    }

    @Test
    fun steeringIsBoundedSpeedLimitedAndSetsFacing() {
        val w = sea()
        w.steerBy(5f, 5f)
        run(w, 300)
        assertEquals(DalgicWorld.WIDTH - DalgicWorld.SUB_R, w.subX, 1e-5f)
        assertEquals(DalgicWorld.FLOOR_Y - DalgicWorld.SUB_R, w.subY, 1e-5f)
        assertEquals(1, w.facing)
        w.steerBy(-0.5f, 0f)
        val x0 = w.subX
        w.step()
        assertEquals(-1, w.facing)
        assertEquals("adım başına en çok SUB_SPEED/60", DalgicWorld.SUB_SPEED * DalgicWorld.STEP, x0 - w.subX, 1e-4f)
        w.setDiversForTest(1)  // boş yüzeye çıkış can götürür; teslimle çıkılır
        w.steerBy(-5f, -5f)
        run(w, 400)
        assertEquals(DalgicWorld.SUB_R, w.subX, 1e-5f)
        assertEquals(DalgicWorld.SURFACE_LEVEL, w.subY, 1e-5f)
        assertTrue(w.atSurface)
        w.steerBy(0f, 0.3f)
        assertEquals("dikey hareket yönü değiştirmez", -1, w.facing)
    }

    @Test
    fun oxygenDrainsWhileDivingWarnsRunsOutAndRefillsAtTheSurface() {
        val w = sea()
        w.setSubForTest(0.5f, 0.8f)
        run(w, 60)
        assertEquals(DalgicWorld.OXYGEN_MAX - 1f, w.oxygen, 0.05f)
        assertFalse(w.atSurface)
        w.setOxygenForTest(DalgicWorld.OXYGEN_LOW + 0.05f)
        val warn = runUntil(w, 10) { it.contains(DalgicEvent.OxygenLow) }
        assertEquals(1, warn.count { it == DalgicEvent.OxygenLow })
        w.setOxygenForTest(0.02f)
        val ev = runUntil(w, 5) { it.any { e -> e is DalgicEvent.LifeLost } }
        assertEquals(DalgicEvent.LifeLost(LifeCause.OXYGEN, 2), ev.last { it is DalgicEvent.LifeLost })
        assertEquals(2, w.lives)
        assertTrue("yüzeye döner", w.atSurface)
        assertEquals(DalgicWorld.OXYGEN_MAX, w.oxygen, 0f)
        assertTrue(w.hud().invulnerable)
        // Yüzeyde dolar.
        w.setOxygenForTest(10f)
        run(w, 60)
        assertEquals(20f, w.oxygen, 0.2f)
        assertTrue(run(w, 120).none { it is DalgicEvent.LifeLost })
        assertEquals("yüzeyde ateş edilmez", 0, w.torpedoes.size)
    }

    @Test
    fun rescuesUpToCapacityAndDeliversAtTheSurfaceWithBonus() {
        val w = sea()
        w.setSubForTest(0.5f, 0.8f)
        var full = 0
        for (i in 0 until 7) {
            w.addDiverForTest(0.5f, 0.8f, dir = 1, speed = 0f)
            val ev = run(w, 1)
            if (ev.contains(DalgicEvent.Full)) full++
            if (i < 6) assertTrue("dalgıç $i toplanır", ev.any { it is DalgicEvent.DiverRescued })
        }
        assertEquals(DalgicWorld.CAPACITY, w.divers)
        assertEquals("kapasite doluyken yedinci alınmaz", 1, w.diverList.count { it.alive })
        assertEquals(1, full)
        val before = w.currents.toList()
        w.setCurrentsForTest(listOf(Current(0.5f, 0.09f, 1, 0.1f)))
        w.steerTo(0.5f, DalgicWorld.SURFACE_LEVEL)
        val ev = runUntil(w, 120) { it.any { e -> e is DalgicEvent.Delivered } }
        val delivered = ev.filterIsInstance<DalgicEvent.Delivered>().single()
        assertEquals(6, delivered.divers)
        assertEquals(6 * DalgicWorld.DIVER_POINTS + DalgicWorld.FULL_BONUS, delivered.points)
        assertEquals(1, delivered.wave)
        assertEquals(delivered.points, w.score)
        assertEquals(0, w.divers)
        assertEquals(6, w.rescued)
        assertEquals(1, w.wave)
        assertEquals(3, w.lives)
        assertTrue(ev.indexOf(DalgicEvent.Surfaced) < ev.indexOf(delivered))
        assertEquals("teslimde akıntılar yeniden dizilir", DalgicWorld.CURRENT_BANDS, w.currents.size)
        assertTrue(before.isEmpty())
        // Dalga bonusu: 1. dalgada dalgıç başına +10.
        w.setSubForTest(0.5f, 0.8f)
        w.addDiverForTest(0.5f, 0.8f, speed = 0f)
        run(w, 1)
        w.steerTo(0.5f, DalgicWorld.SURFACE_LEVEL)
        val ev2 = runUntil(w, 120) { it.any { e -> e is DalgicEvent.Delivered } }
        // Dönüşte ilk turdan kalan yedinci dalgıç da alınır: iki dalgıç × (50 + 10).
        assertEquals(2 * (DalgicWorld.DIVER_POINTS + DalgicWorld.WAVE_BONUS), ev2.filterIsInstance<DalgicEvent.Delivered>().single().points)
        assertTrue(w.difficulty > 1.2f)
    }

    @Test
    fun surfacingEmptyAfterADiveCostsALifeButNotAtTheStart() {
        val w = sea()
        run(w, 120)
        assertEquals("başta yüzeyde beklemek bedava", 3, w.lives)
        w.steerTo(0.5f, 0.6f)
        run(w, 90)
        assertFalse(w.atSurface)
        w.steerTo(0.5f, DalgicWorld.SURFACE_LEVEL)
        val ev = runUntil(w, 120) { it.any { e -> e is DalgicEvent.LifeLost } }
        assertEquals(DalgicEvent.LifeLost(LifeCause.EMPTY_SURFACE, 2), ev.last { it is DalgicEvent.LifeLost })
        assertEquals(2, w.lives)
        // Tekrar dalmadan beklemek can götürmez.
        run(w, 200)
        assertEquals(2, w.lives)
    }

    @Test
    fun sharkContactCostsALifeAndInvulnerabilityProtectsAfterRespawn() {
        val w = sea()
        w.setSubForTest(0.5f, 0.8f)
        // Köpekbalığı arkadan gelir; torpidolar öne (sağa) gittiğinden vurulmaz.
        w.addFoeForTest(FoeKind.SHARK, 0.5f - 0.2f, 0.8f, dir = 1, speed = 0.4f)
        val ev = runUntil(w, 120) { it.any { e -> e is DalgicEvent.LifeLost } }
        assertEquals(DalgicEvent.LifeLost(LifeCause.SHARK, 2), ev.last { it is DalgicEvent.LifeLost })
        assertTrue(w.atSurface)
        // Yeniden doğuşta bir köpekbalığı üstümüze konsa bile 2 s dokunulmaz.
        val shark = w.addFoeForTest(FoeKind.SHARK, w.subX, w.subY, dir = 1, speed = 0f)
        run(w, 100)
        assertEquals(2, w.lives)
        assertTrue(shark.alive)
        run(w, 40)
        assertEquals("dokunulmazlık bitince çarpar", 1, w.lives)
        // Mayın: çarpınca patlar.
        w.setSubForTest(0.5f, 1.2f)
        run(w, 130)
        val mine = w.addFoeForTest(FoeKind.MINE, 0.5f, 1.2f, dir = 0, speed = 0f)
        val ev2 = runUntil(w, 5) { it.any { e -> e is DalgicEvent.LifeLost } }
        assertEquals(LifeCause.MINE, (ev2.last { it is DalgicEvent.LifeLost } as DalgicEvent.LifeLost).cause)
        assertFalse(mine.alive)
        assertEquals(DalgicStatus.OVER, w.status)
        assertEquals(DalgicEvent.Over, ev2.last())
        assertTrue(w.step().isEmpty())
    }

    @Test
    fun torpedoesAutoFireForwardKillFoesAndCancelEnemyShots() {
        val w = sea()
        w.setSubForTest(0.3f, 0.8f)
        w.steerBy(0.001f, 0f)
        val shots = run(w, 60).count { it == DalgicEvent.Shot }
        assertTrue("saniyede ~1,7 atış: $shots", shots in 1..2)
        assertTrue(w.torpedoes.all { it.friendly && it.vx > 0f })
        val shark = w.addFoeForTest(FoeKind.SHARK, 0.7f, 0.8f, dir = -1, speed = 0f)
        val ev = runUntil(w, 60) { it.any { e -> e is DalgicEvent.FoeDown } }
        val down = ev.filterIsInstance<DalgicEvent.FoeDown>().single()
        assertEquals(FoeKind.SHARK, down.kind)
        assertEquals(FoeKind.SHARK.points, down.points)
        assertFalse(shark.alive)
        assertEquals(FoeKind.SHARK.points, w.score)
        // Arkadaki düşmana torpido gitmez.
        val behind = w.addFoeForTest(FoeKind.MINE, 0.1f, 0.8f, dir = 0, speed = 0f)
        run(w, 90)
        assertTrue(behind.alive)
        // Düşman denizaltı ateş eder; torpidosu bize çarpar.
        val v = sea()
        v.setSubForTest(0.3f, 0.8f)
        v.steerBy(-0.001f, 0f)
        v.addFoeForTest(FoeKind.ENEMY_SUB, 0.8f, 0.8f, dir = -1, speed = 0f)
        val shot = runUntil(v, 120) { it.contains(DalgicEvent.EnemyShot) }
        assertTrue(shot.contains(DalgicEvent.EnemyShot))
        val hit = runUntil(v, 120) { it.any { e -> e is DalgicEvent.LifeLost } }
        assertEquals(LifeCause.TORPEDO, (hit.last { it is DalgicEvent.LifeLost } as DalgicEvent.LifeLost).cause)
        // Dost torpido düşman torpidosunu yolda düşürür.
        val u = sea()
        u.setSubForTest(0.3f, 0.8f)
        u.steerBy(0.001f, 0f)
        u.addEnemyTorpedoForTest(0.9f, 0.8f, -DalgicWorld.ENEMY_TORPEDO_SPEED)
        run(u, 90)
        assertEquals("torpidolar çarpışıp yok olur", 3, u.lives)
        assertTrue(u.torpedoes.none { !it.friendly })
        // Dalga puanı: 2. dalgada köpekbalığı 40.
        val z = sea()
        z.setWaveForTest(2)
        z.setSubForTest(0.3f, 0.8f)
        z.steerBy(0.001f, 0f)
        z.addFoeForTest(FoeKind.SHARK, 0.6f, 0.8f, dir = -1, speed = 0f)
        val zev = runUntil(z, 60) { it.any { e -> e is DalgicEvent.FoeDown } }
        assertEquals(FoeKind.SHARK.points + 2 * DalgicWorld.WAVE_BONUS, zev.filterIsInstance<DalgicEvent.FoeDown>().single().points)
    }

    @Test
    fun currentsPushTheSubAndDivers() {
        val w = sea()
        w.setSubForTest(0.5f, 0.8f)
        w.setCurrentsForTest(listOf(Current(0.8f, 0.09f, 1, 0.15f)))
        run(w, 60)
        assertEquals("akıntı saniyede 0,15 sürükler", 0.65f, w.subX, 0.01f)
        val dv = w.addDiverForTest(0.2f, 0.8f, dir = 1, speed = 0.1f)
        run(w, 60)
        assertEquals(0.2f + 0.25f, dv.x, 0.01f)
        // Bandın dışında sürüklenmez.
        w.setSubForTest(0.5f, 1.2f)
        run(w, 60)
        assertEquals(0.5f, w.subX, 1e-4f)
    }

    /** Değişmez: doğan her şey şeritlerde ve suda kalır; mayınlar alt şeritlerde ve en çok üç. */
    @Test
    fun spawnsStayInLanesAndLeaveTheScreen() {
        for (seed in 1L..6L) {
            val w = DalgicWorld(seed)
            var divers = 0
            var sharks = 0
            var subs = 0
            repeat(60 * 60) {
                w.step()
                for (dv in w.diverList) {
                    assertTrue("dalgıç y ${dv.y}", dv.y >= DalgicWorld.laneY(0) - 0.01f && dv.y <= DalgicWorld.laneY(5) + 0.01f)
                }
                for (f in w.foes) {
                    assertTrue("${f.kind} y ${f.y}", f.y >= DalgicWorld.laneY(0) - 0.06f && f.y <= DalgicWorld.FLOOR_Y - 0.03f)
                    if (f.kind == FoeKind.MINE) assertTrue("mayın alt şeritte", f.baseY >= DalgicWorld.laneY(3))
                }
                assertTrue(w.foes.count { it.kind == FoeKind.MINE } <= DalgicWorld.MAX_MINES)
                divers = maxOf(divers, w.diverList.size)
                sharks = maxOf(sharks, w.foes.count { it.kind == FoeKind.SHARK })
                subs = maxOf(subs, w.foes.count { it.kind == FoeKind.ENEMY_SUB })
            }
            assertTrue("tohum $seed: dalgıç $divers köpekbalığı $sharks denizaltı $subs", divers >= 1 && sharks >= 1 && subs >= 1)
            assertTrue("yüzeyde beklemek güvenli", w.lives == DalgicWorld.LIVES && w.atSurface)
            assertTrue("ekranı terk edenler silinir", w.foes.size + w.diverList.size < 30)
        }
    }

    /** Değişmez: pilot 20 tohumun en az 14'ünde bir yük teslim eder (oyun kurtarmaya izin verir). */
    @Test
    fun aPilotCanDeliverDivers() {
        val dives = (1L..20L).map { DalgicBots.play(it, DalgicBots.Pilot(0.12f)) }
        val delivered = dives.count { it.rescued >= 1 }
        assertTrue("teslim eden koşu: $delivered / 20 — ${dives.map { it.rescued }}", delivered >= 14)
        assertTrue(dives.all { it.frames > 60 })
    }
}
