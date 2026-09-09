package com.za.games.ui.filo

import android.content.Context

/** Günün filosu: kullanılan deneme sayısı ve en iyi skor. */
data class FiloDaily(val epochDay: Long, val attempts: Int, val best: Long)

/**
 * Filo kalıcı durumu: günün deneme sayısı ve en iyi skoru. Deneme koşu
 * başlarken düşülür; en iyi skor koşu bitince ya da bırakılınca güncellenir.
 * Yalnızca cihazda tutulur.
 */
class FiloStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_filo", Context.MODE_PRIVATE)

    fun daily(epochDay: Long): FiloDaily? {
        if (prefs.getLong(KEY_DAY, Long.MIN_VALUE) != epochDay) return null
        return FiloDaily(epochDay, prefs.getInt(KEY_ATTEMPTS, 0), prefs.getLong(KEY_BEST, 0L))
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
