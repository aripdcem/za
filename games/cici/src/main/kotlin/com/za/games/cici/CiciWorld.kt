package com.za.games.cici

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cici simülasyonu — Bölüm 1: Uzayda. Dikey arena [WIDTH]×[HEIGHT]; beyaz
 * muhabbet kuşu Cici parmak sürüklemesiyle ([steerBy]) 2B hedefe [CICI_SPEED]
 * ile uçar. Kenarlardan süzülen ikramlar dokununca yakalanır: ballı yem 7,
 * kuş yemi 5, su 2 puan. Uzay kedileri yanlardan geçer (ilerledikçe Cici'nin
 * hizasına kıvrılır), kırmızı top kenarlardan seker ve hızlanır; ikisi de can
 * götürür ([LIVES] can, [INVULN] s dokunulmazlık).
 *
 * Sevinç: [JOY_WINDOW] s içinde art arda yakalamalar seriyi büyütür (puanlar
 * sabittir, kutlama büyür). Hareketsizlik: [IDLE_WARN] s kıpırdamayınca Cici
 * sıkılır, [IDLE_PENALTY] s'den sonra her saniye bir puan gider (sıfırın
 * altına inmez). Hareket parmak girişiyle ölçülür ([steerBy] / [steerTo]).
 *
 * Zorluk [RAMP_TIME] s boyunca doğrusal artar: ikram sıklığı ve hızı, kedi
 * sıklığı, hızı ve kıvrılması, top hızı; [SECOND_BALL_AT] s'de ikinci top.
 * Sabit 1/60 s adım; aynı tohum + aynı giriş dizisi = aynı koşu.
 */
class CiciWorld(val seed: Long, val chapter: CiciChapter = CiciChapter.SPACE) {

    companion object {
        const val STEP = 1f / 60f
        const val WIDTH = 1f
        const val HEIGHT = 1.6f
        const val CICI_R = 0.045f
        const val CICI_SPEED = 1.3f
        const val LIVES = 3
        const val INVULN = 2f
        const val IDLE_WARN = 2f
        const val IDLE_PENALTY = 3f
        const val IDLE_WARN_FRAMES = 120
        const val IDLE_PENALTY_FRAMES = 180
        const val JOY_WINDOW = 3f
        const val HAPPY_TIME = 1.5f
        const val RAMP_TIME = 180f
        const val TREAT_INTERVAL0 = 1.3f
        const val TREAT_INTERVAL_MIN = 0.7f
        const val TREAT_SPEED0 = 0.13f
        const val TREAT_SPEED_GAIN = 0.8f
        const val CAT_R = 0.055f
        const val CAT_DELAY = 6f
        const val CAT_INTERVAL0 = 8f
        const val CAT_INTERVAL_MIN = 3.5f
        const val CAT_SPEED0 = 0.2f
        const val CAT_SPEED_GAIN = 0.9f
        const val CAT_HOMING_MAX = 0.35f
        const val BALL_R = 0.04f
        const val BALL_DELAY = 3f
        const val BALL_SPEED0 = 0.24f
        const val BALL_SPEED_MAX = 0.55f
        const val SECOND_BALL_AT = 120f
        const val MAX_BALLS = 2

        /** Tehlike temasında yarıçap toplamının bu payı: kenar sıyırmaları affedilir. */
        const val HIT_TOLERANCE = 0.8f

        /** Zorluk payı 0..1: [RAMP_TIME] saniyede tam. */
        fun ramp(seconds: Float): Float = (seconds / RAMP_TIME).coerceIn(0f, 1f)
        fun treatInterval(seconds: Float): Float = TREAT_INTERVAL0 + (TREAT_INTERVAL_MIN - TREAT_INTERVAL0) * ramp(seconds)
        fun treatSpeedMul(seconds: Float): Float = 1f + TREAT_SPEED_GAIN * ramp(seconds)
        fun catInterval(seconds: Float): Float = CAT_INTERVAL0 + (CAT_INTERVAL_MIN - CAT_INTERVAL0) * ramp(seconds)
        fun catSpeedMul(seconds: Float): Float = 1f + CAT_SPEED_GAIN * ramp(seconds)

        /** Kedilerin Cici'ye kıvrılma gücü: ilk çeyrekte yok, sonra doğrusal artar. */
        fun catHoming(seconds: Float): Float {
            val r = ramp(seconds)
            return if (r < 0.25f) 0f else CAT_HOMING_MAX * (r - 0.25f) / 0.75f
        }

        fun ballSpeed(seconds: Float): Float = BALL_SPEED0 + (BALL_SPEED_MAX - BALL_SPEED0) * ramp(seconds)

        fun dailySeed(epochDay: Long): Long = mix(epochDay, 0x43, 0x49)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }

