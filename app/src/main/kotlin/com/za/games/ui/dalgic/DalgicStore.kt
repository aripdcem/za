package com.za.games.ui.dalgic

import android.content.Context

/** Günün denizi: kullanılan deneme sayısı ve en iyi skor. */
data class DalgicDaily(val epochDay: Long, val attempts: Int, val best: Int)

/** Dalgıç kalıcı durumu: günün denemeleri ve en iyisi, serbest rekor, son mod. Yalnızca cihazda. */
class DalgicStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_dalgic", Context.MODE_PRIVATE)

    fun daily(epochDay: Long): DalgicDaily? {
        if (prefs.getLong(KEY_DAY, Long.MIN_VALUE) != epochDay) return null
        return DalgicDaily(epochDay, prefs.getInt(KEY_ATTEMPTS, 0), prefs.getInt(KEY_DAY_BEST, 0))
    }

    fun saveDaily(epochDay: Long, attempts: Int, best: Int) {
        prefs.edit().putLong(KEY_DAY, epochDay).putInt(KEY_ATTEMPTS, attempts).putInt(KEY_DAY_BEST, best).apply()
    }

    fun freeBest(): Int = prefs.getInt(KEY_FREE_BEST, 0)

    fun saveFreeBest(score: Int): Boolean {
        if (score <= freeBest()) return false
        prefs.edit().putInt(KEY_FREE_BEST, score).apply()
        return true
    }

    fun lastDaily(): Boolean = prefs.getBoolean(KEY_MODE_DAILY, true)

    fun saveLastDaily(daily: Boolean) {
        prefs.edit().putBoolean(KEY_MODE_DAILY, daily).apply()
    }

    private companion object {
        const val KEY_DAY = "daily_day"
        const val KEY_ATTEMPTS = "daily_attempts"
        const val KEY_DAY_BEST = "daily_best"
        const val KEY_FREE_BEST = "free_best"
        const val KEY_MODE_DAILY = "mode_daily"
    }
}
