package com.za.games.ui.turetme

import android.content.Context

/**
 * Türetme kalıcı durumu: günün bulunan kelimeleri ve pes edilip edilmediği
 * (aynı gün geri gelince tur kaldığı yerden kurulur). Yalnızca cihazda tutulur.
 */
class TuretmeStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_turetme", Context.MODE_PRIVATE)

    fun dailyDay(lang: String): Long = prefs.getLong(KEY_DAILY_DAY + lang, Long.MIN_VALUE)

    fun dailyFound(lang: String): List<String> = prefs.getString(KEY_DAILY_FOUND + lang, "")!!
            .split(',')
            .filter { it.isNotBlank() }

    fun dailyGivenUp(lang: String): Boolean = prefs.getBoolean(KEY_DAILY_GIVEN_UP + lang, false)

    fun saveDaily(lang: String, epochDay: Long, found: Collection<String>, givenUp: Boolean = false) {
        prefs.edit()
            .putLong(KEY_DAILY_DAY + lang, epochDay)
            .putString(KEY_DAILY_FOUND + lang, found.joinToString(","))
            .putBoolean(KEY_DAILY_GIVEN_UP + lang, givenUp)
            .apply()
    }

    private companion object {
        const val KEY_DAILY_DAY = "daily_day_"
        const val KEY_DAILY_FOUND = "daily_found_"
        const val KEY_DAILY_GIVEN_UP = "daily_given_up_"
    }
}
