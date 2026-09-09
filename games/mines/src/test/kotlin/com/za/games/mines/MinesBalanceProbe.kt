package com.za.games.mines

import org.junit.Test

/**
 * Mayın Tarlası denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:mines:probe` ile koşar.
 *
 * Sıra tabanlı oyunlarda soru tepki hızı değil **adillik**: tahta mantıkla
 * çözülebiliyor mu, yoksa oyuncu bir noktada kör tahmin yapmak zorunda mı
 * kalıyor? Mayınlar ilk tıktan sonra, tıklanan hücre ve komşuları hariç
 * yerleştiriliyor (ilk tık hep güvenli), ama tahtanın tamamının tahminsiz
 * çözülebileceği garanti edilmiyor. Ölçüm bunu sayıyor.
 */
class MinesBalanceProbe {

    /** Çözüm sonucu: tahminsiz bitti mi, kaç kez tahmine zorlandı. */
    private data class Sonuc(val bitti: Boolean, val tahmin: Int, val acilan: Int)

    /**
     * Tahminsiz çözücü. Önce basit çıkarım (bir sayının komşularındaki
     * mayınlar kesinse gerisi güvenli, ya da kalan gizliler tam sayıyı
     * tutuyorsa hepsi mayın). Takılınca sınır hücrelerinin tüm tutarlı
     * yerleşimleri sayılır: her yerleşimde mayınsız olan hücre kesin
     * güvenlidir. O da yoksa tahmin gerekmiştir.
     */
    private fun coz(bas: MinesState, ilk: Int): Sonuc {
        var s = bas.reveal(ilk)
        var tahmin = 0
        while (s.status == MinesStatus.RUNNING) {
            val guvenli = kesinGuvenli(s)
            if (guvenli.isNotEmpty()) {
                for (i in guvenli) {
                    if (s.status != MinesStatus.RUNNING) break
                    if (i !in s.revealed) s = s.reveal(i)
                }
                continue
            }
            // Mantıkla ilerlenemiyor: tahmin.
            val gizli = (0 until s.cellCount).filter { it !in s.revealed }
            if (gizli.isEmpty()) break
            tahmin++
            // Tahmini "şanslı" say: mayın olmayan bir hücre seç ki ölçüm
            // tahtanın tamamını taramaya devam etsin.
            val sansli = gizli.firstOrNull { it !in s.mines } ?: break
            s = s.reveal(sansli)
        }
        return Sonuc(s.status == MinesStatus.WON, tahmin, s.revealed.size)
    }

