package com.za.games.gecit

import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Geçit denge ölçümü — birim testi değil, rapor üretir (bkz. docs/oyun-testi.md).
 * `./gradlew :games:gecit:probe` ile koşar; CI'daki `test` görevinden dışlanır.
 *
 * Geçit'in dengesi tek bir gerilime iner: **beklemek zorundasın ama bekleyemezsin.**
 * Şeridi geçmek için trafikte boşluk beklenir; bu sırada kamera yaklaşır
 * (`creep`) ve 3,5 saniyede kartal kapar. Ölçümün sorusu: bir şeridin kapalı
 * kalma süresi, oyuncunun bekleme bütçesini aşıyor mu?
 */
class GecitBalanceProbe {

    // ------------------------------------------------------------------
    // 1) Bekleme bütçesi
    // ------------------------------------------------------------------

    @Test
    fun waitBudget() {
        println("\n=== 1. BEKLEME BÜTÇESİ ===")
        println("kartal sınırı ${GecitWorld.IDLE_LIMIT}s; kamera hızı satır arttıkça büyür")
        println("kamera oyuncunun ${GecitWorld.BEHIND} satır gerisine kadar yaklaşabilir")
        println()
        println("satır | kamera hızı | kamera payı | kartal payı | geçerli bütçe")
        for (satir in listOf(0, 25, 50, 100, 200, 400)) {
            val hiz = 0.35f + 0.9f * min(1f, satir / 200f)
            val kameraPayi = GecitWorld.BEHIND / hiz
            val butce = min(GecitWorld.IDLE_LIMIT, kameraPayi)
            val darBogaz = if (kameraPayi < GecitWorld.IDLE_LIMIT) "kamera" else "kartal"
            println(
                "${"%5d".format(satir)} |   ${"%.2f".format(hiz)} sat/s |   ${"%5.2f".format(kameraPayi)}s   " +
                    "|    ${"%.2f".format(GecitWorld.IDLE_LIMIT)}s    |  ${"%.2f".format(butce)}s ($darBogaz)",
            )
        }
        println()
        println("Not: yana hamle kartal sayacını tamamen sıfırlamaz (${GecitWorld.IDLE_SIDE_RESET}s'e çeker),")
        println("yani yanda oyalanmak bütçeyi yalnızca kısmen tazeler.")
    }

    // ------------------------------------------------------------------
    // 2) Şeritler ne kadar kapalı kalıyor?
    // ------------------------------------------------------------------

    /**
     * Verilen derinliğe kadar şeritler. Dünyayı ilerletmek işe yaramaz: oyuncu
     * hamle etmedikçe kamera durur ve yeni şerit üretilmez. Üreteç doğrudan
     * çağrılır.
     */
    private fun seritler(tohum: Long, satir: Int): List<Lane> {
        val gen = GecitGen(tohum)
        val liste = ArrayList<Lane>()
        repeat(satir + 6) { liste += gen.next(liste) }
        return liste
    }

    /**
     * Şeridi bir kez ilerletirken **tüm sütunları** aynı anda ölçer: her sütun
     * için en uzun kesintisiz kapalı süre, ve tüm sütunların birden kapalı
     * olduğu en uzun süre. (Sütunları ayrı ayrı ölçmek şeridin evresini
     * kaydırdığı için yanlış sonuç verir.)
     */
    private fun kapaliSureler(lane: Lane, ornek: Int = 2400): Pair<FloatArray, Float> {
        val dt = GecitWorld.STEP
        val en = FloatArray(GecitGen.WIDTH)
        val suan = FloatArray(GecitGen.WIDTH)
        var hepsiSuan = 0f
        var hepsiEn = 0f
        repeat(ornek) {
            lane.update(dt)
            var hepsiKapali = true
            for (c in 0 until GecitGen.WIDTH) {
                val kapali = when (lane.kind) {
                    LaneKind.ROAD, LaneKind.RAIL -> lane.hits(c.toFloat())
                    LaneKind.RIVER -> lane.logUnder(c + 0.5f) == null
                    LaneKind.GRASS -> false
                }
                if (kapali) {
                    suan[c] += dt
                    if (suan[c] > en[c]) en[c] = suan[c]
                } else {
                    suan[c] = 0f
                    hepsiKapali = false
                }
            }
            if (hepsiKapali) {
                hepsiSuan += dt
                if (hepsiSuan > hepsiEn) hepsiEn = hepsiSuan
            } else {
                hepsiSuan = 0f
            }
        }
        return en to hepsiEn
    }

