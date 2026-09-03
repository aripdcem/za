package com.za.games.kuyu

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Kuyu simülasyonu: değişebilir dünya, sabit 1/60 s adım. Arayüz her karede
 * [step] çağırır; dönen olayları ses, titreşim ve parçacıklar için kullanır.
 * Aynı tohum + aynı girdi dizisi (ve aynı seçimler) = aynı oyun; günlük mod
 * buna dayanır.
 *
 * Koordinatlar kare (hücre) birimindedir; y aşağı doğru büyür. Kamera yalnızca
 * aşağı iner ([viewTop]); oyuncu görünen alanın üstüne çıkamaz.
 *
 * Her bölgenin sonunda bekçi kapıyı tutar; bekçi ölünce kapı açılır. Yeni
 * bölgeye girince simülasyon [KuyuStatus.CHOOSING] ile durur, [offer] ile bir
 * yükseltme seçilir ve dükkândan alışveriş yapılır; [resumeFromOffer] sürdürür.
 */
class KuyuWorld(
    val seed: Long,
    private val generator: (Int) -> KuyuChunk = { index -> KuyuGen.chunk(seed, index) },
) {
    companion object {
        const val WIDTH = KuyuGen.WIDTH
        const val CHUNK_ROWS = KuyuGen.CHUNK_ROWS
        const val START_ROW = KuyuGen.START_ROW

        /** Mantıksal görüş alanı (satır); ekran daha uzunsa fazlası da çizilir. */
        const val VIEW_ROWS = 24

        /** İnerken oyuncu görüş alanının üstünden bu kadar aşağıda tutulur. */
        const val VIEW_ANCHOR = 8.5f
        const val STEP = 1f / 60f
        const val GRAVITY = 34f
        const val MOVE_SPEED = 7.5f
        const val JUMP_SPEED = 14.5f
        const val MAX_FALL = 14f

        /** Her atışta düşüş hızı en fazla bu olur: botlar askıda tutar. */
        const val SHOT_FALL = 3.2f
        const val SHOT_INTERVAL = 6
        const val BULLET_SPEED = 42f
        const val BULLET_RANGE = 9f
        const val AMMO = 8
        const val MAX_HP = 4
        const val INVINCIBLE_FRAMES = 75
        const val STOMP_BOUNCE = 10.5f
        const val HURT_BOUNCE = 7f
        const val KNOCK_FRAMES = 12
        const val COYOTE_FRAMES = 6
        const val PLAYER_W = 0.7f
        const val PLAYER_H = 0.85f
        const val MAGNET_RANGE = 1.6f

        /** Bu sayıdan itibaren iniş anında kombo bonusu verilir (kombo × [COMBO_GEMS]). */
        const val COMBO_MIN = 3
        const val COMBO_GEMS = 2

        /** Bekçinin yarasa çağırma aralığı (s) ve aynı anda yaşayan azami yarasa. */
        const val BOSS_SUMMON_INTERVAL = 2.5f
        const val BOSS_MAX_MINIONS = 3
        private const val EPS = 0.001f
        private const val ACTIVE_BELOW = 6f
        private const val CULL_ABOVE = 4f
        private const val GEN_AHEAD = 12

        fun dailySeed(epochDay: Long): Long = KuyuGen.mix(epochDay, 0x4B55, 0x59)

        /** Dükkân fiyatları bölgeyle artar. */
        fun price(item: ShopItem, area: Int): Int = when (item) {
            ShopItem.HEAL -> 30 + 10 * area
            ShopItem.AMMO_UP -> 25 + 10 * area
            ShopItem.LIFE -> 45 + 15 * area
        }
    }

    private val chunks = ArrayList<KuyuChunk>()
    private val rng = Random(KuyuGen.mix(seed, 0x51, 0x1D))
    private val events = ArrayList<KuyuEvent>()

    val enemies = ArrayList<Enemy>()
    val bullets = ArrayList<Bullet>()
    val gems = ArrayList<Gem>()
    val perks = KuyuPerks()

    var status = KuyuStatus.RUNNING
        private set
    var offer: KuyuOffer? = null
        private set
    var viewTop = 0f
        private set
    var depth = 0
        private set
    var gemsCollected = 0
        internal set
    var gemsSpent = 0
        private set
    var combo = 0
        private set
    var bestCombo = 0
        private set
    var frames = 0
        private set
    private var lastArea = 0

    val player: Player

    init {
        ensureRows(VIEW_ROWS + GEN_AHEAD)
        player = Player(startX(), START_ROW - PLAYER_H)
        player.grounded = solidBelow(player.x, player.y, PLAYER_W, PLAYER_H)
        player.apexY = player.y
    }

    /** Skor toplanan taşları sayar; harcamak skoru düşürmez. */
    val score: Long get() = (gemsCollected + depth).toLong()
    val wallet: Int get() = gemsCollected - gemsSpent
    val currentChunk: Int get() = max(0, floor(player.y).toInt()) / CHUNK_ROWS
    val area: Int get() = KuyuGen.area(currentChunk)
    val chunkCount: Int get() = chunks.size

    fun hud(): KuyuHud {
        val boss = enemies.firstOrNull { it.alive && it.active && it.kind == EnemyKind.BOSS }
        return KuyuHud(
            score = score,
            depth = depth,
            gems = gemsCollected,
            hp = player.hp,
            ammo = player.ammo,
            combo = combo,
            bestCombo = bestCombo,
            area = area,
            status = status,
            maxHp = perks.maxHp,
            maxAmmo = perks.maxAmmo,
            wallet = wallet,
            bossHp = boss?.hp,
            bossMaxHp = boss?.maxHp ?: 0,
            shieldReady = perks.shieldReady,
        )
    }

    /** Sınır dışı ve tavan (satır < 0) duvardır. */
    fun tile(row: Int, col: Int): Tile {
        if (col < 0 || col >= WIDTH || row < 0) return Tile.WALL
        ensureRows(row)
        return chunks[row / CHUNK_ROWS].tile(row % CHUNK_ROWS, col)
    }

    private fun solid(row: Int, col: Int): Boolean = tile(row, col).solid

    private fun ensureRows(row: Int) {
        while (chunks.size * CHUNK_ROWS <= row) {
            val chunk = generator(chunks.size)
            chunks.add(chunk)
            val speedMul = 1f + min(chunk.index, 25) * 0.03f
            for (spawn in chunk.spawns) enemies.add(Enemy.from(spawn, speedMul, rng))
        }
    }

    private fun startX(): Float {
        for (c in 1 until WIDTH - 1) {
            if (!tile(START_ROW - 1, c).solid && tile(START_ROW, c).solid) return c + (1f - PLAYER_W) / 2f
        }
        return WIDTH / 2f - PLAYER_W / 2f
    }

    private fun emit(event: KuyuEvent) {
        events.add(event)
    }

    /** Yükseltmeyi hemen uygular (teklif seçimi ve testler için). */
    fun grant(upgrade: Upgrade) {
        perks.apply(upgrade, player)
    }

    /** Tekliften yükseltme seçer; teklif başına bir kez. */
    fun chooseUpgrade(index: Int): Boolean {
        val current = offer ?: return false
        if (status != KuyuStatus.CHOOSING || current.chosen != null) return false
        val upgrade = current.upgrades.getOrNull(index) ?: return false
        grant(upgrade)
        current.chosen = upgrade
        return true
    }

    /** Dükkândan alır; cüzdan yetmiyorsa ya da zaten alındıysa false. */
    fun buy(index: Int): Boolean {
        val current = offer ?: return false
        if (status != KuyuStatus.CHOOSING) return false
        val entry = current.shop.getOrNull(index) ?: return false
        if (entry.bought || wallet < entry.price) return false
        when (entry.item) {
            ShopItem.HEAL -> player.hp = perks.maxHp
            ShopItem.AMMO_UP -> perks.maxAmmo = min(perks.maxAmmo + 1, KuyuPerks.MAX_AMMO)
            ShopItem.LIFE -> {
                perks.maxHp = min(perks.maxHp + 1, KuyuPerks.MAX_HP)
                player.hp = min(perks.maxHp, player.hp + 1)
            }
        }
        gemsSpent += entry.price
        entry.bought = true
        return true
    }

    /** Seçim kartını kapatır, kuyuya dönülür. */
    fun resumeFromOffer(): Boolean {
        if (status != KuyuStatus.CHOOSING) return false
        offer = null
        status = KuyuStatus.RUNNING
        player.fireHeld = true // kartı kapatan dokunuş zıplatmasın
        return true
    }

    private fun openOffer(area: Int) {
        val pool = Upgrade.entries.filter { it.stackable || it !in perks.owned }.toMutableList()
        val pick = Random(KuyuGen.mix(seed, area, 0xA5))
        val upgrades = ArrayList<Upgrade>()
        while (upgrades.size < 3 && pool.isNotEmpty()) upgrades += pool.removeAt(pick.nextInt(pool.size))
        val shop = ShopItem.entries.map { ShopEntry(it, price(it, area)) }
        offer = KuyuOffer(area, upgrades, shop)
        status = KuyuStatus.CHOOSING
        bullets.clear()
        emit(KuyuEvent.Offer(area))
    }

    /** Bir kare ilerletir; oyun bittiyse ya da seçim bekliyorsa hiçbir şey yapmaz. */
    fun step(input: KuyuInput): List<KuyuEvent> {
        events.clear()
        if (status != KuyuStatus.RUNNING) return emptyList()
        frames++
        val p = player

        // 1. Yatay hareket; geri tepme sürerken girdi işlemez.
        val dir = (if (input.right) 1 else 0) - (if (input.left) 1 else 0)
        if (p.knock > 0) p.knock-- else p.vx = dir * MOVE_SPEED
        if (dir != 0) p.facing = dir

        // 2. Zıplama: tuşa yeni basıldıysa ve yerdeyse (ya da yerden yeni ayrıldıysa).
        val pressed = input.fire && !p.fireHeld
        p.fireHeld = input.fire
        if (pressed && (p.grounded || p.coyote > 0)) {
            p.vy = -JUMP_SPEED * perks.jumpMul
            p.grounded = false
            p.coyote = 0
            emit(KuyuEvent.Jump)
        }

        // 3. Botlar: havada, tepeyi geçtikten sonra basılı tutulursa aşağı ateş.
        if (p.shotCooldown > 0) p.shotCooldown--
        if (input.fire && !p.grounded && p.vy > -2f && p.ammo > 0 && p.shotCooldown == 0) {
            p.ammo--
            p.shotCooldown = perks.shotInterval
            p.spread = -p.spread
            if (perks.spread) {
                bullets.add(Bullet(p.centerX - 0.3f, p.bottom))
                bullets.add(Bullet(p.centerX, p.bottom))
                bullets.add(Bullet(p.centerX + 0.3f, p.bottom))
            } else {
                bullets.add(Bullet(p.centerX + p.spread * 0.12f, p.bottom))
            }
            p.vy = min(p.vy, SHOT_FALL)
            emit(KuyuEvent.Shot)
        }

        // 4. Yerçekimi ve hareket.
        p.vy = min(p.vy + GRAVITY * STEP, MAX_FALL)
        val prevBottom = p.bottom
        val wasGrounded = p.grounded
        movePlayerX(p)
        movePlayerY(p)
        if (p.y < viewTop) {
            p.y = viewTop
            if (p.vy < 0f) p.vy = 0f
        }
        p.grounded = p.vy >= 0f && solidBelow(p.x, p.y, PLAYER_W, PLAYER_H)
        if (p.grounded) {
            if (!wasGrounded) land(p)
            p.coyote = COYOTE_FRAMES
            p.apexY = p.y
        } else {
            if (p.coyote > 0) p.coyote--
            if (p.y < p.apexY) p.apexY = p.y
        }

        // 5. Mermiler, düşmanlar, çarpışmalar, taşlar.
        updateBullets()
        updateEnemies()
        collideEnemies(p, prevBottom)
        updateGems(p)
        enemies.removeAll { !it.alive }

        // 6. Derinlik, kamera, bölge, dokunulmazlık.
        depth = max(depth, floor(p.bottom).toInt() - START_ROW)
        viewTop = max(viewTop, p.y - VIEW_ANCHOR)
        ensureRows(floor(viewTop).toInt() + VIEW_ROWS + GEN_AHEAD)
        if (p.invincible > 0) p.invincible--
        val currentArea = area
        if (currentArea != lastArea && status == KuyuStatus.RUNNING) {
            lastArea = currentArea
            emit(KuyuEvent.Area(currentArea))
            if (perks.shield) perks.shieldReady = true
            if (currentArea > 0) openOffer(currentArea)
        }
        return events.toList()
    }

    private fun land(p: Player) {
        p.ammo = perks.maxAmmo
        val fallRows = max(0, floor(p.y - p.apexY).toInt())
        if (combo >= COMBO_MIN) {
            val bonus = combo * perks.comboGems
            gemsCollected += bonus
            emit(KuyuEvent.Combo(combo, bonus))
        }
        combo = 0
        emit(KuyuEvent.Land(fallRows))
    }

    private fun solidBelow(x: Float, y: Float, w: Float, h: Float): Boolean {
        val row = floor(y + h + 0.02f).toInt()
        val left = floor(x + EPS).toInt()
        val right = floor(x + w - EPS).toInt()
        for (c in left..right) if (solid(row, c)) return true
        return false
    }

    private fun movePlayerX(p: Player) {
        if (p.vx == 0f) return
        val nx = p.x + p.vx * STEP
        val top = floor(p.y + EPS).toInt()
        val bottom = floor(p.y + PLAYER_H - EPS).toInt()
        if (p.vx > 0f) {
            val col = floor(nx + PLAYER_W - EPS).toInt()
            if ((top..bottom).any { solid(it, col) }) {
                p.x = col - PLAYER_W
                p.vx = 0f
            } else {
                p.x = nx
            }
        } else {
            val col = floor(nx + EPS).toInt()
            if ((top..bottom).any { solid(it, col) }) {
                p.x = (col + 1).toFloat()
                p.vx = 0f
            } else {
                p.x = nx
            }
        }
    }

    private fun movePlayerY(p: Player) {
        val ny = p.y + p.vy * STEP
        val left = floor(p.x + EPS).toInt()
        val right = floor(p.x + PLAYER_W - EPS).toInt()
        if (p.vy > 0f) {
            val row = floor(ny + PLAYER_H - EPS).toInt()
            if ((left..right).any { solid(row, it) }) {
                p.y = row - PLAYER_H
                p.vy = 0f
            } else {
                p.y = ny
            }
        } else if (p.vy < 0f) {
            val row = floor(ny + EPS).toInt()
            if ((left..right).any { solid(row, it) }) {
                p.y = (row + 1).toFloat()
                p.vy = 0f
            } else {
                p.y = ny
            }
        }
    }

    private fun updateBullets() {
        val it = bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            var remaining = BULLET_SPEED * STEP
            var hit = false
            while (remaining > 0f && !hit) {
                val d = min(remaining, 0.3f)
                b.y += d
                b.traveled += d
                remaining -= d
                val row = floor(b.y).toInt()
                val col = floor(b.x).toInt()
                val t = tile(row, col)
                if (t.solid) {
                    if (t.breakable) breakTile(row, col, t)
                    hit = true
                } else {
                    for (i in 0 until enemies.size) {
                        val e = enemies[i]
                        if (!e.alive || !e.active) continue
                        if (b.x >= e.x && b.x <= e.x + e.w && b.y >= e.y && b.y <= e.y + e.h) {
                            damage(e, 1, stomp = false)
                            hit = true
                            break
                        }
                    }
                }
            }
            if (hit || b.traveled >= perks.bulletRange) it.remove()
        }
    }

    private fun breakTile(row: Int, col: Int, tile: Tile) {
        chunks[row / CHUNK_ROWS].set(row % CHUNK_ROWS, col, Tile.EMPTY)
        if (tile == Tile.CHEST) {
            spawnGems(col + 0.5f, row + 0.5f, KuyuGen.CHEST_GEMS)
            emit(KuyuEvent.Chest(row, col, KuyuGen.CHEST_GEMS))
            return
        }
        val withGems = tile == Tile.GEM_BLOCK
        if (withGems) spawnGems(col + 0.5f, row + 0.5f, 2)
        emit(KuyuEvent.BlockBreak(row, col, withGems))
    }

    private fun damage(e: Enemy, amount: Int, stomp: Boolean) {
        e.hp -= amount
        e.hitFlash = 6
        if (e.hp > 0) return
        e.alive = false
        spawnGems(e.centerX, e.centerY, e.kind.gems + perks.greed)
        if (!player.grounded) {
            combo++
            bestCombo = max(bestCombo, combo)
        }
        emit(KuyuEvent.Kill(e.kind, e.centerX, e.centerY, stomp))
        if (e.kind == EnemyKind.BOSS) openGate(floor(e.centerY).toInt() / CHUNK_ROWS)
    }

    /** Bekçi ölünce arenanın kapısı açılır. */
    private fun openGate(chunkIndex: Int) {
        val row = chunkIndex * CHUNK_ROWS + CHUNK_ROWS - 1
        for (c in 0 until WIDTH) {
            if (tile(row, c) == Tile.GATE) chunks[chunkIndex].set(CHUNK_ROWS - 1, c, Tile.EMPTY)
        }
        emit(KuyuEvent.GateOpen(chunkIndex))
    }

    private fun spawnGems(x: Float, y: Float, count: Int) {
        repeat(count) {
            gems.add(Gem(x, y, rng.nextFloat() * 6f - 3f, -(3f + rng.nextFloat() * 4f)))
        }
    }

    private fun updateEnemies() {
        val activationLimit = viewTop + VIEW_ROWS + ACTIVE_BELOW
        for (i in 0 until enemies.size) {
            val e = enemies[i]
            if (!e.alive) continue
            if (e.y + e.h < viewTop - CULL_ABOVE) {
                e.alive = false
                continue
            }
            if (!e.active) {
                if (e.y < activationLimit) e.active = true else continue
            }
            if (e.hitFlash > 0) e.hitFlash--
            when (e.kind) {
                EnemyKind.BLOB, EnemyKind.SPIKY -> patrol(e)
                EnemyKind.BAT -> fly(e)
                EnemyKind.CRAWLER -> crawl(e)
                EnemyKind.BOSS -> guard(e)
            }
        }
    }

    /** Çıkıntıda gezer; kenara ya da duvara gelince döner, desteği yoksa düşer. */
    private fun patrol(e: Enemy) {
        if (!solidBelow(e.x, e.y, e.w, e.h)) {
            e.vy = min(e.vy + GRAVITY * STEP, MAX_FALL)
            val ny = e.y + e.vy * STEP
            val row = floor(ny + e.h - EPS).toInt()
            val left = floor(e.x + EPS).toInt()
            val right = floor(e.x + e.w - EPS).toInt()
            if ((left..right).any { solid(row, it) }) {
                e.y = row - e.h
                e.vy = 0f
            } else {
                e.y = ny
            }
            return
        }
        e.vy = 0f
        val nx = e.x + e.dir * e.kind.speed * e.speedMul * STEP
        val top = floor(e.y + EPS).toInt()
        val bottom = floor(e.y + e.h - EPS).toInt()
        val edgeCol = if (e.dir > 0) floor(nx + e.w - EPS).toInt() else floor(nx + EPS).toInt()
        val blocked = (top..bottom).any { solid(it, edgeCol) }
        val floorRow = floor(e.y + e.h + 0.02f).toInt()
        if (blocked || !solid(floorRow, edgeCol)) e.dir = -e.dir else e.x = nx
    }

    /** Yatay uçar, duvardan döner, doğduğu yükseklik çevresinde salınır. */
    private fun fly(e: Enemy) {
        val nx = e.x + e.dir * e.kind.speed * e.speedMul * STEP
        val top = floor(e.y + EPS).toInt()
        val bottom = floor(e.y + e.h - EPS).toInt()
        val edgeCol = if (e.dir > 0) floor(nx + e.w - EPS).toInt() else floor(nx + EPS).toInt()
        if ((top..bottom).any { solid(it, edgeCol) }) e.dir = -e.dir else e.x = nx
        e.t += 3.2f * STEP
        e.y = e.baseY + sin(e.t) * 0.3f
    }

    /** Duvar boyunca aşağı yukarı; katıya ya da menzil sınırına gelince döner. */
    private fun crawl(e: Enemy) {
        val ny = e.y + e.dir * e.kind.speed * e.speedMul * STEP
        val left = floor(e.x + EPS).toInt()
        val right = floor(e.x + e.w - EPS).toInt()
        val edgeRow = if (e.dir > 0) floor(ny + e.h - EPS).toInt() else floor(ny + EPS).toInt()
        val blocked = edgeRow < e.minRow || edgeRow > e.maxRow || (left..right).any { solid(edgeRow, it) }
        if (blocked) e.dir = -e.dir else e.y = ny
    }

    /** Bekçi: kapının üstünde bir duvardan ötekine salınır, aralıklarla yarasa çağırır. */
    private fun guard(e: Enemy) {
        val nx = e.x + e.dir * e.kind.speed * e.speedMul * STEP
        if (nx < 1f || nx + e.w > WIDTH - 1f) e.dir = -e.dir else e.x = nx
        e.t += STEP
        e.y = e.baseY + sin(e.t * 2f) * 0.4f
        e.spawnTimer += STEP
        if (e.spawnTimer >= BOSS_SUMMON_INTERVAL) {
            e.spawnTimer = 0f
            val minions = enemies.count { it.alive && it.minion }
            if (minions < BOSS_MAX_MINIONS) {
                val bat = Enemy(
                    EnemyKind.BAT,
                    e.centerX - EnemyKind.BAT.w / 2f,
                    e.y - 1.2f,
                    e.speedMul,
                    dir = if (rng.nextBoolean()) 1 else -1,
                )
                bat.minion = true
                bat.active = true
                bat.t = rng.nextFloat() * 6.2832f
                enemies.add(bat)
            }
        }
    }

    private fun collideEnemies(p: Player, prevBottom: Float) {
        val inset = 0.08f
        for (i in 0 until enemies.size) {
            val e = enemies[i]
            if (!e.alive || !e.active) continue
            val overlap = p.x + inset < e.x + e.w && p.x + PLAYER_W - inset > e.x &&
                p.y + inset < e.y + e.h && p.bottom - inset > e.y
            if (!overlap) continue
            val fromAbove = p.vy > 0f && prevBottom <= e.y + 0.35f
            if (fromAbove) {
                p.y = e.y - PLAYER_H
                p.vy = -STOMP_BOUNCE
                p.grounded = false
                p.coyote = 0
                p.apexY = p.y
                if (e.kind.stompable) {
                    p.ammo = perks.maxAmmo
                    damage(e, 2, stomp = true)
                    emit(KuyuEvent.Stomp)
                } else {
                    hurt(p, e)
                }
            } else {
                hurt(p, e)
            }
        }
    }

    private fun hurt(p: Player, e: Enemy) {
        if (p.invincible > 0) return
        p.invincible = INVINCIBLE_FRAMES
        p.vy = -HURT_BOUNCE
        p.vx = if (p.centerX < e.centerX) -MOVE_SPEED * 0.8f else MOVE_SPEED * 0.8f
        p.knock = KNOCK_FRAMES
        p.grounded = false
        if (perks.shieldReady) {
            perks.shieldReady = false
            emit(KuyuEvent.Shield)
            return
        }
        p.hp--
        combo = 0
        emit(KuyuEvent.Hurt)
        if (p.hp <= 0) {
            p.hp = 0
            status = KuyuStatus.OVER
            emit(KuyuEvent.Over)
        }
    }

    private fun updateGems(p: Player) {
        val it = gems.iterator()
        while (it.hasNext()) {
            val g = it.next()
            if (g.y < viewTop - 2f) {
                it.remove()
                continue
            }
            val dx = p.centerX - g.x
            val dy = p.centerY - g.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < perks.magnetRange) {
                if (dist < 0.45f) {
                    gemsCollected++
                    emit(KuyuEvent.Gem(g.x, g.y, gemsCollected))
                    it.remove()
                    continue
                }
                val pull = 14f * STEP
                g.x += dx / dist * pull
                g.y += dy / dist * pull
                g.resting = false
                continue
            }
            if (g.resting) {
                if (!solid(floor(g.y + 0.16f).toInt(), floor(g.x).toInt())) g.resting = false
                continue
            }
            g.vy = min(g.vy + 24f * STEP, 12f)
            val ny = g.y + g.vy * STEP
            if (g.vy > 0f && solid(floor(ny + 0.15f).toInt(), floor(g.x).toInt())) {
                g.y = floor(ny + 0.15f) - 0.15f
                g.vy = 0f
                g.vx = 0f
                g.resting = true
            } else {
                g.y = ny
                val nx = g.x + g.vx * STEP
                if (solid(floor(g.y).toInt(), floor(nx).toInt())) g.vx = -g.vx * 0.5f else g.x = nx
                g.vx *= 0.98f
            }
        }
    }
}
