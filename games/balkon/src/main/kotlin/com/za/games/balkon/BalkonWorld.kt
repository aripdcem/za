package com.za.games.balkon

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Balkon simülasyonu: oyuncu balkon kenarında (derinlik 0) sağa sola kayar ve
 * aşağıdaki sokakta nişan aldığı noktaya atar. Atış [flightTime] kadar havada
 * kalır; bu sürede rüzgâr iniş noktasını kaydırır, hedefler yürümeye devam eder.
 * İnişte yarıçap içindeki hedefler sayılır. Ardışık isabetler mega sayacını
 * doldurur; yasak hedef (kapıcı, komşu) ceza ve kısa bir donma getirir.
 * Seviye: süre dolmadan gereken isabet sayısına ulaşmak; kalan süre bonus olur.
 * 1/60 s sabit adım; aynı tohum + aynı atışlar = aynı oyun.
 */
class BalkonWorld(
    val seed: Long,
    private val windEnabled: Boolean = true,
    private val spawning: Boolean = true,
) {

    companion object {
        const val STEP = 1f / 60f
        const val LEVEL_TIME = 45f

        /** Bundan yakına (duvar dibine) atılamaz. */
        const val MIN_DEPTH = 0.18f
        const val RADIUS = 0.035f
        const val MEGA_RADIUS = 0.10f
        const val MAX_SHOTS = 3
        const val COMBO_FOR_MEGA = 5
        const val MAX_CHARGES = 3
        const val STUN_TIME = 1.5f
        const val FORBIDDEN_PENALTY = 50
        const val CLEAR_PAUSE = 2.2f
        const val AVATAR_SPEED = 2.2f
        const val HIT_ANIM = 0.7f
        const val MAX_WIND = 0.14f
        const val MAX_TARGETS_BASE = 7

        fun required(level: Int): Int = 6 + 2 * level

        /** Uçuş süresi (s): yakın 0,45; en uzak 0,95. */
        fun flightTime(depth: Float): Float = 0.45f + 0.5f * depth

        fun dailySeed(epochDay: Long): Long = Rng.mix(epochDay, 0x42, 0x4B)
    }

    private val rng = Rng(seed)
    private val events = ArrayList<BalkonEvent>()

    /** Adımlar arasında (atış gibi) üretilen olaylar; bir sonraki adımda teslim edilir. */
    private val pending = ArrayList<BalkonEvent>()
    private var nextId = 1

    val targets = ArrayList<Target>()
    val shots = ArrayList<Shot>()

    var status = BalkonStatus.RUNNING
        private set
    var level = 1
        private set
    var hits = 0
        private set
    var score = 0L
        private set
    var timeLeft = LEVEL_TIME
        private set
    var combo = 0
        private set
    var charges = 0
        private set
    var stun = 0f
        private set
    var wind = 0f
        private set
    var avatarX = 0.5f
        private set
    var avatarTarget = 0.5f
        private set

    /** Atış hareketi göstergesi: son atıştan bu yana geçen süre (animasyon için). */
    var throwAge = 10f
        private set
    var frames = 0
        private set
    private var levelTime = 0f
    private var clearTimer = 0f
    private var spawnTimer = 0.4f
    private var baseWind = 0f

    init {
        rollWind()
        // Sokak boş başlamasın: birkaç hedef ekranın içinde.
        if (spawning) repeat(3) { spawn(inside = true) }
    }

    val required: Int get() = required(level)

    fun hud(): BalkonHud = BalkonHud(
        score = score,
        level = level,
        hits = hits,
        required = required,
        seconds = timeLeft.toInt().coerceAtLeast(0),
        timeFraction = (timeLeft / LEVEL_TIME).coerceIn(0f, 1f),
        wind = wind,
        combo = combo,
        charges = charges,
        stunned = stun > 0f,
        status = status,
    )

    private fun emit(event: BalkonEvent) {
        events.add(event)
    }

    /** Seviyeye göre rüzgâr: işareti rastgele, şiddeti seviyeyle artar. */
    private fun rollWind() {
        if (!windEnabled) {
            baseWind = 0f
            wind = 0f
            return
        }
        val magnitude = min(MAX_WIND, 0.03f + 0.022f * (level - 1))
        baseWind = if (rng.chance(0.5f)) magnitude else -magnitude
        wind = baseWind
    }

    /** Testler için: rüzgârı sabitler (salınım kapalıysa da uygulanır). */
    internal fun setWind(value: Float) {
        baseWind = value
        wind = value
    }

    /**
     * Nişan alınan noktaya atar; balkondaki oyuncu o x'e kayar. Duvar dibine,
     * donmuşken, seviye arasında ya da mega şarjı yokken atılamaz.
     */
    fun throwAt(x: Float, y: Float, mega: Boolean): Boolean {
        if (status != BalkonStatus.RUNNING || stun > 0f) return false
        if (y < MIN_DEPTH || y > 1f) return false
        if (shots.size >= MAX_SHOTS) return false
        if (mega && charges <= 0) return false
        if (mega) charges--
        val tx = x.coerceIn(0f, 1f)
        val duration = flightTime(y)
        shots += Shot(nextId++, avatarX, tx, y, duration, mega, wind * duration)
        avatarTarget = tx
        throwAge = 0f
        pending += BalkonEvent.Throw(mega)
        return true
    }

    /** Bir kare ilerletir. */
    fun step(): List<BalkonEvent> {
        events.clear()
        events.addAll(pending)
        pending.clear()
        if (status == BalkonStatus.OVER) return events.toList()
        frames++
        val dt = STEP
        throwAge += dt
        if (stun > 0f) stun = max(0f, stun - dt)

        // Balkondaki oyuncu nişana doğru kayar.
        val dx = avatarTarget - avatarX
        val maxMove = AVATAR_SPEED * dt
        avatarX += dx.coerceIn(-maxMove, maxMove)

        when (status) {
            BalkonStatus.RUNNING -> {
                levelTime += dt
                timeLeft -= dt
                if (windEnabled) wind = baseWind * (1f + 0.35f * sin(levelTime * 0.9f))
                spawnTimer -= dt
                if (spawning && spawnTimer <= 0f) {
                    val aliveCount = targets.count { it.alive }
                    if (aliveCount < maxTargets()) spawn(inside = false)
                    spawnTimer = spawnInterval() * rng.range(0.7f, 1.3f)
                }
            }
            BalkonStatus.CLEARED -> {
                clearTimer -= dt
                if (clearTimer <= 0f) nextLevel()
            }
            BalkonStatus.OVER -> Unit
        }

        moveTargets(dt)
        flyShots(dt)

        if (status == BalkonStatus.RUNNING && timeLeft <= 0f) {
            timeLeft = 0f
            status = BalkonStatus.OVER
            emit(BalkonEvent.Over)
        }
        return events.toList()
    }

    private fun speedScale(): Float = min(1.8f, 1f + 0.08f * (level - 1))

    private fun spawnInterval(): Float = max(0.55f, 1.6f - 0.12f * (level - 1))

    private fun maxTargets(): Int = min(12, MAX_TARGETS_BASE + level)

    private fun moveTargets(dt: Float) {
        val it = targets.iterator()
        while (it.hasNext()) {
            val t = it.next()
            t.age += dt
            if (t.hit) {
                t.hitAge += dt
                if (t.hitAge >= HIT_ANIM) it.remove()
                continue
            }
            if (t.pause > 0f) {
                t.pause -= dt
            } else {
                t.x += t.dir * t.speed * speedScale() * dt
                if (t.kind == TargetKind.CAT && rng.chance(0.35f * dt)) t.pause = rng.range(0.4f, 1.0f)
            }
            t.y = when (t.kind) {
                TargetKind.PIGEON -> t.lane.depth + 0.015f * sin(t.age * 6f + t.phase)
                TargetKind.CAT -> t.lane.depth + 0.008f * sin(t.age * 3f + t.phase)
                else -> t.lane.depth
            }
            if (t.x < -0.2f || t.x > 1.2f) it.remove()
        }
    }

    private fun flyShots(dt: Float) {
        val it = shots.iterator()
        while (it.hasNext()) {
            val s = it.next()
            s.t += dt / s.duration
            if (s.t >= 1f) {
                it.remove()
                land(s)
            }
        }
    }

    private fun land(s: Shot) {
        val x = s.landX
        val y = s.toY
        if (status != BalkonStatus.RUNNING) {
            emit(BalkonEvent.Land(x, y, s.mega, 0))
            return
        }
        val reach = if (s.mega) MEGA_RADIUS else RADIUS
        val struck = targets.filter { t ->
            t.alive && distance(t.x, t.y, x, y) <= reach + t.kind.radius
        }
        var any = false
        var forbidden = false
        for (t in struck) {
            t.hit = true
            if (t.kind.forbidden) {
                forbidden = true
                score = max(0L, score - FORBIDDEN_PENALTY)
                emit(BalkonEvent.Forbidden(t.kind, t.x, t.y))
            } else {
                any = true
                val points = t.kind.points * (if (s.mega) 2 else 1)
                score += points
                hits++
                if (t.kind.bonusTime > 0) {
                    timeLeft = min(LEVEL_TIME, timeLeft + t.kind.bonusTime)
                    emit(BalkonEvent.Bonus(t.kind.bonusTime, t.x, t.y))
                }
                emit(BalkonEvent.Hit(t.kind, t.x, t.y, points))
            }
        }
        emit(BalkonEvent.Land(x, y, s.mega, struck.size))
        when {
            forbidden -> {
                combo = 0
                stun = STUN_TIME
            }
            !any -> {
                combo = 0
                emit(BalkonEvent.Miss)
            }
            else -> {
                combo++
                if (combo >= COMBO_FOR_MEGA) {
                    combo = 0
                    if (charges < MAX_CHARGES) charges++
                    emit(BalkonEvent.MegaReady)
                }
            }
        }
        if (hits >= required) clearLevel()
    }

    private fun clearLevel() {
        val bonus = (timeLeft * 10f).toInt().coerceAtLeast(0)
        score += bonus
        status = BalkonStatus.CLEARED
        clearTimer = CLEAR_PAUSE
        emit(BalkonEvent.LevelClear(level, bonus))
    }

    private fun nextLevel() {
        level++
        hits = 0
        timeLeft = LEVEL_TIME
        levelTime = 0f
        spawnTimer = 0.3f
        status = BalkonStatus.RUNNING
        rollWind()
        emit(BalkonEvent.LevelStart(level))
    }

    private fun distance(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    /** Şerit ve tür seçip ekran dışından (ya da başlangıçta içinden) hedef çıkarır. */
    private fun spawn(inside: Boolean) {
        val lane = pickLane()
        val kind = pickKind(lane) ?: return
        val dir = if (lane.isRoad) lane.trafficDir else if (rng.chance(0.5f)) 1 else -1
        val x = when {
            inside -> rng.range(0.15f, 0.85f)
            dir > 0 -> -0.12f
            else -> 1.12f
        }
        if (kind.vehicle && targets.any { it.alive && it.lane == lane && abs(it.x - x) < 0.28f }) return
        val speed = rng.range(kind.minSpeed, kind.maxSpeed)
        targets += Target(nextId++, kind, lane, x, dir, speed, rng.range(0f, 6.28f))
    }

    /** Testler için: verilen hedefi doğrudan yerleştirir. */
    internal fun spawnTarget(kind: TargetKind, lane: Lane, x: Float, dir: Int, speed: Float): Target {
        val t = Target(nextId++, kind, lane, x, dir, speed, 0f)
        targets += t
        return t
    }

    private fun pickLane(): Lane {
        val r = rng.nextFloat() * 10f
        return when {
            r < 3f -> Lane.NEAR_WALK
            r < 5f -> Lane.ROAD_A
            r < 7f -> Lane.ROAD_B
            else -> Lane.FAR_WALK
        }
    }

    private fun pickKind(lane: Lane): TargetKind? {
        val table = ArrayList<Pair<TargetKind, Float>>()
        if (lane.isRoad) {
            table += TargetKind.BIKE to 3f
            table += TargetKind.CAR to 3f
            if (level >= 2) table += TargetKind.SCOOTER to 2f
        } else {
            table += TargetKind.PIGEON to 5f
            table += TargetKind.CAT to 3f
            table += TargetKind.BALL to 2f
            table += TargetKind.SIMIT to 0.5f
            if (level >= 2) table += TargetKind.JANITOR to 1.2f
            if (level >= 3) table += TargetKind.NEIGHBOR to 1.2f
        }
        val total = table.sumOf { it.second.toDouble() }.toFloat()
        var r = rng.nextFloat() * total
        for ((kind, w) in table) {
            if (r < w) return kind
            r -= w
        }
        return table.lastOrNull()?.first
    }
}
