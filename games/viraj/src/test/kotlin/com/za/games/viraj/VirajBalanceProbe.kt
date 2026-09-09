package com.za.games.viraj

import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Viraj denge ölçümü — birim testi değil, rapor üretir (bkz. docs/oyun-testi.md).
 * `./gradlew :games:viraj:probe` ile koşar; CI'daki `test` görevinden dışlanır.
 *
 * Sürücü modeli gerçek kontrollere uyar: yön ayrık (◄ ► tuşları, −1/0/1),
 * fren bir tuş, ve tepki gecikmesi var. Botun sonucu bir alt sınırdır, tavan
 * değil; bota bağlı olmayan ölçümler (direksiyon yetkisi, süre bütçesi) ayrıca
 * raporlanır.
 */
class VirajBalanceProbe {

    private data class Skill(val name: String, val latency: Int)

    private val skills = listOf(
        Skill("acemi ", 18),   // 300 ms
        Skill("orta  ", 12),   // 200 ms
        Skill("usta  ", 6),    // 100 ms
    )

    // ------------------------------------------------------------------
    // Sürücü
    // ------------------------------------------------------------------

    private class Driver(val world: VirajWorld, val skill: Skill) {
        private var tick = 0
        private var wantX = 0f
        private var wantBrake = false

        fun step(): List<VirajEvent> {
            if (tick % max(1, skill.latency) == 0) decide()
            tick++
            val fark = wantX - world.playerX
            world.steer = when {
                fark > 0.05f -> 1
                fark < -0.05f -> -1
                else -> 0
            }
            world.brake = wantBrake
            return world.step()
        }

        /** Virajı tutmak için gereken azami hız kesri; 1'den büyükse serbest. */
        private fun holdablePct(curve: Float): Float =
            if (abs(curve) < 1e-3f) 2f else 1f / (abs(curve) * VirajWorld.CENTRIFUGAL)

        private fun decide() {
            val idx = world.playerSegmentIndex
            // Önümüzdeki virajın en sertine bak (bir saniyelik yol).
            val ileri = max(1, (world.speed * 1f / VirajWorld.SEGMENT_LENGTH).toInt())
            var enSert = 0f
            for (i in idx..idx + ileri) {
                val c = world.track.segment(i).curve
                if (abs(c) > abs(enSert)) enSert = c
            }
            val pct = world.speed / VirajWorld.MAX_SPEED
            wantBrake = pct > holdablePct(enSert) * 0.95f

            // Hedef sütun: yolun içinde kal, öndeki araçlardan kaç.
            var hedef = -enSert * 0.25f      // virajın içine yaslan
            hedef = hedef.coerceIn(-0.7f, 0.7f)
            val tehlike = world.cars.filter {
                val d = it.z - world.playerZ
                d > 0f && d < VirajWorld.SEGMENT_LENGTH * 12f && it.speed < world.speed
            }
            if (tehlike.isNotEmpty()) {
                val en = tehlike.minByOrNull { it.z - world.playerZ }!!
                if (abs(en.x - hedef) < 0.5f) {
                    hedef = if (en.x > 0f) en.x - 0.6f else en.x + 0.6f
                    hedef = hedef.coerceIn(-0.85f, 0.85f)
                }
            }
            wantX = hedef
        }
    }

    private fun drive(seed: Long, skill: Skill, maxFrames: Int = 60 * 300): Triple<Int, Int, Int> {
        val w = VirajWorld(seed)
        val d = Driver(w, skill)
        var crashes = 0
        var f = 0
        while (w.status == VirajStatus.RUNNING && f < maxFrames) {
            for (e in d.step()) if (e is VirajEvent.Crash) crashes++
            f++
        }
        return Triple(w.checkpoints, w.meters, crashes)
    }

    // ------------------------------------------------------------------
    // 1) Direksiyon yetkisi: viraj tutulabiliyor mu?
    // ------------------------------------------------------------------

