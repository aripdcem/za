package com.aripd.zagames.ui.besharf

import android.content.Context

/**
 * Beş Harf kalıcı durumu: günün tahminleri (aynı gün geri gelince tahta
 * kaldığı yerden kurulur) ve kazanma serisi. Yalnızca cihazda tutulur.
 *
 * Günlük kayıt dile bağlıdır: her dilin günün kelimesi başkadır, bu yüzden
 * Almanca oynanmış tahminler Türkçe tahtaya geri oynatılamaz. Kayıt dilin
 * etiketiyle saklanır ve dil değişince o günün ilerlemesi ayrı tutulur.
 */
class BesHarfStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_besharf", Context.MODE_PRIVATE)

    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) {
            prefs.edit().putInt(KEY_STREAK, value).apply()
        }

    fun dailyDay(lang: String): Long = prefs.getLong(KEY_DAILY_DAY + lang, Long.MIN_VALUE)

    fun dailyGuesses(lang: String): List<String> =
        prefs.getString(KEY_DAILY_GUESSES + lang, "")!!
            .split(',')
            .filter { it.isNotBlank() }

    fun saveDaily(lang: String, epochDay: Long, guesses: List<String>) {
        prefs.edit()
            .putLong(KEY_DAILY_DAY + lang, epochDay)
            .putString(KEY_DAILY_GUESSES + lang, guesses.joinToString(","))
            .apply()
    }

    private companion object {
        const val KEY_STREAK = "streak"
        const val KEY_DAILY_DAY = "daily_day_"
        const val KEY_DAILY_GUESSES = "daily_guesses_"
    }
}
