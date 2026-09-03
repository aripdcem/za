package com.za.games.ui.gecit

import android.content.Context

/** Günün yolu: kullanılan deneme sayısı ve en iyi skor. */
data class GecitDaily(val epochDay: Long, val attempts: Int, val best: Long)

/**
 * Geçit kalıcı durumu: günün deneme sayısı ve en iyi skoru. Deneme, koşu
 * başlarken düşülür (uygulama arka planda kapansa da hak yanar); en iyi skor
 * koşu bitince ya da bırakılınca güncellenir. Yalnızca cihazda tutulur.
 */
class GecitStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_gecit", Context.MODE_PRIVATE)

    fun daily(epochDay: Long): GecitDaily? {
        if (prefs.getLong(KEY_DAY, Long.MIN_VALUE) != epochDay) return null
        return GecitDaily(epochDay, prefs.getInt(KEY_ATTEMPTS, 0), prefs.getLong(KEY_BEST, 0L))
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
