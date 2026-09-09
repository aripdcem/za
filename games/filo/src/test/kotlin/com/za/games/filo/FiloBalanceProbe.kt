package com.za.games.filo

import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Filo denge ölçümü — birim testi değil, rapor üretir (bkz. docs/oyun-testi.md).
 * `./gradlew :games:filo:probe` ile koşar; CI'daki `test` görevinden dışlanır.
 *
 * Motor deterministik olduğu için denge cihazsız ölçülebilir. Ölçüm, gemiyi
 * insan sınırlarıyla süren bir botla yapılır: sınırlı sürükleme hızı, tepki
 * gecikmesi ve yalnızca gözle görülebilen bilgi (düşman hızı kareler arası
 * farktan kestirilir). Botun sonucu bir alt sınırdır, tavan değil.
 */
class FiloBalanceProbe {

    // Cihazda ölçüldü (SM-A515F, 1080x2400 @420dpi): oyun alanının 1 birimi
    // 1016 px; ekran genişliğinin tamamı değil (tuvalin kenar boşluğu var).
    // Ölçüm yordamı: docs/oyun-testi.md → C aşaması.
    private val arenaWidthPx = 1016f
    private val dpi = 420f

    /** Arayüzdeki sürükleme katsayısı (FiloScreen.DRAG_GAIN): parmak yolu bu
     *  kadar büyütülerek gemiye aktarılır, yani aynı el hızı daha çok yol eder. */
    private val dragGain = 1.35f
    private fun unitsToMm(u: Float) = u * arenaWidthPx / dpi * 25.4f

    /** Bot beceri profili. */
    private data class Skill(
        val name: String,
        /** Azami sürükleme hızı (arena birimi/s). */
        val dragSpeed: Float,
        /** Tepki gecikmesi (kare). */
        val latency: Int,
    )

    /** El hızı mm/s → oyun birimi/s (sürükleme katsayısı dahil). */
    private fun handSpeed(mmPerSec: Float): Float = mmPerSec * dragGain / (arenaWidthPx / dpi * 25.4f)

    private val skills = listOf(
        Skill("acemi ", handSpeed(100f), 21),   // 100 mm/s, 350 ms
        Skill("orta  ", handSpeed(190f), 15),   // 190 mm/s, 250 ms
        Skill("usta  ", handSpeed(320f), 9),    // 320 mm/s, 150 ms
        Skill("robot ", 99f, 0),                // sınırsız: motorun teorik tavanı
    )

    // ------------------------------------------------------------------
    // Bot
    // ------------------------------------------------------------------

    private class Bot(val world: FiloWorld, val skill: Skill, val useBombs: Boolean = true) {
        var x = FiloWorld.WIDTH / 2f
        private var desired = x
        private var tick = 0

        /** Düşman hızını gözlemleyerek kestir (oyuncu da gözüyle bunu yapar). */
        private val prev = HashMap<Int, Pair<Float, Float>>()
        private val vel = HashMap<Int, Pair<Float, Float>>()

        fun step(): List<FiloEvent> {
            observe()
            if (tick % max(1, skill.latency) == 0) desired = choose()
            tick++
            val maxStep = skill.dragSpeed * FiloWorld.STEP
            val d = desired - x
            x += if (abs(d) <= maxStep) d else if (d > 0) maxStep else -maxStep
            x = x.coerceIn(FiloWorld.PLAYER_MARGIN, FiloWorld.WIDTH - FiloWorld.PLAYER_MARGIN)
            world.steerTo(x)
            if (useBombs && safety(x) < 0.25f) world.bomb()
            return world.step()
        }

        private fun observe() {
            for (e in world.enemies) {
                if (!e.alive) continue
                val p = prev[e.id]
                if (p != null) vel[e.id] = Pair((e.x - p.first) / FiloWorld.STEP, (e.y - p.second) / FiloWorld.STEP)
                prev[e.id] = Pair(e.x, e.y)
            }
        }

