package com.za.games.kuyu

import org.junit.Test
import kotlin.math.max

/**
 * Kuyu denge ölçümü — birim testi değil, rapor üretir (bkz. docs/oyun-testi.md).
 * `./gradlew :games:kuyu:probe` ile koşar; CI'daki `test` görevinden dışlanır.
 *
 * Kuyu'nun çekirdek kaynağı **şarjör**: tek tuş yerdeyken zıplatır, havada
 * basılı tutulunca aşağı ateş eder ve düşüşü [KuyuWorld.SHOT_FALL] ile
 * sınırlar. Yani şarjör, "havada kalma bütçesi"dir ve inişte dolar. Ölçümün
 * ana sorusu: her yükseltme bu bütçeyi ve şarjör başına hasarı gerçekten
 * iyileştiriyor mu?
 */
class KuyuBalanceProbe {

    /** Boş kuyu: yanlar duvar, iç boş; yalnızca oyuncu fiziği ölçülür. */
    private fun bosDunya(zemin: Set<Int> = setOf(6)): KuyuWorld = KuyuWorld(1L) { index ->
        val g = KuyuGen.WIDTH
        val y = KuyuGen.CHUNK_ROWS
        val tiles = Array(y * g) { Tile.EMPTY }
        for (r in 0 until y) {
            tiles[r * g] = Tile.WALL
            tiles[r * g + g - 1] = Tile.WALL
            if (index * y + r in zemin) for (c in 1 until g - 1) tiles[r * g + c] = Tile.WALL
        }
        KuyuChunk(index, tiles, emptyList())
    }

    // ------------------------------------------------------------------
    // 1) Havada kalma bütçesi
    // ------------------------------------------------------------------

    /** Bir şarjörle havada geçen süre ve kat edilen düşüş (kare cinsinden). */
    private data class Butce(val saniye: Float, val dusus: Float, val atis: Int)

    private fun butce(maxAmmo: Int, shotInterval: Int): Butce {
        // Her atış vy'yi SHOT_FALL'a çeker, arada yerçekimi hızlandırır.
        var vy = KuyuWorld.SHOT_FALL
        var dusus = 0f
        var kare = 0
        repeat(maxAmmo) {
            vy = KuyuWorld.SHOT_FALL
            repeat(shotInterval) {
                vy = minOf(vy + KuyuWorld.GRAVITY * KuyuWorld.STEP, KuyuWorld.MAX_FALL)
                dusus += vy * KuyuWorld.STEP
                kare++
            }
        }
        return Butce(kare / 60f, dusus, maxAmmo)
    }

    @Test
    fun hoverBudget() {
        println("\n=== 1. HAVADA KALMA BÜTÇESİ (bir şarjör) ===")
        println("şarjör inişte dolar; havada ateş düşüşü ${KuyuWorld.SHOT_FALL} kare/s ile sınırlar")
        println("taban: şarjör ${KuyuWorld.AMMO}, atış aralığı ${KuyuWorld.SHOT_INTERVAL} kare")
        println()
        println("kurulum                    | atış | havada süre | düşüş (kare) | hasar/şarjör")
        data class Kur(val ad: String, val ammo: Int, val aralik: Int, val spread: Boolean)
        val kurulumlar = listOf(
            Kur("taban", KuyuWorld.AMMO, KuyuWorld.SHOT_INTERVAL, false),
            Kur("+RAPID", KuyuWorld.AMMO + 4, 4, false),
            Kur("+AMMO ×1", KuyuWorld.AMMO + 2, KuyuWorld.SHOT_INTERVAL, false),
            Kur("+AMMO ×2", KuyuWorld.AMMO + 4, KuyuWorld.SHOT_INTERVAL, false),
            Kur("+SPREAD", KuyuWorld.AMMO, KuyuWorld.SHOT_INTERVAL, true),
            Kur("+RAPID +AMMO ×2", KuyuWorld.AMMO + 4, 4, false),
        )
        for (k in kurulumlar) {
            val b = butce(k.ammo, k.aralik)
            val hasar = k.ammo * (if (k.spread) 3 else 1)
            println(
                "${k.ad.padEnd(26)} | ${"%4d".format(b.atis)} |   ${"%5.2f".format(b.saniye)}s   " +
                    "|    ${"%5.2f".format(b.dusus)}     |     ${"%3d".format(hasar)}",
            )
        }
        println()
        println("Not: şarjör atış başına düşer, mermi başına değil. SPREAD tek atışta üç")
        println("mermi attığı için şarjör başına hasarı üçe katlar; RAPID ise aralığı")
        println("kısaltırken şarjörü de artırdığı için havada kalmayı korur.")
    }

    // ------------------------------------------------------------------
    // 2) Yükseltme karşılaştırması: her biri bir öncekinden iyi mi?
    // ------------------------------------------------------------------

    @Test
    fun upgrades() {
        println("\n=== 2. YÜKSELTMELER ===")
        println("yükseltme | havada süre | şarjör başına hasar | kayıp var mı")
        val taban = butce(KuyuWorld.AMMO, KuyuWorld.SHOT_INTERVAL)
        val tabanHasar = KuyuWorld.AMMO
        data class Y(val ad: String, val ammo: Int, val aralik: Int, val carpan: Int)
        val liste = listOf(
            Y("AMMO", KuyuWorld.AMMO + 2, KuyuWorld.SHOT_INTERVAL, 1),
            Y("RAPID", KuyuWorld.AMMO + 4, 4, 1),
            Y("SPREAD", KuyuWorld.AMMO, KuyuWorld.SHOT_INTERVAL, 3),
        )
        for (y in liste) {
            val b = butce(y.ammo, y.aralik)
            val hasar = y.ammo * y.carpan
            val sureFark = (b.saniye - taban.saniye) / taban.saniye * 100f
            val hasarFark = (hasar - tabanHasar).toFloat() / tabanHasar * 100f
            val uyari = if (sureFark < -1f || hasarFark < -1f) "EVET" else "hayır"
            println(
                "${y.ad.padEnd(10)}|   ${"%5.2f".format(b.saniye)}s (${"%+.0f".format(sureFark)}%)  " +
                    "|      ${"%3d".format(hasar)} (${"%+.0f".format(hasarFark)}%)      |  $uyari",
            )
        }
        println()
        println("RAPID atış aralığını kısaltırken şarjörü de artırır (+4): havada kalma")
        println("korunur, düşüş daha sık frenlenir, şarjör başına hasar artar. Tek başına")
        println("aralık kısaltmak havada kalmayı %33 kısaltıp hasarı değiştirmiyordu.")
    }

