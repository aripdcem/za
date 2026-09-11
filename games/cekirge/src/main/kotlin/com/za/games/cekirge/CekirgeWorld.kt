package com.za.games.cekirge

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Çekirge simülasyonu (Space Invaders türü): [WIDTH] × [HEIGHT] tarla, y
 * aşağı doğru artar (0 üst). Sürü [ROWS] × [COLS] formasyonuyla yana yürür,
 * kenara varınca yön değiştirip [STEP_DOWN] iner; seyreldikçe hızlanır
 * (son çekirge dört kat). Alt kenardaki çiftçi yana kayar ([moveBy]) ve
 * ilaç fıskırtır ([fire]): **tek mermi kuralı** — fıskırtma hedefe varmadan
 * (ya da tarlayı terk etmeden) yenisi atılamaz.
 *
 * Sürünün alt çekirgeleri tükürük bırakır; tükürük balyayı aşındırır,
 * çiftçiye çarparsa can gider (kısa dokunulmazlık, sürü kısa süre durur).
 * Dört saman balyası hücre hücre aşınır (fıskırtma, tükürük ve sürü teması);
 * dalgalar arasında onarılmaz. Kraliçe ara sıra üstten geçer, vurulunca
 * bonus. Sürü temizlenince dalga bonusu; yeni dalga bir kademe aşağıdan ve
 * daha hızlı başlar. Sürü çiftçi hizasına inerse istila: oyun biter.
 * Sabit 1/60 s adım; aynı tohum + aynı giriş dizisi = aynı koşu.
 */
class CekirgeWorld(val seed: Long) {

    companion object {
        const val STEP = 1f / 60f
        const val WIDTH = 1f
        const val HEIGHT = 1.35f
        const val COLS = 7
        const val ROWS = 5
        const val SPACING_X = 0.095f
        const val SPACING_Y = 0.075f
        const val BUG_HALF_W = 0.038f
        const val BUG_HALF_H = 0.025f
        const val SWARM_START_Y = 0.16f
        const val WAVE_DROP = 0.06f
        const val SWARM_MAX_START_Y = 0.5f
        const val SWARM_SPEED0 = 0.06f
        const val SWARM_SPEED_WAVE = 0.15f
        const val SWARM_SPEED_THIN = 3f
        const val STEP_DOWN = 0.03f
        const val EDGE_MARGIN = 0.04f
        const val FARMER_Y = 1.27f
        const val FARMER_HALF_W = 0.045f
        const val FARMER_HALF_H = 0.03f
        const val FARMER_MIN_X = 0.06f
        const val FARMER_MAX_X = 0.94f
        const val SHOT_SPEED = 1.8f
        const val SHOT_HALF_W = 0.006f
        const val SHOT_HALF_H = 0.02f
        const val SPIT_SPEED0 = 0.5f
        const val SPIT_SPEED_WAVE = 0.05f
        const val SPIT_HALF = 0.012f
        const val SPIT_INTERVAL0 = 1.8f
        const val SPIT_INTERVAL_MIN = 0.45f
        const val MAX_SPITS = 3
        const val BALES = 4
        const val BALE_Y = 1.1f
        const val BALE_W = 0.13f
        const val BALE_H = 0.06f
        const val BALE_CX = 6
        const val BALE_CY = 3

        /** Sürünün alt kenarı bu y'ye inerse istila. */
        const val INVASION_Y = FARMER_Y - 0.07f
        const val LIVES = 3
        const val INVULN = 1.5f
        const val RESPAWN_PAUSE = 1.2f
        const val QUEEN_FIRST = 18f
        const val QUEEN_EVERY_MIN = 16f
        const val QUEEN_EVERY_MAX = 28f
        const val QUEEN_Y = 0.09f
        const val QUEEN_SPEED = 0.28f
        const val QUEEN_HALF_W = 0.05f
        const val QUEEN_HALF_H = 0.025f
        val QUEEN_POINTS = intArrayOf(100, 150, 200, 300)
        const val WAVE_BONUS = 100
        const val WAVE_GAP = 1.5f
        const val TOTAL = COLS * ROWS

        fun kindOf(row: Int): BugKind = when (row) {
            0 -> BugKind.KARA
            1, 2 -> BugKind.YESIL
            else -> BugKind.KAHVE
        }

        fun dailySeed(epochDay: Long): Long = mix(epochDay, 0x43, 0x45)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }

    private val rng = Random(mix(seed, 0x43))

