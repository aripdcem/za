package com.za.games.ui.kuyu

import android.content.Context
import com.za.games.platform.SettingsStore

/** Günün koşusu: başlandıysa kaydedilir; [done] koşu bittiğinde true olur. */
data class KuyuDaily(val epochDay: Long, val score: Long, val depth: Int, val done: Boolean)

/**
 * Kuyu kalıcı durumu: kontrol eli ve günün koşusu (skor/derinlik). Günlük koşu
 * başlar başlamaz kaydedilir (tek deneme); duraklamalarda ve bitişte güncellenir,
 * böylece uygulama arka planda kapatılsa bile o ana kadarki sonuç kalır.
 * Yalnızca cihazda tutulur.
 */
class KuyuStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("za_kuyu", Context.MODE_PRIVATE)

    private val settings = SettingsStore(context)

    init {
        // Önceki sürümlerde Kuyu'ya özel saklanan el tercihi ortak ayara taşınır.
        if (prefs.contains(KEY_LEFT_HANDED)) {
            if (!settings.hasLeftHanded) settings.leftHanded = prefs.getBoolean(KEY_LEFT_HANDED, false)
            prefs.edit().remove(KEY_LEFT_HANDED).apply()
        }
    }

    /** Ortak kontrol eli ayarı (Geçit ile paylaşılır). */
    var leftHanded: Boolean
        get() = settings.leftHanded
        set(value) {
            settings.leftHanded = value
        }

    /** Günün koşusu; o gün hiç başlanmadıysa null. */
    fun daily(epochDay: Long): KuyuDaily? {
        if (prefs.getLong(KEY_DAILY_DAY, Long.MIN_VALUE) != epochDay) return null
        return KuyuDaily(
            epochDay = epochDay,
            score = prefs.getLong(KEY_DAILY_SCORE, 0L),
            depth = prefs.getInt(KEY_DAILY_DEPTH, 0),
            done = prefs.getBoolean(KEY_DAILY_DONE, false),
        )
    }

    fun saveDaily(epochDay: Long, score: Long, depth: Int, done: Boolean) {
        prefs.edit()
            .putLong(KEY_DAILY_DAY, epochDay)
            .putLong(KEY_DAILY_SCORE, score)
            .putInt(KEY_DAILY_DEPTH, depth)
            .putBoolean(KEY_DAILY_DONE, done)
            .apply()
    }

    private companion object {
        const val KEY_LEFT_HANDED = "left_handed"
        const val KEY_DAILY_DAY = "daily_day"
        const val KEY_DAILY_SCORE = "daily_score"
        const val KEY_DAILY_DEPTH = "daily_depth"
        const val KEY_DAILY_DONE = "daily_done"
    }
}
