package com.za.games.raket

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Raket simülasyonu: dikey kort [WIDTH]×[HEIGHT] birim, altta ve üstte yatay
 * raketler, ortada top. Sabit 1/60 s adım; aynı tohum + aynı giriş dizisi =
 * aynı koşu.
 *
 * Top rakete çarpınca çıkış açısı vuruş noktasından gelir: merkez düz, kenar
 * [MAX_ANGLE]'a kadar çapraz. Raket vuruş anında hareket ediyorsa falso
 * ([SPIN_GAIN]) açıyı hareket yönüne kaydırır. Her raket vuruşu topu
 * [SPEED_STEP] kat hızlandırır, [MAX_SPEED]'de tavan; servisle hız tabana
 * döner. Kaçan top rakibe sayı yazar; [WIN_POINTS] sayıya en az
 * [WIN_MARGIN] farkla ulaşan kazanır. Servisler sırayla iki tarafa gider.
 *
 * Duvar modunda üst raket yoktur: top arka duvardan küçük bir açı sapmasıyla
 * ([WALL_JITTER]) döner, skor vuruş sayısıdır, her [WALL_SHRINK_EVERY]
 * vuruşta raket daralır, top kaçınca koşu biter.
 *
 * Çarpışma süpürmelidir: top bir adımda raket kalınlığından fazla yol alsa
 * da (tavan hızda 0,038 birim, raket 0,028) raket düzlemini geçtiği an
 * yakalanır; tünelleme olmaz.
 */
class RaketWorld(val seed: Long, val mode: RaketMode, val level: Int = 1) {

    companion object {
        const val STEP = 1f / 60f
        const val WIDTH = 1f
        const val HEIGHT = 1.6f
        const val BALL_R = 0.022f
        const val PADDLE_W = 0.22f
        const val PADDLE_H = 0.028f

        /** Raket merkezinin kendi kenarına uzaklığı. */
        const val PADDLE_INSET = 0.075f
        const val BASE_SPEED = 0.85f
        const val MAX_SPEED = 2.3f
        const val SPEED_STEP = 1.06f

        /** Dikeyden en çok sapma (radyan) ≈ 62°. */
        const val MAX_ANGLE = 1.08f

        /** Raket hızı (birim/s) başına falso (radyan). */
        const val SPIN_GAIN = 0.3f

        /** Falsonun tavanı (radyan) ≈ 20°. */
        const val SPIN_MAX = 0.35f

        /** Topun raket merkezinden en çok uzaklığı: yarı genişlik + yarıçapın bu katı. */
        const val HIT_SLACK = 0.6f
        const val WIN_POINTS = 11
        const val WIN_MARGIN = 2
        const val SERVE_DELAY = 0.9f

        /** Servis açısının sınırı (radyan) ≈ 28°. */
        const val SERVE_ANGLE = 0.5f
        const val OUT_MARGIN = 0.06f

        /** Arka duvar dönüşünde açı sapması (radyan) ≈ 6°. */
        const val WALL_JITTER = 0.1f
        const val WALL_SHRINK_EVERY = 8
        const val WALL_SHRINK = 0.012f
        const val WALL_MIN_W = 0.13f
        const val FLASH_TIME = 0.12f
        const val VEL_SMOOTH = 0.55f
        const val MAX_LEVEL = 2

        fun dailySeed(epochDay: Long): Long = mix(epochDay, 0x52, 0x4B)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }

    private val rng = Random(mix(seed, 0x52))

    val ball = Ball()
    val bottom = Paddle(Side.BOTTOM, HEIGHT - PADDLE_INSET)
    val top = Paddle(Side.TOP, PADDLE_INSET)
    private val paddles = listOf(bottom, top)

    /** Bilgisayar raketi; yalnızca SOLO'da. */
    val ai: RaketAi? = if (mode == RaketMode.SOLO) RaketAi.forLevel(level, Side.TOP, Random(mix(seed, 0x41, level))) else null

    var status = RaketStatus.SERVING
        private set
    var scoreBottom = 0
        private set
    var scoreTop = 0
        private set
    var rally = 0
        private set
    var bestRally = 0
        private set

    /** Koşudaki toplam raket vuruşu. */
    var hits = 0
        private set
    var speed = BASE_SPEED
        private set
    var frames = 0
        private set
    var winner: Side? = null
        private set

