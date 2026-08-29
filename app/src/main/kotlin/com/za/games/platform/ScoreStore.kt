package com.za.games.platform

import android.content.Context

/** Oyun başına en yüksek skoru cihazda saklar. Veri cihazdan asla çıkmaz. */
class ScoreStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_scores", Context.MODE_PRIVATE)

    fun highScore(gameId: String): Long = prefs.getLong(gameId, 0L)

    fun submit(gameId: String, score: Long) {
        if (score > highScore(gameId)) {
            prefs.edit().putLong(gameId, score).apply()
        }
    }
}
