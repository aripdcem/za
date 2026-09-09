package com.za.games.kiskac

import com.za.games.besharf.BesHarfWords
import org.junit.Test

/**
 * Kıskaç denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:kiskac:probe` ile koşar.
 *
 * Kıskaç bir **ikili arama** oyunudur: her tahminde gizli kelimenin alfabetik
 * olarak önce mi sonra mı olduğu söylenir. Dolayısıyla adillik sorusu tam
 * olarak hesaplanabilir: [KiskacState.MAX_GUESSES] hak, arama uzayını ikiye
 * bölerek daraltmaya yetiyor mu?
 *
 * Önemli ayrım: gizli kelime **cevap havuzundan** (1684) seçiliyor, ama
 * oyuncunun ekranda gördüğü ve tahmin edebildiği liste **geçerli tahminler**
 * (7797). Oyuncu hangi kelimelerin cevap olabileceğini bilmediği için pratikte
 * geniş liste üzerinde arar; ölçüm iki durumu da veriyor.
 */
class KiskacBalanceProbe {

    private val cevaplar = BesHarfWords.answers.sortedWith(TurkishOrder::compare)
    private val tahminler = BesHarfWords.allowed.sortedWith(TurkishOrder::compare)

    /**
     * Kusursuz ikili arama: kalan aralığın ortasındaki kelimeyi tahmin eder.
     * Dönüş, cevabı bulmak için gereken tahmin sayısı.
     */
    private fun aramaDerinligi(uzay: List<String>, cevap: String): Int {
        var alt = 0
        var ust = uzay.size - 1
        var tahmin = 0
        while (alt <= ust) {
            tahmin++
            val orta = (alt + ust) / 2
            val cmp = TurkishOrder.compare(cevap, uzay[orta])
            when {
                cmp == 0 -> return tahmin
                cmp > 0 -> alt = orta + 1
                else -> ust = orta - 1
            }
        }
        return 99   // uzayda yok
    }

    @Test
    fun searchDepth() {
        println("\n=== İKİLİ ARAMA DERİNLİĞİ ===")
        println("hak: ${KiskacState.MAX_GUESSES} tahmin")
        println("cevap havuzu: ${cevaplar.size}   ·   geçerli tahmin listesi: ${tahminler.size}")
        println()
        for ((ad, uzay) in listOf("cevap havuzu" to cevaplar, "geçerli tahminler" to tahminler)) {
            val dagilim = HashMap<Int, Int>()
            var enKotu = 0
            for (c in cevaplar) {
                val n = aramaDerinligi(uzay, c)
                dagilim[n] = (dagilim[n] ?: 0) + 1
                if (n > enKotu) enKotu = n
            }
            val asan = dagilim.filterKeys { it > KiskacState.MAX_GUESSES }.values.sum()
            println("--- arama uzayı: $ad (${uzay.size} kelime) ---")
            println("teorik alt sınır: ${Math.ceil(Math.log(uzay.size + 1.0) / Math.log(2.0)).toInt()} tahmin")
            println("ölçülen en kötü: $enKotu tahmin")
            println("hakka sığmayan cevap: $asan / ${cevaplar.size} (%${100 * asan / cevaplar.size})")
            println("dağılım: " + dagilim.keys.sorted().joinToString(", ") { "$it→${dagilim[it]}" })
            println()
        }
        println("Oyuncu ekranda sıralı geçerli tahmin listesini gördüğü için doğal")
        println("strateji o liste üzerinde ikili aramadır. O uzayda hak yetmiyorsa,")
        println("oyuncunun cevabın 'yaygın kelime' olduğunu tahmin edip aramayı")
        println("daraltması gerekir — yani oyun ikili aramadan fazlasını istiyor.")
    }

    @Test
    fun dailyRotation() {
        println("\n=== GÜNLÜK DÖNGÜ ===")
        val gorulen = HashSet<String>()
        var tekrar = 0
        for (gun in 0L until cevaplar.size.toLong()) {
            val s = KiskacState.daily(BesHarfWords.answers, gun)
            if (!gorulen.add(s.answer)) tekrar++
        }
        println("havuz: ${cevaplar.size} kelime")
        println("${cevaplar.size} günde görülen farklı kelime: ${gorulen.size}")
        println("tekrar eden gün: $tekrar")
        println()
        println(if (tekrar == 0) "Havuz tükenmeden tekrar yok: ${cevaplar.size} günlük döngü."
                else "UYARI: kelime havuz tükenmeden tekrarlıyor.")
    }
}