    /** Sıradaki servisin gideceği taraf. */
    var serveTo: Side = if (mode == RaketMode.DUO && rng.nextBoolean()) Side.BOTTOM else Side.TOP
        private set
    private var serveTimer = SERVE_DELAY

    val hasTopPaddle: Boolean get() = mode != RaketMode.WALL

    fun paddle(side: Side): Paddle = if (side == Side.BOTTOM) bottom else top

    /** Oyuncu girişi: raketi [dx] birim kaydırır (kort içinde kalır). */
    fun move(side: Side, dx: Float) {
        if (status == RaketStatus.OVER) return
        if (side == Side.TOP && mode != RaketMode.DUO) return
        shift(paddle(side), dx)
    }

    internal fun shift(p: Paddle, dx: Float) {
        p.x = (p.x + dx).coerceIn(p.width / 2f, WIDTH - p.width / 2f)
    }

    /** Topun raketle buluşacağı y (top merkezi). */
    fun contactY(side: Side): Float =
        if (side == Side.BOTTOM) bottom.y - PADDLE_H / 2f - BALL_R else top.y + PADDLE_H / 2f + BALL_R

    /** Topun, yan duvarlardan sekerek [targetY]'ye vardığında olacağı x. */
    fun predictX(targetY: Float): Float {
        val b = ball
        if (b.vy == 0f) return b.x
        val t = (targetY - b.y) / b.vy
        if (t <= 0f) return b.x
        val lo = BALL_R
        val hi = WIDTH - BALL_R
        val span = hi - lo
        var rel = (b.x + b.vx * t - lo) % (2f * span)
        if (rel < 0f) rel += 2f * span
        return if (rel <= span) lo + rel else hi - (rel - span)
    }

    fun hud(): RaketHud = RaketHud(
        bottom = scoreBottom,
        top = scoreTop,
        rally = rally,
        bestRally = bestRally,
        speed = ((speed - BASE_SPEED) / (MAX_SPEED - BASE_SPEED)).coerceIn(0f, 1f),
        serving = if (status == RaketStatus.SERVING) serveTo else null,
        status = status,
        winner = winner,
    )

    fun step(): List<RaketEvent> {
        val out = ArrayList<RaketEvent>(2)
        if (status == RaketStatus.OVER) return out
        frames++
        for (p in paddles) {
            val inst = (p.x - p.prevX) / STEP
            p.vel = p.vel * VEL_SMOOTH + inst * (1f - VEL_SMOOTH)
            p.prevX = p.x
            if (p.flash > 0f) p.flash = max(0f, p.flash - STEP)
        }
        ai?.drive(this)
        if (status == RaketStatus.SERVING) {
            serveTimer -= STEP
            if (serveTimer <= 0f) launch(out)
            return out
        }
        val px = ball.x
        val py = ball.y
        ball.x += ball.vx * STEP
        ball.y += ball.vy * STEP
        if (ball.x < BALL_R) {
            ball.x = BALL_R
            ball.vx = abs(ball.vx)
            out += RaketEvent.SideWall
        } else if (ball.x > WIDTH - BALL_R) {
            ball.x = WIDTH - BALL_R
            ball.vx = -abs(ball.vx)
            out += RaketEvent.SideWall
        }
        if (ball.vy > 0f) {
            hitPaddle(bottom, px, py, out)
            if (ball.y - BALL_R > HEIGHT + OUT_MARGIN) {
                if (mode == RaketMode.WALL) finish(null, out) else point(Side.TOP, out)
            }
        } else if (ball.vy < 0f) {
            if (mode == RaketMode.WALL) {
                if (ball.y < BALL_R) backWall(out)
            } else {
                hitPaddle(top, px, py, out)
                if (ball.y + BALL_R < -OUT_MARGIN) point(Side.BOTTOM, out)
            }
        }
        return out
    }