        /** [cx] sütununda durursan ilk isabete kalan süre (s); tehdit yoksa büyük. */
        fun safety(cx: Float): Float {
            var t0 = HORIZON
            for (b in world.enemyBullets) {
                if (b.vy <= 0f) continue
                val t = (FiloWorld.PLAYER_Y - b.y) / b.vy
                if (t < 0f || t > HORIZON) continue
                val px = b.x + b.vx * t
                if (abs(px - cx) < FiloWorld.PLAYER_RADIUS + b.radius) t0 = min(t0, t)
            }
            for (e in world.enemies) {
                if (!e.alive) continue
                val v = vel[e.id] ?: continue
                if (v.second <= 0.01f) continue
                val t = (FiloWorld.PLAYER_Y - e.y) / v.second
                if (t < 0f || t > HORIZON) continue
                val px = e.x + v.first * t
                if (abs(px - cx) < FiloWorld.PLAYER_RADIUS + e.kind.radius * 0.8f) t0 = min(t0, t)
            }
            return t0
        }

        /** En güvenli sütun; güvenlik eşitse hedefe en yakın olan. */
        private fun choose(): Float {
            val lo = FiloWorld.PLAYER_MARGIN
            val hi = FiloWorld.WIDTH - FiloWorld.PLAYER_MARGIN
            val target = world.enemies
                .filter { it.alive && it.y > 0f && it.y < FiloWorld.PLAYER_Y - 0.25f }
                .minByOrNull { FiloWorld.PLAYER_Y - it.y }
            var bestX = x
            var bestKey = -Float.MAX_VALUE
            val n = 80
            for (i in 0..n) {
                val cx = lo + (hi - lo) * i / n
                // Ulaşılabilirlik: oraya varana kadar geçen süre.
                val travel = abs(cx - x) / skill.dragSpeed
                val s = min(safety(cx), HORIZON)
                // Varmadan vurulacaksan o sütun işe yaramaz.
                val reachable = if (travel < s) s else s - (travel - s)
                val aim = if (target == null) 0f else -abs(target.x - cx)
                val key = min(reachable, 1.2f) * 10f + aim * 0.5f - abs(cx - x) * 0.05f
                if (key > bestKey) { bestKey = key; bestX = cx }
            }
            return bestX
        }

        companion object { const val HORIZON = 3f }
    }

    private class RunResult(val wave: Int, val score: Long, val kills: Int, val seconds: Float, val deathWaves: List<Int>)

    private fun playRun(seed: Long, skill: Skill, maxFrames: Int = 60 * 60 * 12): RunResult {
        val w = FiloWorld(seed)
        val bot = Bot(w, skill)
        val deaths = ArrayList<Int>()
        var f = 0
        while (w.status == FiloStatus.RUNNING && f < maxFrames) {
            for (e in bot.step()) if (e is FiloEvent.PlayerHit) deaths += w.wave
            f++
        }
        return RunResult(w.wave, w.score, w.kills, f / 60f, deaths)
    }

    // ------------------------------------------------------------------
    // 1) Tam koşum: bot ne kadar ilerliyor?
    // ------------------------------------------------------------------

    @Test
    fun runs() {
        println("\n=== 1. TAM KOŞUM (10 tohum) ===")
        println("beceri | ort.dalga | en iyi | ort.süre | ort.skor | ilk ölüm dalgası")
        for (s in skills) {
            val rs = (1L..10L).map { playRun(it, s) }
            val waves = rs.map { it.wave }
            val firstDeath = rs.mapNotNull { it.deathWaves.firstOrNull() }
            println(
                "${s.name} |   ${"%5.1f".format(waves.average())}   |   ${waves.max()}   " +
                    "| ${"%6.1f".format(rs.map { it.seconds }.average())}s | ${"%8.0f".format(rs.map { it.score.toDouble() }.average())} " +
                    "| ${"%.1f".format(if (firstDeath.isEmpty()) 0.0 else firstDeath.average())}",
            )
        }
    }

