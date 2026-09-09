package com.za.games.ui.viraj

import android.content.Context

/** Günün pisti: kullanılan deneme sayısı ve en iyi skor. */
data class VirajDaily(val epochDay: Long, val attempts: Int, val best: Long)

/**
 * Viraj kalıcı durumu: günün deneme sayısı ve en iyi skoru. Deneme koşu
 * başlarken düşülür; en iyi skor koşu bitince ya da bırakılınca güncellenir.
 * Yalnızca cihazda tutulur.
 */
class VirajStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_viraj", Context.MODE_PRIVATE)

    fun daily(epochDay: Long): VirajDaily? {
        if (prefs.getLong(KEY_DAY, Long.MIN_VALUE) != epochDay) return null
        return VirajDaily(epochDay, prefs.getInt(KEY_ATTEMPTS, 0), prefs.getLong(KEY_BEST, 0L))
    }

    fun saveDaily(epochDay: Long, attempts: Int, best: Long) {
        prefs.edit()
            .putLong(KEY_DAY, epochDay)
            .putInt(KEY_ATTEMPTS, attempts)
            .putLong(KEY_BEST, best)
            .apply()
    }

    private companion object {
        const val KEY_DAY = "daily_day"
        const val KEY_ATTEMPTS = "daily_attempts"
        const val KEY_BEST = "daily_best"
    }
}
