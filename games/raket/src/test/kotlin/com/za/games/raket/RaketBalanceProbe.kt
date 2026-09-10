package com.za.games.raket

import org.junit.Test

/**
 * Denge ölçümü (geçme/kalma yok): üç oyuncu botu üç bilgisayar seviyesine
 * karşı. Koşmak için: ./gradlew :games:raket:probe
 * Protokol ve yorum: docs/oyun-testi.md (D).
 */
class RaketBalanceProbe {

    @Test
    fun aiReport() {
        val seeds = 1L..30L
        println("Raket bilgisayar seviyeleri · ${seeds.count()} maç / hücre · orta bot = 1,4 birim/s, 0,14 s, hata 0,28")
        println("bot      seviye  bot galibiyeti  ort. skor  ort. süre  ort. en uzun ralli  ort. ralli/sayı")
        for (params in RaketBots.ALL) {
            for (level in 0..RaketWorld.MAX_LEVEL) {
                val results = seeds.map { RaketBots.match(it, level, params) }
                val wins = results.count { it.winner == Side.BOTTOM }
                val unfinished = results.count { it.winner == null }
                val avgBottom = results.map { it.bottom }.average()
                val avgTop = results.map { it.top }.average()
                val avgSeconds = results.map { it.frames / 60.0 }.average()
                val avgRally = results.map { it.bestRally }.average()
                val perPoint = results.map { it.hits.toDouble() / (it.bottom + it.top).coerceAtLeast(1) }.average()
                println(
                    String.format(
                        java.util.Locale.ROOT,
                        "%-8s %-7s %3d%% %s  %4.1f-%-4.1f  %5.0f s  %5.1f  %5.1f",
                        params.name, level, wins * 100 / seeds.count(),
                        if (unfinished > 0) "($unfinished bitmedi)" else "           ",
                        avgBottom, avgTop, avgSeconds, avgRally, perPoint,
                    ),
                )
            }
        }
    }
}
