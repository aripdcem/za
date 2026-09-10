package com.za.games.bostan

import org.junit.Test

/**
 * Denge ölçümü (geçme/kalma yok): uzman politika her zorlukta 30 tohumu
 * önce ölçeksiz (1.0) oynar; sonra üreticinin doğruladığı seviyelerin ölçek
 * dağılımı. Koşmak için: ./gradlew :games:bostan:probe
 */
class BostanBalanceProbe {

    @Test
    fun expertReport() {
        val seeds = 1L..30L
        println("Bostan · ${seeds.count()} tohum / zorluk · hazırlık ${BostanState.PREP.toInt()} s, can ${BostanState.LIVES}")
        println("zorluk  dalga  ölçek 1.0: kazanma  ort. can  ort. süre  ort. saldırgan  ort. yerleşim  |  üretici: ölçek dağılımı (ort.)")
        for (d in BostanDifficulty.entries) {
            val raw = seeds.map { BostanExpert.play(BostanLevel(it, d, BostanGenerator.waves(it, d, 1f), 1f, 0, 0)) }
            val counts = seeds.map { BostanGenerator.waves(it, d, 1f).sumOf { w -> w.spawns.size } }
            val gen = seeds.map { BostanGenerator.generate(it, d) }
            val hist = gen.groupingBy { String.format(java.util.Locale.ROOT, "%.2f", it.scale) }.eachCount().toSortedMap(reverseOrder())
            println(
                String.format(
                    java.util.Locale.ROOT,
                    "%-6s  %5d  %16d%%  %8.2f  %7.0f s  %14.1f  %13.1f  |  %s (%.2f)",
                    d.name.lowercase(), d.waves, raw.count { it.status == BostanStatus.WON } * 100 / raw.size,
                    raw.map { it.lives }.average(), raw.map { it.time }.average(), counts.average(), raw.map { it.placed }.average(),
                    hist.entries.joinToString(" ") { "${it.key}×${it.value}" }, gen.map { it.scale }.average(),
                ),
            )
        }
        println()
        println("Dalga bütçeleri (ölçek 1.0):")
        for (d in BostanDifficulty.entries) {
            println("  " + d.name.lowercase() + ": " + (0 until d.waves).joinToString(" ") { w -> String.format(java.util.Locale.ROOT, "%.1f%s", BostanGenerator.budget(d, w, 1f), if (BostanGenerator.isBig(w)) "*" else "") })
        }
        println()
        println("Örnek seviye (zor, tohum 1):")
        val lv = BostanGenerator.generate(1L, BostanDifficulty.ZOR)
        for ((i, w) in lv.waves.withIndex()) {
            println(String.format(java.util.Locale.ROOT, "  dalga %2d%s %5.1f s: %s", i + 1, if (w.big) "*" else " ", w.duration, w.spawns.groupingBy { it.kind }.eachCount().entries.joinToString(" ") { "${it.key.name.lowercase()}×${it.value}" }))
        }
        println(String.format(java.util.Locale.ROOT, "  ölçek %.2f, uzman %d can, %d puan", lv.scale, lv.expertLives, lv.expertScore))
    }
}
