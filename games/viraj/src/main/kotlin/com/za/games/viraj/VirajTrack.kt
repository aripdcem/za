package com.za.games.viraj

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.random.Random

/**
 * Sonsuz pist üreteci: tohumdan deterministik, kesit kesit. Her kesit giriş,
 * tutuş ve çıkış evreleriyle yumuşatılmış bir viraj ve/veya tepedir; ilk
 * kesitler düzdür ve zorluk mesafeyle artar. Yol kenarı nesneleri, eşyalar
 * ve kontrol noktaları kesit üretilirken yerleştirilir.
 */
class VirajTrack(private val seed: Long) {

    private val segments = ArrayList<Segment>()
    private var nextSection = 0

    val size: Int get() = segments.size

    fun segment(index: Int): Segment {
        while (segments.size <= index) addSection(nextSection++)
        return segments[index]
    }

    private fun lastY(): Float = segments.lastOrNull()?.y ?: 0f

    private fun addSection(n: Int) {
        val rng = Random(mix(seed, n))
        val d = difficulty(segments.size)
        val len = 20 + rng.nextInt(25)
        val roll = rng.nextFloat()
        val gentle = n < 3
        val curve = when {
            gentle || roll < 0.25f -> 0f
            else -> (if (rng.nextBoolean()) 1f else -1f) * (1.5f + rng.nextFloat() * (1.5f + 3.5f * d))
        }
        val hill = if (!gentle && rng.nextFloat() < 0.45f) (rng.nextFloat() * 2f - 1f) * (12f + 30f * d) else 0f
        val first = segments.size
        addRoad(len, len, len, curve, hill)
        decorate(first, segments.size, rng, d, n)
    }

    /** Gordon tarzı yol ekleme: viraj girişte/çıkışta, yükseklik tüm kesitte yumuşar. */
    private fun addRoad(enter: Int, hold: Int, leave: Int, curve: Float, hill: Float) {
        val startY = lastY()
        val endY = startY + hill * VirajWorld.SEGMENT_LENGTH
        val total = enter + hold + leave
        for (i in 0 until enter) add(easeIn(0f, curve, i / enter.toFloat()), easeInOut(startY, endY, i / total.toFloat()))
        for (i in 0 until hold) add(curve, easeInOut(startY, endY, (enter + i) / total.toFloat()))
        for (i in 0 until leave) add(easeInOut(curve, 0f, i / leave.toFloat()), easeInOut(startY, endY, (enter + hold + i) / total.toFloat()))
    }

    private fun add(curve: Float, y: Float) {
        val index = segments.size
        segments.add(Segment(index, curve, y).also { it.checkpoint = index > 0 && index % VirajWorld.CHECKPOINT_EVERY == 0 })
    }

    /** Yol kenarı nesneleri ve eşyalar; kontrol noktası parçasına eşya konmaz. */
    private fun decorate(from: Int, to: Int, rng: Random, d: Float, section: Int) {
        for (i in from until to) {
            val seg = segments[i]
            if (rng.nextFloat() < 0.22f) {
                val kind = when (rng.nextInt(10)) {
                    0, 1, 2, 3 -> SpriteKind.TREE
                    4, 5 -> SpriteKind.BUSH
                    6 -> SpriteKind.BOULDER
                    7 -> SpriteKind.SIGN
                    else -> SpriteKind.POLE
                }
                val side = if (rng.nextBoolean()) 1f else -1f
                seg.sprites += Sprite(kind, side * (1.3f + rng.nextFloat() * 1.3f))
            }
            if (i % 30 == 0) seg.sprites += Sprite(SpriteKind.POLE, if ((i / 30) % 2 == 0) -1.15f else 1.15f)
        }
        if (section < 3) return
        val count = 1 + rng.nextInt(2 + (3f * d).toInt())
        repeat(count) {
            val seg = segments[from + rng.nextInt(to - from)]
            if (seg.checkpoint || seg.item != null) return@repeat
            val kind = when {
                rng.nextFloat() < 0.32f -> ItemKind.TURBO
                rng.nextFloat() < 0.45f -> ItemKind.CONE
                rng.nextFloat() < 0.55f -> ItemKind.OIL
                else -> ItemKind.BOX
            }
            seg.item = Item(kind, -0.6f + rng.nextFloat() * 1.2f)
        }
    }

    companion object {
        /** 0 → 1 arası zorluk; 6000. parçada (30 km) tavan. */
        fun difficulty(index: Int): Float = min(1f, index / 6000f)

        fun easeIn(a: Float, b: Float, p: Float): Float = a + (b - a) * p * p
        fun easeInOut(a: Float, b: Float, p: Float): Float = a + (b - a) * (-cos(p * PI.toFloat()) / 2f + 0.5f)

        fun mix(seed: Long, a: Int, b: Int = 0): Long {
            var z = seed xor (a.toLong() shl 32) xor b.toLong() xor -0x61C8864680B583EBL
            z = (z xor (z ushr 30)) * -0x40A7B892E31B1A47L
            z = (z xor (z ushr 27)) * -0x6B2FB644ECCEEE15L
            return z xor (z ushr 31)
        }

        /** İki nesnenin yanal çakışması; [percent] genişliklerin ne kadarı sayılır. */
        fun overlap(x1: Float, w1: Float, x2: Float, w2: Float, percent: Float = 1f): Boolean {
            val half = percent / 2f
            val min1 = x1 - w1 * half
            val max1 = x1 + w1 * half
            val min2 = x2 - w2 * half
            val max2 = x2 + w2 * half
            return !(max1 < min2 || min1 > max2)
        }

        internal fun near(a: Float, b: Float, eps: Float = 1e-3f): Boolean = abs(a - b) <= eps
    }
}