    // ------------------------------------------------------------------
    // 2) Patron canı ve öldürme süresi
    // ------------------------------------------------------------------

    @Test
    fun bosses() {
        println("\n=== 2. PATRON CANI / ÖLDÜRME SÜRESİ (TTK) ===")
        println("teorik DPS: silah1=${"%.1f".format(1 / FiloWorld.FIRE_INTERVAL)}  silah2=${"%.1f".format(2 / FiloWorld.FIRE_INTERVAL)}  silah3≈${"%.1f".format(3 / FiloWorld.FIRE_INTERVAL)} (üçü de isabet ederse)")
        println("dalga | can | TTK s1 | TTK s2 | TTK s3 | +2 bomba sonrası s1")
        for (wave in listOf(5, 10, 15, 20, 25, 30, 40)) {
            val d = FiloWorld.difficulty(wave)
            val hp = 30 + 15 * (wave / FiloWorld.BOSS_EVERY - 1) + (10 * d).toInt()
            fun ttk(dps: Float) = hp / dps
            val s1 = 1 / FiloWorld.FIRE_INTERVAL
            println(
                "  ${"%3d".format(wave)} | ${"%3d".format(hp)} | ${"%6.1f".format(ttk(s1))} | ${"%6.1f".format(ttk(2 * s1))} | ${"%6.1f".format(ttk(3 * s1))} | ${"%6.1f".format((hp - 2 * FiloWorld.BOSS_BOMB_DAMAGE) / s1)}",
            )
        }
        // Gerçek ölçüm: bot patronla yalnız kalınca ne kadar sürede indiriyor?
        println("\nölçülen (bot, silah seviyesi sabit, bombasız):")
        println("dalga | silah | ölçülen TTK | isabet oranı | bu sürede yenen mermi")
        for (wave in listOf(5, 10, 20, 30)) {
            for (weapon in 1..3) {
                val w = FiloWorld(7L)
                w.jumpToWaveForTest(wave)
                w.setWeaponForTest(weapon)
                w.setLivesForTest(99)
                val bot = Bot(w, Skill("x", 5f, 9), useBombs = false)
                var frames = 0
                var down = -1
                var hits = 0
                var shots = 0
                var taken = 0
                while (frames < 60 * 120 && down < 0) {
                    for (e in bot.step()) {
                        when (e) {
                            is FiloEvent.Shot -> shots += if (weapon == 1) 1 else if (weapon == 2) 2 else 3
                            is FiloEvent.EnemyHit -> hits++
                            is FiloEvent.EnemyDown -> if (e.kind == EnemyKind.BOSS) down = frames
                            is FiloEvent.PlayerHit -> taken++
                            else -> Unit
                        }
                    }
                    frames++
                }
                w.setWeaponForTest(weapon)
                println("  ${"%3d".format(wave)} |   $weapon   | ${if (down < 0) "  >120s" else "%6.1f".format(down / 60f)}s | ${"%5.0f".format(100f * hits / max(1, shots))}% | $taken")
            }
        }
    }

    // ------------------------------------------------------------------
    // 3) Sürükleme hassasiyeti: kaçmak için gereken parmak yolu
    // ------------------------------------------------------------------

