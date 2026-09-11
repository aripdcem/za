package com.za.games.cici

import org.junit.Test

/**
 * Denge ölçümü (geçme/kalma yok): üç pilot 20 tohumda uçar; skor, süre,
 * yakalanan ikramlar, can kayıpları ve hareketsizlikten giden puan.
 * Koşmak için: ./gradlew :games:cici:probe
 */
class CiciBalanceProbe {

    @Test
    fun flightReport() {
        val seeds = 1L..20L
        println("Cici · ${seeds.count()} uçuş / pilot · can ${CiciWorld.LIVES}, rampa ${CiciWorld.RAMP_TIME.toInt()} s, en çok 300 s")
        println("pilot   tepki  ort. skor  ort. süre  biten  yem/su/ballı  kedi/top  kayıp puan  en iyi seri")
        for ((name, reaction) in listOf("acemi" to 0.35f, "orta" to 0.2f, "uzman" to 0.1f)) {
            val flights = seeds.map { CiciBots.play(it, CiciBots.Pilot(reaction, seed = it)) }
            println(
                String.format(
                    java.util.Locale.ROOT,
                    "%-7s %4.2f  %9.0f  %7.0f s  %3d  %4.1f/%4.1f/%4.1f  %4.1f/%4.1f  %6.1f  %6.1f",
                    name, reaction, flights.map { it.score }.average(), flights.map { it.seconds }.average(), flights.count { it.over },
                    flights.map { it.caught[TreatKind.SEED] ?: 0 }.average(), flights.map { it.caught[TreatKind.WATER] ?: 0 }.average(),
                    flights.map { it.caught[TreatKind.HONEY] ?: 0 }.average(),
                    flights.map { f -> f.hits.count { it == HazardKind.CAT } }.average(), flights.map { f -> f.hits.count { it == HazardKind.BALL } }.average(),
                    flights.map { it.lostPoints }.average(), flights.map { it.bestStreak }.average(),
                ),
            )
        }
    }
}
