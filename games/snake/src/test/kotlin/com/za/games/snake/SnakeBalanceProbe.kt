package com.za.games.snake

import org.junit.Test

/**
 * Yılan denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:snake:probe` ile koşar.
 *
 * Yılan'da tek zorluk kaynağı hızdır: her yemle adım aralığı kısalır
 * ([snakeSpeedMillis]). Ölçümün sorusu: hız tavanı insanın yön değiştirmesine
 * yetecek süreyi bırakıyor mu, ve tavan ne zaman geliyor?
 */
class SnakeBalanceProbe {

    @Test
    fun speedCurve() {
        println("\n=== HIZ EĞRİSİ ===")
        println("tahta ${SnakeState.WIDTH}×${SnakeState.HEIGHT} = ${SnakeState.WIDTH * SnakeState.HEIGHT} hücre")
        println("adım aralığı = 160 − yem×3, taban 70 ms")
        println()
        println("yem | adım aralığı | saniyede adım | tahtayı boydan boya geçme")
        var tabanYem = -1
        for (yem in listOf(0, 5, 10, 20, 25, 30, 40, 60)) {
            val ms = snakeSpeedMillis(yem)
            if (tabanYem < 0 && ms <= 70L) tabanYem = yem
            val gecis = ms * SnakeState.WIDTH / 1000f
            println(
                "${"%3d".format(yem)} |    ${"%3d".format(ms)} ms    |     ${"%5.1f".format(1000f / ms)}     |         ${"%.2f".format(gecis)}s",
            )
        }
        println()
        println("Hız $tabanYem yemde 70 ms tabanına oturuyor; sonrasında oyun hızlanmıyor.")
        println("70 ms, insan tepki süresinin (~200 ms) altında: yani tavan hızda oyuncu")
        println("tek tek adımlara tepki veremez, önden plan yapmak zorundadır. Yılan")
        println("oyunlarında beklenen budur; kritik olan tavanın ne zaman geldiği.")
    }

    @Test
    fun lengthPressure() {
        println("\n=== UZUNLUK BASKISI ===")
        println("yem yendikçe yılan uzar; boş hücre azaldıkça sıkışma riski artar")
        println()
        val hucre = SnakeState.WIDTH * SnakeState.HEIGHT
        println("yem | yılan uzunluğu | boş hücre | doluluk")
        for (yem in listOf(0, 20, 50, 100, 150, 200, 250)) {
            val uzunluk = yem + 1
            val bos = hucre - uzunluk
            println(
                "${"%3d".format(yem)} |      ${"%4d".format(uzunluk)}      |   ${"%4d".format(bos)}    |   %${"%.0f".format(100f * uzunluk / hucre)}",
            )
        }
        println()
        println("Tahta ${hucre} hücre; kuramsal tavan ${hucre - 1} yem. Hız tavanı 30 yemde")
        println("geldiği için oyunun geri kalanı sabit hızda, artan uzunlukla oynanıyor:")
        println("zorluk hızdan değil yerden geliyor. Tasarım tutarlı.")
    }
}
