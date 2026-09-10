package com.za.games.bostan

import kotlin.random.Random

/**
 * Dalga üretici. Dalga [w]'nin bütçesi
 * `zorluk.budget × (0.6 + 0.4 w) × (büyükse 1.5) × scale`; bütçe bitene dek
 * dalga dizinine göre açılmış türlerden ([unlockAt]) ağırlıklı seçim
 * yapılır, doğumlar 1 s'den başlayıp rastgele aralıklarla sıralanır (büyük
 * dalgada daha sık). Her üçüncü dalga büyüktür.
 *
 * Üretilen seviye [BostanExpert] ile oynatılır; uzman kaybederse bütçe
 * [SCALE_STEP] ile küçültülüp yeniden üretilir ([SCALES] sırası). Hiçbir
 * ölçekte kazanamazsa en küçük ölçekli seviye döner. Aynı tohum + zorluk =
 * aynı seviye.
 */
object BostanGenerator {
    const val SCALE_STEP = 0.85f
    val SCALES: List<Float> = List(6) { i -> var s = 1f; repeat(i) { s *= SCALE_STEP }; s }

    val unlockAt: Map<EnemyKind, Int> = mapOf(
        EnemyKind.KARGA to 0,
        EnemyKind.TAVSAN to 1,
        EnemyKind.KECI to 2,
        EnemyKind.DOMUZ to 3,
        EnemyKind.AYI to 5,
    )
    private val weights: Map<EnemyKind, Float> = mapOf(
        EnemyKind.KARGA to 4f,
        EnemyKind.TAVSAN to 3f,
        EnemyKind.KECI to 3f,
        EnemyKind.DOMUZ to 2f,
        EnemyKind.AYI to 1f,
    )

    fun budget(difficulty: BostanDifficulty, w: Int, scale: Float): Float =
        difficulty.budget * (0.6f + 0.4f * w) * (if (isBig(w)) 1.5f else 1f) * scale

    fun isBig(w: Int): Boolean = w % 3 == 2

    fun generate(seed: Long, difficulty: BostanDifficulty): BostanLevel {
        var last: BostanLevel? = null
        for (scale in SCALES) {
            val waves = waves(seed, difficulty, scale)
            val r = BostanExpert.play(BostanLevel(seed, difficulty, waves, scale, 0, 0))
            val level = BostanLevel(seed, difficulty, waves, scale, r.lives, r.score)
            if (r.status == BostanStatus.WON) return level
            last = level
        }
        return last!!
    }

    fun waves(seed: Long, difficulty: BostanDifficulty, scale: Float): List<Wave> {
        val rng = Random(BostanState.mix(seed, 0x57, difficulty.ordinal))
        return List(difficulty.waves) { w -> wave(rng, difficulty, w, scale) }
    }

    private fun wave(rng: Random, difficulty: BostanDifficulty, w: Int, scale: Float): Wave {
        val big = isBig(w)
        var budget = budget(difficulty, w, scale)
        val allowed = EnemyKind.entries.filter { unlockAt.getValue(it) <= w }
        val spawns = ArrayList<Spawn>()
        var at = 1f
        var lane = rng.nextInt(BostanState.COLS)
        while (true) {
            val pool = allowed.filter { it.cost <= budget }
            if (pool.isEmpty()) break
            val kind = pick(rng, pool)
            budget -= kind.cost
            if (rng.nextFloat() < 0.7f) lane = (lane + 1 + rng.nextInt(BostanState.COLS - 1)) % BostanState.COLS
            spawns += Spawn(at, lane, kind)
            at += if (big) 0.8f + rng.nextFloat() * 1.4f else 1.4f + rng.nextFloat() * 2f
        }
        return Wave(spawns, big)
    }

    private fun pick(rng: Random, pool: List<EnemyKind>): EnemyKind {
        val total = pool.sumOf { weights.getValue(it).toDouble() }.toFloat()
        var r = rng.nextFloat() * total
        for (k in pool) {
            r -= weights.getValue(k)
            if (r <= 0f) return k
        }
        return pool.last()
    }
}
