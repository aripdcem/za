package com.za.games.balkon

import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Balkon denge ölçümü — birim testi değil, rapor üretir (bkz. docs/oyun-testi.md).
 * `./gradlew :games:balkon:probe` ile koşar; CI'daki `test` görevinden dışlanır.
 *
 * Balkon bir öndeleme oyunu: atış [BalkonWorld.flightTime] kadar havada kalır,
 * bu sırada hedef yürür ve rüzgâr iniş noktasını kaydırır. Yani nişan, hedefin
 * *şimdiki* yerine değil *inişteki* yerine alınır. Ölçümün soruları: gereken
 * öndeleme isabet yarıçapına göre ne kadar büyük, rüzgâr telafi edilebilir mi,
 * ve seviye hedefi süreye sığıyor mu?
 */
class BalkonBalanceProbe {

    // ------------------------------------------------------------------
    // 1) Öndeleme: hedef uçuş sırasında ne kadar kayıyor?
    // ------------------------------------------------------------------

    @Test
    fun leading() {
        println("\n=== 1. ÖNDELEME ===")
        println("uçuş süresi ${BalkonWorld.flightTime(0f)}s (yakın) – ${BalkonWorld.flightTime(1f)}s (uzak)")
        println("isabet yarıçapı ${BalkonWorld.RADIUS} (mega ${BalkonWorld.MEGA_RADIUS})")
        println()
        println("hedef    | hız aralığı | 1. sv öndeleme | 11+ sv öndeleme | yarıçapın katı (11+)")
        for (k in TargetKind.entries) {
            val ortHiz = (k.minSpeed + k.maxSpeed) / 2f
            val ucus = BalkonWorld.flightTime(0.5f)
            val sv1 = ortHiz * ucus
            val sv11 = ortHiz * 1.8f * ucus            // speedScale tavanı
            val kat = sv11 / (BalkonWorld.RADIUS + k.radius)
            println(
                "${k.name.padEnd(9)}| ${"%.2f".format(k.minSpeed)}–${"%.2f".format(k.maxSpeed)}   " +
                    "|     ${"%.3f".format(sv1)}      |      ${"%.3f".format(sv11)}      |   ${"%.1f".format(kat)}×",
            )
        }
        println()
        println("Öndeleme yarıçaptan büyükse hedefin şimdiki yerine nişan almak ıskalar;")
        println("oyuncunun hızı okuyup önüne atması gerekir.")
    }

    // ------------------------------------------------------------------
    // 2) Zamanlama toleransı: kaç milisaniyelik hata affediliyor?
    // ------------------------------------------------------------------

    @Test
    fun tolerance() {
        println("\n=== 2. ZAMANLAMA TOLERANSI ===")
        println("nişan doğruysa, hedefin yarıçaptan çıkması için geçen süre")
        println()
        println("hedef    | 1. seviye | 11+ seviye | mega (11+)")
        for (k in TargetKind.entries) {
            val ortHiz = (k.minSpeed + k.maxSpeed) / 2f
            val pencere = (BalkonWorld.RADIUS + k.radius) / ortHiz
            val pencere11 = (BalkonWorld.RADIUS + k.radius) / (ortHiz * 1.8f)
            val mega11 = (BalkonWorld.MEGA_RADIUS + k.radius) / (ortHiz * 1.8f)
            println(
                "${k.name.padEnd(9)}|  ${"%4.0f".format(pencere * 1000)} ms  |  ${"%4.0f".format(pencere11 * 1000)} ms   " +
                    "|  ${"%4.0f".format(mega11 * 1000)} ms",
            )
        }
        println()
        println("İnsan dokunma hassasiyeti ~50–100 ms; bunun altına inen pencereler")
        println("nişanı şansa bırakır.")
    }

    // ------------------------------------------------------------------
    // 3) Rüzgâr: telafi edilebilir mi?
    // ------------------------------------------------------------------

