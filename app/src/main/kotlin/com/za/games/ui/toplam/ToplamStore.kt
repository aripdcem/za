package com.za.games.ui.toplam

import android.content.Context

/** Toplam Kapma tercihleri: rakip, seviye; bilgisayara karşı galibiyet sayısı. */
class ToplamStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_toplam", Context.MODE_PRIVATE)

    var vsComputer: Boolean
        get() = prefs.getBoolean(KEY_VS_COMPUTER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VS_COMPUTER, value).apply()
        }

    var perfect: Boolean
        get() = prefs.getBoolean(KEY_PERFECT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PERFECT, value).apply()
        }

    var wins: Int
        get() = prefs.getInt(KEY_WINS, 0)
        set(value) {
            prefs.edit().putInt(KEY_WINS, value).apply()
        }

    private companion object {
        const val KEY_VS_COMPUTER = "vs_computer"
        const val KEY_PERFECT = "perfect"
        const val KEY_WINS = "wins"
    }
}
