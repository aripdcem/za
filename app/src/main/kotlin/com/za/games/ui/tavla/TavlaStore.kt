package com.za.games.ui.tavla

import android.content.Context
import com.za.games.tavla.TavlaMode

/** Tavla kurulum tercihleri ve bilgisayara karşı kazanılan maç sayısı. Yalnızca cihazda. */
class TavlaStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_tavla", Context.MODE_PRIVATE)

    var mode: TavlaMode
        get() = prefs.getString(KEY_MODE, null)?.let { name -> TavlaMode.entries.firstOrNull { it.name == name } }
            ?: TavlaMode.KLASIK
        set(value) {
            prefs.edit().putString(KEY_MODE, value.name).apply()
        }

    var vsComputer: Boolean
        get() = prefs.getBoolean(KEY_VS_COMPUTER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VS_COMPUTER, value).apply()
        }

    var target: Int
        get() = prefs.getInt(KEY_TARGET, 5)
        set(value) {
            prefs.edit().putInt(KEY_TARGET, value).apply()
        }

    var noPinInOpponentHome: Boolean
        get() = prefs.getBoolean(KEY_NO_PIN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_NO_PIN, value).apply()
        }

    var cube: Boolean
        get() = prefs.getBoolean(KEY_CUBE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CUBE, value).apply()
        }

    /** Bilgisayara karşı kazanılan maç sayısı; rekor olarak bildirilir. */
    var wins: Int
        get() = prefs.getInt(KEY_WINS, 0)
        set(value) {
            prefs.edit().putInt(KEY_WINS, value).apply()
        }

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_VS_COMPUTER = "vs_computer"
        const val KEY_TARGET = "target"
        const val KEY_NO_PIN = "no_pin_home"
        const val KEY_CUBE = "cube"
        const val KEY_WINS = "wins"
    }
}