    @Test
    fun laneBlocking() {
        println("\n=== 2. ŞERİTLERİN KAPALI KALMA SÜRESİ ===")
        println("her şeritte, her sütun için en uzun kesintisiz kapalı süre ölçülür;")
        println("bir sütun bütçeden uzun kapalıysa oyuncu o sütundan geçemez")
        println()
        println("tür   | derinlik | en kötü sütun | ortalama en kötü | bütçe | aşan sütun oranı")
        for (satir in listOf(10, 30, 60, 120, 250)) {
            val hiz = 0.35f + 0.9f * min(1f, satir / 200f)
            val butce = min(GecitWorld.IDLE_LIMIT, GecitWorld.BEHIND / hiz)
            val toplam = HashMap<LaneKind, MutableList<Float>>()
            var asan = 0
            var sayilan = 0
            for (tohum in 1L..4L) {
                val hepsi = seritler(tohum, satir)
                for (r in max(1, satir - 4)..min(hepsi.size - 1, satir + 4)) {
                    val lane = hepsi[r]
                    if (lane.kind == LaneKind.GRASS) continue
                    val (sutunlar, _) = kapaliSureler(lane)
                    toplam.getOrPut(lane.kind) { ArrayList() } += sutunlar.max()
                    for (d in sutunlar) {
                        sayilan++
                        if (d > butce) asan++
                    }
                }
            }
            for ((tur, liste) in toplam.entries.sortedBy { it.key.name }) {
                println(
                    "${tur.name.padEnd(6)}|  ${"%5d".format(satir)}   |    ${"%5.2f".format(liste.max())}s     " +
                        "|      ${"%5.2f".format(liste.average())}s     | ${"%.2f".format(butce)}s |  ${"%3d".format(100 * asan / max(1, sayilan))}%",
                )
            }
        }
        println()
        println("Kapalı süre bütçeyi aşıyorsa oyuncu o sütunda bekleyemez; başka sütuna")
        println("kaymak gerekir. Tüm sütunlar aynı anda aşıyorsa şerit adil değildir.")
    }

    // ------------------------------------------------------------------
    // 3) Aynı anda tüm sütunlar kapalı mı? (adillik)
    // ------------------------------------------------------------------

