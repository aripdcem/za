package com.za.games.bostan

import kotlin.math.ceil
import kotlin.math.max

/**
 * Uzman politika: üreticinin bir seviyeyi "kazanılabilir" saymak için
 * kullandığı betikli oyuncu. Yarım saniyede bir karar verir: yaşı
 * [DROP_DELAY]'i geçen bir damlayı toplar, sonra öncelik sırasıyla en fazla
 * bir savunma koyar: acil kesme (kulübeye yaklaşan ve önünde kimse olmayan
 * saldırgan), erken kuyular, her şeride bir fıskiye, tehdit altındaki şeride
 * korkuluk ya da tuzak, kovanlar, ek kuyular, ek fıskiyeler.
 *
 * Yerleşim: kuyular arka satırlarda (5–6), fıskiyeler 4→3→2, korkuluklar
 * [FRONT_ROW], kovanlar satır 2'de 1. ve 3. şeritte (üçer şeridi kapsar).
 * İnsan gibi yavaş (damla gecikmesi, adım başına tek yerleşim) ki üretici
 * insanın erişemeyeceği bir standarda göre ölçeklemesin.
 */
object BostanExpert {
    const val TICK = 0.5f
    const val DROP_DELAY = 0.8f
    const val WELL_TARGET = 4
    const val WELL_MAX = 7
    const val SPRINKLERS_PER_LANE = 3
    const val FRONT_ROW = 1
    const val RESERVE = 50

    /** Kuyu yuvaları (satır, şerit): arka satır ortadan dışa. */
    private val WELL_SLOTS = listOf(6 to 2, 6 to 1, 6 to 3, 6 to 0, 6 to 4, 5 to 2, 5 to 1, 5 to 3, 5 to 0, 5 to 4)
    private val SPRINKLER_ROWS = intArrayOf(4, 3, 2)

    /** Kovan yuvaları (şerit, satır). */
    private val HIVE_SLOTS = listOf(1 to 2, 3 to 2)
    private val LANE_ORDER = intArrayOf(2, 1, 3, 0, 4)

    data class Result(val status: BostanStatus, val lives: Int, val score: Int, val time: Float, val placed: Int)

    /** Seviyeyi baştan sona uzmanla oynar. */
    fun play(level: BostanLevel, maxTime: Float = 1500f): Result {
        val s = BostanState(level)
        var acc = 0f
        var placed = 0
        while (s.status == BostanStatus.RUNNING && s.time < maxTime) {
            s.step()
            acc += BostanState.STEP
            if (acc >= TICK - 1e-4f) {
                acc -= TICK
                if (act(s)) placed++
            }
        }
        return Result(s.status, s.lives, s.score, s.time, placed)
    }

    /** Bir karar adımı; savunma koyduysa true. */
    fun act(s: BostanState): Boolean {
        s.drops.firstOrNull { it.age >= DROP_DELAY }?.let { s.collectDrop(it.lane, it.row) }
        return emergency(s) || earlyWells(s) || firstSprinklers(s) || block(s) || trap(s) ||
            hives(s) || moreWells(s) || moreSprinklers(s)
    }

    // ---- şerit çözümlemesi ----

    private fun nearest(s: BostanState, lane: Int): Enemy? {
        var best: Enemy? = null
        for (e in s.enemies) if (e.lane == lane && (best == null || e.y > best.y)) best = e
        return best
    }

    private fun laneHp(s: BostanState, lane: Int): Float {
        var hp = 0f
        for (e in s.enemies) if (e.lane == lane) hp += e.hp
        return hp
    }

    private fun sprinklers(s: BostanState, lane: Int): Int = s.defenders.count { it.lane == lane && it.kind == DefenderKind.FISKIYE }

    private fun wells(s: BostanState): Int = s.defenders.count { it.kind == DefenderKind.KUYU }

    /** Şeridin saniyelik hasarı: fıskiyeler + kapsayan kovanlar. */
    private fun dps(s: BostanState, lane: Int): Float {
        var d = sprinklers(s, lane) * BostanState.JET_DAMAGE / BostanState.FIRE_INTERVAL
        for (k in s.defenders) if (k.kind == DefenderKind.KOVAN && kotlin.math.abs(k.lane - lane) <= 1) d += BostanState.HIVE_DAMAGE / BostanState.HIVE_INTERVAL
        return d
    }

    /** Saldırganın önündeki ilk savunma (henüz ulaşmadığı). */
    private fun ahead(s: BostanState, e: Enemy): Defender? {
        var best: Defender? = null
        for (d in s.defenders) {
            if (d.lane == e.lane && d.row - 0.5f > e.front && (best == null || d.row < best.row)) best = d
        }
        return best
    }

    /** Saldırganın önündeki ilk hücre satırı. */
    private fun frontRow(e: Enemy): Int = ceil(e.front + 0.5f).toInt()

    private fun threatened(s: BostanState): Boolean = s.enemies.any { it.y >= 2.5f }

    private fun reserve(s: BostanState): Int = if (s.time < 40f) 0 else RESERVE

    // ---- kurallar ----

