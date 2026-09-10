package com.za.games.dalgic

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Dalgıç simülasyonu: dikey arena [WIDTH]×[HEIGHT]; su yüzeyi [SURFACE_Y],
 * deniz tabanı [FLOOR_Y]. Denizaltı parmak sürüklemesiyle ([steerBy]) 2B
 * hedefe [SUB_SPEED] ile gider; yönü son yatay hareketten gelir ve torpidolar
 * dalmışken o yöne kendiliğinden atılır.
 *
 * Oksijen dalmışken saniyede 1 azalır, yüzeyde hızla dolar; biterse can
 * gider. Dalgıçlar dokununca toplanır ([CAPACITY] kadar); yüzeye çıkınca
 * teslim edilir: dalgıç başına [DIVER_POINTS] + dalga bonusu, tam yükte
 * [FULL_BONUS], dalga bir artar (düşmanlar hızlanır, doğum sıklaşır, akıntılar
 * yeniden dizilir). Daldıktan sonra dalgıçsız yüzeye çıkmak can götürür
 * (Seaquest kuralı): oksijeni bedavaya doldurmanın bedeli.
 *
 * Şeritler ([LANES]) soldan ya da sağdan dalgıç, köpekbalığı ve düşman
 * denizaltı doğurur; mayınlar alt şeritlerde zincirli durur. Boğaz akıntısı:
 * üç bant denizaltıyı ve dalgıçları yana sürükler. Sabit 1/60 s adım; aynı
 * tohum + aynı giriş dizisi = aynı koşu.
 */
class DalgicWorld(val seed: Long) {

    companion object {
        const val STEP = 1f / 60f
        const val WIDTH = 1f
        const val HEIGHT = 1.6f
        const val SURFACE_Y = 0.14f
        const val FLOOR_Y = 1.5f
        const val SUB_R = 0.04f

        /** Yüzeydeyken denizaltı merkezinin y'si (üst yarısı su üstünde). */
        const val SURFACE_LEVEL = SURFACE_Y + SUB_R * 0.5f
        const val SUB_SPEED = 0.85f
        const val OXYGEN_MAX = 30f
        const val OXYGEN_REFILL = 10f
        const val OXYGEN_LOW = 8f
        const val CAPACITY = 6
        const val LIVES = 3
        const val INVULN = 2f
        const val FIRE_INTERVAL = 0.6f
        const val TORPEDO_SPEED = 1.4f
        const val TORPEDO_R = 0.012f
        const val ENEMY_TORPEDO_SPEED = 0.85f
        const val ENEMY_FIRE_EVERY = 2.4f
        const val LANES = 6
        const val LANE0 = 0.34f
        const val LANE_GAP = 0.21f
        const val DIVER_R = 0.03f
        const val SPAWN_BASE = 1.7f
        const val SPAWN_MIN = 0.7f
        const val DIVER_POINTS = 50
        const val FULL_BONUS = 300
        const val WAVE_BONUS = 10
        const val MAX_MINES = 3
        const val CURRENT_BANDS = 3
        const val CURRENT_HALF = 0.09f
        const val DIVE_DEPTH = 0.05f

        fun laneY(i: Int): Float = LANE0 + LANE_GAP * i

        fun dailySeed(epochDay: Long): Long = mix(epochDay, 0x44, 0x4C)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }

    private val rng = Random(mix(seed, 0x44))

    var subX = WIDTH / 2f
        private set
    var subY = SURFACE_LEVEL
        private set
    var facing = 1
        private set
    private var targetX = subX
    private var targetY = subY

    var lives = LIVES
        private set
    var score = 0
        private set
    var divers = 0
        private set
    var rescued = 0
        private set
    var wave = 0
        private set
    var oxygen = OXYGEN_MAX
        private set
    var invuln = 0f
        private set
    var status = DalgicStatus.RUNNING
        private set
    var frames = 0
        private set

    val diverList = ArrayList<Diver>()
    val foes = ArrayList<Foe>()
    val torpedoes = ArrayList<Torpedo>()
    val currents = ArrayList<Current>()

    private var spawnTimer = 1f
    private var fireTimer = 0.3f
    private var hasDived = false
    private var lowWarned = false
    private var nextId = 1
    private var spawning = true

