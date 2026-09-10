package com.za.games.sincap

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sincap simülasyonu: gövde ortada, her yükseklik basamağında ([Level])
 * solda ve/veya sağda bir dal. Sola ya da sağa dokununca ([tap]) sincap o
 * yöndeki en yakın üst dala atlar (en çok [REACH] basamak); o yönde erişilen
 * dal yoksa boşluğa atlayıp düşer. Zıplarken gelen dokunuş bekletilir ve
 * konar konmaz uygulanır.
 *
 * Tehlikeler: kuru dal konduktan [DRY_HOLD] s sonra kırılır; yılanlı dala
 * konan ölür; kargalar kendi basamağından ekranı boydan boya geçer, dalın
 * üstündeyken çarpan düşürür; kedi gövdeden tırmanır, yükseklikle hızlanır
 * (tempo baskısı: koşuyu bitiren odur), yetişirse yakalar. Fındık +[NUT_POINTS], altın fındık +[GOLD_POINTS], her
 * basamak +[HEIGHT_POINTS].
 *
 * Üretim her basamakta en az bir dal bırakır ve her basamaktan güvenli bir
 * kaçış garanti eder ([repair]): iki yanın da erişilen ilk dalı yılanlı
 * olamaz. Kargalı basamağın altındaki basamakta kuru dal olmaz (bekleyecek
 * yer kalsın). Sabit 1/60 s adım; aynı tohum + aynı giriş dizisi = aynı koşu.
 */
class SincapWorld(val seed: Long) {

    companion object {
        const val STEP = 1f / 60f
        const val REACH = 2
        const val JUMP_TIME = 0.22f

        /** Her ek basamak için zıplama süresi payı. */
        const val JUMP_EXTRA = 0.35f
        const val FALL_TIME = 0.5f
        const val DRY_HOLD = 1.1f
        const val BRANCH_X = 0.36f
        const val CROW_HIT = 0.16f
        const val CROW_EDGE = 1.3f

        /** Karga, sincap basamağının bu kadar altındayken belirir. */
        const val CROW_LEAD = 5
        const val CROW_SPEED0 = 0.9f
        const val CROW_SPEED_MAX = 1.5f
        const val CAT_GAP0 = 9f
        const val CAT_GAP_MAX = 10f
        const val CAT_SPEED0 = 0.5f

        /** [CAT_RAMP] basamakta ulaşılan hız; sonrası [CAT_LATE_SLOPE] eğimiyle [CAT_SPEED_CAP]'e dek sürer. */
        const val CAT_SPEED_MAX = 3.2f
        const val CAT_RAMP = 250f
        const val CAT_LATE_SLOPE = 0.8f
        const val CAT_SPEED_CAP = 5f
        const val CAT_WARN = 2.5f
        const val AHEAD = 12
        const val EASY_LEVELS = 3
        const val RAMP = 150f
        const val HEIGHT_POINTS = 10
        const val NUT_POINTS = 20
        const val GOLD_POINTS = 100
        const val MILESTONE = 25

        fun sideX(side: Side): Float = side.x * BRANCH_X

        fun dailySeed(epochDay: Long): Long = mix(epochDay, 0x53, 0x49)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }

    private val rng = Random(mix(seed, 0x53))
    private val _levels = ArrayList<Level>()
    val levels: List<Level> get() = _levels

    var level = 0
        private set
    var side = Side.RIGHT
        private set

    /** Sürekli yükseklik (basamak); zıplarken kalkış ve varış arasında. */
    var y = 0f
        private set
    var jumping = false
        private set
    var falling = false
        private set
    private var jumpT = 0f
    private var jumpDuration = JUMP_TIME
    private var jumpFromLevel = 0
    private var jumpFromSide = Side.RIGHT
    private var jumpToLevel = 0
    private var jumpToSide = Side.RIGHT
    private var fallT = 0f
    private var fallSide = Side.RIGHT
    private var pending: Side? = null

