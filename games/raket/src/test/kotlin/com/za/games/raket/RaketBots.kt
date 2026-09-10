package com.za.games.raket

import kotlin.random.Random

/** Bir maçın özeti; [winner] süre dolduysa null. */
data class MatchResult(val winner: Side?, val bottom: Int, val top: Int, val frames: Int, val bestRally: Int, val hits: Int)

/**
 * Alt raketi süren oyuncu botları. Bilgisayar seviyeleriyle aynı sınıf,
 * farklı ayarlar: böylece "orta bir oyuncu kolayı yener, zora yenilir" gibi
 * iddialar ölçülebilir ve değişmez teste çevrilebilir (docs/oyun-testi.md, D).
 */
object RaketBots {

    class Params(val name: String, val speed: Float, val reaction: Float, val error: Float, val predicts: Boolean, val edgeAims: Boolean = false) {
        fun bot(rng: Random): RaketAi = RaketAi(Side.BOTTOM, speed, reaction, error, predicts, edgeAims, rng)
    }

    /** Topu izler, tahmin etmez; yavaş ve geç. */
    val WEAK = Params("zayıf", 0.95f, 0.26f, 0.55f, predicts = false)

    /** Orta seviye bir insan: tahmin eder, ara sıra ıskalar. */
    val MEDIUM = Params("orta", 1.4f, 0.14f, 0.28f, predicts = true)

    /** Çok hızlı ve isabetli. */
    val STRONG = Params("güçlü", 2.4f, 0.05f, 0.08f, predicts = true)

    val ALL = listOf(WEAK, MEDIUM, STRONG)

    fun match(seed: Long, level: Int, params: Params, maxFrames: Int = 60 * 600): MatchResult {
        val w = RaketWorld(seed, RaketMode.SOLO, level)
        val bot = params.bot(Random(seed * 31 + 7))
        var frames = 0
        while (w.status != RaketStatus.OVER && frames < maxFrames) {
            bot.drive(w)
            w.step()
            frames++
        }
        return MatchResult(w.winner, w.scoreBottom, w.scoreTop, frames, w.bestRally, w.hits)
    }
}