    /** Basit çıkarım + sınır sayımıyla kesin güvenli bulunan hücreler. */
    private fun kesinGuvenli(s: MinesState): Set<Int> {
        val gizli = { i: Int -> i !in s.revealed }
        val sonuc = HashSet<Int>()

        // 1) Basit çıkarım: sayı = çevredeki kesin mayın sayısı → gerisi güvenli.
        val kesinMayin = HashSet<Int>()
        var degisti = true
        while (degisti) {
            degisti = false
            for (i in s.revealed) {
                val n = s.neighbors(i)
                val gizliler = n.filter(gizli)
                if (gizliler.isEmpty()) continue
                val sayi = s.adjacentMines(i)
                val bilinen = gizliler.count { it in kesinMayin }
                val bilinmeyen = gizliler.filter { it !in kesinMayin }
                if (bilinmeyen.isEmpty()) continue
                if (sayi == bilinen) {
                    if (sonuc.addAll(bilinmeyen)) degisti = true
                } else if (sayi - bilinen == bilinmeyen.size) {
                    if (kesinMayin.addAll(bilinmeyen)) degisti = true
                }
            }
        }
        if (sonuc.isNotEmpty()) return sonuc

        // 2) Sınır sayımı. Sınır hücrelerinin tüm tutarlı yerleşimleri denenir;
        // her yerleşimde boş kalan hücre kesin güvenlidir. Sınırın tamamını tek
        // seferde saymak üstel patlar, bu yüzden ortak kısıt paylaşan hücreler
        // **bağımsız bileşenlere** ayrılıp her biri ayrı sayılır (standart
        // yöntem). Bileşenleri ayırmak toplam mayın sayısı kısıtını göz ardı
        // eder; bu yalnızca bazı çıkarımları kaçırabilir, yanlış "güvenli"
        // üretemez — yani ölçüm tahmin sayısını abartabilir, eksiltemez.
        val kisitlar = s.revealed
            .map { i -> s.neighbors(i).filter(gizli) to s.adjacentMines(i) }
            .filter { it.first.isNotEmpty() }
        if (kisitlar.isEmpty()) return emptySet()

        // Hücre → kısıt ilişkisinden bileşenleri çıkar (birleştir-bul).
        val ebeveyn = HashMap<Int, Int>()
        fun bul(a: Int): Int {
            var k = a
            while (ebeveyn[k] != k) { ebeveyn[k] = ebeveyn[ebeveyn[k]]!!; k = ebeveyn[k]!! }
            return k
        }
        for ((huc, _) in kisitlar) for (h in huc) ebeveyn.putIfAbsent(h, h)
        for ((huc, _) in kisitlar) {
            val ilk = bul(huc.first())
            for (h in huc.drop(1)) ebeveyn[bul(h)] = ilk
        }
        val bilesenler = ebeveyn.keys.groupBy { bul(it) }

        for ((_, hucreler) in bilesenler) {
            if (hucreler.size > 24) continue          // pratikte nadir; atlanır
            val sira = hucreler.toList()
            val yer = sira.withIndex().associate { (k, v) -> v to k }
            val ilgili = kisitlar.filter { (huc, _) -> huc.any { it in yer } }
            val yerlesim = BooleanArray(sira.size)
            val mayinOlabilir = BooleanArray(sira.size)
            var sayim = 0

            fun tutarli(k: Int): Boolean = ilgili.all { (huc, sayi) ->
                var kesin = 0
                var acik = 0
                for (h in huc) {
                    val j = yer[h]
                    if (j == null) acik++            // başka bileşende: serbest
                    else if (j < k) { if (yerlesim[j]) kesin++ } else acik++
                }
                kesin <= sayi && kesin + acik >= sayi
            }

            fun ara(k: Int) {
                if (sayim > 200_000) return
                if (k == sira.size) {
                    sayim++
                    for (j in sira.indices) if (yerlesim[j]) mayinOlabilir[j] = true
                    return
                }
                for (deger in listOf(false, true)) {
                    yerlesim[k] = deger
                    if (tutarli(k + 1)) ara(k + 1)
                }
                yerlesim[k] = false
            }
            ara(0)
            if (sayim == 0 || sayim > 200_000) continue
            for (j in sira.indices) if (!mayinOlabilir[j]) sonuc += sira[j]
        }
        return sonuc
    }

    @Test
    fun noGuessFairness() {
        println("\n=== TAHMİNSİZ ÇÖZÜLEBİLİRLİK ===")
        println("ilk tık hep güvenli (mayınlar sonra, komşular hariç yerleşiyor);")
        println("ölçüm, kalan tahtanın mantıkla çözülüp çözülemediğine bakıyor")
        println()
        println("zorluk | tahta | mayın | yoğunluk | tahminsiz biten | ort. tahmin | en kötü")
        for (zorluk in MinesDifficulty.entries) {
            var temiz = 0
            var toplamTahmin = 0
            var enKotu = 0
            val n = 60
            for (tohum in 1L..n.toLong()) {
                val s = MinesState.newGame(zorluk, tohum)
                val ilk = s.cellCount / 2
                val r = coz(s, ilk)
                if (r.tahmin == 0) temiz++
                toplamTahmin += r.tahmin
                if (r.tahmin > enKotu) enKotu = r.tahmin
            }
            val yogunluk = 100f * zorluk.mineCount / (zorluk.width * zorluk.height)
            println(
                "${zorluk.name.padEnd(7)}| ${zorluk.width}×${zorluk.height} |  ${"%3d".format(zorluk.mineCount)}  " +
                    "|   %${"%.0f".format(yogunluk)}   |      %${"%3d".format(100 * temiz / n)}       " +
                    "|    ${"%.2f".format(toplamTahmin.toFloat() / n)}     |   $enKotu",
            )
        }
        println()
        println("Tahmin sayısı 0 değilse oyuncu o tahtada bir noktada kör seçim yapmak")
        println("zorunda kalıyor: kaybı beceriyle önlenemez. Tahtayı üretirken tahminsiz")
        println("çözülebilirlik aranırsa bu oran sıfıra iner (üretim maliyeti karşılığında).")
    }
}
