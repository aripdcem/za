package com.za.games.tavla

import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Tavla denge ölçümü — birim testi değil, rapor üretir
 * (bkz. docs/oyun-testi.md). `./gradlew :games:tavla:probe` ile koşar.
 *
 * Tavlada oyuncunun ilk şüphesi hep zardır. Ölçümün soruları: zar gerçekten
 * düzgün mü dağılıyor, ilk oynayanın avantajı makul mü, ve bilgisayar rakip
 * hem yenilebilir hem de rastgeleden belirgin güçlü mü?
 */
class TavlaBalanceProbe {

    private val modlar = TavlaMode.entries.map { TavlaRules(mode = it) }

    /** Oyun sonucu: kazanan (null = berabere), bitti mi, kilitlenmeyle mi bitti. */
    private data class Sonuc(val kazanan: Int?, val bitti: Boolean, val kilit: Boolean)

    private fun oyna(rules: TavlaRules, seed: Long, aiOyuncu: Int, zarSayaci: IntArray? = null): Sonuc {
        val rastgele = Random(seed xor 0x5EED)
        var s = TavlaState.newMatch(rules, seed).openingRoll()
        zarSayaci?.let { for (d in s.dice) it[d - 1]++ }
        var guvenlik = 0
        while (s.phase != Phase.GAME_OVER && s.phase != Phase.MATCH_OVER && guvenlik++ < 20_000) {
            when (s.phase) {
                Phase.TO_ROLL -> {
                    s = s.roll()
                    zarSayaci?.let { for (d in s.dice) it[d - 1]++ }
                }
                Phase.MOVING -> {
                    if (s.turn == aiOyuncu) {
                        for (m in TavlaAi.chooseTurn(s)) s = s.move(m.from, m.to)
                    } else {
                        while (true) {
                            val hamleler = s.legalMoves()
                            if (hamleler.isEmpty()) break
                            val m = hamleler[rastgele.nextInt(hamleler.size)]
                            s = s.move(m.from, m.to)
                        }
                    }
                    s = s.endTurn()
                }
                else -> break
            }
        }
        val bitti = s.phase == Phase.GAME_OVER || s.phase == Phase.MATCH_OVER
        return Sonuc(s.winner, bitti, s.deadlock)
    }

    // ------------------------------------------------------------------
    // 1) Zar dağılımı
    // ------------------------------------------------------------------

    @Test
    fun diceFairness() {
        println("\n=== ZAR DAĞILIMI ===")
        println("zar, durumun tohumundan karıştırıcı bir fonksiyonla türetiliyor")
        println("(deterministik: aynı tohum aynı oyun); ölçüm ampirik dağılıma bakar")
        println()
        val sayac = IntArray(6)
        for (tohum in 1L..400L) oyna(modlar[0], tohum, aiOyuncu = 0, zarSayaci = sayac)
        val toplam = sayac.sum()
        val beklenen = toplam / 6.0
        println("toplam zar: $toplam   (beklenen her yüz: ${"%.0f".format(beklenen)})")
        println()
        println("yüz | görülme | sapma")
        var enBuyukSapma = 0.0
        for (i in 0 until 6) {
            val sapma = (sayac[i] - beklenen) / beklenen * 100
            if (abs(sapma) > enBuyukSapma) enBuyukSapma = abs(sapma)
            println(" ${i + 1}  |  ${"%6d".format(sayac[i])} | ${"%+5.1f".format(sapma)}%")
        }
        // Ki-kare: 5 serbestlik derecesinde %99 eşiği 15,09.
        val kiKare = (0 until 6).sumOf { val f = sayac[it] - beklenen; f * f / beklenen }
        println()
        println("ki-kare = ${"%.2f".format(kiKare)}  (5 sd, %99 eşiği 15,09)")
        println(if (kiKare < 15.09) "Dağılım düzgün: sapma rastgelelikle açıklanabilir."
                else "UYARI: dağılım beklenenden uzak, zar üretimi incelenmeli.")
        println("en büyük yüz sapması: %${"%.1f".format(enBuyukSapma)}")
    }