    var farmerX = 0.5f
        private set
    var lives = LIVES
        private set
    var score = 0
        private set
    var wave = 1
        private set
    var kills = 0
        private set
    var status = CekirgeStatus.RUNNING
        private set
    var invaded = false
        private set
    var time = 0f
        private set
    var frames = 0
        private set

    val bugs: List<Bug> = List(TOTAL) { i -> Bug(i % COLS, i / COLS, kindOf(i / COLS)) }

    /** Formasyonun 0. sütun / 0. satır merkezi ve yönü. */
    var swarmX = startX()
        private set
    var swarmY = SWARM_START_Y
        private set
    var swarmDir = 1
        private set

    /** Yürüyüş adımı sayacı (bacak animasyonu için); sürü her [SPACING_X]/2 yolda bir artar. */
    var swarmStride = 0
        private set
    private var strideAcc = 0f

    var shot: Shot? = null
        private set
    private val _spits = ArrayList<Spit>()
    val spits: List<Spit> get() = _spits
    val bales: List<Bale> = List(BALES) { i -> Bale(i, 0.125f + i * 0.25f, BALE_Y) }
    var queen: Queen? = null
        private set

    /** Dokunulmazlık ve sürü duraklaması (s). */
    var invuln = 0f
        private set
    var pause = 0f
        private set
    private var waveGap = -1f
    private var spitTimer = SPIT_INTERVAL0
    private var queenTimer = QUEEN_FIRST
    private var nextSpitId = 1
    private var spitsFrozen = false
    private var wavesFrozen = false
    private val _events = ArrayList<CekirgeEvent>()
    val events: List<CekirgeEvent> get() = _events

    /** Girdi olayları ([fire]) bir sonraki adımın sonunda listeye eklenir. */
    private val _inputEvents = ArrayList<CekirgeEvent>()

    private fun startX(): Float = (WIDTH - (COLS - 1) * SPACING_X) / 2f

    val aliveCount: Int get() = bugs.count { it.alive }
    val shotReady: Boolean get() = shot == null && status == CekirgeStatus.RUNNING && waveGap < 0f

    fun bugX(b: Bug): Float = swarmX + b.col * SPACING_X
    fun bugY(b: Bug): Float = swarmY + b.row * SPACING_Y

    /** Sürü hızı (birim/s): dalga ve seyrelmeyle artar. */
    fun swarmSpeed(): Float {
        val alive = aliveCount
        if (alive == 0) return 0f
        return SWARM_SPEED0 * (1f + SWARM_SPEED_WAVE * (wave - 1)) * (1f + SWARM_SPEED_THIN * (1f - alive.toFloat() / TOTAL))
    }

    fun hud(): CekirgeHud = CekirgeHud(score, lives, wave, aliveCount, TOTAL, kills, status, shotReady, invaded)

    // ---- girdi ----

    fun moveBy(dx: Float) {
        if (status != CekirgeStatus.RUNNING) return
        farmerX = (farmerX + dx).coerceIn(FARMER_MIN_X, FARMER_MAX_X)
    }

    fun moveTo(x: Float) {
        if (status != CekirgeStatus.RUNNING) return
        farmerX = x.coerceIn(FARMER_MIN_X, FARMER_MAX_X)
    }

    /** Tek mermi kuralı: uçan fıskırtma varken false. */
    fun fire(): Boolean {
        if (!shotReady || pause > 0f) return false
        shot = Shot(farmerX, FARMER_Y - FARMER_HALF_H)
        _inputEvents += CekirgeEvent.Fired
        return true
    }

    // ---- adım ----

    fun step(): List<CekirgeEvent> {
        _events.clear()
        if (status != CekirgeStatus.RUNNING) {
            _inputEvents.clear()
            return _events
        }
        if (frames == 0) _events += CekirgeEvent.WaveStart(wave)
        time += STEP
        frames++
        if (invuln > 0f) invuln -= STEP
        if (waveGap >= 0f) {
            waveGap -= STEP
            if (waveGap < 0f) nextWave()
        } else if (pause > 0f) {
            pause -= STEP
        } else {
            moveSwarm()
            spitting()
        }
        moveShot()
        moveSpits()
        moveQueen()
        if (status == CekirgeStatus.RUNNING && waveGap < 0f && !wavesFrozen && aliveCount == 0) clearWave()
        _events.addAll(_inputEvents)
        _inputEvents.clear()
        return _events
    }

