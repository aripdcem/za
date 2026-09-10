package com.za.games.ucurtma

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class UcurtmaWorldTest {

    /** Boş gök: üretim kapalı, engeller elle konur. */
    private fun sky(seed: Long = 1L, gadget: Gadget? = null, missions: List<Mission> = emptyList()): UcurtmaWorld =
        UcurtmaWorld(seed, gadget, missions).also { it.freezeSpawnForTest() }

    private fun run(w: UcurtmaWorld, frames: Int): List<UcurtmaEvent> {
        val out = ArrayList<UcurtmaEvent>()
        repeat(frames) { out += w.step() }
        return out
    }

    private fun runUntil(w: UcurtmaWorld, max: Int, pred: (List<UcurtmaEvent>) -> Boolean): List<UcurtmaEvent> {
        val out = ArrayList<UcurtmaEvent>()
        repeat(max) {
            out += w.step()
            if (pred(out)) return out
        }
        throw AssertionError("beklenen durum $max karede oluşmadı; olaylar: $out")
    }

    private fun ahead(w: UcurtmaWorld, dx: Float): Float = w.distance + UcurtmaWorld.KITE_X + dx

    @Test
    fun sameSeedSameInputsSameRun() {
        val a = UcurtmaWorld(11L)
        val b = UcurtmaWorld(11L)
        val rng = Random(3)
        var frames = 0
        while (a.status == UcurtmaStatus.RUNNING && frames < 3600) {
            val h = rng.nextFloat() < 0.5f
            a.hold(h)
            b.hold(h)
            assertEquals(a.step(), b.step())
            frames++
        }
        assertEquals(a.kiteY, b.kiteY, 0f)
        assertEquals(a.meters, b.meters)
        assertEquals(a.status, b.status)
        assertEquals(a.buildings.map { it.x }, b.buildings.map { it.x })
        assertTrue("ilerleme olmalı", a.meters > 0)
        assertEquals(UcurtmaWorld.dailySeed(5L), UcurtmaWorld.dailySeed(5L))
        assertNotEquals(UcurtmaWorld.dailySeed(5L), UcurtmaWorld.dailySeed(6L))
    }

    @Test
    fun holdingClimbsReleasingSinksWithinLimits() {
        val w = sky()
        w.setKiteForTest(0.9f)
        w.hold(true)
        run(w, 30)
        assertTrue("yükselir", w.kiteY < 0.9f)
        assertTrue(w.kiteVy < 0f && w.kiteVy >= -UcurtmaWorld.VMAX)
        run(w, 300)
        assertEquals("tavana dayanır", UcurtmaWorld.CEIL + UcurtmaWorld.KITE_R, w.kiteY, 1e-5f)
        assertEquals(0f, w.kiteVy, 0f)
        w.hold(false)
        run(w, 30)
        assertTrue(w.kiteY > UcurtmaWorld.CEIL + UcurtmaWorld.KITE_R && w.kiteVy > 0f)
        run(w, 400)
        assertEquals("zemin güvenli", UcurtmaWorld.GROUND - UcurtmaWorld.KITE_R, w.kiteY, 1e-5f)
        assertEquals(UcurtmaStatus.RUNNING, w.status)
        val v = sky()
        v.setKiteForTest(0.1f)
        v.hold(false)
        run(v, 45)
        assertEquals("düşüş hızı tavanı", UcurtmaWorld.VMAX, v.kiteVy, 1e-5f)
        assertTrue(v.kiteY < UcurtmaWorld.GROUND - UcurtmaWorld.KITE_R)
    }

    @Test
    fun gadgetsChangeSinkAndLift() {
        val plain = sky()
        val tail = sky(gadget = Gadget.TAIL)
        plain.setKiteForTest(0.3f)
        tail.setKiteForTest(0.3f)
        run(plain, 30)
        run(tail, 30)
        assertTrue("kuyruk daha yavaş alçalır", tail.kiteY < plain.kiteY)
        val reel = sky(gadget = Gadget.REEL)
        val p2 = sky()
        reel.setKiteForTest(1.2f)
        p2.setKiteForTest(1.2f)
        reel.hold(true)
        p2.hold(true)
        run(reel, 30)
        run(p2, 30)
        assertTrue("makara daha güçlü yükselir", reel.kiteY < p2.kiteY)
        assertEquals(UcurtmaWorld.GLASS_CUT_POINTS, sky(gadget = Gadget.GLASS).cutPoints)
        assertEquals(UcurtmaWorld.CUT_POINTS, sky().cutPoints)
    }

    @Test
    fun speedRampsWithDistanceAndCaps() {
        val w = sky()
        assertEquals(UcurtmaWorld.BASE_SPEED, w.speed, 0f)
        w.hold(false)
        var guard = 0
        while (w.speed < UcurtmaWorld.MAX_SPEED && guard++ < 60 * 400) w.step()
        assertEquals(UcurtmaWorld.MAX_SPEED, w.speed, 0f)
        assertTrue("tavan 600 m civarı: ${w.meters}", w.meters in 590..640)
        assertEquals(UcurtmaStatus.RUNNING, w.status)
        assertTrue(w.difficulty > 0.3f && w.difficulty < 0.5f)
        assertEquals(6, w.hud().meters / 100)
    }

    @Test
    fun roofsChimneysWiresAndRivalStringsCrash() {
        val a = sky()
        a.setKiteForTest(UcurtmaWorld.GROUND - UcurtmaWorld.KITE_R)
        a.hold(false)
        a.addBuildingForTest(ahead(a, 0.3f), 0.2f, 0.3f)
        val ev = runUntil(a, 120) { it.any { e -> e is UcurtmaEvent.Crash } }
        assertEquals(UcurtmaEvent.Crash(CrashKind.ROOF), ev.first { it is UcurtmaEvent.Crash })
        assertEquals(UcurtmaEvent.Over, ev.last())
        assertEquals(UcurtmaStatus.OVER, a.status)
        assertEquals(CrashKind.ROOF, a.crash)
        assertTrue(a.step().isEmpty())

        val c = sky()
        val top = UcurtmaWorld.GROUND - 0.3f
        c.setKiteForTest(top - UcurtmaWorld.KITE_R - 0.06f)
        c.hold(false)
        c.addBuildingForTest(ahead(c, 0.05f), 0.2f, 0.3f, chimney = true)
        val cev = runUntil(c, 60) { it.any { e -> e is UcurtmaEvent.Crash } }
        assertEquals("bacaya çarpar", UcurtmaEvent.Crash(CrashKind.ROOF), cev.first { it is UcurtmaEvent.Crash })

        val t = sky()
        t.setKiteForTest(0.8f)
        t.hold(false)
        t.addWireForTest(ahead(t, 0.1f), ahead(t, 0.7f), 0.85f)
        val tev = runUntil(t, 120) { it.any { e -> e is UcurtmaEvent.Crash } }
        assertEquals(UcurtmaEvent.Crash(CrashKind.WIRE), tev.first { it is UcurtmaEvent.Crash })

        val s = sky()
        s.setKiteForTest(0.9f)
        s.hold(false)
        s.addRivalForTest(ahead(s, 0.25f), 0.6f)
        val sev = runUntil(s, 240) { it.any { e -> e is UcurtmaEvent.Crash } }
        assertEquals("üstteki rakibin ipi bizi keser", UcurtmaEvent.Crash(CrashKind.STRING), sev.first { it is UcurtmaEvent.Crash })
    }

    @Test
    fun passingAboveARivalCutsItAndGlassPowderIsImmune() {
        val w = sky()
        w.setKiteForTest(0.3f)
        w.hold(true)
        val r = w.addRivalForTest(ahead(w, 0.4f), 0.8f)
        val ev = runUntil(w, 240) { it.any { e -> e is UcurtmaEvent.Cut } }
        assertFalse(r.alive)
        assertEquals(1, w.cuts)
        assertEquals(UcurtmaWorld.CUT_POINTS, ev.filterIsInstance<UcurtmaEvent.Cut>().single().bonus)
        assertEquals(UcurtmaStatus.RUNNING, w.status)
        assertEquals(w.meters + UcurtmaWorld.CUT_POINTS, w.score)
        run(w, 240)
        assertTrue("kesilen rakip düşer ve silinir", w.rivals.none { it.id == r.id })

        val g = sky(gadget = Gadget.GLASS)
        g.setKiteForTest(1.2f)
        g.hold(false)
        g.addRivalForTest(ahead(g, 0.4f), 0.5f)
        run(g, 240)
        assertEquals("cam tozu rakip ipine bağışık", UcurtmaStatus.RUNNING, g.status)
    }

    @Test
    fun ribbonsUnderWiresAndNearMissesCount() {
        val w = sky()
        w.setKiteForTest(UcurtmaWorld.GROUND - UcurtmaWorld.KITE_R)
        w.hold(false)
        w.addRibbonForTest(ahead(w, 0.2f), UcurtmaWorld.GROUND - UcurtmaWorld.KITE_R)
        w.addWireForTest(ahead(w, 0.4f), ahead(w, 0.8f), 0.9f)
        val ev = runUntil(w, 200) { it.any { e -> e is UcurtmaEvent.UnderWire } }
        assertEquals(1, w.ribbons)
        assertEquals(1, ev.filterIsInstance<UcurtmaEvent.RibbonTaken>().single().count)
        assertEquals(1, w.underWires)
        assertEquals(w.meters + UcurtmaWorld.RIBBON_POINTS, w.score)
        assertTrue(w.ribbonItems.isEmpty())

        // Sıyırma: çatının 2 cm üstünden; pilot yüksekliği tutar.
        val n = sky()
        val top = UcurtmaWorld.GROUND - 0.4f
        val line = top - UcurtmaWorld.KITE_R - 0.02f
        n.setKiteForTest(line)
        n.addBuildingForTest(ahead(n, 0.2f), 0.2f, 0.4f)
        var guard = 0
        var near = false
        while (guard++ < 200 && !near) {
            n.hold(n.kiteVy > 0f || n.kiteY > line)
            near = n.step().any { it is UcurtmaEvent.NearMiss }
        }
        assertTrue("sıyırma sayılmalı", near)
        assertEquals(1, n.nearMisses)
        assertEquals(UcurtmaStatus.RUNNING, n.status)
        // Yüksekten geçiş sıyırma değildir.
        val h = sky()
        h.setKiteForTest(0.3f)
        h.hold(true)
        h.addBuildingForTest(ahead(h, 0.2f), 0.2f, 0.4f)
        run(h, 120)
        assertEquals(0, h.nearMisses)
    }

    /** Değişmez: üretilen dünya her sütunda en az [UcurtmaWorld.MIN_GAP_Y] boşluk bırakır (rakipler hariç), tavan zorlukta bile. */
    @Test
    fun generatedWorldStaysPassable() {
        for (seed in 1L..12L) {
            for (units in listOf(0f, 60f, 200f)) {
                val w = UcurtmaWorld(seed)
                w.setDistanceForTest(units)
                val start = w.nextX
                w.generateForTest(start + 30f)
                var x = start
                while (x < w.nextX) {
                    val (_, gap) = UcurtmaBots.largestGap(UcurtmaWorld.CEIL + UcurtmaWorld.KITE_R, UcurtmaWorld.GROUND - UcurtmaWorld.KITE_R, UcurtmaBots.blockedAt(w, x))
                    assertTrue("tohum $seed mesafe $units x=$x boşluk $gap", gap >= UcurtmaWorld.MIN_GAP_Y)
                    x += 0.01f
                }
                if (units == 200f) {
                    assertTrue("çatı: ${w.buildings.size}", w.buildings.size >= 5)
                    assertTrue("tel: ${w.wires.size}", w.wires.size >= 2)
                    assertTrue("rakip: ${w.rivals.size}", w.rivals.size >= 1)
                    assertTrue("kurdele: ${w.ribbonItems.size}", w.ribbonItems.size >= 6)
                    assertTrue(w.rivals.all { it.baseY - it.amp >= 0.3f && it.baseY + it.amp <= 1.06f })
                    assertTrue(w.wires.all { it.y in 0.14f..1.21f })
                    assertTrue("yüksek tel üretilir", w.wires.any { it.y < 0.31f })
                }
            }
        }
    }

    @Test
    fun missionsProgressAndCompleteOnce() {
        val ms = listOf(Missions.at(0), Missions.at(1), Missions.at(4))
        assertEquals(Mission(0, MissionKind.DISTANCE, 150), ms[0])
        assertEquals(Mission(1, MissionKind.RIBBONS, 6), ms[1])
        assertEquals(Mission(4, MissionKind.CUTS, 1), ms[2])
        val w = UcurtmaWorld(2L, null, ms).also { it.freezeSpawnForTest() }
        w.hold(false)
        val ev = runUntil(w, 60 * 40) { it.any { e -> e is UcurtmaEvent.MissionDone } }
        assertEquals(listOf(ms[0]), ev.filterIsInstance<UcurtmaEvent.MissionDone>().map { it.mission })
        assertTrue(ev.any { it is UcurtmaEvent.Milestone && it.meters == 100 })
        assertEquals(listOf(true, false, false), w.hud().done)
        assertEquals(150, w.hud().progress[0])
        assertTrue("bir kez bildirilir", run(w, 120).none { it is UcurtmaEvent.MissionDone })
        assertEquals(Mission(5, MissionKind.DISTANCE, 300), Missions.at(5))
        assertEquals(Mission(9, MissionKind.CUTS, 2), Missions.at(9))
        assertEquals(emptyList<Gadget>(), Missions.unlocked(1))
        assertEquals(listOf(Gadget.TAIL), Missions.unlocked(2))
        assertEquals(listOf(Gadget.TAIL, Gadget.REEL), Missions.unlocked(5))
        assertEquals(Gadget.entries.toList(), Missions.unlocked(9))
    }

    /** Değişmez: dikkatli bir pilot ilk 300 m'yi çoğunlukla geçer; zorluk sonradan gelir. */
    @Test
    fun aCarefulPilotClearsTheOpening() {
        val pilot = UcurtmaBots.Pilot(reaction = 0.1f)
        val flights = (1L..12L).map { UcurtmaBots.fly(it, UcurtmaBots.Pilot(reaction = 0.1f)) }
        val past300 = flights.count { it.meters >= 300 }
        assertTrue("12 uçuştan en az 8'i 300 m'yi geçer: ${flights.map { it.meters }}", past300 >= 8)
        assertTrue(pilot.reaction > 0f)
    }
}
