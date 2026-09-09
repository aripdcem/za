package com.za.games.besharf

import org.junit.Test

/**
 * Beş Harf denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:besharf:probe` ile koşar.
 *
 * Kelime oyunlarında adillik sorusu şudur: **her cevap hakla çözülebiliyor
 * mu?** Tuzak kelimeler (aynı deseni paylaşan kalabalık aileler) altı hakkı
 * yakabilir ve o gün herkes kaybeder. Ölçüm, makul oynayan bir çözücünün her
 * cevabı kaç tahminde bulduğuna bakar.
 */
class BesHarfBalanceProbe {

    private val cevaplar = BesHarfWords.answers

    /** Geri bildirim oyunun kendi kuralıyla üretilir (yinelenen harf dahil). */
    private fun geri(cevap: String, tahmin: String): List<LetterMark> =
        BesHarfState.mark(cevap, tahmin)

    /**
     * Makul oynayan çözücü: her adımda kalan adaylardan, en kötü durumda en çok
     * eleyeni seçer (minimaks). Adaylar kalabalıkken maliyeti sınırlamak için
     * örneklenir; bu çözücüyü yalnızca zayıflatır, yani sonuç bir üst sınırdır.
     */
    private fun tahminSayisi(cevap: String, acilis: String): Int {
        var adaylar = cevaplar
        var tahmin = acilis
        for (tur in 1..12) {
            if (tahmin == cevap) return tur
            val isaret = geri(cevap, tahmin)
            val onceki = tahmin
            adaylar = adaylar.filter { geri(it, onceki) == isaret }
            if (adaylar.isEmpty()) return 99
            tahmin = sec(adaylar)
        }
        return 99
    }

    /** Kalan adaylar arasından en kötü kovası en küçük olanı seç. */
    private fun sec(adaylar: List<String>): String {
        if (adaylar.size <= 2) return adaylar.first()
        val denenecek = if (adaylar.size > 120) adaylar.take(120) else adaylar
        var enIyi = denenecek.first()
        var enIyiKova = Int.MAX_VALUE
        for (aday in denenecek) {
            val kovalar = HashMap<List<LetterMark>, Int>()
            var enKotu = 0
            for (c in adaylar) {
                val k = kovalar.merge(geri(c, aday), 1, Int::plus)!!
                if (k > enKotu) enKotu = k
            }
            if (enKotu < enIyiKova) {
                enIyiKova = enKotu
                enIyi = aday
            }
        }
        return enIyi
    }

    @Test
    fun solvability() {
        println("\n=== ÇÖZÜLEBİLİRLİK ===")
        println("cevap havuzu ${cevaplar.size}, hak ${BesHarfState.MAX_GUESSES}")
        println("çözücü her adımda en kötü durumda en çok eleyeni seçer")
        println()
        // Havuzun tamamı yavaş; temsil eden bir örneklem taranır.
        val ornek = cevaplar.filterIndexed { i, _ -> i % 4 == 0 }
        val acilis = sec(cevaplar.take(400))
        println("seçilen açılış: $acilis   (örneklem ${ornek.size} cevap)")
        println()
        val dagilim = HashMap<Int, Int>()
        val zorlar = ArrayList<String>()
        for (c in ornek) {
            val n = tahminSayisi(c, acilis)
            dagilim[n] = (dagilim[n] ?: 0) + 1
            if (n > BesHarfState.MAX_GUESSES) zorlar += c
        }
        println("tahmin | cevap sayısı")
        for (n in dagilim.keys.sorted()) {
            val etiket = if (n == 99) "çözülemedi" else "$n"
            println("${etiket.padStart(6)} | ${dagilim[n]}")
        }
        val basarisiz = zorlar.size
        println()
        println("altı hakka sığmayan: $basarisiz / ${ornek.size} (%${100 * basarisiz / ornek.size})")
        if (zorlar.isNotEmpty()) {
            println("örnek zor cevaplar: ${zorlar.take(12).joinToString(", ")}")
        }
        println()
        println("Not: çözücü örneklemeyle zayıflatıldığı için bu sayı bir üst sınırdır;")
        println("kusursuz oyun daha azını ıskalar. Yine de sıfırdan büyük olması, o")
        println("cevapların denk geldiği günde kaybın kaçınılmaz olabileceğini gösterir.")
    }

    @Test
    fun answerPoolSanity() {
        println("\n=== CEVAP HAVUZU SAĞLAMLIĞI ===")
        val uzunlukHatasi = cevaplar.filter { it.length != BesHarfState.WORD_LENGTH }
        val izinsiz = cevaplar.filterNot { BesHarfWords.isAllowed(it) }
        val tekrar = cevaplar.groupingBy { it }.eachCount().filterValues { it > 1 }
        println("cevap sayısı: ${cevaplar.size}")
        println("geçerli tahmin kümesi: ${BesHarfWords.allowed.size}")
        println("yanlış uzunlukta cevap: ${uzunlukHatasi.size}")
        println("tahmin olarak kabul edilmeyen cevap: ${izinsiz.size}")
        println("havuzda tekrar eden cevap: ${tekrar.size}")
        println()
        println("Günlük kelime havuzun kalıcı bir permütasyonundan seçiliyor, yani")
        println("havuz tükenmeden tekrar gelmiyor: ${cevaplar.size} günlük döngü.")
    }
}
