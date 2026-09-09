package com.za.games.filo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FiloWorldTest {

    private fun run(world: FiloWorld, frames: Int): List<FiloEvent> {
        val out = ArrayList<FiloEvent>()
        repeat(frames) { out += world.step() }
        return out
    }

    /** En çok [max] kare ilerler; [pred] biriken olaylar için doğru olunca durur. */
    private fun runUntil(world: FiloWorld, max: Int, pred: (List<FiloEvent>) -> Boolean): List<FiloEvent> {
        val out = ArrayList<FiloEvent>()
        repeat(max) {
            out += world.step()
            if (pred(out)) return out
        }
        throw AssertionError("beklenen durum $max karede oluşmadı; olaylar: ${out.filter { it !is FiloEvent.Shot }}")
    }

    /** Dalga üretimi kapalı, boş arena. */
    private fun arena(seed: Long = 1L): FiloWorld = FiloWorld(seed).also { it.freezeWavesForTest() }

    @Test
    fun sameSeedSameInputsSameRun() {
        val a = FiloWorld(11L)
        val b = FiloWorld(11L)
        a.setLivesForTest(50)
        b.setLivesForTest(50)
        val rng = Random(5)
        repeat(2400) { i ->
            val x = rng.nextFloat()
            a.steerTo(x)
            b.steerTo(x)
            if (i % 500 == 499) assertEquals(a.bomb(), b.bomb())
            assertEquals(a.step(), b.step())
        }
        assertEquals(a.score, b.score)
        assertEquals(a.lives, b.lives)
        assertEquals(a.wave, b.wave)
        assertEquals(a.enemies.map { Triple(it.id, it.x, it.y) }, b.enemies.map { Triple(it.id, it.x, it.y) })
        assertEquals(a.enemyBullets.map { it.x to it.y }, b.enemyBullets.map { it.x to it.y })
        assertTrue("40 saniyede en az 2. dalga: ${a.wave}", a.wave >= 2)
        assertTrue("puan birikmeli: ${a.score}", a.score > 0L)
    }

    @Test
    fun autoFireMatchesWeaponLevel() {
        for (level in 1..FiloWorld.MAX_WEAPON) {
            val w = arena()
            w.setWeaponForTest(level)
            val ev = w.step()
            assertTrue(ev.contains(FiloEvent.Shot))
            assertEquals("seviye $level", level, w.bullets.size)
            assertTrue(w.bullets.all { it.vy < 0f })
        }
        val w = arena()
        val shots = run(w, 60).count { it is FiloEvent.Shot }
        assertTrue("saniyede yaklaşık 6 atış: $shots", shots in 5..7)
    }

    @Test
    fun bulletKillsDroneAndScores() {
        val w = arena()
        w.spawnForTest(EnemyKind.DRONE, 0.5f, 1.0f)
        val ev = runUntil(w, 40) { it.any { e -> e is FiloEvent.EnemyDown } }
        val down = ev.filterIsInstance<FiloEvent.EnemyDown>().single()
        assertEquals(EnemyKind.DRONE, down.kind)
        assertEquals(100L, down.points)
        assertEquals(100L, w.score)
        assertEquals(1, w.kills)
        assertEquals(1, w.chain)
        assertTrue(w.enemies.isEmpty())
        assertTrue("çarpan mermi tüketilmeli", w.bullets.size <= 1)
    }

    @Test
    fun tankNeedsSeveralHits() {
        val w = arena()
        val tank = w.spawnForTest(EnemyKind.TANK, 0.5f, 1.0f)
        val ev = runUntil(w, 120) { it.any { e -> e is FiloEvent.EnemyDown } }
        assertEquals(EnemyKind.TANK.hp - 1, ev.count { it is FiloEvent.EnemyHit })
        assertEquals(300L, w.score)
        assertFalse(tank.alive)
    }

    @Test
    fun chainRaisesMultiplierAndDecays() {
        val w = arena()
        for (i in 0 until 4) w.spawnForTest(EnemyKind.DRONE, 0.5f, 1.0f - i * 0.1f)
        runUntil(w, 120) { ev -> ev.count { it is FiloEvent.EnemyDown } == 4 }
        assertEquals(4, w.chain)
        assertEquals(2, w.multiplier)
        assertEquals(2, w.hud().multiplier)
        assertEquals(500L, w.score) // 100 + 100 + 100 + 200
        run(w, 125)
        assertEquals(0, w.chain)
        assertEquals(1, w.multiplier)
    }

    @Test
    fun enemyBulletCostsLifeAndWeaponThenInvulnerabilityExpires() {
        val w = arena()
        w.setWeaponForTest(3)
        w.addEnemyBulletForTest(0.5f, 1.3f, 0f, FiloWorld.ENEMY_BULLET_SPEED)
        val ev = runUntil(w, 30) { it.any { e -> e is FiloEvent.PlayerHit } }
        assertTrue(ev.contains(FiloEvent.PlayerHit(2)))
        assertEquals(2, w.lives)
        assertEquals(2, w.weapon)
        assertEquals(0, w.chain)
        assertTrue(w.invuln > 0f)
        assertTrue(w.hud().invulnerable)
        assertTrue(w.enemyBullets.isEmpty())
        // Dokunulmazlık sürerken yeni mermi işlemez, bitince işler.
        w.addEnemyBulletForTest(0.5f, FiloWorld.PLAYER_Y, 0f, 0f)
        run(w, 30)
        assertEquals(2, w.lives)
        run(w, 100)
        assertEquals(1, w.lives)
        assertEquals(1, w.weapon)
    }

    @Test
    fun shieldAbsorbsOneHit() {
        val w = arena()
        w.grantShieldForTest()
        assertTrue(w.hud().shield)
        w.addEnemyBulletForTest(0.5f, FiloWorld.PLAYER_Y, 0f, 0f)
        val ev = w.step()
        assertTrue(ev.contains(FiloEvent.ShieldUsed))
        assertFalse(ev.any { it is FiloEvent.PlayerHit })
        assertFalse(w.shield)
        assertEquals(FiloWorld.LIVES, w.lives)
        assertTrue(w.invuln > 0f)
        assertTrue(w.enemyBullets.isEmpty())
    }

    @Test
    fun rammingAnEnemyCostsALifeAndDestroysIt() {
        val w = arena()
        w.spawnForTest(EnemyKind.WASP, 0.5f, FiloWorld.PLAYER_Y - 0.02f)
        val ev = w.step()
        assertTrue(ev.contains(FiloEvent.PlayerHit(2)))
        assertTrue(ev.any { it is FiloEvent.EnemyDown && it.kind == EnemyKind.WASP })
        assertTrue(w.enemies.isEmpty())
        assertEquals(2, w.lives)
    }

    @Test
    fun bombClearsBulletsAndDamagesEnemies() {
        val w = arena()
        val tank = w.spawnForTest(EnemyKind.TANK, 0.2f, 0.5f)
        w.spawnForTest(EnemyKind.DRONE, 0.8f, 0.6f)
        val above = w.spawnForTest(EnemyKind.DRONE, 0.5f, -0.3f)
        repeat(5) { w.addEnemyBulletForTest(0.1f + it * 0.2f, 0.9f, 0f, 0.3f) }
        assertTrue(w.bomb())
        assertEquals(FiloWorld.BOMBS - 1, w.bombs)
        assertTrue(w.enemyBullets.isEmpty())
        val ev = w.step()
        assertTrue(ev.contains(FiloEvent.Bomb))
        assertTrue(ev.any { it is FiloEvent.EnemyDown && it.kind == EnemyKind.DRONE })
        assertEquals(EnemyKind.TANK.hp - FiloWorld.BOMB_DAMAGE, tank.hp)
        assertTrue("ekran dışı düşman etkilenmez", above.alive)
        assertTrue(w.bomb())
        assertFalse(tank.alive)
        assertFalse("bomba kalmadı", w.bomb())
        assertEquals(0, w.bombs)
    }

    @Test
    fun powerUpsApply() {
        val w = arena()
        fun give(kind: PowerKind): List<FiloEvent> {
            w.addPowerForTest(kind, 0.5f, FiloWorld.PLAYER_Y)
            return w.step()
        }
        assertTrue(give(PowerKind.WEAPON).contains(FiloEvent.PowerUp(PowerKind.WEAPON)))
        assertEquals(2, w.weapon)
        give(PowerKind.WEAPON)
        give(PowerKind.WEAPON)
        assertEquals(FiloWorld.MAX_WEAPON, w.weapon)
        give(PowerKind.BOMB)
        assertEquals(FiloWorld.BOMBS + 1, w.bombs)
        give(PowerKind.BOMB)
        give(PowerKind.BOMB)
        assertEquals(FiloWorld.MAX_BOMBS, w.bombs)
        val before = w.score
        give(PowerKind.SCORE)
        assertEquals(before + FiloWorld.SCORE_GIFT, w.score)
        give(PowerKind.SHIELD)
        assertTrue(w.shield)
        assertTrue(w.powers.isEmpty())
    }

    @Test
    fun powerDriftsDownToThePlayer() {
        val w = arena()
        w.addPowerForTest(PowerKind.SHIELD, 0.5f, 0.5f)
        run(w, 60)
        assertFalse(w.shield)
        assertTrue(w.powers.single().y > 0.5f)
        runUntil(w, 300) { it.contains(FiloEvent.PowerUp(PowerKind.SHIELD)) }
        assertTrue(w.shield)
        assertTrue(w.powers.isEmpty())
    }

    @Test
    fun firstWaveStartsSpawnsAndClears() {
        val w = FiloWorld(3L)
        w.setLivesForTest(99)
        assertEquals(0, w.wave)
        val start = runUntil(w, 45) { it.any { e -> e is FiloEvent.WaveStart } }
        assertTrue(start.contains(FiloEvent.WaveStart(1, false)))
        assertEquals(1, w.wave)
        assertTrue("ilk dalga ~0.6 s sonra: ${w.frames}", w.frames in 30..40)
        runUntil(w, 120) { w.enemies.isNotEmpty() }
        assertTrue(w.hud().waveProgress < 1f)
        val clear = runUntil(w, 3600) { it.any { e -> e is FiloEvent.WaveClear } }
        assertTrue(clear.contains(FiloEvent.WaveClear(1, FiloWorld.WAVE_BONUS)))
        assertTrue(w.score >= FiloWorld.WAVE_BONUS)
        assertEquals(1f, w.hud().waveProgress, 0f)
        val next = runUntil(w, 120) { it.any { e -> e is FiloEvent.WaveStart } }
        assertTrue(next.contains(FiloEvent.WaveStart(2, false)))
        assertEquals(2, w.wave)
    }

    @Test
    fun laterWavesBringTougherEnemies() {
        val w = FiloWorld(21L)
        w.setLivesForTest(99)
        w.jumpToWaveForTest(21)
        assertTrue(w.step().contains(FiloEvent.WaveStart(21, false)))
        val kinds = HashSet<EnemyKind>()
        repeat(900) {
            w.step()
            w.enemies.forEach { kinds += it.kind }
        }
        assertTrue("21. dalgada ağır düşmanlar olmalı: $kinds", EnemyKind.TANK in kinds || EnemyKind.WASP in kinds)
    }

    @Test
    fun bossWaveSpawnsBossAndClearsWhenBossDies() {
        val w = FiloWorld(9L)
        w.setLivesForTest(99)
        w.jumpToWaveForTest(FiloWorld.BOSS_EVERY)
        assertTrue(w.step().contains(FiloEvent.WaveStart(FiloWorld.BOSS_EVERY, true)))
        assertNull(w.boss)
        runUntil(w, 90) { w.boss != null }
        val boss = w.boss!!
        assertEquals(EnemyKind.BOSS, boss.kind)
        assertTrue(boss.maxHp >= 30)
        assertEquals(1f, w.hud().bossHp, 0f)
        runUntil(w, 600) { w.enemyBullets.isNotEmpty() }
        assertTrue("patron yerleşmeli: ${boss.y}", boss.y > 0.1f)
        w.damageForTest(boss, boss.hp)
        assertTrue(w.enemyBullets.isEmpty())
        val ev = w.step()
        assertTrue(ev.any { it is FiloEvent.BossDown })
        assertTrue(ev.any { it is FiloEvent.EnemyDown && it.kind == EnemyKind.BOSS })
        assertNull(w.boss)
        assertTrue(w.powers.any { it.kind == PowerKind.BOMB })
        assertEquals(-1f, w.hud().bossHp, 0f)
        assertTrue(w.score >= EnemyKind.BOSS.points)
        val clear = runUntil(w, 1200) { it.any { e -> e is FiloEvent.WaveClear } }
        assertTrue(clear.contains(FiloEvent.WaveClear(FiloWorld.BOSS_EVERY, FiloWorld.WAVE_BONUS * FiloWorld.BOSS_EVERY)))
    }

    @Test
    fun escapingEnemiesLeaveWithoutScore() {
        val w = arena()
        w.spawnForTest(EnemyKind.DRONE, 0.2f, FiloWorld.ESCAPE_Y + 0.01f)
        val sweep = w.spawnForTest(EnemyKind.WASP, -0.16f, 0.5f, pattern = Pattern.SWEEP)
        val ev = w.step()
        assertTrue(w.enemies.isEmpty())
        assertFalse(sweep.alive)
        assertEquals(0, w.kills)
        assertEquals(0L, w.score)
        assertFalse(ev.any { it is FiloEvent.EnemyDown })
    }

    @Test
    fun bulletsIgnoreEnemiesAboveTheScreen() {
        val w = arena()
        w.spawnForTest(EnemyKind.DRONE, 0.5f, -0.05f)
        run(w, 90)
        assertEquals(0, w.kills)
        assertEquals(1, w.enemies.size)
    }

    @Test
    fun enemiesAimAtThePlayer() {
        val w = arena()
        w.steerTo(0.3f)
        w.spawnForTest(EnemyKind.WASP, 0.7f, 0.5f, fireEvery = 0.5f)
        runUntil(w, 60) { w.enemyBullets.isNotEmpty() }
        val b = w.enemyBullets.single()
        assertTrue("aşağı ve sola gitmeli: vx=${b.vx} vy=${b.vy}", b.vx < 0f && b.vy > 0f)

        val w2 = arena()
        w2.spawnForTest(EnemyKind.TANK, 0.8f, 0.5f, fireEvery = 0.5f)
        runUntil(w2, 60) { w2.enemyBullets.isNotEmpty() }
        assertEquals(3, w2.enemyBullets.size)
        assertTrue(w2.enemyBullets.all { it.vy > 0f })
    }

    @Test
    fun gameOverStopsTheSimulation() {
        val w = arena()
        w.setLivesForTest(1)
        w.addEnemyBulletForTest(0.5f, FiloWorld.PLAYER_Y, 0f, 0f)
        val ev = w.step()
        assertTrue(ev.contains(FiloEvent.PlayerHit(0)))
        assertTrue(ev.contains(FiloEvent.Over))
        assertEquals(FiloStatus.OVER, w.status)
        assertEquals(FiloStatus.OVER, w.hud().status)
        val frames = w.frames
        assertTrue(w.step().isEmpty())
        assertEquals(frames, w.frames)
        assertFalse(w.bomb())
    }

    @Test
    fun steeringStaysInsideTheArena() {
        val w = arena()
        w.steerTo(-1f)
        w.step()
        assertEquals(FiloWorld.PLAYER_MARGIN, w.playerX, 1e-6f)
        w.steerTo(5f)
        w.step()
        assertEquals(FiloWorld.WIDTH - FiloWorld.PLAYER_MARGIN, w.playerX, 1e-6f)
        w.steerTo(0.5f)
        w.steerBy(0.1f)
        w.step()
        assertEquals(0.6f, w.playerX, 1e-6f)
        w.steerBy(-1f)
        w.step()
        assertEquals(FiloWorld.PLAYER_MARGIN, w.playerX, 1e-6f)
    }

    @Test
    fun hudSummarizesInitialState() {
        val h = arena().hud()
        assertEquals(FiloWorld.LIVES, h.lives)
        assertEquals(FiloWorld.BOMBS, h.bombs)
        assertEquals(1, h.weapon)
        assertEquals(1, h.multiplier)
        assertEquals(0L, h.score)
        assertEquals(-1f, h.bossHp, 0f)
        assertFalse(h.shield)
        assertFalse(h.invulnerable)
        assertEquals(FiloStatus.RUNNING, h.status)
    }

    @Test
    fun difficultyRampsAndDailySeedIsStable() {
        assertEquals(0f, FiloWorld.difficulty(0), 0f)
        assertEquals(0f, FiloWorld.difficulty(1), 0f)
        assertEquals(1f, FiloWorld.difficulty(26), 0f)
        assertEquals(1f, FiloWorld.difficulty(100), 0f)
        assertEquals(FiloWorld.dailySeed(20_000L), FiloWorld.dailySeed(20_000L))
        assertNotEquals(FiloWorld.dailySeed(20_000L), FiloWorld.dailySeed(20_001L))
    }

    /**
     * Silah yükseltmesi asla gerileme olmamalı. Silah 3'ün yan mermileri fazla
     * açılıysa patron menzilinde ıskalar ve silah 3, silah 2'den zayıf düşer;
     * bu test o dengesizliği yakalar (bkz. FiloWorld.SPREAD_VX).
     */
    @Test
    fun silahYukseltmesiPatronHasariniDusurmez() {
        val kare = (1..FiloWorld.MAX_WEAPON).map { silah ->
            val w = FiloWorld(11L)
            w.jumpToWaveForTest(10)
            w.setLivesForTest(999_999)
            var f = 0
            var dustu = -1
            while (f < 60 * 180 && dustu < 0) {
                w.boss?.let { w.steerTo(it.x) }
                w.setWeaponForTest(silah)
                for (e in w.step()) {
                    if (e is FiloEvent.EnemyDown && e.kind == EnemyKind.BOSS) dustu = f
                }
                f++
            }
            assertTrue("silah $silah patronu indiremedi", dustu > 0)
            dustu
        }
        assertTrue("silah 2 silah 1'den hızlı olmalı (${kare[1]} / ${kare[0]} kare)", kare[1] < kare[0])
        assertTrue("silah 3 silah 2'den hızlı olmalı (${kare[2]} / ${kare[1]} kare)", kare[2] < kare[1])
    }
}