    // ------------------------------------------------------------------
    // 2) İlk oynayan avantajı
    // ------------------------------------------------------------------

    @Test
    fun firstPlayerEdge() {
        println("\n=== İLK OYNAYAN AVANTAJI (yapay zekâ – yapay zekâ) ===")
        println("açılış zarı büyük olan başlar; her iki taraf da aynı sezgiselle oynar")
        println()
        println("mod     | oyun | başlayan kazandı | kilitlenme | berabere | bitmedi*")
        for (rules in modlar) {
            var basladiKazandi = 0
            var berabere = 0
            var bitmedi = 0
            var kilit = 0
            val n = 120
            for (tohum in 1L..n.toLong()) {
                var s = TavlaState.newMatch(rules, tohum).openingRoll()
                val baslayan = s.turn
                var guvenlik = 0
                while (s.phase != Phase.GAME_OVER && s.phase != Phase.MATCH_OVER && guvenlik++ < 20_000) {
                    when (s.phase) {
                        Phase.TO_ROLL -> s = s.roll()
                        Phase.MOVING -> {
                            for (m in TavlaAi.chooseTurn(s)) s = s.move(m.from, m.to)
                            s = s.endTurn()
                        }
                        else -> break
                    }
                }
                val bitti = s.phase == Phase.GAME_OVER || s.phase == Phase.MATCH_OVER
                if (s.deadlock) kilit++
                when {
                    !bitti -> bitmedi++
                    s.winner == null -> berabere++
                    s.winner == baslayan -> basladiKazandi++
                }
            }
            println(
                "${rules.mode.name.padEnd(8)}|  ${"%3d".format(n)} |       %${"%3d".format(100 * basladiKazandi / n)}        " +
                    "|     ${"%3d".format(kilit)}    |    ${"%3d".format(berabere)}   |   $bitmedi",
            )
        }
        println()
        println("* ölçümün 20 000 adımlık güvenlik sınırına takılan, yani bitmeyen oyun;")
        println("  gerçek beraberlik değildir, sayılardan ayrı tutulur.")
        println()
        println("Tavlada ilk oynayanın doğal avantajı vardır; %50–60 bandı beklenir.")
        println("%70'in üstü, açılışın fazla belirleyici olduğunu gösterir.")
    }

    // ------------------------------------------------------------------
    // 3) Bilgisayar rakip ne kadar güçlü?
    // ------------------------------------------------------------------

    @Test
    fun aiStrength() {
        println("\n=== BİLGİSAYAR RAKİP GÜCÜ (yapay zekâ – rastgele yasal hamle) ===")
        println("rastgele oyuncu her turda yasal hamleleri rastgele seçer")
        println()
        println("mod     | oyun | yapay zekâ kazandı | berabere | bitmedi*")
        for (rules in modlar) {
            var aiKazandi = 0
            var berabere = 0
            var bitmedi = 0
            val n = 120
            for (tohum in 1L..n.toLong()) {
                // Tohumun yarısında yapay zekâ ikinci oynasın: ilk oynayan
                // avantajı sonucu kirletmesin.
                val ai = (tohum % 2).toInt()
                val r = oyna(rules, tohum, ai)
                when {
                    !r.bitti -> bitmedi++
                    r.kazanan == null -> berabere++
                    r.kazanan == ai -> aiKazandi++
                }
            }
            println(
                "${rules.mode.name.padEnd(8)}|  ${"%3d".format(n)} |        %${"%3d".format(100 * aiKazandi / n)}         |    $berabere     |   $bitmedi",
            )
        }
        println()
        println("* bitmeyen oyun (güvenlik sınırı), gerçek beraberlik değil.")
        println()
        println("Sezgisel bir rakipten beklenen: rastgeleye karşı ezici üstünlük (%85+),")
        println("ama insana karşı yenilebilir olması. Oran %60'ın altındaysa sezgisel")
        println("çalışmıyor; %100 ise rastgele oyuncu hiç şans bulamıyor demektir.")
    }
}
