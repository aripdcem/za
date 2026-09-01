package com.za.games.ui.turetme

import android.content.Context

/**
 * Türetme kalıcı durumu: günün bulunan kelimeleri ve pes edilip edilmediği
 * (aynı gün geri gelince tur kaldığı yerden kurulur). Yalnızca cihazda tutulur.
 */
class TuretmeStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_turetme", Context.MODE_PRIVATE)

    val dailyDay: Long
        get() = prefs.getLong(KEY_DAILY_DAY, Long.MIN_VALUE)

    val dailyFound: List<String>
        get() = prefs.getString(KEY_DAILY_FOUND, "")!!
            .split(',')
            .filter { it.isNotBlank() }

    val dailyGivenUp: Boolean
        get() = prefs.getBoolean(KEY_DAILY_GIVEN_UP, false)

    fun saveDaily(epochDay: Long, found: Collection<String>, givenUp: Boolean = false) {
        prefs.edit()
            .putLong(KEY_DAILY_DAY, epochDay)
            .putString(KEY_DAILY_FOUND, found.joinToString(","))
            .putBoolean(KEY_DAILY_GIVEN_UP, givenUp)
            .apply()
    }

    private companion object {
        const val KEY_DAILY_DAY = "daily_day"
        const val KEY_DAILY_FOUND = "daily_found"
        const val KEY_DAILY_GIVEN_UP = "daily_given_up"
    }
}
