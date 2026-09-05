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

    /** Ana menüde seçili grup; null = tümü. */
    var hubCategory: GameCategory?
        get() = prefs.getString(KEY_HUB_CATEGORY, null)
            ?.let { name -> GameCategory.entries.firstOrNull { it.name == name } }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_HUB_CATEGORY) else putString(KEY_HUB_CATEGORY, value.name)
            }.apply()
        }

    /** Oyunun son açılma zamanı (epoch ms); hiç açılmadıysa 0. */
    fun lastPlayed(gameId: String): Long = prefs.getLong(KEY_PLAYED_PREFIX + gameId, 0L)

    fun recordPlay(gameId: String, now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_PLAYED_PREFIX + gameId, now).apply()
    }

    private companion object {
        const val KEY_SOUND = "sound_enabled"
        const val KEY_HAPTICS = "haptics_enabled"
        const val KEY_LEFT_HANDED = "left_handed"
        const val KEY_HUB_CATEGORY = "hub_category"
        const val KEY_PLAYED_PREFIX = "played_"
    }
}
