package com.za.games.ucurtma

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Uçurtma simülasyonu: dünya sağdan sola akar, uçurtma ekranın solunda
 * ([KITE_X]) sabit x'te uçar. Basılı tutmak ipi çeker ve uçurtma yükselir
 * ([LIFT]), bırakınca alçalır ([GRAVITY]); dikey hız [VMAX] ile sınırlıdır.
 * Zemin ([GROUND]) güvenlidir, gök tavanı ([CEIL]) sert sınırdır.
 *
 * Engeller tohumdan "öbek öbek" üretilir: çatılar (bacalı), elektrik telleri
 * (tek, çift ya da altında çatı), rakip uçurtmalar ve kurdele yayları.
 * Öbekler arası boşluk ve çatı yüksekliği zorlukla ([difficulty], 1500 m'de
 * tavan) değişir; akış hızı metreyle artar ([BASE_SPEED] → [MAX_SPEED]).
 *
 * Uçurtma kavgası kuralı: rakibin ipi rakip uçurtmadan sol-alta iner, bizim
 * ip bizden sol-alta. Rakibin üstünden geçince rakip bizim ipi keser
 * (bonus); altından geçince rakibin ipi bize dolanır ve ipimiz kesilir.
 * Gövdeler çarpışırsa üstteki kazanır. Cam tozlu ip ([Gadget.GLASS]) rakip
 * ipine karşı bağışıktır ve kesme bonusunu ikiye katlar; kuyruk alçalmayı
 * yavaşlatır, makara yükselişi güçlendirir.
 *
 * Skor = metre + kurdele × [RIBBON_POINTS] + kesme × [CUT_POINTS] (cam tozuyla
 * [GLASS_CUT_POINTS]). Görevler ([Mission]) koşu içi sayaçlardan izlenir.
 * Sabit 1/60 s adım; aynı tohum + aynı giriş dizisi = aynı koşu.
 */
class UcurtmaWorld(val seed: Long, val gadget: Gadget? = null, val missions: List<Mission> = emptyList()) {

    companion object {
        const val STEP = 1f / 60f
        const val WIDTH = 1f
        const val HEIGHT = 1.6f
        const val KITE_X = 0.28f
        const val KITE_R = 0.04f
        const val CEIL = 0.06f
        const val GROUND = 1.45f
        const val LIFT = 2.2f
        const val GRAVITY = 1.7f
        const val VMAX = 1.15f
        const val ANCHOR_X = 0.03f
        const val ANCHOR_Y = HEIGHT + 0.06f
        const val RIVAL_R = 0.04f
        const val RIVAL_STRING_DX = 0.3f
        const val RIBBON_R = 0.022f
        const val CHIMNEY_W = 0.035f
        const val CHIMNEY_H = 0.08f
        const val BASE_SPEED = 0.8f
        const val MAX_SPEED = 1.3f
        const val SPEED_PER_METER = 1f / 1200f
        const val METERS_PER_UNIT = 8f
        const val DIFF_METERS = 1500f
        const val SPAWN_AHEAD = 1.4f
        const val FIRST_CHUNK_X = 1.2f
        const val NEAR_MISS_GAP = 0.03f
        const val RIBBON_POINTS = 5
        const val CUT_POINTS = 25
        const val GLASS_CUT_POINTS = 50
        const val MILESTONE = 100
        const val STRING_HIT = 0.85f
        const val TAIL_GRAVITY = 0.75f
        const val REEL_LIFT = 1.25f
        const val WIRE_PAIR_GAP = 0.34f

        /** Geçilebilirlik: her sütunda en az bu kadar boş dikey aralık kalır. */
        const val MIN_GAP_Y = 0.3f

        fun dailySeed(epochDay: Long): Long = mix(epochDay, 0x55, 0x43)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }

