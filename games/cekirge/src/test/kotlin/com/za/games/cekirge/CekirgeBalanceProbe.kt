package com.za.games.cekirge

import org.junit.Test

/**
 * Denge ölçümü (geçme/kalma yok): üç pilot 20 tohumda oynar; skor, dalga,
 * vuruş, süre ve bitiş nedeni. Koşmak için: ./gradlew :games:cekirge:probe
 */
class CekirgeBalanceProbe {

    @Test
    fun swarmReport() {
        val seeds = 1L..20L
        println("Çekirge · ${seeds.count()} koşu / pilot · sürü ${CekirgeWorld.COLS}×${CekirgeWorld.ROWS}, hız ${CekirgeWorld.SWARM_SPEED0} (+%${(CekirgeWorld.SWARM_SPEED_WAVE * 100).toInt()}/dalga, seyrelince ×${1 + CekirgeWorld.SWARM_SPEED_THIN.toInt()}), tükürük aralığı ${CekirgeWorld.SPIT_INTERVAL0}→${CekirgeWorld.SPIT_INTERVAL_MIN} s")
        println("pilot   tepki  hız   nişan  ort. skor  ort. dalga  ort. vuruş  ort. süre  istila  can  sağ(240 s)")
        for ((name, p) in listOf("acemi" to CekirgeBots.Pilot(0.4f, 0.6f, 0.03f), "orta" to CekirgeBots.Pilot(0.25f, 0.8f, 0.02f), "uzman" to CekirgeBots.Pilot(0.12f, 1f, 0.015f))) {
            val runs = seeds.map { CekirgeBots.play(it, p) }
            println(
                String.format(
                    java.util.Locale.ROOT,
                    "%-7s %4.2f  %4.2f  %5.3f  %9.0f  %10.2f  %10.1f  %7.0f s  %6d  %3d  %10d",
                    name, p.reaction, p.speed, p.aim, runs.map { it.score }.average(), runs.map { it.wave }.average(),
                    runs.map { it.kills }.average(), runs.map { it.frames / 60.0 }.average(),
                    runs.count { it.invaded }, runs.count { it.over && !it.invaded }, runs.count { !it.over },
                ),
            )
        }
    }
}
