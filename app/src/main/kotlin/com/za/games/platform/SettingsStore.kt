package com.za.games.platform

import android.content.Context

/** Platform ayarları; yalnızca cihazda saklanır. */
class SettingsStore(context: Context) {

    // attachBaseContext sırasında (ZaLocale.wrap) uygulama bağlamı henüz
    // kurulmamış olabilir; o durumda verilen bağlamla devam edilir.
    private val prefs = (context.applicationContext ?: context)
        .getSharedPreferences("za_settings", Context.MODE_PRIVATE)

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

    /**
     * Seçili dilin BCP-47 etiketi, ya da [ZaLocale.SYSTEM] (telefonun dili).
     *
     * Yalnızca Android 8-12'de kullanılır; 13+ sürümlerde doğru kaynak sistemin
     * kendi uygulama-dili ayarıdır (bkz. [ZaLocale.selected]).
     */
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, ZaLocale.SYSTEM) ?: ZaLocale.SYSTEM
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
        }

    /**
     * Kelime oyunlarının dili (WordLang etiketi), ya da boş: arayüzün dilini izle.
     *
     * Arayüz dilinden ayrı tutulur, çünkü ikisi aynı olmak zorunda değil:
     * Almanya'daki bir oyuncu uygulamayı Almanca kullanıp Beş Harf'i Türkçe
     * oynayabilir. Arayüz dilinin kelime listesi yoksa oyun İngilizceye düşer.
     */
    var wordLanguage: String
        get() = prefs.getString(KEY_WORD_LANGUAGE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_WORD_LANGUAGE, value).apply()
        }

    /** Ana menüde seçili grup; null = tümü. */
    var hubCategory: GameCategory?
        get() = prefs.getString(KEY_HUB_CATEGORY, null)
            ?.let { name -> GameCategory.entries.firstOrNull { it.name == name } }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_HUB_CATEGORY) else putString(KEY_HUB_CATEGORY, value.name)
            }.apply()
        }

    /** Son görülen sürüm kodu (Yenilikler kartı kapatılınca güncellenir); ilk kurulumda 0. */
    var lastSeenVersionCode: Int
        get() = prefs.getInt(KEY_SEEN_VERSION, 0)
        set(value) {
            prefs.edit().putInt(KEY_SEEN_VERSION, value).apply()
        }

    init {
        // v0.39.0: Blok'un kimliği değişti; "son oynananlar" kaydı bir kez taşınır.
        val legacy = KEY_PLAYED_PREFIX + "tetris"
        if (prefs.contains(legacy)) {
            prefs.edit()
                .putLong(KEY_PLAYED_PREFIX + "blok", prefs.getLong(legacy, 0L))
                .remove(legacy)
                .apply()
        }
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
        const val KEY_LANGUAGE = "language"
        const val KEY_WORD_LANGUAGE = "word_language"
        const val KEY_HUB_CATEGORY = "hub_category"
        const val KEY_PLAYED_PREFIX = "played_"
        const val KEY_SEEN_VERSION = "seen_version"
    }
}
