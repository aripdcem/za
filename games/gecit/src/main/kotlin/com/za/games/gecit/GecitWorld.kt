package com.za.games.gecit

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class GecitStatus { RUNNING, OVER }

enum class Move { FORWARD, BACK, LEFT, RIGHT }

enum class DeathCause { CAR, TRAIN, WATER, CARRIED, CAMERA, EAGLE }

sealed interface GecitEvent {
    data object Hop : GecitEvent
    data object Bump : GecitEvent
    data class Warning(val row: Int) : GecitEvent
    data class Train(val row: Int) : GecitEvent
    data class Milestone(val row: Int) : GecitEvent
    data class Gem(val row: Int, val col: Int, val total: Int) : GecitEvent

    /** Kartal yaklaşıyor: sayaç sınırın yüzde 65'ini geçti (bekleyiş başına bir kez). */
    data object EagleNear : GecitEvent
    data class Over(val cause: DeathCause) : GecitEvent
}

/** Arayüz için değişmez özet. */
data class GecitHud(
    val score: Long,
    val row: Int,
    val gems: Int,
    val seconds: Int,
    val idleFraction: Float,
    val status: GecitStatus,
    val cause: DeathCause?,
)

/** Oyuncu (kurbağa): [x] sol kenar, kütükteyken kesirli olabilir. */
class Player {
    var row = 0
    var x = GecitGen.START_COL.toFloat()
    var fromRow = 0
    var fromX = x
    var hopT = 1f
    var facing = Move.FORWARD

    val centerX: Float get() = x + 0.5f
}

/**
 * Geçit simülasyonu: ızgarada zıplayan oyuncu, sürekli akan araçlar, kütükler
 * ve trenler; 1/60 s sabit adım. Aynı tohum + aynı hamle dizisi = aynı oyun.
 * Satırlar ileriye doğru artar; [camera] görünen alanın alt satırıdır ve
 * ilk hamleden sonra yavaşça ilerler.
 */