        /** Noktanın doğru parçasına uzaklığı. */
        fun segDist(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0f, 1f)
            return hypot(px - (ax + t * dx), py - (ay + t * dy))
        }

        fun circleRect(cx: Float, cy: Float, r: Float, x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
            val nx = cx.coerceIn(x1, x2)
            val ny = cy.coerceIn(y1, y2)
            return hypot(cx - nx, cy - ny) <= r
        }
    }

    private enum class Chunk { ROOFS, WIRE, RIVAL, RIBBONS }

    private val rng = Random(mix(seed, 0x55))

    var kiteY = 0.7f
        private set
    var kiteVy = 0f
        private set
    var holding = false
        private set

    /** Akan mesafe (birim). */
    var distance = 0f
        private set
    val meters: Int get() = (distance * METERS_PER_UNIT).toInt()
    var ribbons = 0
        private set
    var cuts = 0
        private set
    var underWires = 0
        private set
    var nearMisses = 0
        private set
    var frames = 0
        private set
    var status = UcurtmaStatus.RUNNING
        private set
    var crash: CrashKind? = null
        private set

    val cutPoints: Int get() = if (gadget == Gadget.GLASS) GLASS_CUT_POINTS else CUT_POINTS
    val score: Int get() = meters + ribbons * RIBBON_POINTS + cuts * cutPoints
    val speed: Float get() = min(MAX_SPEED, BASE_SPEED + meters * SPEED_PER_METER)
    val difficulty: Float get() = min(1f, meters / DIFF_METERS)

    val buildings = ArrayList<Building>()
    val wires = ArrayList<Wire>()
    val rivals = ArrayList<Rival>()
    val ribbonItems = ArrayList<Ribbon>()

    /** Üretilen son öbeğin bittiği dünya x'i. */
    var nextX = FIRST_CHUNK_X
        private set
    private var chunkCount = 0
    private var lastChunk: Chunk? = null
    private var nextRivalId = 1
    private var lastMilestone = 0
    private val missionDone = BooleanArray(missions.size)
    private var spawning = true

    fun hold(pressed: Boolean) {
        holding = pressed
    }

    /** Dünya x'ini ekran x'ine çevirir. */
    fun screenX(worldX: Float): Float = worldX - distance

    fun progress(m: Mission): Int = when (m.kind) {
        MissionKind.DISTANCE -> meters
        MissionKind.RIBBONS -> ribbons
        MissionKind.UNDER_WIRE -> underWires
        MissionKind.NEAR_MISS -> nearMisses
        MissionKind.CUTS -> cuts
    }

    fun hud(): UcurtmaHud = UcurtmaHud(
        meters = meters,
        ribbons = ribbons,
        cuts = cuts,
        underWires = underWires,
        nearMisses = nearMisses,
        score = score,
        speed = speed,
        status = status,
        crash = crash,
        progress = missions.map { min(progress(it), it.target) },
        done = missions.mapIndexed { i, m -> missionDone[i] || progress(m) >= m.target },
    )

    fun step(): List<UcurtmaEvent> {
        val out = ArrayList<UcurtmaEvent>(2)
        if (status != UcurtmaStatus.OVER) {
            frames++
            distance += speed * STEP
            val lift = LIFT * (if (gadget == Gadget.REEL) REEL_LIFT else 1f)
            val gravity = GRAVITY * (if (gadget == Gadget.TAIL) TAIL_GRAVITY else 1f)
            kiteVy = (kiteVy + (if (holding) -lift else gravity) * STEP).coerceIn(-VMAX, VMAX)
            kiteY += kiteVy * STEP
            if (kiteY < CEIL + KITE_R) {
                kiteY = CEIL + KITE_R
                kiteVy = 0f
            }
            if (kiteY > GROUND - KITE_R) {
                kiteY = GROUND - KITE_R
                kiteVy = 0f
            }
            if (spawning) while (nextX - distance < WIDTH + SPAWN_AHEAD) spawnChunk()
            prune()
            for (r in rivals) {
                r.t += STEP
                if (r.alive) {
                    r.x -= r.speed * STEP
                    r.y = r.baseY + r.amp * sin(r.t * 2.2f + r.phase)
                } else {
                    r.fall += 2.5f * STEP
                    r.y += r.fall * STEP
                }
            }
            collide(out)
            if (status == UcurtmaStatus.RUNNING) {
                val m = meters
                if (m / MILESTONE > lastMilestone) {
                    lastMilestone = m / MILESTONE
                    out += UcurtmaEvent.Milestone(lastMilestone * MILESTONE)
                }
                checkMissions(out)
            }
        }
        return out
    }

    private fun collide(out: MutableList<UcurtmaEvent>) {
        val kx = KITE_X
        val ky = kiteY
        for (b in buildings) {
            val sx = screenX(b.x)
            val sx2 = sx + b.width
            if (sx2 < kx - KITE_R) {
                if (!b.graded) {
                    b.graded = true
                    if (b.minClear in 0f..NEAR_MISS_GAP) {
                        nearMisses++
                        out += UcurtmaEvent.NearMiss(sx2, b.top)
                    }
                }
                continue
            }
            if (sx > kx + KITE_R) continue
            if (circleRect(kx, ky, KITE_R, sx, b.top, sx2, GROUND + 0.2f)) {
                crash(CrashKind.ROOF, out)
                return
            }
            var top = b.top
            if (b.chimneyX >= 0f) {
                val cx = screenX(b.chimneyX)
                if (circleRect(kx, ky, KITE_R, cx, b.chimneyTop, cx + CHIMNEY_W, b.top)) {
                    crash(CrashKind.ROOF, out)
                    return
                }
                if (kx + KITE_R >= cx && kx - KITE_R <= cx + CHIMNEY_W) top = b.chimneyTop
            }
            b.minClear = min(b.minClear, top - (ky + KITE_R))
        }
        for (w in wires) {
            val sx1 = screenX(w.x1)
            val sx2 = screenX(w.x2)
            if (sx2 < kx - KITE_R) {
                if (!w.graded) {
                    w.graded = true
                    if (w.below == true) {
                        underWires++
                        out += UcurtmaEvent.UnderWire(underWires)
                    }
                }
                continue
            }
            if (sx1 > kx + KITE_R) continue
            if (abs(ky - w.y) <= KITE_R * 0.9f) {
                crash(CrashKind.WIRE, out)
                return
            }
            if (w.below == null) w.below = ky > w.y
        }
        for (r in rivals) {
            if (!r.alive) continue
            val rx = screenX(r.x)
            val ry = r.y
            if (rx < -0.2f || rx > WIDTH + 0.4f) continue
            val theirString = segDist(kx, ky, rx, ry, rx - RIVAL_STRING_DX, ANCHOR_Y)
            val theirCut = gadget != Gadget.GLASS && theirString <= KITE_R * STRING_HIT
            val ourString = segDist(rx, ry, kx, ky, ANCHOR_X, ANCHOR_Y)
            val ourCut = ourString <= RIVAL_R
            val bodies = hypot(rx - kx, ry - ky) <= KITE_R + RIVAL_R
            if ((ourCut || bodies) && (!theirCut || ky < ry)) {
                r.alive = false
                cuts++
                out += UcurtmaEvent.Cut(rx, ry, cutPoints)
            } else if (theirCut || bodies) {
                crash(CrashKind.STRING, out)
                return
            }
        }
        for (rb in ribbonItems) {
            if (rb.taken) continue
            val sx = screenX(rb.x)
            if (abs(sx - kx) > KITE_R + RIBBON_R) continue
            if (hypot(sx - kx, rb.y - ky) <= KITE_R + RIBBON_R) {
                rb.taken = true
                ribbons++
                out += UcurtmaEvent.RibbonTaken(sx, rb.y, ribbons)
            }
        }
    }

    private fun crash(kind: CrashKind, out: MutableList<UcurtmaEvent>) {
        crash = kind
        status = UcurtmaStatus.OVER
        out += UcurtmaEvent.Crash(kind)
        out += UcurtmaEvent.Over
    }

    private fun checkMissions(out: MutableList<UcurtmaEvent>) {
        for ((i, m) in missions.withIndex()) {
            if (!missionDone[i] && progress(m) >= m.target) {
                missionDone[i] = true
                out += UcurtmaEvent.MissionDone(m)
            }
        }
    }

    private fun prune() {
        buildings.removeAll { screenX(it.right) < -0.3f }
        wires.removeAll { screenX(it.x2) < -0.3f }
        rivals.removeAll { screenX(it.x) < -0.4f || it.y > HEIGHT + 0.2f }
        ribbonItems.removeAll { it.taken || screenX(it.x) < -0.1f }
    }

    // ---- Üretim ----------------------------------------------------------

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun spawnChunk() {
        val d = difficulty
        val kind = when {
            chunkCount == 0 -> Chunk.RIBBONS
            chunkCount == 1 -> Chunk.ROOFS
            else -> {
                var k = pickChunk(d)
                if (k == lastChunk && k != Chunk.RIBBONS) k = pickChunk(d)
                // Rakip sola süzülerek önceki öbeğe girer; önceki öbek telse yüksek
                // tel + rakip çakışması kaçışsız olurdu.
                if (k == Chunk.RIVAL && lastChunk == Chunk.WIRE) k = Chunk.RIBBONS
                k
            }
        }
        val gap = lerp(0.5f, 0.24f, d) + rng.nextFloat() * 0.2f
        var x = nextX + gap
        when (kind) {
            Chunk.ROOFS -> {
                val n = 2 + rng.nextInt(3)
                val withRibbons = rng.nextFloat() < 0.5f
                repeat(n) {
                    val w = 0.12f + rng.nextFloat() * 0.1f
                    val h = 0.12f + rng.nextFloat() * (0.3f + 0.25f * d)
                    val top = GROUND - h
                    val chimney = rng.nextFloat() < 0.5f
                    val cx = if (chimney) x + w * (0.3f + rng.nextFloat() * 0.4f) else -1f
                    buildings += Building(x, w, top, cx, top - CHIMNEY_H)
                    if (withRibbons) ribbonItems += Ribbon(x + w / 2f, top - CHIMNEY_H - 0.1f - rng.nextFloat() * 0.06f)
                    x += w + 0.02f + rng.nextFloat() * 0.04f
                }
            }
            Chunk.WIRE -> {
                val span = 0.45f + rng.nextFloat() * 0.35f
                // Yüksek tel: üstünden geçilemez, altından geçmek gerekir; tavanda
                // uçmayı tek başına güvenli olmaktan çıkarır.
                val high = rng.nextFloat() < 0.25f + 0.2f * d
                val y = if (high) 0.14f + rng.nextFloat() * 0.16f else 0.45f + rng.nextFloat() * 0.55f
                wires += Wire(x, x + span, y)
                val extra = rng.nextFloat()
                if (extra < 0.3f + 0.4f * d) {
                    if (!high && y <= 0.75f && extra < 0.2f + 0.2f * d) {
                        // Altında çatı: telin üstünden geçmek gerekir.
                        var bx = x + 0.05f
                        repeat(2) {
                            val w = 0.14f + rng.nextFloat() * 0.08f
                            val h = 0.12f + rng.nextFloat() * 0.18f
                            buildings += Building(bx, w, GROUND - h, -1f, GROUND - h - CHIMNEY_H)
                            bx += w + 0.04f
                        }
                    } else {
                        val y2 = if (y + WIRE_PAIR_GAP <= 1.2f) y + WIRE_PAIR_GAP else y - WIRE_PAIR_GAP
                        wires += Wire(x + 0.05f, x + span - 0.05f, y2)
                    }
                }
                x += span
            }
            Chunk.RIVAL -> {
                rivals += Rival(
                    id = nextRivalId++,
                    x = x + 0.15f,
                    baseY = 0.45f + rng.nextFloat() * 0.5f,
                    amp = 0.04f + rng.nextFloat() * 0.06f,
                    phase = rng.nextFloat() * 6.28f,
                    speed = 0.06f + rng.nextFloat() * 0.12f,
                )
                x += 0.35f
            }
            Chunk.RIBBONS -> {
                val n = 6
                val y0 = 0.35f + rng.nextFloat() * 0.75f
                val phase = rng.nextFloat() * 6.28f
                for (i in 0 until n) ribbonItems += Ribbon(x + i * 0.08f, (y0 + 0.12f * sin(i * 0.8f + phase)).coerceIn(0.2f, 1.2f))
                x += n * 0.08f
            }
        }
        lastChunk = kind
        chunkCount++
        nextX = x
    }

    private fun pickChunk(d: Float): Chunk {
        val roll = rng.nextFloat()
        return when {
            roll < 0.34f - 0.08f * d -> Chunk.ROOFS
            roll < 0.62f - 0.04f * d -> Chunk.WIRE
            roll < 0.82f -> Chunk.RIVAL
            else -> Chunk.RIBBONS
        }
    }

    // ---- Test kancaları -------------------------------------------------

    /** Test: engel üretimini durdurur (boş gök). */
    fun freezeSpawnForTest() {
        spawning = false
        buildings.clear()
        wires.clear()
        rivals.clear()
        ribbonItems.clear()
    }

    /** Test: dünya x'i [untilWorldX]'e kadar öbek üretir. */
    fun generateForTest(untilWorldX: Float) {
        while (nextX < untilWorldX) spawnChunk()
    }

    /** Test: akan mesafeyi ileri alır (zorluk ve hız o mesafeye göre); üretim oradan sürer. */
    fun setDistanceForTest(units: Float) {
        distance = units
        if (nextX < units + FIRST_CHUNK_X) nextX = units + FIRST_CHUNK_X
    }

    fun setKiteForTest(y: Float, vy: Float = 0f) {
        kiteY = y
        kiteVy = vy
    }

    fun addBuildingForTest(x: Float, width: Float, height: Float, chimney: Boolean = false): Building {
        val top = GROUND - height
        val b = Building(x, width, top, if (chimney) x + width * 0.4f else -1f, top - CHIMNEY_H)
        buildings += b
        return b
    }

    fun addWireForTest(x1: Float, x2: Float, y: Float): Wire = Wire(x1, x2, y).also { wires += it }

    fun addRivalForTest(x: Float, y: Float, speed: Float = 0.2f): Rival =
        Rival(nextRivalId++, x, y, 0f, 0f, speed).also { rivals += it }

    fun addRibbonForTest(x: Float, y: Float): Ribbon = Ribbon(x, y).also { ribbonItems += it }
}