    private val rng = Random(mix(seed, 0x43))

    var ciciX = WIDTH / 2f
        private set
    var ciciY = HEIGHT * 0.55f
        private set
    var facing = 1
        private set
    private var targetX = ciciX
    private var targetY = ciciY

    var lives = LIVES
        private set
    var score = 0
        private set
    var streak = 0
        private set
    var bestStreak = 0
        private set
    var invuln = 0f
        private set
    var status = CiciStatus.RUNNING
        private set
    var frames = 0
        private set

    /** Hareketsizlikten giden toplam puan. */
    var lostPoints = 0
        private set

    val treats = ArrayList<Treat>()
    val cats = ArrayList<Cat>()
    val balls = ArrayList<Ball>()

    private val caughtByKind = IntArray(TreatKind.entries.size)
    private var treatTimer = 0.6f
    private var catTimer = CAT_DELAY
    private var lastMoveFrame = 0
    private var boredAnnounced = false
    private var joyTimer = 99f
    private var happyTimer = 0f
    private var nextId = 1
    private var spawning = true

    val seconds: Float get() = frames * STEP
    val caught: Int get() = caughtByKind.sum()
    fun caughtOf(kind: TreatKind): Int = caughtByKind[kind.ordinal]

    /** Son parmak girişinden bu yana geçen kare. */
    val idleFrames: Int get() = frames - lastMoveFrame
    val idleSeconds: Float get() = idleFrames * STEP

    val mood: Mood
        get() = when {
            happyTimer > 0f -> Mood.HAPPY
            idleFrames >= IDLE_WARN_FRAMES -> Mood.BORED
            else -> Mood.CALM
        }

    fun hud(): CiciHud = CiciHud(
        score = score,
        lives = lives,
        mood = mood,
        streak = streak,
        bestStreak = bestStreak,
        seconds = seconds.toInt(),
        caught = caught,
        idleSeconds = idleSeconds,
        invulnerable = invuln > 0f,
        status = status,
        facing = facing,
    )

    /** Parmak sürüklemesi: hedef [dx], [dy] birim kayar; sıfır olmayan giriş hareket sayılır. */
    fun steerBy(dx: Float, dy: Float) {
        if (abs(dx) < 1e-5f && abs(dy) < 1e-5f) return
        targetX = (targetX + dx).coerceIn(CICI_R, WIDTH - CICI_R)
        targetY = (targetY + dy).coerceIn(CICI_R, HEIGHT - CICI_R)
        if (abs(dx) > 1e-4f) facing = if (dx > 0f) 1 else -1
        lastMoveFrame = frames
    }

    /** Hedefi doğrudan verir (pilot / test); hedef değişmiyorsa hareket sayılmaz. */
    fun steerTo(x: Float, y: Float) {
        val nx = x.coerceIn(CICI_R, WIDTH - CICI_R)
        val ny = y.coerceIn(CICI_R, HEIGHT - CICI_R)
        if (abs(nx - targetX) < 1e-5f && abs(ny - targetY) < 1e-5f) return
        if (abs(nx - targetX) > 1e-4f) facing = if (nx > targetX) 1 else -1
        targetX = nx
        targetY = ny
        lastMoveFrame = frames
    }

    fun step(): List<CiciEvent> {
        val out = ArrayList<CiciEvent>(2)
        if (status == CiciStatus.OVER) return out
        frames++
        moveCici()
        if (invuln > 0f) invuln = max(0f, invuln - STEP)
        if (happyTimer > 0f) happyTimer = max(0f, happyTimer - STEP)
        joyTimer = min(99f, joyTimer + STEP)
        idle(out)
        if (spawning) spawn()
        moveThings()
        collide(out)
        prune()
        return out
    }

    private fun moveCici() {
        val dx = targetX - ciciX
        val dy = targetY - ciciY
        val dist = hypot(dx, dy)
        val maxStep = CICI_SPEED * STEP
        if (dist <= maxStep) {
            ciciX = targetX
            ciciY = targetY
        } else {
            ciciX += dx / dist * maxStep
            ciciY += dy / dist * maxStep
        }
    }

