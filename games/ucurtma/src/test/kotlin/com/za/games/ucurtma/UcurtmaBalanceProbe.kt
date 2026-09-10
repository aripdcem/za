package com.za.games.ucurtma

import org.junit.Test

/**
 * Denge ölçümü (geçme/kalma yok): üç pilot 20 tohumda uçar; mesafe, skor ve
 * çarpma türleri. Koşmak için: ./gradlew :games:ucurtma:probe
 */
class UcurtmaBalanceProbe {

    @Test
    fun flightReport() {
        val seeds = 1L..20L
        val pilots = listOf("acemi" to 0.3f, "orta" to 0.18f, "uzman" to 0.08f)
        println("Uçurtma · ${seeds.count()} uçuş / pilot · hız ${UcurtmaWorld.BASE_SPEED}→${UcurtmaWorld.MAX_SPEED}, zorluk tavanı ${UcurtmaWorld.DIFF_METERS.toInt()} m")
        println("pilot   tepki   ort. m  en az  en çok  ort. skor  çatı  tel  ip  süre doldu")
        for ((name, reaction) in pilots) {
            val flights = seeds.map { UcurtmaBots.fly(it, UcurtmaBots.Pilot(reaction)) }
            val m = flights.map { it.meters }
            println(
                String.format(
                    java.util.Locale.ROOT,
                    "%-7s %4.2f  %7.0f  %5d  %6d  %9.0f  %4d %4d %3d  %3d",
                    name, reaction, m.average(), m.min(), m.max(), flights.map { it.score }.average(),
                    flights.count { it.crash == CrashKind.ROOF }, flights.count { it.crash == CrashKind.WIRE },
                    flights.count { it.crash == CrashKind.STRING }, flights.count { it.crash == null },
                ),
            )
        }
        for (g in Gadget.entries) {
            val flights = seeds.map { UcurtmaBots.fly(it, UcurtmaBots.Pilot(0.18f), gadget = g) }
            println(String.format(java.util.Locale.ROOT, "orta + %-6s ort. %5.0f m, skor %5.0f", g.name.lowercase(), flights.map { it.meters }.average(), flights.map { it.score }.average()))
        }
    }
}