    private fun moveSwarm() {
        val alive = bugs.filter { it.alive }
        if (alive.isEmpty()) return
        val speed = swarmSpeed()
        val dx = swarmDir * speed * STEP
        swarmX += dx
        strideAcc += abs(dx)
        if (strideAcc >= SPACING_X / 2f) {
            strideAcc -= SPACING_X / 2f
            swarmStride++
        }
        val minCol = alive.minOf { it.col }
        val maxCol = alive.maxOf { it.col }
        val left = swarmX + minCol * SPACING_X - BUG_HALF_W
        val right = swarmX + maxCol * SPACING_X + BUG_HALF_W
        if (swarmDir > 0 && right >= WIDTH - EDGE_MARGIN) {
            swarmX -= right - (WIDTH - EDGE_MARGIN)
            swarmDir = -1
            swarmY += STEP_DOWN
        } else if (swarmDir < 0 && left <= EDGE_MARGIN) {
            swarmX += EDGE_MARGIN - left
            swarmDir = 1
            swarmY += STEP_DOWN
        }
        // Balyaları kemirme ve istila.
        val maxRow = alive.maxOf { it.row }
        val bottom = swarmY + maxRow * SPACING_Y + BUG_HALF_H
        if (bottom >= BALE_Y) {
            for (b in alive) chewBale(bugX(b), bugY(b), BUG_HALF_W, BUG_HALF_H)
        }
        if (bottom >= INVASION_Y) {
            invaded = true
            over()
        }
    }

    /** Aynı anda uçan tükürük sayısı: 1. dalgada 2, sonra [MAX_SPITS]. */
    fun maxSpits(): Int = min(MAX_SPITS, 1 + wave)

    private fun spitting() {
        if (spitsFrozen) return
        spitTimer -= STEP
        if (spitTimer > 0f || _spits.size >= maxSpits()) return
        val interval = max(SPIT_INTERVAL_MIN, SPIT_INTERVAL0 - 0.12f * (wave - 1))
        spitTimer = interval * (0.7f + rng.nextFloat() * 0.6f)
        val cols = (0 until COLS).filter { c -> bugs.any { it.alive && it.col == c } }
        if (cols.isEmpty()) return
        val col = cols[rng.nextInt(cols.size)]
        val shooter = bugs.filter { it.alive && it.col == col }.maxBy { it.row }
        _spits += Spit(nextSpitId++, bugX(shooter), bugY(shooter) + BUG_HALF_H, SPIT_SPEED0 + SPIT_SPEED_WAVE * (wave - 1))
    }

    private fun moveShot() {
        val s = shot ?: return
        s.y -= SHOT_SPEED * STEP
        if (s.y < -0.05f) {
            shot = null
            return
        }
        val q = queen
        if (q != null && abs(q.x - s.x) <= QUEEN_HALF_W + SHOT_HALF_W && abs(QUEEN_Y - s.y) <= QUEEN_HALF_H + SHOT_HALF_H) {
            score += q.points
            _events += CekirgeEvent.QueenHit(q.points, q.x)
            queen = null
            shot = null
            return
        }
        var target: Bug? = null
        for (b in bugs) {
            if (!b.alive) continue
            if (abs(bugX(b) - s.x) <= BUG_HALF_W + SHOT_HALF_W && abs(bugY(b) - s.y) <= BUG_HALF_H + SHOT_HALF_H) {
                if (target == null || b.row > target.row) target = b
            }
        }
        if (target != null) {
            target.alive = false
            kills++
            score += target.kind.points
            _events += CekirgeEvent.BugHit(target.kind, bugX(target), bugY(target), target.kind.points)
            shot = null
            return
        }
        val it = _spits.iterator()
        while (it.hasNext()) {
            val sp = it.next()
            if (abs(sp.x - s.x) <= SPIT_HALF + SHOT_HALF_W && abs(sp.y - s.y) <= SPIT_HALF + SHOT_HALF_H) {
                it.remove()
                _events += CekirgeEvent.SpitHit(sp.x, sp.y)
                shot = null
                return
            }
        }
        if (erodeBale(s.x, s.y)) {
            _events += CekirgeEvent.BaleHit(s.x, s.y)
            shot = null
        }
    }

    private fun moveSpits() {
        if (pause > 0f) return
        val it = _spits.iterator()
        while (it.hasNext()) {
            val sp = it.next()
            sp.y += sp.speed * STEP
            if (sp.y > HEIGHT + 0.05f) {
                it.remove()
                continue
            }
            if (erodeBale(sp.x, sp.y)) {
                _events += CekirgeEvent.BaleHit(sp.x, sp.y)
                it.remove()
                continue
            }
            if (invuln <= 0f && abs(sp.x - farmerX) <= FARMER_HALF_W + SPIT_HALF && abs(sp.y - FARMER_Y) <= FARMER_HALF_H + SPIT_HALF) {
                it.remove()
                farmerHit()
                return
            }
        }
    }