    @Test
    fun drag() {
        println("\n=== 3. SÜRÜKLEME HASSASİYETİ ===")
        println("oyun alanı 1 birim = ${"%.0f".format(arenaWidthPx)} px = ${"%.1f".format(unitsToMm(1f))} mm; sürükleme katsayısı ${dragGain}")
        println("oyun alanı genişliği = ${"%.1f".format(unitsToMm(1f))} mm parmak yolu")
        println("kenardan kenara (%.2f birim) = %.1f mm parmak yolu".format(1f - 2 * FiloWorld.PLAYER_MARGIN, unitsToMm(1f - 2 * FiloWorld.PLAYER_MARGIN) / dragGain))
        println("en dar kaçış (gemi+mermi yarıçapı) = ${"%.1f".format(unitsToMm(FiloWorld.PLAYER_RADIUS + FiloWorld.ENEMY_BULLET_RADIUS))} mm")
        println()
        println("Mermi iniş süresi ve o sürede kat edilebilen yol:")
        println("dalga | mermi hızı | patron→oyuncu süre | acemi | orta | usta  (mm)")
        for (wave in listOf(1, 5, 10, 20, 26)) {
            val d = FiloWorld.difficulty(wave)
            val speed = FiloWorld.ENEMY_BULLET_SPEED * (1f + 0.5f * d)
            val t = (FiloWorld.PLAYER_Y - FiloWorld.BOSS_Y) / speed
            val reach = skills.dropLast(1).map { s ->
                val eff = max(0f, t - s.latency / 60f)
                unitsToMm(eff * s.dragSpeed)
            }
            println(
                "  ${"%3d".format(wave)} |   ${"%.2f".format(speed)}   |      ${"%.2f".format(t)}s       |" +
                    reach.joinToString("|") { "%6.1f".format(it) },
            )
        }
        println()
        // Yakın menzil: dalış yapan düşman oyuncuya yakınken ateş ederse?
        println("Yakın menzilden (y=1.0) atılan mermi için:")
        for (wave in listOf(1, 10, 26)) {
            val d = FiloWorld.difficulty(wave)
            val speed = FiloWorld.ENEMY_BULLET_SPEED * (1f + 0.5f * d)
            val t = (FiloWorld.PLAYER_Y - 1.0f) / speed
            val need = unitsToMm(FiloWorld.PLAYER_RADIUS + FiloWorld.ENEMY_BULLET_RADIUS)
            println("  dalga ${"%3d".format(wave)}: süre ${"%.2f".format(t)}s, gereken ${"%.1f".format(need)} mm → " +
                skills.dropLast(1).joinToString(", ") { s ->
                    val eff = max(0f, t - s.latency / 60f)
                    val got = unitsToMm(eff * s.dragSpeed)
                    "${s.name.trim()} ${"%.1f".format(got)}mm ${if (got >= need) "✓" else "✗"}"
                })
        }
    }

    // ------------------------------------------------------------------
    // 4) Düşman hızları ve dalga yoğunluğu
    // ------------------------------------------------------------------