    val atSurface: Boolean get() = subY <= SURFACE_LEVEL + 0.005f

    /** Düşman hızı ve doğum sıklığı çarpanı. */
    val difficulty: Float get() = 1f + 0.12f * wave

    init {
        regenCurrents()
    }

    fun hud(): DalgicHud = DalgicHud(
        score = score,
        lives = lives,
        divers = divers,
        oxygen = (oxygen / OXYGEN_MAX).coerceIn(0f, 1f),
        wave = wave,
        rescued = rescued,
        status = status,
        facing = facing,
        atSurface = atSurface,
        invulnerable = invuln > 0f,
    )

    /** Parmak sürüklemesi: hedef [dx], [dy] birim kayar; yön son yatay harekettir. */
    fun steerBy(dx: Float, dy: Float) {
        targetX = (targetX + dx).coerceIn(SUB_R, WIDTH - SUB_R)
        targetY = (targetY + dy).coerceIn(SURFACE_LEVEL, FLOOR_Y - SUB_R)
        if (abs(dx) > 1e-4f) facing = if (dx > 0f) 1 else -1
    }

    fun steerTo(x: Float, y: Float) {
        val dx = x - targetX
        targetX = x.coerceIn(SUB_R, WIDTH - SUB_R)
        targetY = y.coerceIn(SURFACE_LEVEL, FLOOR_Y - SUB_R)
        if (abs(dx) > 1e-4f) facing = if (dx > 0f) 1 else -1
    }

    fun step(): List<DalgicEvent> {
        val out = ArrayList<DalgicEvent>(2)
        if (status == DalgicStatus.OVER) return out
        frames++
        moveSub()
        if (invuln > 0f) invuln = (invuln - STEP).coerceAtLeast(0f)
        if (atSurface) {
            oxygen = min(OXYGEN_MAX, oxygen + OXYGEN_REFILL * STEP)
            fireTimer = 0.2f
            if (hasDived) surface(out)
            if (status == DalgicStatus.OVER) return out
        } else {
            if (subY > SURFACE_LEVEL + DIVE_DEPTH) hasDived = true
            oxygen -= STEP
            if (!lowWarned && oxygen <= OXYGEN_LOW) {
                lowWarned = true
                out += DalgicEvent.OxygenLow
            }
            if (oxygen <= 0f) {
                loseLife(LifeCause.OXYGEN, out)
                return out
            }
            fireTimer -= STEP
            if (fireTimer <= 0f) {
                fireTimer = FIRE_INTERVAL
                torpedoes += Torpedo(subX + facing * SUB_R * 1.3f, subY, facing * TORPEDO_SPEED, friendly = true)
                out += DalgicEvent.Shot
            }
        }
        if (spawning) {
            spawnTimer -= STEP
            if (spawnTimer <= 0f) {
                spawn()
                spawnTimer = (SPAWN_BASE / difficulty).coerceAtLeast(SPAWN_MIN) * (0.8f + rng.nextFloat() * 0.4f)
            }
        }
        moveThings(out)
        collide(out)
        prune()
        return out
    }

    private fun moveSub() {
        val dx = targetX - subX
        val dy = targetY - subY
        val dist = hypot(dx, dy)
        val maxStep = SUB_SPEED * STEP
        if (dist <= maxStep) {
            subX = targetX
            subY = targetY
        } else {
            subX += dx / dist * maxStep
            subY += dy / dist * maxStep
        }
        for (c in currents) {
            if (abs(subY - c.y) <= c.half) {
                val drift = c.dir * c.speed * STEP
                subX = (subX + drift).coerceIn(SUB_R, WIDTH - SUB_R)
                targetX = (targetX + drift).coerceIn(SUB_R, WIDTH - SUB_R)
            }
        }
    }

    private fun surface(out: MutableList<DalgicEvent>) {
        hasDived = false
        lowWarned = false
        out += DalgicEvent.Surfaced
        if (divers > 0) {
            val points = divers * (DIVER_POINTS + WAVE_BONUS * wave) + (if (divers >= CAPACITY) FULL_BONUS else 0)
            score += points
            rescued += divers
            wave++
            out += DalgicEvent.Delivered(divers, points, wave)
            divers = 0
            regenCurrents()
        } else {
            loseLife(LifeCause.EMPTY_SURFACE, out)
        }
    }

