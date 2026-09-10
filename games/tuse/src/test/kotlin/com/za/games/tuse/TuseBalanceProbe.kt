package com.za.games.tuse

import org.junit.Test

/**
 * Denge ölçümü (geçme/kalma yok): Sonsuz modda insan benzeri botlar — karoyu
 * gördükten [reaction] s sonra, saniyede en çok [cadence] dokunuşla vurur.
 * Hız karo başına arttığından koşu, botun temposu akış hızının altında
 * kalınca biter. Koşmak için: ./gradlew :games:tuse:probe
 */
class TuseBalanceProbe {

    private class Bot(val name: String, val reaction: Float, val cadence: Float)

    @Test
    fun arcadeReport() {
        val bots = listOf(Bot("acemi", 0.28f, 4.5f), Bot("orta", 0.18f, 7f), Bot("hızlı", 0.12f, 9.5f), Bot("uzman", 0.08f, 12f))
        val seeds = 1L..20L
        println("Tuşe Sonsuz · taban ${TuseWorld.ARCADE_BASE} satır/s, karo başına +${TuseWorld.ARCADE_GAIN}, tavan ${TuseWorld.ARCADE_MAX}")
        println("bot      tepki  tempo   ort. karo  en az  en çok  ort. süre")
        for (bot in bots) {
            val results = seeds.map { seed ->
                val w = TuseWorld(seed, TuseMode.ARCADE, Songs.ALL[(seed % Songs.ALL.size).toInt()])
                var seenAt = -1f
                var lastTap = -10f
                var t = 0f
                w.tap(w.lane(0), 0L)
                while (!w.isComplete && t < 600f) {
                    val i = w.tapped
                    val rowsAbove = i - w.scroll
                    if (rowsAbove < TuseWorld.ROWS && seenAt < 0f) seenAt = t
                    if (seenAt >= 0f && t - seenAt >= bot.reaction && t - lastTap >= 1f / bot.cadence) {
                        w.tap(w.lane(i), (t * 1000).toLong())
                        lastTap = t
                        seenAt = -1f
                    }
                    w.step()
                    t += TuseWorld.STEP
                }
                w.tapped to t
            }
            val tiles = results.map { it.first }
            println(
                String.format(
                    java.util.Locale.ROOT,
                    "%-8s %4.2f  %4.1f/s  %7.1f  %5d  %6d  %6.1f s",
                    bot.name, bot.reaction, bot.cadence, tiles.average(), tiles.min(), tiles.max(), results.map { it.second }.average(),
                ),
            )
        }
    }
}
