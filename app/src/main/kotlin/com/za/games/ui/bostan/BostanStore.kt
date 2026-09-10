package com.za.games.ui.bostan

import android.content.Context
import com.za.games.bostan.BostanDifficulty

/** Günün bostanı: kullanılan deneme sayısı (zorluktan bağımsız) ve zorluk başına en iyi skor. */
data class BostanDaily(val epochDay: Long, val attempts: Int, val best: List<Int>) {
    fun bestOf(d: BostanDifficulty): Int = best.getOrElse(d.ordinal) { 0 }
}

/** Bostan kalıcı durumu: günün denemeleri ve en iyileri, zorluk başına serbest rekor, son mod ve zorluk. Yalnızca cihazda. */
class BostanStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_bostan", Context.MODE_PRIVATE)

    fun daily(epochDay: Long): BostanDaily? {
        if (prefs.getLong(KEY_DAY, Long.MIN_VALUE) != epochDay) return null
        return BostanDaily(
            epochDay,
            prefs.getInt(KEY_ATTEMPTS, 0),
            BostanDifficulty.entries.map { prefs.getInt(KEY_DAY_BEST + it.ordinal, 0) },
        )
    }

    fun saveDaily(epochDay: Long, attempts: Int, best: List<Int>) {
        val e = prefs.edit().putLong(KEY_DAY, epochDay).putInt(KEY_ATTEMPTS, attempts)
        for (d in BostanDifficulty.entries) e.putInt(KEY_DAY_BEST + d.ordinal, best.getOrElse(d.ordinal) { 0 })
        e.apply()
    }

    fun freeBest(): List<Int> = BostanDifficulty.entries.map { prefs.getInt(KEY_FREE_BEST + it.ordinal, 0) }

    fun saveFreeBest(difficulty: BostanDifficulty, score: Int): Boolean {
        if (score <= prefs.getInt(KEY_FREE_BEST + difficulty.ordinal, 0)) return false
        prefs.edit().putInt(KEY_FREE_BEST + difficulty.ordinal, score).apply()
        return true
    }

    fun lastDaily(): Boolean = prefs.getBoolean(KEY_MODE_DAILY, true)

    fun saveLastDaily(daily: Boolean) {
        prefs.edit().putBoolean(KEY_MODE_DAILY, daily).apply()
    }

    fun lastDifficulty(): BostanDifficulty =
        BostanDifficulty.entries.getOrElse(prefs.getInt(KEY_DIFFICULTY, 0)) { BostanDifficulty.KOLAY }

    fun saveLastDifficulty(d: BostanDifficulty) {
        prefs.edit().putInt(KEY_DIFFICULTY, d.ordinal).apply()
    }

    private companion object {
        const val KEY_DAY = "daily_day"
        const val KEY_ATTEMPTS = "daily_attempts"
        const val KEY_DAY_BEST = "daily_best_"
        const val KEY_FREE_BEST = "free_best_"
        const val KEY_MODE_DAILY = "mode_daily"
        const val KEY_DIFFICULTY = "difficulty"
    }
}
