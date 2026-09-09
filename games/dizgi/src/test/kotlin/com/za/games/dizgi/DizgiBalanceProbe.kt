package com.za.games.dizgi

import org.junit.Test
import kotlin.random.Random

/**
 * Dizgi denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:dizgi:probe` ile koşar.
 *
 * Dizgi'de adillik harf torbasından gelir: torba sözlüğe uymuyorsa oyuncu
 * oynanamaz ellerle kalır. Ölçümün soruları: torbadaki sesli/sessiz dengesi
 * sözlükle uyumlu mu, ve rastgele bir el kaç kez kelime kuramıyor?
 */
class DizgiBalanceProbe {

    private val sozluk = DizgiWords.valid
    private val sesliler = setOf('a', 'e', 'ı', 'i', 'o', 'ö', 'u', 'ü')

    @Test
    fun bagComposition() {
        println("\n=== TORBA BİLEŞİMİ ===")
        val torba = DizgiLetters.bag()
        val harfler = torba.map { it.letter }
        val joker = harfler.count { it == DizgiLetters.JOKER }
        val sesliSayisi = harfler.count { it in sesliler }
        val sessizSayisi = harfler.size - sesliSayisi - joker
        println("toplam taş: ${harfler.size}  (joker $joker)")
        println("sesli: $sesliSayisi (%${100 * sesliSayisi / harfler.size})   sessiz: $sessizSayisi (%${100 * sessizSayisi / harfler.size})")
        println()

        // Sözlükteki harf sıklığıyla karşılaştır.
        val sozlukSayim = HashMap<Char, Int>()
        var toplamHarf = 0
        for (k in sozluk) for (c in k) {
            sozlukSayim[c] = (sozlukSayim[c] ?: 0) + 1
            toplamHarf++
        }
        val torbaSayim = harfler.filter { it != DizgiLetters.JOKER }.groupingBy { it }.eachCount()
        val torbaToplam = torbaSayim.values.sum()
        println("harf | torba % | sözlük % | fark")
        var enBuyukFark = 0.0
        for (c in sozlukSayim.keys.sortedBy { -(sozlukSayim[it] ?: 0) }.take(12)) {
            val t = 100.0 * (torbaSayim[c] ?: 0) / torbaToplam
            val s = 100.0 * (sozlukSayim[c] ?: 0) / toplamHarf
            if (Math.abs(t - s) > enBuyukFark) enBuyukFark = Math.abs(t - s)
            println("  $c  |  ${"%5.2f".format(t)}  |  ${"%5.2f".format(s)}  | ${"%+5.2f".format(t - s)}")
        }
        println()
        println("en büyük sapma (ilk 12 harf): ${"%.2f".format(enBuyukFark)} puan")
        println("Torba sözlük sıklığından türetildiği için sapmanın küçük kalması beklenir.")
    }

    @Test
    fun rackPlayability() {
        println("\n=== EL OYNANABİLİRLİĞİ ===")
        println("rastgele çekilen ${DizgiState.RACK_SIZE} taşla en az bir kelime kurulabiliyor mu?")
        println("(tahtaya bağlanma aranmadan, yalnızca eldeki harflerle)")
        println()
        val torba = DizgiLetters.bag()
        var oynanabilir = 0
        var toplamSecenek = 0
        val n = 400
        val rng = Random(7)
        for (deneme in 1..n) {
            val el = torba.shuffled(rng).take(DizgiState.RACK_SIZE).map { it.letter }
            val stok = HashMap<Char, Int>()
            var jokerSayisi = 0
            for (c in el) if (c == DizgiLetters.JOKER) jokerSayisi++ else stok[c] = (stok[c] ?: 0) + 1
            var secenek = 0
            for (kelime in sozluk) {
                if (kelime.length > DizgiState.RACK_SIZE) continue
                val gerek = HashMap<Char, Int>()
                for (c in kelime) gerek[c] = (gerek[c] ?: 0) + 1
                var eksik = 0
                for ((c, adet) in gerek) eksik += maxOf(0, adet - (stok[c] ?: 0))
                if (eksik <= jokerSayisi) secenek++
                if (secenek > 0 && deneme > 40) break     // ilk 40 denemede say, sonra yalnız varlık
            }
            if (secenek > 0) oynanabilir++
            if (deneme <= 40) toplamSecenek += secenek
        }
        println("oynanabilir el: $oynanabilir / $n (%${100 * oynanabilir / n})")
        println("ilk 40 elde ortalama seçenek: ${toplamSecenek / 40}")
        println()
        println("Oynanamaz el oranı yüksekse oyuncu sık sık pas geçer ya da taş")
        println("değiştirir; torbadaki sesli oranı gözden geçirilmeli.")
    }
}
