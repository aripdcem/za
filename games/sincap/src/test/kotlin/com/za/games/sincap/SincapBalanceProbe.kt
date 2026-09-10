package com.za.games.sincap

import org.junit.Test

/**
 * Denge ölçümü (geçme/kalma yok): üç pilot 20 tohumda tırmanır; yükseklik,
 * skor, fındık, süre ve ölüm nedenleri. Koşmak için: ./gradlew :games:sincap:probe
 */
class SincapBalanceProbe {

    @Test
    fun climbReport() {
        val seeds = 1L..20L
        println("Sincap · ${seeds.count()} tırmanış / pilot · erişim ${SincapWorld.REACH}, kuru dal ${SincapWorld.DRY_HOLD} s, kedi ${SincapWorld.CAT_SPEED0}→${SincapWorld.CAT_SPEED_MAX} basamak/s (${SincapWorld.CAT_RAMP.toInt()} basamakta), sonra +${SincapWorld.CAT_LATE_SLOPE}/${SincapWorld.CAT_RAMP.toInt()}")
        println("pilot   tepki  ort. yük.  en iyi  ort. skor  ort. fındık  ort. süre  basamak/s  nedenler")
        for ((name, reaction) in listOf("acemi" to 0.35f, "orta" to 0.2f, "uzman" to 0.1f)) {
            val runs = seeds.map { SincapBots.play(it, SincapBots.Pilot(reaction)) }
            println(
                String.format(
                    java.util.Locale.ROOT,
                    "%-7s %4.2f  %8.1f  %6d  %9.0f  %11.1f  %7.0f s  %9.2f  %s",
                    name, reaction, runs.map { it.height }.average(), runs.maxOf { it.height }, runs.map { it.score }.average(),
                    runs.map { it.nuts }.average(), runs.map { it.frames / 60.0 }.average(), runs.map { it.height * 60.0 / it.frames }.average(),
                    DeathCause.entries.joinToString(" ") { c -> "${c.name.lowercase()}=${runs.count { it.cause == c }}" } + " sağ=${runs.count { it.cause == null }}",
                ),
            )
        }
        println()
        println("Basamak dağılımı (tohum 1, 0–199): tür sayıları")
        val w = SincapWorld(1L)
        w.levelAt(200)
        val counts = HashMap<BranchKind, Int>()
        var crows = 0
        var single = 0
        for (i in 0 until 200) {
            val lv = w.levels[i]
            counts[lv.left] = (counts[lv.left] ?: 0) + 1
            counts[lv.right] = (counts[lv.right] ?: 0) + 1
            if (lv.crow) crows++
            if (!lv.left.present || !lv.right.present) single++
        }
        println("  " + BranchKind.entries.joinToString(" ") { "${it.name.lowercase()}=${counts[it] ?: 0}" } + " karga=$crows tek-dal=$single")
    }
}