    // ------------------------------------------------------------------
    // 3) Ölçülen: gerçek dünyada bir şarjörle ne kadar havada kalınıyor?
    // ------------------------------------------------------------------

    @Test
    fun measuredHover() {
        println("\n=== 3. ÖLÇÜLEN HAVADA KALMA (dipsiz kuyu, motor üstünde) ===")
        println("zemin yok: oyuncu serbest düşerken tuş basılı; şarjör bitene dek ölç")
        println("kurulum  | havada kare | süre  | düşüş (kare) | 1. bölümdeki kestirim")
        for ((ad, rapid) in listOf("taban" to false, "RAPID" to true)) {
            val w = bosDunya(zemin = emptySet())
            if (rapid) w.grant(Upgrade.RAPID)
            val basY = w.player.y
            var kare = 0
            while (kare < 60 * 20 && w.player.ammo > 0) {
                w.step(KuyuInput(fire = true))
                kare++
            }
            val kestirim = butce(w.perks.maxAmmo, w.perks.shotInterval)
            println(
                "${ad.padEnd(9)}|    ${"%4d".format(kare)}     | ${"%5.2f".format(kare / 60f)}s " +
                    "|    ${"%5.2f".format(w.player.y - basY)}     |  ${"%5.2f".format(kestirim.saniye)}s / ${"%.2f".format(kestirim.dusus)}",
            )
        }
    }

    // ------------------------------------------------------------------
    // 4) Düşüş: insan sınırlı bot ne kadar iniyor?
    // ------------------------------------------------------------------

    @Test
    fun descent() {
        println("\n=== 4. İNİŞ (insan sınırlı bot, 6 tohum) ===")
        println("beceri | ort.derinlik | en iyi | ort.taş | ort.bölge")
        for ((ad, gecikme) in listOf("acemi " to 18, "orta  " to 12, "usta  " to 6)) {
            val sonuc = (1L..6L).map { tohum -> kos(tohum, gecikme) }
            println(
                "$ad |     ${"%5.0f".format(sonuc.map { it.first.toDouble() }.average())}    " +
                    "|  ${sonuc.maxOf { it.first }}  |   ${"%4.0f".format(sonuc.map { it.second.toDouble() }.average())}  " +
                    "|    ${"%.1f".format(sonuc.map { it.third.toDouble() }.average())}",
            )
        }
    }

    /**
     * İniş botu. Kuyu'da kendiliğinden düşülmez: zemindeki boşluğu bulup oraya
     * yürümek gerekir. Bot altındaki ilk katı tarar, en yakın boşluğa yönelir;
     * düşerken altında düşman varsa ya da iniş sertse ateşler (hem vurur hem
     * frenler). Tepki gecikmesi insan sınırını temsil eder.
     */
    private fun kos(tohum: Long, gecikme: Int): Triple<Int, Int, Int> {
        val w = KuyuWorld(tohum)
        var tik = 0
        var sol = false
        var sag = false
        var ates = false
        while (w.status != KuyuStatus.OVER && w.frames < 60 * 240) {
            if (w.status == KuyuStatus.CHOOSING) {
                w.chooseUpgrade(0)
                w.resumeFromOffer()
                continue
            }
            if (tik % max(1, gecikme) == 0) {
                val p = w.player
                val sutun = (p.centerX).toInt()
                val satir = (p.y + KuyuWorld.PLAYER_H).toInt()

                val altDusman = w.enemies.any {
                    it.alive && it.y > p.y && it.y - p.y < 6f && kotlin.math.abs(it.x - p.x) < 1.5f
                }
                ates = p.grounded.not() && (altDusman || p.vy > 9f)

                // Altındaki katta en yakın boşluğu bul ve oraya yönel.
                var hedef = sutun
                if (p.grounded) {
                    var enYakin = Int.MAX_VALUE
                    for (c in 1 until KuyuGen.WIDTH - 1) {
                        // Delik, oyuncunun bastığı zemin satırının kendisindedir.
                        var bosluk = !w.tile(satir, c).solid && !w.tile(satir + 1, c).solid
                        if (bosluk && kotlin.math.abs(c - sutun) < enYakin) {
                            enYakin = kotlin.math.abs(c - sutun)
                            hedef = c
                        }
                    }
                }
                val yanDusman = w.enemies.filter {
                    it.alive && kotlin.math.abs(it.y - p.y) < 1.5f && kotlin.math.abs(it.x - p.x) < 2.5f
                }
                when {
                    yanDusman.any { it.x > p.x } -> { sol = true; sag = false }
                    yanDusman.any { it.x < p.x } -> { sol = false; sag = true }
                    else -> { sol = hedef < sutun; sag = hedef > sutun }
                }
            }
            tik++
            w.step(KuyuInput(left = sol, right = sag, fire = ates))
        }
        return Triple(w.depth, w.gemsCollected, w.area + 1)
    }
}