    private fun loseLife(cause: LifeCause, out: MutableList<DalgicEvent>) {
        lives--
        divers = 0
        out += DalgicEvent.LifeLost(cause, lives)
        if (lives <= 0) {
            status = DalgicStatus.OVER
            out += DalgicEvent.Over
            return
        }
        subX = WIDTH / 2f
        subY = SURFACE_LEVEL
        targetX = subX
        targetY = subY
        oxygen = OXYGEN_MAX
        invuln = INVULN
        hasDived = false
        lowWarned = false
        torpedoes.clear()
    }

    private fun spawn() {
        val lane = rng.nextInt(LANES)
        val y = laneY(lane)
        val fromLeft = rng.nextBoolean()
        val dir = if (fromLeft) 1 else -1
        val x = if (fromLeft) -0.08f else WIDTH + 0.08f
        val busy = diverList.any { abs(it.y - y) < 0.12f && abs(it.x - x) < 0.3f } ||
            foes.any { it.kind != FoeKind.MINE && abs(it.y - y) < 0.12f && abs(it.x - x) < 0.3f }
        if (busy) return
        val roll = rng.nextFloat()
        val d = difficulty
        when {
            roll < 0.36f -> diverList += Diver(nextId++, x, y, dir, 0.12f + rng.nextFloat() * 0.08f)
            roll < 0.68f -> foes += Foe(nextId++, FoeKind.SHARK, x, y, dir, (0.22f + rng.nextFloat() * 0.14f) * d, 0.02f + rng.nextFloat() * 0.03f, rng.nextFloat() * 6.28f)
            roll < 0.9f -> foes += Foe(nextId++, FoeKind.ENEMY_SUB, x, y, dir, (0.2f + rng.nextFloat() * 0.1f) * d, 0f, 0f)
            lane >= 3 && foes.count { it.kind == FoeKind.MINE && it.alive } < MAX_MINES ->
                foes += Foe(nextId++, FoeKind.MINE, 0.15f + rng.nextFloat() * 0.7f, y + 0.05f, 0, 0f, 0.015f, rng.nextFloat() * 6.28f)
            else -> diverList += Diver(nextId++, x, y, dir, 0.12f + rng.nextFloat() * 0.08f)
        }
    }

    private fun regenCurrents() {
        currents.clear()
        val lanes = (0 until LANES).shuffled(rng).take(CURRENT_BANDS)
        for (l in lanes) {
            currents += Current(laneY(l), CURRENT_HALF, if (rng.nextBoolean()) 1 else -1, (0.1f + rng.nextFloat() * 0.1f) * (1f + 0.05f * wave))
        }
    }

    private fun driftFor(y: Float): Float {
        var drift = 0f
        for (c in currents) if (abs(y - c.y) <= c.half) drift += c.dir * c.speed * STEP
        return drift
    }

    private fun moveThings(out: MutableList<DalgicEvent>) {
        for (dv in diverList) {
            dv.t += STEP
            dv.x += dv.dir * dv.speed * STEP + driftFor(dv.y)
        }
        for (f in foes) {
            f.t += STEP
            if (f.flash > 0f) f.flash = (f.flash - STEP).coerceAtLeast(0f)
            when (f.kind) {
                FoeKind.SHARK -> {
                    f.x += f.dir * f.speed * STEP
                    f.y = f.baseY + f.wobble * sin(f.t * 3f + f.phase)
                }
                FoeKind.ENEMY_SUB -> {
                    f.x += f.dir * f.speed * STEP
                    f.fireTimer -= STEP
                    if (f.fireTimer <= 0f && f.x > 0.05f && f.x < WIDTH - 0.05f) {
                        f.fireTimer = ENEMY_FIRE_EVERY
                        torpedoes += Torpedo(f.x + f.dir * FoeKind.ENEMY_SUB.radius * 1.3f, f.y, f.dir * ENEMY_TORPEDO_SPEED, friendly = false)
                        out += DalgicEvent.EnemyShot
                    }
                }
                FoeKind.MINE -> f.y = f.baseY + f.wobble * sin(f.t * 2f + f.phase)
            }
        }
        for (t in torpedoes) t.x += t.vx * STEP
    }