class GecitWorld(
    val seed: Long,
    private val laneFactory: ((List<Lane>) -> Lane)? = null,
) {
    companion object {
        const val WIDTH = GecitGen.WIDTH
        const val START_COL = GecitGen.START_COL
        const val VIEW_ROWS = 14
        const val BEHIND = 4
        const val STEP = 1f / 60f
        const val HOP_TIME = 0.12f

        /** İleri hamle yapmadan bu kadar bekleyen oyuncuyu kartal kapar. */
        const val IDLE_LIMIT = 3.5f

        /** Yana/geri hamle sayacı en fazla buraya indirir: nefes aldırır, sonsuz oyalamayı önlemez. */
        const val IDLE_SIDE_RESET = IDLE_LIMIT * 0.5f
        const val EAGLE_NEAR = IDLE_LIMIT * 0.65f
        const val MILESTONE = 25
        private const val GEN_AHEAD = 10

        fun dailySeed(epochDay: Long): Long = GecitGen.mix(epochDay, 0x47, 0x43)
    }

    private val gen = GecitGen(seed)
    private val events = ArrayList<GecitEvent>()
    private var pending: Move? = null

    val lanes = ArrayList<Lane>()
    val player = Player()

    var camera = -BEHIND.toFloat()
        private set
    var status = GecitStatus.RUNNING
        private set
    var cause: DeathCause? = null
        private set
    var maxRow = 0
        private set
    var gems = 0
        private set
    var idle = 0f
        private set
    private var eagleWarned = false
    var started = false
        private set
    var frames = 0
        private set

    init {
        ensureRows(VIEW_ROWS + GEN_AHEAD)
    }

    /** Skor = geçilen şerit + toplanan taş. */
    val score: Long get() = (maxRow + gems).toLong()
    val seconds: Int get() = frames / 60
    val idleFraction: Float get() = (idle / IDLE_LIMIT).coerceIn(0f, 1f)

    fun hud(): GecitHud = GecitHud(score, player.row, gems, seconds, idleFraction, status, cause)

    /** Satır şeridi; negatif satırlar (başlangıcın gerisi) yoktur. */
    fun lane(row: Int): Lane? {
        if (row < 0) return null
        ensureRows(row)
        return lanes[row]
    }

    private fun ensureRows(row: Int) {
        while (lanes.size <= row) lanes.add(laneFactory?.invoke(lanes) ?: gen.next(lanes))
    }

    private fun emit(event: GecitEvent) {
        events.add(event)
    }

    /** Bir kare ilerletir; [move] varsa (zıplama bitince) uygulanır. */
    fun step(move: Move?): List<GecitEvent> {
        events.clear()
        if (status != GecitStatus.RUNNING) return emptyList()
        frames++
        val p = player
        if (move != null) pending = move
        if (p.hopT < 1f) {
            p.hopT = min(1f, p.hopT + STEP / HOP_TIME)
            if (p.hopT >= 1f) collectGem(p)
        }
        if (p.hopT >= 1f) {
            val m = pending
            if (m != null) {
                pending = null
                hop(m)
            }
        }

        // Şeritler: görünen alan ve biraz ötesi akar.
        val bottom = floor(camera).toInt()
        val high = bottom + VIEW_ROWS + 6
        ensureRows(high + GEN_AHEAD)
        for (r in max(0, bottom - 2)..high) {
            val change = lanes[r].update(STEP) ?: continue
            if (r > bottom + VIEW_ROWS) continue
            when (change) {
                RailPhase.WARNING -> emit(GecitEvent.Warning(r))
                RailPhase.TRAIN -> emit(GecitEvent.Train(r))
                RailPhase.IDLE -> Unit
            }
        }

        // Çarpışma ve kütükle taşınma.
        val lane = lanes[p.row]
        when (lane.kind) {
            LaneKind.ROAD -> if (lane.hits(p.x)) die(DeathCause.CAR)
            LaneKind.RAIL -> if (lane.hits(p.x)) die(DeathCause.TRAIN)
            LaneKind.RIVER -> if (p.hopT >= 1f) {
                val log = lane.logUnder(p.centerX)
                if (log == null) {
                    die(DeathCause.WATER)
                } else {
                    p.x += lane.dir * lane.speed * STEP
                    if (p.x < -0.6f || p.x > WIDTH - 0.4f) die(DeathCause.CARRIED)
                }
            }
            LaneKind.GRASS -> Unit
        }
        if (status != GecitStatus.RUNNING) return events.toList()

        // Kamera ve kartal: ilk hamleden sonra işler.
        if (started) {
            camera += creep() * STEP
            idle += STEP
            if (!eagleWarned && idle >= EAGLE_NEAR) {
                eagleWarned = true
                emit(GecitEvent.EagleNear)
            }
        }
        camera = max(camera, p.row - BEHIND.toFloat())
        if (p.row + 1f <= camera) {
            die(DeathCause.CAMERA)
        } else if (idle >= IDLE_LIMIT) {
            die(DeathCause.EAGLE)
        }
        return events.toList()
    }

    /** Kameranın kendi ilerleme hızı (satır/s); ilerledikçe artar. */
    fun creep(): Float = 0.35f + 0.9f * min(1f, maxRow / 200f)

    private fun hop(m: Move) {
        val p = player
        var row2 = p.row
        var x2 = p.x
        when (m) {
            Move.FORWARD -> row2++
            Move.BACK -> row2--
            Move.LEFT -> x2 -= 1f
            Move.RIGHT -> x2 += 1f
        }
        p.facing = m
        if (row2 < 0 || x2 < -0.5f || x2 > WIDTH - 0.5f || row2 + 1f <= camera + 0.5f) {
            emit(GecitEvent.Bump)
            return
        }
        val target = lane(row2)!!
        val col = x2.roundToInt().coerceIn(0, WIDTH - 1)
        if (target.kind == LaneKind.GRASS && target.trees[col]) {
            emit(GecitEvent.Bump)
            return
        }
        // Kütükten inerken sütuna oturur; nehirde kesirli konum korunur.
        if (target.kind != LaneKind.RIVER) x2 = col.toFloat()
        p.fromRow = p.row
        p.fromX = p.x
        p.row = row2
        p.x = x2
        p.hopT = 0f
        started = true
        if (m == Move.FORWARD) {
            idle = 0f
            eagleWarned = false
        } else if (idle > IDLE_SIDE_RESET) {
            idle = IDLE_SIDE_RESET
        }
        if (p.row > maxRow) {
            maxRow = p.row
            if (maxRow % MILESTONE == 0) emit(GecitEvent.Milestone(maxRow))
        }
        emit(GecitEvent.Hop)
    }

    /** Zıplama bitince hücredeki taş alınır. */
    private fun collectGem(p: Player) {
        val lane = lanes[p.row]
        val col = p.x.roundToInt()
        if (lane.gemCol != col) return
        lane.gemCol = -1
        gems++
        emit(GecitEvent.Gem(p.row, col, gems))
    }

    private fun die(cause: DeathCause) {
        if (status != GecitStatus.RUNNING) return
        status = GecitStatus.OVER
        this.cause = cause
        emit(GecitEvent.Over(cause))
    }
}