    private fun moveQueen() {
        val q = queen
        if (q != null) {
            if (pause <= 0f) q.x += q.dir * QUEEN_SPEED * STEP
            if (q.x < -0.12f || q.x > WIDTH + 0.12f) queen = null
            return
        }
        if (waveGap >= 0f || pause > 0f) return
        queenTimer -= STEP
        if (queenTimer <= 0f && (aliveCount > 0 || wavesFrozen)) {
            val dir = if (rng.nextBoolean()) 1 else -1
            queen = Queen(dir, if (dir > 0) -0.1f else WIDTH + 0.1f, QUEEN_POINTS[rng.nextInt(QUEEN_POINTS.size)])
            queenTimer = QUEEN_EVERY_MIN + rng.nextFloat() * (QUEEN_EVERY_MAX - QUEEN_EVERY_MIN)
            _events += CekirgeEvent.QueenSpawned(dir)
        }
    }

    private fun farmerHit() {
        lives--
        _events += CekirgeEvent.FarmerHit(lives)
        _spits.clear()
        shot = null
        invuln = INVULN
        pause = RESPAWN_PAUSE
        if (lives <= 0) over()
    }

    private fun clearWave() {
        val bonus = WAVE_BONUS * wave
        score += bonus
        _events += CekirgeEvent.WaveCleared(wave, bonus)
        waveGap = WAVE_GAP
        _spits.clear()
        shot = null
    }

    private fun nextWave() {
        wave++
        for (b in bugs) b.alive = true
        swarmX = startX()
        swarmY = min(SWARM_START_Y + WAVE_DROP * (wave - 1), SWARM_MAX_START_Y)
        swarmDir = 1
        strideAcc = 0f
        spitTimer = SPIT_INTERVAL0
        waveGap = -1f
        _events += CekirgeEvent.WaveStart(wave)
    }

    private fun over() {
        status = CekirgeStatus.OVER
        shot = null
        _events += CekirgeEvent.Over(invaded)
    }

    // ---- balyalar ----

    /** (x, y) bir balya hücresine düşüyorsa hücreyi ve komşularını siler; silindiyse true. */
    private fun erodeBale(x: Float, y: Float): Boolean {
        for (b in bales) {
            val cx = ((x - b.left) / b.cellW).toInt()
            val cy = ((y - b.top) / b.cellH).toInt()
            if (x < b.left || cx !in 0 until BALE_CX || y < b.top || cy !in 0 until BALE_CY) continue
            if (!b.cells[cy][cx]) continue
            b.cells[cy][cx] = false
            // Küçük krater: yatay komşular.
            if (cx > 0 && rng.nextFloat() < 0.5f) b.cells[cy][cx - 1] = false
            if (cx < BALE_CX - 1 && rng.nextFloat() < 0.5f) b.cells[cy][cx + 1] = false
            return true
        }
        return false
    }

    private fun chewBale(x: Float, y: Float, hw: Float, hh: Float) {
        for (b in bales) {
            if (x + hw < b.left || x - hw > b.left + BALE_W || y + hh < b.top || y - hh > b.top + BALE_H) continue
            for (cy in 0 until BALE_CY) {
                for (cx in 0 until BALE_CX) {
                    val cellX = b.left + (cx + 0.5f) * b.cellW
                    val cellY = b.top + (cy + 0.5f) * b.cellH
                    if (abs(cellX - x) <= hw + b.cellW / 2f && abs(cellY - y) <= hh + b.cellH / 2f) b.cells[cy][cx] = false
                }
            }
        }
    }

    // ---- test kancaları ----

    fun killForTest(col: Int, row: Int) {
        bugs.first { it.col == col && it.row == row }.alive = false
    }

    fun spawnSpitForTest(x: Float, y: Float, speed: Float = SPIT_SPEED0): Spit {
        val s = Spit(nextSpitId++, x, y, speed)
        _spits += s
        return s
    }

    fun setSwarmForTest(x: Float, y: Float, dir: Int) {
        swarmX = x
        swarmY = y
        swarmDir = dir
    }

    fun setFarmerForTest(x: Float) { farmerX = x }
    fun setQueenTimerForTest(v: Float) { queenTimer = v }
    fun freezeSpitsForTest() { spitsFrozen = true }

    /** Sürü bitince dalga temizlenmez (kraliçe gibi kuralları boş tarlada ölçmek için). */
    fun freezeWavesForTest() { wavesFrozen = true }
}