    @Test
    fun steering() {
        println("\n=== 1. DİREKSİYON YETKİSİ ===")
        println("yanal hız = 2·speedPct birim/s; merkezkaç = 2·speedPct²·viraj·${VirajWorld.CENTRIFUGAL}")
        println("tam karşı direksiyona rağmen dışarı savrulma koşulu: speedPct·viraj·${VirajWorld.CENTRIFUGAL} > 1")
        println()
        println("viraj | tutulabilen azami hız | 240 km/s karşılığı")
        for (curve in listOf(1.5f, 2.0f, 3.0f, 3.33f, 4.0f, 5.0f, 6.5f)) {
            val pct = 1f / (curve * VirajWorld.CENTRIFUGAL)
            val etiket = if (pct >= 1f) "tam gaz" else "%${(pct * 100).toInt()}"
            println("${"%5.2f".format(curve)} | ${etiket.padEnd(21)} | ${(min(1f, pct) * VirajWorld.KMH_AT_MAX).toInt()} km/s")
        }
        println()
        println("Yolun ürettiği viraj aralığı (zorluk 0 → 1):")
        for (d in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val alt = 1.5f
            val ust = 1.5f + (1.5f + 3.5f * d)
            val segment = (d * 6000).toInt()
            println("  segment ${"%5d".format(segment)} (zorluk ${"%.2f".format(d)}): viraj ${"%.1f".format(alt)}–${"%.1f".format(ust)}" +
                if (ust * VirajWorld.CENTRIFUGAL > 1f) "  ← en sertinde fren şart (azami %${(100f / (ust * VirajWorld.CENTRIFUGAL)).toInt()})" else "")
        }
    }

    // ------------------------------------------------------------------
    // 2) Süre bütçesi: kontrol noktasına yetişilir mi?
    // ------------------------------------------------------------------

    @Test
    fun timeBudget() {
        println("\n=== 2. SÜRE BÜTÇESİ ===")
        val yol = VirajWorld.CHECKPOINT_EVERY * VirajWorld.SEGMENT_LENGTH
        println("kontrol noktası arası ${VirajWorld.CHECKPOINT_EVERY} parça = ${yol.toInt()} birim")
        println("başlangıç süresi ${VirajWorld.START_TIME}s, tavan ${VirajWorld.MAX_TIME}s")
        println()
        println("ortalama hız | geçiş süresi | zorluk 0 ödülü (${VirajWorld.CHECKPOINT_BONUS}s) | zorluk 1 ödülü (${VirajWorld.CHECKPOINT_BONUS - 4}s)")
        for (pct in listOf(1.0f, 0.85f, 0.7f, 0.6f, 0.5f, 0.4f)) {
            val sure = yol / (VirajWorld.MAX_SPEED * pct)
            fun isaret(bonus: Float) = if (sure <= bonus) "yeter" else "AÇIK ${"%.1f".format(sure - bonus)}s"
            println("      %${(pct * 100).toInt()}    |   ${"%5.1f".format(sure)}s    |      ${isaret(VirajWorld.CHECKPOINT_BONUS).padEnd(16)}|  ${isaret(VirajWorld.CHECKPOINT_BONUS - 4f)}")
        }
        println()
        println("Yani ortalama hız, ödülün yola bölümünün altına düşerse süre eriyor.")
        val basaBas0 = yol / VirajWorld.CHECKPOINT_BONUS / VirajWorld.MAX_SPEED
        val basaBas1 = yol / (VirajWorld.CHECKPOINT_BONUS - 4f) / VirajWorld.MAX_SPEED
        println("başa baş ortalama hız: zorluk 0 → %${(basaBas0 * 100).toInt()}, zorluk 1 → %${(basaBas1 * 100).toInt()}")
    }

    // ------------------------------------------------------------------
    // 3) Tam koşum
    // ------------------------------------------------------------------

    @Test
    fun runs() {
        println("\n=== 3. TAM KOŞUM (8 tohum) ===")
        println("beceri | ort.kontrol noktası | en iyi | ort.metre | ort.çarpışma")
        for (s in skills) {
            val rs = (1L..8L).map { drive(it, s) }
            println(
                "${s.name} |        ${"%4.1f".format(rs.map { it.first }.average())}         |   ${rs.maxOf { it.first }}    " +
                    "|  ${"%6.0f".format(rs.map { it.second.toDouble() }.average())}   |     ${"%.1f".format(rs.map { it.third.toDouble() }.average())}",
            )
        }
    }
}