    @Test
    fun wind() {
        println("\n=== 3. RÜZGÂR ===")
        println("kayma = rüzgâr × uçuş süresi; atış anında sabitlenir (Shot.drift),")
        println("yani atarken bilinir ve HUD'da gösterilir — tahmin değil, telafi işi.")
        println()
        println("rüzgâr | yakın atış (0,45s) | uzak atış (0,95s) | yarıçapın katı (uzak)")
        for (w in listOf(0f, 0.05f, 0.10f, BalkonWorld.MAX_WIND)) {
            val yakin = w * BalkonWorld.flightTime(0f)
            val uzak = w * BalkonWorld.flightTime(1f)
            println(
                "${"%5.2f".format(w)}  |       ${"%.3f".format(yakin)}        |      ${"%.3f".format(uzak)}       " +
                    "|   ${"%.1f".format(uzak / BalkonWorld.RADIUS)}×",
            )
        }
        println()
        println("Azami rüzgârda uzak atışın kayması isabet yarıçapının katı kadar:")
        println("telafi edilmezse uzak hedef vurulamaz.")
    }

    // ------------------------------------------------------------------
    // 4) Seviye hedefi süreye sığıyor mu?
    // ------------------------------------------------------------------

    @Test
    fun levelDemand() {
        println("\n=== 4. SEVİYE HEDEFİ ===")
        println("süre ${BalkonWorld.LEVEL_TIME}s; aynı anda havada en çok ${BalkonWorld.MAX_SHOTS} atış")
        println()
        println("seviye | gereken isabet | ekrandaki hedef | isabet/s | gereken isabet oranı*")
        for (sv in listOf(1, 3, 5, 8, 11, 15, 20)) {
            val gereken = BalkonWorld.required(sv)
            val hedef = min(12, BalkonWorld.MAX_TARGETS_BASE + sv)
            val hizi = gereken / BalkonWorld.LEVEL_TIME
            // Uçuş ~0,7s ve en çok 3 atış havada: kabaca 4,3 atış/s mümkün.
            val mumkunAtis = BalkonWorld.MAX_SHOTS / BalkonWorld.flightTime(0.5f)
            val oran = hizi / mumkunAtis
            println(
                "${"%6d".format(sv)} |       ${"%3d".format(gereken)}      |       ${"%2d".format(hedef)}        " +
                    "|  ${"%.2f".format(hizi)}   |        %${"%.0f".format(oran * 100)}",
            )
        }
        println()
        println("* atış hızı tavanına göre gereken isabet oranı; %100'e yaklaşmak")
        println("  kusursuz nişan demektir. Kalan süre bonusa döndüğü için asıl")
        println("  baskı isabet oranında, atış hızında değil.")
    }

    // ------------------------------------------------------------------
    // 5) Ölçülen: kusursuz nişan alan bot seviye hedefini tutuyor mu?
    // ------------------------------------------------------------------

    @Test
    fun perfectAim() {
        println("\n=== 5. KUSURSUZ NİŞAN (öndeleme + rüzgâr telafisi, motor üstünde) ===")
        println("bot hedefin iniş anındaki yerini hesaplar ve rüzgâr kaymasını düşer;")
        println("tek koşuda nereye kadar gidebildiğine bakılır (5 tohum)")
        println()
        println("tohum | ulaşılan seviye | toplam isabet | bitiş")
        val seviyeler = ArrayList<Int>()
        for (tohum in 1L..5L) {
            val w = BalkonWorld(tohum)
            var kare = 0
            // CLEARED seviye arası duraklamadır, bitiş değil: yalnızca OVER durdurur.
            while (w.status != BalkonStatus.OVER && kare < 60 * 900) {
                val hedef = w.targets.firstOrNull { !it.kind.forbidden && !it.hit }
                if (hedef != null && w.shots.size < BalkonWorld.MAX_SHOTS) {
                    val ucus = BalkonWorld.flightTime(hedef.y)
                    val olcek = min(1.8f, 1f + 0.08f * (w.level - 1))
                    // Hedefin iniş anındaki yeri, eksi rüzgâr kayması.
                    val nisanX = hedef.x + hedef.dir * hedef.speed * olcek * ucus - w.wind * ucus
                    w.throwAt(nisanX, hedef.y, mega = false)
                }
                w.step()
                kare++
            }
            seviyeler += w.level
            println(
                "${"%5d".format(tohum)} |        ${"%2d".format(w.level)}       |      ${"%4d".format(w.hits)}     " +
                    "|  ${if (w.status == BalkonStatus.OVER) "süre bitti" else "ölçüm penceresi doldu"}",
            )
        }
        println()
        println("ortalama ulaşılan seviye: ${"%.1f".format(seviyeler.average())}")
        println("Kusursuz nişanın durduğu yer, oyunun tavanıdır: insan bunun altında kalır.")
    }
}