    @Test
    fun fairness() {
        println("\n=== 3. ADİLLİK: ŞERİT TAMAMEN KAPANIYOR MU? ===")
        println("bir şeridin bütün sütunlarının aynı anda kapalı kaldığı en uzun süre")
        println()
        println("derinlik | tür   | en uzun tam kapanma | bütçe | durum")
        for (satir in listOf(10, 30, 60, 120, 250)) {
            val hiz = 0.35f + 0.9f * min(1f, satir / 200f)
            val butce = min(GecitWorld.IDLE_LIMIT, GecitWorld.BEHIND / hiz)
            val enKotu = HashMap<LaneKind, Float>()
            for (tohum in 1L..4L) {
                val hepsi = seritler(tohum, satir)
                for (r in max(1, satir - 4)..min(hepsi.size - 1, satir + 4)) {
                    val lane = hepsi[r]
                    if (lane.kind == LaneKind.GRASS) continue
                    val (_, hepsiKapali) = kapaliSureler(lane)
                    enKotu[lane.kind] = max(enKotu[lane.kind] ?: 0f, hepsiKapali)
                }
            }
            for ((tur, sure) in enKotu.entries.sortedBy { it.key.name }) {
                val durum = if (sure > butce) "AŞIYOR" else "tamam"
                println(
                    "  ${"%5d".format(satir)}  | ${tur.name.padEnd(6)}|       ${"%5.2f".format(sure)}s        " +
                        "| ${"%.2f".format(butce)}s | $durum",
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // 4) İlerleme: insan sınırlı oyuncu nereye kadar gidiyor?
    // ------------------------------------------------------------------

    @Test
    fun progress() {
        println("\n=== 4. İLERLEME (insan sınırlı bot, 8 tohum) ===")
        println("beceri | ort.satır | en iyi | ort.süre | ölüm nedenleri")
        for ((ad, gecikme) in listOf("acemi " to 18, "orta  " to 12, "usta  " to 6)) {
            val satirlar = ArrayList<Int>()
            val sureler = ArrayList<Float>()
            val nedenler = HashMap<DeathCause, Int>()
            for (tohum in 1L..8L) {
                val w = GecitWorld(tohum)
                var tik = 0
                var hamle: Move? = null
                while (w.status == GecitStatus.RUNNING && w.frames < 60 * 300) {
                    if (tik % max(1, gecikme) == 0) hamle = karar(w)
                    tik++
                    w.step(hamle)
                    hamle = null
                }
                satirlar += w.maxRow
                sureler += w.frames / 60f
                w.cause?.let { nedenler[it] = (nedenler[it] ?: 0) + 1 }
            }
            val ozet = nedenler.entries.sortedByDescending { it.value }
                .joinToString(", ") { "${it.key.name}×${it.value}" }
            println(
                "$ad |   ${"%5.0f".format(satirlar.average())}   |  ${satirlar.max()}  " +
                    "|  ${"%5.1f".format(sureler.average())}s |  $ozet",
            )
        }
    }

    /** İleri güvenliyse ilerle; değilse güvenli sütuna kay, o da yoksa bekle. */
    private fun karar(w: GecitWorld): Move? {
        val p = w.player
        val ileri = w.lane(p.row + 1) ?: return Move.FORWARD
        val sutun = p.x.toInt()
        if (inisGuvenli(ileri, sutun)) return Move.FORWARD
        val simdi = w.lane(p.row)
        for (yon in listOf(1, -1)) {
            val hedef = sutun + yon
            if (hedef !in 0 until GecitGen.WIDTH) continue
            if (simdi != null && !inisGuvenli(simdi, hedef)) continue
            if (inisGuvenli(ileri, hedef)) return if (yon > 0) Move.RIGHT else Move.LEFT
        }
        return null
    }

    /**
     * Hamle *inerken* güvenli mi? Zıplama [GecitWorld.HOP_TIME] sürer ve o
     * sırada trafik akar; yalnızca "şu an boş" diye bakmak arabanın altına
     * atlamaktır (ilk sürümde bot ölümlerinin hepsi CAR'dı).
     */
    private fun inisGuvenli(lane: Lane, sutun: Int): Boolean =
        guvenli(lane, sutun, 0f) &&
            guvenli(lane, sutun, GecitWorld.HOP_TIME) &&
            guvenli(lane, sutun, GecitWorld.HOP_TIME + 0.15f)

    /** Şeridin [t] saniye sonraki hâlinde [sutun] güvenli mi? */
    private fun guvenli(lane: Lane, sutun: Int, t: Float): Boolean {
        val sol = sutun + 0.25f
        val sag = sutun + 0.75f
        val merkez = sutun + 0.5f
        return when (lane.kind) {
            LaneKind.GRASS -> !lane.trees[sutun.coerceIn(0, GecitGen.WIDTH - 1)]
            LaneKind.ROAD -> lane.movers.none { m ->
                val mx = ileriX(lane, m, t)
                mx < sag && mx + m.len > sol
            }
            // Tren hızlı ve geniş: uyarı ya da geçiş varken hiç girilmez.
            LaneKind.RAIL -> lane.railPhase == RailPhase.IDLE
            LaneKind.RIVER -> lane.movers.any { m ->
                val mx = ileriX(lane, m, t)
                merkez >= mx - 0.1f && merkez <= mx + m.len + 0.1f
            }
        }
    }

    /** Nesnenin [t] saniye sonraki ekran hücresi. */
    private fun ileriX(lane: Lane, m: Mover, t: Float): Float {
        val evre = lane.phase + lane.dir * lane.speed * t
        return (((m.start + evre) % Lane.TRACK) + Lane.TRACK) % Lane.TRACK - Lane.PAD
    }
}