    /** Hareketsizlik: [IDLE_WARN_FRAMES] karede sıkılma duyurusu, [IDLE_PENALTY_FRAMES] kareden sonra saniyede bir puan. */
    private fun idle(out: MutableList<CiciEvent>) {
        val idle = idleFrames
        if (idle < IDLE_WARN_FRAMES) {
            boredAnnounced = false
            return
        }
        if (!boredAnnounced) {
            boredAnnounced = true
            out += CiciEvent.Bored
        }
        if (idle >= IDLE_PENALTY_FRAMES && (idle - IDLE_PENALTY_FRAMES) % 60 == 0 && score > 0) {
            score--
            lostPoints++
            out += CiciEvent.PointLost(score)
        }
    }

    private fun spawn() {
        val s = seconds
        treatTimer -= STEP
        if (treatTimer <= 0f) {
            spawnTreat(s)
            treatTimer = treatInterval(s) * (0.8f + rng.nextFloat() * 0.4f)
        }
        catTimer -= STEP
        if (catTimer <= 0f) {
            spawnCat(s)
            catTimer = catInterval(s) * (0.8f + rng.nextFloat() * 0.4f)
        }
        val wanted = when {
            s >= SECOND_BALL_AT -> MAX_BALLS
            s >= BALL_DELAY -> 1
            else -> 0
        }
        while (balls.size < wanted) spawnBall(s)
    }

    private fun spawnTreat(s: Float) {
        val roll = rng.nextFloat()
        val kind = when {
            roll < 0.55f -> TreatKind.SEED
            roll < 0.85f -> TreatKind.WATER
            else -> TreatKind.HONEY
        }
        val edge = rng.nextInt(4)
        val along = 0.1f + rng.nextFloat() * 0.8f
        val speed = (TREAT_SPEED0 + rng.nextFloat() * 0.08f) * treatSpeedMul(s)
        val jitter = (rng.nextFloat() - 0.5f) * 1.2f
        val x: Float
        val y: Float
        val heading: Float
        when (edge) {
            0 -> { x = along * WIDTH; y = -0.06f; heading = HALF_PI }
            1 -> { x = along * WIDTH; y = HEIGHT + 0.06f; heading = -HALF_PI }
            2 -> { x = -0.06f; y = along * HEIGHT; heading = 0f }
            else -> { x = WIDTH + 0.06f; y = along * HEIGHT; heading = PI_F }
        }
        val a = heading + jitter
        treats += Treat(nextId++, kind, x, y, cos(a) * speed, sin(a) * speed, 0.03f + rng.nextFloat() * 0.05f, rng.nextFloat() * 6.28f)
    }

    private fun spawnCat(s: Float) {
        val fromLeft = rng.nextBoolean()
        val y = 0.15f + rng.nextFloat() * (HEIGHT - 0.3f)
        val speed = (CAT_SPEED0 + rng.nextFloat() * 0.1f) * catSpeedMul(s)
        val vy = (rng.nextFloat() - 0.5f) * 0.12f
        cats += Cat(nextId++, if (fromLeft) -0.1f else WIDTH + 0.1f, y, if (fromLeft) speed else -speed, vy, catHoming(s), rng.nextInt(3))
    }

    /** Top Cici'den en uzak köşede doğar, çapraz yola çıkar. */
    private fun spawnBall(s: Float) {
        var bestX = 0.12f
        var bestY = 0.2f
        var bestD = -1f
        for (cx in floatArrayOf(0.12f, WIDTH - 0.12f)) {
            for (cy in floatArrayOf(0.2f, HEIGHT - 0.2f)) {
                val d = hypot(cx - ciciX, cy - ciciY)
                if (d > bestD) {
                    bestD = d
                    bestX = cx
                    bestY = cy
                }
            }
        }
        val a = 0.6f + rng.nextFloat() * 0.4f
        val sp = ballSpeed(s)
        val sx = if (bestX < WIDTH / 2f) 1f else -1f
        val sy = if (bestY < HEIGHT / 2f) 1f else -1f
        balls += Ball(bestX, bestY, sx * cos(a) * sp, sy * sin(a) * sp)
    }

