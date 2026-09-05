package com.za.games.ui.vergici

import android.content.Context

/** Vergici tercihleri ve rekorlar: sayı aralığı, galibiyet sayısı, aralık başına en iyi puan. */
class VergiciStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_vergici", Context.MODE_PRIVATE)

    var size: Int
        get() = prefs.getInt(KEY_SIZE, 20)
        set(value) {
            prefs.edit().putInt(KEY_SIZE, value).apply()
        }

    var wins: Int
        get() = prefs.getInt(KEY_WINS, 0)
        set(value) {
            prefs.edit().putInt(KEY_WINS, value).apply()
        }

    fun best(size: Int): Int = prefs.getInt(KEY_BEST_PREFIX + size, 0)

    fun setBest(size: Int, score: Int) {
        prefs.edit().putInt(KEY_BEST_PREFIX + size, score).apply()
    }

    private companion object {
        const val KEY_SIZE = "size"
        const val KEY_WINS = "wins"
        const val KEY_BEST_PREFIX = "best_"
    }
}