    /** Kuru dalın kalan süresi (s); kuru dalda değilse ≤ 0. */
    var dryLeft = -1f
        private set
    var catY = -CAT_GAP0
        private set
    var height = 0
        private set
    var nuts = 0
        private set
    var golds = 0
        private set
    var time = 0f
        private set
    var frames = 0
        private set
    var status = SincapStatus.RUNNING
        private set
    var cause: DeathCause? = null
        private set

    private val _crows = ArrayList<Crow>()
    val crows: List<Crow> get() = _crows
    private var crowCursor = 0
    private var nextCrowId = 1
    private var catWarned = false
    private var crowsFrozen = false
    private var catFrozen = false
    private val _events = ArrayList<SincapEvent>()

    /** Adımlar arasında ([tap]) üretilen olaylar; bir sonraki [step] sonunda listeye eklenir. */
    private val _inputEvents = ArrayList<SincapEvent>()

    init {
        ensureLevels(AHEAD)
    }

    val score: Int get() = height * HEIGHT_POINTS + nuts * NUT_POINTS + golds * GOLD_POINTS
    val catGap: Float get() = y - catY
    val onDry: Boolean get() = dryLeft > 0f

    /** Zıplama ilerlemesi (0..1); zıplamıyorsa 0. */
    val jumpProgress: Float get() = if (jumping) min(1f, jumpT / jumpDuration) else if (falling) min(1f, fallT / FALL_TIME) else 0f

    /** Sincabın yatay konumu: dalda [sideX], zıplarken iki dal arasında. */
    val x: Float
        get() = when {
            jumping -> sideX(jumpFromSide) + (sideX(jumpToSide) - sideX(jumpFromSide)) * jumpProgress
            falling -> sideX(side) + (sideX(fallSide) - sideX(side)) * min(1f, fallT / FALL_TIME * 2f)
            else -> sideX(side)
        }

    /** Zıplarken hedef basamak; değilse −1. */
    val jumpTarget: Int get() = if (jumping) jumpToLevel else -1

    fun hud(): SincapHud = SincapHud(height, nuts, score, catGap, status, cause)

    /** [side] yönünde erişilen ilk dalın basamağı (level+1..level+[REACH]); yoksa −1. */
    fun target(side: Side): Int {
        for (j in level + 1..level + REACH) if (levelAt(j).at(side).present) return j
        return -1
    }

    fun levelAt(i: Int): Level {
        ensureLevels(i)
        return _levels[i]
    }

    /** Sola/sağa dokunuş. Zıplarken bekletilir. Bitmişse false. */
    fun tap(side: Side): Boolean {
        if (status != SincapStatus.RUNNING) return false
        if (jumping || falling) {
            pending = side
            return true
        }
        jump(side)
        return true
    }

    private fun jump(side: Side) {
        val j = target(side)
        _inputEvents += SincapEvent.Jumped(level, j, side)
        dryLeft = -1f
        if (j < 0) {
            falling = true
            fallT = 0f
            fallSide = side
            return
        }
        jumping = true
        jumpT = 0f
        jumpDuration = JUMP_TIME * (1f + JUMP_EXTRA * (j - level - 1))
        jumpFromLevel = level
        jumpFromSide = this.side
        jumpToLevel = j
        jumpToSide = side
    }

    /** Son adımın olayları; dokunuşla üretilen [SincapEvent.Jumped] bir sonraki adımda gelir. */
    val events: List<SincapEvent> get() = _events

    fun step(): List<SincapEvent> {
        _events.clear()
        if (status != SincapStatus.RUNNING) {
            _inputEvents.clear()
            return _events
        }
        time += STEP
        frames++
        when {
            jumping -> {
                jumpT += STEP
                y = jumpFromLevel + (jumpToLevel - jumpFromLevel) * min(1f, jumpT / jumpDuration)
                if (jumpT >= jumpDuration) land()
            }
            falling -> {
                fallT += STEP
                // Kısa bir yay: yükselir, sonra düşer.
                y = level + 0.6f * sin(min(1f, fallT / FALL_TIME) * 3.1416f) - 1.5f * max(0f, fallT / FALL_TIME - 0.5f)
                if (fallT >= FALL_TIME) over(DeathCause.FALL)
            }
            dryLeft > 0f -> {
                dryLeft -= STEP
                if (dryLeft <= 0f) {
                    levelAt(level).set(side, BranchKind.NONE)
                    _events += SincapEvent.Broke(level, side)
                    over(DeathCause.BROKE)
                }
            }
        }
        if (status == SincapStatus.RUNNING) crows()
        if (status == SincapStatus.RUNNING) cat()
        _events.addAll(_inputEvents)
        _inputEvents.clear()
        return _events
    }

