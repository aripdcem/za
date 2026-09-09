package com.za.games.filo

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Filo simülasyonu: dikey kaydırmalı uzay savaşı. Oyun alanı [WIDTH]×[HEIGHT]
 * birim (dikey ekran), oyuncu altta yatay hareket eder, ateş otomatiktir.
 * Sabit 1/60 s adım; aynı tohum + aynı giriş dizisi = aynı koşu.
 *
 * Dalgalar tohumdan üretilir: her dalga birkaç gruptan oluşur (dalış, sinüs,
 * süpürme, halka, asteroit sürüklenmesi), her [BOSS_EVERY]. dalga patrondur.
 * Düşmanı vurmak puan verir; art arda vuruşlar zinciri büyütür (çarpan 1–4).
 * Vurulunca can gider, kısa dokunulmazlık başlar ve silah bir seviye düşer;
 * kalkan bir vuruşu emer. Bomba ekrandaki mermileri siler ve düşmanlara
 * hasar verir. Bazı düşmanlar güç artırımı bırakır.
 */
class FiloWorld(val seed: Long) {

    companion object {
        const val STEP = 1f / 60f
        const val WIDTH = 1f
        const val HEIGHT = 1.6f
        const val PLAYER_Y = 1.42f
        const val PLAYER_RADIUS = 0.038f
        const val PLAYER_MARGIN = 0.05f
        const val FIRE_INTERVAL = 0.17f
        const val BULLET_SPEED = 2.6f
        const val BULLET_RADIUS = 0.012f
        const val ENEMY_BULLET_SPEED = 0.7f
        const val ENEMY_BULLET_RADIUS = 0.016f
        const val LIVES = 3
        const val BOMBS = 2
        const val MAX_BOMBS = 4
        const val MAX_WEAPON = 3
        const val INVULN_TIME = 2f
        const val SHIELD_INVULN_TIME = 0.8f
        const val CHAIN_WINDOW = 2f
        const val FIRST_WAVE_DELAY = 0.6f
        const val WAVE_GAP = 1.6f
        const val BOSS_EVERY = 5
        const val BOMB_DAMAGE = 3
        const val BOSS_BOMB_DAMAGE = 6
        const val POWER_SPEED = 0.35f
        const val POWER_RADIUS = 0.035f
        const val DROP_CHANCE = 0.14f
        const val SCORE_GIFT = 500L
        const val WAVE_BONUS = 250L
        const val ESCAPE_Y = HEIGHT + 0.12f
        const val RING_ENTRY = 0.35f
        const val RING_RADIUS = 0.17f
        const val RING_LOOPS = 1.25f
        const val BOSS_Y = 0.22f

        fun dailySeed(epochDay: Long): Long = mix(epochDay, 0x46, 0x49)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }

