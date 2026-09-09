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
     * Tahminsiz çözücü artık ana kaynakta ([MinesSolver]) ve üretim tarafından
     * kullanılıyor; ölçüm aynı çözücüyle kaç tahtanın tahmin gerektirdiğini
     * ve üretimin ne kadar sürdüğünü sayar.
     */
    private fun coz(bas: MinesState, ilk: Int): Sonuc {
        var s = bas.reveal(ilk)
        var tahmin = 0
        while (s.status == MinesStatus.RUNNING) {
            val guvenli = MinesSolver.certainlySafe(s)
            if (guvenli.isNotEmpty()) {
                for (i in guvenli) {
                    if (s.status != MinesStatus.RUNNING) break
                    if (i !in s.revealed) s = s.reveal(i)
                }
                continue
            }
            val gizli = (0 until s.cellCount).filter { it !in s.revealed }
            if (gizli.isEmpty()) break
            tahmin++
            val sansli = gizli.firstOrNull { it !in s.mines } ?: break
            s = s.reveal(sansli)
        }
        return Sonuc(s.status == MinesStatus.WON, tahmin, s.revealed.size)
    }

    @Test
    fun noGuessFairness() {
        println("\n=== TAHMİNSİZ ÇÖZÜLEBİLİRLİK ===")
        println("ilk tık hep güvenli (mayınlar sonra, komşular hariç yerleşiyor);")
        println("ölçüm, kalan tahtanın mantıkla çözülüp çözülemediğine bakıyor")
        println()
        println("zorluk | tahta | mayın | yoğunluk | tahminsiz biten | ort. tahmin | en kötü | üretim ms")
        for (zorluk in MinesDifficulty.entries) {
            var temiz = 0
            var toplamTahmin = 0
            var enKotu = 0
            val n = 60
            var uretimNs = 0L
            for (tohum in 1L..n.toLong()) {
                val s = MinesState.newGame(zorluk, tohum)
                val ilk = s.cellCount / 2
                val t0 = System.nanoTime()
                val hazir = s.reveal(ilk)          // üretim ilk tıkta olur
                uretimNs += System.nanoTime() - t0
                if (hazir.status == MinesStatus.LOST) continue
                val r = coz(s, ilk)
                if (r.tahmin == 0) temiz++
                toplamTahmin += r.tahmin
                if (r.tahmin > enKotu) enKotu = r.tahmin
            }
            val yogunluk = 100f * zorluk.mineCount / (zorluk.width * zorluk.height)
            println(
                "${zorluk.name.padEnd(7)}| ${zorluk.width}×${zorluk.height} |  ${"%3d".format(zorluk.mineCount)}  " +
                    "|   %${"%.0f".format(yogunluk)}   |      %${"%3d".format(100 * temiz / n)}       " +
                    "|    ${"%.2f".format(toplamTahmin.toFloat() / n)}     |   $enKotu    |   ${"%5.1f".format(uretimNs / 1e6 / n)}",
            )
        }
        println()
        println("Tahmin sayısı 0 değilse oyuncu o tahtada bir noktada kör seçim yapmak")
        println("zorunda kalıyor: kaybı beceriyle önlenemez. Tahtayı üretirken tahminsiz")
        println("çözülebilirlik aranırsa bu oran sıfıra iner (üretim maliyeti karşılığında).")
    }
}
