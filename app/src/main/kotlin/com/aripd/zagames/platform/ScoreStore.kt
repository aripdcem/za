package com.aripd.zagames.platform

import android.content.Context

/** Oyun başına en yüksek skoru cihazda saklar. Veri cihazdan asla çıkmaz. */
class ScoreStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_scores", Context.MODE_PRIVATE)

    init {
        migrateLegacyBlokKey()
    }

    fun highScore(gameId: String): Long = prefs.getLong(gameId, 0L)

    fun submit(gameId: String, score: Long) {
        if (score > highScore(gameId)) {
            prefs.edit().putLong(gameId, score).apply()
        }
    }

    /**
     * v0.39.0: Blok'un kimliği değişti; eski anahtarda duran rekor bir kez
     * taşınır ve eski anahtar silinir. Sonraki kurulumlarda hiçbir şey yapmaz.
     */
    private fun migrateLegacyBlokKey() {
        if (!prefs.contains(LEGACY_BLOK_KEY)) return
        val old = prefs.getLong(LEGACY_BLOK_KEY, 0L)
        val edit = prefs.edit().remove(LEGACY_BLOK_KEY)
        if (old > highScore(BLOK_KEY)) edit.putLong(BLOK_KEY, old)
        edit.apply()
    }

    private companion object {
        const val LEGACY_BLOK_KEY = "tetris"
        const val BLOK_KEY = "blok"
    }
}
