package com.za.games.raket

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Raket süren bot. Üç kısıt insanı taklit eder: raket hızı [speed] ile
 * sınırlıdır, hedef ancak [reaction] saniyede bir (o andaki top bilgisiyle)
 * güncellenir ve her yaklaşımda normal dağılımlı bir nişan hatası yapılır.
 * Hatanın ölçüsü raketin yarı genişliğinin [error] katıdır ve top hızlandıkça
 * [ERROR_SPEED] katına kadar büyür: yavaş topu herkes karşılar, tavan hızda
 * orta bir raket üç dönüşten birini kaçırır. Hata hızla büyümezse iki
 * tahmin eden raket sonsuza dek ralli yapar (ölçüm: docs/oyun-testi.md, D).
 * [predicts] açıksa topun duvarlardan sekerek varacağı yer hesaplanır;
 * kapalıysa topun o anki x'i izlenir (top çaprazdan gelince raket geç kalır).
 * [edgeAims] açıksa bot bazen topu bilerek raketin kenarıyla, rakibin uzağına
 * doğru vurur; tavan hızda bu riskli bir vuruştur.
 *
 * Aynı sınıf testlerde alt raketi süren "oyuncu botu" olarak da kullanılır;
 * böylece seviyeler ölçülebilir (docs/oyun-testi.md, D).
 */
class RaketAi(
    val side: Side,
    val speed: Float,
    val reaction: Float,
    val error: Float,
    val predicts: Boolean,
    val edgeAims: Boolean,
    private val rng: Random,
) {
    private var timer = 0f
    private var target = RaketWorld.WIDTH / 2f
    private var offset = 0f
    private var approaching = false

    /** Her adımda, top hareket etmeden önce çağrılır; raketi hedefe doğru sürer. */
    fun drive(world: RaketWorld) {
        val me = world.paddle(side)
        val ball = world.ball
        val coming = world.status == RaketStatus.RALLY && (if (side == Side.TOP) ball.vy < 0f else ball.vy > 0f)
        if (coming && !approaching) {
            val half = me.width / 2f
            val sigma = error * half * (1f + ERROR_SPEED * world.hud().speed)
            offset = gaussian().coerceIn(-3f, 3f) * sigma
            if (edgeAims && rng.nextFloat() < EDGE_CHANCE) {
                val other = world.paddle(if (side == Side.TOP) Side.BOTTOM else Side.TOP)
                // Rakip soldaysa top sağa gitsin: top raketin sağ yarısına gelsin diye
                // raket merkezi tahminin soluna alınır.
                val dir = if (other.x < RaketWorld.WIDTH / 2f) 1f else -1f
                offset -= dir * EDGE_OFFSET * half
            }
            timer = 0f
        }
        approaching = coming
        timer -= RaketWorld.STEP
        if (timer <= 0f) {
            timer = reaction
            target = when {
                !coming -> RaketWorld.WIDTH / 2f
                predicts -> world.predictX(world.contactY(side)) + offset
                else -> ball.x + offset
            }
        }
        val maxStep = speed * RaketWorld.STEP
        world.shift(me, (target - me.x).coerceIn(-maxStep, maxStep))
    }

    /** Box–Muller; ortalama 0, sapma 1. */
    private fun gaussian(): Float {
        val u1 = rng.nextFloat().coerceAtLeast(1e-6f)
        val u2 = rng.nextFloat()
        return (sqrt(-2f * ln(u1)) * cos(2f * PI.toFloat() * u2))
    }

    companion object {
        const val EDGE_CHANCE = 0.6f
        const val EDGE_OFFSET = 0.45f

        /** Tavan hızda nişan hatası taban hatanın (1 + bu) katı. */
        const val ERROR_SPEED = 3.5f

        /** Seviye 0 kolay, 1 orta, 2 zor. */
        val SPEEDS = floatArrayOf(0.8f, 1.4f, 2.0f)
        val REACTIONS = floatArrayOf(0.28f, 0.15f, 0.07f)
        val ERRORS = floatArrayOf(0.7f, 0.3f, 0.12f)

        fun forLevel(level: Int, side: Side, rng: Random): RaketAi {
            val l = level.coerceIn(0, RaketWorld.MAX_LEVEL)
            return RaketAi(side, SPEEDS[l], REACTIONS[l], ERRORS[l], predicts = l >= 1, edgeAims = l >= 2, rng = rng)
        }
    }
}
