package com.za.games.sudoku

import org.junit.Test

/**
 * Sudoku denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:sudoku:probe` ile koşar.
 *
 * Üretici tek çözümü garantiliyor (`countSolutions == 1`), yani adillik
 * tarafında sorun yok. Açık kalan soru **zorluğun ne anlama geldiği**:
 * zorluk yalnızca ipucu sayısıyla tanımlanıyor (40/32/26), oysa ipucu sayısı
 * zorluğun zayıf bir göstergesidir. Ölçüm, her zorluğun hangi insan
 * teknikleriyle çözülebildiğine bakar.
 */
class SudokuBalanceProbe {

    private val birimler: List<List<Int>> = buildList {
        for (r in 0 until 9) add((0 until 9).map { r * 9 + it })
        for (c in 0 until 9) add((0 until 9).map { it * 9 + c })
        for (b in 0 until 9) {
            val r0 = b / 3 * 3
            val c0 = b % 3 * 3
            add((0 until 9).map { (r0 + it / 3) * 9 + (c0 + it % 3) })
        }
    }

    private fun adaylar(v: IntArray, i: Int): Set<Int> {
        if (v[i] != 0) return emptySet()
        val kullanilan = SudokuState.peers(i).map { v[it] }.toSet()
        return (1..9).filterNot { it in kullanilan }.toSet()
    }

    /** Çözüm sırasında gereken en ileri teknik. */
    private enum class Teknik { TEK_ADAY, GIZLI_TEK, DAHA_ILERI }

    /**
     * İnsan teknikleriyle çözmeye çalışır ve gereken en ileri tekniği döndürür.
     * Sırayla: tek aday (hücrede tek seçenek), gizli tek (bir rakam birimde tek
     * hücreye sığıyor). İkisi de ilerletmiyorsa daha ileri teknik (ya da
     * tahmin) gerekiyordur.
     */
    private fun cozTeknikle(baslangic: List<Int>): Teknik {
        val v = baslangic.toIntArray()
        var enIleri = Teknik.TEK_ADAY
        while (v.any { it == 0 }) {
            var ilerledi = false

            // Tek aday.
            for (i in 0 until 81) {
                if (v[i] != 0) continue
                val a = adaylar(v, i)
                if (a.size == 1) {
                    v[i] = a.first()
                    ilerledi = true
                }
            }
            if (ilerledi) continue

            // Gizli tek.
            for (birim in birimler) {
                for (rakam in 1..9) {
                    if (birim.any { v[it] == rakam }) continue
                    val yer = birim.filter { v[it] == 0 && rakam in adaylar(v, it) }
                    if (yer.size == 1) {
                        v[yer.first()] = rakam
                        ilerledi = true
                        if (enIleri == Teknik.TEK_ADAY) enIleri = Teknik.GIZLI_TEK
                    }
                }
            }
            if (!ilerledi) return Teknik.DAHA_ILERI
        }
        return enIleri
    }

    @Test
    fun difficultyMeaning() {
        println("\n=== ZORLUK NE DEMEK? ===")
        println("üretici tek çözümü garantiliyor; ölçüm zorluğun gerçekten")
        println("zorlaşıp zorlaşmadığına bakıyor (40 tohum/zorluk)")
        println()
        println("zorluk | hedef ipucu | gerçek ipucu (ort / en az) | tek adayla | gizli tekle | daha ileri")
        for (zorluk in SudokuDifficulty.entries) {
            var toplamIpucu = 0
            var enAz = 81
            val sayac = HashMap<Teknik, Int>()
            val n = 40
            for (tohum in 1L..n.toLong()) {
                val s = SudokuState.newGame(zorluk, tohum)
                val ipucu = s.given.count { it }
                toplamIpucu += ipucu
                if (ipucu < enAz) enAz = ipucu
                val t = cozTeknikle(s.values)
                sayac[t] = (sayac[t] ?: 0) + 1
            }
            fun yuzde(t: Teknik) = 100 * (sayac[t] ?: 0) / n
            println(
                "${zorluk.name.padEnd(7)}|     ${"%2d".format(zorluk.targetClues)}      " +
                    "|        ${"%.1f".format(toplamIpucu.toFloat() / n)} / ${"%2d".format(enAz)}         " +
                    "|    %${"%3d".format(yuzde(Teknik.TEK_ADAY))}   |    %${"%3d".format(yuzde(Teknik.GIZLI_TEK))}    " +
                    "|    %${"%3d".format(yuzde(Teknik.DAHA_ILERI))}",
            )
        }
        println()
        println("'Tek aday' en kolay teknik: hücrede tek seçenek kalmış. 'Gizli tek' bir")
        println("adım ileri: rakam birimde tek hücreye sığıyor. 'Daha ileri' ise bu iki")
        println("teknikle çözülemeyen, yani çift/üçlü çıkarımı ya da deneme gerektiren")
        println("tahta demek — tek çözümü olsa da acemi oyuncuyu duvara toslatır.")
    }
}
