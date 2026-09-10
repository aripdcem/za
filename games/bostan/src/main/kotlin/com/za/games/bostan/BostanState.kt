package com.za.games.bostan

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Bostan simülasyonu: [COLS] şerit × [ROWS] satırlık tarla. Saldırganlar
 * satır −1'de doğar, +y yönünde (kulübeye) yürür; [HUT_Y]'yi geçen can
 * götürür. Savunmalar hücrelere su karşılığı konur ([place]); kuyular su
 * üretir, gökten düşen damlalar dokununca su verir ([collectDrop]). Fıskiye
 * şeridinde önündeki saldırgana jet atar, korkuluk yolu keser, kovan
 * çevresindeki üç şeride arı salar, tuzak kurulduktan sonra üstüne basanı
 * patlatıp tükenir. Şeridinde önüne savunma çıkan saldırgan durup onu
 * kemirir; savunma bitince yürümeye devam eder.
 *
 * Dalgalar [BostanLevel.waves] sırasıyla gelir: ilk dalga [PREP] saniye
 * hazırlıktan sonra; bir dalga tüm doğumları bitip tarla boşalınca
 * temizlenir ([WAVE_GAP] sonra sıradaki), temizlenmese de süresi +
 * [WAVE_GRACE] geçince sıradaki başlar. Son dalga temizlenince kazanılır.
 * Sabit 1/60 s adım; aynı seviye + aynı giriş dizisi = aynı koşu.
 *
 * Girdi çağrıları ([place], [remove], [collectDrop]) adımlar arasında
 * yapılır; ürettikleri olaylar bir sonraki [step] başında silinen listeye
 * eklenir, bu yüzden arayüz bu üçünün geri bildirimini dönüş değerinden alır.
 */
class BostanState(val level: BostanLevel) {

