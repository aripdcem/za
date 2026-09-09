package com.za.games.g2048

import org.junit.Test

/**
 * 2048 denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:g2048:probe` ile koşar.
 *
 * 2048'de zaman baskısı yok; denge sorusu **kazanılabilirlik**: makul oynayan
 * biri 2048'e ulaşabiliyor mu, yoksa taş doğuşu oyunu şansa mı bırakıyor?
 * Ayrıca doğuş dağılımı (%90 iki, %10 dört) gerçekten öyle mi?
 */
class G2048BalanceProbe {

    /**
     * Oyuncu modeli: yaygın 2048 sezgiseli + iki kat derinlik (expectimax).
     * Puanlama boş hücre, **tekdüzelik** (değerlerin bir yöne doğru azalması),
     * komşu uyumu ve en büyük taşın köşede olması üzerine kurulu. Basit
     * "en çok boş hücre" sezgiseli 256'da tıkanıyordu; tekdüzelik olmadan
     * ölçüm oyun hakkında değil sezgisel hakkında bilgi verir.
     */
    private fun puanla(s: G2048State): Double {
        val n = s.size
        val c = s.cells
        var bos = 0.0
        var enBuyuk = 0
        for (v in c) {
            if (v == 0) bos++
            if (v > enBuyuk) enBuyuk = v
        }
        fun log2(v: Int) = if (v <= 0) 0.0 else (Math.log(v.toDouble()) / Math.log(2.0))

        // Tekdüzelik: satır ve sütunlarda değerlerin tek yönde azalması ödüllenir.
        var tekduze = 0.0
        for (r in 0 until n) {
            var sol = 0.0
            var sag = 0.0
            for (i in 0 until n - 1) {
                val a = log2(c[r * n + i])
                val b = log2(c[r * n + i + 1])
                if (a > b) sol += b - a else sag += a - b
            }
            tekduze += maxOf(sol, sag)
        }
        for (col in 0 until n) {
            var yukari = 0.0
            var asagi = 0.0
            for (i in 0 until n - 1) {
                val a = log2(c[i * n + col])
                val b = log2(c[(i + 1) * n + col])
                if (a > b) yukari += b - a else asagi += a - b
            }
            tekduze += maxOf(yukari, asagi)
        }

        // Komşu uyumu (pürüzsüzlük).
        var puruz = 0.0
        for (r in 0 until n) {
            for (col in 0 until n) {
                val v = c[r * n + col]
                if (v == 0) continue
                if (col + 1 < n && c[r * n + col + 1] > 0) puruz -= Math.abs(log2(v) - log2(c[r * n + col + 1]))
                if (r + 1 < n && c[(r + 1) * n + col] > 0) puruz -= Math.abs(log2(v) - log2(c[(r + 1) * n + col]))
            }
        }

        val kose = if (c[0] == enBuyuk || c[n - 1] == enBuyuk ||
            c[n * n - n] == enBuyuk || c[n * n - 1] == enBuyuk
        ) log2(enBuyuk) * 3.0 else 0.0

        return bos * 2.7 + tekduze * 1.0 + puruz * 0.1 + kose
    }

    /** Hamleden sonra en kötü doğuşu varsayarak değer biçer (bir kat ileri). */
    private fun derinPuan(s: G2048State): Double {
        val bosIndeksler = s.cells.indices.filter { s.cells[it] == 0 }
        if (bosIndeksler.isEmpty()) return puanla(s)
        // Tümünü denemek pahalı; birkaç boş hücrenin en kötüsüne bakmak yeter.
        var enKotu = Double.MAX_VALUE
        for (i in bosIndeksler.take(6)) {
            val taklit = s.copy(cells = s.cells.toMutableList().also { it[i] = 4 })
            var enIyi = -Double.MAX_VALUE
            for (yon in MoveDir.entries) {
                val sonraki = taklit.move(yon)
                if (sonraki.cells == taklit.cells) continue
                val p = puanla(sonraki)
                if (p > enIyi) enIyi = p
            }
            if (enIyi == -Double.MAX_VALUE) enIyi = puanla(taklit) - 100.0
            if (enIyi < enKotu) enKotu = enIyi
        }
        return enKotu
    }

    /** Bir oyun oynar; ulaşılan en büyük taşı ve skoru döndürür. */
    private fun oyna(tohum: Long): Pair<Int, Long> {
        var s = G2048State.newGame(tohum)
        var guvenlik = 0
        while (s.status == G2048Status.RUNNING && guvenlik++ < 20_000) {
            var enIyi: G2048State? = null
            var enIyiPuan = -Double.MAX_VALUE
            for (yon in MoveDir.entries) {
                val sonraki = s.move(yon)
                if (sonraki.cells == s.cells) continue
                val p = derinPuan(sonraki)
                if (p > enIyiPuan) {
                    enIyiPuan = p
                    enIyi = sonraki
                }
            }
            s = enIyi ?: break
        }
        return s.cells.max() to s.score
    }

    @Test
    fun winnability() {
        println("\n=== KAZANILABİLİRLİK ===")
        println("oyuncu modeli: tekdüzelik + boş hücre + köşe sezgiseli, bir kat ileri bakış")
        println()
        val dagilim = HashMap<Int, Int>()
        var toplamSkor = 0L
        val n = 60
        for (tohum in 1L..n.toLong()) {
            val (enBuyuk, skor) = oyna(tohum)
            dagilim[enBuyuk] = (dagilim[enBuyuk] ?: 0) + 1
            toplamSkor += skor
        }
        println("en büyük taş | oyun sayısı")
        for (t in dagilim.keys.sorted()) {
            println("${"%12d".format(t)} | ${dagilim[t]}")
        }
        val ulasan = dagilim.filterKeys { it >= 2048 }.values.sum()
        println()
        println("2048'e ulaşan: $ulasan / $n (%${100 * ulasan / n})")
        println("ortalama skor: ${toplamSkor / n}")
        println()
        println("Sezgisel kaba; insan daha iyisini yapar. Buradaki sayı bir alt sınırdır:")
        println("2048'e hiç ulaşılamıyorsa doğuş ya da birleşme kuralında sorun aranır.")
    }

    @Test
    fun spawnDistribution() {
        println("\n=== TAŞ DOĞUŞU ===")
        println("kural: onda dokuz 2, onda bir 4")
        println()
        var iki = 0
        var dort = 0
        for (tohum in 1L..300L) {
            var s = G2048State.newGame(tohum)
            var guvenlik = 0
            while (s.status == G2048Status.RUNNING && guvenlik++ < 300) {
                val once = s.cells
                val sonraki = MoveDir.entries.map { s.move(it) }.firstOrNull { it.cells != once } ?: break
                if (sonraki.lastSpawn >= 0) {
                    if (sonraki.cells[sonraki.lastSpawn] == 2) iki++ else dort++
                }
                s = sonraki
            }
        }
        val toplam = iki + dort
        println("toplam doğuş: $toplam")
        println("2 gelen: $iki (%${"%.1f".format(100.0 * iki / toplam)})   beklenen %90")
        println("4 gelen: $dort (%${"%.1f".format(100.0 * dort / toplam)})   beklenen %10")
        val sapma = kotlin.math.abs(100.0 * dort / toplam - 10.0)
        println()
        println(if (sapma < 1.5) "Dağılım kurala uygun." else "UYARI: dağılım kuraldan sapıyor.")
    }
}