    private fun land() {
        jumping = false
        level = jumpToLevel
        side = jumpToSide
        y = level.toFloat()
        val lv = levelAt(level)
        val kind = lv.at(side)
        _events += SincapEvent.Landed(level, side, kind)
        if (level > height) {
            val prev = height
            height = level
            if (height / MILESTONE > prev / MILESTONE) _events += SincapEvent.Milestone(height)
        }
        ensureLevels(level + AHEAD)
        when (kind) {
            BranchKind.SNAKE -> {
                over(DeathCause.SNAKE)
                return
            }
            BranchKind.NUT -> {
                nuts++
                lv.set(side, BranchKind.NORMAL)
                _events += SincapEvent.Nut(level, side, NUT_POINTS, false)
            }
            BranchKind.GOLD -> {
                golds++
                lv.set(side, BranchKind.NORMAL)
                _events += SincapEvent.Nut(level, side, GOLD_POINTS, true)
            }
            BranchKind.DRY -> {
                dryLeft = DRY_HOLD
                _events += SincapEvent.Cracking(level, side)
            }
            else -> Unit
        }
        if (crowHit()) {
            over(DeathCause.CROW)
            return
        }
        val p = pending
        pending = null
        if (p != null) jump(p)
    }

    private fun over(c: DeathCause) {
        status = SincapStatus.OVER
        cause = c
        jumping = false
        falling = false
        pending = null
        _inputEvents.clear()
        _events += SincapEvent.Over(c)
    }

    // ---- kargalar ----

    private fun crows() {
        if (!crowsFrozen) {
            while (crowCursor < _levels.size && crowCursor <= level + CROW_LEAD) {
                if (_levels[crowCursor].crow) spawnCrow(crowCursor)
                crowCursor++
            }
        }
        val it = _crows.iterator()
        while (it.hasNext()) {
            val c = it.next()
            c.x += c.dir * c.speed * STEP
            if (abs(c.x) > CROW_EDGE) it.remove()
        }
        if (!jumping && !falling && crowHit()) over(DeathCause.CROW)
    }

    private fun spawnCrow(lv: Int) {
        val dir = if (rng.nextBoolean()) 1 else -1
        val speed = CROW_SPEED0 + (CROW_SPEED_MAX - CROW_SPEED0) * min(1f, lv / RAMP) * (0.8f + rng.nextFloat() * 0.4f)
        _crows += Crow(nextCrowId++, lv, dir, -dir * CROW_EDGE, speed)
        _events += SincapEvent.CrowSpawned(lv, dir)
    }

    private fun crowHit(): Boolean {
        val sx = sideX(side)
        for (c in _crows) if (c.level == level && abs(c.x - sx) < CROW_HIT) return true
        return false
    }

    // ---- kedi ----

    /** Kedi hızı (basamak/s): yüksekliğe göre doğrusal, rampadan sonra yavaşça artmayı sürdürür. */
    fun catSpeed(): Float {
        val ramp = CAT_SPEED0 + (CAT_SPEED_MAX - CAT_SPEED0) * min(1f, height / CAT_RAMP)
        val late = CAT_LATE_SLOPE * max(0f, (height - CAT_RAMP) / CAT_RAMP)
        return min(CAT_SPEED_CAP, ramp + late)
    }

