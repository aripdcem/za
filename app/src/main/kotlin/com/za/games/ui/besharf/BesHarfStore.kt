package com.za.games.ui.besharf

import android.content.Context

/**
 * Beş Harf kalıcı durumu: günün tahminleri (aynı gün geri gelince tahta
 * kaldığı yerden kurulur) ve kazanma serisi. Yalnızca cihazda tutulur.
 */
class BesHarfStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_besharf", Context.MODE_PRIVATE)

    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) {
            prefs.edit().putInt(KEY_STREAK, value).apply()
        }

    val dailyDay: Long
        get() = prefs.getLong(KEY_DAILY_DAY, Long.MIN_VALUE)

    val dailyGuesses: List<String>
        get() = prefs.getString(KEY_DAILY_GUESSES, "")!!
            .split(',')
            .filter { it.isNotBlank() }

    fun saveDaily(epochDay: Long, guesses: List<String>) {
        prefs.edit()
            .putLong(KEY_DAILY_DAY, epochDay)
            .putString(KEY_DAILY_GUESSES, guesses.joinToString(","))
            .apply()
    }

    private companion object {
        const val KEY_STREAK = "streak"
        const val KEY_DAILY_DAY = "daily_day"
        const val KEY_DAILY_GUESSES = "daily_guesses"
    }
}
