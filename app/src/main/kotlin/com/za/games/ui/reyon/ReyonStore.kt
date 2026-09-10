package com.za.games.ui.reyon

import android.content.Context
import com.za.games.reyon.ReyonLevel

enum class ReyonMode { DAILY, FREE }

/** Oyun türü: diziliş bulmacası ya da uyum denetimi. */
enum class ReyonKind { PUZZLE, AUDIT }

/**
 * Reyon kalıcı durumu: son seçilen zorluk ve mod, devam eden bulmaca
 * (tohumdan yeniden üretilir; yalnızca yerleşimler, ipucu sayısı ve süre
 * saklanır), günün çözüm kayıtları ve seviye başına en iyi süreler.
 * Yalnızca cihazda tutulur.
 */
class ReyonStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_reyon", Context.MODE_PRIVATE)

    class Saved(
        val seed: Long,
        val level: ReyonLevel,
        val mode: ReyonMode,
        val day: Long,
        val snapshot: IntArray,
        val hints: Int,
        val elapsed: Int,
    )

    data class Record(val time: Int, val hints: Int)

    data class AuditRecord(val time: Int, val mistakes: Int, val hints: Int)

    class SavedAudit(
        val seed: Long,
        val level: ReyonLevel,
        val mode: ReyonMode,
        val day: Long,
        val snapshot: IntArray,
        val elapsed: Int,
    )

    fun lastKind(): ReyonKind =
        ReyonKind.entries.firstOrNull { it.name == prefs.getString(KEY_KIND, null) } ?: ReyonKind.PUZZLE

    fun saveKind(kind: ReyonKind) {
        prefs.edit().putString(KEY_KIND, kind.name).apply()
    }

    fun saveAudit(seed: Long, level: ReyonLevel, mode: ReyonMode, day: Long, snapshot: IntArray, elapsed: Int) {
        prefs.edit()
            .putLong(KEY_A_SEED, seed)
            .putString(KEY_A_LEVEL, level.name)
            .putString(KEY_A_MODE, mode.name)
            .putLong(KEY_A_DAY, day)
            .putString(KEY_A_SNAPSHOT, snapshot.joinToString(","))
            .putInt(KEY_A_ELAPSED, elapsed)
            .apply()
    }

    fun clearAudit() {
        prefs.edit()
            .remove(KEY_A_SEED)
            .remove(KEY_A_LEVEL)
            .remove(KEY_A_MODE)
            .remove(KEY_A_DAY)
            .remove(KEY_A_SNAPSHOT)
            .remove(KEY_A_ELAPSED)
            .apply()
    }

    fun restoreAudit(): SavedAudit? {
        if (!prefs.contains(KEY_A_SEED)) return null
        val level = ReyonLevel.entries.firstOrNull { it.name == prefs.getString(KEY_A_LEVEL, null) } ?: return null
        val mode = ReyonMode.entries.firstOrNull { it.name == prefs.getString(KEY_A_MODE, null) } ?: return null
        val raw = prefs.getString(KEY_A_SNAPSHOT, null) ?: return null
        val snapshot = raw.split(',').map { it.toIntOrNull() ?: return null }.toIntArray()
        if (snapshot.size < 3) return null
        return SavedAudit(prefs.getLong(KEY_A_SEED, 0L), level, mode, prefs.getLong(KEY_A_DAY, 0L), snapshot, prefs.getInt(KEY_A_ELAPSED, 0).coerceAtLeast(0))
    }

    fun auditDailyRecord(day: Long, level: ReyonLevel): AuditRecord? {
        if (prefs.getLong(KEY_A_DAILY_DAY + level.name, Long.MIN_VALUE) != day) return null
        return AuditRecord(
            prefs.getInt(KEY_A_DAILY_TIME + level.name, 0),
            prefs.getInt(KEY_A_DAILY_MISTAKES + level.name, 0),
            prefs.getInt(KEY_A_DAILY_HINTS + level.name, 0),
        )
    }

    fun saveAuditDaily(day: Long, level: ReyonLevel, time: Int, mistakes: Int, hints: Int) {
        if (auditDailyRecord(day, level) != null) return
        prefs.edit()
            .putLong(KEY_A_DAILY_DAY + level.name, day)
            .putInt(KEY_A_DAILY_TIME + level.name, time)
            .putInt(KEY_A_DAILY_MISTAKES + level.name, mistakes)
            .putInt(KEY_A_DAILY_HINTS + level.name, hints)
            .apply()
    }

    fun auditBest(level: ReyonLevel): Int = prefs.getInt(KEY_A_BEST + level.name, 0)

    fun saveAuditBest(level: ReyonLevel, time: Int): Boolean {
        val current = auditBest(level)
        if (current in 1..time) return false
        prefs.edit().putInt(KEY_A_BEST + level.name, time).apply()
        return true
    }

    fun lastLevel(): ReyonLevel =
        ReyonLevel.entries.firstOrNull { it.name == prefs.getString(KEY_LEVEL, null) } ?: ReyonLevel.KOLAY

    fun lastMode(): ReyonMode =
        ReyonMode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) } ?: ReyonMode.DAILY

    fun saveLast(level: ReyonLevel, mode: ReyonMode) {
        prefs.edit().putString(KEY_LEVEL, level.name).putString(KEY_MODE, mode.name).apply()
    }

    fun save(seed: Long, level: ReyonLevel, mode: ReyonMode, day: Long, snapshot: IntArray, hints: Int, elapsed: Int) {
        prefs.edit()
            .putLong(KEY_SEED, seed)
            .putString(KEY_RUN_LEVEL, level.name)
            .putString(KEY_RUN_MODE, mode.name)
            .putLong(KEY_RUN_DAY, day)
            .putString(KEY_SNAPSHOT, snapshot.joinToString(","))
            .putInt(KEY_HINTS, hints)
            .putInt(KEY_ELAPSED, elapsed)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_SEED)
            .remove(KEY_RUN_LEVEL)
            .remove(KEY_RUN_MODE)
            .remove(KEY_RUN_DAY)
            .remove(KEY_SNAPSHOT)
            .remove(KEY_HINTS)
            .remove(KEY_ELAPSED)
            .apply()
    }

    /** Kayıtlı bulmaca; yoksa ya da veri bozuksa null. */
    fun restore(): Saved? {
        if (!prefs.contains(KEY_SEED)) return null
        val level = ReyonLevel.entries.firstOrNull { it.name == prefs.getString(KEY_RUN_LEVEL, null) } ?: return null
        val mode = ReyonMode.entries.firstOrNull { it.name == prefs.getString(KEY_RUN_MODE, null) } ?: return null
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        val snapshot = raw.split(',').map { it.toIntOrNull() ?: return null }.toIntArray()
        if (snapshot.isEmpty()) return null
        return Saved(
            seed = prefs.getLong(KEY_SEED, 0L),
            level = level,
            mode = mode,
            day = prefs.getLong(KEY_RUN_DAY, 0L),
            snapshot = snapshot,
            hints = prefs.getInt(KEY_HINTS, 0).coerceAtLeast(0),
            elapsed = prefs.getInt(KEY_ELAPSED, 0).coerceAtLeast(0),
        )
    }

    fun dailyRecord(day: Long, level: ReyonLevel): Record? {
        if (prefs.getLong(KEY_DAILY_DAY + level.name, Long.MIN_VALUE) != day) return null
        return Record(prefs.getInt(KEY_DAILY_TIME + level.name, 0), prefs.getInt(KEY_DAILY_HINTS + level.name, 0))
    }

    /** Günün ilk çözümü kaydedilir; sonrakiler kaydı değiştirmez. */
    fun saveDaily(day: Long, level: ReyonLevel, time: Int, hints: Int) {
        if (dailyRecord(day, level) != null) return
        prefs.edit()
            .putLong(KEY_DAILY_DAY + level.name, day)
            .putInt(KEY_DAILY_TIME + level.name, time)
            .putInt(KEY_DAILY_HINTS + level.name, hints)
            .apply()
    }

    /** Seviyenin en iyi süresi (saniye); yoksa 0. İpucusuz çözümler sayılır. */
    fun best(level: ReyonLevel): Int = prefs.getInt(KEY_BEST + level.name, 0)

    /** Daha iyiyse kaydeder; döndürdüğü değer yeni rekor olup olmadığıdır. */
    fun saveBest(level: ReyonLevel, time: Int): Boolean {
        val current = best(level)
        if (current in 1..time) return false
        prefs.edit().putInt(KEY_BEST + level.name, time).apply()
        return true
    }

    private companion object {
        const val KEY_LEVEL = "level"
        const val KEY_MODE = "mode"
        const val KEY_SEED = "run_seed"
        const val KEY_RUN_LEVEL = "run_level"
        const val KEY_RUN_MODE = "run_mode"
        const val KEY_RUN_DAY = "run_day"
        const val KEY_SNAPSHOT = "run_snapshot"
        const val KEY_HINTS = "run_hints"
        const val KEY_ELAPSED = "run_elapsed"
        const val KEY_DAILY_DAY = "daily_day_"
        const val KEY_DAILY_TIME = "daily_time_"
        const val KEY_DAILY_HINTS = "daily_hints_"
        const val KEY_BEST = "best_"
        const val KEY_KIND = "kind"
        const val KEY_A_SEED = "audit_seed"
        const val KEY_A_LEVEL = "audit_level"
        const val KEY_A_MODE = "audit_mode"
        const val KEY_A_DAY = "audit_day"
        const val KEY_A_SNAPSHOT = "audit_snapshot"
        const val KEY_A_ELAPSED = "audit_elapsed"
        const val KEY_A_DAILY_DAY = "audit_daily_day_"
        const val KEY_A_DAILY_TIME = "audit_daily_time_"
        const val KEY_A_DAILY_MISTAKES = "audit_daily_mistakes_"
        const val KEY_A_DAILY_HINTS = "audit_daily_hints_"
        const val KEY_A_BEST = "audit_best_"
    }
}
