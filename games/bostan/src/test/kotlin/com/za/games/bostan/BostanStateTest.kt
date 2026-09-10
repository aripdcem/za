package com.za.games.bostan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BostanStateTest {

    private fun level(waves: List<Wave> = listOf(Wave(listOf(Spawn(1f, 2, EnemyKind.KARGA)), false)), difficulty: BostanDifficulty = BostanDifficulty.KOLAY) =
        BostanLevel(7L, difficulty, waves, 1f, 3, 0)

    /** Dalgasız, yağmursuz sahne: kurallar tek tek ölçülür. */
    private fun sandbox(water: Int = 1000): BostanState {
        val s = BostanState(level())
        s.holdWavesForTest()
        s.stopRainForTest()
        s.setWaterForTest(water)
        return s
    }

    /** Koşul sağlanana dek adımlar; sağlandığı simülasyon anını, süre dolarsa −1 döner. */
    private fun runUntil(s: BostanState, seconds: Float, collect: MutableList<BostanEvent>, until: (BostanEvent) -> Boolean): Float {
        val n = (seconds / BostanState.STEP + 0.5f).toInt()
        repeat(n) {
            s.step()
            collect.addAll(s.events)
            if (s.events.any(until)) return s.time
        }
        return -1f
    }

    private fun run(s: BostanState, seconds: Float, collect: MutableList<BostanEvent>? = null) {
        val n = (seconds / BostanState.STEP + 0.5f).toInt()
        repeat(n) {
            s.step()
            collect?.addAll(s.events)
        }
    }

    @Test
    fun placementCostsWaterAndRespectsCellsAndCooldown() {
        val s = sandbox(water = 120)
        assertFalse("tarla dışı", s.place(DefenderKind.KUYU, 5, 0))
        assertFalse("tarla dışı", s.place(DefenderKind.KUYU, 0, BostanState.ROWS))
        assertTrue(s.place(DefenderKind.KUYU, 1, 6))
        assertEquals(70, s.water)
        assertFalse("dolu hücre", s.place(DefenderKind.KORKULUK, 1, 6))
        assertFalse("kart beklemede", s.place(DefenderKind.KUYU, 2, 6))
        assertTrue(s.hud().cooldowns[DefenderKind.KUYU.ordinal] > 0.9f)
        assertFalse("su yetmez (100)", s.place(DefenderKind.FISKIYE, 2, 4))
        run(s, DefenderKind.KUYU.cooldown + 0.1f)
        assertTrue("bekleme bitti", s.place(DefenderKind.KUYU, 2, 6))
        assertEquals(20, s.water)
        assertTrue(s.remove(2, 6))
        assertNull(s.defenderAt(2, 6))
        assertEquals("kürek su iade etmez", 20, s.water)
        assertFalse(s.remove(2, 6))
    }

    @Test
    fun wellsProduceWaterOnInterval() {
        val s = sandbox(water = 100)
        s.place(DefenderKind.KUYU, 2, 6)
        val ev = ArrayList<BostanEvent>()
        run(s, BostanState.WELL_INTERVAL - 0.1f, ev)
        assertEquals(50, s.water)
        assertTrue(ev.none { it is BostanEvent.Water })
        run(s, 0.2f, ev)
        assertEquals(50 + BostanState.WELL_WATER, s.water)
        assertEquals(1, ev.count { it is BostanEvent.Water })
    }

    @Test
    fun rainDropsCanBeCollectedAndExpire() {
        val s = BostanState(level())
        s.holdWavesForTest()
        val ev = ArrayList<BostanEvent>()
        run(s, BostanState.DROP_FIRST + 0.1f, ev)
        val fell = ev.filterIsInstance<BostanEvent.DropFell>()
        assertEquals(1, fell.size)
        assertEquals(1, s.drops.size)
        val d = s.drops.first()
        assertEquals(fell[0].lane, d.lane)
        assertFalse("başka hücre", s.collectDrop((d.lane + 1) % BostanState.COLS, d.row))
        val before = s.water
        assertTrue(s.collectDrop(d.lane, d.row))
        assertEquals(before + BostanState.DROP_WATER, s.water)
        assertTrue(s.drops.isEmpty())

        val s2 = sandbox()
        s2.dropForTest(0, 0)
        run(s2, BostanState.DROP_TTL + 0.1f)
        assertTrue("süresi dolan damla kalkar", s2.drops.isEmpty())
        assertFalse(s2.collectDrop(0, 0))
    }

    @Test
    fun sprinklerFiresOnlyAtItsLaneAndKillsCrow() {
        val s = sandbox()
        s.place(DefenderKind.FISKIYE, 2, 4)
        run(s, 2f)
        assertTrue("hedef yokken ateş yok", s.jets.isEmpty())
        val other = s.spawnForTest(EnemyKind.KARGA, 0, 1f)
        run(s, 2f)
        assertTrue("başka şerit hedef değil", s.jets.isEmpty())
        assertEquals(EnemyKind.KARGA.hp, other.hp, 1e-5f)

        val crow = s.spawnForTest(EnemyKind.KARGA, 2, 1f)
        val ev = ArrayList<BostanEvent>()
        run(s, 0.2f, ev)
        assertTrue("konur konmaz ateş", s.jets.isNotEmpty() || crow.hp < EnemyKind.KARGA.hp)
        run(s, 5 * BostanState.FIRE_INTERVAL + 2f, ev)
        val down = ev.filterIsInstance<BostanEvent.EnemyDown>()
        assertEquals(1, down.size)
        assertEquals(EnemyKind.KARGA, down[0].kind)
        assertEquals(1, s.kills)
        assertEquals(EnemyKind.KARGA.points, s.score)
        assertTrue(ev.count { it is BostanEvent.EnemyHit } >= 5)
        assertTrue("karga listeden düştü", s.enemies.none { it.lane == 2 })
        assertEquals("diğer şeritteki karga sağ", 1, s.enemies.size)
    }

    @Test
    fun scarecrowBlocksThenFallsAndEnemyResumes() {
        val s = sandbox()
        s.place(DefenderKind.KORKULUK, 1, 3)
        val goat = s.spawnForTest(EnemyKind.KECI, 1, 1.5f)
        run(s, 8f)
        val block = s.defenderAt(1, 3)!!
        assertTrue("kemiriyor", goat.biting)
        assertTrue("önünde durdu", goat.y <= 3 - 0.95f + 0.01f && goat.y > 1.9f)
        assertTrue(block.hp < DefenderKind.KORKULUK.hp)
        val ev = ArrayList<BostanEvent>()
        run(s, DefenderKind.KORKULUK.hp / EnemyKind.KECI.bite + 1f, ev)
        assertTrue(ev.any { it is BostanEvent.DefenderDown && it.kind == DefenderKind.KORKULUK })
        assertNull(s.defenderAt(1, 3))
        assertFalse(goat.biting)
        assertTrue("yürümeye devam", goat.y > 2.5f)
    }

    @Test
    fun unblockedEnemyCostsLifeAndThreeLivesLose() {
        val s = sandbox()
        val ev = ArrayList<BostanEvent>()
        s.spawnForTest(EnemyKind.TAVSAN, 4, 5f)
        run(s, (BostanState.HUT_Y - 5f) / EnemyKind.TAVSAN.speed + 0.2f, ev)
        val lost = ev.filterIsInstance<BostanEvent.LifeLost>()
        assertEquals(1, lost.size)
        assertEquals(2, lost[0].lives)
        assertEquals(4, lost[0].lane)
        assertEquals(2, s.lives)
        assertTrue(s.enemies.isEmpty())
        assertEquals("geçen puan vermez", 0, s.kills)
        s.spawnForTest(EnemyKind.TAVSAN, 0, 6f)
        s.spawnForTest(EnemyKind.TAVSAN, 1, 6f)
        run(s, 3f, ev)
        assertEquals(BostanStatus.LOST, s.status)
        assertTrue(ev.any { it is BostanEvent.Lost })
        assertFalse("bitince yerleşim yok", s.place(DefenderKind.KUYU, 0, 0))
    }

    @Test
    fun hiveStingsThreeLanesWithinReach() {
        val s = sandbox()
        s.place(DefenderKind.KOVAN, 2, 2)
        val near = s.spawnForTest(EnemyKind.KECI, 1, 1f)
        val far = s.spawnForTest(EnemyKind.KECI, 3, -0.5f)
        val out = s.spawnForTest(EnemyKind.KECI, 4, 1f)
        run(s, 0.1f)
        assertEquals("kapsamda anında", EnemyKind.KECI.hp - BostanState.HIVE_DAMAGE, near.hp, 1e-4f)
        assertEquals("iki satır ötesi kapsam dışı", EnemyKind.KECI.hp, far.hp, 1e-4f)
        assertEquals("iki şerit ötesi kapsam dışı", EnemyKind.KECI.hp, out.hp, 1e-4f)
        run(s, BostanState.HIVE_INTERVAL)
        assertTrue("aralıkla tekrar", near.hp <= EnemyKind.KECI.hp - 2 * BostanState.HIVE_DAMAGE + 1e-4f)
    }

    @Test
    fun trapBlocksWhileArmingThenBlastsAndIsConsumed() {
        val s = sandbox()
        s.place(DefenderKind.TUZAK, 3, 2)
        val crow = s.spawnForTest(EnemyKind.KARGA, 3, 1.2f)
        run(s, 1f)
        assertTrue("kurulmadan önce kemirilir", crow.biting)
        assertFalse(s.defenderAt(3, 2)!!.armed)
        assertTrue(s.defenderAt(3, 2)!!.hp < DefenderKind.TUZAK.hp)
        val ev = ArrayList<BostanEvent>()
        run(s, BostanState.TRAP_ARM, ev)
        assertTrue("kurulunca kemirene patlar", ev.any { it is BostanEvent.TrapBlast })
        assertNull("tuzak tükendi", s.defenderAt(3, 2))
        assertTrue(ev.none { it is BostanEvent.DefenderDown })
        assertTrue("karga patlamada öldü", ev.any { it is BostanEvent.EnemyDown && it.kind == EnemyKind.KARGA })

        // Domuz 2/s kemirir: 4 canlık tuzak 3 s'de kurulamadan biter.
        val s3 = sandbox()
        s3.place(DefenderKind.TUZAK, 3, 2)
        s3.spawnForTest(EnemyKind.DOMUZ, 3, 1.2f)
        val ev3 = ArrayList<BostanEvent>()
        run(s3, BostanState.TRAP_ARM + 0.1f, ev3)
        assertTrue(ev3.any { it is BostanEvent.DefenderDown && it.kind == DefenderKind.TUZAK })
        assertTrue(ev3.none { it is BostanEvent.TrapBlast })

        val s2 = sandbox()
        s2.place(DefenderKind.TUZAK, 0, 3)
        run(s2, BostanState.TRAP_ARM + 0.1f)
        assertTrue(s2.defenderAt(0, 3)!!.armed)
        val bear = s2.spawnForTest(EnemyKind.AYI, 0, 3f - 0.95f - 0.05f)
        run(s2, 1.5f)
        assertNull(s2.defenderAt(0, 3))
        assertEquals("ayı 30 hasar yer, sağ kalır", EnemyKind.AYI.hp - BostanState.TRAP_DAMAGE, bear.hp, 1e-4f)
    }

    @Test
    fun wavesFlowClearBonusAndWin() {
        val waves = listOf(
            Wave(listOf(Spawn(1f, 2, EnemyKind.KARGA)), false),
            Wave(listOf(Spawn(1f, 2, EnemyKind.KARGA), Spawn(2f, 2, EnemyKind.KARGA)), true),
        )
        val s = BostanState(level(waves))
        s.stopRainForTest()
        s.setWaterForTest(1000)
        s.place(DefenderKind.FISKIYE, 2, 4)
        s.place(DefenderKind.KORKULUK, 2, 1)
        val ev = ArrayList<BostanEvent>()
        run(s, BostanState.PREP - 0.1f, ev)
        assertEquals(0, s.hud().wave)
        assertTrue(abs(s.hud().nextWaveIn - 0.1f) < 0.05f)
        run(s, 0.2f, ev)
        val start = ev.filterIsInstance<BostanEvent.WaveStart>()
        assertEquals(1, start.size)
        assertEquals(0, start[0].index)
        assertFalse(start[0].big)
        assertEquals(1, s.hud().wave)
        run(s, 1f, ev)
        assertEquals(1, ev.count { it is BostanEvent.EnemySpawned })
        val clearedAt = runUntil(s, 25f, ev) { it is BostanEvent.WaveClear }
        assertTrue("ilk dalga temizlendi", clearedAt >= 0f)
        assertEquals(EnemyKind.KARGA.points + BostanState.WAVE_BONUS, s.score)
        assertEquals(1, ev.count { it is BostanEvent.WaveStart })
        assertTrue(abs(s.hud().nextWaveIn - BostanState.WAVE_GAP) < 0.05f)
        val startedAt = runUntil(s, 10f, ev) { it is BostanEvent.WaveStart }
        assertEquals("ikinci dalga bekleme payından sonra", BostanState.WAVE_GAP, startedAt - clearedAt, 0.05f)
        val starts = ev.filterIsInstance<BostanEvent.WaveStart>()
        assertEquals(2, starts.size)
        assertTrue(starts[1].big)
        assertEquals(BostanStatus.RUNNING, s.status)
        run(s, 40f, ev)
        assertEquals(BostanStatus.WON, s.status)
        assertTrue(ev.any { it is BostanEvent.Won })
        assertEquals(3, s.kills)
        assertEquals(3 * EnemyKind.KARGA.points + 2 * BostanState.WAVE_BONUS + 3 * BostanState.LIFE_BONUS + s.water / 5, s.score)
        assertEquals(0f, s.hud().nextWaveIn, 1e-5f)
    }

    @Test
    fun unclearedWaveYieldsToNextAfterGrace() {
        val waves = listOf(
            Wave(listOf(Spawn(1f, 0, EnemyKind.AYI)), false),
            Wave(listOf(Spawn(1f, 4, EnemyKind.KARGA)), false),
        )
        val s = BostanState(level(waves))
        s.stopRainForTest()
        s.setWaterForTest(1000)
        s.place(DefenderKind.KORKULUK, 0, 1)
        val ev = ArrayList<BostanEvent>()
        run(s, BostanState.PREP + 1f + BostanState.WAVE_GRACE - 0.5f, ev)
        assertEquals(1, ev.count { it is BostanEvent.WaveStart })
        run(s, 1f, ev)
        assertEquals("ayı sağken de ikinci dalga geldi", 2, ev.count { it is BostanEvent.WaveStart })
        assertTrue(s.enemies.any { it.kind == EnemyKind.AYI })
        assertEquals(2, s.hud().wave)
    }

    @Test
    fun generatorIsDeterministicAndUnlocksKindsByWave() {
        for (d in BostanDifficulty.entries) {
            val a = BostanGenerator.waves(11L, d, 1f)
            val b = BostanGenerator.waves(11L, d, 1f)
            assertEquals(d.waves, a.size)
            for (w in a.indices) {
                assertEquals(a[w].spawns, b[w].spawns)
                assertEquals(BostanGenerator.isBig(w), a[w].big)
                for (sp in a[w].spawns) {
                    assertTrue("${sp.kind} dalga $w'de erken", BostanGenerator.unlockAt.getValue(sp.kind) <= w)
                    assertTrue(sp.lane in 0 until BostanState.COLS)
                }
                val spent = a[w].spawns.sumOf { it.kind.cost.toDouble() }.toFloat()
                assertTrue("bütçe aşıldı", spent <= BostanGenerator.budget(d, w, 1f) + 1e-3f)
                assertTrue("dalga boş", a[w].spawns.isNotEmpty())
                val ats = a[w].spawns.map { it.at }
                assertEquals("doğumlar zamana göre sıralı", ats.sorted(), ats)
            }
            assertNotEquals("tohum değişince dalga değişir", a[0].spawns, BostanGenerator.waves(12L, d, 1f)[0].spawns)
            val scaled = BostanGenerator.waves(11L, d, 0.5f)
            assertTrue("küçük ölçek daha az saldırgan", scaled.sumOf { it.spawns.size } < a.sumOf { it.spawns.size })
        }
    }

    @Test
    fun difficultiesOrderByEnemyCount() {
        val seeds = 1L..10L
        val avg = BostanDifficulty.entries.map { d -> seeds.map { BostanGenerator.waves(it, d, 1f).sumOf { w -> w.spawns.size } }.average() }
        assertTrue("kolay < orta: $avg", avg[0] < avg[1])
        assertTrue("orta < zor: $avg", avg[1] < avg[2])
    }

    @Test
    fun everyGeneratedLevelIsWinnableByExpert() {
        for (d in BostanDifficulty.entries) {
            for (seed in 1L..6L) {
                val level = BostanGenerator.generate(seed, d)
                assertNotNull(level)
                assertTrue("ölçek aralıkta", level.scale in BostanGenerator.SCALES.last()..1f)
                val r = BostanExpert.play(level)
                assertEquals("$d/$seed ölçek ${level.scale}: uzman kazanmalı", BostanStatus.WON, r.status)
                assertEquals(level.expertLives, r.lives)
                assertEquals(level.expertScore, r.score)
                assertTrue(r.time < 900f)
            }
        }
    }

    @Test
    fun generatedLevelIsSameForSameSeed() {
        val a = BostanGenerator.generate(3L, BostanDifficulty.ORTA)
        val b = BostanGenerator.generate(3L, BostanDifficulty.ORTA)
        assertEquals(a.scale, b.scale, 0f)
        assertEquals(a.expertScore, b.expertScore)
        assertEquals(a.waves.map { it.spawns }, b.waves.map { it.spawns })
    }

    @Test
    fun dailySeedVariesByDay() {
        assertNotEquals(BostanState.dailySeed(20_000), BostanState.dailySeed(20_001))
        assertEquals(BostanState.dailySeed(20_000), BostanState.dailySeed(20_000))
        assertNotEquals(BostanState.mix(1L, 1), BostanState.mix(1L, 2))
    }
}
