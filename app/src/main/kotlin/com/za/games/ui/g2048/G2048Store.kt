package com.za.games.ui.g2048

import android.content.Context
import com.za.games.g2048.G2048State
import com.za.games.g2048.G2048Status

/**
 * Devam eden 2048 tahtasını cihazda saklar; uygulama kapatılsa da oyun
 * kaldığı yerden sürer. Yalnızca oynanabilir tahtalar kaydedilir — oyun
 * bitince kayıt silinir, dönüşte yeni oyun başlar.
 */
class G2048Store(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_2048", Context.MODE_PRIVATE)

    fun save(state: G2048State) {
        prefs.edit()
            .putString(KEY_CELLS, state.cells.joinToString(","))
            .putLong(KEY_SCORE, state.score)
            .putInt(KEY_MOVES, state.moves)
            .putBoolean(KEY_REACHED_2048, state.reached2048)
            .putLong(KEY_SEED, state.seed)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    /** Kayıtlı oyun; yoksa ya da veri bozuksa null. */
    fun restore(): G2048State? {
        val raw = prefs.getString(KEY_CELLS, null) ?: return null
        val cells = raw.split(',').mapNotNull { it.toIntOrNull() }
        if (cells.size != G2048State.SIZE * G2048State.SIZE) return null
        if (cells.any { it != 0 && (it < 2 || Integer.bitCount(it) != 1) }) return null
        if (cells.count { it > 0 } < 2) return null
        return G2048State(
            size = G2048State.SIZE,
            cells = cells,
            score = prefs.getLong(KEY_SCORE, 0L),
            moves = prefs.getInt(KEY_MOVES, 0),
            status = G2048Status.RUNNING,
            reached2048 = prefs.getBoolean(KEY_REACHED_2048, false),
            seed = prefs.getLong(KEY_SEED, 0L),
        )
    }

    private companion object {
        const val KEY_CELLS = "cells"
        const val KEY_SCORE = "score"
        const val KEY_MOVES = "moves"
        const val KEY_REACHED_2048 = "reached_2048"
        const val KEY_SEED = "seed"
    }
}
