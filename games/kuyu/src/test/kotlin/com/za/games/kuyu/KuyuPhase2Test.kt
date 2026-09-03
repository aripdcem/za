package com.za.games.kuyu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KuyuPhase2Test {

    private val w = KuyuGen.WIDTH
    private val h = KuyuGen.CHUNK_ROWS
    private val fire = KuyuInput(fire = true)
    private val none = KuyuInput.NONE

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

    private fun run(world: KuyuWorld, frames: Int, input: KuyuInput = none): List<KuyuEvent> {
        val out = ArrayList<KuyuEvent>()
        repeat(frames) { out += world.step(input) }
        return out
    }

    private fun onFloor(kind: EnemyKind, col: Int, floorRow: Int) =
        Enemy(kind, col + (1f - kind.w) / 2f, floorRow - kind.h, speedMul = 0f)

    @Test
    fun arenaChunkHasGateLedgesAndGuardian() {
        assertTrue(KuyuGen.isBossChunk(7))
        assertTrue(KuyuGen.isBossChunk(15))
        assertFalse(KuyuGen.isBossChunk(6))
        assertFalse(KuyuGen.isBossChunk(0))
        val chunk = KuyuGen.chunk(1L, 7)
        for (c in 1 until w - 1) assertEquals(Tile.GATE, chunk.tile(h - 1, c))
        assertEquals(Tile.WALL, chunk.tile(h - 1, 0))
        KuyuGen.ARENA_LEDGE_ROWS.forEachIndexed { i, r ->
            val cols = if (i % 2 == 0) 1..3 else w - 4 until w - 1
            for (c in cols) assertEquals(Tile.WALL, chunk.tile(r, c))
        }
        assertEquals(listOf(KuyuSpawn(EnemyKind.BOSS, 7 * h + KuyuGen.BOSS_ROW, w / 2)), chunk.spawns)
    }

    @Test
    fun nichesHoldChestsInsideThickWalls() {
        var chests = 0
        for (seed in 1L..6L) {
            for (index in 1 until 80) {
                val chunk = KuyuGen.chunk(seed, index)
                for (r in 0 until h) {
                    for (c in 0 until w) {
                        if (chunk.tile(r, c) != Tile.CHEST) continue
                        chests++
                        assertTrue("sandık duvar içinde değil: $seed/$index", c <= 2 || c >= w - 3)
                        assertEquals(Tile.EMPTY, chunk.tile(r - 1, c))
                        assertEquals(Tile.EMPTY, chunk.tile(r - 2, c))
                        assertEquals(Tile.WALL, chunk.tile(r, if (c <= 2) 0 else w - 1))
                    }
                }
            }
        }
        assertTrue("hiç sandık yok", chests > 20)
    }

    @Test
    fun chestDropsManyGems() {
        val world = KuyuWorld(1L, shaft(floors = setOf(12), blocks = mapOf((8 to 5) to Tile.CHEST)))
        world.player.x = 5f + (1f - KuyuWorld.PLAYER_W) / 2f
        world.player.y = 3f
        val events = run(world, 40, fire)
        val chest = events.filterIsInstance<KuyuEvent.Chest>().single()
        assertEquals(8, chest.row)
        assertEquals(KuyuGen.CHEST_GEMS, chest.gems)
        assertEquals(Tile.EMPTY, world.tile(8, 5))
        assertEquals(KuyuGen.CHEST_GEMS, world.gems.size + world.gemsCollected)
    }

    @Test
    fun guardianHoldsGateThenOfferOpens() {
        val world = KuyuWorld(3L)
        val arenaTop = 7 * h
        world.player.x = 5.55f
        world.player.y = arenaTop + 1f
        world.step(none)
        val gateRow = arenaTop + h - 1
        assertEquals(Tile.GATE, world.tile(gateRow, 5))
        val guardian = world.enemies.single { it.kind == EnemyKind.BOSS }
        assertTrue(guardian.alive)
        assertEquals(10, guardian.maxHp)
        // Deterministik dövüş: bekçi oyuncunun tam altında sabit dursun.
        guardian.alive = false
        val target = Enemy(EnemyKind.BOSS, world.player.centerX - EnemyKind.BOSS.w / 2f, arenaTop + KuyuGen.BOSS_ROW + 0.4f, speedMul = 0f)
        world.enemies += target
        val events = ArrayList<KuyuEvent>()
        var frames = 0
        while (target.alive && frames < 1500) {
            events += world.step(if ((frames / 20) % 2 == 0) fire else none)
            frames++
        }
        assertFalse("bekçi ölmedi", target.alive)
        assertTrue(events.any { it is KuyuEvent.Kill && it.kind == EnemyKind.BOSS })
        assertEquals(listOf(KuyuEvent.GateOpen(7)), events.filterIsInstance<KuyuEvent.GateOpen>())
        assertEquals(Tile.EMPTY, world.tile(gateRow, 5))
        assertTrue(world.player.hp > 0)

        // Kapıdan düşünce yeni bölge: seçim kartı açılır, simülasyon durur.
        frames = 0
        while (world.status == KuyuStatus.RUNNING && frames < 900) {
            events += world.step(none)
            frames++
        }
        assertEquals(KuyuStatus.CHOOSING, world.status)
        assertTrue(events.any { it == KuyuEvent.Area(1) })
        assertTrue(events.any { it == KuyuEvent.Offer(1) })
        val offer = world.offer
        assertNotNull(offer)
        assertEquals(1, offer!!.area)
        assertEquals(3, offer.upgrades.size)
        assertEquals(3, offer.shop.size)
        val frozen = world.frames
        assertTrue(run(world, 10, fire).isEmpty())
        assertEquals(frozen, world.frames)
        assertTrue(world.chooseUpgrade(0))
        assertFalse(world.chooseUpgrade(1))
        assertTrue(world.perks.owned.contains(offer.upgrades[0]))
        assertTrue(world.resumeFromOffer())
        assertEquals(KuyuStatus.RUNNING, world.status)
        assertNull(world.offer)
        run(world, 5)
        assertEquals(frozen + 5, world.frames)
    }

    @Test
    fun offersAreSeededAndSkipOwnedUniques() {
        val a = KuyuWorld(5L)
        val b = KuyuWorld(5L)
        for (world in listOf(a, b)) {
            world.player.y = 8f * h
            world.step(none)
            assertEquals(KuyuStatus.CHOOSING, world.status)
        }
        assertEquals(a.offer!!.upgrades, b.offer!!.upgrades)
        assertEquals(listOf(40, 35, 60), a.offer!!.shop.map { it.price })
        val unique = a.offer!!.upgrades.indexOfFirst { !it.stackable }
        assertTrue(unique >= 0)
        val chosen = a.offer!!.upgrades[unique]
        assertTrue(a.chooseUpgrade(unique))
        a.resumeFromOffer()
        a.player.y = 16f * h
        a.step(none)
        assertEquals(KuyuStatus.CHOOSING, a.status)
        assertEquals(2, a.offer!!.area)
        assertFalse(chosen in a.offer!!.upgrades)
        assertEquals(3, a.offer!!.upgrades.size)
    }

    @Test
    fun shopSpendsWalletWithoutLoweringScore() {
        val world = KuyuWorld(5L)
        world.player.y = 8f * h
        world.step(none)
        assertEquals(KuyuStatus.CHOOSING, world.status)
        assertFalse(world.buy(0))
        world.gemsCollected = 100
        val scoreBefore = world.score
        world.player.hp = 2
        assertTrue(world.buy(0)) // iyileş
        assertEquals(world.perks.maxHp, world.player.hp)
        assertEquals(100 - 40, world.wallet)
        assertEquals(scoreBefore, world.score)
        assertFalse(world.buy(0)) // bir kez
        assertTrue(world.buy(2)) // can +1
        assertEquals(KuyuWorld.MAX_HP + 1, world.perks.maxHp)
        assertEquals(KuyuWorld.MAX_HP + 1, world.player.hp)
        assertEquals(100 - 40 - 60, world.wallet)
        assertFalse(world.buy(1)) // 35 > kalan 0
    }

    @Test
    fun ammoAndJumpPerks() {
        val world = KuyuWorld(1L, shaft(floors = setOf(12)))
        world.player.x = 5f
        world.player.y = 12f - KuyuWorld.PLAYER_H
        world.player.ammo = 3
        world.grant(Upgrade.AMMO)
        assertEquals(KuyuWorld.AMMO + 2, world.perks.maxAmmo)
        assertEquals(KuyuWorld.AMMO + 2, world.player.ammo) // hemen dolar
        world.player.y = 6f
        run(world, 60, fire)
        assertTrue(world.player.ammo < KuyuWorld.AMMO + 2)
        run(world, 120, none)
        assertTrue(world.player.grounded)
        assertEquals(KuyuWorld.AMMO + 2, world.player.ammo) // inişte tam şarjör

        val startY = world.player.y
        world.grant(Upgrade.JUMP)
        world.step(fire)
        var minY = world.player.y
        repeat(70) { world.step(none); minY = minOf(minY, world.player.y) }
        val height = startY - minY
        assertTrue("yaylı zıplama $height", height in 3.4f..4.4f)
    }

    @Test
    fun spreadRapidAndRangePerks() {
        val world = KuyuWorld(1L, shaft())
        world.player.x = 5f
        world.player.y = 3f
        world.grant(Upgrade.SPREAD)
        world.grant(Upgrade.RAPID)
        world.grant(Upgrade.RANGE)
        val shotFrames = ArrayList<Int>()
        var maxTraveled = 0f
        var bulletsAfterFirstShot = -1
        repeat(40) {
            val events = world.step(fire)
            if (events.any { it == KuyuEvent.Shot }) {
                shotFrames += world.frames
                if (bulletsAfterFirstShot < 0) bulletsAfterFirstShot = world.bullets.size
            }
            for (b in world.bullets) maxTraveled = maxOf(maxTraveled, b.traveled)
        }
        assertEquals(3, bulletsAfterFirstShot) // yayılan atış: tek atışta üç mermi
        assertTrue(shotFrames.size >= 2)
        assertEquals(4, shotFrames[1] - shotFrames[0]) // hızlı botlar
        assertTrue("menzil $maxTraveled", maxTraveled > KuyuWorld.BULLET_RANGE && maxTraveled <= 14.5f)
    }

    @Test
    fun shieldAbsorbsOneHitPerArea() {
        val world = KuyuWorld(1L, shaft(floors = setOf(12)))
        val blob = onFloor(EnemyKind.BLOB, 5, 12)
        world.enemies += blob
        world.grant(Upgrade.SHIELD)
        world.player.x = blob.x
        world.player.y = blob.y
        world.player.vy = 0f
        val first = run(world, 1)
        assertTrue(first.any { it == KuyuEvent.Shield })
        assertTrue(first.none { it == KuyuEvent.Hurt })
        assertEquals(KuyuWorld.MAX_HP, world.player.hp)
        assertFalse(world.perks.shieldReady)
        world.player.invincible = 0
        world.player.knock = 0 // geri tepme oyuncuyu temastan uzaklaştırmasın
        world.player.vx = 0f
        world.player.x = blob.x
        world.player.y = blob.y
        world.player.vy = 0f
        val second = run(world, 1)
        assertTrue(second.any { it == KuyuEvent.Hurt })
        assertEquals(KuyuWorld.MAX_HP - 1, world.player.hp)
    }

    @Test
    fun greedMagnetAndComboPerks() {
        val world = KuyuWorld(1L, shaft(floors = setOf(12)))
        world.enemies += onFloor(EnemyKind.BLOB, 5, 12)
        world.grant(Upgrade.GREED)
        world.grant(Upgrade.MAGNET)
        world.grant(Upgrade.COMBO)
        assertEquals(KuyuWorld.MAGNET_RANGE * 2f, world.perks.magnetRange, 0f)
        world.player.x = 5f + (1f - KuyuWorld.PLAYER_W) / 2f
        world.player.y = 3f
        val events = run(world, 30, fire)
        assertTrue(events.any { it is KuyuEvent.Kill })
        assertEquals(EnemyKind.BLOB.gems + 1, world.gems.size + world.gemsCollected)
        // Kombo bonusu: üç sabit yarasa, iniş.
        val combo = KuyuWorld(1L, shaft(floors = setOf(20)))
        for (row in listOf(6f, 9f, 12f)) {
            combo.enemies += Enemy(EnemyKind.BAT, 5f + (1f - EnemyKind.BAT.w) / 2f, row, speedMul = 0f)
        }
        combo.grant(Upgrade.COMBO)
        combo.player.x = 5f + (1f - KuyuWorld.PLAYER_W) / 2f
        combo.player.y = 2f
        var frames = 0
        while (combo.combo < 3 && frames < 120) {
            combo.step(fire)
            frames++
        }
        assertEquals(3, combo.combo)
        val bonus = run(combo, 240, none).filterIsInstance<KuyuEvent.Combo>().single()
        assertEquals(3 * KuyuWorld.COMBO_GEMS * 2, bonus.bonus)
    }
}