    companion object {
        const val STEP = 1f / 60f
        const val COLS = 5
        const val ROWS = 7
        const val LIVES = 3

        /** Saldırgan merkezinin kulübeye ulaştığı y (tarlanın alt kenarı). */
        const val HUT_Y = ROWS - 0.5f
        const val ENEMY_HALF = 0.45f
        const val PREP = 10f
        const val WAVE_GAP = 4f
        const val WAVE_GRACE = 12f
        const val DROP_FIRST = 2f
        const val DROP_INTERVAL = 7f
        const val DROP_TTL = 6f
        const val DROP_WATER = 25
        const val WELL_INTERVAL = 8f
        const val WELL_WATER = 25
        const val FIRE_INTERVAL = 1.4f
        const val JET_SPEED = 3f
        const val JET_DAMAGE = 1f
        const val HIVE_INTERVAL = 2f
        const val HIVE_DAMAGE = 2f
        const val HIVE_REACH = 2f
        const val TRAP_ARM = 3f
        const val TRAP_DAMAGE = 30f
        const val TRAP_REACH = 1f
        const val WAVE_BONUS = 50
        const val LIFE_BONUS = 100
        const val FLASH = 0.15f

        fun dailySeed(epochDay: Long): Long = mix(epochDay, 0x42, 0x4F)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }
    }

    private val rng = Random(mix(level.seed, 0x42, level.difficulty.ordinal))

    var time = 0f
        private set
    var water = level.startWater
        private set
    var lives = LIVES
        private set
    var kills = 0
        private set
    var score = 0
        private set
    var status = BostanStatus.RUNNING
        private set

    /** Süren dalganın dizini; hazırlıkta −1. */
    var waveIndex = -1
        private set
    private var waveClock = 0f
    private var nextWaveAt = PREP
    private var spawnCursor = 0
    private var waveCleared = false
    private var dropTimer = DROP_FIRST
    private var nextId = 1
    private val cooldown = FloatArray(DefenderKind.entries.size)

    private val _defenders = ArrayList<Defender>()
    private val _enemies = ArrayList<Enemy>()
    private val _jets = ArrayList<Jet>()
    private val _drops = ArrayList<Drop>()
    private val _events = ArrayList<BostanEvent>()

    val defenders: List<Defender> get() = _defenders
    val enemies: List<Enemy> get() = _enemies
    val jets: List<Jet> get() = _jets
    val drops: List<Drop> get() = _drops

    /** Son adımın olayları (girdi olayları bir sonraki adıma dek kalır). */
    val events: List<BostanEvent> get() = _events

    val wave: Int get() = waveIndex + 1

    val waveProgress: Float
        get() {
            if (waveIndex < 0) return 0f
            val d = level.waves[waveIndex].duration
            return if (d <= 0f) 1f else min(1f, waveClock / d)
        }

    val nextWaveIn: Float
        get() = if (status == BostanStatus.RUNNING && waveIndex < level.waves.lastIndex) max(0f, nextWaveAt - time) else 0f

    fun defenderAt(lane: Int, row: Int): Defender? {
        for (d in _defenders) if (d.alive && d.lane == lane && d.row == row) return d
        return null
    }

    fun dropAt(lane: Int, row: Int): Drop? {
        for (d in _drops) if (d.ttl > 0f && d.lane == lane && d.row == row) return d
        return null
    }

    /** Kartın kalan bekleme süresi (s). */
    fun cooldownOf(kind: DefenderKind): Float = cooldown[kind.ordinal]

    fun affordable(kind: DefenderKind): Boolean = water >= kind.cost
    fun ready(kind: DefenderKind): Boolean = cooldown[kind.ordinal] <= 0f

    fun canPlace(kind: DefenderKind, lane: Int, row: Int): Boolean =
        status == BostanStatus.RUNNING && lane in 0 until COLS && row in 0 until ROWS &&
            defenderAt(lane, row) == null && affordable(kind) && ready(kind)

    /** Savunma koyar; su düşer, kart beklemeye girer. Koyamazsa false. */
    fun place(kind: DefenderKind, lane: Int, row: Int): Boolean {
        if (!canPlace(kind, lane, row)) return false
        water -= kind.cost
        val d = Defender(kind, lane, row)
        // Fıskiye ve kovan konur konmaz ateşe hazır: geri bildirim anında.
        if (kind == DefenderKind.FISKIYE) d.timer = FIRE_INTERVAL
        if (kind == DefenderKind.KOVAN) d.timer = HIVE_INTERVAL
        _defenders += d
        cooldown[kind.ordinal] = kind.cooldown
        _events += BostanEvent.Placed(kind, lane, row)
        return true
    }

    /** Kürek: hücredeki savunmayı kaldırır (su iadesi yok). */
    fun remove(lane: Int, row: Int): Boolean {
        if (status != BostanStatus.RUNNING) return false
        val d = defenderAt(lane, row) ?: return false
        d.hp = 0f
        _defenders.remove(d)
        _events += BostanEvent.Removed(d.kind, lane, row)
        return true
    }

    /** Hücredeki damlayı toplar: +[DROP_WATER]. */
    fun collectDrop(lane: Int, row: Int): Boolean {
        if (status != BostanStatus.RUNNING) return false
        val d = dropAt(lane, row) ?: return false
        _drops.remove(d)
        gain(DROP_WATER, lane, row)
        return true
    }

    fun step() {
        _events.clear()
        if (status != BostanStatus.RUNNING) return
        time += STEP
        for (i in cooldown.indices) if (cooldown[i] > 0f) cooldown[i] = max(0f, cooldown[i] - STEP)
        advanceWaves()
        rain()
        for (d in _defenders) if (d.alive) act(d)
        moveJets()
        moveEnemies()
        prune()
        checkEnd()
    }

    fun hud(): BostanHud = BostanHud(
        water = water,
        lives = lives,
        wave = wave,
        totalWaves = level.waves.size,
        waveProgress = waveProgress,
        nextWaveIn = nextWaveIn,
        kills = kills,
        score = score,
        status = status,
        cooldowns = DefenderKind.entries.map { k -> if (k.cooldown <= 0f) 0f else min(1f, cooldown[k.ordinal] / k.cooldown) },
        enemiesAlive = _enemies.size,
    )

    // ---- dalgalar ----

    private fun advanceWaves() {
        if (waveIndex < level.waves.lastIndex && time >= nextWaveAt) startWave(waveIndex + 1)
        if (waveIndex < 0) return
        waveClock += STEP
        val w = level.waves[waveIndex]
        while (spawnCursor < w.spawns.size && w.spawns[spawnCursor].at <= waveClock) {
            val s = w.spawns[spawnCursor++]
            spawn(s.kind, s.lane, -1f)
        }
    }

    private fun startWave(index: Int) {
        waveIndex = index
        waveClock = 0f
        spawnCursor = 0
        waveCleared = false
        val w = level.waves[index]
        nextWaveAt = time + w.duration + WAVE_GRACE
        _events += BostanEvent.WaveStart(index, w.big)
    }

    private fun spawn(kind: EnemyKind, lane: Int, y: Float) {
        val e = Enemy(nextId++, kind, lane)
        e.y = y
        _enemies += e
        _events += BostanEvent.EnemySpawned(kind, lane)
    }

    private fun checkEnd() {
        if (lives <= 0) {
            status = BostanStatus.LOST
            _events += BostanEvent.Lost
            return
        }
        if (waveIndex < 0 || waveCleared) return
        val w = level.waves[waveIndex]
        if (spawnCursor >= w.spawns.size && _enemies.isEmpty()) {
            waveCleared = true
            score += WAVE_BONUS
            _events += BostanEvent.WaveClear(waveIndex)
            if (waveIndex == level.waves.lastIndex) {
                score += lives * LIFE_BONUS + water / 5
                status = BostanStatus.WON
                _events += BostanEvent.Won
            } else {
                nextWaveAt = min(nextWaveAt, time + WAVE_GAP)
            }
        }
    }

    // ---- su ----

    private fun rain() {
        dropTimer -= STEP
        if (dropTimer <= 0f) {
            dropTimer += DROP_INTERVAL * (0.85f + rng.nextFloat() * 0.3f)
            val lane = rng.nextInt(COLS)
            val row = rng.nextInt(ROWS)
            _drops += Drop(nextId++, lane, row)
            _events += BostanEvent.DropFell(lane, row)
        }
        for (d in _drops) d.ttl -= STEP
    }

    private fun gain(amount: Int, lane: Int, row: Int) {
        water += amount
        _events += BostanEvent.Water(amount, lane, row)
    }

    // ---- savunmalar ----

    private fun act(d: Defender) {
        if (d.flash > 0f) d.flash -= STEP
        d.timer += STEP
        when (d.kind) {
            DefenderKind.KUYU -> if (d.timer >= WELL_INTERVAL) {
                d.timer -= WELL_INTERVAL
                gain(WELL_WATER, d.lane, d.row)
            }
            DefenderKind.FISKIYE -> if (d.timer >= FIRE_INTERVAL && hasTarget(d)) {
                d.timer = 0f
                _jets += Jet(d.lane, d.row - 0.3f)
            }
            DefenderKind.KORKULUK -> Unit
            DefenderKind.KOVAN -> if (d.timer >= HIVE_INTERVAL) {
                var any = false
                for (e in _enemies) {
                    if (e.alive && abs(e.lane - d.lane) <= 1 && abs(e.y - d.row) <= HIVE_REACH) {
                        hit(e, HIVE_DAMAGE)
                        any = true
                    }
                }
                if (any) d.timer = 0f
            }
            DefenderKind.TUZAK -> {
                if (!d.armed && d.timer >= TRAP_ARM) d.armed = true
                if (d.armed && _enemies.any { it.alive && it.lane == d.lane && reaches(it, d.row) }) blast(d)
            }
        }
    }

    /** Fıskiyenin şeridinde, önünde (ya da onu kemiren) bir saldırgan var mı. */
    private fun hasTarget(d: Defender): Boolean {
        for (e in _enemies) if (e.alive && e.lane == d.lane && e.y <= d.row + 0.2f) return true
        return false
    }

    /** Saldırganın önü hücreye girmiş ve merkezi onu geçmemiş mi. */
    private fun reaches(e: Enemy, row: Int): Boolean = e.front >= row - 0.5f && e.y <= row + 0.2f

    private fun blast(d: Defender) {
        d.hp = 0f
        _events += BostanEvent.TrapBlast(d.lane, d.row)
        for (e in _enemies) {
            if (e.alive && e.lane == d.lane && abs(e.y - d.row) <= TRAP_REACH + ENEMY_HALF) hit(e, TRAP_DAMAGE)
        }
    }

    private fun moveJets() {
        val it = _jets.iterator()
        while (it.hasNext()) {
            val j = it.next()
            j.y -= JET_SPEED * STEP
            var target: Enemy? = null
            for (e in _enemies) {
                if (e.alive && e.lane == j.lane && abs(e.y - j.y) <= ENEMY_HALF && (target == null || e.y > target.y)) target = e
            }
            if (target != null) {
                hit(target, JET_DAMAGE)
                it.remove()
            } else if (j.y < -1.5f) {
                it.remove()
            }
        }
    }

    // ---- saldırganlar ----

    private fun moveEnemies() {
        for (e in _enemies) {
            if (!e.alive) continue
            if (e.flash > 0f) e.flash -= STEP
            val blocker = blockerOf(e)
            if (blocker != null) {
                e.biting = true
                blocker.hp -= e.kind.bite * STEP
                blocker.flash = FLASH
                if (blocker.hp <= 0f) _events += BostanEvent.DefenderDown(blocker.kind, blocker.lane, blocker.row)
            } else {
                e.biting = false
                e.y += e.kind.speed * STEP
                if (e.y >= HUT_Y) {
                    e.hp = 0f
                    lives--
                    _events += BostanEvent.LifeLost(lives, e.lane)
                }
            }
        }
    }

    /** Şeritte saldırganın önüne çıkan ilk canlı savunma. */
    private fun blockerOf(e: Enemy): Defender? {
        var best: Defender? = null
        for (d in _defenders) {
            if (d.alive && d.lane == e.lane && reaches(e, d.row) && (best == null || d.row < best.row)) best = d
        }
        return best
    }

    private fun hit(e: Enemy, dmg: Float) {
        if (!e.alive) return
        e.hp -= dmg
        e.flash = FLASH
        _events += BostanEvent.EnemyHit(e.lane, e.y)
        if (e.hp <= 0f) {
            kills++
            score += e.kind.points
            _events += BostanEvent.EnemyDown(e.kind, e.lane, e.y, e.kind.points)
        }
    }

    private fun prune() {
        _enemies.removeAll { !it.alive }
        _defenders.removeAll { !it.alive }
        _drops.removeAll { it.ttl <= 0f }
    }

    // ---- test kancaları ----

    fun spawnForTest(kind: EnemyKind, lane: Int, y: Float): Enemy {
        spawn(kind, lane, y)
        return _enemies.last()
    }

    fun setWaterForTest(v: Int) { water = v }
    fun setLivesForTest(v: Int) { lives = v }

    /** Dalgaları askıya alır: hiçbir dalga başlamaz. */
    fun holdWavesForTest() { nextWaveAt = Float.MAX_VALUE }

    /** Yağmuru keser: damla düşmez. */
    fun stopRainForTest() { dropTimer = Float.MAX_VALUE }

    fun dropForTest(lane: Int, row: Int): Drop {
        val d = Drop(nextId++, lane, row)
        _drops += d
        return d
    }

    fun clearCooldownsForTest() { cooldown.fill(0f) }
}
