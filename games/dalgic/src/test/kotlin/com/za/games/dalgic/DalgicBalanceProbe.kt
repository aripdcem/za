package com.za.games.dalgic

import org.junit.Test

/**
 * Denge ölçümü (geçme/kalma yok): üç pilot 20 tohumda dalar; skor, dalga,
 * kurtarılan dalgıç ve süre. Koşmak için: ./gradlew :games:dalgic:probe
 */
class DalgicBalanceProbe {

    @Test
    fun diveReport() {
        val seeds = 1L..20L
        println("Dalgıç · ${seeds.count()} dalış / pilot · oksijen ${DalgicWorld.OXYGEN_MAX.toInt()} s, kapasite ${DalgicWorld.CAPACITY}, can ${DalgicWorld.LIVES}")
        println("pilot   tepki  ort. skor  ort. dalga  ort. dalgıç  ort. süre  biten  can kaybı nedenleri")
        for ((name, reaction) in listOf("acemi" to 0.35f, "orta" to 0.2f, "uzman" to 0.1f)) {
            val dives = seeds.map { DalgicBots.play(it, DalgicBots.Pilot(reaction)) }
            println(
                String.format(
                    java.util.Locale.ROOT,
                    "%-7s %4.2f  %9.0f  %10.1f  %11.1f  %7.0f s  %3d  %s",
                    name, reaction, dives.map { it.score }.average(), dives.map { it.wave }.average(),
                    dives.map { it.rescued }.average(), dives.map { it.frames / 60.0 }.average(), dives.count { it.over },
                    LifeCause.entries.joinToString(" ") { c -> "${c.name.lowercase()}=${dives.sumOf { d -> d.causes.count { it == c } }}" },
                ),
            )
        }
    }
}
