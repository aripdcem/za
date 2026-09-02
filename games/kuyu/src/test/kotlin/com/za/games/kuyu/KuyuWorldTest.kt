package com.za.games.kuyu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class KuyuWorldTest {

    private val w = KuyuGen.WIDTH
    private val h = KuyuGen.CHUNK_ROWS

    /**
     * Denetlenebilir kuyu: dış duvarlar, [floors] satırlarında kırılmaz tam zemin,
     * [blocks] tekil hücreler; düşman doğmaz.
     */
    private fun shaft(
        floors: Set<Int> = emptySet(),
        blocks: Map<Pair<Int, Int>, Tile> = emptyMap(),
    ): (Int) -> KuyuChunk = { index ->
        val tiles = Array(h * w) { Tile.EMPTY }
        for (r in 0 until h) {
            tiles[r * w] = Tile.WALL
            tiles[r * w + w - 1] = Tile.WALL
            val row = index * h + r
            if (row in floors) for (c in 1 until w - 1) tiles[r * w + c] = Tile.WALL
            for ((pos, t) in blocks) if (pos.first == row) tiles[r * w + pos.second] = t
        }
        KuyuChunk(index, tiles, emptyList())
    }

    private val fire = KuyuInput(fire = true)
    private val none = KuyuInput.NONE

    private fun bat(col: Int, row: Float) =
        Enemy(EnemyKind.BAT, col + (1f - EnemyKind.BAT.w) / 2f, row, speedMul = 0f)

    private fun onFloor(kind: EnemyKind, col: Int, floorRow: Int) =
        Enemy(kind, col + (1f - kind.w) / 2f, floorRow - kind.h, speedMul = 0f)

    private fun run(world: KuyuWorld, frames: Int, input: KuyuInput = none): List<KuyuEvent> {
        val out = ArrayList<KuyuEvent>()
        repeat(frames) { out += world.step(input) }
        return out
    }

    @Test
    fun startsStandingOnPlatform() {
        val world = KuyuWorld(1L)
        assertTrue(world.player.grounded)
        assertEquals(0, world.depth)
        assertEquals(0L, world.score)
        assertEquals(KuyuWorld.AMMO, world.player.ammo)
        assertEquals(KuyuWorld.MAX_HP, world.player.hp)
        assertEquals(KuyuStatus.RUNNING, world.status)
        run(world, 30)
        assertTrue(world.player.grounded)
        assertEquals(0, world.depth)
    }

    @Test
    fun jumpReachesAboutThreeTilesAndCannotDoubleJump() {
        val world = KuyuWorld(1L, shaft(floors = setOf(12)))
        world.player.x = 5f
        world.player.y = 12f - KuyuWorld.PLAYER_H
        run(world, 2)
        assertTrue(world.player.grounded)
        val startY = world.player.y
        val events = ArrayList<KuyuEvent>()
        events += world.step(fire)
        var minY = world.player.y
        repeat(20) { events += world.step(none); minY = minOf(minY, world.player.y) }
        // Havada yeniden basmak zıplatmaz (ateş eder).
        events += world.step(fire)
        repeat(60) { events += world.step(fire); minY = minOf(minY, world.player.y) }
        val height = startY - minY
        assertTrue("zıplama yüksekliği $height", height in 2.6f..3.4f)
        assertEquals(1, events.count { it == KuyuEvent.Jump })
        assertTrue(events.any { it == KuyuEvent.Shot })
        assertTrue(world.player.grounded)
        assertEquals(startY, world.player.y, 0.01f)
    }

    @Test
    fun jumpAtStartStaysBelowCeiling() {
        val world = KuyuWorld(3L)
        world.step(fire)
        var minY = world.player.y
        repeat(60) { world.step(none); minY = minOf(minY, world.player.y) }
        assertTrue(minY >= 0f)
    }

    @Test
    fun fallIsCappedAndBootsSlowItDown() {
        val free = KuyuWorld(1L, shaft())
        free.player.x = 5f
        free.player.y = 3f
        var maxVy = 0f
        repeat(90) { free.step(none); maxVy = maxOf(maxVy, free.player.vy) }
        assertTrue(maxVy <= KuyuWorld.MAX_FALL + 0.01f)
        assertTrue(maxVy >= KuyuWorld.MAX_FALL - 0.01f)

        val boots = KuyuWorld(1L, shaft())
        boots.player.x = 5f
        boots.player.y = 3f
        val events = run(boots, 90, fire)
        assertEquals(KuyuWorld.AMMO, events.count { it == KuyuEvent.Shot })
        assertEquals(0, boots.player.ammo)
        assertTrue("botlar düşüşü yavaşlatmalı", boots.player.y < free.player.y - 3f)
    }

    @Test
    fun landingRefillsAmmoAndReportsFall() {
        val world = KuyuWorld(1L, shaft(floors = setOf(20)))
        world.player.x = 5f
        world.player.y = 3f
        val events = run(world, 240, fire)
        val land = events.filterIsInstance<KuyuEvent.Land>().first()
        assertTrue("iniş yüksekliği ${land.fallRows}", land.fallRows >= 15)
        assertEquals(KuyuWorld.AMMO, world.player.ammo)
        assertTrue(world.player.grounded)
        assertEquals(20f - KuyuWorld.PLAYER_H, world.player.y, 0.01f)
        assertEquals(18, world.depth)
        assertEquals(18L, world.score)
    }

    @Test
    fun bulletsBreakBlocksAndGemBlocksDropGems() {
        val world = KuyuWorld(
            1L,
            shaft(floors = setOf(12), blocks = mapOf((8 to 5) to Tile.GEM_BLOCK, (9 to 5) to Tile.BLOCK)),
        )
        world.player.x = 5f + (1f - KuyuWorld.PLAYER_W) / 2f
        world.player.y = 3f
        val events = run(world, 40, fire)
        val breaks = events.filterIsInstance<KuyuEvent.BlockBreak>()
        assertEquals(listOf(8 to true, 9 to false), breaks.map { it.row to it.gems })
        assertEquals(Tile.EMPTY, world.tile(8, 5))
        assertEquals(Tile.EMPTY, world.tile(9, 5))
        assertTrue(world.gems.size + world.gemsCollected == 2)
    }

    @Test
    fun bulletKillsEnemyAirborneKillCountsCombo() {
        val world = KuyuWorld(1L, shaft(floors = setOf(12)))
        world.enemies += onFloor(EnemyKind.BLOB, 5, 12)
        world.player.x = 5f + (1f - KuyuWorld.PLAYER_W) / 2f
        world.player.y = 3f
        val events = run(world, 30, fire)
        val kill = events.filterIsInstance<KuyuEvent.Kill>().single()
        assertEquals(EnemyKind.BLOB, kill.kind)
        assertFalse(kill.stomp)
        assertEquals(1, world.combo)
        assertEquals(EnemyKind.BLOB.gems, world.gems.size + world.gemsCollected)
        // İniş: kombo 3'ün altında, bonus yok; taşlar mıknatısla toplanır.
        run(world, 240, none)
        assertTrue(events.none { it is KuyuEvent.Combo })
        assertEquals(0, world.combo)
        assertEquals(EnemyKind.BLOB.gems, world.gemsCollected)
    }

    @Test
    fun stompKillsBouncesAndRefillsAmmo() {
        val world = KuyuWorld(1L, shaft(floors = setOf(12)))
        world.enemies += onFloor(EnemyKind.BLOB, 5, 12)
        world.player.x = 5f + (1f - KuyuWorld.PLAYER_W) / 2f
        world.player.y = 7f
        world.player.ammo = 2
        var stomped = false
        repeat(60) {
            val events = world.step(none)
            if (events.any { it == KuyuEvent.Stomp }) {
                stomped = true
                assertTrue(events.filterIsInstance<KuyuEvent.Kill>().single().stomp)
                assertTrue(world.player.vy < 0f)
                assertEquals(KuyuWorld.AMMO, world.player.ammo)
                assertEquals(1, world.combo)
                assertEquals(KuyuWorld.MAX_HP, world.player.hp)
            }
        }
        assertTrue(stomped)
    }

    @Test
    fun stompingSpikyHurts() {
        val world = KuyuWorld(1L, shaft(floors = setOf(12)))
        world.enemies += onFloor(EnemyKind.SPIKY, 5, 12)
        world.player.x = 5f + (1f - KuyuWorld.PLAYER_W) / 2f
        world.player.y = 7f
        val events = run(world, 60)
        assertTrue(events.any { it == KuyuEvent.Hurt })
        assertTrue(events.none { it is KuyuEvent.Kill })
        assertEquals(KuyuWorld.MAX_HP - 1, world.player.hp)
        assertTrue(world.enemies.any { it.kind == EnemyKind.SPIKY && it.alive })
    }

    @Test
    fun contactHurtsOnceThenInvincibleThenGameOver() {
        val world = KuyuWorld(1L, shaft(floors = setOf(12)))
        val blob = onFloor(EnemyKind.BLOB, 5, 12)
        world.enemies += blob
        world.player.x = blob.x
        world.player.y = blob.y
        world.player.vy = 0f
        val first = run(world, 1)
        assertEquals(1, first.count { it == KuyuEvent.Hurt })
        assertEquals(KuyuWorld.MAX_HP - 1, world.player.hp)
        assertTrue(world.player.invincible > 0)
        repeat(30) {
            world.player.x = blob.x
            world.player.y = blob.y
            world.player.vy = 0f
            world.step(none)
        }
        assertEquals(KuyuWorld.MAX_HP - 1, world.player.hp)

        world.player.hp = 1
        world.player.invincible = 0
        world.player.x = blob.x
        world.player.y = blob.y
        world.player.vy = 0f
        val last = run(world, 1)
        assertTrue(last.any { it == KuyuEvent.Over })
        assertEquals(KuyuStatus.OVER, world.status)
        assertEquals(0, world.player.hp)
        val frames = world.frames
        assertTrue(run(world, 5, fire).isEmpty())
        assertEquals(frames, world.frames)
    }

    @Test
    fun comboBonusPaidOnLanding() {
        val world = KuyuWorld(1L, shaft(floors = setOf(20)))
        world.enemies += bat(5, 6f)
        world.enemies += bat(5, 9f)
        world.enemies += bat(5, 12f)
        world.player.x = 5f + (1f - KuyuWorld.PLAYER_W) / 2f
        world.player.y = 2f
        var frames = 0
        while (world.combo < 3 && frames < 120) {
            world.step(fire)
            frames++
        }
        assertEquals(3, world.combo)
        assertEquals(3, world.bestCombo)
        val gemsBefore = world.gemsCollected
        val events = run(world, 240, none)
        val combo = events.filterIsInstance<KuyuEvent.Combo>().single()
        assertEquals(3, combo.count)
        assertEquals(3 * KuyuWorld.COMBO_GEMS, combo.bonus)
        assertTrue(world.gemsCollected >= gemsBefore + combo.bonus)
        assertEquals(0, world.combo)
        assertTrue(world.player.grounded)
    }

    @Test
    fun wallsStopHorizontalMovement() {
        val world = KuyuWorld(5L)
        run(world, 120, KuyuInput(left = true))
        val p = world.player
        assertTrue(p.x >= 1f)
        assertFalse(world.tile(kotlin.math.floor(p.centerY).toInt(), kotlin.math.floor(p.x).toInt()).solid)
        run(world, 120, KuyuInput(right = true))
        assertTrue(p.x + KuyuWorld.PLAYER_W <= (w - 1).toFloat())
    }

    @Test
    fun dailySeedIsStablePerDay() {
        assertEquals(KuyuWorld.dailySeed(20_000L), KuyuWorld.dailySeed(20_000L))
        assertNotEquals(KuyuWorld.dailySeed(20_000L), KuyuWorld.dailySeed(20_001L))
    }

    private fun scripted(frame: Int): KuyuInput = when ((frame / 20) % 6) {
        0 -> KuyuInput(right = true)
        1 -> KuyuInput(right = true, fire = true)
        2 -> KuyuInput(fire = true)
        3 -> KuyuInput(left = true)
        4 -> KuyuInput(left = true, fire = true)
        else -> none
    }

    @Test
    fun sameSeedAndInputsGiveSameRun() {
        val a = KuyuWorld(KuyuWorld.dailySeed(20_300L))
        val b = KuyuWorld(KuyuWorld.dailySeed(20_300L))
        var eventsA = 0
        var eventsB = 0
        for (frame in 0 until 1500) {
            eventsA += a.step(scripted(frame)).size
            eventsB += b.step(scripted(frame)).size
        }
        assertEquals(a.hud(), b.hud())
        assertEquals(a.player.x, b.player.x, 0f)
        assertEquals(a.player.y, b.player.y, 0f)
        assertEquals(a.enemies.size, b.enemies.size)
        assertEquals(a.gems.size, b.gems.size)
        assertEquals(eventsA, eventsB)
        assertTrue(a.depth > 0)
    }

    @Test
    fun randomRunSurvivesAndDescends() {
        for (seed in 1L..4L) {
            val world = KuyuWorld(seed)
            val rng = Random(seed * 31)
            var frames = 0
            while (world.status == KuyuStatus.RUNNING && frames < 4000) {
                val roll = rng.nextInt(8)
                world.step(
                    KuyuInput(
                        left = roll == 0 || roll == 3,
                        right = roll == 1 || roll == 4,
                        fire = roll >= 3,
                    ),
                )
                frames++
            }
            assertTrue("tohum $seed derinlik ${world.depth}", world.depth > 5)
            assertTrue(world.chunkCount >= 3)
            assertEquals(world.score, (world.gemsCollected + world.depth).toLong())
            assertTrue(world.player.hp in 0..KuyuWorld.MAX_HP)
            assertTrue(world.viewTop <= world.player.y + 0.001f)
        }
    }
}