    /** Kulübeye yaklaşan ve önünde kimse olmayan saldırganı kes. */
    private fun emergency(s: BostanState): Boolean {
        var worst: Enemy? = null
        for (lane in 0 until BostanState.COLS) {
            val e = nearest(s, lane) ?: continue
            if (e.y < 2.5f || ahead(s, e) != null) continue
            if (worst == null || e.y > worst.y) worst = e
        }
        val e = worst ?: return false
        val row = max(frontRow(e), 0)
        if (row >= BostanState.ROWS) return false
        for (kind in listOf(DefenderKind.KORKULUK, DefenderKind.FISKIYE, DefenderKind.TUZAK, DefenderKind.KOVAN)) {
            if (s.place(kind, e.lane, row)) return true
        }
        return false
    }

    private fun earlyWells(s: BostanState): Boolean {
        if (wells(s) >= WELL_TARGET || threatened(s)) return false
        if (s.time >= 40f && s.water < DefenderKind.KUYU.cost + RESERVE) return false
        return placeWell(s)
    }

    private fun placeWell(s: BostanState): Boolean {
        for ((row, lane) in WELL_SLOTS) if (s.defenderAt(lane, row) == null) return s.place(DefenderKind.KUYU, lane, row)
        return false
    }

    /** Her şeride önce bir fıskiye; tehdit altındaki şerit önce. */
    private fun firstSprinklers(s: BostanState): Boolean {
        if (!s.affordable(DefenderKind.FISKIYE) || !s.ready(DefenderKind.FISKIYE)) return false
        var lane = -1
        var best = -1f
        for (l in LANE_ORDER) {
            if (sprinklers(s, l) > 0) continue
            val t = laneHp(s, l)
            if (t > best) { best = t; lane = l }
        }
        if (lane < 0) return false
        return placeSprinkler(s, lane)
    }

    private fun placeSprinkler(s: BostanState, lane: Int): Boolean {
        for (row in SPRINKLER_ROWS) if (s.defenderAt(lane, row) == null) return s.place(DefenderKind.FISKIYE, lane, row)
        return false
    }

    /** Hasar yetişmeyecekse korkuluk (ya da vakti varsa tuzak) ile kes. */
    private fun block(s: BostanState): Boolean {
        val lanes = (0 until BostanState.COLS).mapNotNull { nearest(s, it) }.filter { it.y >= -0.6f }.sortedByDescending { it.y }
        for (e in lanes) {
            val next = ahead(s, e)
            if (next != null && next.kind == DefenderKind.KORKULUK) continue
            val reachRow = next?.row?.toFloat() ?: BostanState.HUT_Y
            val tReach = (reachRow - 0.5f - e.front) / e.kind.speed
            val tKill = laneHp(s, e.lane) / max(dps(s, e.lane), 0.05f)
            if (tKill <= tReach) continue
            val limit = next?.row ?: BostanState.ROWS
            var row = max(frontRow(e), FRONT_ROW)
            while (row < limit && s.defenderAt(e.lane, row) != null) row++
            if (row >= limit) continue
            if (s.place(DefenderKind.KORKULUK, e.lane, row)) return true
            val tCell = (row - 0.5f - e.front) / e.kind.speed
            if (tCell >= BostanState.TRAP_ARM + 0.5f && s.place(DefenderKind.TUZAK, e.lane, row)) return true
        }
        return false
    }

    /** İri saldırgana (domuz/ayı) daha girerken tuzak. */
    private fun trap(s: BostanState): Boolean {
        if (s.water < DefenderKind.TUZAK.cost + reserve(s) || !s.ready(DefenderKind.TUZAK)) return false
        for (e in s.enemies) {
            if (e.kind.hp < EnemyKind.DOMUZ.hp || e.y > 0f) continue
            if (s.defenders.any { it.lane == e.lane && it.kind == DefenderKind.TUZAK }) continue
            var row = max(frontRow(e), FRONT_ROW)
            while (row <= 3 && s.defenderAt(e.lane, row) != null) row++
            if (row > 3) continue
            val tCell = (row - 0.5f - e.front) / e.kind.speed
            if (tCell < BostanState.TRAP_ARM + 0.5f) continue
            if (s.place(DefenderKind.TUZAK, e.lane, row)) return true
        }
        return false
    }

    private fun hives(s: BostanState): Boolean {
        if (s.time < 30f || s.water < DefenderKind.KOVAN.cost + RESERVE + 25) return false
        for ((lane, row) in HIVE_SLOTS) if (s.defenderAt(lane, row) == null) return s.place(DefenderKind.KOVAN, lane, row)
        return false
    }

    private fun moreWells(s: BostanState): Boolean {
        if (wells(s) >= WELL_MAX || threatened(s) || s.water < DefenderKind.KUYU.cost + 100) return false
        return placeWell(s)
    }

    private fun moreSprinklers(s: BostanState): Boolean {
        if (s.water < DefenderKind.FISKIYE.cost + reserve(s) || !s.ready(DefenderKind.FISKIYE)) return false
        var lane = -1
        var bestCount = Int.MAX_VALUE
        var bestHp = -1f
        for (l in LANE_ORDER) {
            val c = sprinklers(s, l)
            if (c >= SPRINKLERS_PER_LANE) continue
            val hp = laneHp(s, l)
            if (c < bestCount || (c == bestCount && hp > bestHp)) { bestCount = c; bestHp = hp; lane = l }
        }
        if (lane < 0) return false
        return placeSprinkler(s, lane)
    }
}
