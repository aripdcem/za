package com.za.games.ui.balkon

import android.content.Context

/**
 * Görünüm teması: mekanik aynıdır, yalnızca mermi, mega ve sesler değişir.
 * Kabak çekirdeği + avuç dolusu, su balonu + kova, tükürük + balgam.
 */
enum class BalkonTheme { CEKIRDEK, BALON, TUKURUK }

/** Balkon kalıcı tercihi: seçili tema. Yalnızca cihazda. */
class BalkonStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_balkon", Context.MODE_PRIVATE)

    var theme: BalkonTheme
        get() = prefs.getString(KEY_THEME, null)
            ?.let { name -> BalkonTheme.entries.firstOrNull { it.name == name } }
            ?: BalkonTheme.CEKIRDEK
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    /** Ulaşılan en yüksek seviye (bitiş kartında). */
    var bestLevel: Int
        get() = prefs.getInt(KEY_BEST_LEVEL, 0)
        set(value) {
            prefs.edit().putInt(KEY_BEST_LEVEL, value).apply()
        }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_BEST_LEVEL = "best_level"
    }
}
