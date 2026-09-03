package com.za.games.kuyu

import kotlin.math.min
import kotlin.random.Random

/**
 * Kuyu üreteci: parça (16 satır) başına tohumdan deterministik duvar, çıkıntı
 * ve düşman yerleşimi. Komşu parçalar birbirini bilmeden uyumlu duvar kalınlığı
 * türetir (bant başına karma). Her satırda en az [MIN_GAP] bitişik boş hücre
 * kalır ve art arda iki satıra çıkıntı konmaz; böylece oyuncu hiçbir zaman
 * sıkışmaz (bkz. KuyuGenTest geçilebilirlik testi).
 */
object KuyuGen {
    const val WIDTH = 12
    const val CHUNK_ROWS = 16

    /** Her satırda garanti edilen bitişik boşluk. */
    const val MIN_GAP = 3

    /** Başlangıç platformunun satırı; oyuncu bunun üstünde doğar. */
    const val START_ROW = 2

    /** Bölge (palet ve düşman seti) bu kadar parçada bir değişir. */
    const val AREA_CHUNKS = 8

    private const val BAND_ROWS = 4
    private const val NICHE_CHANCE = 0.35f

    /** Arenadaki kırılmaz çıkıntıların yerel satırları. */
    val ARENA_LEDGE_ROWS = listOf(2, 5, 8, 11)

    /** Bekçinin arenadaki yerel satırı; kapı son satırdadır. */
    const val BOSS_ROW = 12

    /** Sandık kırılınca bırakılan taş. */
    const val CHEST_GEMS = 15

    /** splitmix64 karması; tüm rastgelelik buradan tohumlanır. */
    fun mix(seed: Long, a: Int, b: Int = 0): Long {
        var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
        z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
        z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
        return z xor (z ushr 31)
    }

    /** 0, 1, 2: bölge dizini. */
    fun area(chunkIndex: Int): Int = min(chunkIndex / AREA_CHUNKS, 2)

    fun leftWall(seed: Long, row: Int): Int = thickness(mix(seed, row / BAND_ROWS, 11))

    fun rightWall(seed: Long, row: Int): Int = thickness(mix(seed, row / BAND_ROWS, 13))

    private fun thickness(hash: Long): Int = when (((hash ushr 40) % 4).toInt()) {
        0 -> 1
        3 -> 3
        else -> 2
    }

    private fun blockOrGem(rng: Random): Tile =
        if (rng.nextFloat() < 0.3f) Tile.GEM_BLOCK else Tile.BLOCK

    /** Her bölgenin son parçası bekçi arenasıdır. */
    fun isBossChunk(index: Int): Boolean = index > 0 && (index + 1) % AREA_CHUNKS == 0

    fun chunk(seed: Long, index: Int): KuyuChunk {
        require(index >= 0) { "Parça dizini negatif olamaz" }
        if (isBossChunk(index)) return arena(index)
        val rng = Random(mix(seed, index, 3))
        val tiles = Array(CHUNK_ROWS * WIDTH) { Tile.EMPTY }
        fun set(r: Int, c: Int, t: Tile) {
            tiles[r * WIDTH + c] = t
        }
        val left = IntArray(CHUNK_ROWS) { leftWall(seed, index * CHUNK_ROWS + it) }
        val right = IntArray(CHUNK_ROWS) { rightWall(seed, index * CHUNK_ROWS + it) }
        for (r in 0 until CHUNK_ROWS) {
            for (c in 0 until left[r]) set(r, c, Tile.WALL)
            for (c in WIDTH - right[r] until WIDTH) set(r, c, Tile.WALL)
        }

        // Çıkıntı konmuş (ya da korunan) satırlar; ilk ve son satır hep boş kalır
        // ki parça sınırında iki çıkıntı üst üste gelmesin.
        val occupied = BooleanArray(CHUNK_ROWS)
        var r = 1
        if (index == 0) {
            // Başlangıç platformu: sol duvara yapışık, sağında MIN_GAP boşluk.
            val l = left[START_ROW]
            val len = WIDTH - l - right[START_ROW] - MIN_GAP
            for (c in l until l + len) set(START_ROW, c, Tile.BLOCK)
            for (rr in 0..START_ROW + 1) occupied[rr] = true
            r = START_ROW + 2
        }
        val ledgeChance = 0.26f + min(index, 20) * 0.006f
        while (r < CHUNK_ROWS - 1) {
            if (occupied[r] || rng.nextFloat() >= ledgeChance) {
                r++
                continue
            }
            val innerW = WIDTH - left[r] - right[r]
            val roll = rng.nextInt(10)
            when {
                roll < 2 && r < CHUNK_ROWS - 2 && !occupied[r + 1] -> {
                    // Duvara yapışık 2×2 küme (iki satır); satır dar kalırsa o satır atlanır.
                    val onLeft = rng.nextBoolean()
                    var rows = 0
                    for (rr in r..r + 1) {
                        if (WIDTH - left[rr] - right[rr] - 2 < MIN_GAP) break
                        for (k in 0 until 2) {
                            val c = if (onLeft) left[rr] + k else WIDTH - right[rr] - 1 - k
                            set(rr, c, blockOrGem(rng))
                        }
                        occupied[rr] = true
                        rows++
                    }
                    r += if (rows > 0) rows + 1 else 1
                }
                roll < 7 -> {
                    // Duvara yapışık çıkıntı; karşı tarafta en az MIN_GAP boşluk kalır.
                    val maxLen = innerW - MIN_GAP
                    if (maxLen >= 1) {
                        val len = 1 + rng.nextInt(min(maxLen, 4))
                        val onLeft = rng.nextBoolean()
                        for (k in 0 until len) {
                            val c = if (onLeft) left[r] + k else WIDTH - right[r] - 1 - k
                            set(r, c, blockOrGem(rng))
                        }
                        occupied[r] = true
                        r += 2
                    } else {
                        r++
                    }
                }
                else -> {
                    // Yüzen platform: iki yanında en az 2, bir yanında en az MIN_GAP boşluk.
                    val maxLen = innerW - 2 - MIN_GAP
                    if (maxLen >= 1) {
                        val len = 1 + rng.nextInt(min(maxLen, 3))
                        val start = left[r] + 2 + rng.nextInt(innerW - 4 - len + 1)
                        for (k in 0 until len) set(r, start + k, blockOrGem(rng))
                        occupied[r] = true
                        r += 2
                    } else {
                        r++
                    }
                }
            }
        }
        if (index >= 1) niche(rng, tiles, left, right)
        return KuyuChunk(index, tiles, spawns(rng, index, tiles, left, right))
    }