    private fun cat() {
        if (catFrozen) return
        catY += catSpeed() * STEP
        if (catY < y - CAT_GAP_MAX) catY = y - CAT_GAP_MAX
        if (catGap < CAT_WARN) {
            if (!catWarned) {
                catWarned = true
                _events += SincapEvent.CatClose
            }
        } else if (catGap > CAT_WARN + 1f) {
            catWarned = false
        }
        if (catY >= y - 0.15f) over(DeathCause.CAT)
    }

    // ---- üretim ----

    private fun ensureLevels(upTo: Int) {
        while (_levels.size <= upTo) generateLevel(_levels.size)
    }

    private fun generateLevel(i: Int) {
        if (i <= EASY_LEVELS) {
            _levels += Level(BranchKind.NORMAL, BranchKind.NORMAL, false)
            return
        }
        val p = min(1f, i / RAMP)
        val both = rng.nextFloat() < 0.75f - 0.35f * p
        var left = BranchKind.NONE
        var right = BranchKind.NONE
        if (both) {
            left = BranchKind.NORMAL
            right = BranchKind.NORMAL
        } else if (rng.nextBoolean()) {
            left = BranchKind.NORMAL
        } else {
            right = BranchKind.NORMAL
        }
        if (left.present) left = kind(i, p)
        if (right.present) right = kind(i, p)
        val prev = _levels[i - 1]
        if (!prev.safe && !left.safe && !right.safe) {
            if (left.present) left = BranchKind.NORMAL else right = BranchKind.NORMAL
        }
        val crow = i > EASY_LEVELS + 2 && rng.nextFloat() < 0.1f + 0.2f * p
        if (crow) {
            if (prev.left == BranchKind.DRY) prev.left = BranchKind.NORMAL
            if (prev.right == BranchKind.DRY) prev.right = BranchKind.NORMAL
        }
        _levels += Level(left, right, crow)
        if (i - REACH >= 0) repair(i - REACH)
    }

    private fun kind(i: Int, p: Float): BranchKind {
        val r = rng.nextFloat()
        val gold = 0.03f
        val nut = 0.22f
        val dry = 0.05f + 0.25f * p
        val snake = if (i > 10) 0.02f + 0.1f * p else 0f
        return when {
            r < gold -> BranchKind.GOLD
            r < gold + nut -> BranchKind.NUT
            r < gold + nut + dry -> BranchKind.DRY
            r < gold + nut + dry + snake -> BranchKind.SNAKE
            else -> BranchKind.NORMAL
        }
    }

    /** [k] basamağından erişilen ilk dal ([side] yönünde); yoksa NONE. */
    fun nearest(k: Int, side: Side): BranchKind {
        for (j in k + 1..k + REACH) {
            val b = _levels.getOrNull(j)?.at(side) ?: return BranchKind.NONE
            if (b.present) return b
        }
        return BranchKind.NONE
    }

    /** [k] basamağından güvenli bir yön var mı. */
    fun escapable(k: Int): Boolean = Side.entries.any { nearest(k, it).safe }

    /** Kaçış yoksa erişilen ilk yılanlı dalı sağlamlaştırır. */
    private fun repair(k: Int) {
        if (escapable(k)) return
        for (s in Side.entries) {
            for (j in k + 1..k + REACH) {
                val lv = _levels[j]
                if (lv.at(s) == BranchKind.SNAKE) {
                    lv.set(s, BranchKind.NORMAL)
                    return
                }
            }
        }
        _levels[k + 1].set(Side.RIGHT, BranchKind.NORMAL)
    }

    // ---- test kancaları ----

    fun setLevelForTest(i: Int, left: BranchKind, right: BranchKind) {
        ensureLevels(i)
        _levels[i].left = left
        _levels[i].right = right
    }

    fun spawnCrowForTest(level: Int, dir: Int, x: Float, speed: Float): Crow {
        val c = Crow(nextCrowId++, level, dir, x, speed)
        _crows += c
        return c
    }

    fun freezeCrowsForTest() { crowsFrozen = true }
    fun freezeCatForTest() { catFrozen = true }
    fun setCatForTest(v: Float) { catY = v }
    fun setDryLeftForTest(v: Float) { dryLeft = v }
}
