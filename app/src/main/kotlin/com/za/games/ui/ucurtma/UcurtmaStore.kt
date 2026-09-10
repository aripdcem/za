package com.za.games.ui.ucurtma

import android.content.Context
import com.za.games.ucurtma.Gadget

/** Günün göğü: kullanılan deneme sayısı ve en iyi skor. */
data class UcurtmaDaily(val epochDay: Long, val attempts: Int, val best: Int)

/**
 * Uçurtma kalıcı durumu: günün deneme sayısı ve en iyi skoru, serbest rekor,
 * açık üç görevin dizinleri, tamamlanan görev sayısı, takılı ekipman.
 * Yalnızca cihazda.
 */
class UcurtmaStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_ucurtma", Context.MODE_PRIVATE)

    fun daily(epochDay: Long): UcurtmaDaily? {
        if (prefs.getLong(KEY_DAY, Long.MIN_VALUE) != epochDay) return null
        return UcurtmaDaily(epochDay, prefs.getInt(KEY_ATTEMPTS, 0), prefs.getInt(KEY_DAY_BEST, 0))
    }

    fun saveDaily(epochDay: Long, attempts: Int, best: Int) {
        prefs.edit().putLong(KEY_DAY, epochDay).putInt(KEY_ATTEMPTS, attempts).putInt(KEY_DAY_BEST, best).apply()
    }

    fun freeBest(): Int = prefs.getInt(KEY_FREE_BEST, 0)

    /** Serbest rekoru yükseltir; yeni rekorsa true. */
    fun saveFreeBest(score: Int): Boolean {
        if (score <= freeBest()) return false
        prefs.edit().putInt(KEY_FREE_BEST, score).apply()
        return true
    }

    fun lastDaily(): Boolean = prefs.getBoolean(KEY_MODE_DAILY, true)

    fun saveLastDaily(daily: Boolean) {
        prefs.edit().putBoolean(KEY_MODE_DAILY, daily).apply()
    }

    /** Açık görevlerin dizinleri; ilk açılışta 0, 1, 2. */
    fun activeMissions(): List<Int> {
        val raw = prefs.getString(KEY_ACTIVE, null) ?: return listOf(0, 1, 2)
        val parsed = raw.split(',').mapNotNull { it.toIntOrNull() }
        return if (parsed.size == 3) parsed else listOf(0, 1, 2)
    }

    /** Görev dizisinde sıradaki (henüz verilmemiş) dizin. */
    fun nextMission(): Int = prefs.getInt(KEY_NEXT, 3)

    fun completedMissions(): Int = prefs.getInt(KEY_COMPLETED, 0)

    fun saveMissions(active: List<Int>, next: Int, completed: Int) {
        prefs.edit()
            .putString(KEY_ACTIVE, active.joinToString(","))
            .putInt(KEY_NEXT, next)
            .putInt(KEY_COMPLETED, completed)
            .apply()
    }

    fun gadget(): Gadget? = Gadget.entries.firstOrNull { it.name == prefs.getString(KEY_GADGET, null) }

    fun saveGadget(gadget: Gadget?) {
        prefs.edit().apply {
            if (gadget == null) remove(KEY_GADGET) else putString(KEY_GADGET, gadget.name)
        }.apply()
    }

    private companion object {
        const val KEY_DAY = "daily_day"
        const val KEY_ATTEMPTS = "daily_attempts"
        const val KEY_DAY_BEST = "daily_best"
        const val KEY_FREE_BEST = "free_best"
        const val KEY_MODE_DAILY = "mode_daily"
        const val KEY_ACTIVE = "missions_active"
        const val KEY_NEXT = "missions_next"
        const val KEY_COMPLETED = "missions_completed"
        const val KEY_GADGET = "gadget"
    }
}