    /**
     * Bekçi arenası: ince duvarlar, dönüşümlü kırılmaz çıkıntılar (2, 5, 8, 11.
     * satırlar), en altta kapı ve kapının üstünde salınan bekçi.
     */
    fun arena(index: Int): KuyuChunk {
        val tiles = Array(CHUNK_ROWS * WIDTH) { Tile.EMPTY }
        for (r in 0 until CHUNK_ROWS) {
            tiles[r * WIDTH] = Tile.WALL
            tiles[r * WIDTH + WIDTH - 1] = Tile.WALL
        }
        ARENA_LEDGE_ROWS.forEachIndexed { i, r ->
            val onLeft = i % 2 == 0
            for (k in 0 until 3) {
                val c = if (onLeft) 1 + k else WIDTH - 2 - k
                tiles[r * WIDTH + c] = Tile.WALL
            }
        }
        for (c in 1 until WIDTH - 1) tiles[(CHUNK_ROWS - 1) * WIDTH + c] = Tile.GATE
        val spawns = listOf(KuyuSpawn(EnemyKind.BOSS, index * CHUNK_ROWS + BOSS_ROW, WIDTH / 2))
        return KuyuChunk(index, tiles, spawns)
    }

    /**
     * Hazine oyuğu: kalınlığı 3 olan bir duvar bandının içine 2×2 boşluk, altında
     * sandık. Oyuncu içeri girip sandığın üstünden aşağı ateş ederek açar.
     */
    private fun niche(rng: Random, tiles: Array<Tile>, left: IntArray, right: IntArray) {
        if (rng.nextFloat() >= NICHE_CHANCE) return
        val onLeft = rng.nextBoolean()
        val candidates = (2 until CHUNK_ROWS - 3).filter { r ->
            r % BAND_ROWS <= 1 && (0..2).all { k -> (if (onLeft) left[r + k] else right[r + k]) == 3 }
        }
        if (candidates.isEmpty()) return
        val r = candidates[rng.nextInt(candidates.size)]
        val cols = if (onLeft) intArrayOf(1, 2) else intArrayOf(WIDTH - 3, WIDTH - 2)
        for (rr in r..r + 1) for (c in cols) tiles[rr * WIDTH + c] = Tile.EMPTY
        tiles[(r + 2) * WIDTH + (if (onLeft) 1 else WIDTH - 2)] = Tile.CHEST
    }

    private fun spawns(
        rng: Random,
        index: Int,
        tiles: Array<Tile>,
        left: IntArray,
        right: IntArray,
    ): List<KuyuSpawn> {
        fun get(r: Int, c: Int) = tiles[r * WIDTH + c]
        val count = if (index == 0) 1 else min(2 + index / 2, 7)
        val allowed = when (area(index)) {
            0 -> listOf(EnemyKind.BLOB, EnemyKind.BAT)
            1 -> listOf(EnemyKind.BLOB, EnemyKind.BAT, EnemyKind.SPIKY, EnemyKind.BLOB)
            else -> listOf(EnemyKind.BLOB, EnemyKind.BAT, EnemyKind.SPIKY, EnemyKind.CRAWLER)
        }
        val minRow = if (index == 0) START_ROW + 3 else 1
        val floorCells = ArrayList<Int>()
        val airCells = ArrayList<Int>()
        val wallCells = ArrayList<Int>()
        for (r in minRow until CHUNK_ROWS - 1) {
            for (c in 1 until WIDTH - 1) {
                if (get(r, c) != Tile.EMPTY) continue
                val code = r * WIDTH + c
                if (get(r + 1, c).solid) {
                    floorCells += code
                } else if (!get(r - 1, c).solid) {
                    airCells += code
                }
                val hugsWall = c == left[r] || c == WIDTH - 1 - right[r]
                if (hugsWall && r in 2 until CHUNK_ROWS - 2) wallCells += code
            }
        }
        val used = HashSet<Int>()
        val result = ArrayList<KuyuSpawn>()
        repeat(count) {
            var kind = allowed[rng.nextInt(allowed.size)]
            val pool = when (kind) {
                EnemyKind.BLOB, EnemyKind.SPIKY -> floorCells
                EnemyKind.BAT, EnemyKind.BOSS -> airCells // bekçi yalnız arenada doğar
                EnemyKind.CRAWLER -> wallCells
            }
            var candidates = pool.filter { it !in used }
            if (candidates.isEmpty()) {
                kind = EnemyKind.BAT
                candidates = airCells.filter { it !in used }
                if (candidates.isEmpty()) return@repeat
            }
            val cell = candidates[rng.nextInt(candidates.size)]
            used += cell
            result += KuyuSpawn(kind, index * CHUNK_ROWS + cell / WIDTH, cell % WIDTH)
        }
        return result
    }
}
