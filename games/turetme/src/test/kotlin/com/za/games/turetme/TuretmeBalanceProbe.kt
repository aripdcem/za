package com.za.games.turetme

import org.junit.Test

/**
 * Türetme denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:turetme:probe` ile koşar.
 *
 * Türetme'de oyuncu bir taban kelimenin harflerinden başka kelimeler üretir.
 * Adillik sorusu iki yanlı: taban **çok az** kelime veriyorsa tur sönük geçer,
 * **çok fazla** veriyorsa tamamlamak imkânsızlaşır. Ölçüm hedef sayısının
 * dağılımına ve uç tabanlara bakar.
 */
class TuretmeBalanceProbe {

    private val tabanlar = TuretmeWords.bases
    private val gecerli = TuretmeWords.valid

    @Test
    fun targetCounts() {
        println("\n=== HEDEF SAYISI DAĞILIMI ===")
        println("taban sayısı: ${tabanlar.size}   ·   geçerli kelime: ${gecerli.size}")
        println("en kısa hedef: ${TuretmeState.MIN_LENGTH} harf")
        println()
        val sayilar = tabanlar.map { TuretmeState.targetsFor(it, gecerli).size }.sorted()
        fun yuzdelik(p: Double) = sayilar[(sayilar.size * p).toInt().coerceIn(0, sayilar.size - 1)]
        println("en az | %10 | ortanca | %90 | en çok | ortalama")
        println(
            "${"%5d".format(sayilar.first())} | ${"%3d".format(yuzdelik(0.10))}  |   ${"%3d".format(yuzdelik(0.50))}   " +
                "| ${"%3d".format(yuzdelik(0.90))} |  ${"%4d".format(sayilar.last())}  |  ${"%.1f".format(sayilar.average())}",
        )
        println()
        val azlar = tabanlar.filter { TuretmeState.targetsFor(it, gecerli).size < 5 }
        val coklar = tabanlar.filter { TuretmeState.targetsFor(it, gecerli).size > 60 }
        println("5'ten az hedefi olan taban: ${azlar.size} (%${100 * azlar.size / tabanlar.size})")
        if (azlar.isNotEmpty()) println("  örnek: ${azlar.take(8).joinToString(", ")}")
        println("60'tan çok hedefi olan taban: ${coklar.size} (%${100 * coklar.size / tabanlar.size})")
        if (coklar.isNotEmpty()) println("  örnek: ${coklar.take(8).joinToString(", ")}")
        println()
        println("Az hedefli taban turu sönükleştirir; çok hedefli taban tamamlamayı")
        println("(COMPLETION_BONUS) ulaşılmaz kılar. İkisi de aşırı değilse denge iyi.")
    }

    @Test
    fun baseSanity() {
        println("\n=== TABAN SAĞLAMLIĞI ===")
        val gecersizTaban = tabanlar.filterNot { it in gecerli }
        val kendisiYok = tabanlar.filterNot { TuretmeState.targetsFor(it, gecerli).contains(it) }
        val uzunluklar = tabanlar.groupingBy { it.length }.eachCount()
        println("taban uzunlukları: " + uzunluklar.keys.sorted().joinToString(", ") { "$it→${uzunluklar[it]}" })
        println("geçerli kelime listesinde olmayan taban: ${gecersizTaban.size}")
        println("kendi hedef kümesinde bulunmayan taban: ${kendisiYok.size}")
        println()
        println("Taban kendi hedefleri arasında olmalı (BASE_BONUS onu ödüllendiriyor).")

        // Günlük döngü.
        val gorulen = HashSet<String>()
        for (gun in 0L until tabanlar.size.toLong()) {
            gorulen += TuretmeState.daily(tabanlar, gecerli, gun).base
        }
        println()
        println("${tabanlar.size} günde görülen farklı taban: ${gorulen.size}")
        println(if (gorulen.size == tabanlar.size) "Havuz tükenmeden tekrar yok." else "UYARI: taban erken tekrarlıyor.")
    }
}
