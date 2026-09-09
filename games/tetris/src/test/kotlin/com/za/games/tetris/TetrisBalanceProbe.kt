package com.za.games.tetris

import org.junit.Test

/**
 * Blok denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:tetris:probe` ile koşar.
 *
 * Blok'ta zaman baskısı tek yerden gelir: yerçekimi. Seviye her 10 satırda bir
 * artar ve düşme aralığı kısalır. Ölçümün sorusu: hangi seviyede parça,
 * oyuncunun onu yerleştirmesine yetecek süreden az havada kalıyor?
 */
class TetrisBalanceProbe {

    /** Bir parçanın tepeden dibe düşme süresi (s); [TetrisState.HEIGHT] hücre. */
    private fun dususSuresi(seviye: Int): Float =
        gravityMillis(seviye) * TetrisState.HEIGHT / 1000f

    @Test
    fun gravityCurve() {
        println("\n=== YERÇEKİMİ EĞRİSİ ===")
        println("seviye = satır / 10 + 1; düşme aralığı Guideline formülü,")
        println("50 ms tabanla sınırlı")
        println()
        println("seviye | gereken satır | hücre başına | tepeden dibe | sığan hamle*")
        var tabanSeviye = -1
        for (sv in listOf(1, 3, 5, 7, 9, 10, 11, 13, 15, 20)) {
            val ms = gravityMillis(sv)
            val dusus = dususSuresi(sv)
            // Ekran tuşuna basma + tekrar aralığı gerçekçi olarak ~120 ms.
            val hamle = (dusus / 0.12f).toInt()
            if (tabanSeviye < 0 && ms <= 50L) tabanSeviye = sv
            println(
                "${"%6d".format(sv)} |      ${"%4d".format((sv - 1) * 10)}     |   ${"%4d".format(ms)} ms   " +
                    "|   ${"%5.2f".format(dusus)}s    |     ${"%3d".format(hamle)}",
            )
        }
        println()
        println("* 120 ms'lik tuş temposuyla, parça dibe inene dek sığdırılabilen hamle")
        println("  sayısı (yumuşak düşürme ve kilit gecikmesi hesaba katılmadan).")
        println()
        println("Yerçekimi ${tabanSeviye}. seviyede 50 ms tabanına oturuyor; o noktadan")
        println("sonra oyun hızlanmıyor, yani zorluk artışı ${(tabanSeviye - 1) * 10}. satırda duruyor.")
    }

    @Test
    fun placementBudget() {
        println("\n=== YERLEŞTİRME BÜTÇESİ ===")
        println("bir parçayı en kötü durumda yerleştirmek için gereken hamle:")
        println("en fazla 3 dönüş + 5 yatay adım ≈ 8 giriş")
        println()
        println("seviye | tepeden dibe | 8 giriş için gereken tempo | insan için")
        for (sv in listOf(1, 5, 9, 10, 12, 15, 20)) {
            val dusus = dususSuresi(sv)
            val tempo = dusus / 8f * 1000f
            val yorum = when {
                tempo >= 150f -> "rahat"
                tempo >= 100f -> "sıkı"
                tempo >= 60f -> "çok sıkı"
                else -> "insan üstü"
            }
            println(
                "${"%6d".format(sv)} |   ${"%5.2f".format(dusus)}s    |        ${"%5.0f".format(tempo)} ms/giriş       |  $yorum",
            )
        }
        println()
        println("Not: yumuşak/sert düşürme oyuncunun elinde olduğu için bu bütçe bir")
        println("tavan değil alt sınırdır; oyuncu isterse parçayı daha çabuk indirir.")
        println("Asıl kısıt, parçayı yerleştirmeye karar verecek zamanın kalması.")
    }
}