    private fun moveThings() {
        for (t in treats) {
            t.t += STEP
            val spd = hypot(t.vx, t.vy)
            val w = t.wobble * sin(t.t * 4f + t.phase)
            val px = if (spd > 0f) -t.vy / spd else 0f
            val py = if (spd > 0f) t.vx / spd else 0f
            t.x += (t.vx + px * w) * STEP
            t.y += (t.vy + py * w) * STEP
        }
        for (c in cats) {
            c.t += STEP
            if (c.homing > 0f) {
                val want = ((ciciY - c.y) * 2f).coerceIn(-0.25f, 0.25f)
                c.vy += (want - c.vy) * min(1f, c.homing * 6f * STEP)
            }
            c.x += c.vx * STEP
            c.y += c.vy * STEP
            if (c.y < CAT_R) {
                c.y = CAT_R
                if (c.vy < 0f) c.vy = -c.vy
            }
            if (c.y > HEIGHT - CAT_R) {
                c.y = HEIGHT - CAT_R
                if (c.vy > 0f) c.vy = -c.vy
            }
        }
        val sp = ballSpeed(seconds)
        for (b in balls) {
            b.t += STEP
            val mag = hypot(b.vx, b.vy)
            if (mag > 0f) {
                b.vx *= sp / mag
                b.vy *= sp / mag
            }
            b.x += b.vx * STEP
            b.y += b.vy * STEP
            if (b.x < BALL_R) {
                b.x = BALL_R
                b.vx = abs(b.vx)
            }
            if (b.x > WIDTH - BALL_R) {
                b.x = WIDTH - BALL_R
                b.vx = -abs(b.vx)
            }
            if (b.y < BALL_R) {
                b.y = BALL_R
                b.vy = abs(b.vy)
            }
            if (b.y > HEIGHT - BALL_R) {
                b.y = HEIGHT - BALL_R
                b.vy = -abs(b.vy)
            }
        }
    }

    private fun collide(out: MutableList<CiciEvent>) {
        for (t in treats) {
            if (!t.alive) continue
            if (hypot(t.x - ciciX, t.y - ciciY) <= CICI_R + t.kind.radius) {
                t.alive = false
                streak = if (joyTimer <= JOY_WINDOW) streak + 1 else 1
                joyTimer = 0f
                happyTimer = HAPPY_TIME
                bestStreak = max(bestStreak, streak)
                score += t.kind.points
                caughtByKind[t.kind.ordinal]++
                out += CiciEvent.Caught(t.kind, t.kind.points, streak, t.x, t.y)
            }
        }
        if (invuln > 0f) return
        for (c in cats) {
            if (c.alive && hypot(c.x - ciciX, c.y - ciciY) <= (CICI_R + CAT_R) * HIT_TOLERANCE) {
                hit(HazardKind.CAT, out)
                return
            }
        }
        for (b in balls) {
            if (hypot(b.x - ciciX, b.y - ciciY) <= (CICI_R + BALL_R) * HIT_TOLERANCE) {
                hit(HazardKind.BALL, out)
                return
            }
        }
    }

    private fun hit(by: HazardKind, out: MutableList<CiciEvent>) {
        lives--
        streak = 0
        joyTimer = 99f
        happyTimer = 0f
        out += CiciEvent.Hit(by, lives)
        if (lives <= 0) {
            status = CiciStatus.OVER
            out += CiciEvent.Over
            return
        }
        invuln = INVULN
    }

    private fun prune() {
        treats.removeAll { !it.alive || it.x < -0.15f || it.x > WIDTH + 0.15f || it.y < -0.15f || it.y > HEIGHT + 0.15f }
        cats.removeAll { !it.alive || it.x < -0.2f || it.x > WIDTH + 0.2f }
    }

    // ---- Test kancaları -------------------------------------------------

    /** Test: doğumu durdurur ve sahneyi boşaltır. */
    fun freezeSpawnForTest() {
        spawning = false
        treats.clear()
        cats.clear()
        balls.clear()
    }

    fun setCiciForTest(x: Float, y: Float) {
        ciciX = x.coerceIn(CICI_R, WIDTH - CICI_R)
        ciciY = y.coerceIn(CICI_R, HEIGHT - CICI_R)
        targetX = ciciX
        targetY = ciciY
        lastMoveFrame = frames
    }

    fun setScoreForTest(value: Int) {
        score = value
    }

    fun setLivesForTest(value: Int) {
        lives = value
    }

    /** Geçen süreyi ileri alır (zorluk rampası için); hareket sayacı da sıfırlanır. */
    fun setSecondsForTest(value: Float) {
        frames = (value * 60f).toInt()
        lastMoveFrame = frames
    }

    fun addTreatForTest(kind: TreatKind, x: Float, y: Float, vx: Float = 0f, vy: Float = 0f): Treat =
        Treat(nextId++, kind, x, y, vx, vy, 0f, 0f).also { treats += it }

    fun addCatForTest(x: Float, y: Float, vx: Float = 0f, vy: Float = 0f, homing: Float = 0f): Cat =
        Cat(nextId++, x, y, vx, vy, homing, 0).also { cats += it }

    fun addBallForTest(x: Float, y: Float, vx: Float, vy: Float): Ball =
        Ball(x, y, vx, vy).also { balls += it }
}

private const val PI_F = 3.1415927f
private const val HALF_PI = PI_F / 2f
