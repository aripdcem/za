package com.za.games.ui.tuse

import android.content.Context

/** Günün parçası: kullanılan deneme sayısı ve en iyi süre (ms; 0 = yok). */
data class TuseDaily(val epochDay: Long, val attempts: Int, val bestMs: Long)

/**
 * Tuşe kalıcı durumu: son seçimler, parça başına Klasik en iyi süre ve Sonsuz
 * rekoru, günün deneme sayısı ve en iyi süresi. Yalnızca cihazda.
 */
class TuseStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_tuse", Context.MODE_PRIVATE)

    fun lastMenu(): TuseMenuMode =
        TuseMenuMode.entries.firstOrNull { it.name == prefs.getString(KEY_MENU, null) } ?: TuseMenuMode.CLASSIC

    fun lastSong(): String? = prefs.getString(KEY_SONG, null)

    fun saveLast(menu: TuseMenuMode, songId: String) {
        prefs.edit().putString(KEY_MENU, menu.name).putString(KEY_SONG, songId).apply()
    }

    /** Klasik en iyi süre (ms); yoksa 0. */
    fun classicBest(songId: String): Long = prefs.getLong(KEY_CLASSIC + songId, 0L)

    /** Daha kısa süre rekordur; yeni rekorsa true. */
    fun saveClassicBest(songId: String, ms: Long): Boolean {
        val current = classicBest(songId)
        if (ms <= 0L || (current in 1..ms)) return false
        prefs.edit().putLong(KEY_CLASSIC + songId, ms).apply()
        return true
    }

    fun arcadeBest(songId: String): Int = prefs.getInt(KEY_ARCADE + songId, 0)

    fun saveArcadeBest(songId: String, tiles: Int): Boolean {
        if (tiles <= arcadeBest(songId)) return false
        prefs.edit().putInt(KEY_ARCADE + songId, tiles).apply()
        return true
    }

    fun daily(epochDay: Long): TuseDaily? {
        if (prefs.getLong(KEY_DAY, Long.MIN_VALUE) != epochDay) return null
        return TuseDaily(epochDay, prefs.getInt(KEY_ATTEMPTS, 0), prefs.getLong(KEY_DAY_BEST, 0L))
    }

    fun saveDaily(epochDay: Long, attempts: Int, bestMs: Long) {
        prefs.edit()
            .putLong(KEY_DAY, epochDay)
            .putInt(KEY_ATTEMPTS, attempts)
            .putLong(KEY_DAY_BEST, bestMs)
            .apply()
    }

    private companion object {
        const val KEY_MENU = "last_menu"
        const val KEY_SONG = "last_song"
        const val KEY_CLASSIC = "classic_"
        const val KEY_ARCADE = "arcade_"
        const val KEY_DAY = "daily_day"
        const val KEY_ATTEMPTS = "daily_attempts"
        const val KEY_DAY_BEST = "daily_best"
    }
}
