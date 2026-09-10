package com.za.games.ui.raket

import android.content.Context
import com.za.games.raket.RaketMode
import com.za.games.raket.RaketWorld

/** Günün duvarı: en iyi ralli. */
data class RaketDaily(val epochDay: Long, val best: Int)

/** Bilgisayara karşı bir seviyenin sicili. */
data class RaketRecord(val wins: Int, val losses: Int)

/**
 * Raket kalıcı durumu: son seçimler, seviye başına galibiyet/mağlubiyet,
 * günlük duvarın en iyi rallisi ve serbest duvar rekoru. Yalnızca cihazda.
 */
class RaketStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_raket", Context.MODE_PRIVATE)

    fun lastMode(): RaketMode =
        RaketMode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) } ?: RaketMode.SOLO

    fun lastLevel(): Int = prefs.getInt(KEY_LEVEL, 1).coerceIn(0, RaketWorld.MAX_LEVEL)

    fun lastWallDaily(): Boolean = prefs.getBoolean(KEY_WALL_DAILY, true)

    fun saveLast(mode: RaketMode, level: Int, wallDaily: Boolean) {
        prefs.edit()
            .putString(KEY_MODE, mode.name)
            .putInt(KEY_LEVEL, level)
            .putBoolean(KEY_WALL_DAILY, wallDaily)
            .apply()
    }

    fun record(level: Int): RaketRecord =
        RaketRecord(prefs.getInt("$KEY_WINS$level", 0), prefs.getInt("$KEY_LOSSES$level", 0))

    fun addResult(level: Int, won: Boolean) {
        val key = (if (won) KEY_WINS else KEY_LOSSES) + level
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun daily(epochDay: Long): RaketDaily? {
        if (prefs.getLong(KEY_DAY, Long.MIN_VALUE) != epochDay) return null
        return RaketDaily(epochDay, prefs.getInt(KEY_DAY_BEST, 0))
    }

    /** Günün en iyisini yükseltir; yeni rekorsa true. */
    fun saveDaily(epochDay: Long, rally: Int): Boolean {
        val current = daily(epochDay)
        if (current != null && current.best >= rally) return false
        prefs.edit().putLong(KEY_DAY, epochDay).putInt(KEY_DAY_BEST, rally).apply()
        return true
    }

    fun wallBest(): Int = prefs.getInt(KEY_WALL_BEST, 0)

    /** Serbest duvar rekorunu yükseltir; yeni rekorsa true. */
    fun saveWallBest(rally: Int): Boolean {
        if (rally <= wallBest()) return false
        prefs.edit().putInt(KEY_WALL_BEST, rally).apply()
        return true
    }

    private companion object {
        const val KEY_MODE = "last_mode"
        const val KEY_LEVEL = "last_level"
        const val KEY_WALL_DAILY = "last_wall_daily"
        const val KEY_WINS = "wins_"
        const val KEY_LOSSES = "losses_"
        const val KEY_DAY = "daily_day"
        const val KEY_DAY_BEST = "daily_best"
        const val KEY_WALL_BEST = "wall_best"
    }
}