        /** 0 → 1 arası zorluk; 26. dalgada tavan. */
        fun difficulty(wave: Int): Float = ((wave - 1) / 25f).coerceIn(0f, 1f)
    }

    private class Spawn(val at: Float, val enemy: Enemy)

    /** Bir sonraki [step] çağrısında teslim edilecek olaylar. */
    private val events = ArrayList<FiloEvent>()

    var playerX = WIDTH / 2f
        private set
    val playerY: Float get() = PLAYER_Y
    var lives = LIVES
        private set
    var bombs = BOMBS
        private set
    var weapon = 1
        private set
    var shield = false
        private set
    var invuln = 0f
        private set
    var score = 0L
        private set
    var wave = 0
        private set
    var chain = 0
        private set
    var kills = 0
        private set
    var status = FiloStatus.RUNNING
        private set
    var frames = 0
        private set

    val enemies = ArrayList<Enemy>()
    val bullets = ArrayList<Bullet>()
    val enemyBullets = ArrayList<Bullet>()
    val powers = ArrayList<Power>()

    private val queue = ArrayList<Spawn>()
    private var waveTime = 0f
    private var waveTotal = 0
    private var waveKilled = 0
    private var waveEscaped = 0
    private var gapTimer = FIRST_WAVE_DELAY
    private var waveActive = false
    private var wavesFrozen = false
    private var fireTimer = 0f
    private var chainTimer = 0f
    private var nextId = 1
    private var targetX = WIDTH / 2f

    val multiplier: Int get() = 1 + min(3, chain / 4)
    val boss: Enemy? get() = enemies.firstOrNull { it.kind == EnemyKind.BOSS && it.alive }

    fun hud(): FiloHud {
        val b = boss
        val total = max(1, waveTotal)
        return FiloHud(
            score = score,
            lives = lives,
            bombs = bombs,
            wave = wave,
            chain = chain,
            multiplier = multiplier,
            weapon = weapon,
            shield = shield,
            invulnerable = invuln > 0f,
            bossHp = if (b != null) b.hp / b.maxHp.toFloat() else -1f,
            waveProgress = if (waveActive) ((waveKilled + waveEscaped) / total.toFloat()).coerceIn(0f, 1f) else 1f,
            kills = kills,
            status = status,
        )
    }

    /** Gemiyi verilen sütuna götürür (sürükleme girişi; sınırlar içinde kırpılır). */
    fun steerTo(x: Float) {
        targetX = x.coerceIn(PLAYER_MARGIN, WIDTH - PLAYER_MARGIN)
    }

    /** Gemiyi yatay kaydırır (parmak farkı). */
    fun steerBy(dx: Float) = steerTo(targetX + dx)

    /**
     * Bomba: düşman mermilerini siler, ekrandaki düşmanlara hasar verir.
     * Olaylar bir sonraki [step] ile teslim edilir.
     */
    fun bomb(): Boolean {
        if (status != FiloStatus.RUNNING || bombs <= 0) return false
        bombs--
        enemyBullets.clear()
        for (e in enemies) {
            if (!e.alive || e.y < -0.05f) continue
            damage(e, if (e.kind == EnemyKind.BOSS) BOSS_BOMB_DAMAGE else BOMB_DAMAGE)
        }
        events += FiloEvent.Bomb
        return true
    }

    fun step(): List<FiloEvent> {
        if (status != FiloStatus.RUNNING) {
            events.clear()
            return emptyList()
        }
        frames++
        val dt = STEP
        playerX = targetX
        invuln = max(0f, invuln - dt)

        // Dalga akışı.
        if (!wavesFrozen) {
            if (!waveActive) {
                gapTimer -= dt
                if (gapTimer <= 0f) startWave(wave + 1)
            } else {
                waveTime += dt
                while (queue.isNotEmpty() && queue[0].at <= waveTime) {
                    enemies += queue.removeAt(0).enemy
                }
                if (queue.isEmpty() && enemies.none { it.alive }) {
                    val bonus = WAVE_BONUS * wave
                    score += bonus
                    events += FiloEvent.WaveClear(wave, bonus)
                    waveActive = false
                    gapTimer = WAVE_GAP
                }
            }
        }

        // Otomatik ateş.
        fireTimer -= dt
        if (fireTimer <= 0f) {
            fireTimer += FIRE_INTERVAL
            fire()
        }

        // Mermiler.
        moveBullets(bullets, dt)
        moveBullets(enemyBullets, dt)

        // Düşmanlar.
        for (e in enemies) {
            if (!e.alive) continue
            e.t += dt
            e.flash = max(0f, e.flash - dt)
            moveEnemy(e)
            if (e.fireEvery > 0f && e.y > 0.03f && e.y < HEIGHT - 0.3f) {
                e.fireTimer -= dt
                if (e.fireTimer <= 0f) {
                    e.fireTimer += e.fireEvery
                    enemyFire(e)
                }
            }
            if (e.y > ESCAPE_Y || (e.pattern == Pattern.SWEEP && (e.x < -0.15f || e.x > WIDTH + 0.15f))) {
                e.alive = false
                waveEscaped++
            }
        }

        // Güç artırımları.
        val pit = powers.iterator()
        while (pit.hasNext()) {
            val p = pit.next()
            p.y += POWER_SPEED * dt
            if (p.y > ESCAPE_Y) {
                pit.remove()
                continue
            }
            if (hit(p.x, p.y, POWER_RADIUS, playerX, playerY, PLAYER_RADIUS + 0.01f)) {
                collect(p.kind)
                pit.remove()
            }
        }

        collideBullets()
        collidePlayer()

        // Zincir.
        if (chainTimer > 0f) {
            chainTimer -= dt
            if (chainTimer <= 0f) chain = 0
        }

        enemies.removeAll { !it.alive }
        val out = events.toList()
        events.clear()
        return out
    }

    // -----------------------------------------------------------------------
    // Dalgalar
    // -----------------------------------------------------------------------

    private fun startWave(n: Int) {
        wave = n
        waveTime = 0f
        waveKilled = 0
        waveEscaped = 0
        queue.clear()
        val rng = Random(mix(seed, n))
        val d = difficulty(n)
        val boss = n % BOSS_EVERY == 0
        if (boss) {
            val hp = 30 + 15 * (n / BOSS_EVERY - 1) + (10 * d).toInt()
            queue += Spawn(1.0f, Enemy(nextId++, EnemyKind.BOSS, Pattern.BOSS, 0.5f, 0.32f, 0.35f + 0.1f * d, 0f, 1, hp, 1.3f - 0.4f * d))
            repeat(4) { i ->
                queue += Spawn(2.5f + i * 0.5f, drone(rng, Pattern.SINE, d, i))
            }
        } else {
            val groups = 2 + rng.nextInt(2 + (2f * d).toInt())
            var t = 0.7f
            repeat(groups) { g ->
                val pattern = when (rng.nextInt(10)) {
                    0, 1, 2 -> Pattern.DIVE
                    3, 4, 5 -> Pattern.SINE
                    6, 7 -> Pattern.SWEEP
                    8 -> Pattern.RING
                    else -> Pattern.DRIFT
                }
                val count = if (pattern == Pattern.DRIFT) 2 + rng.nextInt(2) else 3 + rng.nextInt(3 + (3f * d).toInt())
                val cx = 0.2f + rng.nextFloat() * 0.6f
                val amp = 0.12f + rng.nextFloat() * 0.18f
                val phase = rng.nextFloat() * 2f * PI.toFloat()
                val dir = if (rng.nextBoolean()) 1 else -1
                val gap = if (pattern == Pattern.DRIFT) 0.9f else 0.32f
                for (i in 0 until count) {
                    val kind = when {
                        pattern == Pattern.DRIFT -> EnemyKind.ASTEROID
                        rng.nextFloat() < 0.12f * d + (if (g == groups - 1) 0.15f else 0f) -> EnemyKind.TANK
                        rng.nextFloat() < 0.25f + 0.35f * d -> EnemyKind.WASP
                        else -> EnemyKind.DRONE
                    }
                    queue += Spawn(t + i * gap, spawn(kind, pattern, cx, amp, phase, dir, d, i, rng))
                }
                t += count * gap + 1.0f
            }
        }
        waveTotal = queue.size
        waveActive = true
        events += FiloEvent.WaveStart(n, boss)
    }

    private fun drone(rng: Random, pattern: Pattern, d: Float, i: Int): Enemy =
        spawn(EnemyKind.DRONE, pattern, 0.25f + rng.nextFloat() * 0.5f, 0.2f, rng.nextFloat() * 6f, 1, d, i, rng)

    private fun spawn(kind: EnemyKind, pattern: Pattern, cx: Float, amp: Float, phase: Float, dir: Int, d: Float, i: Int, rng: Random): Enemy {
        val base = when (pattern) {
            Pattern.DIVE -> 0.42f + 0.28f * d
            Pattern.SINE -> 0.28f + 0.2f * d
            Pattern.SWEEP -> 0.45f + 0.25f * d
            Pattern.RING -> 0.3f + 0.15f * d
            Pattern.DRIFT -> 0.22f + 0.12f * d
            Pattern.BOSS -> 0.35f
        }
        val speed = base * (0.9f + rng.nextFloat() * 0.2f)
        val fireEvery = when (kind) {
            EnemyKind.DRONE -> if (rng.nextFloat() < 0.35f + 0.4f * d) 3.2f - 1.2f * d else 0f
            EnemyKind.WASP -> 2.4f - 0.8f * d
            EnemyKind.TANK -> 1.9f - 0.5f * d
            EnemyKind.ASTEROID -> 0f
            EnemyKind.BOSS -> 1.3f
        }
        val hp = kind.hp + (if (kind == EnemyKind.TANK) (2f * d).toInt() else 0)
        val e = Enemy(nextId++, kind, pattern, cx, amp, speed, phase + i * 0.6f, dir, hp, fireEvery)
        when (pattern) {
            Pattern.DIVE -> {
                e.x = cx + (i % 3 - 1) * 0.12f
                e.y = -0.08f
            }
            Pattern.SINE, Pattern.RING -> {
                e.x = cx
                e.y = -0.08f
            }
            Pattern.SWEEP -> {
                e.x = if (dir > 0) -0.1f else WIDTH + 0.1f
                e.y = 0.15f + amp
            }
            Pattern.DRIFT -> {
                e.x = 0.1f + rng.nextFloat() * 0.8f
                e.y = -0.1f
            }
            Pattern.BOSS -> {
                e.x = 0.5f
                e.y = -0.2f
            }
        }
        return e
    }

    private fun moveEnemy(e: Enemy) {
        val dt = STEP
        when (e.pattern) {
            Pattern.DIVE -> {
                e.y += e.speed * dt
                e.x += sin(e.t * 2f + e.phase) * 0.12f * dt
            }
            Pattern.SINE -> {
                e.y += e.speed * dt
                e.x = e.cx + e.amp * sin(e.t * 2.2f + e.phase)
            }
            Pattern.SWEEP -> {
                e.x += e.dir * e.speed * dt
                e.y += 0.05f * dt
            }
            Pattern.RING -> when (e.stage) {
                // Halkanın tepesine kadar in, sonra çember çiz, sonra aşağı çık.
                0 -> {
                    e.y += e.speed * 1.4f * dt
                    if (e.y >= RING_ENTRY - RING_RADIUS) {
                        e.y = RING_ENTRY - RING_RADIUS
                        e.stage = 1
                        e.t = 0f
                    }
                }
                1 -> {
                    val w = 1.6f
                    val a = -PI.toFloat() / 2f + e.dir * e.t * w
                    e.x = e.cx + RING_RADIUS * cos(a)
                    e.y = RING_ENTRY + RING_RADIUS * sin(a)
                    if (e.t * w >= 2f * PI.toFloat() * RING_LOOPS) e.stage = 2
                }
                else -> e.y += e.speed * 1.5f * dt
            }
            Pattern.DRIFT -> {
                e.y += e.speed * dt
                e.x += cos(e.phase) * 0.05f * dt
            }
            Pattern.BOSS -> when (e.stage) {
                0 -> {
                    e.y += e.speed * dt
                    if (e.y >= BOSS_Y) {
                        e.y = BOSS_Y
                        e.stage = 1
                        e.t = 0f
                    }
                }
                else -> {
                    e.x = 0.5f + e.amp * sin(e.t * 0.9f)
                    e.y = BOSS_Y + 0.03f * sin(e.t * 1.7f)
                }
            }
        }
        e.x = e.x.coerceIn(-0.2f, WIDTH + 0.2f)
    }

    private fun enemyFire(e: Enemy) {
        val d = difficulty(wave)
        val speed = ENEMY_BULLET_SPEED * (1f + 0.5f * d)
        when (e.kind) {
            EnemyKind.DRONE, EnemyKind.WASP -> aimed(e.x, e.y, speed)
            EnemyKind.TANK -> spread(e.x, e.y, speed * 0.9f, 3, 0.35f)
            EnemyKind.BOSS -> {
                val low = e.hp < e.maxHp / 2
                spread(e.x, e.y + 0.08f, speed, if (low) 7 else 5, 0.28f)
                if (low) aimed(e.x, e.y + 0.08f, speed * 1.2f)
            }
            EnemyKind.ASTEROID -> Unit
        }
    }

    private fun aimed(x: Float, y: Float, speed: Float) {
        val dx = playerX - x
        val dy = playerY - y
        val len = max(1e-3f, sqrt(dx * dx + dy * dy))
        enemyBullets += Bullet(x, y, dx / len * speed, dy / len * speed, ENEMY_BULLET_RADIUS)
    }

    private fun spread(x: Float, y: Float, speed: Float, count: Int, step: Float) {
        val base = atan2(playerY - y, playerX - x)
        for (i in 0 until count) {
            val a = base + (i - (count - 1) / 2f) * step
            enemyBullets += Bullet(x, y, cos(a) * speed, sin(a) * speed, ENEMY_BULLET_RADIUS)
        }
    }

    // -----------------------------------------------------------------------
    // Oyuncu ve çarpışma
    // -----------------------------------------------------------------------

    private fun fire() {
        val y = playerY - PLAYER_RADIUS
        when (weapon) {
            1 -> bullets += Bullet(playerX, y, 0f, -BULLET_SPEED, BULLET_RADIUS)
            2 -> {
                bullets += Bullet(playerX - 0.022f, y, 0f, -BULLET_SPEED, BULLET_RADIUS)
                bullets += Bullet(playerX + 0.022f, y, 0f, -BULLET_SPEED, BULLET_RADIUS)
            }
            else -> {
                bullets += Bullet(playerX, y, 0f, -BULLET_SPEED, BULLET_RADIUS)
                bullets += Bullet(playerX - 0.02f, y, -0.5f, -BULLET_SPEED * 0.95f, BULLET_RADIUS)
                bullets += Bullet(playerX + 0.02f, y, 0.5f, -BULLET_SPEED * 0.95f, BULLET_RADIUS)
            }
        }
        events += FiloEvent.Shot
    }

    private fun moveBullets(list: ArrayList<Bullet>, dt: Float) {
        val it = list.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.x += b.vx * dt
            b.y += b.vy * dt
            if (b.y < -0.1f || b.y > ESCAPE_Y || b.x < -0.1f || b.x > WIDTH + 0.1f) it.remove()
        }
    }

    private fun hit(x1: Float, y1: Float, r1: Float, x2: Float, y2: Float, r2: Float): Boolean {
        val dx = x1 - x2
        val dy = y1 - y2
        val r = r1 + r2
        return dx * dx + dy * dy <= r * r
    }

    private fun collideBullets() {
        val it = bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            var consumed = false
            for (e in enemies) {
                if (!e.alive || e.y < -0.02f) continue
                if (hit(b.x, b.y, b.radius, e.x, e.y, e.kind.radius)) {
                    damage(e, 1)
                    consumed = true
                    break
                }
            }
            if (consumed) it.remove()
        }
    }

    private fun damage(e: Enemy, amount: Int) {
        if (!e.alive) return
        e.hp -= amount
        e.flash = 0.12f
        if (e.hp > 0) {
            events += FiloEvent.EnemyHit(e.x, e.y)
            return
        }
        e.alive = false
        kills++
        waveKilled++
        chain++
        chainTimer = CHAIN_WINDOW
        val points = e.kind.points.toLong() * multiplier
        score += points
        events += FiloEvent.EnemyDown(e.kind, e.x, e.y, points)
        if (e.kind == EnemyKind.BOSS) {
            events += FiloEvent.BossDown(points)
            enemyBullets.clear()
            powers += Power(e.x, e.y, PowerKind.BOMB)
            return
        }
        val rng = Random(mix(seed, e.id, 0x33))
        if (rng.nextFloat() < DROP_CHANCE) {
            val roll = rng.nextFloat()
            val kind = when {
                roll < 0.35f -> if (weapon < MAX_WEAPON) PowerKind.WEAPON else PowerKind.SCORE
                roll < 0.6f -> PowerKind.SHIELD
                roll < 0.75f -> PowerKind.BOMB
                else -> PowerKind.SCORE
            }
            powers += Power(e.x, e.y, kind)
        }
    }

    private fun collect(kind: PowerKind) {
        when (kind) {
            PowerKind.WEAPON -> weapon = min(MAX_WEAPON, weapon + 1)
            PowerKind.SHIELD -> shield = true
            PowerKind.BOMB -> bombs = min(MAX_BOMBS, bombs + 1)
            PowerKind.SCORE -> score += SCORE_GIFT * multiplier
        }
        events += FiloEvent.PowerUp(kind)
    }

    private fun collidePlayer() {
        if (invuln > 0f) return
        var struck = false
        val bit = enemyBullets.iterator()
        while (bit.hasNext()) {
            val b = bit.next()
            if (hit(b.x, b.y, b.radius, playerX, playerY, PLAYER_RADIUS)) {
                bit.remove()
                struck = true
                break
            }
        }
        if (!struck) {
            for (e in enemies) {
                if (e.alive && hit(e.x, e.y, e.kind.radius * 0.8f, playerX, playerY, PLAYER_RADIUS)) {
                    struck = true
                    if (e.kind != EnemyKind.BOSS) damage(e, e.hp)
                    break
                }
            }
        }
        if (!struck) return
        if (shield) {
            shield = false
            invuln = SHIELD_INVULN_TIME
            enemyBullets.removeAll { hit(it.x, it.y, it.radius, playerX, playerY, 0.2f) }
            events += FiloEvent.ShieldUsed
            return
        }
        lives--
        chain = 0
        chainTimer = 0f
        weapon = max(1, weapon - 1)
        enemyBullets.clear()
        invuln = INVULN_TIME
        events += FiloEvent.PlayerHit(lives)
        if (lives <= 0) {
            status = FiloStatus.OVER
            events += FiloEvent.Over
        }
    }

    // -----------------------------------------------------------------------
    // Test kancaları
    // -----------------------------------------------------------------------

    /** Sabit duran (hız 0, genlik 0) bir düşman ekler. */
    internal fun spawnForTest(kind: EnemyKind, x: Float, y: Float, pattern: Pattern = Pattern.SINE, fireEvery: Float = 0f): Enemy {
        val e = Enemy(nextId++, kind, pattern, x, 0f, 0f, 0f, 1, kind.hp, fireEvery)
        e.x = x
        e.y = y
        enemies += e
        return e
    }

    /** Dalga akışını durdurur: yeni dalga başlamaz, dalga temizlenmez. */
    internal fun freezeWavesForTest() {
        wavesFrozen = true
        waveActive = false
        queue.clear()
    }

    /** Bir sonraki adımda [n]. dalgayı başlatır. */
    internal fun jumpToWaveForTest(n: Int) {
        wavesFrozen = false
        waveActive = false
        wave = n - 1
        gapTimer = 0f
        queue.clear()
        enemies.clear()
        enemyBullets.clear()
    }

    internal fun addEnemyBulletForTest(x: Float, y: Float, vx: Float, vy: Float) {
        enemyBullets += Bullet(x, y, vx, vy, ENEMY_BULLET_RADIUS)
    }

    internal fun addPowerForTest(kind: PowerKind, x: Float, y: Float) {
        powers += Power(x, y, kind)
    }

    internal fun setWeaponForTest(level: Int) {
        weapon = level.coerceIn(1, MAX_WEAPON)
    }

    internal fun setLivesForTest(n: Int) {
        lives = n
    }

    internal fun grantShieldForTest() {
        shield = true
    }

    internal fun damageForTest(e: Enemy, amount: Int) = damage(e, amount)
}
