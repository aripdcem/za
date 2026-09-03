package com.za.games.platform

import android.content.Context

/** Platform ayarları; yalnızca cihazda saklanır. */
class SettingsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_settings", Context.MODE_PRIVATE)

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND, value).apply()
        }

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
        }

    /** Kontrol eli: true = sol el (ateş/ileri tuşu solda). Gerçek zamanlı oyunlar paylaşır. */
    var leftHanded: Boolean
        get() = prefs.getBoolean(KEY_LEFT_HANDED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LEFT_HANDED, value).apply()
        }

    val hasLeftHanded: Boolean get() = prefs.contains(KEY_LEFT_HANDED)

    private companion object {
        const val KEY_SOUND = "sound_enabled"
        const val KEY_HAPTICS = "haptics_enabled"
        const val KEY_LEFT_HANDED = "left_handed"
    }
}