    private fun collide(out: MutableList<DalgicEvent>) {
        // Dost torpido → düşman ve düşman torpidosu.
        for (t in torpedoes) {
            if (!t.friendly || t.x < -0.05f || t.x > WIDTH + 0.05f) continue
            var used = false
            for (f in foes) {
                if (!f.alive) continue
                if (hypot(f.x - t.x, f.y - t.y) <= f.kind.radius + TORPEDO_R) {
                    f.alive = false
                    val points = f.kind.points + (if (f.kind == FoeKind.MINE) 0 else WAVE_BONUS * wave)
                    score += points
                    out += DalgicEvent.FoeDown(f.kind, f.x, f.y, points)
                    used = true
                    break
                }
            }
            if (!used) {
                for (e in torpedoes) {
                    if (e.friendly || e.x < -1f) continue
                    if (hypot(e.x - t.x, e.y - t.y) <= TORPEDO_R * 2f + 0.01f) {
                        e.x = -9f
                        used = true
                        break
                    }
                }
            }
            if (used) t.x = -9f
        }
        // Dalgıçlar.
        for (dv in diverList) {
            if (!dv.alive || divers >= CAPACITY) continue
            if (hypot(dv.x - subX, dv.y - subY) <= SUB_R + DIVER_R) {
                dv.alive = false
                divers++
                out += DalgicEvent.DiverRescued(divers, dv.x, dv.y)
                if (divers >= CAPACITY) out += DalgicEvent.Full
            }
        }
        if (invuln > 0f) return
        // Düşman torpidosu → denizaltı.
        for (t in torpedoes) {
            if (t.friendly || t.x < -1f) continue
            if (hypot(t.x - subX, t.y - subY) <= SUB_R + TORPEDO_R) {
                t.x = -9f
                loseLife(LifeCause.TORPEDO, out)
                return
            }
        }
        // Düşmanlar → denizaltı.
        for (f in foes) {
            if (!f.alive) continue
            if (hypot(f.x - subX, f.y - subY) <= SUB_R + f.kind.radius) {
                if (f.kind == FoeKind.MINE) f.alive = false
                loseLife(
                    when (f.kind) {
                        FoeKind.SHARK -> LifeCause.SHARK
                        FoeKind.ENEMY_SUB -> LifeCause.ENEMY_SUB
                        FoeKind.MINE -> LifeCause.MINE
                    },
                    out,
                )
                return
            }
        }
    }

    private fun prune() {
        diverList.removeAll { !it.alive || it.x < -0.15f || it.x > WIDTH + 0.15f }
        foes.removeAll { !it.alive || it.x < -0.2f || it.x > WIDTH + 0.2f }
        torpedoes.removeAll { it.x < -0.1f || it.x > WIDTH + 0.1f }
    }

    // ---- Test kancaları -------------------------------------------------

    /** Test: doğumu durdurur ve sahneyi boşaltır. */
    fun freezeSpawnForTest() {
        spawning = false
        diverList.clear()
        foes.clear()
        torpedoes.clear()
        currents.clear()
    }

    fun setSubForTest(x: Float, y: Float) {
        subX = x.coerceIn(SUB_R, WIDTH - SUB_R)
        subY = y.coerceIn(SURFACE_LEVEL, FLOOR_Y - SUB_R)
        targetX = subX
        targetY = subY
        if (subY > SURFACE_LEVEL + DIVE_DEPTH) hasDived = true
    }

    fun setOxygenForTest(value: Float) {
        oxygen = value
    }

    fun setWaveForTest(value: Int) {
        wave = value
    }

    fun setDiversForTest(count: Int) {
        divers = count.coerceIn(0, CAPACITY)
    }

    fun addDiverForTest(x: Float, y: Float, dir: Int = 1, speed: Float = 0.15f): Diver =
        Diver(nextId++, x, y, dir, speed).also { diverList += it }

    fun addFoeForTest(kind: FoeKind, x: Float, y: Float, dir: Int = -1, speed: Float = 0.3f): Foe =
        Foe(nextId++, kind, x, y, dir, speed, 0f, 0f).also { foes += it }

    fun addEnemyTorpedoForTest(x: Float, y: Float, vx: Float): Torpedo =
        Torpedo(x, y, vx, friendly = false).also { torpedoes += it }

    fun setCurrentsForTest(list: List<Current>) {
        currents.clear()
        currents += list
    }
}