    private fun hitPaddle(p: Paddle, px: Float, py: Float, out: MutableList<RaketEvent>) {
        val down = p.side == Side.BOTTOM
        val plane = if (down) p.y - PADDLE_H / 2f else p.y + PADDLE_H / 2f
        val prevEdge = if (down) py + BALL_R else py - BALL_R
        val newEdge = if (down) ball.y + BALL_R else ball.y - BALL_R
        val crossed = if (down) prevEdge <= plane && newEdge >= plane else prevEdge >= plane && newEdge <= plane
        if (!crossed) return
        val span = newEdge - prevEdge
        val t = if (span == 0f) 1f else ((plane - prevEdge) / span).coerceIn(0f, 1f)
        val xAt = px + (ball.x - px) * t
        if (abs(xAt - p.x) > p.width / 2f + BALL_R * HIT_SLACK) return
        val u = ((xAt - p.x) / (p.width / 2f)).coerceIn(-1f, 1f)
        val spin = (p.vel * SPIN_GAIN).coerceIn(-SPIN_MAX, SPIN_MAX)
        val angle = (u * MAX_ANGLE + spin).coerceIn(-MAX_ANGLE, MAX_ANGLE)
        speed = min(MAX_SPEED, speed * SPEED_STEP)
        ball.vx = speed * sin(angle)
        ball.vy = (if (down) -1f else 1f) * speed * cos(angle)
        ball.y = if (down) plane - BALL_R else plane + BALL_R
        rally++
        hits++
        if (rally > bestRally) bestRally = rally
        p.flash = FLASH_TIME
        if (mode == RaketMode.WALL && hits % WALL_SHRINK_EVERY == 0) {
            p.width = max(WALL_MIN_W, p.width - WALL_SHRINK)
            shift(p, 0f)
        }
        out += RaketEvent.PaddleHit(p.side, speed, angle)
    }

    private fun backWall(out: MutableList<RaketEvent>) {
        ball.y = BALL_R
        val a = atan2(ball.vx, -ball.vy)
        val jitter = (rng.nextFloat() * 2f - 1f) * WALL_JITTER
        val na = (a + jitter).coerceIn(-MAX_ANGLE, MAX_ANGLE)
        ball.vx = speed * sin(na)
        ball.vy = speed * cos(na)
        out += RaketEvent.BackWall
    }

    private fun point(scorer: Side, out: MutableList<RaketEvent>) {
        if (scorer == Side.BOTTOM) scoreBottom++ else scoreTop++
        rally = 0
        out += RaketEvent.Point(scorer, scoreBottom, scoreTop)
        if (max(scoreBottom, scoreTop) >= WIN_POINTS && abs(scoreBottom - scoreTop) >= WIN_MARGIN) {
            finish(if (scoreBottom > scoreTop) Side.BOTTOM else Side.TOP, out)
            return
        }
        serveTo = if (serveTo == Side.TOP) Side.BOTTOM else Side.TOP
        resetBall()
    }

    private fun resetBall() {
        ball.x = WIDTH / 2f
        ball.y = HEIGHT / 2f
        ball.vx = 0f
        ball.vy = 0f
        speed = BASE_SPEED
        status = RaketStatus.SERVING
        serveTimer = SERVE_DELAY
    }

    private fun finish(w: Side?, out: MutableList<RaketEvent>) {
        winner = w
        status = RaketStatus.OVER
        ball.vx = 0f
        ball.vy = 0f
        out += RaketEvent.Over(w)
    }

    private fun launch(out: MutableList<RaketEvent>) {
        val angle = (rng.nextFloat() * 2f - 1f) * SERVE_ANGLE
        val dir = if (serveTo == Side.TOP) -1f else 1f
        ball.vx = speed * sin(angle)
        ball.vy = dir * speed * cos(angle)
        status = RaketStatus.RALLY
        out += RaketEvent.Serve(serveTo)
    }

    // ---- Test kancaları -------------------------------------------------

    /** Test: topu konumlandırıp ralliyi başlatır. */
    fun placeForTest(x: Float, y: Float, vx: Float, vy: Float, speed: Float = BASE_SPEED) {
        ball.x = x
        ball.y = y
        ball.vx = vx
        ball.vy = vy
        this.speed = speed
        status = RaketStatus.RALLY
    }

    /** Test: raketi durgun olarak [x]'e koyar. */
    fun placePaddleForTest(side: Side, x: Float) {
        val p = paddle(side)
        p.x = x.coerceIn(p.width / 2f, WIDTH - p.width / 2f)
        p.prevX = p.x
        p.vel = 0f
    }

    fun setScoreForTest(bottom: Int, top: Int) {
        scoreBottom = bottom
        scoreTop = top
    }
}