    @Test
    fun enemies() {
        println("\n=== 4. DÜŞMAN HIZLARI ===")
        println("desen  | dalga1 hız | dalga26 hız | ekranda kalma s (d1→d26) | oyuncuya varış s")
        for (p in listOf(Pattern.DIVE, Pattern.SINE, Pattern.SWEEP, Pattern.RING, Pattern.DRIFT)) {
            fun base(d: Float) = when (p) {
                Pattern.DIVE -> 0.42f + 0.28f * d
                Pattern.SINE -> 0.28f + 0.2f * d
                Pattern.SWEEP -> 0.45f + 0.25f * d
                Pattern.RING -> 0.3f + 0.15f * d
                Pattern.DRIFT -> 0.22f + 0.12f * d
                Pattern.BOSS -> 0.35f
            }
            val v1 = base(0f); val v26 = base(1f)
            val cross1 = FiloWorld.HEIGHT / v1
            val cross26 = FiloWorld.HEIGHT / v26
            val reach1 = (FiloWorld.PLAYER_Y + 0.08f) / v1
            val reach26 = (FiloWorld.PLAYER_Y + 0.08f) / v26
            println(
                "${p.name.padEnd(7)}|    ${"%.2f".format(v1)}    |    ${"%.2f".format(v26)}     |   ${"%.1f".format(cross1)} → ${"%.1f".format(cross26)}   |  ${"%.1f".format(reach1)} → ${"%.1f".format(reach26)}",
            )
        }
        println("\n=== DALGA YOĞUNLUĞU (tohum 1..8 ortalaması) ===")
        println("dalga | düşman | drone | wasp | tank | asteroit | ateş eden | süre s | tepe mermi")
        for (wave in listOf(1, 3, 5, 8, 12, 16, 20, 26, 32)) {
            var tot = 0; var dr = 0; var wa = 0; var ta = 0; var asx = 0; var fire = 0
            var dur = 0f; var peak = 0; val seeds = 8
            for (seed in 1L..seeds.toLong()) {
                val w = FiloWorld(seed)
                w.jumpToWaveForTest(wave)
                w.setLivesForTest(9999)
                val bot = Bot(w, Skill("x", 3f, 15), useBombs = false)
                var f = 0
                var started = false
                var maxB = 0
                val seen = HashSet<Int>()
                while (f < 60 * 120) {
                    val evs = bot.step()
                    if (evs.any { it is FiloEvent.WaveStart }) started = true
                    for (e in w.enemies) if (e.alive && seen.add(e.id)) {
                        tot++
                        if (e.fireEvery > 0f) fire++
                        when (e.kind) {
                            EnemyKind.DRONE -> dr++
                            EnemyKind.WASP -> wa++
                            EnemyKind.TANK -> ta++
                            EnemyKind.ASTEROID -> asx++
                            EnemyKind.BOSS -> Unit
                        }
                    }
                    maxB = max(maxB, w.enemyBullets.size)
                    if (started && evs.any { it is FiloEvent.WaveClear }) break
                    f++
                }
                dur += f / 60f; peak += maxB
            }
            fun a(v: Int) = v.toFloat() / seeds
            println(
                "  ${"%3d".format(wave)} |  ${"%4.1f".format(a(tot))}  | ${"%5.1f".format(a(dr))} | ${"%4.1f".format(a(wa))} | ${"%4.1f".format(a(ta))} |   ${"%4.1f".format(a(asx))}   |   ${"%5.1f".format(a(fire))}   | ${"%6.1f".format(dur / seeds)} |    ${"%3d".format(peak / seeds)}",
            )
        }
    }

    // ------------------------------------------------------------------
    // 5) Silah verimi: kusursuz nişan, kaçınma yok (DPS tavanı)
    // ------------------------------------------------------------------

    @Test
    fun weapons() {
        println("\n=== 5. SİLAH VERİMİ (patronu kusursuz takip, kaçınma yok) ===")
        println("dalga | can | silah1 | silah2 | silah3   (saniye; parantezde isabet oranı)")
        for (wave in listOf(5, 10, 15, 20, 25, 30)) {
            val cells = (1..3).map { weapon ->
                val w = FiloWorld(11L)
                w.jumpToWaveForTest(wave)
                w.setLivesForTest(999999)
                var f = 0; var down = -1; var shots = 0; var hits = 0
                while (f < 60 * 180 && down < 0) {
                    val b = w.boss
                    if (b != null) w.steerTo(b.x)
                    w.setWeaponForTest(weapon)
                    for (e in w.step()) {
                        when (e) {
                            is FiloEvent.Shot -> shots += weapon.coerceAtMost(3)
                            is FiloEvent.EnemyHit -> hits++
                            is FiloEvent.EnemyDown -> if (e.kind == EnemyKind.BOSS) down = f
                            else -> Unit
                        }
                    }
                    f++
                }
                val ttk = if (down < 0) "  >180" else "%6.1f".format(down / 60f)
                "$ttk (${"%3.0f".format(100f * hits / max(1, shots))}%)"
            }
            val d = FiloWorld.difficulty(wave)
            val hp = 30 + 15 * (wave / FiloWorld.BOSS_EVERY - 1) + (10 * d).toInt()
            println("  ${"%3d".format(wave)} | ${"%3d".format(hp)} | ${cells.joinToString(" | ")}")
        }
    }
}